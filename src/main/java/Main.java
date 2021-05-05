import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(64);
        server.addHandler("GET","/classic.html",((request, out) -> {
            try {
                final var filePath = Path.of(".", "public", request.getPath());
                final var mimeType = Files.probeContentType(filePath);
                System.out.println(request.getPath());
                if (request.getQueryParam()!= null){
                    request.getQueryParam().forEach((key, value) ->System.out.println(key+":"+value));
                }


                // special case for classic


                    final var template = Files.readString(filePath);
                    final var content = template.replace(
                            "{time}",
                            LocalDateTime.now().toString()
                    ).getBytes();
                    out.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + content.length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.write(content);
                    out.flush();


            }catch (Exception exception){
//                fixme

            }
        }
                ));
        server.listen(9999);
    }
}
