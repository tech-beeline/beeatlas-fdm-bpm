package ru.beeline.fdmbpm;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableProcessApplication
@EnableScheduling
@SpringBootApplication
public class FdmBpmApplication {

    public static void main(String[] args) {
        SpringApplication.run(FdmBpmApplication.class, args);
    }
}
