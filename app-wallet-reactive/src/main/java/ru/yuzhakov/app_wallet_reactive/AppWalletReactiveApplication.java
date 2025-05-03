package ru.yuzhakov.app_wallet_reactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableWebFlux
public class AppWalletReactiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppWalletReactiveApplication.class, args);
    }
}
