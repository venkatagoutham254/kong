package aforo.kong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"aforo.kong", "com.aforo.apigee"})  // Still need apigee services temporarily
public class KongApplication {
    public static void main(String[] args) {
        SpringApplication.run(KongApplication.class, args);
    }
}
