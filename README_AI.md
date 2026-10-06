# README-AI.md — правила для AI-ассистента

> Этот документ описывает правила работы AI-ассистента в репозитории
> `ai-ready-product-recommendations`. AI-ассистент обязан следовать
> этим правилам при генерации кода, тестов, документации и конфигурации.

---

## 1. Контекст проекта

**Предметная область:** электронная коммерция.

**Задача:** сервис генерации рекомендаций сопутствующих товаров
(cross-sell) для интернет-магазина.

**Стек:**

- Java 17
- Spring Boot 3.2.x
- Maven
- JUnit 5 + Mockito
- Micrometer + Actuator

**API-контракт:**

```
POST /api/v1/products/recommendations
Content-Type: application/json

{
  "name": "string (обязательное, до 200 символов)",
  "category": "string (обязательное, до 200 символов)",
  "description": "string (опционально, до 2000 символов)",
  "price": "number (опционально, положительное)"
}
```

**Ответ:**

```json
{
  "recommendations": ["string", "..."],
  "source": "string (имя провайдера)",
  "fallback": "boolean"
}
```

---

## 2. Архитектурные слои

| Пакет | Назначение |
|---|---|
| `controller` | Приём HTTP-запросов, валидация DTO, вызов service |
| `service` | Бизнес-логика, построение prompt, sanitize ответа |
| `client` | Вызовы внешних API (каталог товаров) |
| `llm` | Абстракция над LLM: `LlmClient`, `AiGateway`, реализации |
| `dto` | Объекты передачи данных (records с валидацией) |
| `domain` | Доменные модели (records) |
| `config` | Конфигурация Spring |
| `exception` | Обработка ошибок |

---

## 3. Разрешено

1. Создавать DTO в пакете `dto` — только records с аннотациями
   `@NotBlank`, `@Size`, `@Positive`.
2. Добавлять бизнес-логику **только** в `service`-layer.
3. Вызывать внешние API **только** через `client`-layer.
4. Вызывать LLM **только** через `AiGateway` (не через `LlmClient`
   напрямую из service).
5. Писать unit-тесты для `service`-layer с моками зависимостей.
6. Писать интеграционные тесты для `controller` через `MockMvc`.
7. Обновлять OpenAPI-спецификацию при изменении публичного API.
8. Добавлять метрики через Micrometer (счётчики `llm.requests`).
9. Логировать вызовы LLM через SLF4J без чувствительных данных
   (только имя провайдера, длина prompt, статус).
10. Использовать конструкторную инъекцию зависимостей.
11. Использовать records для DTO и domain-моделей.
12. Хранить секреты **только** в переменных окружения.

---

## 4. Запрещено

1. **Хранить секреты** (API-ключи, пароли) в `application.yml`,
   в коде или в тестах. Только `${ENV_VAR:default}`.
2. **Вызывать внешние сервисы или LLM из `controller`** — нарушение
   слоёв.
3. **Вызывать LLM из бизнес-логики напрямую** — только через
   `AiGateway`.
4. **Менять публичный API** без обновления документации и тестов.
5. **Смешивать JPA entity и DTO** — DTO отдельно, entity отдельно.
6. **Удалять тесты** ради успешной сборки.
7. **Добавлять зависимости** без обоснования.
8. **Использовать `System.out.println`** вместо логгера.
9. **Логировать секреты**, prompt целиком, ответ LLM целиком
   (только длину, статус, имя провайдера).
10. **Хардкодить URL провайдеров** — только через `application.yml`.
11. **Писать `activeProvider = "openai"`** как дефолт — безопасный
    дефолт `internal`.
12. **Игнорировать ошибки** — все исключения должны логироваться
    и обрабатываться (`GlobalExceptionHandler`).

---

## 5. Конвенции кода

### Форматирование

- Отступ: 4 пробела.
- Длина строки: ≤ 120 символов.
- Пакеты: `com.example.airecommendations.*`.
- Классы: `PascalCase`.
- Методы и поля: `camelCase`.
- Константы: `UPPER_SNAKE_CASE`.

### DTO

```java
public record ProductDto(
        @NotBlank(message = "Product name is required")
        @Size(max = 200)
        String name,

        @NotBlank(message = "Category is required")
        @Size(max = 200)
        String category,

        @Size(max = 2000)
        String description,

        @Positive
        Double price
) {}
```

### Service

```java
@Service
public class ProductRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(ProductRecommendationService.class);

    private final AiGateway aiGateway;
    private final ProductCatalogClient catalogClient;

    public ProductRecommendationService(AiGateway aiGateway,
                                        ProductCatalogClient catalogClient) {
        this.aiGateway = aiGateway;
        this.catalogClient = catalogClient;
    }

    public RecommendationResponse recommend(ProductDto product) {
        validate(product);
        // ...
    }
}
```

