package center.oneapi.mobile.features.desktop;

import org.json.JSONArray;
import org.json.JSONObject;

import center.oneapi.mobile.core.ApiClient;

public class DesktopRepository {
    private final ApiClient api;

    public DesktopRepository(ApiClient api) {
        this.api = api;
    }

    public JSONArray sessions() throws Exception {
        return arrayData(api.get("/api/mobile/desktop-sessions"));
    }

    public JSONArray sessions(String deviceId) throws Exception {
        String clean = deviceId == null ? "" : deviceId.trim();
        String query = clean.isEmpty() ? "" : "?device_id=" + ApiClient.enc(clean);
        return arrayData(api.get("/api/mobile/desktop-sessions" + query));
    }

    public JSONArray devices() throws Exception {
        return arrayData(api.get("/api/mobile/desktop-devices"));
    }

    public JSONArray extensions(String client, String deviceId) throws Exception {
        String query = "?client=" + ApiClient.enc(client) + "&device_id=" + ApiClient.enc(deviceId);
        return arrayData(api.get("/api/mobile/desktop-extensions" + query));
    }

    public JSONObject bindDevice(String deviceId, String appId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("deviceId", deviceId);
        body.put("appId", appId);
        return api.post("/api/mobile/desktop-bindings", body);
    }

    public JSONObject unbindDevice(String deviceId, String appId) throws Exception {
        return api.delete("/api/mobile/desktop-bindings/" + ApiClient.enc(deviceId) + "?appId=" + ApiClient.enc(appId));
    }

    private static JSONArray arrayData(JSONObject envelope) {
        Object data = envelope == null ? null : envelope.opt("data");
        if (data instanceof JSONArray) return (JSONArray) data;
        if (data instanceof JSONObject) {
            JSONArray sessions = ((JSONObject) data).optJSONArray("sessions");
            if (sessions != null) return sessions;
            JSONArray items = ((JSONObject) data).optJSONArray("items");
            if (items != null) return items;
        }
        return new JSONArray();
    }
}
