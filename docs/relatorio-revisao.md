# Relatório de revisão — Clean Code, camadas de teste e escalabilidade

Data da revisão: 19/08/2026 · Baseline: `wms 0.1.0-SNAPSHOT` · Java 21 · Spring Boot 3.5.16

## 1. Escopo e método

Foram lidos **todos os 74 arquivos Java** do projeto (68 em `src/main`, 2.487 linhas; 6 em `src/test`, 317 linhas), além de `pom.xml`, `application.yml`, a migration `V1__initial_schema.sql`, `Dockerfile`, `compose.yaml`, o workflow de CI e os 5 documentos em `docs/`.

A suíte foi executada localmente para validar as afirmações deste relatório:

```
mvn test  ->  Tests run: 13, Failures: 0, Errors: 0, Skipped: 1  ·  BUILD SUCCESS  (10,5 s)
```

O teste ignorado é `InventoryConcurrencyIntegrationTest`, desativado automaticamente por ausência de Docker na máquina (`@Testcontainers(disabledWithoutDocker = true)`). **Isso significa que o único teste de integração do projeto não foi de fato exercitado nesta execução** — ponto relevante para a seção 4.

## 2. Resumo executivo

| Eixo | Nota | Leitura curta |
|---|---|---|
| Clean Code | **9 / 10** | Excelente. Ressalvas são cosméticas ou de granularidade, não de design. |
| Camadas de teste | **5 / 10** | O que existe é de ótima qualidade, mas cobre ~2 das 5 camadas necessárias. É o eixo mais fraco. |
| Escalabilidade | **7,5 / 10** | Fundação correta e limites conscientes. Faltam ajustes baratos e de alto retorno. |

O projeto está **acima da média de mercado** em arquitetura e **abaixo do próprio padrão** em testes. Se houver apenas uma coisa a fazer a seguir, é fechar as lacunas da seção 4.

---

## 3. Clean Code

### 3.1 O que está correto (e é raro encontrar)

**Domínio livre de framework.** `StockBalance`, `Reservation`, `SalesOrder`, `Product`, `Warehouse` e `Sku` não têm uma única anotação de Spring ou JPA. Isso não é estética: é o que permite testar regra de negócio em 16 ms sem subir contexto.

**Construção sempre válida.** Todo objeto de domínio valida no construtor privado e só é criado por fábricas estáticas nomeadas — `create(...)` para novo, `restore(...)` para reidratação vinda do banco. Não existe estado intermediário inválido, nem setter público. `Sku` (`product/domain/Sku.java`) é um `record` que normaliza e valida no compact constructor: `new Sku("  abc-123 ")` já nasce `ABC-123` ou não nasce.

**Invariantes no lugar certo.** `StockBalance.reserve()` não pergunta ao serviço se pode reservar — ele decide e lança `ConflictException`. A regra `reserved <= onHand` não está espalhada em `if`s de service; está no objeto que a possui.

**Inversão de dependência real, não decorativa.** `InventoryService` depende de `InventoryRepository`, `TransactionRunner` e `AccessControl` — três interfaces do próprio módulo. Ele não conhece `JpaRepository`, `EntityManager`, `@Transactional` nem `SecurityContextHolder`. As implementações vivem em `infrastructure` e são ligadas manualmente em `shared/infrastructure/ApplicationConfiguration.java`. Transação e autorização como *portas* é uma decisão madura e pouco comum.

**Três representações separadas e conscientes.** Para cada conceito existem: objeto de domínio (regra), `*JpaEntity` (persistência) e `record` de request/view (HTTP). Nenhuma entidade JPA vaza pela API.

**Erros como linguagem.** `BusinessException` carrega um `code` estável (`inventory.insufficient_stock`, `order.invalid_state`). O `GlobalExceptionHandler` traduz com *pattern matching* de `switch` sobre o tipo — sem cadeia de `instanceof`. O corpo de erro é um contrato: `timestamp`, `status`, `code`, `message`, `path`, `correlationId`, `fields`.

**Nomes de teste como frases.** `concurrentReservationsCannotOversellLastUnit`, `manualIssueCannotUseReservedUnits`, `orderCanBeConfirmedOnlyAfterEveryItemIsReserved`. Cada nome descreve a regra, não o método testado.

