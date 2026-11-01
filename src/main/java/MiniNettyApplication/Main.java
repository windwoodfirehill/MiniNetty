package MiniNettyApplication;

import Core.BossLoop;
import Core.ChannelDispatcher;
import Core.WorkerLoop;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;

public class Main {

    public static void main(String[] args) throws IOException {
        ChannelDispatcher channelDispatcher = new ChannelDispatcher();
        //原本：new InetSocketAddress()，没有无参构造器
        int port = args.length == 0 ? 8888 : Integer.parseInt(args[0]); //新增：测试可指定端口
        BossLoop bossLoop = new BossLoop(new InetSocketAddress("127.0.0.1", port),channelDispatcher); //修改
        WorkerLoop workerLoop = new WorkerLoop();
        channelDispatcher.injectWorkerLoop(workerLoop);
        Thread boss = new Thread(()->{
            try {
                bossLoop.loop();
            }catch (IOException ex){
                ex.printStackTrace();
            }
        },"boss-1");

        Thread worker = new Thread(()->{
            try{
                workerLoop.loop();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        },"worker-1");

        boss.start();
        worker.start();
    }
}
