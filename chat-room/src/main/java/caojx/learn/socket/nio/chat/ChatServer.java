package caojx.learn.socket.nio.chat;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * 使用nio编程模型实现多人聊天室-服务端
 *
 * @author caojx created on 2020/6/23 6:50 下午
 */
public class ChatServer {

    private static final int DEFAULT_PORT = 8888;

    private static final String QUIT = "quit";

    /**
     * 缓冲区大小
     */
    private static final int BUFFER = 1024;

    /**
     * 服务器端通道，使用通道进行通信
     */
    private ServerSocketChannel serverSocketChannel;

    /**
     * 选择器
     */
    private Selector selector;

    /**
     * 用来读取消息的buffer
     */
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);

    /**
     * 用来写入信息的buffer
     */
    private ByteBuffer wBeBuffer = ByteBuffer.allocate(BUFFER);

    private Charset charset = Charset.forName("UTF-8");

    private int port;

    public ChatServer(int port) {
        this.port = port;
    }

    /**
     * 检查是否退出
     *
     * @param msg
     * @return
     */
    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    /**
     * 开始启动
     */
    public void start() {
        try {
            // 创建一个ServerSocketChannel通道
            serverSocketChannel = ServerSocketChannel.open();
            // 设置通道为未非阻塞（默认是阻塞的）
            serverSocketChannel.configureBlocking(false);
            // 绑定到监听端口
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            // 创建selector
            selector = Selector.open();
            // 服务端通道上注册需要监听的ACCEPT客户端连接请求事件
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("启动服务器，监听端口:" + port + ".....");
            // 进入监听模式
            while (true) {
                // select()函数是阻塞式的
                selector.select();
                // 获取监听事件，每一个被触发的事件与他相关的信息都包装在SelectionKey对象中
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys) {
                    // 处理被触发事件
                    handles(selectionKey);
                }
                // 手动把已处理的事件清空
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(selector);
        }
    }

    /**
     * 处理被触发事件
     * 主要处理两种事件
     * 1.客户端连接请求时间ACCEPT事件
     * 2.已连接的客户端发送消息后的READ事件
     *
     * @param selectionKey
     * @throws IOException
     */
    private void handles(SelectionKey selectionKey) throws IOException {
        // ACCEPT事件--和客户建立了链接
        if (selectionKey.isAcceptable()) {
            // 获服务器通道，即返回为之创建此键的通道
            ServerSocketChannel server = (ServerSocketChannel) selectionKey.channel();
            // 获取客户端通道，并接受客户端连接请求
            SocketChannel client = server.accept();
            // 设置非阻塞
            client.configureBlocking(false);
            // 客户端通道上注册需要监听的READ事件
            client.register(selector, SelectionKey.OP_READ);
            System.out.println("客户端[" + client.socket().getPort() + "]" + "客户端链接了");
            // READ事件---客户发送消息，有了可读的事件
        } else if (selectionKey.isReadable()) {
            // 获取客户端通道，并读取客户端发送过来的消息
            SocketChannel client = (SocketChannel) selectionKey.channel();
            String fwMsg = receive(client);

            if (fwMsg.isEmpty()) {
                // 客户端异常，不再监听该客户端可能发送过来的消息
                selectionKey.cancel();
                // 事件发生了变化，更新selector监听的事件
                selector.wakeup();
            } else {
                // 消息转发给其他在线的客户端
                forwardMessage(client, fwMsg);
                // 检查用户是否退出
                if (readyToQuit(fwMsg)) {
                    selectionKey.cancel();
                    selector.wakeup();
                    System.out.println("客户端[" + client.socket().getPort() + "]" + "断开链接了");
                }
            }
        }

    }

    /**
     * 转发消息给客户端
     *
     * @param client 发送消息的客户端本身
     * @param fwMsg  消息
     */
    private void forwardMessage(SocketChannel client, String fwMsg) throws IOException {
        // selector.keys() 会返回所有已经注册在selector上的SelectionKey的集合，
        // 我们可以认为注册在selector上的SelectionKey即是当前在线的客户端
        for (SelectionKey key : selector.keys()) {

            // 跳过服务器端的通道 ServerSocketChannel
            Channel connectedClient = key.channel();
            if (connectedClient instanceof ServerSocketChannel) {
                continue;
            }
            // 检测channel没有被关闭，且通道不是自己本身
            if (key.isValid() && !client.equals(connectedClient)) {
                wBeBuffer.clear();
                wBeBuffer.put(charset.encode(fwMsg));
                wBeBuffer.flip();
                while (wBeBuffer.hasRemaining()) {
                    ((SocketChannel) connectedClient).write(wBeBuffer);
                }
            }
            System.out.println("客户端[" + client.socket().getPort() + "]" + fwMsg);
        }

    }

    /**
     * 读取channel上面的信息
     *
     * @param client
     * @return
     * @throws IOException
     */
    private String receive(SocketChannel client) throws IOException {
        // 清理残留的内容
        rBuffer.clear();
        while (client.read(rBuffer) > 0) ;
        // 写模式切换回读模式
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    /**
     * 关闭资源
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
        ChatServer charServer = new ChatServer(DEFAULT_PORT);
        charServer.start();
    }
}
