package com.harry.aifrontier;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.harry.aifrontier.mapper")
public class AiFrontierApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiFrontierApplication.class, args);
    }
}
