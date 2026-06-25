package center.oneapi.mobile.features.billing;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AlipayPaymentSourceTest {
    @Test
    public void androidAppPaymentUsesOfficialSdkOrderStringOnly() throws Exception {
        String source = read("src/main/java/center/oneapi/mobile/MainActivity.java");

        assertTrue(source.contains("Class.forName(\"com.alipay.sdk.app.PayTask\")"));
        assertTrue(source.contains("payV2.invoke(payTask, orderString, true)"));
        assertTrue(source.contains("handleAlipaySdkResult"));
        assertTrue(source.contains("result.optString(\"resultStatus\""));
        assertFalse(source.contains("alipayqr://platformapi/startapp"));
        assertFalse(source.contains("alipays://platformapi/startapp?appId=20000067"));
    }

    @Test
    public void createAlipayOrderTargetsAndroidAppPayScene() throws Exception {
        String source = read("src/main/java/center/oneapi/mobile/features/billing/BillingController.java");

        assertTrue(source.contains("body.put(\"platform\", \"android\")"));
        assertTrue(source.contains("body.put(\"pay_scene\", \"app\")"));
        assertTrue(source.contains("body.put(\"pay_product\", \"alipay.trade.app.pay\")"));
        assertTrue(source.contains("body.put(\"payment_product\", \"alipay.trade.app.pay\")"));
        assertTrue(source.contains("return api.post(\"/api/user/alipay/pay\", body)"));
    }

    private static String read(String path) throws Exception {
        Path root = Paths.get("").toAbsolutePath();
        return new String(Files.readAllBytes(root.resolve(path)), StandardCharsets.UTF_8);
    }
}
