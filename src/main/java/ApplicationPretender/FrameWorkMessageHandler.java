package ApplicationPretender;

import ConnectionService.Connection;
import Handler.HttpHandler;
import Handler.MessageHandler;
import Protocol.HttpRequest;
import Protocol.HttpResponse;
import Protocol.Protocol;
import java.nio.charset.StandardCharsets; //新增
import java.util.List; //新增

public class FrameWorkMessageHandler implements MessageHandler<HttpRequest>{


    @Override
    public void onMessage(Connection connection, HttpRequest httpRequest) {
            System.out.println(httpRequest.getMethod() +" " +new String(httpRequest.getBody(), StandardCharsets.UTF_8)); //修改：明确字符编码
            String body = "MiniNetty received: " + httpRequest.getMethod() + " "
                    + httpRequest.getTarget() + "\n"
                    + new String(httpRequest.getBody(), StandardCharsets.UTF_8); //新增：测试响应正文
            HttpResponse httpResponse = new HttpResponse(httpRequest.getVersion(), 200, "OK",
                    body.getBytes(StandardCharsets.UTF_8)); //修改：补齐状态行和正文
            httpResponse.getMap().put("Content-Type", List.of("text/plain; charset=UTF-8")); //新增
            sendMessage(connection, httpResponse); //新增：把响应交给当前连接
    }

    @Override
    public void sendMessage(Connection connection, Protocol response) { //修改：接收响应
        if (!connection.offerAndWeakUp(response)) { //修改：入队响应，并报告队满
            System.err.println("响应发送队列已满，入队失败");
        }
    }
}
