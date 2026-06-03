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

    @Test
    public void extractResult_readsUsageTokens() throws Exception {
        JSONObject response = new JSONObject("{\"data\":[{\"b64_json\":\"abc\"}],\"usage\":{\"input_tokens\":12,\"output_tokens\":34,\"total_tokens\":46}}");
        ImageController.ImageResult result = ImageController.extractResult(response);

        assertEquals("data:image/png;base64,abc", result.image);
        assertEquals("Tokens: 12 34 46", result.tokenText);
    }
}
