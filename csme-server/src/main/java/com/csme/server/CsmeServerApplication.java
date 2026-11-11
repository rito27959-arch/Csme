package com.csme.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Point d'entrée de l'API CSME ERP. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CsmeServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CsmeServerApplication.class, args);
    }
}