**Métricas de tamanho saudáveis.** Maior classe: 248 linhas (`InventoryService`). Média em `src/main`: 36 linhas por arquivo. Nenhum "God object".

### 3.2 Correções pontuais

**C1 — Nomes totalmente qualificados no meio do código.** Quebra a consistência de importação do resto do projeto.

- `order/application/OrderService.java:103` — `throw new com.example.wms.shared.domain.ConflictException(...)`
- `identity/infrastructure/security/SecurityConfiguration.java:93-95` — `new com.example.wms...UserJpaEntity(`, `java.util.UUID.randomUUID()`, `java.util.Set.of(...)`
- `identity/infrastructure/security/JwtTokenService.java:43` — `java.time.Instant` dentro do `record Token`
- `test/.../InventoryConcurrencyIntegrationTest.java:54` — `java.util.List.of(...)`

Correção: adicionar os `import` e usar o nome simples. Custo: 5 minutos.

**C2 — Duplicação estrutural entre `receive` e `issue`.** `InventoryService.java:43-68`: os dois métodos são idênticos exceto por uma linha (`balance.receive` vs `balance.issueAvailable`) e pelo `MovementType`. O padrão "consulta idempotência → trava saldo → muta → salva → registra movimento" aparece 4 vezes na classe.

Correção sugerida: um método privado `applyMovement(StockCommand, MovementType, LongConsumer mutation)`. Elimina ~25 linhas e cria um único ponto onde a política de idempotência vive.

**C3 — `transfer` faz coisas demais.** `InventoryService.java:134-190` tem ~57 linhas e três responsabilidades: verificar idempotência da transferência, ordenar os locks por UUID e executar a movimentação dupla. A ordenação determinística de lock (linhas 160-170) é um conceito com nome próprio e merece um método `lockPair(sourceId, destinationId, productId)`.

**C4 — Prefixos de referência como *strings* soltas.** `"reserve:"`, `"consume:"`, `"release:"`, `"transfer-out:"`, `"transfer-in:"`, `"order:" + id + ":item:" + id` aparecem concatenados em `InventoryService` e `OrderService`. Há um acoplamento **implícito e não documentado**: `external_reference` de movimento aceita 120 caracteres e a referência do usuário aceita 100 — sobram apenas 20 para prefixos. `"transfer-out:"` já consome 13. Ninguém testa esse limite.

Correção: uma classe `MovementReference` com métodos de fábrica (`MovementReference.reserve(ref)`) que centralize prefixos e valide o comprimento resultante.

**C5 — Normalização de código de depósito duplicada.** `WarehouseService.java:37` faz `command.code().trim().toUpperCase(Locale.ROOT)` e `Warehouse.java:19-24` faz a mesma normalização de novo. A regra de normalização pertence ao domínio; o serviço deveria construir o `Warehouse` primeiro e consultar `warehouse.code()`.

**C6 — Efeito colateral antes da verificação de estado.** `OrderService.cancel` (linhas 88-97) itera liberando reservas **antes** de chamar `order.cancel()`, que é quem valida o estado. Cancelar um pedido já `CONFIRMED` falha corretamente (rollback), mas com o erro errado: o cliente recebe `reservation.not_active` em vez de `order.invalid_state`. Correção: validar a transição antes de tocar no inventário.

**C7 — `existsActive` mistura dois contratos.** `ProductUseCases`/`WarehouseUseCases` expõem `create`/`get` (usados pelo HTTP, com `accessControl`) e `existsActive` (usado internamente por `inventory`, **sem** `accessControl`). São duas audiências na mesma interface. Separar em `ProductUseCases` (entrada HTTP) e `ProductCatalog` (porta entre módulos) tornaria a fronteira explícita.

---

## 4. Camadas de teste

### 4.1 O que existe hoje

| Camada | Arquivos | Testes | Situação |
|---|---|---|---|
| Domínio (unitário, sem Spring) | 4 | 10 | **Bom.** Cobre invariantes de saldo, reserva, pedido e SKU. |
| Aplicação (unitário com mocks) | 1 | 2 | **Insuficiente.** Só `InventoryService`. |
| Persistência (`@DataJpaTest`) | 0 | 0 | **Ausente.** |
| Web (`@WebMvcTest`) | 0 | 0 | **Ausente.** |
| Integração (`@SpringBootTest`) | 1 | 1 | **Existe, mas não roda sem Docker.** |

