package center.oneapi.mobile.features.desktop;

import org.json.JSONArray;
import org.json.JSONObject;

import center.oneapi.mobile.core.ApiClient;

public class DesktopController {
    private final ApiClient api;

    public DesktopController(ApiClient api) {
        this.api = api;
    }

    public JSONObject buildJob(
            String client,
            String deviceId,
            String sessionId,
            String prompt,
            String model,
            String reasoningEffort,
            String permissionMode,
            JSONArray extensionRefs
    ) throws Exception {
        JSONObject body = new JSONObject();
        body.put("client", client);
        body.put("deviceId", deviceId);
        body.put("sessionId", sessionId == null ? "" : sessionId);
        body.put("prompt", prompt);
        body.put("model", model == null ? "" : model);
        body.put("reasoningEffort", reasoningEffort == null ? "medium" : reasoningEffort);
        body.put("permissionMode", permissionMode == null ? "restricted" : permissionMode);
        body.put("extensionRefs", extensionRefs == null ? new JSONArray() : extensionRefs);
        return body;
    }

    public JSONObject sendJob(String client, String deviceId, String sessionId, String prompt, String model, String reasoningEffort, String permissionMode, JSONArray extensionRefs) throws Exception {
        return api.post("/api/mobile/desktop-jobs", buildJob(client, deviceId, sessionId, prompt, model, reasoningEffort, permissionMode, extensionRefs));
    }
}
