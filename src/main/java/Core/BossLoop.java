package Core;

import ConnectionService.ServiceCreater;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey; //新增：匹配父类方法参数
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

public class BossLoop extends EventLoop{
    private final Selector selector;
    private final ServiceCreater serviceCreater;
    private volatile boolean stop = false;
    private final ChannelDispatcher channelDispatcher;
    public BossLoop(SocketAddress socketAddress, ChannelDispatcher channelDispatcher) throws IOException {
        this.selector = Selector.open();
        this.serviceCreater = new ServiceCreater(socketAddress);
        this.channelDispatcher = channelDispatcher;
        register(selector);
    }
    @Override
    public void loop()throws  IOException{
        while(!stop){
            //
            int readyCount = selector.select();
            if(stop){
                break;
            }
            if(readyCount <= 0){
                continue;
            }
            //获取就绪链接
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while(iterator.hasNext()){
                SelectionKey key = iterator.next();
                iterator.remove();
                if(!key.isValid()){
                    continue;
                }
                if(key.isAcceptable()){
                    SocketChannel socketChannel = serviceCreater.acceptConnection();
                    if (socketChannel == null) {
                        continue;
                    }
                    if(!channelDispatcher.submit(socketChannel)){
                        try{
                            socketChannel.close();
                        }catch (IOException ex){
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
    }
         public void shutdown() {
              stop = true;
              selector.wakeup();
        }
    //初始化
    @Override
    void register(Selector selector) throws IOException {
        serviceCreater.register(selector);
    }

    @Override
    //原本：void read()
    void read(SelectionKey key) throws IOException { //修改：匹配父类声明，保留空实现

    }

    @Override
    //原本：void write()
    void write(SelectionKey key) { //修改：匹配父类声明，保留空实现

    }
}
