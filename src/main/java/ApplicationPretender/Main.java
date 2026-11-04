package ApplicationPretender;

import Handler.HttpHandler;
import MiniNettyApplication.MiniNettyBootStrap;
import Protocol.HttpProtocol;

import java.nio.charset.StandardCharsets;

public class Main {

//    public static void main(String[] args) {
//        int port = 8888;
//        String host = "127.0.0.1";
//        MiniNettyBootStrap miniNettyBootStrap = new MiniNettyBootStrap(port,host,new HttpProtocol());
//        miniNettyBootStrap.setHandler("http",new HttpHandler());
//    }
public static void main(String[] args) {

    HttpProtocol httpProtocol = new HttpProtocol();
    String post = "POST /api/login HTTP/1.1\r\n"
            + "Host: www.example.com\r\n"
            + "Content-Type: application/json\r\n"
            + "Content-Length: 43\r\n"
            + "\r\n"
            + "{\"username\":\"zhangsan\",\"password\":\"123456\"}";
    httpProtocol.decoder(post.getBytes(StandardCharsets.UTF_8));

    System.out.println("----");

    String get = "GET /search?q=java HTTP/1.1\r\n"
            + "Host: www.example.com\r\n"
            + "Accept: application/json\r\n"
            + "\r\n";
    httpProtocol.decoder(get.getBytes(StandardCharsets.UTF_8));
}
}
