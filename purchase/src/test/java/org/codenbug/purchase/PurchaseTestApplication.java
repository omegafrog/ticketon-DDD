package org.codenbug.purchase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"org.codenbug.purchase", "org.codenbug.message"})
public class PurchaseTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(PurchaseTestApplication.class, args);
    }
}
