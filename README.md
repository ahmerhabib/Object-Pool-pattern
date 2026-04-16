# Java Object Pool Studio

Object Pool Studio is a Java project that demonstrates the Object Pool pattern in two ways:
- A core reusable pool engine (`ObjectPool_main`) with safer limits and collector guardrails.
- A modern Swing/AWT desktop app (`ObjectPoolDashboard`) for interactive management, profiling, persistence, and benchmarking.

It is designed for learning, experimentation, and as a practical starter for production-style pool tooling.

## Highlights

- Modern desktop dashboard with real-time active/available views
- Profiles and presets (save/load/delete)
- Settings export/import (`.properties`)
- Benchmark mode (pooled vs no-pool comparison)
- Accessibility controls (light/dark theme, font scaling, high contrast)
- Structured audit/error logs and session/utilization history
- JUnit 5 regression suite + GitHub Actions CI

## Requirements

- Java 11+ (CI runs on Java 21)
- `bash`/`zsh` shell for `mvnw` script usage

## Quick Start

Compile all Java sources:

```sh
javac -d bin src/*.java
```

Run the desktop app:

```sh
java -cp bin ObjectPoolDashboard
```

Run the console demos:

```sh
java -cp bin ObjectPool
java -cp bin ObjectPool_main
```

Run tests (recommended via wrapper):

```sh
./mvnw test
```

Alternative if Maven is already installed:

```sh
mvn test
```

## Dashboard Features

- Create/reset pools with validated constraints
- Borrow/release objects from the UI
- Search/filter object lists
- Inspect per-object detail panel
- Track utilization trend over time
- Save and load named configurations
- Export and import configuration files
- Record audit events and errors to disk

## Project Structure

- `src/ObjectPool.java`: simple pool demo
- `src/ObjectPool_main.java`: advanced pool implementation
- `src/ObjectPoolDashboard.java`: Swing/AWT desktop interface
- `src/DashboardConfig.java`: defaults, validation, sanitization
- `src/PoolProfileStore.java`: profile persistence and config import/export
- `src/DashboardPersistence.java`: audit/error/session/utilization persistence
- `src/BenchmarkService.java`: benchmark engine
- `src/DashboardUiSupport.java`: filter/detail/trend helpers
- `src/test/java`: JUnit test suite
- `.github/workflows/ci.yml`: CI test workflow

## Runtime Data

Generated at runtime (git-ignored):

- `data/profiles/`
- `data/logs/`
- `data/history/`

## CI

GitHub Actions runs tests on every push and pull request:

- `.github/workflows/ci.yml`

## License

MIT License (see `LICENSE.MD`).
