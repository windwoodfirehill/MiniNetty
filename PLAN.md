# MiniNetty 学习计划

目标：用 JDK 17 的 Java NIO 手写一个小型网络服务器，理解 Selector、事件循环、Channel 和 Handler。代码由学习者自己实现，每完成一步再检查和测试。

## 阶段一：接受连接

- 创建单线程服务端，使用 ServerSocketChannel 和 Selector。
- 绑定端口，注册 OP_ACCEPT，接受客户端连接并打印连接信息。
- 验收：多个客户端可以连接，服务端能够继续接受新连接。

## 阶段二：读取与 Echo

- 将客户端 SocketChannel 设为非阻塞，并注册 OP_READ。
- 用 ByteBuffer 读取字节，正确处理无数据和客户端断开。
- 将收到的字节写回；处理非阻塞部分写以及 OP_WRITE。
- 验收：多个客户端可以收发数据，连接关闭后资源得到释放。

## 阶段三：拆分事件循环

- 将 Selector 循环抽象为 EventLoop。
- 拆分 Boss 接受连接与 Worker 处理读写，每个 Worker 管理多个连接。
- 实现跨线程注册任务和 Selector.wakeup()。
- 验收：多个连接分配给 Worker，跨线程注册后能及时处理事件。
*
## 阶段四：Handler 与简单 HTTP

- 定义小型 Handler，让网络事件与应用处理分离。
- 处理 TCP 字节流的半包和粘包，设置合理的请求大小限制。
- 实现简化 HTTP 解码：请求行、请求头、Content-Length 和文本请求体。
- 返回合法的 HTTP 响应；暂不处理视频和其他二进制业务。
- 验收：浏览器或客户端能发送简单 GET、POST，服务端解析并响应。

## 暂不实现

内存池、零拷贝、TLS、完整 HTTP 协议、原生 epoll、生产级性能与完整 Netty API。每个阶段先运行并理解，再进入下一阶段。
