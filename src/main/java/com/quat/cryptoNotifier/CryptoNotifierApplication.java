package com.quat.cryptoNotifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CryptoNotifierApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoNotifierApplication.class, args);
    }

}
