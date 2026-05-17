package com.claircore;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ClairCoreApplication {

    private static final Logger log = LoggerFactory.getLogger(ClairCoreApplication.class);

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        // Never print environment variables (secrets). Only load them into system properties.
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });

        ConfigurableApplicationContext context = SpringApplication.run(ClairCoreApplication.class, args);
        String port = context.getEnvironment().getProperty("server.port", "8080");

        log.info("Swagger UI: http://localhost:{}/swagger-ui.html", port);
        log.info("API Docs:   http://localhost:{}/v3/api-docs", port);
    }

}
