# Схема баз данных

Схемы создаются Hibernate (`ddl-auto: create-drop`) в отдельных in-memory H2 каждого сервиса.
Связи между сервисами (`bookings.room_id` → `rooms.id`) логические, без внешних ключей: у каждого сервиса своя БД.

## Booking Service (`jdbc:h2:mem:bookings`)

```mermaid
erDiagram
    users ||--o{ bookings : has
    users {
        bigint id PK
        varchar(50) username UK
        varchar(100) password "BCrypt"
        varchar(16) role "USER | ADMIN"
        timestamp created_at
    }
    bookings {
        bigint id PK
        bigint user_id FK
        bigint room_id "id номера в Hotel Service"
        date start_date
        date end_date
        varchar(16) status "PENDING | CONFIRMED | CANCELLED"
        varchar(64) request_id UK "ключ идемпотентности"
        timestamp created_at
        timestamp updated_at
        bigint version "оптимистичная блокировка"
    }
```

| Таблица | Ограничения и индексы | Зачем |
|---|---|---|
| users | `uk_users_username` | Уникальный логин |
| bookings | `uk_bookings_request_id` | Повторная доставка не создаёт дубликат |
| bookings | `idx_bookings_user_created (user_id, created_at)` | История пользователя с сортировкой |
| bookings | `idx_bookings_status_created (status, created_at)` | Поиск зависших `PENDING` |
| bookings | FK `user_id → users.id` | Нельзя удалить пользователя с бронированиями |

## Hotel Service (`jdbc:h2:mem:hotels`)

```mermaid
erDiagram
    hotels ||--o{ rooms : contains
    rooms ||--o{ room_reservations : locks
    hotels {
        bigint id PK
        varchar(120) name
        varchar(255) address
        varchar(80) city
    }
    rooms {
        bigint id PK
        bigint hotel_id FK
        varchar(20) number
        int capacity
        decimal price_per_night
        boolean available "операционная доступность"
        bigint times_booked "статистика для рекомендаций"
    }
    room_reservations {
        bigint id PK
        bigint room_id FK
        varchar(64) request_id UK
        bigint booking_id
        date start_date
        date end_date
        varchar(16) status "ACTIVE | RELEASED"
        timestamp created_at
    }
```

| Таблица | Ограничения и индексы | Зачем |
|---|---|---|
| hotels | `idx_hotels_city` | Фильтр по городу |
| rooms | `uk_rooms_hotel_number (hotel_id, number)` | Номер уникален внутри отеля |
| rooms | `idx_rooms_times_booked (times_booked, id)` | Сортировка рекомендаций |
| room_reservations | `uk_reservations_request_id` | Идемпотентность confirm/release |
| room_reservations | `idx_reservations_room_period (room_id, status, start_date, end_date)` | Быстрая проверка пересечения дат |

`rooms.available` отражает только операционную доступность (ремонт, вывод из эксплуатации).
Занятость по датам определяется активными записями `room_reservations` (`status = ACTIVE`),
которые являются отражением подтверждённых бронирований Booking Service.
Запись со статусом `RELEASED` без дат — маркер компенсации, пришедшей раньше подтверждения.