O `InventoryServiceTest` merece destaque positivo: injeta um `TransactionRunner` anônimo síncrono e um `Clock.fixed(...)`, o que torna o teste determinístico no tempo e independente de banco. É exatamente o retorno prometido pela arquitetura de portas — e prova que o desenho funciona.

O `InventoryConcurrencyIntegrationTest` é o teste mais valioso do repositório: dois *threads* disputando a última unidade, com PostgreSQL real via Testcontainers, verificando que exatamente um vence. É o tipo de teste que a maioria dos projetos não escreve.

### 4.2 Lacunas, em ordem de risco

**T1 — Nenhum teste de mapeamento domínio ↔ JPA.** O ADR 0003 escolheu deliberadamente manter dois modelos e assume o custo de "risco de divergência, reduzido por testes de integração". Esses testes não existem. `OrderPersistenceAdapter.save` reconstrói um `OrderJpaEntity` inteiro com `cascade = ALL` + `orphanRemoval = true` a cada chamada — o comportamento de *merge* de coleção detachada é a área mais frágil do Hibernate e tem **zero** cobertura. Se algo quebrar silenciosamente no projeto, começa aqui.

**T2 — Nenhum teste da camada web.** `GlobalExceptionHandler` mapeia 8 famílias de exceção para códigos HTTP e nada verifica esse mapeamento. As anotações Bean Validation (`@Positive`, `@Size(max = 100)`, `@NotEmpty`) nunca são exercitadas. O contrato de erro publicado na API é, hoje, não testado.

**T3 — Segurança nunca é testada de ponta a ponta.** O único teste de integração faz `@MockitoBean AccessControl` — ou seja, **desliga a autorização**. Não existe teste que confirme que um `VIEWER` recebe 403 ao tentar `POST /api/inventory/receipts`, nem que uma requisição sem token recebe 401, nem que `/api/auth/token` emite um JWT com as *roles* corretas. `spring-security-test` está no `pom.xml` e não é usado.

**T4 — `OrderService` sem teste.** É a orquestração mais complexa do sistema (criar pedido reservando N itens atomicamente, confirmar consumindo todas as reservas, cancelar liberando todas) e não tem um único teste de caso de uso. O achado **C6** deste relatório existe justamente porque não há teste que force a ordem correta.

**T5 — Idempotência não é testada onde ela vive.** A idempotência é apresentada como característica central do projeto e é implementada em dois lugares: na aplicação (comparação de payload) e no banco (`unique constraint`). Existe **um** teste unitário do caminho da aplicação (`reusedIdempotencyReferenceWithDifferentPayloadIsRejectedBeforeMutation`) e nenhum do caminho do banco — que é o único que protege contra requisições simultâneas.

**T6 — Nenhum teste arquitetural.** O ADR 0001 admite: *"Fronteiras modulares dependem de disciplina e testes arquiteturais futuros"*. Sem ArchUnit, nada impede que amanhã um `import` em `inventory/domain` traga `jakarta.persistence`, ou que `inventory` passe a depender de `order` criando o ciclo que o desenho evita. A arquitetura é o principal ativo do projeto e é o menos protegido.

**T7 — Sem medição.** Não há JaCoCo (cobertura) nem PIT (mutação) no `pom.xml`. Não existe *quality gate*: o CI passa com um teste ou com trezentos.

**T8 — Sem `src/test/resources`.** Não há `application-test.yml`. O teste de integração herda a configuração de produção e sobrescreve o datasource via `@DynamicPropertySource`, o que funciona, mas não isola perfis.

### 4.3 Alvo sugerido

```
                 /\        E2E (1-2)  ......  fluxo completo pela API com JWT real
                /  \
               /----\      Integracao (5-8)  .  concorrencia OK, idempotencia, mapeamento JPA
              /      \
             /--------\    Web + Persistencia (12-20)  .  @WebMvcTest, @DataJpaTest
            /          \
           /------------\  Aplicacao (10-15)  .  InventoryService OK (2), OrderService FALTA
          /              \
         /----------------\ Dominio (25-40)  .  10 hoje - ampliar para casos de borda
        /__________________\
         + ArchUnit (5-8 regras) atravessando tudo
```

