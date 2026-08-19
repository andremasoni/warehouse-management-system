# Guia de estudo do projeto WMS

Documento em linguagem simples. Objetivo: entender **o que o sistema faz**, **por que ele é considerado avançado** e **em que ordem se constrói um projeto assim**.

---

## Parte 1 — O que este projeto é, em uma frase

É o **cérebro de um centro de distribuição**: o sistema que sabe, a qualquer instante, quantas unidades de cada produto existem em cada depósito, quantas já estão prometidas a alguém, e quantas ainda podem ser vendidas.

WMS = *Warehouse Management System* = Sistema de Gestão de Armazém.

### O problema real que ele resolve

Imagine um galpão com 50 mil caixas e um site vendendo 24 horas por dia. Duas pessoas clicam "comprar" no mesmo segundo, e resta **uma** unidade no estoque.

Um sistema mal feito vende para as duas. Aí alguém no galpão descobre que a caixa não existe, e a empresa precisa ligar para um cliente pedindo desculpas. Isso se chama **overselling** e custa dinheiro real.

Este projeto existe para que isso **não possa** acontecer — e tem um teste automatizado que prova (`InventoryConcurrencyIntegrationTest`).

### As três quantidades

Esta é a ideia mais importante do sistema inteiro. Todo o resto gira em torno dela.

| Nome | O que significa | Analogia |
|---|---|---|
| **onHand** (físico) | O que está fisicamente na prateleira | Cadeiras no restaurante |
| **reserved** (reservado) | Já prometido a um pedido, ainda não saiu | Mesas com reserva no nome de alguém |
| **available** (disponível) | `onHand − reserved` | Mesas que você pode oferecer a quem chega agora |

A regra de ouro: **`available` nunca é armazenado no banco, é sempre calculado**. Se fosse armazenado, ele poderia ficar dessincronizado com os outros dois — e um número errado de estoque é o pior defeito possível neste tipo de sistema.

### O ciclo de vida de uma unidade

```
    [ Recebimento ]
          |
          v
      onHand +1                    "chegou um caminhão"
          |
          +----> [ Reserva ]  --> reserved +1        "cliente fez o pedido"
          |            |
          |            +--> [ Liberar ]  --> reserved -1     "pedido cancelado"
          |            |
          |            +--> [ Consumir ] --> reserved -1
          |                                  onHand   -1     "pedido despachado"
          |
          +----> [ Saída manual ] --> onHand -1       "avaria, ajuste, amostra"
          |
          +----> [ Transferência ] --> sai de um depósito, entra em outro
```

Repare em **Consumir**: ele baixa `reserved` e `onHand` **juntos, na mesma operação**. Se fizesse um de cada vez, existiria um instante em que os números não fecham. Esse detalhe é o coração do desenho.

### O que a API oferece

| Endpoint | O que faz |
|---|---|
| `POST /api/auth/token` | Faz login e devolve um token JWT |
| `POST /api/products` | Cadastra um produto (com SKU) |
| `POST /api/warehouses` | Cadastra um depósito |
| `POST /api/inventory/receipts` | Registra entrada de mercadoria |
| `POST /api/inventory/issues` | Registra saída manual |
| `POST /api/inventory/reservations` | Reserva unidades |
| `DELETE /api/inventory/reservations/{id}` | Libera uma reserva |
| `POST /api/inventory/transfers` | Move estoque entre depósitos |
| `GET /api/inventory/balances/{dep}/{prod}` | Consulta saldo |
| `GET /api/inventory/movements` | Histórico paginado |
| `POST /api/orders` | Cria pedido reservando **todos** os itens de uma vez |
| `POST /api/orders/{id}/confirmation` | Confirma (consome as reservas) |
| `POST /api/orders/{id}/cancellation` | Cancela (libera as reservas) |

Existem 4 perfis de acesso: `ADMIN`, `STOCK_OPERATOR`, `ORDER_OPERATOR` e `VIEWER`.

---

## Parte 2 — Por que este é um projeto avançado

A maior parte dos projetos de portfólio é um CRUD: um formulário salva no banco, uma lista lê do banco. Este projeto tem **oito características** que quase nunca aparecem juntas. Cada uma abaixo é explicada em linguagem comum.

