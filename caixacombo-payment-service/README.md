# CaixaCombo Payment Service

Microserviço de pagamentos e relatórios construído com Spring Boot.

## Funcionalidades

- **Processamento de Pagamentos** via Stone API
- **Cancelamento e Estorno** de pagamentos
- **Webhooks** para notificações da Stone
- **Relatórios** em PDF e Excel
- **Dashboard** com métricas em tempo real
- **Auditoria** completa de todas as operações
- **Cache** com Redis para alta performance
- **Filas** com RabbitMQ para comunicação assíncrona

## Tecnologias

- Java 17
- Spring Boot 3.2
- Spring Security + JWT
- Spring Data JPA + PostgreSQL
- Spring AMQP (RabbitMQ)
- Spring Cache (Redis)
- Flyway (migrations)
- JasperReports (PDF)
- Apache POI (Excel)
- OpenAPI/Swagger

## Pré-requisitos

- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Redis 7+
- RabbitMQ 3.12+

## Configuração

### Variáveis de Ambiente

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/caixacombo_payments
SPRING_DATASOURCE_USERNAME=caixacombo
SPRING_DATASOURCE_PASSWORD=caixacombo123

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# RabbitMQ
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest

# JWT
JWT_SECRET=your-secret-key-here

# Stone API
STONE_API_URL=https://api.stone.com.br
STONE_API_KEY=your-api-key
STONE_MERCHANT_ID=your-merchant-id
STONE_ENVIRONMENT=PRODUCTION

# Node.js API
NODEJS_API_URL=http://localhost:3001
```

## Executar

### Desenvolvimento

```bash
# Compilar
mvn clean install

# Executar
mvn spring-boot:run

# Ou com Java
java -jar target/payment-service-1.0.0.jar
```

### Docker

```bash
# Build
docker build -t caixacombo-payment-service .

# Executar
docker run -p 8080:8080 caixacombo-payment-service
```

## API Documentation

Após iniciar, acesse:
- Swagger UI: http://localhost:8080/api/v1/swagger-ui.html
- OpenAPI: http://localhost:8080/api/v1/v3/api-docs

## Endpoints Principais

### Pagamentos

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/payments` | Criar pagamento |
| GET | `/payments/{id}` | Buscar pagamento |
| GET | `/payments/empresa/{empresaId}` | Listar pagamentos |
| POST | `/payments/{id}/cancel` | Cancelar pagamento |
| POST | `/payments/{id}/refund` | Estornar pagamento |

### Webhooks

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/webhooks/stone` | Webhook da Stone |
| POST | `/webhooks/nodejs` | Webhook do Node.js |

### Relatórios

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/reports/dashboard/{empresaId}` | Dashboard |
| POST | `/reports/pdf` | Gerar PDF |
| POST | `/reports/excel` | Gerar Excel |

### Auditoria

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/audit/empresa/{empresaId}` | Logs de auditoria |

## Integração com Node.js

O microserviço se comunica com o Node.js via:

1. **RabbitMQ** - Eventos assíncronos (recomendado)
2. **HTTP** - Webhooks síncronos (fallback)

### Eventos RabbitMQ

- `payment.approved` - Pagamento aprovado
- `payment.declined` - Pagamento recusado
- `payment.cancelled` - Pagamento cancelado
- `payment.refunded` - Pagamento estornado

## Arquitetura

```
┌─────────────┐     REST/WS      ┌──────────────────┐
│ Android POS │ ◄──────────────► │   Node.js API    │ ◄──► MongoDB
└─────────────┘                  └──────────────────┘
                                       ▲
                                       │ HTTP/RabbitMQ
                                       ▼
                                 ┌──────────────────┐
                                 │  Spring Boot     │
                                 │  (Payment Svc)   │ ◄──► PostgreSQL
                                 └──────────────────┘ ◄──► Redis
                                                        ◄──► RabbitMQ
```

## License

Proprietary - CaixaCombo
