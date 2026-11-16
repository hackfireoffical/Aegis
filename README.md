# Aegis

Realistic missiles and radar for Minecraft (NeoForge 1.21.1, Java 21, mod id `aegis`).

## Features (v0.2)
- **Radar block**: scans 64 blocks, tracks mobs, players and missiles. Right-click opens a PPI scope
  with a rotating sweep, fading blips, bearings and distances.
- **Missile item**: right-click fires a missile. It locks onto the living entity nearest your crosshair
  (96 blocks, line of sight needed). Flight: boost, homing cruise with target lead, tighter terminal
  turns, then burnout and ballistic fall. Explodes on impact, with a smoke and flame trail.
- **Missile Launcher block**: load up to 4 missiles by right-clicking with them, fire with a redstone pulse. Aims at the nearest hostile mob within 64 blocks, otherwise fires straight up. Drops its missiles when broken.
- **Radar redstone output**: strength 1-15 that rises as a tracked missile gets closer (0 when none); comparators read the contact count.
- Recipes for all items; creative tab "Aegis".

## Plan
Launcher block, radar warning output (redstone), proper missile model and renderer,
sounds, flares / anti-missile defence, config options.

## Build
GitHub Actions builds the jar (`.github/workflows/build.yml`); download it from the run artifacts.
