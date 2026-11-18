package com.krekno.tripservice;

import com.krekno.tripservice.config.RsaKeyConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableConfigurationProperties(RsaKeyConfigProperties.class)
public class TripServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TripServiceApplication.class, args);
    }

}
