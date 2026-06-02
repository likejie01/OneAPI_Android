package center.oneapi.mobile.features.image;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImageControllerTest {
    @Test
    public void extractImage_wrapsBase64() throws Exception {
        JSONObject response = new JSONObject("{\"data\":[{\"b64_json\":\"abc\"}]}");
        assertEquals("data:image/png;base64,abc", ImageController.extractImage(response));
    }
}
