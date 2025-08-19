package org.codenbug.purchase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication(scanBasePackages = {
    "org.codenbug.purchase",
    "org.codenbug.message"
})
@EnableKafka
public class PurchaseTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(PurchaseTestApplication.class, args);
    }
}