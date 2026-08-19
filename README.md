# Warehouse Management System

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Authentication-000000?style=for-the-badge&logo=jsonwebtokens)

Backend para gerenciamento de **estoque, depósitos e pedidos de um centro de distribuição**, desenvolvido com **Java 21 e Spring Boot 3.5**.

O projeto utiliza uma arquitetura de **monólito modular** e aborda problemas reais de backend, como **concorrência, consistência transacional, idempotência, segurança e rastreabilidade**.

## Tecnologias

- Java 21
- Spring Boot 3.5
- Maven
- PostgreSQL
- Spring Data JPA / Hibernate
- Flyway
- Spring Security / JWT
- Bean Validation
- JUnit 5 / Mockito
- Testcontainers
- Docker / Docker Compose
- OpenAPI / Swagger
- Actuator / Micrometer / Prometheus

## Funcionalidades

- Cadastro de produtos e depósitos
- Entrada e saída de estoque
- Controle de saldo físico, reservado e disponível
- Reserva e liberação de estoque
- Transferência entre depósitos
- Criação, confirmação e cancelamento de pedidos
- Histórico de movimentações
- Idempotência por referência externa
- Autenticação JWT e autorização por roles
- Logs, métricas e observabilidade
- Testes de domínio e concorrência

## Arquitetura

```text
com.example.wms
├── product
├── warehouse
├── inventory
├── order
├── identity
└── shared
````

Cada módulo é organizado em:

```text
domain
application
infrastructure
presentation
```

As regras de negócio são mantidas desacopladas de **Spring, JPA e HTTP**.

## Executando

```bash
cp .env.example .env
docker compose up --build
```

API:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

Health Check:

```text
http://localhost:8080/actuator/health
```

## Testes

```bash
mvn clean verify
```

Os testes de concorrência utilizam **Testcontainers com PostgreSQL real**.

## Roadmap

* Gestão de usuários
* Refresh e revogação de tokens
* Expiração automática de reservas
* Estoque avariado e bloqueado
* Transferências em trânsito
* Outbox e mensageria
* Testes de carga
* Métricas e SLOs de produção

## Documentação

As principais decisões arquiteturais estão disponíveis em:

```text
docs/adr
```

```
```
