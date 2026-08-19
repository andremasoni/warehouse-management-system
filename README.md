# Warehouse Management System

Backend educacional e executável para estoque e pedidos de um centro de distribuição. A versão atual é um monólito modular em Java 21 e Spring Boot 3.5, com PostgreSQL, Flyway, JWT, observabilidade e testes.

## O que está implementado

- cadastro e consulta de produtos e depósitos;
- entrada e saída manual de estoque;
- saldo físico, reservado e disponível por produto/depósito;
- reserva e liberação de unidades;
- transferência atômica entre depósitos;
- criação de pedido com reserva atômica de todos os itens;
- confirmação e cancelamento de pedidos;
- histórico paginado de movimentações;
- idempotência por referência externa;
- autenticação JWT e autorização por caso de uso;
- logs com correlation ID, Actuator, Micrometer e Prometheus;
- schema versionado, constraints de integridade e índices;
- testes de domínio e teste de concorrência com PostgreSQL real.

## Arquitetura

O código é organizado primeiro por contexto de negócio e depois por responsabilidade:

```text
com.example.wms
├── product
├── warehouse
├── inventory
├── order
├── identity
└── shared
```

Em cada módulo, `domain` contém regras sem Spring/JPA; `application` contém casos de uso e portas; `infrastructure` implementa persistência e detalhes técnicos; `presentation` adapta HTTP. Os serviços de aplicação são classes Java puras e recebem transação e autorização por portas.

Dependências de negócio:

```text
order -> inventory -> product
                  -> warehouse
identity -> autoriza casos de uso, sem entrar no domínio de estoque
```

Reservas e movimentações pertencem a `inventory`, porque fazem parte da mesma invariância quantitativa. O saldo é uma visão atual; `stock_movements` é o registro rastreável e append-only pela aplicação.

## Invariantes principais

- `onHand >= 0`;
- `reserved >= 0`;
- `reserved <= onHand`;
- `available = onHand - reserved`;
- apenas reserva ativa pode ser liberada ou consumida;
- um pedido só é confirmado após consumir todas as reservas;
- um pedido cancelado não pode ser confirmado;
- produtos não podem se repetir no mesmo pedido;
- uma referência externa não gera duas movimentações;
- transferências bloqueiam saldos em ordem determinística para reduzir deadlocks.

Essas regras existem no domínio e, quando possível, também em constraints do PostgreSQL. A duplicação aqui é defesa em profundidade, não duplicação acidental.

## Executar com Docker

Requisitos: Docker com Compose.

```bash
cp .env.example .env
# altere senhas e JWT_SECRET no .env
docker compose up --build
```

A API ficará em `http://localhost:8080`; Swagger UI em `http://localhost:8080/swagger-ui.html`; health check em `http://localhost:8080/actuator/health`.

Para desenvolvimento, se as variáveis não forem definidas, um administrador `admin`/`admin123` é criado na primeira inicialização. Não use esses valores fora do ambiente local.

## Primeiro fluxo pela API

Obtenha um token:

```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

Use `Authorization: Bearer <token>` nas próximas chamadas. A ordem recomendada é:

1. `POST /api/products`;
2. `POST /api/warehouses`;
3. `POST /api/inventory/receipts`;
4. `POST /api/orders`;
5. `POST /api/orders/{id}/confirmation` ou `/cancellation`;
6. `GET /api/inventory/movements`.

Toda operação mutável recebe `externalReference`. Repetir a mesma solicitação com a mesma referência não cria uma segunda alteração.

## Perfis

- `ADMIN`: todas as operações;
- `STOCK_OPERATOR`: depósitos, entradas, saídas, reservas e transferências;
- `ORDER_OPERATOR`: pedidos e operações de reserva necessárias ao pedido;
- `VIEWER`: consultas.

Nesta versão, apenas o administrador inicial é criado automaticamente. Gestão de usuários é uma evolução planejada; inserir usuários diretamente no banco não é uma API suportada.

## Testes e build

```bash
mvn clean verify
```

Testes de domínio não iniciam Spring. O teste de concorrência usa Testcontainers/PostgreSQL e é ignorado automaticamente quando Docker não está disponível.

## Decisões e limites

ADRs estão em [`docs/adr`](docs/adr). Limitações conhecidas:

- não há expiração automática de reservas;
- pedidos usam um único depósito e não permitem atendimento parcial;
- não há posições internas/bin locations dentro de um depósito;
- transferências são instantâneas, sem estado “em trânsito”;
- não há endpoint de administração de usuários;
- mensagens externas e Outbox ainda não são necessários;
- movimentos são imutáveis pela aplicação, mas proteção por privilégios SQL pode ser endurecida em produção;
- chaves de idempotência são retidas indefinidamente nesta versão.

## Roadmap

1. revisar linguagem de domínio e decisões desta baseline;
2. adicionar gestão de usuários e refresh/revogação de tokens;
3. definir retenção e limpeza das chaves de idempotência;
4. implementar reserva com expiração;
5. modelar posições físicas e estoque avariado/bloqueado;
6. modelar transferências em trânsito;
7. adicionar Outbox apenas quando existir mensageria real;
8. realizar testes de carga e escolher métricas/SLOs de produção.
