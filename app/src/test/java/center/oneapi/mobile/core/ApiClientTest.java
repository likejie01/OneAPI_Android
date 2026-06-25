package center.oneapi.mobile.core;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApiClientTest {
    @Test
    public void buildUrl_joinsBaseAndPath() {
        assertEquals("https://ai.oneapi.center/api/status", ApiClient.buildUrl("https://ai.oneapi.center/", "/api/status"));
        assertEquals("https://ai.oneapi.center/api/status", ApiClient.buildUrl("https://ai.oneapi.center", "api/status"));
    }

    @Test
    public void errorMessage_prefersJsonMessage() throws Exception {
        assertEquals("bad token", ApiClient.errorMessage(new JSONObject("{\"message\":\"bad token\"}"), 401));
    }

    @Test
    public void errorMessage_usesDataStringWhenMessageIsGenericError() throws Exception {
        JSONObject json = new JSONObject("{\"success\":false,\"message\":\"error\",\"data\":\"Alipay app pay response missing order_string\"}");

        assertEquals("Alipay app pay response missing order_string", ApiClient.errorMessage(json, 400));
    }

    @Test
    public void isErrorEnvelope_detectsLegacyMessageErrorWithoutSuccessFlag() throws Exception {
        JSONObject json = new JSONObject("{\"message\":\"error\",\"data\":\"拉起支付宝支付失败：Alipay app pay response missing order_string\"}");

        assertEquals(true, ApiClient.isErrorEnvelope(json));
        assertEquals("拉起支付宝支付失败：Alipay app pay response missing order_string", ApiClient.errorMessage(json, 200));
    }
}
