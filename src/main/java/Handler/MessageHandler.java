package Handler;

import ConnectionService.Connection;
import Protocol.HttpRequest;

public interface MessageHandler<T> {
    public void onMessage(Connection connection, T t);
    public void sendMessage(Connection connection, Object response); //修改：发送的响应可以与请求类型不同
}
