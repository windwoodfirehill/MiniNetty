package Core;

import java.io.IOException;
//修改：删除未使用的 import；原本：import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

public class WorkerLoop extends EventLoop{
    //原本：public boolean stop = false;
    public volatile boolean stop = false; //修改：保证其他线程修改后可见
    public static final int QUE_SIZE = 1;
    public final ArrayBlockingQueue<SocketChannel> queue;
    //原本：在 loop 中初始化 Selector，并吞掉初始化异常
    public final Selector selector; //修改：构造完成后即可供 Boss 调用 wakeup()
    public WorkerLoop() throws IOException { //修改：初始化失败向调用方抛出
        queue = new ArrayBlockingQueue<>(QUE_SIZE);
        selector = Selector.open(); //新增：每个 Worker 只创建一个 Selector
    }

    @Override
    void loop() throws IOException {
        while(!stop){
            //原本：if，只处理一个待注册连接
            while(!queue.isEmpty()){ //修改：处理完待注册连接再等待事件
                register(selector);
            }
            selector.select(); //修改：原本在创建迭代器后调用，现移到取集合之前
            if (stop) { //新增：被唤醒后检查停止状态
                break;
            }
            Set<SelectionKey> selectionKey = selector.selectedKeys();

            Iterator<SelectionKey> iterator = selectionKey.iterator();

            while(iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                //原本：if(key.isValid())，会跳过所有有效连接
                if(!key.isValid()){ //修改：只跳过失效连接
                    continue;
                }
                if (key.isReadable()) {
                    read(key);
                }
                if (key.isValid()&&key.isWritable()) {
                    write(key);
                }
            }
        }
    }

    @Override
    //注册连接
    void register(Selector selector) throws IOException {
        SocketChannel channel = getChannel(queue);

        if(channel == null){
            return;
        }

        try { //新增：单个连接注册失败不退出整个 Worker 循环
            channel.configureBlocking(false); //新增：注册前确保非阻塞
            ConnectionState state = new ConnectionState();
            channel.register(selector, SelectionKey.OP_READ,state);
        } catch (IOException ex) {
            ex.printStackTrace();
            try {
                channel.close();
            } catch (IOException closeEx) {
                closeEx.printStackTrace();
            }
        }
    }

    SocketChannel getChannel(ArrayBlockingQueue<SocketChannel> queue) {
        return queue.poll();
    }

    @Override
    void read(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel)key.channel();
        ConnectionState state = (ConnectionState) key.attachment();

        ByteBuffer readBuffer = state.readBuffer;

        int count = 0;

        try{
            count = client.read(readBuffer);
        }catch (IOException ex){
            closeConnection(key);
            return;
        }

        if(count == -1){
           closeConnection(key);
           return;
        }
        if(count > 0){

            readBuffer.flip();

            byte[] data = new byte[count];

            readBuffer.get(data);

            System.out.println(new String(data));

            readBuffer.clear();

            state.writeBuffer = ByteBuffer.wrap(data);

            key.interestOps(SelectionKey.OP_WRITE);

            write(key);
        }
    }

    @Override
    void write(SelectionKey key) throws IOException {

        SocketChannel client = (SocketChannel) key.channel();

        ConnectionState state =(ConnectionState) key.attachment();

        ByteBuffer writeBuffer = state.writeBuffer;

        if (writeBuffer == null) { //新增：没有待发送数据时恢复关注读事件
            key.interestOps(SelectionKey.OP_READ);
            return;
        }

        try{
            client.write(writeBuffer);
        }catch (IOException ex){
            closeConnection(key);
            return;
        }

        if(writeBuffer.hasRemaining()){
            key.interestOps(SelectionKey.OP_WRITE);
        }else{
            state.writeBuffer = null;
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    //新增：停止时唤醒 Selector，否则仅修改 stop 仍可能一直等待
    public void shutdown() {
        stop = true;
        selector.wakeup();
    }

    private void closeConnection(SelectionKey key) {
        key.cancel();
        try {
            key.channel().close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
