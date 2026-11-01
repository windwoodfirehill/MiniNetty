package Core;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey; //新增：读写方法需要知道处理哪个连接

abstract class EventLoop {

    //循环
    abstract void loop() throws IOException;
    //注册channel
    abstract void register(Selector selector) throws IOException;
    //可读
    //原本：abstract void read();
    abstract void read(SelectionKey key) throws IOException; //修改：与 WorkerLoop 保持一致
    //可写
    //原本：abstract void write();
    abstract void write(SelectionKey key) throws IOException; //修改：与 WorkerLoop 保持一致
}
