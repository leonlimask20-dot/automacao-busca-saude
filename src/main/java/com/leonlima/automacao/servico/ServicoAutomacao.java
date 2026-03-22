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
 * Executa buscas automatizadas com Selenium.
 *
 * Usa a Wikipedia em português — site público, sem bloqueio de automação,
 * com conteúdo rico sobre medicamentos para demonstração.
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
            navegador = contexto.getBean(WebDriver.class);

            log.info("Buscando '{}' em '{}'", requisicao.getTermo(), requisicao.getSitioAlvo());

            WebDriverWait espera = new WebDriverWait(navegador, Duration.ofSeconds(tempoEsperaSegundos));

            // Wikipedia: busca pública sem bloqueio de automação
            String urlBusca = "https://pt.wikipedia.org/w/index.php?search="
                    + requisicao.getTermo().replace(" ", "+")
                    + "&title=Especial%3APesquisar&ns0=1";

            navegador.get(urlBusca);
            log.info("Pagina aberta: {}", navegador.getTitle());

            // Aguarda resultados ou redireciona direto para o artigo
            try {
                espera.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.cssSelector(".mw-search-result-heading a, #firstHeading, h1")
                ));
            } catch (TimeoutException e) {
                log.warn("Seletor principal nao encontrado, aguardando body");
                espera.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            }

            List<BuscaDTO.ItemResultado> resultados = extrairResultados(navegador);
            long tempoExecucao = System.currentTimeMillis() - inicio;

            log.info("Busca concluida — {} resultados em {}ms", resultados.size(), tempoExecucao);

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
            log.warn("Timeout: {}", e.getMessage());
            return respostaDeErro(requisicao, "Timeout: pagina demorou para carregar",
                    System.currentTimeMillis() - inicio);

        } catch (WebDriverException e) {
            log.error("Erro no navegador: {}", e.getMessage());
            return respostaDeErro(requisicao, "Erro no navegador: " + e.getMessage(),
                    System.currentTimeMillis() - inicio);

        } finally {
            if (navegador != null) {
                navegador.quit();
                log.info("Navegador encerrado");
            }
        }
    }

    private List<BuscaDTO.ItemResultado> extrairResultados(WebDriver navegador) {
        List<BuscaDTO.ItemResultado> resultados = new ArrayList<>();

        // Caso 1: página de resultados de busca da Wikipedia
        List<WebElement> itensResultado = navegador.findElements(
            By.cssSelector(".mw-search-result-heading a")
        );

        if (!itensResultado.isEmpty()) {
            log.info("Modo lista de resultados — {} itens", itensResultado.size());
            int limite = Math.min(itensResultado.size(), 10);
            for (int i = 0; i < limite; i++) {
                try {
                    WebElement el = itensResultado.get(i);
                    String titulo = el.getText().trim();
                    String href = el.getAttribute("href");

                    String descricao = "";
                    try {
                        WebElement pai = el.findElement(By.xpath("../../.."));
                        WebElement snippet = pai.findElement(By.cssSelector(".searchresult"));
                        descricao = snippet.getText().trim();
                        if (descricao.length() > 200) descricao = descricao.substring(0, 200) + "...";
                    } catch (NoSuchElementException ignorado) {}

                    if (!titulo.isEmpty()) {
                        resultados.add(BuscaDTO.ItemResultado.builder()
                                .titulo(titulo)
                                .descricao(descricao)
                                .url(href != null ? href : "")
                                .build());
                    }
                } catch (StaleElementReferenceException e) {
                    log.warn("Elemento obsoleto {}", i);
                }
            }
        } else {
            // Caso 2: redirecionou direto para o artigo
            log.info("Modo artigo direto — extraindo secoes");
            try {
                String titulo = navegador.findElement(By.cssSelector("#firstHeading, h1")).getText();
                List<WebElement> paragrafos = navegador.findElements(By.cssSelector("#mw-content-text p"));
                String descricao = paragrafos.isEmpty() ? "" :
                    paragrafos.get(0).getText().trim();
                if (descricao.length() > 300) descricao = descricao.substring(0, 300) + "...";

                resultados.add(BuscaDTO.ItemResultado.builder()
                        .titulo(titulo)
                        .descricao(descricao)
                        .url(navegador.getCurrentUrl())
                        .build());

                // Adiciona secoes do artigo como resultados
                List<WebElement> secoes = navegador.findElements(By.cssSelector("h2 .mw-headline, h3 .mw-headline"));
                for (int i = 0; i < Math.min(secoes.size(), 8); i++) {
                    String nomeSecao = secoes.get(i).getText().trim();
                    if (!nomeSecao.isEmpty() && !nomeSecao.equals("Ver também") && !nomeSecao.equals("Referências")) {
                        resultados.add(BuscaDTO.ItemResultado.builder()
                                .titulo(titulo + " — " + nomeSecao)
                                .descricao("Seção do artigo sobre " + titulo)
                                .url(navegador.getCurrentUrl())
                                .build());
                    }
                }
            } catch (NoSuchElementException e) {
                log.warn("Nenhum elemento encontrado na pagina");
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
