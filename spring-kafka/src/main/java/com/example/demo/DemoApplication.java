// package com.example.demo;

// import org.springframework.boot.CommandLineRunner;
// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;

// import javax.sql.DataSource;
// import java.sql.Connection;

// @SpringBootApplication
// public class DemoApplication implements CommandLineRunner {

//     private final DataSource dataSource;

//     public DemoApplication(DataSource dataSource) {
//         this.dataSource = dataSource;
//     }

//     public static void main(String[] args) {
//         SpringApplication.run(DemoApplication.class, args);
//     }

//     @Override
//     public void run(String... args) throws Exception {
//         try (Connection conn = dataSource.getConnection()) {
//             System.out.println("Successfully connected to DB: " + conn.getMetaData().getURL());
//         }
//     }
// }


package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    // Make this bean refreshable
    @Bean
    @RefreshScope
    public DataSource dataSource(DataSourceProperties properties) {

        System.out.println("=== DB Credentials ===");
        System.out.println("URL: " + properties.getUrl());
        System.out.println("Username: " + properties.getUsername());
        System.out.println("Password: " + properties.getPassword());
        System.out.println("=====================");

        
        return DataSourceBuilder.create()
                .url(properties.getUrl())
                .username(properties.getUsername())
                .password(properties.getPassword())
                .build();
    }
}
