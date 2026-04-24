package com.claircore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class ClairCoreApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(ClairCoreApplication.class, args);
        Environment env = context.getEnvironment();
        String port = env.getProperty("server.port", "8080");

        System.out.println("\n---------------------------------------------------------");
        System.out.println("\tSwagger UI: http://localhost:" + port + "/swagger-ui.html");
        System.out.println("\tAPI Docs:   http://localhost:" + port + "/v3/api-docs");
        System.out.println("---------------------------------------------------------\n");
    }

}
