package Core;

import java.nio.charset.StandardCharsets;
import ConnectionService.Connection;
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
import java.security.Key;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Supplier;

public class WorkerLoop extends EventLoop{
    //原本：QUE_SIZE = 1
    public static final int QUE_SIZE = 1024; //修改：允许暂存更多待注册连接
    public final ArrayBlockingQueue<SocketChannel> queue;
    private final String protocolType; //修改：Worker 保存协议类型，不共用解析器
    private final Map<String,MessageHandler> handlerMap;
    private  Map<String, Supplier<Protocol>> protocolHandlers;
    private int workerId;

    public WorkerLoop(String protocolType, MessageHandler messageHandler,Map handlerMap,
                      Map<String, Supplier<Protocol>> protocolHandlers,int workerId) throws IOException {
        this.protocolType = protocolType;
        this.handlerMap = handlerMap; //修改：初始化失败向调用方抛出
        queue = new ArrayBlockingQueue<>(QUE_SIZE);
        this.protocolHandlers = protocolHandlers;
        this.workerId = workerId;
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
                    read(key); //修改：read 返回 void，关闭连接后由下面的 isValid 判断
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
            state.protocol = protocolHandlers.get(protocolType).get();

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

            MessageHandler handler;

            Protocol protocol = state.protocol; //修改：连接存续期间复用同一个解析器

            handler = handlerMap.get(protocol.getType());

            ByteBuffer byteBuffer = readBuffer.get(data);
            //判断是否完全到达
            String temp = new String(data, StandardCharsets.UTF_8);
            if(!protocol.isArrived(temp)){
                return;
            }
            Connection connection = new Connection(this,state);

            handler.onMessage(connection,protocol.decoder(data));

            System.out.println(new String(data, StandardCharsets.UTF_8));

            readBuffer.clear();
        }
    }


    void write(SelectionKey key) throws IOException {

        SocketChannel client = (SocketChannel) key.channel();

        ConnectionState state =(ConnectionState) key.attachment();

        ByteBuffer writeBuffer = state.writeBuffer;

        if (writeBuffer == null ) {
            if(state.queue != null) {
                Object object = state.queue.poll();
                if (object != null) {
                    Protocol protocol = state.protocol;
                    byte[] data = protocol.encoder(object);
                    writeBuffer = ByteBuffer.wrap(data);

                } else {
                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                    return;
                }
            }else{
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
        key.cancel();
        try {
            key.channel().close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
