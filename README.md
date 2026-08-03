# Nametag Changer (Fabric, MC 1.21.4)

A **client-side only** Fabric mod that lets you override the nametag text
shown above other players' heads, purely on your own screen. It does not
change anyone's real name, doesn't sync to the server, and doesn't affect
what other players see.

## How it works

- A Mixin hooks into `EntityRenderer#renderLabelIfPresent` and swaps the
  `Text` argument right before it's drawn, but only for entities that are
  players (`AbstractClientPlayerEntity`) and only if you've set an override
  for that player's UUID.
- Overrides are keyed by UUID (not name), stored in
  `config/nametagchanger.json`, and persist across sessions/servers.

## Commands

All commands are **client-side commands** (registered via Fabric API's
`ClientCommandManager`), so they work even on vanilla servers and don't
require any server-side mod or permissions.

| Command | Effect |
|---|---|
| `/nametag set <player> <new name>` | Show `<new name>` above `<player>` instead of their real name |
| `/nametag reset <player>` | Remove the override for `<player>` |
| `/nametag resetall` | Clear every override |
| `/nametag list` | List all current overrides |

`<player>` is matched against the current tab list (case-insensitive), so
the target player needs to be visible/loaded at least once (e.g. appear in
your tab list) before you can target them by name. You can also pass a raw
UUID directly.

Example:
```
/nametag set Notch WalkingLegend
/nametag reset Notch
```

## Project layout

```
nametag-changer/
├── build.gradle
├── settings.gradle
├── gradle.properties
└── src/main/
    ├── java/com/example/nametagchanger/
    │   ├── NametagChangerClient.java   # mod entrypoint + commands
    │   ├── NametagConfig.java          # UUID -> custom name storage
    │   └── mixin/
    │       └── PlayerEntityRendererMixin.java
    └── resources/
        ├── fabric.mod.json
        └── nametagchanger.mixins.json
```

## Building

Requires JDK 21.

```bash
./gradlew build
```

The compiled jar will be in `build/libs/`. Drop it (the one **without**
`-sources` or `-dev` in the name) into your `.minecraft/mods` folder,
alongside:

- Fabric Loader `>=0.16.9` for Minecraft `1.21.4`
- Fabric API `0.119.0+1.21.4` (or newer for 1.21.4)

## Notes / limitations

- This only changes what *you* see. It cannot be used to impersonate
  someone else to other players, since nothing is sent to the server.
- If a resource pack or another mod also changes nametag rendering, the
  mod that runs its mixin last "wins" — there's no built-in compatibility
  layer for stacking multiple nametag mods.
- Versions pinned in `gradle.properties` (Yarn `1.21.4+build.8`, Loader
  `0.16.9`, Fabric API `0.119.0+1.21.4`) were current at time of writing;
  bump them via the [Fabric documentation](https://fabricmc.net/develop/)
  if newer builds are available.
