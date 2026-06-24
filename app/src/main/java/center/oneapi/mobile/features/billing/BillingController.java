package center.oneapi.mobile.features.billing;

import org.json.JSONObject;

import center.oneapi.mobile.core.ApiClient;

public class BillingController {
    private final ApiClient api;

    public BillingController(ApiClient api) {
        this.api = api;
    }

    public JSONObject profile() throws Exception {
        return api.get("/api/user/self");
    }

    public JSONObject topups() throws Exception {
        return api.get("/api/user/topup/self?p=1&page_size=20");
    }

    public JSONObject usage() throws Exception {
        return api.get("/api/log/self?p=1&page_size=50");
    }

    public JSONObject plans() throws Exception {
        return api.get("/api/subscription/plans");
    }

    public JSONObject subscriptions() throws Exception {
        return api.get("/api/subscription/self");
    }

    public JSONObject purchase(JSONObject body) throws Exception {
        return api.post("/api/subscription/wallet/pay", body);
    }

    public JSONObject createAlipayOrder(int amount) throws Exception {
        JSONObject body = new JSONObject();
        body.put("amount", amount);
        body.put("platform", "android");
        body.put("pay_scene", "app");
        body.put("pay_product", "alipay.trade.app.pay");
        body.put("payment_product", "alipay.trade.app.pay");
        return api.post("/api/user/alipay/pay", body);
    }

    public JSONObject queryAlipayOrder(String tradeNo) throws Exception {
        String cleanTradeNo = tradeNo == null ? "" : tradeNo.trim();
        return api.get("/api/user/alipay/query?trade_no=" + java.net.URLEncoder.encode(cleanTradeNo, java.nio.charset.StandardCharsets.UTF_8.name()));
    }

    public JSONObject cancelAlipayOrder(String tradeNo) throws Exception {
        JSONObject body = new JSONObject();
        body.put("trade_no", tradeNo == null ? "" : tradeNo.trim());
        return api.post("/api/user/alipay/cancel", body);
    }
}
