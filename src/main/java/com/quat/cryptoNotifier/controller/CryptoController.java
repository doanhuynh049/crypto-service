package com.quat.cryptoNotifier.controller;

import com.quat.cryptoNotifier.service.SchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CryptoController {

    @Autowired
    private SchedulerService schedulerService;

    @GetMapping("/trigger-advisory")
    public String triggerAdvisory() {
        try {
            schedulerService.runManualAdvisory();
            return "Advisory triggered successfully";
        } catch (Exception e) {
            return "Error triggering advisory: " + e.getMessage();
        }
    }

    @GetMapping("/health")
    public String health() {
        return "Crypto Advisory Service is running";
    }
}
