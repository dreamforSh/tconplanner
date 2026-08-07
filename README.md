# Tinker's Planner

匠魂 3（Tinkers' Construct 3）的客户端蓝图规划器：在真正锻造之前，
先把工具、盔甲和弓的最终属性看个清楚。

[![在 GitHub 上获取][badge-github]][repo]
[![支持 Forge][badge-forge]][forge]
[![使用 Java 17 构建][badge-java17]][java]
[![反馈问题][badge-issues]][issues]

[![Minecraft][shield-mc]][mc]
[![Forge][shield-forge]][forge]
[![Tinkers' Construct][shield-tcon]][tconstruct]
![仅客户端][shield-side]
![模组版本][shield-version]

[![代码许可：LGPL-3.0-or-later][shield-license-code]](LICENSE)
[![资源许可：CC BY-NC-SA 4.0][shield-license-assets]](LICENSE.md)
[![上游许可：MIT][shield-license-upstream]](LICENSE.txt)

## 简介

Tinker's Planner 是一个纯客户端模组。安装后，工匠台（Tinker Station）
与工匠砧（Tinker's Anvil）的界面上会出现一个规划器图标，点击即可打开
规划界面。

在规划界面中，你可以自由组合任意部件（即使背包里并没有这些材料），
实时预览成品的全部属性，从而避免“辛苦凑齐材料，做出来却不理想”的情况。

## 功能特性

* **工具与盔甲双模式**：在 Tools / Armors 之间切换，规划工具、盔甲与弓。
* **属性实时预览**：耐久、挖掘速度、攻击力、护甲值、精准度、拉弓速度等
  属性随部件改变即时刷新。
* **部件排序与搜索**：按指定属性（如挖掘速度、耐久）排序，或直接搜索材料。
* **书签与收藏**：把满意的设计收藏下来，随时回看、对比。
* **强化（Modifier）模拟**：编辑强化栈、增减等级、模拟创造槽位。
* **一键导入 / 移动部件**：把工匠台中的现有工具导入规划器，或把规划好的
  部件移动回工匠台。
* **随机组合**：随机搭配材料，用来碰运气或找灵感。
* **多语言**：English、简体中文、日本語、Русский。

## 环境要求

| 项目 | 版本 |
|------|------|
| Minecraft | 1.19.2 |
| Forge | 43.5.0 及以上（`[43,)`） |
| Tinkers' Construct | 3.8.4.50 及以上 |
| Mantle | 1.10.36 |
| Java | 17 |
| 运行端 | 仅客户端（服务端无需安装） |

JEI 11.6.0.1024 仅用于开发环境运行时，不是游玩时的必需依赖。

## 安装

1.  安装 Forge 43.5.0 或更高版本。
2.  安装 Tinkers' Construct 3.8.4.50 及其依赖 Mantle。
3.  把本模组的 jar 放入 `.minecraft/mods` 目录。
4.  启动游戏，打开工匠台或工匠砧，点击界面上的规划器图标。

## 从源码构建

[![使用 Gradle 构建][badge-gradle]][gradle]

```shell
./gradlew build
```

产物位于 `build/libs/`。在 IDE 中调试可使用 `./gradlew runClient`。

## 许可

本项目基于 tiffit 以 MIT 许可发布的 tconplanner 二次开发，并对不同部分
采用不同许可：

| 部分 | 许可 | 许可文件 |
|------|------|----------|
| 代码（`src/main/java`） | LGPL-3.0-or-later | [LICENSE](LICENSE) |
| 材质与美术资源 | CC BY-NC-SA 4.0 | [LICENSE.md](LICENSE.md) |
| 上游原始代码 | MIT，© 2021 tiffit | [LICENSE.txt](LICENSE.txt) |

需要注意：

* MIT 允许在保留原始版权声明的前提下以其他许可再发布衍生作品，
  `LICENSE.txt` 即为必须随分发保留的上游声明。
* 本仓库新增与修改的代码以 LGPL-3.0-or-later 授权。分发衍生版本时，
  需要以同一许可公开相应的源码改动。
* 材质、图标等美术资源（`src/main/resources/assets/tconplanner/textures`
  与 `src/main/resources/logo.png`）以 CC BY-NC-SA 4.0 授权：
  署名、非商业、相同方式共享。其中源自上游 MIT 版本的原始文件，
  仍可继续按 MIT 使用。
* CC BY-NC-SA 4.0 含非商业条款。整合包收录、转载与二次创作请保持非商业
  用途并注明来源；如需商业授权，请通过 [Issues][issues] 联系维护者。

## 致谢

* **tiffit** —— 原版 Tinker's Planner 作者（MIT）。
* **SpikyStars**、**xinian** —— 本分支的开发与维护。
* **ssakura49** —— 功能增强与简体中文翻译。
* **Twister** —— 日本語翻译。
* **Slime Knights** —— Tinkers' Construct 与 Mantle。

## English

Tinker's Planner is a client-side add-on for Tinkers' Construct 3 that lets
you preview assembled tools, armor and bows in a blueprint interface before
committing materials to them.

After installing the mod, a planner icon appears in the Tinker Station and
Tinker's Anvil GUIs; click it to open the planner. There you can combine any
parts — even ones you do not own — and preview the resulting stats, bookmark
designs for later, sort parts by attributes such as mining speed or
durability, and randomize materials for fun.

Licensing: code is LGPL-3.0-or-later, textures and artwork are CC BY-NC-SA
4.0, and the upstream work by tiffit remains under MIT. See the
[许可](#许可) section above for details.

## 另请参阅

* [Tinkers' Construct][tconstruct]
* [Minecraft Forge][forge]
* [Creative Commons BY-NC-SA 4.0][cc-by-nc-sa]
* [GNU LGPL 3.0][lgpl]

[repo]: https://github.com/dreamforSh/tconplanner
[issues]: https://github.com/dreamforSh/tconplanner/issues
[forge]: https://files.minecraftforge.net/
[java]: https://adoptium.net/
[gradle]: https://gradle.org/
[mc]: https://www.minecraft.net/
[tconstruct]: https://www.curseforge.com/minecraft/mc-mods/tinkers-construct
[cc-by-nc-sa]: https://creativecommons.org/licenses/by-nc-sa/4.0/
[lgpl]: https://www.gnu.org/licenses/lgpl-3.0.html

[badge-github]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg
[badge-forge]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/forge_vector.svg
[badge-java17]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/java17_vector.svg
[badge-issues]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/issues_vector.svg
[badge-gradle]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/gradle_vector.svg

[shield-mc]: https://img.shields.io/badge/Minecraft-1.19.2-52A535?style=for-the-badge
[shield-forge]: https://img.shields.io/badge/Forge-43%2B-1E2D42?style=for-the-badge
[shield-tcon]: https://img.shields.io/badge/Tinkers%27%20Construct-3.8.4.50%2B-8AB84D?style=for-the-badge
[shield-side]: https://img.shields.io/badge/%E8%BF%90%E8%A1%8C%E7%AB%AF-%E4%BB%85%E5%AE%A2%E6%88%B7%E7%AB%AF-6E5AA8?style=for-the-badge
[shield-version]: https://img.shields.io/badge/%E7%89%88%E6%9C%AC-1.2.0-informational?style=for-the-badge
[shield-license-code]: https://img.shields.io/badge/%E4%BB%A3%E7%A0%81-LGPL--3.0--or--later-2E6DB4?style=flat-square
[shield-license-assets]: https://img.shields.io/badge/%E8%B5%84%E6%BA%90-CC%20BY--NC--SA%204.0-EF9421?style=flat-square
[shield-license-upstream]: https://img.shields.io/badge/%E4%B8%8A%E6%B8%B8-MIT-4C9A2A?style=flat-square
