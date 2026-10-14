# Contributing

Thanks for helping improve Fredplenter.

## Before opening a pull request

1. Check existing issues and pull requests for related work.
2. Keep changes focused and preserve server-authoritative validation for gameplay and progression.
3. For code or resource changes, run the project's tests and build:

   ```sh
   ./gradlew test build
   ```

   On Windows, use `.\gradlew.bat test build`.
4. Describe the player-facing behavior changed, the Minecraft/NeoForge versions used, and the validation performed.

## Repository contents

Commit source code, tests, required mod assets/data, build configuration, and documentation. Do not commit local IDE state, game-run directories, Gradle caches, generated build output, or built mod JARs. Use GitHub Releases for distributable JAR files.

For datapack aspect mappings and gameplay architecture, see [the architecture guide](docs/ARCHITECTURE.md).
