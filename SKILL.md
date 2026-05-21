<!-- @format -->

# Collaboration Guide

## Team Workflow

- Start with a short plan for multi-step changes.
- Keep each change small and focused.
- Prefer incremental refactors over rewrites.
- Verify behavior with tests or a build after code changes.

## Architecture Flow

- UI reads input and displays state.
- Controllers handle orchestration and validation.
- Repositories own SQLite access.
- Services keep business rules like FEFO, ABC, and EOQ.

## Hard Rules

- Keep business logic out of UI classes.
- Use repository data instead of hardcoded menu or inventory values.
- Preserve NetBeans-generated form sections unless a change really needs them.
- Update tests when behavior changes.
- Do not commit build output such as `target/`.

## Project Notes

- Database path: `data/coffee-cafe.db`.
- Main app entry: `loginregister.Login`.
- Default test flow: `mvn test`.
- Packaging flow: `mvn clean package`.
