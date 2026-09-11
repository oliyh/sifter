# Sifter

A Minecraft (Fabric) mod that adds **the Sifter** — a yellow mob that mines
skulk, moves with a wavy dance, and trades with the player.

This is a learning project. See:

- [`LESSON_PLAN.md`](LESSON_PLAN.md) — the phase-by-phase curriculum we're following.
- [`SETUP.md`](SETUP.md) — how to build the mod and load it into Minecraft on Windows.

## Quick reference

- Minecraft version: **1.21.1**
- Mod loader: **Fabric**
- Language: **Java 21**
- Build tool: **Gradle** (via `./gradlew`, no separate install needed)

```bash
./gradlew build          # compiles the mod, runs the tests, produces a
                           # .jar in build/libs/ (fails if a test fails)
./gradlew test            # just run the automated tests (src/test/java)
./gradlew runClient       # (optional, Linux-only) launches a throwaway test
                           # Minecraft client on THIS machine to sanity-check
                           # the build before deploying to Windows
```