### 1. O domínio não conhece o Spring

Abra `inventory/domain/StockBalance.java`. Não há uma única anotação. Nenhum `@Entity`, nenhum `@Service`, nenhum `import org.springframework`.

**Por que isso importa?** As regras de negócio (o que é uma reserva válida, quando pode dar baixa) são a parte do software que dura 10 anos. O framework é a parte que você troca a cada 3. Quando a regra está grudada no framework, trocar de framework significa reescrever a regra.

**Efeito prático imediato:** os testes de domínio rodam em **16 milissegundos**, porque não precisam subir o Spring. Um teste que sobe contexto leva 3-5 segundos. A diferença entre 16 ms e 5 s é a diferença entre rodar os testes a cada salvamento e rodar uma vez por dia.

### 2. Objeto inválido não pode existir

Tente criar um SKU inválido:

```java
new Sku("a b");   // lança IllegalArgumentException
new Sku("  abc-123 ");  // vira "ABC-123", normalizado
```

Não existe `sku.setValue(...)`. Não existe um `Sku` "meio pronto" circulando pelo sistema. Ou o objeto nasce válido, ou não nasce.

**Contraste com o comum:** o padrão de mercado é criar o objeto vazio, chamar 8 setters e torcer para que alguém tenha validado. Entre o `new` e o último setter existe um objeto quebrado que pode ser passado adiante por engano.

### 3. Idempotência

Você está no celular, clica "confirmar pedido", a internet cai. O pedido foi criado? Você clica de novo. Comprou duas vezes?

Neste sistema, **não**. Toda operação que muda algo recebe um `externalReference` (uma chave sua). Se a mesma chave chegar duas vezes, o sistema devolve o resultado da primeira e **não faz nada de novo**.

E se alguém tentar reaproveitar a mesma chave com dados diferentes? Recebe erro `409 Conflict` com o código `idempotency.reference_reused` — antes de qualquer alteração no banco.

**Por que é avançado:** idempotência é assunto de sistema de pagamento. Ver isso em projeto de estudo é raríssimo.

### 4. Concorrência resolvida no banco, não no Java

O trecho mais importante do projeto está em `InventoryPersistenceAdapter.lockOrCreate`:

```sql
insert into stock_balances (...) values (...) on conflict (warehouse_id, product_id) do nothing
```
seguido de um `SELECT ... FOR UPDATE`.

Traduzindo: *"crie a linha de saldo se ela não existir e **tranque** essa linha para mim; qualquer outra transação que queira mexer neste produto neste depósito espera na fila"*.

**Por que não dá para resolver isso em Java:** um `if (disponivel >= quantidade)` em Java só olha uma cópia do número na memória. Dois servidores rodando o mesmo código lêem "1 disponível" ao mesmo tempo e ambos aprovam. O único árbitro capaz de decidir é o banco de dados, porque ele é único.

E há um detalhe ainda mais fino: em uma **transferência** entre dois depósitos, o código tranca os dois saldos **sempre na ordem crescente de UUID**. Se a transação A trancasse X depois Y, e a B trancasse Y depois X, elas travariam para sempre uma esperando a outra — um *deadlock*. Ordenar elimina a possibilidade. Isso é conhecimento de quem já apanhou de banco de dados em produção.

### 5. Trilha de auditoria imutável

Existem duas coisas diferentes no banco:

- `stock_balances` — o saldo **atual**. Uma linha por produto/depósito. Muda o tempo todo.
- `stock_movements` — o **histórico**. Uma linha por evento. Só cresce, nunca é alterada.

**Por que separar:** o saldo responde *"quanto tem agora?"*. O histórico responde *"por que tem isso? quem mexeu? quando? com qual referência?"*. Em um armazém real, a segunda pergunta é a que aparece na auditoria.

### 6. Regra escrita duas vezes, de propósito

A regra `reserved <= onHand` está em `StockBalance.java` (Java) **e** em `V1__initial_schema.sql` (constraint do PostgreSQL).

