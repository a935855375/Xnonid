package server.mvc;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Document;
import server.libs.Json;
import server.libs.XML;
import server.libs.j.Files;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Defines HTTP standard objects.
 */
public class Http {
    /**
     * The request body.
     */
    public static final class RequestBody {

        private final Object body;

        public RequestBody(Object body) {
            this.body = body;
        }

        /**
         * The request content parsed as multipart form data.
         *
         * @param <A> the file type (e.g. play.api.libs.Files.TemporaryFile)
         * @return the content parsed as multipart form data
         */
        public <A> MultipartFormData asMultipartFormData() {
            return as(MultipartFormData.class);
        }

        /**
         * The request content parsed as URL form-encoded.
         *
         * @return the request content parsed as URL form-encoded.
         */
        public Map<String, String[]> asFormUrlEncoded() {
            // Best effort, check if it's a map, then check if the first element in that map is String ->
            // String[].
            if (body instanceof Map) {
                if (((Map) body).isEmpty()) {
                    return Collections.emptyMap();
                } else {
                    Map.Entry<Object, Object> first;
                    first = ((Map<Object, Object>) body).entrySet().iterator().next();
                    if (first.getKey() instanceof String && first.getValue() instanceof String[]) {
                        return (Map<String, String[]>) body;
                    }
                }
            }
            return null;
        }

        /**
         * The request content as Array bytes.
         *
         * @return The request content as Array bytes.
         */
        public RawBuffer asRaw() {
            return as(RawBuffer.class);
        }

        /**
         * The request content as text.
         *
         * @return The request content as text.
         */
        public String asText() {
            return as(String.class);
        }

        /**
         * The request content as XML.
         *
         * @return The request content as XML.
         */
        public Document asXml() {
            return as(Document.class);
        }

        /**
         * The request content as Json.
         *
         * @return The request content as Json.
         */
        public JsonNode asJson() {
            return as(JsonNode.class);
        }

        /**
         * Converts a JSON request to a given class. Conversion is performed with
         * [[Json.fromJson(JsonNode,Class)]].
         *
         * <p>Will return Optional.empty() if the request body is not an instance of JsonNode. If the
         * JsonNode simply has missing fields, a valid reference with null fields is returne.
         *
         * @param <A>   The type to convert the JSON value to.
         * @param clazz The class to convert the JSON value to.
         * @return The converted value if the request has a JSON body or an empty value if the request
         * has an empty body or a body of a different type.
         */
        public <A> Optional<A> parseJson(Class<A> clazz) {
            return (body instanceof JsonNode)
                    ? Optional.of(Json.fromJson(asJson(), clazz))
                    : Optional.empty();
        }

        /**
         * The request content as a ByteString.
         *
         * <p>This makes a best effort attempt to convert the parsed body to a ByteString, if it knows
         * how. This includes String, json, XML and form bodies. It doesn't include multipart/form-data
         * or raw bodies that don't fit in the configured max memory buffer, nor does it include custom
         * output types from custom body parsers.
         *
         * @return the request content as a ByteString
         */
        public ByteString asBytes() {
            if (body == null) {
                return ByteString.emptyByteString();
            } else if (body instanceof Optional) {
                if (!((Optional<?>) body).isPresent()) {
                    return ByteString.emptyByteString();
                }
            } else if (body instanceof ByteString) {
                return (ByteString) body;
            } else if (body instanceof byte[]) {
                return ByteString.fromArray((byte[]) body);
            } else if (body instanceof String) {
                return ByteString.fromString((String) body);
            } else if (body instanceof RawBuffer) {
                return ((RawBuffer) body).asBytes();
            } else if (body instanceof JsonNode) {
                return ByteString.fromString(Json.stringify((JsonNode) body));
            } else if (body instanceof Document) {
                return XML.toBytes((Document) body);
            } else {
                Map<String, String[]> form = asFormUrlEncoded();
                if (form != null) {
                    return ByteString.fromString(
                            form.entrySet().stream()
                                    .flatMap(
                                            entry -> {
                                                String key = encode(entry.getKey());
                                                return Arrays.stream(entry.getValue())
                                                        .map(value -> key + "=" + encode(value));
                                            })
                                    .collect(Collectors.joining("&")));
                }
            }
            return null;
        }

