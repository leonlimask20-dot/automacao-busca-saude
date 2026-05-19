# Health Search Automation

![CI](https://github.com/leonlimask20-dot/automacao-busca-saude/actions/workflows/ci.yml/badge.svg)

Automation of searches on health portals with Selenium WebDriver integrated
into Spring Boot. The automation is exposed as a REST service — any system can
call the API and receive the results as JSON.

---

## Tech stack

| Technology | Version |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.3 |
| Selenium | 4.18.1 |
| WebDriverManager | 5.7.0 |
| JUnit 5 + Mockito | — |

---

## How Selenium works

Selenium drives a real browser (Chrome) through the WebDriver:

```java
// Open Chrome
WebDriver browser = new ChromeDriver(options);

// Navigate to a URL
browser.get("https://bula.fiocruz.br/");

// Locate the search field with a CSS selector
WebElement field = browser.findElement(By.cssSelector("input[type='search']"));

// Simulate the user typing and pressing Enter
field.sendKeys("paracetamol");
field.sendKeys(Keys.ENTER);

// Wait for the results to load (explicit wait)
WebDriverWait wait = new WebDriverWait(browser, Duration.ofSeconds(15));
wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("h2")));

// Extract the data
List<WebElement> results = browser.findElements(By.cssSelector("h2"));
for (WebElement el : results) {
    System.out.println(el.getText());
}

// Close the browser at the end — mandatory to free memory
browser.quit();
```

---

## JSOUP vs Selenium

| | JSOUP | Selenium |
|---|---|---|
| How it works | Parses static HTML | Drives a real browser |
| Runs JavaScript | ❌ | ✅ |
| Speed | Very fast | Slower |
| Static sites (SSR) | ✅ Ideal | Works |
| SPAs (React, Angular) | ❌ | ✅ Required |

---

## Architecture

```
src/main/java/com/leonlima/automacao/
├── config/      → ConfiguracaoNavegador (Chrome + WebDriverManager)
├── controller/  → ControladorBusca (REST endpoint)
├── servico/     → ServicoAutomacao (Selenium logic)
├── dto/         → BuscaDTO (request and response)
└── excecao/     → TratadorDeExcecoes
```

---

## How to run

Google Chrome must be installed. WebDriverManager downloads ChromeDriver automatically.

```bash
mvn spring-boot:run
```

API available at `http://localhost:8085`.

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
      "titulo": "Paracetamol 500mg — Full leaflet",
      "descricao": "Analgesic and antipyretic indicated for...",
      "url": "https://bula.fiocruz.br/..."
    }
  ],
  "tempoExecucaoMs": 4230,
  "executadoEm": "2025-01-01T10:00:00",
  "status": "SUCESSO"
}
```

---

## Tests

```bash
mvn test
```

The unit tests use Mockito to simulate the WebDriver — without opening Chrome
and without depending on external sites.

---

## 🤖 Agent Architecture

This project was built and code-reviewed using a **multi-agent
context-optimization workflow**: specialized AI agents each audit a single
slice of the codebase — browser config, automation logic, REST layer, tests —
within a strict context budget. The approach cuts review time and token cost
while keeping full traceability of every finding.

Methodology, agent templates and the full playbook: **[leonlim3.gumroad.com](https://leonlim3.gumroad.com)**

---

## Author

**LNL**
GitHub: [@leonlimask20-dot](https://github.com/leonlimask20-dot)
Email: leonlimask@gmail.com
