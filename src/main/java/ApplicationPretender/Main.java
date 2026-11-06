package ApplicationPretender;

import Protocol.HttpRequest;
import Protocol.HttpResponse;
import Protocol.HttpProtocol;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Main {

//    public static void main(String[] args) {
//        int port = 8888;
//        String host = "127.0.0.1";
//        MiniNettyBootStrap miniNettyBootStrap = new MiniNettyBootStrap(port,host,new HttpProtocol());
//        miniNettyBootStrap.setHandler("http",new HttpHandler());
//    }
    public static void main(String[] args) {
        HttpProtocol protocol = new HttpProtocol();
        String raw = "POST /echo HTTP/1.1\r\nHost: localhost\r\nContent-Length: 6\r\n\r\n你好";
        System.out.println("原始请求\n" + raw);

        HttpRequest request = protocol.decoder(raw.getBytes(UTF_8));
        System.out.println("\n请求解码 → " + request.getMethod() + " " + request.getTarget()
                + " " + request.getHeaders() + " body=" + new String(request.getBody(), UTF_8));

        HttpResponse reply = new HttpResponse(request.getVersion(), 200, "OK", request.getBody());
        byte[] encoded = protocol.encoder(reply);
        System.out.println("\n响应编码\n" + new String(encoded, UTF_8));

    }
}
