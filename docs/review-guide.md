# Roteiro para revisão orientada ao aprendizado

Revisar o projeto nesta ordem evita começar pelos detalhes do Spring antes de entender o negócio.

## 1. Invariantes de estoque

Arquivos centrais:

- `inventory/domain/StockBalance.java`
- `inventory/domain/Reservation.java`
- `inventory/domain/StockMovement.java`
- testes em `inventory/domain`

Perguntas de revisão:

1. Por que `available` é calculado e não armazenado?
2. Por que consumir uma reserva reduz `reserved` e `onHand` juntos?
3. Qual inconsistência surgiria se o histórico fosse salvo em outra transação?

## 2. Caso de uso e portas

Leia `InventoryService` junto das três interfaces de repositório. Observe que o serviço conhece contratos, mas não `JpaRepository`, `EntityManager` ou HTTP.

Perguntas:

1. Qual lógica é orquestração e qual é regra de domínio?
2. Por que transação e autorização também são portas?
3. Quando separar a fachada `InventoryUseCases` em casos de uso menores?

## 3. Concorrência

Leia `InventoryPersistenceAdapter.lockOrCreate` e o ADR 0002.

Perguntas:

1. Por que validar disponibilidade antes do lock não seria suficiente?
2. Que custo o lock pessimista causa em um SKU muito disputado?
3. Como seria a alternativa com versão otimista e retry?

## 4. Pedido como orquestrador

Leia `SalesOrder`, `OrderService` e os testes do pedido.

Perguntas:

1. Por que `order` chama `inventory`, mas o caminho inverso não existe?
2. O que garante que um pedido não fique parcialmente reservado?
3. Como o modelo mudaria para permitir atendimento parcial ou múltiplos depósitos?

## 5. Adapters técnicos

Compare um objeto de domínio com sua entidade JPA e seu controller. Identifique as três representações: negócio, persistência e HTTP.

Perguntas:

1. Qual custo essa separação adiciona?
2. Em que tipo de CRUD ela seria desnecessária?
3. Por que retornar entidade JPA pela API quebraria fronteiras?

## 6. Segurança e operação

Finalize com `SecurityConfiguration`, `SecurityAccessControl`, `GlobalExceptionHandler`, `application.yml`, Docker e CI. Esses componentes suportam o domínio, mas não definem suas regras.

## Primeira sessão sugerida

Começar somente por `StockBalance` e seus quatro testes. Depois, alterar deliberadamente uma invariante, prever quais testes falharão e executar `./mvnw test`. Só então avançar para `InventoryService`.