---

## 5. Escalabilidade

### 5.1 Fundação correta

- **Aplicação sem estado.** JWT autocontido + `SessionCreationPolicy.STATELESS`. Escalar horizontalmente é acrescentar réplicas; não há sessão nem *sticky session*.
- **Fronteiras extraíveis.** Cada módulo já conversa por interface. Extrair `inventory` para um serviço próprio é substituir uma implementação de porta, não reescrever o núcleo.
- **Schema versionado.** Flyway como fonte da verdade, `ddl-auto: validate`. Deploy de schema é reprodutível e auditável.
- **Concorrência resolvida no lugar certo.** `INSERT ... ON CONFLICT DO NOTHING` + `SELECT ... FOR UPDATE`, com ordenação determinística de locks por UUID em transferências para evitar deadlock. Isso está correto e é testado.
- **Defesa em profundidade no banco.** `check (reserved >= 0 and reserved <= on_hand)`, `unique (external_reference)`, FKs. Mesmo um bug de aplicação não consegue gravar estado inválido.
- **Índices alinhados às consultas.** `(warehouse_id, occurred_at desc)` e `(product_id, occurred_at desc)` servem exatamente o histórico paginado.
- **`open-in-view: false`.** Impede que consultas lazy vazem para a serialização HTTP — desativado explicitamente, não por acidente.
- **Imagem de container correta.** Multi-stage, `dependency:go-offline` antes de copiar o `src` (cache de camada), usuário não-root, `MaxRAMPercentage=75`, `HEALTHCHECK` na *readiness probe*, `shutdown: graceful`.

### 5.2 Gargalos e correções

**E1 — Duas consultas extras por operação de escrita, fora da transação.** `InventoryService.validateContext` (linha 198) chama `warehouses.existsActive()` e `products.existsActive()` antes de cada `receive`, `issue`, `reserve` — e **duas vezes** em `transfer` (linhas 139-140, 4 consultas). Cada `existsActive` é um `findById` completo que materializa o objeto de domínio só para ler um `boolean`. Em um pico de recebimento, isso é 2-4× o tráfego de leitura necessário. Correções, em ordem de esforço: (a) um `existsByIdAndActiveTrue` que retorne `boolean` no SQL; (b) cache local com TTL curto — catálogo de produto e depósito muda raramente; (c) delegar ao banco, já que as FKs garantem existência.

**E2 — Paginação por *offset* em tabela que só cresce.** `MovementPersistenceAdapter.find` usa `PageRequest.of(page, size)`. `stock_movements` é append-only: em 10 milhões de linhas, `page=50000` faz o PostgreSQL descartar 1 milhão de linhas antes de devolver 20, e o `count(*)` do `Page` percorre a tabela toda a cada requisição. Correção: paginação por *keyset* (`where occurred_at < :cursor order by occurred_at desc limit :size`) e `Slice` em vez de `Page` para eliminar o `count`.

**E3 — `save()` faz um `SELECT` desnecessário antes de todo `INSERT`.** Todas as entidades têm o `@Id` (UUID) preenchido pela aplicação. `JpaRepository.save()` verifica se a entidade é nova pelo id; como ele **nunca** é nulo, o Hibernate trata tudo como *detached* e executa `merge`, que dispara um `SELECT` antes do `INSERT`. Isso dobra as idas ao banco em cada gravação de `StockMovement`, `Product`, `Warehouse`, `Reservation`. Correções: implementar `Persistable<UUID>` com um campo `isNew`, ou usar `EntityManager.persist()` no adapter para criação. É a otimização de melhor relação custo/benefício do projeto.

**E4 — Consulta com `:param is null or coluna = :param`.** `SpringDataMovementRepository.findFiltered` usa esse padrão para filtros opcionais. O PostgreSQL costuma gerar um plano genérico que não aproveita bem os índices de `(warehouse_id, occurred_at)`. Correção: `Specification` ou consultas separadas por combinação de filtro.

**E5 — Virtual threads desligados.** O projeto usa Java 21 e é 100% bloqueante (JDBC + servlet). Adicionar `spring.threads.virtual.enabled: true` no `application.yml` é uma linha e aumenta substancialmente a concorrência sustentada sob I/O. É a melhoria de maior retorno por caractere digitado.

