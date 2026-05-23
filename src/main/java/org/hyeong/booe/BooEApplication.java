package org.hyeong.booe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BooEApplication {

    public static void main(String[] args) {
        SpringApplication.run(BooEApplication.class, args);


    }

}
