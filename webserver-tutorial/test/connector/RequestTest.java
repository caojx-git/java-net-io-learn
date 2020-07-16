package connector;

import org.junit.Assert;
import org.junit.Test;
import util.TestUtils;

/**
 * 测试request
 */
public class RequestTest {

    /**
     * 有效请求
     */
    private static final String validRequest = "GET /index.html HTTP/1.1";

    /**
     * 静态资源请求测试，测试是否能成功的获取的请求的静态资源
     */
    @Test
    public void givenValidRequest_thenExtrackUri() {
        Request request = TestUtils.createRequest(validRequest);
        Assert.assertEquals("/index.html", request.getRequestURI());
    }
}
