package processor;

import connector.Request;
import org.junit.Assert;
import org.junit.Test;
import util.TestUtils;

import javax.servlet.Servlet;
import java.net.MalformedURLException;
import java.net.URLClassLoader;

/**
 * 测试动态资源
 */
public class ProcessorTest {

    /**
     * 动态资源请求
     */
    private static final String servletRequest = "GET /servlet/TimeServlet HTTP/1.1";

    @Test
    public void givenServletRequest_thenLoadServlet() throws MalformedURLException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        // 创建请求request
        Request request = TestUtils.createRequest(servletRequest);

        // 创建处理动态资源的ServletProcessor
        ServletProcessor processor = new ServletProcessor();
        URLClassLoader loader = processor.getServletLoader();
        Servlet servlet = processor.getServlet(loader, request);

        // 验证是否取得TimeServlet实例
        Assert.assertEquals("TimeServlet", servlet.getClass().getName());
    }
}
