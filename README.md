# gregtech6-neo

Порт **GregTech 6** (Minecraft 1.7.10 / Forge) на **Minecraft 26.1.2 / NeoForge**.

> Статус: **скелет** — поднят инструментарий (сборка + headless-тесты). Портирования
> контента пока нет. Класс `GregTech6` временный (один пример-блок как мишень для
> будущих GameTest'ов); при портировании lifecycle заменится связкой GAPI/GAPI_POST/GT.

## Основа

Сгенерирован из официального шаблона NeoForge **MDK-26.1.2-ModDevGradle**:

| | |
|---|---|
| Сборка | ModDevGradle `net.neoforged.moddev` 2.0.141 |
| Java | 25 (toolchain) |
| Minecraft / NeoForge | 26.1.2 / 26.1.2.77 |
| Gradle | 9.2.1 (wrapper) |

## Команды

```bash
# сборка + юнит-тесты
./gradlew build

# только headless-юнит-тесты (JUnit)
./gradlew test

# headless GameTest-сервер (прогоняет все геймтесты и выходит с кодом = число упавших)
./gradlew runGameTestServer

# датаген ассетов
./gradlew runData

# графический клиент / сервер (для ручной проверки)
./gradlew runClient
./gradlew runServer
```

JVM для запуска Gradle — JDK 25 (`JAVA_HOME` → `jdk-25`). Первый `build` качает NeoForge
и декомпилирует Minecraft (память декомпилятора поднята до 6G в `gradle.properties`).

## Инструментарий тестирования

- **JUnit 5** (`./gradlew test`) — headless-тесты чистой логики; см. `src/test/java/gregtech6/`.
- **NeoForge test framework** (`net.neoforged:testframework`) — тесты в контексте MC-сервера
  через `@ExtendWith(EphemeralTestServerProvider.class)` (headless-реестры).
- **GameTest** (`runGameTestServer`) — поведение в мире, без графики.

## Документация проекта

Архитектура порта, ROADMAP, решения по несовместимостям (F1…F8) и инструментарий живут
в родительском рабочем пространстве: `../doc/missions/gt6-port/` (в этот git-репозиторий не
входят). Точка входа — `../doc/missions/gt6-port/README.md`.
