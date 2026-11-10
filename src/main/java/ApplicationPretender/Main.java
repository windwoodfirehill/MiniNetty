package ApplicationPretender;

import Handler.HttpHandler;
import MiniNettyApplication.MiniNettyBootStrap;
import ProtocolParser.HttpProtocolParser;
import java.io.IOException; //新增


public class Main {

//    public static void main(String[] args) {
//        int port = 8888;
//        String host = "127.0.0.1";
//        MiniNettyBootStrap miniNettyBootStrap = new MiniNettyBootStrap(port,host,new HttpProtocolParser());
//        miniNettyBootStrap.setHandler("http",new HttpHandler());
//    }

    public static void main(String[] args) throws IOException { //修改：启动可能失败
        int port = 8888;
        String host = "127.0.0.1";
        MiniNettyBootStrap miniNettyBootStrap = new MiniNettyBootStrap(port,host,new HttpProtocolParser());
        miniNettyBootStrap.setHandler("http",new FrameWorkMessageHandler());
        miniNettyBootStrap.MiniteyStart(); //新增：实际启动服务
        System.out.println("system start");
    }
}
