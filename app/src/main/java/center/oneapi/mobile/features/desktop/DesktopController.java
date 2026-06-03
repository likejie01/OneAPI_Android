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
            JSONArray extensionRefs,
            String projectPath,
            String origin,
            String clientRequestId,
            JSONArray attachments
    ) throws Exception {
        JSONObject body = new JSONObject();
        body.put("client", client);
        body.put("deviceId", deviceId);
        body.put("sessionId", sessionId == null ? "" : sessionId);
        body.put("projectPath", projectPath == null ? "" : projectPath);
        body.put("origin", origin == null ? "mobile" : origin);
        body.put("source", origin == null ? "mobile" : origin);
        body.put("clientRequestId", clientRequestId == null ? "" : clientRequestId);
        body.put("prompt", prompt);
        body.put("model", model == null ? "" : model);
        body.put("reasoningEffort", reasoningEffort == null ? "medium" : reasoningEffort);
        body.put("permissionMode", permissionMode == null ? "restricted" : permissionMode);
        body.put("extensionRefs", extensionRefs == null ? new JSONArray() : extensionRefs);
        body.put("attachments", attachments == null ? new JSONArray() : attachments);
        return body;
    }

    public JSONObject sendJob(String client, String deviceId, String sessionId, String prompt, String model, String reasoningEffort, String permissionMode, JSONArray extensionRefs) throws Exception {
        return sendJob(client, deviceId, sessionId, prompt, model, reasoningEffort, permissionMode, extensionRefs, "", "mobile", "", new JSONArray());
    }

    public JSONObject sendJob(String client, String deviceId, String sessionId, String prompt, String model, String reasoningEffort, String permissionMode, JSONArray extensionRefs, String projectPath, String origin, String clientRequestId, JSONArray attachments) throws Exception {
        return api.post("/api/mobile/desktop-jobs", buildJob(client, deviceId, sessionId, prompt, model, reasoningEffort, permissionMode, extensionRefs, projectPath, origin, clientRequestId, attachments));
    }
}
