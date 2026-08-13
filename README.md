# PlayerLocatorPlus NeoForged

[Modrinth](https://modrinth.com/mod/playerlocatorplus-neoforged) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/playerlocatorplus-neoforged) · [Issues](https://github.com/kanoyo-git/PlayerLocatorPlus-Neoforged/issues)

PlayerLocatorPlus NeoForged adds a compass-like multiplayer HUD above the experience bar. It shows the direction of other players in the same dimension, making it easier to find teammates without opening a map or waypoint screen.

This project is a NeoForge 1.21.1 port of [Player Locator Plus by timas130 and contributors](https://github.com/timas130/PlayerLocatorPlus).

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

Server parameters:
- `enabled`: Is the mod enabled server-side at all. Note that the bar will still be visible if `visible` is true. Default: `true`
- `sendServerConfig`: Whether the server will try to force its config on the client. This will only have effect if the clients have `acceptServerConfig` set to true. Default: `true`
- `sendDistance`: Whether to send distance information along with the direction information.
  There's basically no point in settings this to `false`, as players can still easily
  triangulate the exact location of others even without knowing the distance.
  However, it is harder with this option on, especially if you consider that other players
  move.
  Default: `true`
- `maxDistance`: The maximum distance at which other players are visible on the compass.
  Default: `0` (unlimited)
- `directionPrecision`: The amount of segments the direction vector is split into. Decreasing this value significantly will make tracking way more inaccurate but be helpful in preventing triangulation. Default: `300`
- `ticksBetweenUpdates`: How many ticks apart are the compass updates.
  The less, the smoother the movements of faraway players are.
  Close players (inside the entity render distance) do not depend on this parameter as much.
  Default: `5` (four times per second)
- `sneakingHides`: Whether sneaking hides players from the locator. Default: `true`
- `pumpkinHides`: Whether wearing a carved pumpkin hides players from the locator.
  Default: `true`
- `mobHeadsHide`: Whether wearing a mob/player head hides players from the locator. The exact list can be edited with a datapack by changing `data/player-locator-plus/tags/item/hiding_equipment.json` (see default [here](https://raw.githubusercontent.com/kanoyo-git/PlayerLocatorPlus-Neoforged/refs/heads/main/src/main/resources/data/player-locator-plus/tags/item/hiding_equipment.json). Default: `true`
- `invisibilityHides`: Whether being invisible hides players from the locator. Default: `true`
- `colorMode`: How to determine the color of the markers. Available modes:
  - `UUID` (default): Assign a random color based on the UUID of the player.
  - `TEAM_COLOR`: Use the color of the player's team (or white)
  - `CUSTOM`: Allow every player to assign a color with the `/plp color` command
  - `CONSTANT`: Everyone has the same color from the `constantColor` option
- `constantColor`: Color used when `colorMode` is `CONSTANT`. Default: `0xFFFFFF` (white)
  
Client parameters:
- `visible`: Show the player locator. Default: `true`
- `visibleEmpty`: Show the player locator even there are no players online and no markers in sight. Default: `false`
- `acceptServerConfig`: Whether to override the client-side config with the server-side one. The server config will also be able set client parameters. Default: `true`
- `fadeMarkers`: Fade markers of faraway players. Default: `true`
- `fadeStart`: At what distance the markers start to fade. Default: `100`
- `fadeEnd`: At what distance the markers stop fading and settle at `fadeEndOpacity`. Default: `1000`
- `fadeEndOpacity`: The final opacity when/after `fadeEnd` is reached. Default: `0.3`
- `showHeight`: Show little arrows above/below a marker if the height difference is significant.
  Default: `true`
- `alwaysShowHeads`: Always show player heads, regardless of whether Tab is pressed. Default: `false`
- `showHeadsOnTab`: Show player heads when Tab is pressed. Default: `true`
- `showNamesOnTab`: Show player names when Tab is pressed. Default: `true`

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
