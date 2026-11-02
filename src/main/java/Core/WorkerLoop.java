package Core;

import ConnectionService.ConnectionState;
import Handler.MessageHandler;
import Protocol.Protocol;
import java.io.IOException;
//修改：删除未使用的 import；原本：import java.nio.Buffer;
import Protocol.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

public class WorkerLoop extends EventLoop{
    //原本：QUE_SIZE = 1
    public static final int QUE_SIZE = 1024; //修改：允许暂存更多待注册连接
    public final ArrayBlockingQueue<SocketChannel> queue;
    private final Protocol protocol;
    private final Map<String,MessageHandler> handlerMap;
    public WorkerLoop(Protocol protocol, MessageHandler messageHandler,Map handlerMap) throws IOException {
        this.protocol = protocol;
        this.handlerMap = handlerMap; //修改：初始化失败向调用方抛出
        queue = new ArrayBlockingQueue<>(QUE_SIZE);
    }

    public boolean offerToQueue(SocketChannel socketChannel){
        boolean result = queue.offer(socketChannel);
        selector.wakeup();
        return result;
    }

    @Override
    public void loop() throws IOException {
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
        //设置非阻塞，只打印异常栈而不终止整个loop
        try {
            channel.configureBlocking(false);
            ConnectionState state = new ConnectionState();
            if(co()){
                System.out.println("正在开发中->获取协议->设置state的协议/否则使用默认协议");
            }
            state.protocol = protocol;

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
    //未来的协商
    public boolean co(){
        return false;
    }

    SocketChannel getChannel(ArrayBlockingQueue<SocketChannel> queue) {
        return queue.poll();
    }

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
            MessageHandler handler;
            System.out.println(new String(data));
            if(state.protocol == null) {
                handler = handlerMap.get(protocol.getType());
            }else{
                handler = handlerMap.get(state.protocol.getType());
            }

            handler.onMessage((HttpRequest)protocol.decoder(data));

            readBuffer.clear();
            //以下代码预期清除
            state.writeBuffer = ByteBuffer.wrap(data);

            key.interestOps(SelectionKey.OP_WRITE);

            write(key);
        }
    }


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

    private void closeConnection(SelectionKey key) {
        key.cancel();
        try {
            key.channel().close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