        private String encode(String value) {
            try {
                return URLEncoder.encode(value, "utf8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Cast this RequestBody as T if possible.
         *
         * @param tType class that we are trying to cast the body as
         * @param <T>   type of the provided <code>tType</code>
         * @return either a successful cast into T or null
         */
        public <T> T as(Class<T> tType) {
            if (tType.isInstance(body)) {
                return tType.cast(body);
            } else {
                return null;
            }
        }

        public String toString() {
            return "RequestBody of " + (body == null ? "null" : body.getClass());
        }
    }

    /**
     * Multipart form data body.
     */
    public abstract static class MultipartFormData<A> {

        /**
         * Info about a file part
         */
        public static class FileInfo {
            private final String key;
            private final String filename;
            private final String contentType;

            public FileInfo(String key, String filename, String contentType) {
                this.key = key;
                this.filename = filename;
                this.contentType = contentType;
            }

            public String getKey() {
                return key;
            }

            public String getFilename() {
                return filename;
            }

            public String getContentType() {
                return contentType;
            }
        }

        public interface Part<A> {
        }

        /**
         * A file part.
         */
        public static class FilePart<A> implements Part<A> {

            final String key;
            final String filename;
            final String contentType;
            final A ref;
            final String dispositionType;
            final long fileSize;

            public FilePart(String key, String filename, String contentType, A ref) {
                this(key, filename, contentType, ref, -1);
            }

            public FilePart(String key, String filename, String contentType, A ref, long fileSize) {
                this(key, filename, contentType, ref, fileSize, "form-data");
            }

            public FilePart(
                    String key,
                    String filename,
                    String contentType,
                    A ref,
                    long fileSize,
                    String dispositionType) {
                this.key = key;
                this.filename = filename;
                this.contentType = contentType;
                this.ref = ref;
                this.dispositionType = dispositionType;
                this.fileSize = fileSize;
            }

            /**
             * The part name.
             *
             * @return the part name
             */
            public String getKey() {
                return key;
            }

            /**
             * The file name.
             *
             * @return the file name
             */
            public String getFilename() {
                return filename;
            }

            /**
             * The file Content-Type
             *
             * @return the content type
             */
            public String getContentType() {
                return contentType;
            }

            /**
             * The File.
             *
             * @return the file
             * @deprecated Deprecated as of 2.7.0. Use {@link #getRef()} instead, which however (when
             * using the default Play {@code BodyParser}) will give you a {@link
             * server.libs.Files.TemporaryFile} instance instead of a {@link java.io.File} one. <a
             * href="https://www.playframework.com/documentation/latest/Migration27#Javas-FilePart-exposes-the-TemporaryFile-for-uploaded-files">See
             * migration guide.</a>
             */
            @Deprecated
            public A getFile() {
                if (ref instanceof Files.TemporaryFile) {
                    // For backwards compatibility
                    return (A) ((Files.TemporaryFile) ref).path().toFile();
                }
                return ref;
            }

            /**
             * The File.
             *
             * @return the file
             */
            public A getRef() {
                return ref;
            }

            /**
             * The disposition type.
             *
             * @return the disposition type
             */
            public String getDispositionType() {
                return dispositionType;
            }

            /**
             * The size of the file in bytes.
             *
             * @return the size of the file in bytes
             */
            public long getFileSize() {
                return fileSize;
            }
        }

        public static class DataPart implements Part<Source<ByteString, ?>> {
            private final String key;
            private final String value;

            public DataPart(String key, String value) {
                this.key = key;
                this.value = value;
            }

            /**
             * The part name.
             *
             * @return the part name
             */
            public String getKey() {
                return key;
            }

            /**
             * The part value.
             *
             * @return the part value
             */
            public String getValue() {
                return value;
            }
        }

        /**
         * Extract the data parts as Form url encoded.
         *
         * @return the data that was URL encoded
         */
        public abstract Map<String, String[]> asFormUrlEncoded();

        /**
         * Retrieves all file parts.
         *
         * @return the file parts
         */
        public abstract List<FilePart<A>> getFiles();

        /**
         * Access a file part.
         *
         * @param key name of the file part to access
         * @return the file part specified by key
         */
        public FilePart<A> getFile(String key) {
            for (FilePart<A> filePart : getFiles()) {
                if (filePart.getKey().equals(key)) {
                    return filePart;
                }
            }
            return null;
        }
    }

    /**
     * Handle the request body a raw bytes data.
     */
    public abstract static class RawBuffer {

        /**
         * Buffer size.
         *
         * @return the buffer size
         */
        public abstract Long size();

        /**
         * Returns the buffer content as a bytes array.
         *
         * @param maxLength The max length allowed to be stored in memory
         * @return null if the content is too big to fit in memory
         */
        public abstract ByteString asBytes(int maxLength);

        /**
         * Returns the buffer content as a bytes array
         *
         * @return the bytes
         */
        public abstract ByteString asBytes();

        /**
         * Returns the buffer content as File
         *
         * @return the file
         */
        public abstract File asFile();
    }
}
