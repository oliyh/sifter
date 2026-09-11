# Setup

This project is written and built **in this Linux VM**, but the actual game
runs on your **Windows host** via the vanilla Minecraft launcher. This
document covers both sides.

## 1. What's already installed (in this VM)

Checked and confirmed working:

- **JDK 21** (`java -version` → OpenJDK 21.0.12) — exactly what Minecraft
  1.21.1 needs.
- **Gradle** — you don't need to install this. The project ships a
  "Gradle wrapper" (`./gradlew`) that downloads the right Gradle version
  automatically on first use. This already happened once and works.
- **git**.
- **VS Code** (`code` CLI is on `PATH`).

The very first build has already been run successfully in this repo (it
downloaded Minecraft 1.21.1, Fabric Loader, Fabric API, and the official
Mojang mappings — a few hundred MB, cached in `~/.gradle`). Future builds
will be much faster.

## 2. VS Code setup

VS Code works fine for this (Java modding doesn't require IntelliJ, though
you'll see IntelliJ recommended in a lot of tutorials because Fabric's own
team uses it — VS Code's Java tooling has caught up).

Install the two extensions this project recommends (also listed in
`.vscode/extensions.json` — VS Code should prompt you to install them
automatically when you open the folder):

```bash
code --install-extension vscjava.vscode-java-pack
code --install-extension vscjava.vscode-gradle
```

- **Extension Pack for Java** — syntax highlighting, autocomplete, jump-to-
  definition, the debugger.
- **Gradle for Java** — lets VS Code understand the build and run Gradle
  tasks from the sidebar instead of the terminal.

Open the project with `code /home/oliy/dev/sifter`. The first time, give
it a minute or two to index — you'll see a progress notification bottom
right.

## 3. Building the mod

```bash
cd /home/oliy/dev/sifter
./gradlew build
```

This produces `build/libs/sifter-0.1.0.jar` — that file is the mod.
(The version number in the filename comes from `mod_version` in
`gradle.properties`, and will change as we bump it.)

## 4. Getting Fabric running on the Windows host

You need three separate things installed on Windows before your own mod
will load:

### a) Fabric Loader (once)

1. On Windows, download the Fabric installer from fabricmc.net (get the
   "Installer" for the client).
2. Run it. Choose:
   - Minecraft version: **1.21.1**
   - Loader version: latest (0.19.x is fine)
   - Leave "Create profile" checked.
3. This adds a **Fabric Loader 1.21.1** profile to your vanilla launcher.
   You don't need to touch this again unless you change Minecraft version.

### b) Fabric API (once, re-download only if you change Minecraft version)

Our mod depends on Fabric API at runtime (it's not bundled into our jar).
Download the matching version:

- **Fabric API `0.116.17+1.21.1`** for **Minecraft 1.21.1**, from Modrinth
  or CurseForge (search "Fabric API").
- Drop the downloaded `.jar` into your Windows mods folder (see below).

### c) Our mod (every time we rebuild)

Copy `build/libs/sifter-0.1.0.jar` into the same mods folder.

**Windows mods folder location:**

```
%appdata%\.minecraft\mods
```

(Paste that into the Windows File Explorer address bar, or
`C:\Users\<you>\AppData\Roaming\.minecraft\mods` — create the `mods`
folder if it doesn't exist yet, the vanilla launcher creates it the first
time you launch a Fabric profile.)

## 5. Getting the jar from this VM onto Windows

You mentioned you might mount the Windows mods folder inside the VM —
that's the smoothest option. Here's how, using VMware's shared folders:

### One-time setup

1. In your VMware app (Workstation/Fusion/Player), with the VM **shut
   down or paused**: open **VM Settings → Options → Shared Folders**.
2. Enable shared folders, "Always enabled".
3. Add a folder: point it at
   `C:\Users\<you>\AppData\Roaming\.minecraft\mods` on Windows, name it
   something like `mcmods`.
4. Start the VM. Shared folders show up under `/mnt/hgfs/`. Check:

   ```bash
   ls /mnt/hgfs/
   ```

   You should see `mcmods`. (Right now `/mnt/hgfs` exists but is empty —
   that's expected until you add a shared folder in the VM settings above.)

   If `/mnt/hgfs` doesn't appear at all even after adding the shared
   folder, VMware Tools' shared-folder support may not be running:

   ```bash
   sudo apt install open-vm-tools-desktop
   sudo vmhgfs-fuse .host:/ /mnt/hgfs -o allow_other
   ```

### Every time you rebuild

```bash
./gradlew build
cp build/libs/sifter-0.1.0.jar /mnt/hgfs/mcmods/
```

(Once this is working we can wire up a small Gradle task that copies the
jar automatically after every build, if it gets tedious.)

**If shared folders turn out to be awkward** (some VMware/host
combinations are finicky), the fallback is just: build here, then drag
the jar across using whatever file-transfer your VMware setup offers
(shared clipboard file copy, a synced folder like OneDrive/Dropbox
pointed at the mods folder, or a USB-drive-style transfer). Say the word
if `/mnt/hgfs` doesn't cooperate and we'll sort out an alternative.

## 6. First run checklist

1. Confirm the mods folder has (at least): `fabric-api-0.116.17+1.21.1.jar`
   and `sifter-0.1.0.jar`.
2. Open the vanilla Minecraft launcher on Windows, select the
   **Fabric Loader 1.21.1** profile, click Play.
3. On the main menu, click **Mods** — you should see **Sifter** listed
   with its description. That's proof the mod loaded, before you even
   open a world.
4. Open/create a world. You should see the chat message
   *"The Sifter mod says hello!"* appear as you spawn in.

### If something's wrong

- Mod not in the Mods list → wrong Minecraft/Fabric version, or the jar
  didn't actually copy. Check the file's there and its name/size look
  right.
- Game crashes on launch → open
  `%appdata%\.minecraft\logs\latest.log` on Windows and look near the
  bottom for the error; paste it back here and we'll debug it together.
- Fabric API version mismatch errors → the fabric.mod.json in this
  project pins `minecraft: ~1.21.1`; if you ever bump
  `minecraft_version` in `gradle.properties`, you'll need a matching
  Fabric API build and Fabric Loader profile too.

## 7. Optional: testing without Windows at all

Loom can also launch a real Minecraft client *inside this VM* for a quick
sanity check, if this machine has a desktop/display:

```bash
./gradlew runClient
```

First run downloads client assets and will ask you to log in with a
Microsoft account (same as normal Minecraft). This is a good way to catch
crashes early, before copying a jar across to Windows — but it's optional,
not required for the main workflow.
