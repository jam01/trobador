package io.github.jam01.xtrasonnet.plugins;

/*-
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.TestUtils;
import io.github.jam01.xtrasonnet.Transformer;
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;

public class ExcelPluginTest {
    private final String simple_xlsx_json = """
            {
                "Sheet1": [
                    {"A":"a1","B":"b1","C":"c1","D":"d1","E":"e1"},
                    {"A":"a2","B":"b2","C":"c2","D":"d2","E":"e2"},
                    {"A":"a3","B":"b3","C":"c3","D":"d3","E":"e3"},
                    {"A":"a4","B":"b4","C":"c4","D":"d4","E":"e4"},
                    {"A":"a5","B":"b5","C":"c5","D":"d5","E":"e5"}
                ],
                "Sheet2": [
                    {"A":"a1","B":"b1","C":"c1","D":"d1","E":"e1"},
                    {"A":"a2","B":"b2","C":"c2","D":"d2","E":"e2"},
                    {"A":"a3","B":"b3","C":"c3","D":"d3","E":"e3"},
                    {"A":"a4","B":"b4","C":"c4","D":"d4","E":"e4"},
                    {"A":"a5","B":"b5","C":"c5","D":"d5","E":"e5"}
                ],
                "Sheet3": [
                    {"A":"a1","B":"b1","C":"c1","D":"d1","E":"e1"},
                    {"A":"a2","B":"b2","C":"c2","D":"d2","E":"e2"},
                    {"A":"a3","B":"b3","C":"c3","D":"d3","E":"e3"},
                    {"A":"a4","B":"b4","C":"c4","D":"d4","E":"e4"},
                    {"A":"a5","B":"b5","C":"c5","D":"d5","E":"e5"}
                ]
            }""";

    @Test
    public void read_xlsx_simple() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(TestUtils.resourceAsFile("simple.xlsx"), MediaTypes.APPLICATION_EXCEL));

        JSONAssert.assertEquals(simple_xlsx_json, doc.getContent(), true);
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_xls_simple() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(TestUtils.resourceAsFile("simple.xls"), MediaTypes.APPLICATION_OOXML_SPREADSHEET_SHEET));

        JSONAssert.assertEquals(simple_xlsx_json, doc.getContent(), true);
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    public final String simple_empty_cells = """
            {
                "Sheet1":[
                    {"B":"b2","C":"c2","D":"d2","E":"e2"},
                    {"B":"b3","C":"c3","D":"d3","E":"e3"},
                    {"B":"b4","C":"c4","D":"d4","E":"e4"},
                    {"B":"b5","C":"c5","D":"d5","E":"e5"}
                ],
                "Sheet2":[
                    {"A":"a1","C":"c1","D":"d1","E":"e1"},
                    {"A":"a3","C":"c3","D":"d3","E":"e3"},
                    {"A":"a4","C":"c4","D":"d4","E":"e4"},
                    {"A":"a5","C":"c5","D":"d5","E":"e5"}
                ],
                "Sheet3":[
                        {"A":"a1","B":"b1","D":"d1","E":"e1"},
                        {"A":"a2","B":"b2","D":"d2","E":"e2"},
                        {"A":"a4","B":"b4","D":"d4","E":"e4"},
                        {"A":"a5","B":"b5","D":"d5","E":"e5"}
                ]
            }
            """;

    @Test
    public void read_xlsx_simple_empty_cells() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(TestUtils.resourceAsFile("simple-empty-cells.xlsx"), MediaTypes.APPLICATION_EXCEL));

        JSONAssert.assertEquals(simple_empty_cells, doc.getContent(), true);
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    public String dates_xlsx_json = """
            { "Sheet1": [
                {"A":123,"B":456,"C":789},
                {"A":123.456,"B":789.123,"C":456.789},
                {"A":"2023-03-29T00:00","B":"2023-03-30T00:00","C":"2023-03-31T00:00"}
            ]}""";

    @Test
    public void read_xlsx_dates() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(TestUtils.resourceAsFile("dates.xlsx"), MediaTypes.APPLICATION_EXCEL));

        JSONAssert.assertEquals(dates_xlsx_json, doc.getContent(), true);
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    public String formula_xlsx_json = """
            {"Sheet3":[{"A":"","B":"","C":"","D":"","E":""},{"A":"","B":60,"C":60,"D":3600,"E":""}]}""";

    @Test
    public void read_xlsx_formula() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(TestUtils.resourceAsFile("formula.xlsx"), MediaTypes.APPLICATION_EXCEL));

        JSONAssert.assertEquals(formula_xlsx_json, doc.getContent(), true);
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }
}