Isso parece violar "não se repita". Não viola — é **defesa em profundidade**. A validação em Java dá uma mensagem de erro boa para o usuário. A constraint no banco garante que **nenhum caminho** — nem um bug futuro, nem um script manual, nem outro sistema conectado ao mesmo banco — consiga gravar um estado impossível.

O README explica isso explicitamente. Saber *quando* duplicar é mais difícil do que saber quando não duplicar.

### 7. Transação e autorização como "portas"

Olhe `InventoryService`. Ele não tem `@Transactional`. Ele recebe um `TransactionRunner` — uma interface de 3 linhas.

```java
public interface TransactionRunner {
    <T> T required(Supplier<T> operation);
}
```

Em produção, a implementação usa `TransactionTemplate` do Spring. No teste, o `InventoryServiceTest` passa uma implementação que só executa a função direto. Resultado: o teste do caso de uso roda **sem Spring e sem banco**, e mesmo assim exercita a lógica de coordenação real.

O mesmo vale para `AccessControl`: a autorização é chamada **dentro do caso de uso**, não só na rota HTTP. Se amanhã o mesmo caso de uso for chamado por uma fila de mensagens ou por um job agendado, a autorização continua valendo.

### 8. Decisões registradas (ADRs)

Em `docs/adr/` há três documentos curtos. Cada um responde: qual era o contexto, o que foi decidido, **quais alternativas foram descartadas e por quê**, e quais são as consequências.

Exemplo real do ADR 0002: escolheu-se lock pessimista; foram consideradas e recusadas as opções de lock otimista, update condicional e mensageria por chave; e admite-se a consequência de que um produto muito disputado vai formar fila.

**Por que isso é sinal de senioridade:** júnior escolhe. Sênior escolhe, registra a alternativa que descartou e admite o preço da escolha. Em uma entrevista técnica, este é o material que diferencia.

### Bônus: o projeto sabe o que ele *não* faz

O README tem uma seção "Decisões e limites" listando 8 limitações conhecidas (sem expiração de reserva, sem posições internas no depósito, sem Outbox...). E uma seção que diz que Outbox e mensageria foram **adiados porque ainda não são necessários**.

Resistir a adicionar tecnologia só porque ela é impressionante é uma das habilidades mais difíceis de engenharia.

### Resumo comparativo

| | CRUD comum | Este projeto |
|---|---|---|
| Regra de negócio | espalhada nos services | dentro dos objetos de domínio |
| Concorrência | ignorada | lock pessimista + teste que prova |
| Requisição repetida | duplica dados | idempotente por design |
| Histórico | não existe | append-only e auditável |
| Schema do banco | gerado pelo Hibernate | migration versionada, Hibernate só valida |
| Testes | poucos, lentos | domínio isolado + Testcontainers |
| Decisões | na cabeça do autor | ADRs versionados |
| Segurança | `@PreAuthorize` na rota | autorização no caso de uso |

---

## Parte 3 — Como se constrói um projeto assim, passo a passo

A ordem abaixo é a ordem em que este projeto **deveria** ter sido construído — e ela importa mais do que as tecnologias. O erro mais comum é começar pelo passo 5.

### Passo 0 — Escreva o que o sistema faz, em uma página, sem código

Antes de abrir a IDE, responda por escrito:

- Qual problema real dói? (*"não podemos vender o que não temos"*)
- Quais são os substantivos do negócio? (produto, depósito, saldo, reserva, movimento, pedido)
- Quais são os verbos? (receber, reservar, liberar, consumir, transferir, confirmar, cancelar)
- Quais frases são **sempre verdadeiras**, aconteça o que acontecer?

Essa última lista tem nome: **invariantes**. Neste projeto são nove, e estão no README:

```
onHand >= 0
reserved >= 0
reserved <= onHand
available = onHand - reserved
só reserva ativa pode ser liberada ou consumida
pedido só confirma depois de consumir todas as reservas
pedido cancelado não pode ser confirmado
produtos não se repetem no mesmo pedido
uma referência externa não gera duas movimentações
```

**Por que primeiro:** essas nove frases determinam o modelo de dados, as transações, os testes e os endpoints. Se você começar pela tabela do banco, vai descobrir as invariantes depois — e vai ter que refazer a tabela.

