package com.example.docker.dockerexamplecopyjar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class DockerExampleCopyJarApplication {

    public static void main(String[] args) {
        SpringApplication.run(DockerExampleCopyJarApplication.class, args);
    }


    @RequestMapping("/hello")
    public String hello() {
        return "hello docker";
    }

}
