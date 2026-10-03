package dev.fintech.omniledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class OmniLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OmniLedgerApplication.class, args);
    }
}