> **Como fazer:** um arquivo `docs/dominio.md`. Uma sessão de 2 horas. Sem código.

### Passo 1 — Modele o domínio em Java puro

Crie **apenas** as classes de negócio. Sem Spring, sem banco, sem JSON.

```java
public final class StockBalance {
    private long onHand;
    private long reserved;

    public void reserve(long quantity) {
        if (quantity <= 0) throw new IllegalArgumentException(...);
        if (available() < quantity) throw new ConflictException("inventory.insufficient_stock", ...);
        reserved += quantity;
    }

    public long available() { return onHand - reserved; }
}
```

Regras práticas:
- construtor **privado** + fábricas `create(...)` e `restore(...)`;
- **nenhum setter** público;
- validação no construtor, sempre;
- lance exceções de negócio com **código** estável, não `RuntimeException` genérica.

> **Teste de sanidade:** se você precisar de qualquer `import` de framework nesta pasta, parou no lugar errado.

### Passo 2 — Teste o domínio *antes* de existir banco

Cada invariante do passo 0 vira pelo menos um teste. Nome do teste = a frase da regra:

```java
@Test
void manualIssueCannotUseReservedUnits() { ... }
```

Esses testes rodam em milissegundos e são os únicos que ainda vão estar corretos daqui a cinco anos, quando o Spring 8 mudar tudo.

> **Marco:** só avance quando as invariantes estiverem verdes.

### Passo 3 — Escreva os casos de uso e as *portas*

Agora conecte os verbos. Um caso de uso: (1) verifica permissão, (2) abre transação, (3) carrega, (4) chama o domínio, (5) salva, (6) registra o evento.

O caso de uso **não sabe** de onde vêm os dados. Ele declara interfaces — as **portas**:

```java
public interface InventoryRepository {
    StockBalance lockOrCreate(UUID warehouseId, UUID productId);
    Optional<StockBalance> find(UUID warehouseId, UUID productId);
    StockBalance save(StockBalance balance);
}
```

E, o pulo do gato deste projeto: **transação e autorização também são portas** (`TransactionRunner`, `AccessControl`). O resultado é que o caso de uso é testável com mocks, sem subir nada.

> **Erro comum:** colocar `@Transactional` no service. Funciona, mas amarra o caso de uso ao Spring e torna a transação invisível na leitura do código.

### Passo 4 — Desenhe o schema do banco à mão

Escreva o SQL você mesmo, como uma migration Flyway (`V1__initial_schema.sql`). **Não** deixe o Hibernate gerar.

Dentro do SQL, repita as invariantes que puder:

```sql
constraint ck_stock_balance_reserved check (reserved >= 0 and reserved <= on_hand)
constraint uk_stock_movement_reference unique (external_reference)
```

E configure `ddl-auto: validate` — o Hibernate passa a **conferir** o schema, nunca a criá-lo.

> **Por quê:** o banco sobrevive à aplicação. Se a regra existe só no Java, qualquer outro processo que toque o banco pode corrompê-lo. E migration versionada é a única forma de fazer deploy de schema com segurança.

### Passo 5 — Só agora escreva a persistência

Implemente as portas do passo 3 com adapters. Entidade JPA é uma classe **separada** do objeto de domínio; o adapter converte de um para o outro.

Sim, dá mais trabalho. O que você compra com isso: o domínio nunca é contaminado por proxies, lazy loading e callbacks do Hibernate. Está registrado no ADR 0003 deste projeto.

> **Sinal de que está certo:** você consegue trocar JPA por JDBC puro alterando **só** a pasta `infrastructure/persistence`.

### Passo 6 — Trate concorrência explicitamente

Pergunte, para cada operação: *"o que acontece se duas requisições fizerem isso no mesmo milissegundo?"*

Se a resposta for "dá problema", escolha uma estratégia e **registre a escolha**:

| Estratégia | Quando usar |
|---|---|
| Lock pessimista (`SELECT FOR UPDATE`) | conflito frequente, correção acima de throughput |
| Lock otimista (`@Version` + retry) | conflito raro |
| Update condicional em SQL | operação simples, muito volume |
| Fila por chave | escala grande, aceita consistência eventual |

