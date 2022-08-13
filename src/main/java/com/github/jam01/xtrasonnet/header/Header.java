package com.github.jam01.xtrasonnet.header;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/* datasonnet-mapper copyright/notice, per Apache-2.0 § 4.c */
/*-
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*-
 * Adopted:
 * - 0b946df9667c64ac631934a77d2444700889accf: fix system-dependent header parsing; allow any standard line separators
 * - f3e2584ce6d2bf68f49d762503b876255cb29ac4: convert opaque array index exceptions to descriptive exceptions and i...
 *  mprove locating of invalid media format problems in commit
 * - d94c7e337e77a358dd3f8944b81b2425c0235d43: improve resilience of header parsing, including unknown header lines...
 *  and unterminated headers
 * - 695eba21d86ca7ab9b1812ce7689af41db4c83a4: change so the new header format is opt-in with new DataSonnet (the la...
 *  nguage) version, re-add support for old headers with old version
 * - 6f5fb26e1c5550f1074748a7e34a8e7dc99dc788: adjust default functionality to align with multiple output and named...
 *  input formats
 * - eade46e7493bf1f2e2fcbe5269890b8cf7969a96: Merge pull request #69 from datasonnet/minor-header-versions
 * - ad59b61447ad423a66c2e51b3ec79914aaed28bb: Merge pull request #67 from datasonnet/header-comments
 *
 * Changed:
 * - fd1273fc9745dece77ec44a314c52f82f1e23b62: Merge pull request #76 from datasonnet/qparam
 * - 2abc04cd80fa82d1dae7805964e9165f13ea6889: remove header 1.0 support
 */

