# Repository Guidelines

## Project Structure & Module Organization
The application is a Java 21 Spring Boot project. Main code lives in `src/main/java/com/yusheng/aiagentproject/` with feature packages such as `app/`, `advisor/`, `config/`, `controller/`, `demo/`, `rag/`, and `chatmemory/`. Configuration and bundled content live in `src/main/resources/`, including `application.yml` and the Markdown knowledge-base files under `src/main/resources/doc/`. Tests mirror the main package layout in `src/test/java/com/yusheng/aiagentproject/`. Project notes and learning documents are kept in `document/`.

## Build, Test, and Development Commands
Use the Maven wrapper to keep builds consistent:

```bash
./mvnw -q -DskipTests package
./mvnw test
./mvnw spring-boot:run
```

`package` builds the jar without running tests. `test` runs the Spring Boot test suite. `spring-boot:run` starts the app locally on port `8123` with context path `/api`.

## Coding Style & Naming Conventions
Use 4-space indentation and standard Spring Boot conventions. Package names stay lowercase, for example `com.yusheng.aiagentproject.rag`. Classes use `UpperCamelCase`; methods and fields use `lowerCamelCase`; constants use `UPPER_SNAKE_CASE`. Prefer Lombok where the project already uses it, such as logging or boilerplate reduction. No formatter or linter is configured, so keep imports tidy and follow surrounding code style.每次编写或者修改代码后保证代码不出现中文乱码的情况。

## Testing Guidelines
Tests use Spring Boot Test with JUnit 5. Name test classes with the `*Test` suffix and place them in the matching package, for example `src/test/java/com/yusheng/aiagentproject/app/LoveAppTest.java`. Add focused tests for new advisors, controllers, and RAG configuration paths. Run `./mvnw test` before opening a pull request.

## Commit & Pull Request Guidelines
Recent history mixes descriptive Chinese summaries with generic merge commits. For new work, prefer short imperative messages with an optional scope, such as `feat(rag): add query rewrite fallback` or `fix(advisor): handle empty forbidden words`. Pull requests should include a concise summary, impacted areas, test commands run, related issues, and screenshots when API docs or UI output change.

## Security & Configuration Tips
Do not commit secrets. Supply `MYSQL_URL`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`, `PG_URL`, `PG_USERNAME`, `PG_PASSWORD`, and model credentials through environment variables or external config. Avoid committing generated output under `target/`.

## Mandatory Skill Routing

Use skills proactively whenever they clearly match the task.

- Use `codebase-summarizer` before modifying unfamiliar modules or when summarizing existing flows.
- Use `systematic-debugging` for exceptions, startup failures, unexpected behavior, integration failures, and unclear root causes.
- Use `system-architect` for new modules, package boundaries, extensibility design, and major refactors.
- Use `code-review-excellence` when reviewing maintainability, readability, edge cases, or patch quality.
- Use `requirements-analysis` when the request is vague or multiple implementation paths exist.
- Use `java-springboot` for controllers, services, configs, beans, validation, exception handling, and Spring Boot best practices.
- Use `spring-ai-mcp-server-patterns` for MCP, tool exposure, agent-tool integration, and model-driven orchestration patterns.
- Use `spring-boot-engineer` when the task is broader engineering delivery around Spring Boot systems.
- Use `security-threat-model` when prompts, external APIs, uploaded files, secrets, permissions, or tool execution are involved.
- Use `sre-monitoring-and-observability` when adding logs, metrics, traces, alerts, or production diagnostics.
- Use `deployment-procedures` or `devops-engineer` for environment setup, CI/CD, deployment, rollback, and release procedures.
- Use `test-master` when designing or revising unit tests, integration tests, regression tests, or test strategy.
- Use `penpot-uiux-design` only for explicit UI/UX or prototype design tasks.
- Use `find-skills` when the correct skill is not obvious or the task spans multiple domains.

If a task strongly matches a skill, prefer using that skill rather than solving it in an ad hoc way.

## File editing rules
- Prefer structured patch edits for existing files when the tool is available.
- If patch editing fails, do not use PowerShell redirection, Out-File, or Set-Content without explicit UTF-8 no BOM encoding.
- On Windows, never write source files with BOM.
- Preserve the existing file encoding and line endings when editing an existing file.
- For Java, XML, YAML, properties, JSON, Markdown, and source files, use UTF-8 without BOM.

## Safe fallback strategy
- For small edits: modify files with a patch-based tool if available.
- For full-file rewrites: write to a temporary file with UTF-8 no BOM, then atomically replace the target file.
- After editing, verify the target file has no BOM and that the build still passes.

## Validation rules
- After any file creation or rewrite, run encoding validation.
- After Java file edits, run the project compile/test command before finishing.