# Система управления заказами и платежами

Микросервисная система для обработки заказов и управления платежами. Тут всё сделано аналогично прошлой дз, но 
был использован асинхронный подход через rabbitMQ. Также были реализованые все паттерны Transactional box, согласно тз.
В остальном всё сделал максимально красиво, кроме фронтенда, т.к я ненавижу всё что связано с JS. Если будут вопросы
или что-то вдруг не поднимается, напишите мне в тг пжлст @lopata1239. 

## Архитектура

Система состоит из следующих компонентов:

- **API Gateway** (порт 8080) - единая точка входа для всех запросов
- **Order Service** - сервис для управления заказами
- **Payment Service** - сервис для управления платежами
- **Eureka Server** (порт 8761) - сервис регистрации и обнаружения микросервисов
- **PostgreSQL** (порт 5432) - база данных для хранения данных о заказах и платежах
- **RabbitMQ** (порт 5672) - брокер сообщений для асинхронной коммуникации между сервисами

## Функциональность

### Order Service
- Создание заказов
- Получение информации о заказах
- Получение списка заказов пользователя
- Проверка статуса заказа

### Payment Service
- Создание платежных аккаунтов
- Пополнение баланса
- Проверка баланса
- Проверка существования аккаунта

## Технологии

- Java 21
- Spring Boot
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka
- Spring Data JPA
- PostgreSQL
- RabbitMQ
- Swagger/OpenAPI
- Gradle

## Запуск проекта

1. Поднимите все микросервисы:
```bash
docker-compose up --build
```

## API Documentation

Swagger UI доступен по следующим адресам:
- Order Service: http://localhost:8080/order/swagger-ui/index.html#
- Payment Service: http://localhost:8080/payment/swagger-ui/index.html#

## Postman Collection

В проекте есть готовая Postman коллекция (`postmanCollection.json`), которая содержит все необходимые запросы для тестирования API.
(там могут быть ошибки, мне было очень лениво её делать, т.к через сваггер удобней и понятней)

### Основные эндпоинты

#### Order Service
- `POST /order/api/orders` - создание нового заказа
- `GET /order/api/orders` - получение списка заказов пользователя
- `GET /order/api/orders/{orderId}` - получение информации о конкретном заказе
- `GET /order/api/health/ping` - проверка работоспособности сервиса

#### Payment Service
- `POST /payment/api/accounts` - создание платежного аккаунта
- `POST /payment/api/accounts/{userId}/deposit` - пополнение баланса
- `GET /payment/api/accounts/{userId}/balance` - проверка баланса
- `GET /payment/api/accounts/check/{userId}` - проверка существования аккаунта
- `GET /payment/api/health/ping` - проверка работоспособности сервиса

## Разработка

### Структура проекта
```
.
├── api-gateway
├── frontend
├── eureka-server
├── order-service
├── payment-service
└── gradle
```

### Запуск тестов
```bash
gradle :order-service:test :payment-service:test
```

Покрытие также можно найти после этого в папке reports, покрытие везде больше 15%
