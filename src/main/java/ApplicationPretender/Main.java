package ApplicationPretender;

import Handler.HttpHandler;
import MiniNettyApplication.MiniNettyBootStrap;
import Protocol.HttpProtocol;

public class Main {

    public static void main(String[] args) {
        int port = 8888;
        String host = "127.0.0.1";
        MiniNettyBootStrap miniNettyBootStrap = new MiniNettyBootStrap(port,host,new HttpProtocol());
        miniNettyBootStrap.setHandler("http",new HttpHandler());
    }
}
