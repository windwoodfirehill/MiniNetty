package Handler;

import ConnectionService.Connection;
import Protocol.HttpRequest;

public interface MessageHandler<T> {
    public void onMessage(Connection connection, T t);
    public void sendMessage(Connection connection,T t);
}
