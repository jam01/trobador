package com.github.jam01.xtrasonnet.plugins;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.github.jam01.xtrasonnet.document.DefaultDocument;
import com.github.jam01.xtrasonnet.document.Document;
import com.github.jam01.xtrasonnet.document.MediaType;
import com.github.jam01.xtrasonnet.document.MediaTypes;
import com.github.jam01.xtrasonnet.spi.AbstractDataFormatPlugin;
import com.github.jam01.xtrasonnet.spi.PluginException;
import com.github.jam01.xtrasonnet.spi.ujsonUtils;
import ujson.Value;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;

public class DefaultJSONFormatPlugin extends AbstractDataFormatPlugin {
    public DefaultJSONFormatPlugin() {
        supportedTypes.add(MediaTypes.APPLICATION_JSON);
        supportedTypes.add(new MediaType("application", "*+json"));

        writerParams.add(DS_PARAM_INDENT);

        readerSupportedClasses.add(java.lang.String.class);
        readerSupportedClasses.add(java.lang.CharSequence.class);
        readerSupportedClasses.add(java.nio.file.Path.class);
        readerSupportedClasses.add(java.io.File.class);
        readerSupportedClasses.add(java.nio.ByteBuffer.class);
        readerSupportedClasses.add(byte[].class);

        writerSupportedClasses.add(java.lang.String.class);
        writerSupportedClasses.add(java.lang.CharSequence.class);
        writerSupportedClasses.add(java.nio.ByteBuffer.class);
        writerSupportedClasses.add(java.io.OutputStream.class);
        writerSupportedClasses.add(byte[].class);
    }

    @Override
    public Value read(Document<?> doc) throws PluginException {
        if (doc.getContent() == null) {
            return ujson.Null$.MODULE$;
        }

        Class<?> targetType = doc.getContent().getClass();

        if (String.class.isAssignableFrom(targetType)) {
            return ujsonUtils.read(ujson.Readable.fromString((String) doc.getContent()), false);
        }

        if (CharSequence.class.isAssignableFrom(targetType)) {
            return ujsonUtils.read(ujson.Readable.fromCharSequence((CharSequence) doc.getContent()), false);
        }

        if (Path.class.isAssignableFrom(targetType)) {
            return ujsonUtils.read((ujson.Readable) ujson.Readable.fromPath((Path) doc.getContent()), false);
        }

        if (File.class.isAssignableFrom(targetType)) {
            return ujsonUtils.read((ujson.Readable) ujson.Readable.fromFile((File) doc.getContent()), false);
        }

        if (ByteBuffer.class.isAssignableFrom(targetType)) {
            return ujsonUtils.read(ujson.Readable.fromByteBuffer((ByteBuffer) doc.getContent()), false);
        }

        if (byte[].class.isAssignableFrom(targetType)) {
            return ujsonUtils.read(ujson.Readable.fromByteArray((byte[]) doc.getContent()), false);
        }

        throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        Charset charset = mediaType.getCharset();
        if (charset == null) {
            charset = Charset.defaultCharset();
        }

        int indent = mediaType.getParameters().containsKey(DS_PARAM_INDENT) ? 4 : -1;

        if (targetType.isAssignableFrom(String.class)) {
            return new DefaultDocument<>((T) ujsonUtils.write(input, indent, false), MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(CharSequence.class)) {
            return new DefaultDocument<>((T) ujsonUtils.write(input, indent, false), MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(OutputStream.class)) {
            BufferedOutputStream out = new BufferedOutputStream(new ByteArrayOutputStream());
            ujsonUtils.writeTo(input, new OutputStreamWriter(out, charset), indent, false);

            return new DefaultDocument<>((T) out, MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(ByteBuffer.class)) {
            return new DefaultDocument<>((T) ByteBuffer.wrap(ujsonUtils.write(input, indent, false).getBytes(charset)), MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(byte[].class)) {
            return new DefaultDocument<>((T) ujsonUtils.write(input, indent, false).getBytes(charset), MediaTypes.APPLICATION_JSON);
        }

        throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"));
    }
}