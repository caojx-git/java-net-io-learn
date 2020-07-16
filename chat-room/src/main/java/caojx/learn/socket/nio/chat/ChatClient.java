package caojx.learn.socket.nio.chat;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * 使用nio编程模型实现多人聊天室-客户端
 *
 * @author caojx created on 2020/6/26 6:50 下午
 */
public class ChatClient {

    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";

    private static final int DEFAULT_SERVER_PORT = 9999;

    private static final String QUIT = "quit";

    /**
     * 缓冲区大小
     */
    private static final int BUFFER = 1024;

    private String host;

    private int port;

    private SocketChannel client;

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

    public void start() {
        try {
            // 创建客户端通道
            client = SocketChannel.open();
            client.configureBlocking(false);
            selector = Selector.open();
            // 注册客户端需要监听的连接事件 CONNECT
            client.register(selector, SelectionKey.OP_CONNECT);
            // 向服务器发送连接请求
            client.connect(new InetSocketAddress(host, port));
            while (true) {
                selector.select();
                // 获取被触发的事件
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys) {
                    handles(selectionKey);
                }
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }catch (ClosedSelectorException e){
            // 用户正常退出
        } finally {
            close(selector);
        }
    }

    /**
     * 处理被触发的事件
     *
     * @param selectionKey
     * @throws IOException
     */
    private void handles(SelectionKey selectionKey) throws IOException {
        // CONNECT事件---连接就绪事件
        if (selectionKey.isConnectable()) {
            // 获取selectionKey上对应的客户端通道
            SocketChannel client = (SocketChannel) selectionKey.channel();
            // 请求是否已经链接
            if (client.isConnectionPending()) {
                // 正式建立链接
                client.finishConnect();

                // 处理用户的输入信息
                new Thread(new UserInputHandler(this)).start();
            }

            // 注册READ事件，接收其他客户端转发过来的消息
            client.register(selector, SelectionKey.OP_READ);
        }
        // READ事件---服务器转发消息事件
        else if (selectionKey.isReadable()) {
            SocketChannel client = (SocketChannel) selectionKey.channel();
            String msg = receive(client);
            if (msg.isEmpty()) {
                // 服务器异常，关闭selector，客户端退出
                close(selector);
            } else {
                System.out.println("客户端["+client.socket().getPort()+"]"+msg);
            }
        }
    }

    /**
     * 读取消息
     *
     * @param client
     * @return
     * @throws IOException
     */
    private String receive(SocketChannel client) throws IOException {
        rBuffer.clear();
        while (client.read(rBuffer) > 0) ;
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    public ChatClient() {
        this(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
    }

    public ChatClient(String host, int port) {
        this.host = host;
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
     * 发送消息
     *
     * @param msg
     * @throws IOException
     */
    public void send(String msg) throws IOException {
        if (msg.isEmpty()) {
            return;
        }
        wBeBuffer.clear();
        wBeBuffer.put(charset.encode(msg));
        wBeBuffer.flip();
        while (wBeBuffer.hasRemaining()) {
            client.write(wBeBuffer);
        }
        //检查用户是否准备推出
        if (readyToQuit(msg)) {
            close(selector);
        }
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
        ChatClient chartClient = new ChatClient("127.0.0.1", 8888);
        chartClient.start();
    }
}