### Тесты

```java
@ExtendWith(MockitoExtension.class)
class ProductRecommendationServiceTest {

    @Mock
    private AiGateway aiGateway;

    @Mock
    private ProductCatalogClient catalogClient;

    @InjectMocks
    private ProductRecommendationService service;

    @BeforeEach
    void setUp() {
        lenient().when(catalogClient.findSimilar(anyString()))
                .thenReturn(List.of());
    }

    @Test
    void shouldReturnParsedRecommendations() {
        // ...
    }
}
```

---

## 6. Примеры: хорошо и плохо

### Хорошо: вызов LLM через AI Gateway

```java
@Service
public class ProductRecommendationService {

    private final AiGateway aiGateway;

    public RecommendationResponse recommend(ProductDto product) {
        LlmRequest request = LlmRequest.builder()
                .prompt(buildPrompt(product))
                .maxTokens(300)
                .temperature(0.4)
                .build();
        LlmResponse response = aiGateway.generate(request);
        return new RecommendationResponse(
                parseAndSanitize(response.text()),
                response.provider(),
                response.fallback());
    }
}
```

### Плохо: прямой вызов LLM из контроллера

```java
@RestController
public class BadController {

    @Value("${openai.api.key}")   // ← секрет в коде
    private String apiKey;

    @PostMapping("/bad")
    public String bad(@RequestBody String prompt) {
        RestTemplate rt = new RestTemplate();
        // ← прямой вызов LLM из контроллера
        return rt.postForObject(
                "https://api.openai.com/v1/chat/completions",
                prompt, String.class);
    }
}
```

### Хорошо: секреты через env

```yaml
llm:
  providers:
    openai:
      api-key: ${OPENAI_API_KEY:}
```

### Плохо: секрет в application.yml

```yaml
llm:
  providers:
    openai:
      api-key: sk-abc123...   # ← секрет в репозитории
```

---

## 7. Правила для тестов

1. **Unit-тесты** для `service` — с моками `AiGateway` и
   `ProductCatalogClient`.
2. **Unit-тесты** для `AiGateway` — с моками `LlmClient`.
3. **Интеграционные тесты** для `controller` — с `@SpringBootTest`
   и `MockMvc`.
4. В интеграционных тестах **обязательно** указывать
   `@TestPropertySource(properties = {"llm.active-provider=internal"})`,
   чтобы не ходить в OpenAI.
5. Использовать `lenient()` для stub-ов, которые не всегда вызываются.
6. Проверять не только успешные сценарии, но и ошибки (валидация,
   fallback).
7. Использовать `ArgumentCaptor` для проверки содержимого prompt.

---

## 8. Правила для конфигурации

1. `application.yml` — **только** плейсхолдеры для секретов.
2. Дефолт `llm.active-provider` — **`internal`** (безопасный).
3. URL провайдеров — через env-переменные.
4. Логирование уровней — через `logging.level.*`.
5. Метрики — через `management.endpoints.web.exposure.include`.

---

## 9. Что делать при добавлении нового LLM-провайдера

1. Создать класс `XxxLlmClient implements LlmClient` в пакете `llm`.
2. Пометить `@Component`.
3. Реализовать `providerName()` и `generate()`.
4. Добавить секцию в `application.yml`:

```yaml
llm:
  providers:
    xxx:
      base-url: ${XXX_BASE_URL:...}
      api-key: ${XXX_API_KEY:}
      model: ${XXX_MODEL:...}
```

5. **Не трогать** `AiGateway`, `Service`, `Controller` — они работают
   через абстракцию `LlmClient`.
6. Добавить unit-тест для нового клиента.
7. Обновить README-AI.md и ADR.

---

## 10. Контрольный список для AI-ассистента

Перед генерацией кода проверь:

- [ ] Пакет соответствует слою (`controller` / `service` / `llm` / ...)?
- [ ] Нет ли прямого вызова LLM из `controller` или `service`?
- [ ] Нет ли секретов в коде или `application.yml`?
- [ ] Используется ли `AiGateway` для вызовов LLM?
- [ ] Есть ли валидация входных данных?
- [ ] Есть ли обработка ошибок и fallback?
- [ ] Есть ли логирование без чувствительных данных?
- [ ] Есть ли метрики?
- [ ] Есть ли unit-тесты?
- [ ] Обновлена ли документация?

Если хотя бы один пункт не выполнен — **не генерируй код**,
а сначала исправь.