Este projeto escolheu pessimista e explicou o porquê no ADR 0002.

E **escreva o teste de corrida**: dois threads, um recurso, exatamente um vencedor. Sem esse teste, você só *acha* que resolveu.

### Passo 7 — Adicione idempotência antes de precisar

Cada operação de escrita recebe uma chave externa. Duas camadas de proteção:

1. `unique constraint` no banco (a garantia real);
2. antes de mutar, procure a chave; se existir, **compare o payload**: igual → devolve o resultado anterior; diferente → `409`.

Retrofitar idempotência em um sistema que já roda é doloroso; colocar desde o início é quase de graça.

### Passo 8 — Exponha o HTTP por último

Controller é **tradutor**, não é onde a regra mora. Ele só: recebe JSON, valida formato (`@Valid`, `@Positive`, `@Size`), chama o caso de uso, devolve o resultado.

Regras:
- **nunca** devolva entidade JPA pela API — ela vira contrato público sem querer;
- um `@RestControllerAdvice` centraliza os erros;
- o corpo do erro é contrato: `timestamp`, `status`, `code`, `message`, `path`, `correlationId`, `fields`;
- códigos de erro estáveis (`inventory.insufficient_stock`) para o cliente tratar sem depender do texto.

> **Sinal de alerta:** se houver um `if` de regra de negócio dentro de um controller, ele está no lugar errado.

### Passo 9 — Segurança em duas camadas

1. **Na rota:** quem entra (`SecurityFilterChain` — o que é público, o que exige token).
2. **No caso de uso:** o que pode fazer (`accessControl.requireAny(Role.ADMIN, Role.STOCK_OPERATOR)`).

A segunda camada é a que a maioria esquece. Ela é o que garante que a regra continue valendo quando o mesmo caso de uso for chamado por uma fila, um job ou um teste.

Nunca coloque segredo em código: `${JWT_SECRET}` vindo do ambiente, com um `.env.example` mostrando o formato e o `.env` real no `.gitignore`.

### Passo 10 — Complete a pirâmide de testes

Não é "escrever mais testes". É escrever testes **em camadas diferentes**, cada uma respondendo a uma pergunta diferente:

| Camada | Ferramenta | Pergunta que responde | Quantidade |
|---|---|---|---|
| Domínio | JUnit puro | *a regra está certa?* | muitos, rápidos |
| Aplicação | JUnit + Mockito | *a orquestração está certa?* | muitos |
| Persistência | `@DataJpaTest` | *o mapeamento ida e volta está certo?* | um por adapter |
| Web | `@WebMvcTest` | *status, validação e erros estão certos?* | um por controller |
| Integração | `@SpringBootTest` + Testcontainers | *funciona com PostgreSQL de verdade?* | poucos, os críticos |
| Arquitetura | ArchUnit | *alguém quebrou a fronteira dos módulos?* | 5 a 8 regras |

> **Nota honesta:** este projeto hoje cobre bem as camadas 1 e 2, tem 1 teste na camada 5, e **não tem** as camadas 3, 4 e 6. É a maior oportunidade de melhoria — veja o `relatorio-revisao.md`.

Use Testcontainers, nunca H2: banco em memória tem SQL, tipos e comportamento de lock diferentes do PostgreSQL, então ele mente para você exatamente nos pontos que mais importam.

### Passo 11 — Torne o sistema observável

Um sistema que você não consegue enxergar em produção não está pronto:

- **Correlation ID** — um `X-Correlation-ID` por requisição, no MDC, aparecendo em toda linha de log e na resposta de erro. Permite reconstruir uma requisição inteira nos logs;
- **Logs em JSON** — máquina lê, ferramenta agrega;
- **Actuator** — `/actuator/health` com probes de liveness e readiness (é o que o Kubernetes e o Docker consultam);
- **Micrometer + Prometheus** — métricas;
- **Nunca** logue token, senha ou hash.

### Passo 12 — Empacote e automatize

