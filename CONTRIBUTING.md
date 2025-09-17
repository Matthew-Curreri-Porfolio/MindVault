# Contributing to MindVault

Thanks for helping! Before you open a PR:

1. **Follow the style guide**  
   See [`STYLEGUIDE.md`](./STYLEGUIDE.md). We use native conventions per platform.  
   - Kotlin/Android: Kotlin official style (camelCase for code, snake_case for resources)  
   - Python/FastAPI: snake_case  
   - SQL & JSON: snake_case

2. **Formatting & lint**
   - Android: use Android Studio formatter + ktlint.
   - Server: `black`, `isort`, `ruff`.

3. **Security**
   - No secrets in code, logs, or tests.
   - Never print plaintext journals or keys.

4. **PR checklist**
   - Clear title + scope.
   - Tests for new logic.
   - Update docs if contracts change.

5. **Commit style**
   - Conventional-ish: `feat: …`, `fix: …`, `refactor: …`, `chore: …`, `docs: …`.

Thanks for keeping it clean and consistent.

