import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final ExecutorService executorService;
    private final Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();
    private final Handler notFoundHandler = ((request, out) -> {
        try {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();


        } catch (IOException e) {
//            fixme

        }


    });

    public Server(int poolSize) {
        this.executorService = Executors.newFixedThreadPool(poolSize);
    }


    public void addHandler(String method, String path, Handler handler) {
        if (handlers.get(method) == null) {
            handlers.put(method, new ConcurrentHashMap<>());
        }
        handlers.get(method).put(path, handler);
    }

    public void listen(int port) {


        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {

                final var socket = serverSocket.accept();
                executorService.submit(() -> {
                    try {
                        processConnection(socket);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                });


            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processConnection(Socket socket) throws URISyntaxException {
        try (
                socket;
                final var in = socket.getInputStream();
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            var request = Request.fromInputStream(in, out);
            var handlerMap = handlers.get(request.getMethod());
            if (handlerMap == null) {
                notFoundHandler.handle(request, out);
                return;
            }
            var handler = handlerMap.get(request.getPath());
            if (handler == null) {
                notFoundHandler.handle(request, out);
                return;
            }
            handler.handle(request, out);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}