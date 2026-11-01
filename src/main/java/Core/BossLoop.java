package Core;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey; //新增：匹配父类方法参数
import java.nio.channels.SocketChannel;

public class BossLoop extends EventLoop{

    @Override
    void loop() {

    }

    @Override
    void register(Selector selector) throws IOException {

    }

    @Override
    //原本：void read()
    void read(SelectionKey key) { //修改：匹配父类声明，保留空实现

    }

    @Override
    //原本：void write()
    void write(SelectionKey key) { //修改：匹配父类声明，保留空实现

    }
}
