# Building and running

[← README](README.md)

## Requirements

You need an installed **JDK 25** (**JDK 17** for the `1.20.1` branch), about **8 GB of free RAM** and
**10 GB of disk**. The first build downloads NeoForge and decompiles Minecraft, which is what the memory
is for (the decompiler inherits the Gradle JVM heap, set to 6 GB in `gradle.properties`) — and it takes
a while. Later builds are fast.

## Building

```bash
./gradlew build           # compile, jar, tests
./gradlew assemble        # just the jar -> build/libs/gregtech6-<version>.jar
./gradlew assemble -Pgt6u # GT6 and the separate optional GT6U addon jar
./gradlew compileJava     # just compile
```

The build downloads only the platform: Gradle, NeoForge, Minecraft and their libraries. It never fetches
other mods or a JDK. Integrations with other mods compile against the declarations in
`src/compat-mirror/provided`, which never enter the jar.

## GT6U addon

The optional GT6U addon is built from this repository as a second mod:
`build/libs-gt6u/gt6u-<version>.jar`. The addon has its own version and declares the exact GregTech 6
version it requires in its metadata; the current values live in `gradle.properties` (`gt6u_version`
and `mod_version`). Install both jars for the same Minecraft version. Without the addon, GregTech 6
runs on its own.

## Running

```bash
./gradlew runClient        # development client
./gradlew runServer        # dedicated server (own directory: ./runserver)
./gradlew runClient -Pgt6u # run with the optional GT6U addon
./gradlew runData          # regenerate generated data files
```

Client and server deliberately use separate game directories, so both can run at once.

To run with JEI, Jade, Applied Energistics 2, JourneyMap, More Red or BuildCraft, place their jars in
`../stands/_mods` (`../stands-1.20.1/_mods` on the `1.20.1` branch) under the file names that
`build.gradle` lists; the runs pick up whichever are present.

## Building without a network

<details>
<summary>Local mode</summary>

If a folder named `platform` sits beside the repository (`../platform`), `gradlew` switches the build
to local mode: nothing is downloaded, and every input comes from that folder. The folder is not part of
this repository — it holds Minecraft files, which may not be redistributed — so you assemble your own
from files you have downloaded once:

| Path under `../platform` | Content |
|---|---|
| `gradle/` | an unpacked Gradle distribution of the version named in `gradle/wrapper/gradle-wrapper.properties` |
| `maven/` | a Maven-layout repository with every plugin and dependency of the build, POM and module files included |
| `minecraft/resolved/launcher_manifest.json` | a launcher manifest whose version entry, and every address inside that version's manifest, is a `file:` URI |
| `minecraft/assets/objects/` | the game's asset objects, stored by hash as the launcher stores them |
| `jdk/21/` | a JDK 21 — the build plugin starts its Minecraft pipeline on it |
| `jdk/25/` | a JDK 25 for the `main` branch; `1.20.1` also needs `jdk/17/` |

</details>

## In-game checks

<details>
<summary><code>-Pgt6probes</code> flag</summary>

There is one opt-in build flag, `-Pgt6probes`, which attaches the in-engine verification stands. Those
live **outside this repository**, in the developer's working environment (`../stands`). The repository
contains none of them, so they cannot end up in a player's jar. Given that directory and the flag,
`runClient`/`runServer` can run automated in-game checks.

</details>

## Verifying a download

Each release jar is published with a `.sha256` file and a GitHub build-provenance attestation. Two
independent checks:

```bash
gh attestation verify gregtech6-<version>.jar --repo wolfram0108/gregtech6_w
gh attestation verify gt6u-<addon-version>.jar --repo wolfram0108/gregtech6_w
```

The attestation confirms that GitHub itself witnessed the exact file being built from the tagged commit
by this repository's release workflow. The build is reproducible, so the claim can also be checked by
building the tag:

```bash
./gradlew assemble -Pgt6u && sha256sum build/libs/*.jar build/libs-gt6u/*.jar
```

The result must equal the published `.sha256` files byte for byte.
