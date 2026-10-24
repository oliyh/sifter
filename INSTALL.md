# Installing Sifter manually

Every commit to `main` is built by GitHub Actions and published as the
**[latest release](../../releases/latest)** (look for the "Latest build"
pre-release under [Releases](../../releases)).

## Requirements

- Minecraft **1.21.1** (Java Edition)
- [Fabric Loader](https://fabricmc.net/use/installer/) 0.19.5 or newer
- [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.1

## Steps

1. Open the [Releases page](../../releases) and download `sifter-<version>.jar`
   from the **Latest build** release. Ignore any `-sources` jar.
2. Install Fabric Loader for 1.21.1 using the Fabric installer, if you haven't.
3. Download the Fabric API jar for 1.21.1 from Modrinth.
4. Put **both** jars in your mods folder:
   - Windows: `%appdata%\.minecraft\mods`
   - macOS: `~/Library/Application Support/minecraft/mods`
   - Linux: `~/.minecraft/mods`
   - Modrinth App / Prism: right-click the instance, then *Open folder*, then `mods`
5. Start Minecraft with the Fabric profile (or your Fabric instance).

## Updating

Delete the old `sifter-*.jar` from the mods folder, download the new one from
the release page, and put it in the same place.

## Using a pre-release build from a workflow run

If you're signed in to GitHub you can also grab the jar from
**Actions**, then the run, then **Artifacts**, then `sifter-jar`. It comes as a zip, so
unzip it and use the jar inside.
