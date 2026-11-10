package Handler;

import ConnectionService.Connection;
import Protocol.Protocol;

public interface MessageHandler<T> {
    public void onMessage(Connection connection, T t);
    public void sendMessage(Connection connection, Protocol response);
}
