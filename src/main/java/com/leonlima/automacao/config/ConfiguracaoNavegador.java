package com.leonlima.automacao.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Configura o Chrome para uso com Selenium.
 *
 * Scope "prototype": cada vez que o Spring injeta o WebDriver,
 * uma nova instância é criada. O WebDriver não é thread-safe,
 * então cada requisição precisa da sua própria instância.
 */
@Configuration
@Slf4j
public class ConfiguracaoNavegador {

    @Value("${selenium.headless}")
    private boolean headless;

    @Bean
    @Scope("prototype")
    public WebDriver navegador() {
        // WebDriverManager detecta a versão do Chrome instalada e baixa o driver compatível
        WebDriverManager.chromedriver().setup();

        ChromeOptions opcoes = new ChromeOptions();

        if (headless) {
            opcoes.addArguments("--headless=new");
        }

        // Necessário em ambientes de servidor
        opcoes.addArguments("--no-sandbox");
        opcoes.addArguments("--disable-dev-shm-usage");
        opcoes.addArguments("--disable-gpu");
        opcoes.addArguments("--window-size=1920,1080");
        opcoes.addArguments("--disable-blink-features=AutomationControlled");

        log.info("Inicializando navegador — headless={}", headless);
        return new ChromeDriver(opcoes);
    }
}
