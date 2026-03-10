# Repository Guidelines

## Project Structure & Module Organization
- Source code lives under `src/main/java/com/yusheng/aiagentproject/` with feature areas like `app/`, `controller/`, and `demo/`.
- Resources are in `src/main/resources/` (e.g., `static/`, `templates/`).
- Tests are under `src/test/java/com/yusheng/aiagentproject/` mirroring main package layout.
- Build configuration is `pom.xml` (Spring Boot, Java 21). The Maven wrapper scripts are `mvnw` and `mvnw.cmd`.

## Build, Test, and Development Commands
Use the Maven wrapper to avoid local Maven version drift.

```bash
./mvnw -q -DskipTests package
```
Builds the project without running tests.

```bash
./mvnw test
```
Runs the test suite.

```bash
./mvnw spring-boot:run
```
Starts the application locally using Spring Boot.

## Coding Style & Naming Conventions
- Language: Java 21. Use 4-space indentation and standard Spring Boot conventions.
- Package names: lowercase, e.g., `com.yusheng.aiagentproject.app`.
- Classes: `UpperCamelCase` (e.g., `LoveApp`).
- Methods/fields: `lowerCamelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- Prefer Lombok annotations where already used (e.g., `@Slf4j`). No formatter or linter is configured in this repo.

## Testing Guidelines
- Framework: Spring Boot test starter (JUnit 5 by default).
- Test classes should follow `*Test` suffix and mirror the package of the class under test.
- No explicit coverage threshold is configured.

## Commit & Pull Request Guidelines
- Existing commit messages are inconsistent (e.g., ¡°Initial commit¡±, ¡°Merge branch¡­¡±). Adopt concise, imperative summaries going forward, optionally with a scope prefix.

Examples:
- `feat: add chat memory advisor`
- `fix: handle null response in LoveApp`

- PRs should include a short summary, testing notes (commands run), and screenshots if UI changes are made. Link related issues if applicable.

## Security & Configuration Tips
- API keys or model credentials should be provided via environment variables or external config, not committed to the repo.
- Avoid committing generated files under `target/`.