import com.github.jam01.xtrasonnet.document.Document;
import com.github.jam01.xtrasonnet.document.InvalidMediaTypeException;
import com.github.jam01.xtrasonnet.document.MediaType;
import com.github.jam01.xtrasonnet.document.MediaTypes;
import com.github.jam01.xtrasonnet.document.Document;
import com.github.jam01.xtrasonnet.document.InvalidMediaTypeException;
import com.github.jam01.xtrasonnet.document.MediaType;
import com.github.jam01.xtrasonnet.document.MediaTypes;
import com.github.jam01.xtrasonnet.document.Document;
import com.github.jam01.xtrasonnet.document.InvalidMediaTypeException;
import com.github.jam01.xtrasonnet.document.MediaType;
import com.github.jam01.xtrasonnet.document.MediaTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Header {
    public static final String DATASONNET_HEADER = "/** DataSonnet";
    public static final String COMMENT_PREFIX = "//";
    public static final Pattern VERSION_LINE = Pattern.compile("^version *= *(?<version>[a-zA-Z0-9.+-]+) *(\\r?\\n|$)");
    public static final String DATASONNET_INPUT = "input";
    public static final Pattern INPUT_LINE = Pattern.compile("^(?:input (?<name>\\w+)|input (?<all>\\*)) (?<mediatype>\\S.*)$");
    public static final String DATASONNET_OUTPUT = "output";
    public static final Pattern OUTPUT_LINE = Pattern.compile("^output (?<mediatype>\\S.*)$");
    public static final String DATASONNET_PRESERVE_ORDER = "preserveOrder";
    public static final String DATAFORMAT_PREFIX = "dataformat";
    public static final String LATEST_RELEASE_VERSION = "2.0"; //update this as required
    private final String versionMajor;
    private final String versionMinor;
    private final boolean preserveOrder;
    private final Map<String, Map<Integer, MediaType>> namedInputs;
    private final Map<Integer, MediaType> outputs;
    // using maps to facilitate only one per super/sub type
    private final Map<Integer, MediaType> allInputs;
    private final Map<Integer, MediaType> dataFormats;
    private final HashMap<String, MediaType> defaultInputs;
    private final MediaType defaultOutput;

    public Header(String version,
                  boolean preserveOrder,
                  Map<String, Collection<MediaType>> namedInputs,
                  List<MediaType> outputs,
                  Iterable<MediaType> allInputs,
                  Iterable<MediaType> dataFormats) {
        String[] versions = version.split("\\.",2); //[0] = major [1] = minor + remainder if exists
        this.versionMajor = versions[0];
        this.versionMinor = versions[1];
        this.preserveOrder = preserveOrder;
        this.defaultInputs = new HashMap<>();
        this.namedInputs = new HashMap<>();

        for (Map.Entry<String, Collection<MediaType>> entry : namedInputs.entrySet()) {
            Collection<MediaType> types = entry.getValue();
            if (types.size() > 0) {
                Map<Integer, MediaType> indexed = indexMediaTypes(types);
                this.namedInputs.put(entry.getKey(), indexed);

                List<MediaType> sorted = new ArrayList<>(indexed.values());
                MediaType.sortByQualityValue(sorted);
                this.defaultInputs.put(entry.getKey(), sorted.get(0));
            }
        }

        this.outputs = indexMediaTypes(outputs);
        if (outputs.size() > 0) {
            List<MediaType> sorted = new ArrayList<>(outputs);
            MediaType.sortByQualityValue(sorted);
            this.defaultOutput = sorted.get(0);
        } else {
            this.defaultOutput = MediaTypes.ANY;
        }

        this.allInputs = indexMediaTypes(allInputs);
        this.dataFormats = indexMediaTypes(dataFormats);
    }

    private Map<Integer, MediaType> indexMediaTypes(Iterable<MediaType> mediaTypes) {
        Map<Integer, MediaType> indexed = new HashMap<>();
        for(MediaType mediaType : mediaTypes) {
            indexed.put(calculateIndex(mediaType), mediaType);
        }
        return indexed;
    }

    private Integer calculateIndex(MediaType mediaType) {
        return mediaType.getType().hashCode() + mediaType.getSubtype().hashCode();
    }

    private static final Header EMPTY =
            new Header(LATEST_RELEASE_VERSION, true, Collections.emptyMap(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

    public static Header parseHeader(String script) throws HeaderParseException {
        if (!script.trim().startsWith(DATASONNET_HEADER)) {
            return EMPTY;
        }

        String headerSection = extractHeader(script);

        Matcher versionMatcher = VERSION_LINE.matcher(headerSection);
        if(!versionMatcher.find()) {
            throw new HeaderParseException("The first line of the header must be a version line, but is not");
        }

        String version = versionMatcher.group("version");
        String headerWithoutVersion = headerSection.substring(versionMatcher.end());
        String[] splitVersion = version.split("\\.",2); //[0] = major [1] = minor + remainder if exists

        switch (splitVersion[0]) {
            case "2":
                if ("0".equals(splitVersion[1])) {
                    return parseHeader20(headerWithoutVersion);
                } else {
                    //not sure if a print out is a good enough warning or if we want to add loggers
                    System.err.println("WARNING: You are using a version that is still in development. " +
                                       "The latest release version is: " + LATEST_RELEASE_VERSION);
                    return parseHeader20(headerWithoutVersion, version);
                }
            default:
                throw new HeaderParseException("Major version must be one of [2] but is " + splitVersion[0]);
        }
    }

    @NotNull
    private static String extractHeader(String script) throws HeaderParseException {
        int terminus = script.indexOf("*/");
        if(terminus == -1) {
            throw new HeaderParseException("Unterminated header. Headers must end with */");
        }

        return script
                .substring(0, terminus)
                .replace(DATASONNET_HEADER, "")
                .trim();
    }

    @NotNull
    private static Header parseHeader20(String headerSection, String version) throws HeaderParseException {
        boolean preserve = true;
        List<MediaType> outputs = new ArrayList<>(4);
        Map<String, List<MediaType>> inputs = new HashMap<>(4);
        List<MediaType> allInputs = new ArrayList<>(4);
        List<MediaType> dataformat = new ArrayList<>(4);

        for (String line : headerSection.split("\\r?\\n")) {
            line = line.trim();  // we never care about leading or trailing whitespace
            try {
                if (line.startsWith(DATASONNET_PRESERVE_ORDER)) {
                    String[] tokens = line.split("=", 2);
                    preserve = Boolean.parseBoolean(tokens[1]);
                } else if (line.startsWith(DATASONNET_INPUT)) {
                    Matcher matcher = INPUT_LINE.matcher(line);
                    if(!matcher.matches()) {
                        throw new HeaderParseException("Unable to parse header line " + line + ", it must follow the input line format");
                    }
                    String name = matcher.group("name");
                    MediaType mediaType = MediaType.valueOf(matcher.group("mediatype"));

                    if (matcher.group("all") != null) {  // there's a *. This also means it can't be a default.
                        allInputs.add(mediaType);
                    } else {
                        if(!inputs.containsKey(name)) {
                            inputs.put(name, new ArrayList<>());
                        }
                        inputs.get(name).add(mediaType);
                    }
                } else if (line.startsWith(DATASONNET_OUTPUT)) {
                    Matcher matcher = OUTPUT_LINE.matcher(line);

                    if (!matcher.matches()) {
                        throw new HeaderParseException("Unable to parse header line " + line + ", it must follow the output line format");
                    }

                    MediaType mediaType = MediaType.valueOf(matcher.group("mediatype"));
                    outputs.add(mediaType);

                } else if (line.startsWith(DATAFORMAT_PREFIX)) {
                    String[] tokens = line.split(" ", 2);
                    MediaType toAdd = MediaType.valueOf(tokens[1]);
                    dataformat.add(toAdd);
                } else if (line.isEmpty() || line.startsWith(COMMENT_PREFIX)) {
                    // deliberately do nothing
                } else {
                    throw new HeaderParseException("Unable to parse header line: " + line);
                }
            } catch (InvalidMediaTypeException exc) {
                throw new HeaderParseException("Could not parse media type from header in line " + line, exc);
            } catch(ArrayIndexOutOfBoundsException exc) {
                throw new HeaderParseException("Problem with header formatting in line " + line);
            }
        }

        return new Header(version, preserve, Collections.unmodifiableMap(inputs), outputs, allInputs, dataformat);
    }

    @NotNull
    private static Header parseHeader20(String headerSection) throws HeaderParseException{
        return parseHeader20(headerSection, "2.0");
    }

    public String getVersion() {
        return versionMajor + "." + versionMinor;
    }

    public String getVersionMajor(){
        return versionMajor;
    }

    public String getVersionMinor(){
        return versionMinor;
    }

    public Map<String, Iterable<MediaType>> getNamedInputs() {
        Map<String, Iterable<MediaType>> namedInputs = new HashMap<>(this.namedInputs.size());
        for(Map.Entry<String, Map<Integer, MediaType>> entry : this.namedInputs.entrySet()) {
            namedInputs.put(entry.getKey(), Collections.unmodifiableCollection(entry.getValue().values()));
        }
        return Collections.unmodifiableMap(namedInputs);
    }

    public Optional<MediaType> getDefaultNamedInput(String name) {
        return Optional.ofNullable(defaultInputs.get(name));
    }

    public Collection<MediaType> getOutputs() {
        return Collections.unmodifiableCollection(outputs.values());
    }

    public Optional<MediaType> getDefaultOutput() {
        return Optional.ofNullable(defaultOutput);
    }

    public Optional<MediaType> getDefaultPayload() {
        return Optional.ofNullable(defaultInputs.get("payload"));
    }

    public Collection<MediaType> getAllInputs() {
        return Collections.unmodifiableCollection(allInputs.values());
    }

    public Collection<MediaType> getDataFormats() {
        return Collections.unmodifiableCollection(dataFormats.values());
    }

    public boolean isPreserveOrder() {
        return preserveOrder;
    }

    public <T> Document<T> combineInputParams(String inputName, Document<T> doc) {
        Map<String, String> params = new HashMap<>(4);
        MediaType mediaType = doc.getMediaType();
        Integer key = calculateIndex(mediaType);

        if (dataFormats.containsKey(key)) {
            params.putAll(dataFormats.get(key).getParameters());
        }

        if (allInputs.containsKey(key)) {
            params.putAll(allInputs.get(key).getParameters());
        }

        if (namedInputs.containsKey(inputName)) {
            Map<Integer, MediaType> inputTypes = namedInputs.get(inputName);
            if (inputTypes != null && inputTypes.containsKey(key)) {
                params.putAll(inputTypes.get(key).getParameters());
            }
        }

        // hmmm, I see why this is here, but it feels tricky. Some parameters control
        // what inputs look like after parsing; overriding those with things from the input is bad.
        // other parameters control how the content is parsed; overriding those with things from the input is good.
        // since the former are (deservedly) rarer, leaving this here is probably best, but we may want
        // something more sophisticated later.
        params.putAll(mediaType.getParameters());

        return doc.withMediaType(new MediaType(mediaType, params));
    }

    public MediaType combineOutputParams(MediaType mediaType) {
        Map<String, String> params = new HashMap<>(4);
        Integer key = calculateIndex(mediaType);

        if (dataFormats.containsKey(key)) {
            params.putAll(dataFormats.get(key).getParameters());
        }

        if (outputs.containsKey(key)) {
            params.putAll(outputs.get(key).getParameters());
        }

        params.putAll(mediaType.getParameters());

        return new MediaType(mediaType, params);
    }
}