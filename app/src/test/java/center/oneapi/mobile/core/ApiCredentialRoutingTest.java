package center.oneapi.mobile.core;

import org.junit.Test;

import center.oneapi.mobile.features.chat.ChatController;
import center.oneapi.mobile.features.image.ImageController;

import static org.junit.Assert.assertEquals;

public class ApiCredentialRoutingTest {
    @Test
    public void bearerValue_prefersExplicitRelayKeyForModelCalls() {
        assertEquals("sk-app", ApiClient.bearerValue("access-token", "sk-app"));
    }

    @Test
    public void bearerValue_fallsBackToLoginTokenWhenRelayKeyMissing() {
        assertEquals("access-token", ApiClient.bearerValue("access-token", " "));
    }

    @Test
    public void chatAndImageUseRelayRoutesThatAcceptApiKeys() {
        assertEquals("/v1/chat/completions", ChatController.CHAT_COMPLETIONS_PATH);
        assertEquals("/v1/images/generations", ImageController.IMAGE_GENERATIONS_PATH);
        assertEquals("/v1/images/edits", ImageController.IMAGE_EDITS_PATH);
    }
}
