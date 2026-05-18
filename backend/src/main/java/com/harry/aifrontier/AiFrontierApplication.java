package com.harry.aifrontier;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.harry.aifrontier.mapper")
public class AiFrontierApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiFrontierApplication.class, args);
    }
}
