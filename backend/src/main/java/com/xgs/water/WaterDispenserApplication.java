package com.xgs.water;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.xgs.water.mapper")
public class WaterDispenserApplication {
    public static void main(String[] args) {
        SpringApplication.run(WaterDispenserApplication.class, args);
    }
}
