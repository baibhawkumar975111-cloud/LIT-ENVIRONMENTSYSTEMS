# LIT-Team Environment System

A Paper plugin that keeps the in-game sky in step with the real clock, controls the weather, and gives admins a clean menu for both.

## Requirements

| What | Version |
| --- | --- |
| Paper server | 26.1 or newer (Spigot and CraftBukkit are not supported) |
| Java (server and build) | 25 |
| Gradle (build only) | 9.1 or newer |

No other plugins are needed. Adventure and MiniMessage ship with Paper.

## Building

```text
gradle build
```

The jar ends up in `build/libs/Lit-environmentsystem-1.0.0.jar`. Drop it into the server's `plugins/` folder and restart.

The first build needs internet access to `repo.papermc.io`. If Gradle complains that it cannot find a Java 25 toolchain, install JDK 25 or add the `org.gradle.toolchains.foojay-resolver-convention` plugin to `settings.gradle.kts` so Gradle can download it.

The plugin is compiled against the 26.1 API and declares `api-version: '26.1'`, so it also runs on 26.2 and newer.

## Commands

| Command | What it does |
| --- | --- |
| `/lit` | Open the menu |
| `/lit time status` | Show real time, world time and sync state |
| `/lit time set HH:mm` | Set the time and pause the sync so it stays there |
| `/lit time pause` / `resume` | Freeze the clock or go back to real time |
| `/lit weather clear\|rain\|thunder` | Set the weather in all managed worlds |
| `/lit weather automatic [on\|off]` | Toggle the automatic weather cycle |
| `/lit intro` | Preview the LIT-Team intro on yourself |
| `/lit reload` | Reload `config.yml` and `messages.yml` |

Aliases: `/litenv`, `/environment`.

## Permissions

| Permission | Allows |
| --- | --- |
| `lit.environment.admin` | Everything below, plus world settings and `/lit intro` |
| `lit.environment.use` | Open the menu and view status |
| `lit.environment.time` | Change time, pause and resume |
| `lit.environment.weather` | Change weather |
| `lit.environment.reload` | Reload the configuration |

All default to op.

## How the time sync works

Sunrise is mapped to tick 0 and sunset to tick 12000, using the `sunrise` and `sunset` times in `config.yml`. With the defaults:

| Real time | Game tick | Sky |
| --- | --- | --- |
| 06:00 | 0 | Sunrise |
| 12:00 | 6000 | Noon |
| 18:00 | 12000 | Sunset |
| 00:00 | 18000 | Midnight |

If you move the sunrise and sunset times, the daylight hours stretch or shrink to match, so the sun really does rise and set at those times.

The world time is updated every server tick, and the vanilla daylight cycle is switched off in managed worlds while the plugin runs so the two never fight. The plugin turns the vanilla `advance_time` and `advance_weather` rules back on when it is disabled.

`/lit time set` and the Set Day / Set Night buttons pause the sync, because otherwise the next tick would put the clock straight back to real time. Use `/lit time resume` to follow real time again.

## Weather

Automatic mode picks a new weather every `automatic-cycle-seconds`, weighted by `weather.chances`. Turn it off and the weather stays exactly as you set it.

## Worlds

`worlds.mode` is either `blacklist` (everything except the listed worlds, the default with an empty list) or `whitelist` (only the listed worlds). Only overworld-type worlds are managed; the Nether and the End have no day cycle or weather.

Per-world overrides go under `worlds.per-world.<world-name>` (`enabled`, `time-enabled`, `weather-enabled`, `pause`).

## Startup intro

The console prints a LIT-Team banner when the plugin starts, and players see a title with a rising particle ring shortly after they join. Both can be turned off, and the title, particle, radius and duration are configurable under `startup:`.

## Updating from an older version

Delete the old `plugins/Lit-environmentsystem/config.yml` once so the new one is generated, then restart.
