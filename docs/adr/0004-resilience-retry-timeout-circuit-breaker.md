# ADR-0004. Устойчивость вызовов Hotel Service

**Статус:** принято

## Решение

- `RestClient` на JDK `HttpClient`: connect-timeout 1 с, read-timeout 3 с (`app.hotel-client.*`).
  `HttpURLConnection` не используется, так как он может самостоятельно повторить POST.
- Resilience4j Retry `hotel`: 3 попытки, пауза 300 мс с множителем 2. Повторяются только тайм-ауты/IO
  и 5xx; ответы 4xx (в том числе 409) не повторяются.
- Resilience4j CircuitBreaker `hotel` на `recommend` и `confirm-availability`: окно 10 вызовов,
  порог 50 %, открыт 10 с. Компенсирующий `release` не закрыт предохранителем, чтобы компенсация
  выполнялась даже при открытом предохранителе.
- `@LoadBalanced` клиент находит Hotel Service через Eureka (`http://hotel-service`).

## Последствия

Худшее время ответа при недоступном Hotel Service ограничено (≈ 3 × 3 с + паузы), после открытия
предохранителя запросы отклоняются сразу с 503, а бронирования компенсируются.
