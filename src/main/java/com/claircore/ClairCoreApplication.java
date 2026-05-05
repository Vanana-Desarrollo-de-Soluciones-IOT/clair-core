package com.claircore;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ClairCoreApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        
        System.out.println("Loading environment variables from .env file...");
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
            System.out.println("Loaded variable: " + entry.getKey());
        });

        ConfigurableApplicationContext context = SpringApplication.run(ClairCoreApplication.class, args);
        String port = context.getEnvironment().getProperty("server.port", "8080");

        System.out.println("\n---------------------------------------------------------");
        System.out.println("\tSwagger UI: http://localhost:" + port + "/swagger-ui.html");
        System.out.println("\tAPI Docs:   http://localhost:" + port + "/v3/api-docs");
        System.out.println("---------------------------------------------------------\n");
    }

}
