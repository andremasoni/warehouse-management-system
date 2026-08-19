# ADR 0003 — Separar domínio e entidades JPA

## Contexto

O modelo possui invariantes, transições de estado e bloqueios que não devem depender de proxies, setters ou callbacks do ORM.

## Decisão

Manter objetos de domínio sem anotações e criar entidades JPA e mapeadores nos adaptadores de persistência.

## Alternativas consideradas

- Um único modelo JPA/domínio: menos classes, adequado para CRUDs simples, mas aproxima o núcleo das limitações do Hibernate.
- Persistência por JDBC: oferece controle direto, porém exige mais SQL e mapeamento para todas as operações.

## Consequências

O domínio é testável sem Spring e controla as mutações. Há custo de mapeamento e risco de divergência, reduzido por testes de integração e schema validado pelo Hibernate.
