# Player Locator Plus NeoForged

[Modrinth](https://modrinth.com/mod/playerlocatorplus-neoforged) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/playerlocatorplus-neoforged) · [Issues](https://github.com/kanoyo-git/PlayerLocatorPlus-Neoforged/issues)

Player Locator Plus NeoForged adds a compass-like multiplayer HUD above the experience bar. It shows the direction of other players in the same dimension, making it easier to find teammates without opening a map or waypoint screen.

This project is a NeoForged 1.21.1 port of [Player Locator Plus by timas130 and contributors](https://github.com/timas130/PlayerLocatorPlus).

![Player locator overview](https://raw.githubusercontent.com/kanoyo-git/PlayerLocatorPlus-Neoforged/main/screenshot1.png)

## Features

- Colored player markers using UUID, team, custom, or shared colors
- Player heads and names while holding Tab
- Distance-based marker fading and shrinking
- Up/down indicators for large height differences
- Configurable tracking range, direction precision, and update rate
- Server-controlled hiding while sneaking, invisible, or wearing configured equipment
- Correct marker cleanup and synchronization when changing dimensions
- Vanilla experience and jump indicators retain HUD priority when needed

![Distance-based marker fading](https://raw.githubusercontent.com/kanoyo-git/PlayerLocatorPlus-Neoforged/main/screenshot2.png)

## Installation

- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21
- Install on both the client and server

The release jar includes its required Kotlin and TOML runtime libraries. Fabric API, Fabric Language Kotlin, and Cloth Config are not required.

## Configuration

Configuration is stored in `config/player-locator-plus.toml`. After editing server settings, run `/plp reload` as an operator or restart the server.

Server owners can disable tracking, set a maximum range, reduce direction precision, control update frequency, and choose whether distance data is sent. Players can be hidden while:

- Sneaking
- Invisible
- Wearing a carved pumpkin
- Wearing equipment in the `player-locator-plus:hiding_equipment` item tag

The equipment tag can be changed with a datapack. Disabling distance data does not prevent approximate triangulation from directional updates; use `directionPrecision` and `maxDistance` for stronger limits.

![Player hidden by a carved pumpkin](https://raw.githubusercontent.com/kanoyo-git/PlayerLocatorPlus-Neoforged/main/screenshot3.png)

## Commands

- `/plp reload` — reload the configuration (operator only)
- `/plp color <color>` — set your marker color when custom colors are enabled
- `/plp color <color> <player>` — set another player's marker color (permission level 3)
- `/plp random` — display test markers (permission level 2)

Colors can be Minecraft color names or six-digit hexadecimal values such as `#ff8800`.

## NeoForge port

The original mod provides the locator HUD, marker customization, hiding rules, commands, and client/server configuration model. This fork:

- Ports the project from Fabric to NeoForge for Minecraft 1.21.1
- Targets Java 21 and packages the required runtime libraries
- Reimplements HUD integration using NeoForge GUI layers
- Fixes stale markers after players change dimensions

This is an independent fork and is not endorsed by the upstream authors. Full attribution is available in [NOTICE.md](https://github.com/kanoyo-git/PlayerLocatorPlus-Neoforged/blob/main/NOTICE.md).

## License

Licensed under [GPL-3.0-or-later](https://github.com/kanoyo-git/PlayerLocatorPlus-Neoforged/blob/main/LICENSE), matching the upstream project.
