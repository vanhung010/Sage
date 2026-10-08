# Saga Res Backend Agent Instructions

## Project

This directory contains the Spring Boot backend for Saga Res.

Tech stack:

- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven

Base package:

`com.sagares.saga_res_api`

## Required context

Before implementing or modifying backend business logic, read:

- `docs/backend-business-rules.md`

Before modifying JPA entities, repositories, mappings, locking, or database-facing code, also read:

- `docs/05-jpa-entities.md`

Before modifying authentication, authorization, JWT, or Spring Security, also read:

- `docs/09-authentication-jwt-security-design.md`

Treat these documents as the backend implementation contract.

Do not invent new business rules, statuses, fields, transitions, or workflows that are not documented.

## Architecture

Use package-by-feature structure.

Examples:

- `account`
- `auth`
- `security`
- `restaurant`
- `menu`
- `table`
- `reservation`
- `order`

Within a feature, use clear responsibilities such as:

- `controller`
- `dto`
- `service`
- `repository`
- `entity`

Do not introduce a global layer-only architecture.

Do not create large god services.

## API conventions

- Do not expose JPA entities directly from controllers.
- Use request/response DTOs.
- Return DTOs directly for normal `200 OK` responses.
- Use `ResponseEntity` when a custom HTTP status or header is required.
- Do not wrap every success response in a generic `ApiResponse<T>`.
- Standardize error responses through the global exception handling layer.
- Use appropriate HTTP status codes, especially `400`, `401`, `403`, `404`, and `409`.

## Security

- Authentication is stateless JWT authentication.
- Do not add refresh tokens, blacklist tables, or server-side sessions unless the specification changes.
- CUSTOMER self-registration must always create role `CUSTOMER`.
- Never trust role or ownership information supplied by the client.
- CUSTOMER-owned resources must enforce ownership in the query/service layer.
- A valid role check does not replace ownership checks.

## Persistence

- Flyway migrations are the source of truth for schema changes.
- Do not rely on Hibernate schema generation.
- Keep JPA mappings synchronized with the database schema.
- Use `EnumType.STRING`.
- Prefer LAZY for `ManyToOne`.
- Do not use Lombok `@Data` on JPA entities.
- Preserve required pessimistic and optimistic locking semantics documented in `docs/`.

## Transactions and concurrency

- Put business transaction boundaries in service methods.
- Do not remove locking or database constraints merely because an application-level validation exists.
- Reservation overlap correctness must remain safe under concurrent requests.
- Preserve account-row locking for active-reservation limits.
- Preserve table-row locking for reservation assignment and table suspension races.

## Coding rules

- Prefer simple, explicit code over unnecessary abstraction.
- Reuse existing project conventions before introducing a new pattern.
- Keep methods focused on one business responsibility.
- Avoid premature generic frameworks.
- Validate input at API boundaries and enforce business rules in services.
- Do not silently change existing API contracts.

## Before changing code

1. Inspect existing implementation.
2. Inspect the relevant documentation.
3. Check existing migration/schema constraints.
4. Identify affected business rules.
5. Make the smallest coherent change.
6. Update or add tests for affected behavior.

## Testing

When changing business logic, add or update tests for:

- authorization;
- ownership;
- validation;
- transaction behavior;
- state transitions;
- concurrency where relevant.

Do not weaken existing tests just to make a change pass.

## Scope control

Do not implement features explicitly marked out of scope in `docs/backend-business-rules.md`.

If a requested change conflicts with the documented design, point out the conflict before changing the implementation.