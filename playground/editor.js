/*
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

// see: https://codemirror.net/examples/bundle/
// https://codemirror.net/docs/ref/
// https://codemirror.net/examples/config/
// https://codemirror.net/try/
// https://discuss.codemirror.net/t/codemirror-6-proper-way-to-listen-for-changes/2395/10
// https://discuss.codemirror.net/t/listen-to-change-event/5095/1
// https://codemirror.net/examples/styling/

import {EditorView, basicSetup} from "codemirror"
import {Compartment} from "@codemirror/state"
import {json} from "@codemirror/lang-json"
import {xml} from "@codemirror/lang-xml"
import {language, syntaxHighlighting} from "@codemirror/language" // included by other lang deps
import {classHighlighter} from "@lezer/highlight"

// see: https://rollupjs.org/troubleshooting/#warning-treating-module-as-external-dependency
import {Subject, debounceTime} from 'rxjs'; // external dep through cdn

// HTTP request to transformer app
async function transform(data) {
    return await fetch('https://6psg46wepa.execute-api.us-east-1.amazonaws.com/', {
        method: 'POST',
        mode: 'cors',
        cache: 'no-cache',
        credentials: 'same-origin',
        headers: {
            'Content-Type': 'application/json'
        },
        redirect: 'follow',
        referrerPolicy: 'no-referrer',
        body: JSON.stringify(data) // body data type must match "Content-Type" header
    });
}

// todo: somehow cancel an ongoing transformation if new changes
// only act on most recent change from each burst
// see: https://rxjs.dev/api/operators/debounceTime
const transformSubject = new Subject().pipe(debounceTime(1000));
transformSubject.subscribe({
    error: _ => console.error("Error!"),
    next: _ => {
        const out = document.getElementById("out-editor");
        out.style.filter = "blur(2px)";

        // todo: consider using rxjs instead of promises
        transform({
            template: xtrEditor.state.doc.toString(),
            payload: {'content-type': 'unknown/unknown', content: inEditor.state.doc.toString()}
        }).then((resp) => {
            return Promise.all([resp, resp.text()])
        }).then((resp) => {
            var ctype = resp[0].headers.get("content-type");
            var data = resp[1];
            var lang = null;
            if (ctype.includes("xml")) {
                data = html_beautify(resp[1]); // global functions through cdn w/o needing rollup 'external'
                lang = xml();
            } else if (ctype.includes("json")) {
                data = js_beautify(resp[1]);
                lang = json();
            }

            // only change editor lang if it's actually changed
            if (lang != null && outEditor.state.facet(language).name != lang.language.name) {
                outEditor.dispatch({
                    effects: outFormat.reconfigure(lang)
                })
            }

            const update = outEditor.state.update({
                changes: {
                    from: 0,
                    to: outEditor.state.doc.length,
                    insert: data
                }
            });
            outEditor.update([update]);
        }).finally(() => out.style.filter = "")
    }
});

let transformListener = EditorView.updateListener.of(view => {
    if (view.docChanged) {
        transformSubject.next(view);
    }
})

let outFormat = new Compartment
let outEditor = new EditorView({
    extensions: [basicSetup, EditorView.lineWrapping, outFormat.of(xml()), EditorView.editable.of(false)],
    parent: document.getElementById("out-editor"),
    doc: `<?xml version='1.0' encoding='UTF-8'?>
<root>
    <message>hello world!</message>
</root>`
})

let xtrEditor = new EditorView({
    parent: document.getElementById("xtr-editor"),
    extensions: [basicSetup, EditorView.lineWrapping, transformListener],
    doc: `/** xtrasonnet
input payload application/json
output application/xml
*/
{
    root: payload
}`
})

let inFormat = new Compartment
let inEditor = new EditorView({
    parent: document.getElementById("in-editor"),
    extensions: [basicSetup, EditorView.lineWrapping, inFormat.of(json()), transformListener],
    doc: `{
    "message": "hello world!"
}`
})
