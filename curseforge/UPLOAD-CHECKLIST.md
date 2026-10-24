# Autarkia: CurseForge upload checklist

All files are in `E:\minecraft-mods\autarkia\curseforge\`. The images are in `images\`. To make the images again, run `F:/envs/5070_11/python.exe tools/curseforge_art.py`.

## 1. Before you start

- **License.** The mod jar says `MIT` (from 0.11.1). Select **MIT License** on CurseForge. The jar must always have the same license as the project page.
- **Account.** Sign in at authors.curseforge.com with your CurseForge account.

## 2. Create the project

| Field | Value |
|---|---|
| Game | Minecraft |
| Class | Mods |
| Name | Autarkia |
| Summary | Copy the text of `summary.txt`. |
| Logo / avatar | `images/logo.png` (512 x 512) |
| Categories (examples) | Technology, Automation, Processing, Energy, Item and Fluid Transport, Armor, Tools and Weapons |
| License | MIT License (see section 1) |
| Description | See section 3. |

## 3. Description with images

CurseForge shows an image in the description only from a web address. So upload the images first.

1. Open the project and go to **Gallery**. Upload these images: `banner.png`, `tier_1.png` to `tier_6.png`, `workers.png`, `armor.png`, `items_equipment.png`, `items_materials.png`, `items_components.png`, `machines_all.png`, and `scene_1_muscle_and_timber.png` to `scene_6_ascendant.png`.
2. Open the description editor.
   - If the editor has a **Markdown** mode: paste `description.md`. Replace each image file name, for example `banner.png`, with the web address of that image from the gallery.
   - If the editor has only the visual mode: open `description.html` in a browser, then copy and paste the text. Insert each image at its place with the image button of the editor.
3. Save.

## 4. Upload the file

| Field | Value |
|---|---|
| File | `E:\minecraft-mods\autarkia\build\libs\autarkia-0.14.7.jar` |
| Display name | Autarkia 0.14.7 |
| Release type | **Alpha** (tier 6 and the newest integrations did not run in the game) |
| Game version | 26.3 (if CurseForge does not list 26.3 yet, wait until it does) |
| Mod loader | NeoForge |
| Environment | Client and Server |
| Changelog | Copy the text of `changelog-0.12.1-to-0.14.7.md` (all changes from 0.12.1 to 0.14.7 in one file) (for the first upload, also the older `changelog-*.md` files). |

### Relations (dependencies)

Add each project by its CurseForge name. The version numbers are the minimum versions in the mod file.

| Relation | Project | Mod id | Minimum version |
|---|---|---|---|
| Required dependency | GeckoLib | `geckolib` | 5.5.7 |
| Required dependency | SmartBrainLib | `smartbrainlib` | 2.0.3 |
| Required dependency | Tiny Multiblock Lib | `tinymultiblocklib` | 4.0.1 |
| Required dependency | Magic Particles Lib | `magicparticleslib` | 1.9.0 |
| Required dependency | GuideME | `guideme` | 26.3.4-alpha (exact version) |
| Required dependency | FrozenLib | `frozenlib` | 3.0.3 |
| Required dependency | Player Animation Library | `player_animation_library` | 1.2.8 (client only) |
| Required dependency | anthology | `anthology` | 1.0.0 |
| Optional dependency | Just Enough Items (JEI) | `jei` | 31.14 |
| Optional dependency | Jade | `jade` | 26.3.5 |
| Optional dependency | YetAnotherConfigLib (YACL) | `yet_another_config_lib_v3` | 3.9.7 |

If a required library is not on CurseForge, players cannot install Autarkia from CurseForge. Tell me which one, and we find a solution.

## 5. Send for review

- Submit the project. CurseForge moderators review each new project before it becomes public. This usually takes some days.
- After the approval, players find Autarkia in the CurseForge app and on the website.

## Files in this folder

| File | Use |
|---|---|
| `summary.txt` | Project summary (one sentence). |
| `description.md` | Project description, Markdown. |
| `description.html` | The same description as HTML, for the visual editor. |
| `changelog-*.md` | Changelogs of the file uploads (0.11.1 to 0.14.7). |
| `changelog-0.12.1-to-0.14.7.md` | All changes from 0.12.1 to 0.14.7, sorted by topic. |
| `images/logo.png` | Project logo, 512 x 512. |
| `images/banner.png` | Header image for the description, 840 px wide. |
| `images/tier_1.png` to `tier_6.png` | All machines of each tier, with names. |
| `images/machines_all.png` | All machines of all tiers in one image. |
| `images/workers.png` | The 9 worker robots. |
| `images/armor.png` | The 6 armour sets with their items. |
| `images/items_equipment.png` | Tools and weapons. |
| `images/items_materials.png` | Ores, ingots and processing stages. |
| `images/items_components.png` | Components, cables, frames, seeds, upgrades and colony tools. |
| `images/scene_1_*.png` to `scene_6_*.png` | One rendered 3D scene per tier, 840 x 480. |
| `description_text_only.md` / `.html` | The description without images, for the create step. |