**E6 — Configuração de *tracing* inerte.** `application.yml:39-41` define `management.tracing.sampling.probability`, mas o `pom.xml` **não** contém `micrometer-tracing-bridge-otel` nem `brave`. A propriedade não faz nada hoje. Ou adicione a dependência, ou remova a configuração para não sugerir uma capacidade inexistente.

**E7 — Leituras sem `readOnly`.** `balance()`, `movements()`, `get()` não abrem transação alguma (o `TransactionRunner` só é usado nas escritas). Cada consulta roda em autocommit. Uma transação `readOnly = true` permitiria roteamento para réplica de leitura no futuro e evita o *dirty checking* do Hibernate.

**E8 — Pool de conexões vs. lock pessimista.** `maximum-pool-size: 10` com locks pessimistos: se 10 requisições disputarem o mesmo SKU, todas as conexões ficam presas na fila do lock e o restante da aplicação para. Correção: `lock_timeout` no PostgreSQL para falhar rápido, e uma métrica Micrometer de espera de lock. O ADR 0002 já antecipa esse limite; falta instrumentá-lo.

**E9 — CI incompleto.** `.github/workflows/ci.yml` compila, testa e constrói a imagem. Faltam: verificação de vulnerabilidades de dependências (OWASP Dependency-Check ou `dependabot`), análise estática, publicação de relatório de cobertura e cache das camadas Docker. O workflow também usa `mvn` do runner em vez do `./mvnw` versionado no repositório — o wrapper existe justamente para garantir a mesma versão em toda máquina.

**E10 — Sem limite de requisições.** `/api/auth/token` é público e faz BCrypt a cada tentativa (custo de CPU proposital). Sem *rate limiting*, é um vetor de exaustão de CPU trivial.

### 5.3 Limites já documentados (não são falhas)

O README lista honestamente: sem expiração de reserva, um único depósito por pedido, sem *bin locations*, transferência sem estado "em trânsito", sem gestão de usuários, sem Outbox, chaves de idempotência retidas para sempre. **Documentar limites é sinal de maturidade, não de dívida.** O ADR 0002 inclusive antecipa a troca da estratégia de lock quando houver dados de carga.

---

## 6. Backlog priorizado

| # | Ação | Eixo | Esforço | Impacto |
|---|---|---|---|---|
| 1 | ArchUnit: proibir Spring/JPA em `domain`, proibir ciclos entre módulos | Teste | Baixo | **Alto** |
| 2 | `spring.threads.virtual.enabled: true` | Escala | Trivial | **Alto** |
| 3 | `@WebMvcTest` para `GlobalExceptionHandler` + validação + 401/403 | Teste | Médio | **Alto** |
| 4 | `Persistable<UUID>` ou `persist()` — eliminar o `SELECT` antes do `INSERT` | Escala | Baixo | **Alto** |
| 5 | Testes de `OrderService` (criar/confirmar/cancelar) + corrigir **C6** | Teste | Médio | **Alto** |
| 6 | `@DataJpaTest` para os 5 adapters de persistência (mapeamento ida e volta) | Teste | Médio | **Alto** |
| 7 | JaCoCo com *quality gate* no CI | Teste | Baixo | Médio |
| 8 | `existsByIdAndActiveTrue` retornando `boolean` | Escala | Baixo | Médio |
| 9 | Corrigir **C1** (nomes qualificados) e extrair **C2** (duplicação) | Clean Code | Trivial | Médio |
| 10 | Teste de integração de idempotência com PostgreSQL real | Teste | Médio | Médio |
| 11 | `./mvnw` no CI + verificação de vulnerabilidades | Escala | Baixo | Médio |
| 12 | Paginação por *keyset* em `stock_movements` | Escala | Médio | Médio (futuro) |
| 13 | Classe `MovementReference` (**C4**) | Clean Code | Baixo | Médio |
| 14 | Adicionar bridge de tracing ou remover a config (**E6**) | Escala | Trivial | Baixo |
| 15 | *Rate limiting* em `/api/auth/token` | Escala | Médio | Baixo (hoje) |

**Se houver tempo para apenas três:** itens 1, 3 e 4. O primeiro protege a arquitetura, o segundo cobre o maior buraco de teste, o terceiro é a otimização mais barata do sistema.
