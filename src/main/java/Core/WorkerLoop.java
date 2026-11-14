package Core;

import java.nio.charset.StandardCharsets;
import ConnectionService.Connection;
import ConnectionService.ConnectionState;
import Handler.MessageHandler;
import Protocol.Protocol;
import ProtocolParser.ProtocolParser;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.Key;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Supplier;

public class WorkerLoop extends EventLoop{

    public static final int QUE_SIZE = 1024;
    public static final int BUF_MIN_SIZE = 1024;
    public static final int BUF_MAX_SIZE = 4096;
    public static final int GROW_RATE = 2;
    public final ArrayBlockingQueue<SocketChannel> queue;
    private final String protocolType;
    private final Map<String,MessageHandler> handlerMap;
    private Map<String, Supplier<ProtocolParser>> protocolParsers;

    public WorkerLoop(String protocolType, MessageHandler messageHandler,Map handlerMap,
                      Map<String, Supplier<ProtocolParser>> protocolParsers) throws IOException {
        this.protocolType = protocolType;
        this.handlerMap = handlerMap; //修改：初始化失败向调用方抛出
        queue = new ArrayBlockingQueue<>(QUE_SIZE);
        this.protocolParsers = protocolParsers;
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

                if(key.isValid() && key.isWritable()){
                    write(key);
                }
            }
            Set<SelectionKey> allKeys = selector.keys();
            Iterator<SelectionKey> allKeysIterator = allKeys.iterator();
            //可写
            while(allKeysIterator.hasNext()){
                SelectionKey key = allKeysIterator.next();
                if(!key.isValid()){
                    continue;
                }
                ConnectionState state = (ConnectionState) key.attachment();
                if(state.writeBuffer == null && state.queue != null && !state.queue.isEmpty()){
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
            //不判空，谁tm给我源码啊
            state.protocolParser = protocolParsers.get(protocolType).get();

            channel.register(selector, SelectionKey.OP_READ,state);

            SelectionKey key = channel.keyFor(selector);

            if(key != null && key.isValid()){
                state.key = key;
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            try {
                channel.close();
            } catch (IOException closeEx) {
                closeEx.printStackTrace();
            }
        }
    }
    public ByteBuffer expand(ByteBuffer byteBuffer,boolean reader){
        int capacity = byteBuffer.capacity();
        if (capacity == 0 || capacity >= BUF_MAX_SIZE) {
            return byteBuffer;
        }
        ByteBuffer temp = byteBuffer.duplicate(); // 修改：复制时保留原 buffer 的位置
        ByteBuffer biggerBuf = ByteBuffer.allocate(Math.min(capacity * GROW_RATE, BUF_MAX_SIZE));
        if(reader) {
            temp.flip();
        }
        biggerBuf.put(temp);
        if (!reader) {
            biggerBuf.flip(); // 新增：发送缓冲区返回时可直接交给 Socket 续写。
        }
        return biggerBuf;
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

            while (readBuffer.hasRemaining()) {
                int requestStart = readBuffer.position(); //新增：记录当前请求的起点
                byte[] data = new byte[readBuffer.remaining()]; //修改：取累计剩余字节，原本只取 count

                MessageHandler handler;

                ProtocolParser parser = state.protocolParser; //修改：连接存续期间复用同一个解析器

                handler = handlerMap.get(parser.getType());

                ByteBuffer byteBuffer = readBuffer.get(data);
                //判断是否完全到达
                String temp = new String(data, StandardCharsets.UTF_8);
                int requestLength = parser.isArrived(data);
                if( requestLength < 0){
                    readBuffer.position(requestStart); //修改：只保留当前尚未完成的请求
                    break; //修改：退出解析循环后统一 compact
                }

                Connection connection = new Connection(this,state);

                handler.onMessage(connection,parser.decoder(data));

                System.out.println(new String(data, 0, requestLength, StandardCharsets.UTF_8)); //修改：只打印本次请求
                //只清楚这次的requestLength

                readBuffer.position(requestStart + requestLength); //修改：从当前请求起点向后消费
            }
            readBuffer.compact();
            if(!readBuffer.hasRemaining()){
                ByteBuffer temp = expand(readBuffer,true);
                if(temp == state.readBuffer){
                    closeConnection(key);
                    return;
                }
                state.readBuffer = temp;
            }
        }
    }

    void write(SelectionKey key) throws IOException {

        SocketChannel client = (SocketChannel) key.channel();

        ConnectionState state =(ConnectionState) key.attachment();

        ByteBuffer writeBuffer = state.writeBuffer;

        if (writeBuffer == null ) {//检查缓冲区是否为空
            if(state.queue != null) {//队列不为空
                Protocol message = state.queue.poll();
                if (message != null) {//取出不为空
                    int length = message.getLength();
                    int remain =writeBuffer.capacity() - writeBuffer.position();
                    while (remain < length) {
                        ByteBuffer temp = expand(writeBuffer,true);
                        if(temp == writeBuffer){
                            //如果没空接住
                            return;
                        }
                        remain += writeBuffer.capacity();
                    }
                    //避免浪费资源
                    byte[] data = state.protocolParser.encoder(message);
                    writeBuffer.put(data);

                } else {//取出消息为空，取消关注
                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                    return;
                }
            }else{//队列为空，取消关注
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                return;
            }
        }

        try{
            client.write(writeBuffer);
        }catch (IOException ex){
            closeConnection(key);
            return;
        }

        if(writeBuffer.hasRemaining()){
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            state.writeBuffer = writeBuffer;
        }else{
            state.writeBuffer = null;
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    private void closeConnection(SelectionKey key) {
        ConnectionState state = (ConnectionState) key.attachment();
        key.cancel();
        try {
            state.key = null;
            key.channel().close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    //0cancel all 1 cancel read 2 cancel write
    public void cancelInterest(int signal,ConnectionState state){
        SelectionKey key = state.key;
        switch (signal){
            case 0:
                key.interestOps(~SelectionKey.OP_WRITE & ~SelectionKey.OP_READ);
            case 1:
                key.interestOps(~SelectionKey.OP_READ);
            case 2:
                key.interestOps(~SelectionKey.OP_WRITE);
        }
    }
}
