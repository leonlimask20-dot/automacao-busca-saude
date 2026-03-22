# Automação de Busca em Saúde

Automação de buscas em portais de saúde com Selenium WebDriver integrado ao Spring Boot. A automação é exposta como um serviço REST — qualquer sistema pode chamar a API e receber os resultados como JSON.

---

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.3 |
| Selenium | 4.18.1 |
| WebDriverManager | 5.7.0 |
| JUnit 5 + Mockito | — |

---

## Como o Selenium funciona

O Selenium controla um browser real (Chrome) via WebDriver:

```java
// Abre o Chrome
WebDriver navegador = new ChromeDriver(opcoes);

// Navega para uma URL
navegador.get("https://bula.fiocruz.br/");

// Localiza o campo de busca com seletor CSS
WebElement campo = navegador.findElement(By.cssSelector("input[type='search']"));

// Simula o usuário digitando e pressionando Enter
campo.sendKeys("paracetamol");
campo.sendKeys(Keys.ENTER);

// Aguarda os resultados carregarem (espera explícita)
WebDriverWait espera = new WebDriverWait(navegador, Duration.ofSeconds(15));
espera.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("h2")));

// Extrai os dados
List<WebElement> resultados = navegador.findElements(By.cssSelector("h2"));
for (WebElement el : resultados) {
    System.out.println(el.getText());
}

// Fecha o browser ao final — obrigatório para liberar memória
navegador.quit();
```

---

## JSOUP vs Selenium

| | JSOUP | Selenium |
|---|---|---|
| Como funciona | Parse do HTML estático | Controla browser real |
| Executa JavaScript | ❌ | ✅ |
| Velocidade | Muito rápido | Mais lento |
| Sites estáticos (SSR) | ✅ Ideal | Funciona |
| SPAs (React, Angular) | ❌ | ✅ Necessário |

---

## Arquitetura

```
src/main/java/com/leonlima/automacao/
├── config/      → ConfiguracaoNavegador (Chrome + WebDriverManager)
├── controller/  → ControladorBusca (endpoint REST)
├── servico/     → ServicoAutomacao (lógica Selenium)
├── dto/         → BuscaDTO (requisição e resposta)
└── excecao/     → TratadorDeExcecoes
```

---

## Como executar

Google Chrome deve estar instalado. O WebDriverManager baixa o ChromeDriver automaticamente.

```bash
mvn spring-boot:run
```

API disponível em `http://localhost:8085`.

---

## Endpoint

### POST /api/busca

```bash
curl -X POST http://localhost:8085/api/busca \
  -H "Content-Type: application/json" \
  -d '{"termo": "paracetamol"}'
```

```json
{
  "termo": "paracetamol",
  "sitioAlvo": "bula.fiocruz.br",
  "totalResultados": 8,
  "resultados": [
    {
      "titulo": "Paracetamol 500mg — Bula completa",
      "descricao": "Analgésico e antitérmico indicado para...",
      "url": "https://bula.fiocruz.br/..."
    }
  ],
  "tempoExecucaoMs": 4230,
  "executadoEm": "2025-01-01T10:00:00",
  "status": "SUCESSO"
}
```

---

## Testes

```bash
mvn test
```

Os testes unitários usam Mockito para simular o WebDriver — sem abrir o Chrome, sem depender de sites externos.

---

## Autor

**LNL**
GitHub: [@leonlimask20-dot](https://github.com/leonlimask20-dot)
Email: leonlimask@gmail.com