- **Dockerfile multi-stage:** estágio de build com Maven, estágio final só com JRE. Copie o `pom.xml` e baixe as dependências **antes** de copiar o `src` — assim o cache de camada sobrevive a mudanças de código;
- **Usuário não-root** no container, `HEALTHCHECK`, `-XX:MaxRAMPercentage`;
- **compose.yaml** para subir app + banco com um comando;
- **CI** que roda `clean verify` e constrói a imagem em todo PR. Use o `./mvnw` do repositório, não o Maven da máquina.

### Passo 13 — Documente as decisões, não o código

Três documentos, com propósitos distintos:

- **README** — o que faz, como rodar, quais são os limites conhecidos;
- **`docs/architecture.md`** — módulos, fronteiras, limites transacionais, diagramas;
- **`docs/adr/NNNN-titulo.md`** — uma decisão por arquivo: contexto → decisão → **alternativas descartadas** → consequências.

Comentário no código envelhece e mente. ADR é datado e imutável: ele conta o que você sabia **naquele momento**, que é exatamente a informação que falta quando alguém pergunta "por que isso foi feito assim?" dois anos depois.

---

## Parte 4 — Roteiro de leitura do código

Se o objetivo é **estudar**, leia nesta ordem — nunca comece pelo `SecurityConfiguration`.

| Ordem | Arquivo | Pergunta a levar na leitura |
|---|---|---|
| 1 | `inventory/domain/StockBalance.java` | Por que `available` é calculado e não guardado? |
| 2 | `inventory/domain/StockBalanceTest.java` | Cada teste corresponde a qual frase do negócio? |
| 3 | `inventory/domain/Reservation.java` | Por que a reserva tem estado próprio e não é só um número? |
| 4 | `inventory/application/InventoryUseCases.java` | O que este contrato promete ao mundo exterior? |
| 5 | `inventory/application/InventoryService.java` | O que aqui é orquestração e o que é regra? |
| 6 | `inventory/infrastructure/persistence/InventoryPersistenceAdapter.java` | Por que o lock precisa estar no banco? |
| 7 | `docs/adr/0002-stock-concurrency.md` | Quais alternativas foram descartadas e por quê? |
| 8 | `order/domain/SalesOrder.java` + `order/application/OrderService.java` | Por que `order` chama `inventory` e nunca o contrário? |
| 9 | `inventory/presentation/InventoryController.java` | Quantas linhas de regra existem aqui? (resposta: zero) |
| 10 | `shared/presentation/GlobalExceptionHandler.java` | Como o erro de domínio vira um status HTTP? |
| 11 | `identity/infrastructure/security/` | Onde a autorização acontece — na rota ou no caso de uso? |
| 12 | `V1__initial_schema.sql` | Quais invariantes do Java reaparecem aqui? |

### Exercício que ensina mais do que ler

1. Abra `StockBalance.java`;
2. **quebre uma invariante de propósito** — por exemplo, apague a verificação de disponibilidade em `reserve()`;
3. **antes de rodar**, escreva no papel quais testes você acha que vão falhar;
4. rode `./mvnw test`;
5. compare.

Acertar a previsão significa que você entendeu o modelo. Errar mostra exatamente onde está o buraco no seu entendimento — que é a informação mais útil que um estudo pode produzir.

### Exercícios de evolução, em ordem de dificuldade

1. **Fácil:** adicionar `GET /api/products` com paginação;
2. **Fácil:** cobrir `GlobalExceptionHandler` com `@WebMvcTest`;
3. **Médio:** escrever os testes de `OrderService` que faltam;
4. **Médio:** adicionar ArchUnit proibindo `jakarta.persistence` em qualquer pacote `domain`;
5. **Difícil:** reserva com expiração automática (exige job agendado, nova coluna e decisão sobre o que fazer com pedidos afetados);
6. **Difícil:** permitir que um pedido seja atendido por múltiplos depósitos (muda o agregado `SalesOrder` inteiro);
7. **Muito difícil:** trocar lock pessimista por otimista com retry, medir a diferença sob carga e escrever o ADR 0004 justificando ou revertendo.

O exercício 7 é o que transforma leitura em experiência.
