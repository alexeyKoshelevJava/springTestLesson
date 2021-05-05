import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;

public class Request {
    private final String method;
    private final String path;
    private final List<String> headers;
    private final InputStream in;
    private final Map<String, String> queryParam;
    public static final String GET = "GET";
    public static final String POST = "POST";


    private Request(String method, String path, List<String> headers, Map<String, String> queryParam, InputStream in) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.queryParam = queryParam;
        this.in = in;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public InputStream getIn() {
        return in;
    }

    public Map<String, String> getQueryParam() {
        return queryParam;
    }

    public static Request fromInputStream(InputStream inputStream, BufferedOutputStream out) throws IOException, URISyntaxException {
        final var allowedMethods = List.of(GET, POST);
        final var in = new BufferedInputStream(inputStream);
        final var limit = 4096;

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            badRequest(out);
            return null;

        }

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            badRequest(out);
            return null;
        }

        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            badRequest(out);
            return null;
        }
        System.out.println(method);


        final var pathWithQuery = requestLine[1];
        if (!pathWithQuery.startsWith("/")) {
            badRequest(out);
            return null;
        }
        System.out.println(pathWithQuery);
        final String path;
        final Map<String, String> query;

        if (pathWithQuery.contains("?")) {

//        поучаем query
            String[] value = pathWithQuery.split("\\?");
            path = value[0];
            String queryLine = value[1];
            query = parseURItoQuery(queryLine);

        } else {
            path = pathWithQuery;
            query = null;
        }
        System.out.println(path);

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            badRequest(out);
            return null;
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        System.out.println(headers);

        // для GET тела нет
        if (!method.equals(GET)) {
            in.skip(headersDelimiter.length);
            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);

                final var body = new String(bodyBytes);
                System.out.println(body);
            }
        }


        return new Request(method, path, headers, query, inputStream);


    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }


    public static Map<String, String> parseURItoQuery(String uri) throws URISyntaxException {
        if (!uri.startsWith("?")) {
            uri = "?".concat(uri);
        }
        HashMap<String, String> map = new HashMap<>();
        List<NameValuePair> parse = URLEncodedUtils.parse(new URI(uri), Charset.defaultCharset());
        for (NameValuePair nameValuePair : parse) {
            map.put(nameValuePair.getName(), nameValuePair.getValue());
        }
        return map;


    }
}