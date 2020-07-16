package caojx.learn.socket.aio.chat;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 使用AIO编程模型实现多人聊天室-服务端
 */
public class ChatServer {

    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    /**
     * 使用自定义的异步共享通道组
     */
    private AsynchronousChannelGroup asynchronousChannelGroup;

    /**
     * 服务器端通道
     */
    private AsynchronousServerSocketChannel serverSocketChannel;

    /**
     * 在线客户端列表
     */
    private List<ClientHandler> connectedClients;

    /**
     * 编码方式
     */
    private Charset charset = Charset.forName("UTF-8");

    private int port;

    public ChatServer(int port) {
        this.port = port;
        this.connectedClients = new ArrayList<>();
    }


    public void start() {
        try {
            // 自定义AsynchronousChannelGroup
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            // 将线程池加入到异步通道中
            asynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(executorService);

            // 打开通道,使用自定义的asynchronousChannelGroup
            serverSocketChannel = AsynchronousServerSocketChannel.open(asynchronousChannelGroup);

            // 通道绑定本地主机和端口
            serverSocketChannel.bind(new InetSocketAddress(LOCALHOST, DEFAULT_PORT));
            System.out.println("启动服务器,监听端口" + DEFAULT_PORT);

            // while循环，持续监听客户端的连接请求
            while (true) {
                // 一直调用accept函数,接收要与服务端建立连接的用户
                serverSocketChannel.accept(null, new AcceptHandler());
                // 阻塞式调用,防止占用系统资源
                System.in.read();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(serverSocketChannel);
        }
    }

    /**
     * 创建AcceptHandler，用来处理accept函数的异步调用的返回结果，即接收客户端的连接请求后，进行回调
     */
    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

        @Override
        public void completed(AsynchronousSocketChannel clientChannel, Object attachment) {
            if (serverSocketChannel.isOpen()) {
                // 继续等待监听新的客户端的连接请求
                serverSocketChannel.accept(null, this);
            }

            // 处理当前已连接的客户端的数据读写
            if (clientChannel != null && clientChannel.isOpen()) {
                // 为该新连接的用户创建handler,用于读写操作
                ClientHandler clientHandler = new ClientHandler(clientChannel);

                // 数据读写需要通过buffer
                ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER);

                // 将新用户添加到在线列表
                addClient(clientHandler);

                // 第一个buffer：表示从clientChannel中读取的信息写入到buffer缓冲区中
                // 第二个buffer：handler回调函数被调用时,buffer会被当做一个attachment参数传入到该回调函数中
                // buffer、attachment、使用ClientHandler来处理read函数的异步调用的返回结果
                clientChannel.read(byteBuffer, byteBuffer, clientHandler);
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("连接失败" + exc);
        }
    }

    /**
     * 创建客户端的ClientHandler，用来处理read函数的异步调用的返回结果
     */
    private class ClientHandler implements CompletionHandler<Integer, Object> {

        /**
         * 当前处理对应的客户端通道
         */
        private AsynchronousSocketChannel clientChannel;

        private ClientHandler(AsynchronousSocketChannel clientChannel) {
            this.clientChannel = clientChannel;
        }

        /**
         * 成功的异步回调
         *
         * @param result     表示我们从read中读取了多少数据
         * @param attachment
         */
        @Override
        public void completed(Integer result, Object attachment) {
            ByteBuffer byteBuffer = (ByteBuffer) attachment;
            if (byteBuffer != null) {
                if (result <= 0) {
                    // result<=0，非正整数，可以理解为客户端异常
                    // 将客户移除出在线列表
                    removeClient(this);
                } else {
                    // 将 buffer 从写变为读模式
                    byteBuffer.flip();

                    // 打印消息
                    String fwdMsg = receive(byteBuffer);
                    System.out.println(getClientName(clientChannel) + ":" + fwdMsg);

                    // 转发消息给当前的其他在线用户
                    fwdwordMessage(clientChannel, fwdMsg);

                    // 重置buffer
                    byteBuffer.clear();

                    // 如果客户端发送的是quit退出消息，则把客户移除监听的客户列表
                    if (readyToQuit(fwdMsg)) {
                        // 将客户从在线客户列表中去除
                        removeClient(this);
                    } else {
                        // 如果不是则继续等待读取用户输入的信息
                        clientChannel.read(byteBuffer, byteBuffer, this);
                    }
                }
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("读写失败:" + exc);
        }
    }

    /**
     * 添加一个新的客户端进客户端列表(list集合)
     *
     * @param handler
     */
    private synchronized void addClient(ClientHandler handler) {
        connectedClients.add(handler);
        System.out.println(getClientName(handler.clientChannel) + "已经连接到服务器");
    }

    /**
     * 将该客户(下线)从列表中删除
     *
     * @param clientHandler
     */
    private synchronized void removeClient(ClientHandler clientHandler) {
        connectedClients.remove(clientHandler);
        System.out.println(getClientName(clientHandler.clientChannel) + "已断开连接");
        //关闭该客户对应流
        close(clientHandler.clientChannel);
    }

    /**
     * 服务器其实客户端发送的信息,并将该信息进行utf-8解码
     *
     * @param buffer
     * @return
     */
    private synchronized String receive(ByteBuffer buffer) {
        return String.valueOf(charset.decode(buffer));
    }

    /**
     * 服务器端转发该客户发送的消息到其他客户控制室上(转发信息)
     *
     * @param clientChannel
     * @param fwdMsg
     */
    private synchronized void fwdwordMessage(AsynchronousSocketChannel clientChannel, String fwdMsg) {
        for (ClientHandler handler : connectedClients) {
            // 该信息不用再转发到发送信息的那个人那
            if (!handler.clientChannel.equals(clientChannel)) {
                try {
                    // 将要转发的信息写入到缓冲区中
                    ByteBuffer buffer = charset.encode(getClientName(handler.clientChannel) + ":" + fwdMsg);
                    // 将相应的信息写入到用户通道中,用户再通过获取通道中的信息读取到对应转发的内容
                    handler.clientChannel.write(buffer, null, handler);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 获取客户端的端口号并打印出来
     *
     * @param clientChannel
     * @return
     */
    private String getClientName(AsynchronousSocketChannel clientChannel) {
        int clientPort = -1;
        try {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) clientChannel.getRemoteAddress();
            clientPort = inetSocketAddress.getPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "客户端[" + clientPort + "]:";
    }

    /**
     * 判断是否退出
     *
     * @param msg
     * @return
     */
    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    /**
     * 回收资源
     *
     * @param closeable
     */
    public void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatServer charServer = new ChatServer(8886);
        charServer.start();
    }
}
