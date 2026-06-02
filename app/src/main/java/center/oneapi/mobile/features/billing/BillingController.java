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
}
