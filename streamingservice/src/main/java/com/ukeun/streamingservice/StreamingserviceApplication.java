package com.ukeun.streamingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.ukeun.streamingservice.rtmp")
public class StreamingserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamingserviceApplication.class, args);
    }

}
