# ADR 0002: Абстракция LlmClient

## Статус

Принято.

## Контекст

Сервис должен поддерживать **несколько LLM-провайдеров**:

- `OpenAiLlmClient` — внешний провайдер (OpenAI API);
- `InternalLlmClient` — внутренний/заглушечный провайдер.

Без абстракции пришлось бы писать `if/else` в `AiGateway`
и дублировать логику обработки ответа. Это нарушает OCP
(Open/Closed Principle): добавление нового провайдера требовало бы
изменения существующего кода.

## Решение

Ввести интерфейс `LlmClient`:

```java
public interface LlmClient {
    String providerName();
    LlmResponse generate(LlmRequest request);
}
```

Каждый провайдер реализует этот интерфейс и помечается `@Component`.
Spring автоматически собирает все реализации в `List<LlmClient>`,
который инжектится в `AiGateway`.

`AiGateway` строит `Map<String, LlmClient>` по имени провайдера
и выбирает нужный по конфигурации.

## Последствия

### Положительные

- **OCP** — добавление нового провайдера не требует изменения
  `AiGateway` или `Service`.
- **DIP** (Dependency Inversion Principle) — `Service` зависит
  от абстракции, а не от конкретной реализации.
- **Тестируемость** — легко подменить `LlmClient` моком.
- **Единый контракт** — `LlmRequest` / `LlmResponse` для всех
  провайдеров.

### Отрицательные

- **Ограничение контракта** — все провайдеры должны возвращать
  ответ в формате `LlmResponse`. Специфичные поля (например,
  `finish_reason` у OpenAI) теряются.

### Нейтральные

- Требуется регистрация всех реализаций как Spring-бинов.
- Имя провайдера должно быть уникальным (иначе `Collectors.toMap`
  упадёт с `IllegalStateException`).

## Альтернативы

- **Абстрактный класс `AbstractLlmClient`** — избыточно, интерфейса
  достаточно.
- **Функциональный интерфейс** — не подходит, нужны два метода.
- **SPI (Service Provider Interface)** — избыточно для двух
  провайдеров.

## Пример использования

```java
@Component
public class OpenAiLlmClient implements LlmClient {

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        // HTTP-вызов OpenAI
        return LlmResponse.of(text, providerName());
    }
}
```
