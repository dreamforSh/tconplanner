# Tinker's Planner

A client-side blueprint planner for Tinkers' Construct 3: see exactly what a
tool, armor piece or bow will end up like before you forge it.

[![Available on CurseForge][badge-curseforge]][curseforge]
[![Available on Modrinth][badge-modrinth]][modrinth]
[![Available on GitHub][badge-github]][repo]
[![Supports Forge][badge-forge]][forge]
[![Report issues][badge-issues]][issues]

[![Code license: LGPL-3.0-or-later][shield-license-code]](LICENSE)
[![Asset license: CC BY-NC-SA 4.0][shield-license-assets]](LICENSE.md)
[![Upstream license: MIT][shield-license-upstream]](LICENSE.txt)

## Overview

Tinker's Planner is a purely client-side mod. Once installed, a planner icon
appears in the Tinker Station and Tinker's Anvil GUIs; click it to open the
planner.

Inside the planner you can freely combine any parts — even ones you do not
have on hand — and preview the finished item's full stats, so you never again
gather materials for a tool that turns out to be a disappointment.

## Features

* **Tools and armor modes**: switch between Tools and Armors to plan tools,
  armor and bows.
* **Live stat preview**: durability, mining speed, attack damage, armor,
  accuracy, draw speed and more update as you swap parts.
* **Sorting and search**: sort parts by a chosen attribute, such as mining
  speed or durability, or search materials directly.
* **Bookmarks and stars**: save designs you like and come back to compare
  them later.
* **Modifier simulation**: edit the modifier stack, add or remove levels and
  simulate creative slots.
* **Import and move parts**: import an existing tool from the station into
  the planner, or move planned parts back to the station.
* **Randomize**: roll random material combinations for fun or inspiration.
* **Translations**: English, Simplified Chinese, Japanese, Russian.

## License

This project is derived from tiffit's tconplanner, which is released under
the MIT license. Different parts of this repository carry different licenses:

| Part | License | File |
|------|---------|------|
| Code (`src/main/java`) | LGPL-3.0-or-later | [LICENSE](LICENSE) |
| Textures and artwork | CC BY-NC-SA 4.0 | [LICENSE.md](LICENSE.md) |
| Upstream code | MIT, © 2021 tiffit | [LICENSE.txt](LICENSE.txt) |

Notes:

* MIT permits redistributing a derivative work under a different license as
  long as the original copyright notice is retained; `LICENSE.txt` is that
  notice and must ship with any distribution.
* Code added or modified in this repository is licensed under
  LGPL-3.0-or-later. Distributing a derivative build requires publishing the
  corresponding source changes under the same license.
* Textures, icons and other artwork
  (`src/main/resources/assets/tconplanner/textures` and
  `src/main/resources/logo.png`) are licensed under CC BY-NC-SA 4.0:
  attribution, non-commercial, share-alike. Original files inherited from the
  upstream MIT release remain usable under MIT.
* CC BY-NC-SA 4.0 carries a non-commercial clause. Modpack inclusion,
  reuploads and derivative artwork must stay non-commercial and credit the
  source. For a commercial license, get in touch through [Issues][issues].

## Credits

* **tiffit** — author of the original Tinker's Planner (MIT).
* **SpikyStars**, **xinian** — development and maintenance of this fork.
* **ssakura49** — feature work and Simplified Chinese translation.
* **Twister** — Japanese translation.
* **Slime Knights** — Tinkers' Construct and Mantle.

## See also

* [Tinkers' Construct][tconstruct]
* [Minecraft Forge][forge]
* [Creative Commons BY-NC-SA 4.0][cc-by-nc-sa]
* [GNU LGPL 3.0][lgpl]

[repo]: https://github.com/dreamforSh/tconplanner
[issues]: https://github.com/dreamforSh/tconplanner/issues
[curseforge]: https://www.curseforge.com/minecraft/mc-mods/tinkers-planner-reborn
[modrinth]: https://modrinth.com/mod/tinkers-planner-reborn
[cc-by-nc-sa]: https://creativecommons.org/licenses/by-nc-sa/4.0/
[lgpl]: https://www.gnu.org/licenses/lgpl-3.0.html

[badge-curseforge]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg
[badge-modrinth]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg
[badge-github]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg
[badge-forge]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/forge_vector.svg
[badge-java17]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/java17_vector.svg
[badge-issues]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/issues_vector.svg
[badge-gradle]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/gradle_vector.svg

[shield-mc]: https://img.shields.io/badge/Minecraft-1.19.2-52A535?style=for-the-badge
[shield-forge]: https://img.shields.io/badge/Forge-43%2B-1E2D42?style=for-the-badge
[shield-tcon]: https://img.shields.io/badge/Tinkers%27%20Construct-3.8.4.50%2B-8AB84D?style=for-the-badge
[shield-side]: https://img.shields.io/badge/Side-Client--only-6E5AA8?style=for-the-badge
[shield-version]: https://img.shields.io/badge/Version-1.2.0-informational?style=for-the-badge
[shield-license-code]: https://img.shields.io/badge/Code-LGPL--3.0--or--later-2E6DB4?style=flat-square
[shield-license-assets]: https://img.shields.io/badge/Assets-CC%20BY--NC--SA%204.0-EF9421?style=flat-square
[shield-license-upstream]: https://img.shields.io/badge/Upstream-MIT-4C9A2A?style=flat-square
