# ADR 0002 — Concorrência de estoque com lock pessimista

## Contexto

Duas transações podem ler a última unidade disponível e reservá-la simultaneamente. Validar apenas no objeto Java não protege contra essa corrida.

## Decisão

Criar a linha de saldo com `INSERT ... ON CONFLICT DO NOTHING` e carregá-la com `PESSIMISTIC_WRITE` dentro da transação. Transferências bloqueiam os depósitos em ordem crescente de UUID.

## Alternativas consideradas

- Lock otimista: melhora concorrência quando conflitos são raros, mas exige política explícita de retry.
- Update condicional: eficiente, mas desloca parte importante da regra para SQL e complica a atualização conjunta de reservas e histórico.
- Mensageria por chave: útil em escala maior, porém adiciona consistência assíncrona e infraestrutura.

## Consequências

Overselling é evitado com um modelo simples. Produtos muito disputados podem gerar espera e reduzir throughput; métricas e testes de carga devem orientar uma futura troca de estratégia.
