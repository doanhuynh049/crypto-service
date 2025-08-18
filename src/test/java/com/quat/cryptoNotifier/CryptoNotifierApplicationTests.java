package com.quat.cryptoNotifier;

import com.quat.cryptoNotifier.service.SchedulerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CryptoNotifierApplicationTests {

    @Autowired
    private SchedulerService schedulerService;

    @Test
    void contextLoads() {
        // Test that the application context loads successfully
    }

    @Test
    void testManualAdvisory() {
        // This test can be used to manually trigger the advisory process
        // Uncomment the line below to test the full flow
        // schedulerService.runManualAdvisory();
    }

}
