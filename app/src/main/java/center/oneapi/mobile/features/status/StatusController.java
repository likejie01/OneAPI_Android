package center.oneapi.mobile.features.status;

import org.json.JSONObject;

import center.oneapi.mobile.core.ApiClient;

public class StatusController {
    private final ApiClient api;

    public StatusController(ApiClient api) {
        this.api = api;
    }

    public JSONObject announcements() throws Exception {
        return api.get("/api/status");
    }

    public JSONObject serviceStatus() throws Exception {
        return api.get("/api/service-status");
    }

    public JSONObject downloadPackages() throws Exception {
        return api.get("/api/download/packages");
    }
}
