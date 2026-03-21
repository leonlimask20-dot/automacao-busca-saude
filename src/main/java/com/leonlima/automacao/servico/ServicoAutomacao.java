package com.leonlima.automacao.servico;

import com.leonlima.automacao.dto.BuscaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Executa buscas automatizadas em portais de saúde com Selenium.
 *
 * O Selenium controla um browser real — diferente do JSOUP, ele executa
 * JavaScript, aguarda carregamentos dinâmicos e interage com a página
 * exatamente como um usuário faria.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServicoAutomacao {

    private final ApplicationContext contexto;

    @Value("${selenium.tempo-espera-segundos}")
    private int tempoEsperaSegundos;

    public BuscaDTO.Resposta buscar(BuscaDTO.Requisicao requisicao) {
        long inicio = System.currentTimeMillis();
        WebDriver navegador = null;

        try {
            // Cada requisição obtém sua própria instância do Chrome (scope prototype)
            navegador = contexto.getBean(WebDriver.class);

            log.info("Buscando '{}' em '{}'", requisicao.getTermo(), requisicao.getSitioAlvo());

            // WebDriverWait: aguarda condições antes de prosseguir
            // Mais confiável que Thread.sleep() porque espera exatamente até o elemento estar disponível
            WebDriverWait espera = new WebDriverWait(navegador, Duration.ofSeconds(tempoEsperaSegundos));

            navegador.get("https://bula.fiocruz.br/");
            log.info("Página aberta: {}", navegador.getTitle());

            // Localiza o campo de busca — By.cssSelector funciona igual ao querySelector do JavaScript
            WebElement campoBusca = espera.until(
                ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[type='search'], input[name='q'], input[placeholder*='busca']")
                )
            );

            // sendKeys simula o usuário digitando no campo
            campoBusca.clear();
            campoBusca.sendKeys(requisicao.getTermo());
            campoBusca.sendKeys(Keys.ENTER);  // simula a tecla Enter

            // Aguarda os resultados aparecerem antes de tentar extraí-los
            espera.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("h2, h3, article, .resultado")
            ));

            List<BuscaDTO.ItemResultado> resultados = extrairResultados(navegador);
            long tempoExecucao = System.currentTimeMillis() - inicio;

            log.info("Busca concluída — {} resultados em {}ms", resultados.size(), tempoExecucao);

            return BuscaDTO.Resposta.builder()
                    .termo(requisicao.getTermo())
                    .sitioAlvo(requisicao.getSitioAlvo())
                    .totalResultados(resultados.size())
                    .resultados(resultados)
                    .tempoExecucaoMs(tempoExecucao)
                    .executadoEm(LocalDateTime.now())
                    .status("SUCESSO")
                    .build();

        } catch (TimeoutException e) {
            log.warn("Timeout ao aguardar elementos: {}", e.getMessage());
            return respostaDeErro(requisicao, "Timeout: página demorou para carregar",
                    System.currentTimeMillis() - inicio);

        } catch (WebDriverException e) {
            log.error("Erro no navegador: {}", e.getMessage());
            return respostaDeErro(requisicao, "Erro no navegador: " + e.getMessage(),
                    System.currentTimeMillis() - inicio);

        } finally {
            // O navegador DEVE ser fechado sempre — mesmo em caso de exceção
            // Sem o quit(), o processo do Chrome permanece em memória indefinidamente
            if (navegador != null) {
                navegador.quit();
                log.info("Navegador encerrado");
            }
        }
    }

    private List<BuscaDTO.ItemResultado> extrairResultados(WebDriver navegador) {
        List<BuscaDTO.ItemResultado> resultados = new ArrayList<>();

        // Tenta diferentes seletores — cada site tem sua própria estrutura HTML
        List<WebElement> elementos = navegador.findElements(By.cssSelector("h2 a, h3 a"));
        if (elementos.isEmpty()) elementos = navegador.findElements(By.cssSelector("h2, h3"));

        int limite = Math.min(elementos.size(), 10);

        for (int i = 0; i < limite; i++) {
            WebElement el = elementos.get(i);
            try {
                String titulo = el.getText().trim();
                if (titulo.isEmpty()) continue;

                String href = "";
                try {
                    href = el.getAttribute("href") != null
                        ? el.getAttribute("href")
                        : el.findElement(By.tagName("a")).getAttribute("href");
                } catch (NoSuchElementException ignorado) {}

                String descricao = "";
                try {
                    WebElement pai = el.findElement(By.xpath(".."));
                    descricao = pai.findElement(By.cssSelector("p, .descricao")).getText().trim();
                } catch (NoSuchElementException ignorado) {}

                resultados.add(BuscaDTO.ItemResultado.builder()
                        .titulo(titulo)
                        .descricao(descricao)
                        .url(href)
                        .build());

            } catch (StaleElementReferenceException e) {
                // Ocorre quando a página atualiza o DOM enquanto estamos lendo
                log.warn("Elemento obsoleto ao extrair resultado {}", i);
            }
        }

        return resultados;
    }

    private BuscaDTO.Resposta respostaDeErro(BuscaDTO.Requisicao requisicao, String erro, long tempo) {
        return BuscaDTO.Resposta.builder()
                .termo(requisicao.getTermo())
                .sitioAlvo(requisicao.getSitioAlvo())
                .totalResultados(0)
                .resultados(List.of())
                .tempoExecucaoMs(tempo)
                .executadoEm(LocalDateTime.now())
                .status("ERRO")
                .mensagemErro(erro)
                .build();
    }
}
