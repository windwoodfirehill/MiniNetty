package Handler;

import Protocol.HttpRequest;

public interface MessageHandler {
    public void onMessage(HttpRequest request);
    public void sendMessage();
}
