package com.hcn;

import com.hcn.util.Matrix;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HcnApplication {
    public static void main(String[] args) {
        SpringApplication.run(HcnApplication.class, args);
    }
    
    @Bean
    public CommandLineRunner run() {
        return args -> {
            System.out.println("HCN Application started. Visit http://localhost:9090");
        };
    }
}
