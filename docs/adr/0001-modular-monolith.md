# ADR 0001 — Monólito modular organizado por contexto

## Contexto

Produtos, estoque, pedidos e identidade têm responsabilidades diferentes, mas os fluxos críticos exigem consistência forte e ainda não existe necessidade de escala ou deploy independente.

## Decisão

Usar um único artefato Spring Boot e um banco PostgreSQL, organizando o código por contexto de negócio. Dentro de cada contexto, separar domínio, aplicação, infraestrutura e apresentação.

## Alternativas consideradas

- Pacotes globais por camada: simples no início, porém mistura contextos conforme o sistema cresce.
- Microserviços: aumentariam deploy, observabilidade e consistência distribuída sem requisito que justifique esse custo.

## Consequências

Transações entre pedidos e inventário permanecem locais. Fronteiras modulares dependem de disciplina e testes arquiteturais futuros, mas os módulos poderão ser extraídos se surgirem necessidades concretas.
