//package com.example.demo;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@Component
//public class VaultTestRunner implements CommandLineRunner {
//
//    @Value("${spring.datasource.username}")
//    private String username;
//
//    @Value("${spring.datasource.password}")
//    private String password;
//
//    @Override
//    public void run(String... args) {
//        System.out.println("Vault-injected DB username: " + username);
//        System.out.println("Vault-injected DB password: " + password);
//    }
//}
