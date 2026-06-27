package center.oneapi.mobile;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NetworkRequestPolicyTest {
    @Test
    public void criticalPanelsUseShortOrStableTtls() {
        assertEquals(15_000L, NetworkRequestPolicy.panelTtlMs("钱包与消耗"));
        assertEquals(30_000L, NetworkRequestPolicy.panelTtlMs("服务状态"));
        assertEquals(5 * 60_000L, NetworkRequestPolicy.panelTtlMs("套餐订阅"));
        assertEquals(60_000L, NetworkRequestPolicy.panelTtlMs("版本信息"));
    }
}
