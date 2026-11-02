package Core;

import java.io.IOException;
import java.nio.channels.Selector;

abstract class EventLoop {

    public final Selector selector; //新增：每个 EventLoop 持有自己的 Selector
    public volatile boolean stop = false; //新增：供不同线程安全地观察停止状态

    EventLoop() throws IOException {
        selector = Selector.open(); //新增：共享初始化逻辑
    }

    public void shutdown() { //新增：共享停止和唤醒逻辑
        stop = true;
        selector.wakeup();
    }

    //循环
    abstract void loop() throws IOException;
    //注册channel
    abstract void register(Selector selector) throws IOException;
    //修改：read/write 属于 Worker，不再要求 Boss 提供空实现。
}
