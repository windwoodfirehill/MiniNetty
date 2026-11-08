package ApplicationPretender;

import ConnectionService.Connection;
import Handler.HttpHandler;
import Handler.MessageHandler;
import Protocol.HttpRequest;
import Protocol.HttpResponse;

public class FrameWorkMessageHandler implements MessageHandler<HttpRequest>{


    @Override
    public void onMessage(Connection connection, HttpRequest httpRequest) {

    }

    @Override
    public void sendMessage(Connection connection, HttpRequest httpRequest) {

    }
}
