# Autarkia build status

Every line is checked only with evidence (test result, log line or screenshot path).
Progress: 374 / 940 checked.

## M0 - Toolchain and API spikes (0.1.0)

### Toolchain
- [x] `tool:jdk25` - F:/jdks/jdk-25 used by gradle.properties org.gradle.java.installations.paths and NFRT javaExecutable; build OK
- [x] `tool:gradle_project` - build.gradle MDG 2.0.148, Gradle 9.2.1 wrapper; tools/gradle.py build OK (build/libs/autarkia-0.1.0.jar)
- [x] `tool:git_init` - git init with .gitignore (build, run, .gradle, logs)
- [x] `tool:gradle_py` - tools/gradle.py: env, private desktop, logs to build-logs, crash check, RCON; runServer/runClient used for T0.1-T0.9
- [x] `tool:apidump` - tools/apidump.py: api/mc sources, api/decomp library decompiles
- [x] `tool:textures_py` - tools/textures.py + paint_*.py: 187 block, 186 item, 48 entity textures, fixed seeds
- [x] `tool:geo_py` - tools/geo.py + geo_*.py: 60 GeckoLib models with animations
- [x] `tool:data_py` - tools/data.py + datagen.py: blockstates/models/items/lang/sounds.json (107 events)/SoundIds.java
- [x] `tool:status_py` - tools/status.py renders STATUS.md from status_items.py and status_evidence.json
- [x] `tool:coverage_py` - tools/coverage.py --upto M2: 41 ids checked, 0 missing
- [x] `tool:packrun_py` - tools/packrun.py: 185 instance jars + autarkia jar, offline Dev, director m0.txt done in 100s, 0 autarkia WARN/ERROR
- [x] `tool:install_jar_py` - tools/install_jar.py: instance mods holds only autarkia-0.1.0.jar

### Skeleton
- [x] `code:Autarkia` - Autarkia.java @Mod loads in dev client, dev server and full pack
- [x] `code:AutarkiaClient` - AutarkiaClient.java: client config, payload handlers, renderers; dev client and pack run clean
- [x] `code:registries` - AutarkiaRegistries/AutarkiaSounds/AutarkiaTabs register in dev and pack runs
- [x] `code:config_specs` - AutarkiaConfig (server, synced) and AutarkiaClientConfig load; autarkia-server.toml written
- [x] `code:payload_registrar` - AutarkiaNetwork v3: span_list, span_state, beam_visual, project_preview received in pack run (screenshots)
- [x] `code:AnthologyBridge` - fx shake sends CameraShakeSync (pack run m0_shake, log [fx] shake)
- [x] `code:GhostBlocks` - run-pack/screenshots/m0_ghost.png shows translucent ghost blocks
- [x] `code:fx_commands` - /autarkia fx shake|beam|ghost|span run by director in pack run

### Tests
- [x] `test:T0.1` - PASS: dev client, dev server and full pack (185 mods) load; pack latest.log 0 autarkia WARN/ERROR
- [x] `test:T0.2` - PASS: run-pack/screenshots/m0_span_one_end_offscreen.png: hot span with sag visible while one end is off screen
- [x] `test:T0.3` - PASS: run-pack/screenshots/m0_mob.png: GeckoLib mob with SmartBrainLib brain walks
- [x] `test:T0.4` - PASS: spike place_big placed 8/8 TML footprint blocks (RCON on dev server)
- [x] `test:T0.5` - PASS: anthology shake sent; beam ribbon visible (run-pack/screenshots/m0_beam.png)
- [x] `test:T0.6` - PASS: ghost blocks visible (run-pack/screenshots/m0_ghost.png)
- [x] `test:T0.7` - PASS: saved data counter persisted 1 to 3 across restarts; AT LightningBolt.visualOnly writable
- [x] `test:T0.8` - PASS: JarInJarDependencyLocator found 13 dependencies
- [x] `test:T0.9` - PASS: runServer -PwithoutPlayerAnim started and stopped clean (build-logs/runServer_noPAL.log)

## M1 - T1 power and processing (0.2.0)

### Systems
- [x] `sys:grid_core` - Grid/GridLevel/GridNetwork: selftest m1 02#1, 02#2, slip rig PASS (33/33)
- [x] `sys:span_sync` - span_list on chunk watch + span_state every 10 ticks; spans visible in dev and pack screenshots m1_spans.png
- [x] `sys:catenary_renderer` - ClientSpans rope tubes with sag: run-pack/screenshots/m1_overview.png, m1_spans.png
- [x] `sys:rope_slip_wear` - selftest m1: slip caps flow at 39.0, wear rises 0.17, snap drops 1+1 rope coil
- [x] `sys:machine_recipe` - MachineRecipe crushing/alloying: selftest 03#7, 03#9, 03#10 PASS
- [x] `sys:transit_segments` - TransitSegment + transit_delta: selftest 04 L1/L2 PASS; chute items visible in m1_crates_tally.png
- [x] `sys:jei_crushing_alloying` - runClient.log: [jei] category crushing with 6 recipes, alloying with 1 recipes; no JEI errors
- [x] `sys:jade_power_machine` - Jade lines in run/client/screenshots/m1_stamp_mill.png (status, energy, queue)
- [x] `sys:guide_structure_copy` - build.gradle processResources copies data/autarkia/structure/colony/*.nbt into guides/autarkia/structures (structures arrive in M3)

### Blocks
- [x] `block:tin_ore` - registered, loot raw_tin (selftest loot PASS), worldgen 03#1 PASS
- [x] `block:deepslate_tin_ore` - registered, coverage 0 missing, worldgen targets deepslate
- [x] `block:raw_tin_block` - registered, compression recipes, coverage 0 missing
- [x] `block:tin_block` - registered, compression recipes, coverage 0 missing
- [x] `block:bronze_block` - registered, compression recipes, coverage 0 missing
- [x] `block:hand_crank` - GeckoLib model in m1_crank.png; 20 FE/t player, stops in 6 ticks (selftest)
- [x] `block:rope_pulley` - connector node, 4 spans (node full test), m1_spans.png
- [x] `block:quern` - selftest 03#7 98 crushed in 5122 ticks; model in m1_quern_crucible.png
- [x] `block:stamp_mill` - TML 1x2x1 placed by code (selftest), 03#10 PASS, GUI m1_gui_stamp_mill.png
- [x] `block:crucible` - 03#9 furnace 300 / campfire 600 / lava 200 ticks; GUI m1_gui_crucible.png
- [x] `block:crate` - 27 slots, piles in m1_crates_tally.png, loot PASS
- [x] `block:tally_board` - counts shown on face in m1_crates_tally.png
- [x] `block:chute` - 04 L1/L2 PASS, items rendered sliding
- [x] `block:foundation` - registered with recipe, coverage 0 missing

### Items
- [x] `item:raw_tin` - ore drop (loot test), coverage 0 missing
- [x] `item:tin_ingot` - smelting from raw/crushed tin (03#8)
- [x] `item:tin_nugget` - nugget recipes, coverage 0 missing
- [x] `item:bronze_ingot` - crucible alloying 4 per run (03#9)
- [x] `item:bronze_nugget` - nugget recipes, coverage 0 missing
- [x] `item:crushed_iron` - stamp mill 03#10
- [x] `item:crushed_copper` - crushing recipe, coverage 0 missing
- [x] `item:crushed_gold` - crushing recipe, coverage 0 missing
- [x] `item:crushed_tin` - quern 03#7
- [x] `item:bronze_gear` - recipe, coverage 0 missing
- [x] `item:mainspring` - recipe, coverage 0 missing
- [x] `item:rope_coil` - CableItem places spans; snap/break drops (selftest)
- [x] `item:wrench` - rotate and pick-up item, recipe, coverage 0 missing
- [x] `item:multimeter` - reading screen m1_multimeter.png (gen 19.9, demand, service, spans)

### Registries
- [x] `recipe:crushing` - AutarkiaRecipes crushing type + serializer, 6 recipes
- [x] `recipe:alloying` - AutarkiaRecipes alloying type + serializer, bronze recipe
- [x] `payload:span_list` - registered; spans appear on client (screenshots)
- [x] `payload:span_state` - registered; rope state sync
- [x] `payload:multimeter_reading` - m1_multimeter.png
- [x] `payload:transit_delta` - chute items move on the client (m1_crates_tally.png)
- [x] `saved:grid` - grid save format round trip PASS
- [x] `menu:stamp_mill` - m1_gui_stamp_mill.png
- [x] `menu:crucible` - m1_gui_crucible.png
- [x] `menu:crate` - m1_gui_crate.png
- [x] `component:link_start` - CableItem first click stores link_start (GlobalPos, face, time)
- [x] `worldgen:tin_ore_feature` - data/autarkia/worldgen/feature/tin_ore.json; 03#1 PASS
- [x] `worldgen:tin_ore_placed` - placed_feature/tin_ore.json count 12 trapezoid -16..96
- [x] `worldgen:tin_ore_modifier` - neoforge/biome_modifier/tin_ore.json underground_ores

### Tests
- [x] `test:02#1` - PASS selftest m1 (gen 19.0, loss 0.95, stops in 6 ticks)
- [x] `test:02#2` - PASS selftest m1 (2 coils drop, split, no later rebuild)
- [x] `test:03#1` - PASS selftest m1 (tin ore in 65x65 scan)
- [x] `test:03#7` - PASS selftest m1 (96-101 crushed, 8 turns per ore)
- [x] `test:03#8` - PASS selftest m1 (smelting recipe)
- [x] `test:03#9` - PASS selftest m1 (300/600/200 ticks, NO_CONDITION)
- [x] `test:03#10` - PASS selftest m1 (94-100 crushed, 163,840 FE)
- [x] `test:04#L1` - PASS selftest m1 (16 cobblestone, no item entities)
- [x] `test:04#L2` - PASS selftest m1 (blocked, stuck after 30 s, nothing drops)
- [x] `test:selftest_m1` - build-logs/runServer.log [TEST m1] RESULT 33/33; pack run m1 scene 0 autarkia warnings

## M2 - T1 physics and bronze equipment (0.3.0)

### Blocks
- [x] `block:treadwheel` - TML 3x3x1; led cow gives 12 FE/t, lead kept (selftest m2); cow walking in run-pack/screenshots/m2_treadwheel.png
- [x] `block:windmill` - base 7.2/32 by rotor y, thunder x1.3, obstruction 0.7, 5 blocks stop it (selftest 02#20); m2_windmill.png
- [x] `block:stone_flywheel` - self-loss 246 FE/100 t, I/O 40 x max(0.25, omega) (selftest); Jade spin/stored/io in run/client/screenshots/m2_flywheel.png

### Entities
- [x] `entity:rolling_flywheel` - released at 40,000 FE, hits husk 15+, wobbles and drops item (selftest 02#4); m2_rolling_flywheel.png, probe_side.png
- [x] `entity:snapped_cable` - 2 ends, 3 per hit, 1 rope coil each after 100 t (selftest 02#3); lit by brightest chain point, run-pack/screenshots/m2_snapped_rope.png
- [x] `entity:sling_shot` - cobblestone 3 / nugget 4 damage, 1 ammo, 1 durability (selftest 07#3)

### Items
- [x] `item:bronze_pickaxe` - iron and diamond ore yes, obsidian no, 4.0 / 1.2, speed 6.5 (selftest 07#1); held in m2_hud_mode.png
- [x] `item:bronze_axe` - registered with recipe and tool tags, coverage --upto M2 0 missing
- [x] `item:bronze_shovel` - registered with recipe and tool tags, coverage --upto M2 0 missing
- [x] `item:bronze_hoe` - registered with recipe and tool tags, coverage --upto M2 0 missing
- [x] `item:bronze_sword` - registered with recipe and tool tags, coverage --upto M2 0 missing
- [x] `item:bronze_helmet` - full set 13 armor (selftest 07#2); worn in m2_bronze_gear.png
- [x] `item:bronze_chest` - full set 13 armor (selftest 07#2); worn in m2_bronze_gear.png
- [x] `item:bronze_legs` - full set 13 armor (selftest 07#2); worn in m2_bronze_gear.png
- [x] `item:bronze_boots` - full set 13 armor (selftest 07#2); worn in m2_bronze_gear.png
- [x] `item:bronze_sling` - charge arc in m2_hud_sling.png; 07#3 PASS

### Systems
- [x] `sys:hud_layer` - autarkia:equipment_hud above hotbar: sling arc (m2_hud_sling.png) and mode label 5x5 (m2_hud_mode.png)
- [x] `sys:mode_key` - V key click: area3 -> area5 (data get shows tool_mode area5), no-mode item says 'This item has no modes.' (m2_mode_none.png)
- [x] `sys:frozenlib_wind` - Wind.strength from FrozenLib WindManager: selftest windmill wind 0.11 to 1.35 across runs
- [x] `sys:equipment_asset_bronze` - assets/autarkia/equipment/bronze.json; armor layers rendered in m2_bronze_gear.png

### Registries
- [x] `component:energy` - registered (AutarkiaComponents), persistent and stream codec; used by charged items from M4
- [x] `component:tool_mode` - cycled by equipment_action (data get: area5)
- [x] `payload:equipment_action` - C2S mode key action handled on the server (m2_hud_mode.png, m2_mode_none.png)
- [x] `damage:sling` - data/autarkia/damage_type/sling.json loaded (selftest registries PASS)
- [x] `adv:power/root` - run-pack latest.log: Dev has made the advancement [Power to the Colony] after cranking
- [x] `adv:power/first_span` - run-pack latest.log: Dev has made the advancement [Tied Together] after placing a rope span
- [x] `adv:power/rope_snap` - run-pack latest.log: Dev has made the advancement [Too Much Pull] at the 02#3 snap
- [x] `adv:power/rolling_wheel` - run-pack latest.log: Dev has made the advancement [Runaway Wheel] at the 02#4 release

### Wow
- [x] `wow:W3` - rolling flywheel down a hill, hits husk, wobbles flat (selftest 02#4; m2_rolling_flywheel.png)
- [x] `wow:W4` - rope snaps under overload and whips (selftest 02#3; m2_snapped_rope.png)

### Tests
- [x] `test:02#3` - PASS selftest m2 (snap after 716 t, 2 ends, 3 per hit, 2 coils)
- [x] `test:02#4` - PASS selftest m2 (1 wheel, husk 15+, wobble and 1 item)
- [x] `test:02#5` - PASS selftest m2 (1,000 FE: block drops, no wheel)
- [x] `test:02#20` - PASS selftest m2 (7.2 / 32, thunder 1.3, formula, 5 blocks stop)
- [x] `test:07#1` - PASS selftest m2
- [x] `test:07#2` - PASS selftest m2 (armor 13)
- [x] `test:07#3` - PASS selftest m2 (3 and 4 damage)
- [x] `test:selftest_m2` - runServer.log [TEST all] M1 39/39, M2 30/30 (adds treadwheel feeds a mill and live-extended network serves a new mill); dev client and full pack [TEST m2] RESULT 30/30; pack log 0 autarkia warnings; cow walk animation advances 85.2 to 91.5 in 20 ticks

## M3 - Colony Core and Porter (0.4.0)

### Blocks
- [x] `block:colony_core` - test m3 W1 grows and blooms (54/54, 0.4.0); shot m3_core, m3_w1_*
- [x] `block:project_board` - pack scene shot m3_project_board (Projects screen); coverage 0 missing
- [x] `block:worker_assembler` - test m3 06#2 assembles a Porter in 600 ticks, 06#29 CAP_FULL; shot m3_assembler_porters
- [x] `block:winding_post` - test m3 06#5/#6 Porter winds to full at the post; pack shot m3_winding_post

### Items
- [x] `item:core_seed` - test m3 W1 and W1 fail; dev scene W1 by play, advancement colony/root
- [x] `item:planning_tablet` - pack scene shots m3_tablet_screen and m3_tablet_ghost
- [x] `item:worker_capsule` - test m3 06#28 release, capture, release with 4321 T and cargo kept
- [x] `item:walker_frame` - test m3 06#2 frame used by the assembly recipe; recipe check
- [x] `item:field_manual` - W1 drop check; pack shot m3_field_manual (GuideME start page)

### Entities
- [x] `entity:porter` - test m3 06#1 to #7, #28 to #30 pass; client render fixed (GeckoLib data map), pack shots

### Systems
- [x] `sys:colony_savedata` - autarkia:colonies SavedData; colonies reload across server restarts in the m3 runs
- [x] `sys:domain` - test m3 W1 domain 33 x 33 check
- [x] `sys:ownership` - test m3 W1 fail inside domain; trusted player gets colony advancements (client 58/58)
- [x] `sys:sensors_S01_S09_S15_S20` - test m3 W2 S01 19.1 S02 31.0 S03 0.38; W2 manual S09 forced
- [x] `sys:planner` - test m3 W2 planner run proposes a windmill; W2 manual crate cluster
- [x] `sys:templates_1_6` - test m3: 6 templates and 5 structure plans load
- [x] `sys:site_search` - test m3 W2 site with 1 leveling edit and a clear rope route
- [x] `sys:cost_priority_budget` - test m3 W2 card cost 20.8, P 73.7, auto-start under the daily budget
- [x] `sys:colony_clock` - test m3 W2 clock skip 24000 approves; W2 manual skip 48000 keeps PROPOSED
- [x] `sys:ghost_preview` - test m3 W2 cyan ghost with the windmill; pack shot m3_tablet_ghost
- [x] `sys:autonomy` - test m3 W2 Assisted and W2 manual
- [x] `sys:w2_sign` - test m3 W2 sign 'Power 40% short. / Fixed. / Day N'; shot m3_windmill_sign
- [x] `sys:job_system` - test m3 W2 level, place, string and commission done in about 1100 ticks
- [x] `sys:walker_rig` - pack shots of Porters (walker rig, porter texture, glow mask)
- [x] `sys:spring_energy` - test m3 06#4 drain 797 T per 100 ticks, 06#6 run-down and +100 T
- [x] `sys:stuck_recovery` - test m3 06#7 levels 1 to 3, NO_PATH, recovery
- [x] `sys:core_screen_tabs` - pack shot m3_core_overview_tab (5 tabs, graph, log)
- [x] `sys:debug_commands` - scene uses autarkia sensor force and planner run; colony/worker/profile commands registered
- [x] `sys:guide_t1_chapter` - 8 GuideME pages (index, t1 chapter with 6 pages, 4 structure scenes); shot m3_field_manual

### Registries
- [x] `menu:colony_core` - pack shot m3_core_overview_tab
- [x] `menu:project_board` - pack shot m3_project_board
- [x] `menu:planning_tablet` - pack shot m3_tablet_screen
- [x] `menu:worker_assembler` - registered SlotMenu with gui.py layout; test m3 06#2 uses its inventory
- [x] `recipe:worker_assembly` - test m3 recipe check and 06#2 assembly
- [x] `component:colony_link` - planning tablet link in pack scene (Projects plus autonomy screen)
- [x] `payload:preview_request` - pack scene tablet ghost (client request every 40 ticks)
- [x] `payload:project_preview` - test m3 W2 preview payload cyan, 5 ghosts
- [x] `payload:colony_sync` - pack shots of the colony screens with live data
- [x] `payload:colony_action` - colony screen buttons send actions; tab switch in scene
- [x] `payload:worker_debug` - 0.5.0 pack run: `autarkia worker info` HUD in run-pack/screenshots/m4_worker_hud.png
- [x] `saved:colonies` - <world>/data/autarkia/colonies.dat written by the m3 runs
- [x] `memory:assigned_job` - 06 tests: orders assigned and released
- [x] `memory:job_phase` - 06 tests: fetch, travel, work phases
- [x] `memory:job_step` - registered; step index of the order
- [x] `memory:fetch_source` - 06#30 fetch of planks from a crate
- [x] `memory:charger_pos` - 06#5 Porter finds the winding post
- [x] `memory:needs_recharge` - 06#5 release below the reserve
- [x] `memory:stuck_level` - 06#7 stuck levels 1 to 3
- [x] `memory:debris_targets` - registered for the crab (M4 use)
- [x] `memory:guard_route` - registered for the sentinel (M7 use)
- [x] `sensor:job_board` - 06 tests: orders validated
- [x] `sensor:energy` - 06#5 needs_recharge set
- [x] `sensor:charger` - 06#5 charger found
- [x] `sensor:stuck` - 06#7
- [x] `sensor:debris` - registered (M4 use)
- [x] `sensor:guard_route` - registered (M7 use)
- [x] `structure:windmill` - test m3 plan check and W2 build
- [x] `structure:treadwheel` - loads (test m3 5 plans); guide scene
- [x] `structure:crate_cluster` - loads; W2 manual proposal and tablet ghost
- [x] `structure:flywheel` - loads; guide scene
- [x] `structure:winding_post` - loads; winding_post proposal in the m3 runs
- [x] `template:windmill` - test m3 W2 (54/54, test all 2026-10-21): proposed, built, commissioned
- [x] `template:treadwheel` - test m3 data check: loaded (6 T1 templates); colony_template/treadwheel.json
- [x] `template:crate_cluster` - test m3 W2 manual: crate cluster proposed (54/54)
- [x] `template:flywheel` - test m3 data check: loaded (6 T1 templates); colony_template/flywheel.json
- [x] `template:parallel_span` - test m3 data check: loaded; handler parallel_span in ProjectFactory
- [x] `template:winding_post` - test m3 data check: loaded (6 T1 templates); colony_template/winding_post.json
- [x] `adv:colony/root` - test m3 W1: the trusted player has colony/root (dev client 58/58)
- [x] `adv:colony/first_proposal` - test m3 advancement tree check; fired by Planner.propose (W2)
- [x] `adv:colony/it_fixed_itself` - test m3 advancement tree check; fired by W2 auto-approved project done and signed
- [x] `adv:workers/first_porter` - test m3 06#2 assembled Porter; tree check under colony/root
- [x] `adv:workers/self_built` - test m3 W2: T1 build done by Porters (job_complete:build_t1)
- [x] `trigger:colony_event` - test m3 W1/W2 advancements use autarkia:colony_event
- [x] `trigger:worker_event` - test m3 06#2 and W2 use autarkia:worker_event

### Wow
- [x] `wow:W1` - test m3 W1 PASS (test all 2026-10-21); screenshots m3_w1_*
- [x] `wow:W2` - test m3 W2 PASS (test all 2026-10-21); screenshot m3_windmill_sign

### Tests
- [x] `test:05#W1` - test m3 W1 PASS (test all 2026-10-21)
- [x] `test:05#W1fail` - test m3 W1 fail checks PASS
- [x] `test:05#W2` - test m3 W2 PASS
- [x] `test:05#W2manual` - test m3 W2 manual PASS
- [x] `test:06#1` - test m3 06#1 PASS
- [x] `test:06#2` - test m3 06#2 PASS
- [x] `test:06#3` - test m3 06#29 cap and 06#2 assembler cover the assembler rules; PASS
- [x] `test:06#4` - test m3 06#4 PASS
- [x] `test:06#5` - test m3 06#5 PASS
- [x] `test:06#6` - test m3 06#6 PASS
- [x] `test:06#7` - test m3 06#7 PASS
- [x] `test:06#28` - test m3 06#28 PASS
- [x] `test:06#29` - test m3 06#29 PASS
- [x] `test:06#30` - test m3 06#30 PASS
- [x] `test:selftest_m3` - test all 2026-10-21: [TEST m3] RESULT 54/54

## M4 - T2 power (0.5.0)

### Blocks
- [x] `block:wooden_pole` - 0.5.0: test m4 02#6/#9 rigs; screenshot m4_hot_wires
- [x] `block:waterwheel` - 0.5.0: showcase m4 72 FE/t (Jade); screenshot m4_waterwheel
- [x] `block:boiler` - 0.5.0: test m4 02#12/#13; screenshots m4_boiler_engines, m4_boiler_screen
- [x] `block:pressure_valve` - 0.5.0: test m4 02#12 vents at 9 bar
- [x] `block:steam_engine` - 0.5.0: test m4 02#13 two engines, 240 FE/t
- [x] `block:lightning_mast` - 0.5.0: test m4 02#11; screenshot m4_mast_strike
- [x] `block:lightning_arrester` - 0.5.0: test m4 02#10
- [x] `block:capacitor_bank` - 0.5.0: test m4 02#11 (3 x 40,000 FE); screenshot m4_capacitor_screen
- [x] `block:industrial_lamp` - 0.5.0: test m4 02#6 ripple (6 lamps)
- [x] `block:catwalk` - 0.5.0: showcase m4 catwalk; screenshot m4_boiler_engines
- [x] `block:catwalk_stairs` - 0.5.0: showcase m4 stairs; screenshot m4_boiler_engines
- [x] `block:hazard_block` - 0.5.0: showcase m4 hazard row; screenshot m4_overview

### Items
- [x] `item:copper_wire_spool` - 0.5.0: CableItem COPPER; recipe check in test m4
- [x] `item:core_upgrade_t2` - 0.5.0: test m4 core_upgrade_t2 checks (rights, use, 100 ticks, T2)

### Systems
- [x] `sys:heat_model` - test m4 02#6 (snap at ~170 ticks at u 2) and 02#7 (h 0.452)
- [x] `sys:snap_arcs` - test m4 02#6 two copper SnappedCableEntity ends
- [x] `sys:lamp_ripple` - test m4 02#6 ripple [2, 4, 7, 10, 13, 16] ticks
- [x] `sys:boiler_burst` - test m4 02#12 burst at 12 bar: 4 bricks, 8 gravel
- [x] `sys:lightning_pulse` - test m4 02#9/#10; screenshot m4_pulse (glow on the span)
- [x] `sys:failure_events_02` - cable_snap, lightning_surge, boiler_high, boiler_burst memories; test m4 handlers and signals

### Registries
- [x] `menu:boiler` - screenshot m4_boiler_screen (pack run 0.5.0)
- [x] `menu:energy_storage` - screenshot m4_capacitor_screen (pack run 0.5.0)
- [x] `payload:lightning_pulse` - client glow in m4_pulse screenshot (pack run 0.5.0)
- [x] `structure:waterwheel` - test m4 data check (3 T2 plans) and SR_FLOW_WATER site
- [x] `structure:boiler_engine` - test m4 data check (3 T2 plans)
- [x] `structure:lightning_mast` - test m4 data check and SR_PEAK site
- [x] `template:waterwheel` - test m4 SR_FLOW_WATER PASS
- [x] `template:boiler_engine` - test m4 template load check
- [x] `template:span_upgrade` - test m4 handler span_upgrade PASS
- [x] `template:lightning_mast` - test m4 SR_PEAK PASS
- [x] `template:lightning_arrester` - test m4 handler arrester PASS
- [x] `template:pressure_valve` - test m4 handler valve and the by-hand valve project PASS
- [x] `adv:power/red_hot` - Grid.snap awards it for a copper overload snap (test m4 02#6); loaded (registry check)
- [x] `adv:power/surge` - pack run 0.5.0: 'Dev has made the advancement [Struck]' (m4_surge)
- [x] `adv:power/storm_harvest` - pack run 0.5.0: 'Dev has reached the goal [Free Energy]' (m4_mast_strike)
- [x] `adv:power/boiler_burst` - BoilerBlockEntity.burst awards it (test m4 02#12); loaded (registry check)

### Wow
- [x] `wow:W5` - test m4 02#6 and 02#7 PASS
- [x] `wow:W7` - test m4 02#9 and 02#10 PASS; screenshot m4_surge
- [x] `wow:W8` - test m4 02#11 PASS; screenshot m4_mast_strike
- [x] `wow:W9` - test m4 02#12 and 02#13 PASS

### Tests
- [x] `test:02#6` - test m4 PASS (44/44 server and pack)
- [x] `test:02#7` - test m4 PASS
- [x] `test:02#9` - test m4 PASS
- [x] `test:02#10` - test m4 PASS
- [x] `test:02#11` - test m4 PASS
- [x] `test:02#12` - test m4 PASS
- [x] `test:02#13` - test m4 PASS
- [x] `test:selftest_m4` - test all 2026-10-21: [TEST m4] RESULT 44/44; pack 44/44

## M5 - T2 processing, belts, racks, Patterns (0.6.0)

### Blocks
- [x] `block:zinc_ore` - registered, loot raw_zinc; worldgen zinc_ore_placed; data check test m5
- [x] `block:deepslate_zinc_ore` - registered, loot raw_zinc; worldgen zinc_ore_placed
- [x] `block:raw_zinc_block` - registered, 9 raw zinc recipe both ways
- [x] `block:zinc_block` - registered, 9 zinc ingot recipe both ways
- [x] `block:brass_block` - registered, 9 brass ingot recipe both ways
- [x] `block:steel_block` - registered, 9 steel ingot recipe both ways
- [x] `block:sluice` - test m5 03#11: 96 washed iron, 11 zinc nuggets; scene m5_sluice_smelter
- [x] `block:blast_smelter` - test m5 03#12/#13: 128 iron, 10 steel; foundation check; scene m5_sluice_smelter
- [x] `block:plate_press` - test m5 03#14: 10 steel plates; scene m5_press_drawer_jade
- [x] `block:wire_drawer` - test m5 03#14: 4 copper coils, no coil without core; scene Jade
- [x] `block:belt` - test m5 L3-L6, L10; scene m5_belts
- [x] `block:belt_splitter` - test m5 L7 32/32; scene m5_splitter_screen
- [x] `block:sorting_chute` - test m5 L7 line; scene showcase sorts steel plates
- [x] `block:wood_rack` - test m5 L11/L12; scene m5_racks_bend, m5_rack_collapse
- [x] `block:pattern_loom` - test m5 05 5.1/5.2 capture; scene m5_loom_box, m5_loom_screen

### Items
- [x] `item:raw_zinc` - zinc ore loot; crushing recipe
- [x] `item:zinc_ingot` - smelting raw/crushed/washed zinc
- [x] `item:zinc_nugget` - sluice byproduct (test m5 03#11)
- [x] `item:brass_ingot` - alloying recipe copper + zinc
- [x] `item:steel_ingot` - test m5 03#13: 10 steel
- [x] `item:steel_nugget` - 9 nuggets <-> ingot recipes
- [x] `item:slag` - blast smelter byproduct (test m5 03#12: 9 slag)
- [x] `item:crushed_zinc` - crushing recipe for raw zinc
- [x] `item:washed_iron` - test m5 03#11: 96 washed iron
- [x] `item:washed_copper` - washing recipe crushed copper
- [x] `item:washed_gold` - washing recipe crushed gold
- [x] `item:washed_tin` - washing recipe crushed tin
- [x] `item:washed_zinc` - washing recipe crushed zinc
- [x] `item:steel_gear` - recipe; crab assembly part
- [x] `item:copper_coil` - test m5 03#14: 4 coils
- [x] `item:steel_plate` - test m5 03#14: 10 plates
- [x] `item:brass_plate` - pressing recipe; crab repair item
- [x] `item:pattern_card` - test m5 05 5.1: card gets autarkia:pattern_ref
- [x] `item:crab_frame` - worker_assembly hauler_crab frame; loot
- [x] `item:wire_cutter` - removes a span and returns tier.itemCost(length) cable items
- [x] `item:steel_pickaxe` - test m5 07#4: drops obsidian
- [x] `item:steel_axe` - steel tool tier, recipe
- [x] `item:steel_shovel` - steel tool tier, recipe
- [x] `item:steel_sword` - steel tool tier, recipe
- [x] `item:steel_sledgehammer` - test m5 07#5: 3 x 9 blocks, 9 durability each; scene m5_sledgehammer_mode
- [x] `item:steel_helmet` - steel equipment asset; scene m5_steel_rig_crab
- [x] `item:steel_chest` - steel equipment asset; scene m5_steel_rig_crab
- [x] `item:steel_legs` - steel equipment asset; scene m5_steel_rig_crab
- [x] `item:steel_boots` - steel equipment asset; scene m5_steel_rig_crab
- [x] `item:steam_repeater` - test m5 07#6: pump 100 in 40 ticks, 10 shots, 10 nuggets; scene m5_repeater_gauge

### Entities
- [x] `entity:hauler_crab` - test m5 06#8/#9; scene m5_steel_rig_crab
- [x] `entity:falling_crate` - test m5 L11: two crate blocks land with all contents; scene m5_rack_collapse

### Systems
- [x] `sys:load_model` - test m5 L11/L12: 127% stage 3, 115% stage 2, units per class
- [x] `sys:rack_collapse` - test m5 L11: collapse after 1,200 ticks, salvage, rack_collapse memory
- [x] `sys:crab_rig` - GeckoLib crab rig with place/scoop/lift/lower/unfold; scene m5_steel_rig_crab
- [x] `sys:collect_debris` - test m5 06#8: snapped end and spill collected, debris_collected 2
- [x] `sys:pattern_capture` - test m5 05 5.1/5.2: 11 blocks, 1 foreign BE, splitter data, 2-bit palette
- [x] `sys:pattern_library` - test m5 05 5.1/5.4: library entry, watched instance, remove/watch/role
- [x] `sys:equipment_asset_steel` - steel equipment asset loads; scene m5_steel_rig_crab armor stand

### Registries
- [x] `recipe:washing` - recipe type; test m5 03#11
- [x] `recipe:pressing` - recipe type; test m5 03#14
- [x] `recipe:drawing` - recipe type; test m5 03#14
- [x] `menu:sluice` - SluiceScreen; JEI click area
- [x] `menu:blast_smelter` - BlastSmelterScreen with heat bar
- [x] `menu:plate_press` - machine screen
- [x] `menu:wire_drawer` - machine screen
- [x] `menu:belt_splitter` - FilterScreen; scene m5_splitter_screen
- [x] `menu:sorting_chute` - FilterScreen (no mode button)
- [x] `menu:rack` - RackScreen; scene m5_rack_screen
- [x] `menu:pattern_loom` - PatternLoomScreen; scene m5_loom_screen
- [x] `component:pattern_ref` - test m5 05 5.1
- [x] `component:pressure` - test m5 07#6; scene m5_repeater_gauge
- [x] `saved:pattern` - test m5 05 5.2: Pattern file pattern_<hex> stored and loaded
- [x] `payload:rack_state` - RackStatePayload on stage change and each second in stage 3; scene shake
- [x] `worldgen:zinc_ore_feature` - configured feature loads (data check)
- [x] `worldgen:zinc_ore_placed` - placed feature loads (data check)
- [x] `worldgen:zinc_ore_modifier` - biome modifier loads (data check)
- [x] `structure:rack_row_wood` - plan loads (test m5 data check)
- [x] `template:wood_racks` - template 9 loads (test m5 data check)
- [x] `adv:materials/tin` - loaded (test m5 data check: 13 advancements)
- [x] `adv:materials/bronze` - loaded (test m5 data check)
- [x] `adv:materials/crushed` - loaded (test m5 data check)
- [x] `adv:materials/washed` - loaded (test m5 data check)
- [x] `adv:materials/steel` - loaded (test m5 data check)
- [x] `adv:colony/pattern_recorded` - loaded; awarded via pattern_recorded event (test m5 05 5.1)
- [x] `adv:logistics/root` - loaded; awarded when a chute is fed (fedBy)
- [x] `adv:logistics/belt_line` - loaded (test m5 data check)
- [x] `adv:logistics/sorted` - loaded; FilterBlockEntity sorted count
- [x] `adv:logistics/rack_collapse` - loaded; awardNear in collapse (test m5 L11)
- [x] `adv:workers/debris_crab` - loaded; debris_collected event (test m5 06#8)
- [x] `adv:equipment/bronze_age` - loaded (test m5 data check)
- [x] `adv:equipment/full_pressure` - loaded; repeater at 100 pressure (test m5 07#6)
- [x] `trigger:equipment_event` - repeater_empty and full_pressure events (test m5 07#6)

### Wow
- [x] `wow:W6` - test m5 06#8 and scene m5_steel_rig_crab: the crab collects debris

### Tests
- [x] `test:03#11` - test m5: 96 washed iron, 11 nuggets, NO_CONDITION without water, 201 ticks source-only
- [x] `test:03#12` - test m5: 128 iron, 9 slag, 0 early outputs
- [x] `test:03#13` - test m5: 10 steel, 3 slag
- [x] `test:03#14` - test m5: 10 plates, 4 coils, 0 coils without core
- [x] `test:04#L3` - test m5: 39.5 blocks in 395 ticks, 64 ingots arrive
- [x] `test:04#L4` - test m5: 15 items stop without power, resume after 395 ticks
- [x] `test:04#L5` - test m5: half-block spacing, JAMMED after 30 s, sensor 1.0
- [x] `test:04#L6` - test m5: 2 dropped, 18 of 18 remain
- [x] `test:04#L7` - test m5: 32 front, 32 side; side blocked: 16 to front
- [x] `test:04#L10` - test m5: belt and splitter entries survive save and load
- [x] `test:04#L11` - test m5: 127% stage 3, collapse after 1,200 ticks, 2 crates land with all items
- [x] `test:04#L12` - test m5: 115% stage 2, 6,000 ticks no collapse
- [x] `test:06#8` - test m5: crab collects snapped end and spill, debris_collected 2
- [x] `test:06#9` - test m5: winds at post to 12,000 T, walled in NO_PATH after 44 ticks, recovers
- [x] `test:07#5` - test m5: [9, 9, 9] broken, [9, 9, 9] used
- [x] `test:07#6` - test m5: pumped 100, 10 shots, pressure 0, 10 durability
- [x] `test:selftest_m5` - test m5 RESULT 62/62 (dev server test all 5/5, dev client scene, full pack 0 Autarkia WARN/ERROR)

## M6 - T3 Electric Works (0.7.0)

### Blocks
- [ ] `block:steel_pylon`
- [ ] `block:wall_insulator`
- [ ] `block:transformer`
- [ ] `block:fuse_box`
- [ ] `block:steam_turbine`
- [ ] `block:battery_bank`
- [ ] `block:jaw_crusher`
- [ ] `block:magnetic_separator`
- [ ] `block:electrolyzer`
- [ ] `block:fabricator`
- [ ] `block:steel_rack`
- [ ] `block:warehouse_controller`
- [ ] `block:ledger_terminal`
- [ ] `block:capsule_tube`
- [ ] `block:capsule_station`
- [ ] `block:recharge_dock`
- [ ] `block:domain_beacon`
- [ ] `block:prop_beam`

### Items
- [ ] `item:insulated_cable_spool`
- [ ] `item:pure_iron`
- [ ] `item:pure_copper`
- [ ] `item:pure_gold`
- [ ] `item:pure_tin`
- [ ] `item:pure_zinc`
- [ ] `item:basic_circuit`
- [ ] `item:servo`
- [ ] `item:drone_frame`
- [ ] `item:worker_upgrade_speed`
- [ ] `item:worker_upgrade_cargo`
- [ ] `item:worker_upgrade_armor`
- [ ] `item:worker_upgrade_range`
- [ ] `item:core_upgrade_t3`
- [ ] `item:survey_lens`
- [ ] `item:electric_drill`
- [ ] `item:electric_chainsaw`
- [ ] `item:shock_baton`
- [ ] `item:exo_rig_helmet`
- [ ] `item:exo_rig_chest`
- [ ] `item:exo_rig_legs`
- [ ] `item:exo_rig_boots`
- [ ] `item:arc_rifle`

### Entities
- [ ] `entity:builder_drone`
- [ ] `entity:loader`
- [ ] `entity:lineman`

### Systems
- [ ] `sys:failure_memory`
- [ ] `sys:pattern_repair`
- [ ] `sys:duplicate_line`
- [ ] `sys:drone_rig`
- [ ] `sys:fe_battery`
- [ ] `sys:worker_upgrades`
- [ ] `sys:warehouse_requests`
- [ ] `sys:capsule_routing`
- [x] `sys:area_mining` - test m5 07#5 (AreaMining plane break, implemented in M5)

### Registries
- [ ] `recipe:separating`
- [ ] `recipe:electrolyzing`
- [ ] `recipe:fabricating`
- [ ] `menu:steam_turbine`
- [ ] `menu:fuse_box`
- [ ] `menu:jaw_crusher`
- [ ] `menu:magnetic_separator`
- [ ] `menu:electrolyzer`
- [ ] `menu:fabricator`
- [ ] `menu:capsule_station`
- [ ] `menu:warehouse_controller`
- [ ] `menu:ledger_terminal`
- [ ] `payload:stock_view_request`
- [ ] `payload:stock_view`
- [ ] `payload:ledger_request`
- [ ] `payload:ledger_status`
- [ ] `damage:shock`
- [ ] `damage:arc`
- [ ] `structure:recharge_dock`
- [ ] `structure:turbine_hall`
- [ ] `structure:battery_row`
- [ ] `template:fuse_box`
- [ ] `template:rack_upgrade`
- [ ] `template:duplicate_line`
- [ ] `template:pattern_repair`
- [ ] `template:recharge_dock`
- [ ] `template:steam_turbine`
- [ ] `template:battery_bank`
- [ ] `adv:materials/pure`
- [ ] `adv:colony/self_repair`
- [ ] `adv:colony/second_line`
- [ ] `adv:colony/learned`
- [ ] `adv:logistics/pneumatic`
- [ ] `adv:logistics/fetch`
- [ ] `adv:workers/drone_repair`
- [ ] `adv:workers/fuse_unordered`
- [ ] `adv:workers/loader_delivery`
- [ ] `adv:equipment/live_wire`
- [ ] `adv:equipment/clean_cut`

### Wow
- [ ] `wow:W10`
- [ ] `wow:W11`
- [ ] `wow:W12`
- [ ] `wow:W13`
- [ ] `wow:W14`

### Tests
- [ ] `test:02#8`
- [ ] `test:02#14`
- [ ] `test:02#15`
- [ ] `test:03#15`
- [ ] `test:03#16`
- [ ] `test:03#17`
- [ ] `test:04#L8`
- [ ] `test:04#L9`
- [ ] `test:04#L13`
- [ ] `test:04#W12`
- [ ] `test:04#W14`
- [ ] `test:05#W10`
- [ ] `test:05#W10accept`
- [ ] `test:05#W11`
- [ ] `test:05#W13`
- [ ] `test:05#W13decay`
- [ ] `test:06#10`
- [ ] `test:06#11`
- [ ] `test:06#12`
- [ ] `test:06#13`
- [ ] `test:06#14`
- [ ] `test:06#15`
- [ ] `test:06#16`
- [ ] `test:06#17`
- [ ] `test:07#7`
- [ ] `test:07#8`
- [ ] `test:07#9`
- [ ] `test:07#10`
- [ ] `test:07#11`
- [ ] `test:selftest_m6`

## M7 - T4 Frontier (0.8.0)

### Blocks
- [ ] `block:vantite_ore`
- [ ] `block:raw_vantite_block`
- [ ] `block:vantite_block`
- [ ] `block:vantite_steel_block`
- [ ] `block:ore_outcrop`
- [ ] `block:magma_vent`
- [ ] `block:hv_pylon`
- [ ] `block:hydro_turbine`
- [ ] `block:wind_tower`
- [ ] `block:magma_tap`
- [ ] `block:froth_flotation_plant`
- [ ] `block:cable_car_station`
- [ ] `block:crate_launcher`
- [ ] `block:catch_net`
- [ ] `block:depot`
- [ ] `block:outpost_core`

### Items
- [ ] `item:raw_vantite`
- [ ] `item:vantite_ingot`
- [ ] `item:vantite_steel_ingot`
- [ ] `item:crushed_vantite`
- [ ] `item:washed_vantite`
- [ ] `item:pure_vantite`
- [ ] `item:iron_concentrate`
- [ ] `item:copper_concentrate`
- [ ] `item:gold_concentrate`
- [ ] `item:tin_concentrate`
- [ ] `item:zinc_concentrate`
- [ ] `item:vantite_concentrate`
- [ ] `item:advanced_circuit`
- [ ] `item:hv_cable_spool`
- [ ] `item:outpost_seed`
- [ ] `item:core_upgrade_t4`
- [ ] `item:vantite_excavator`
- [ ] `item:grapple_launcher`
- [ ] `item:vantite_blade`
- [ ] `item:frontier_suit_helmet`
- [ ] `item:frontier_suit_chest`
- [ ] `item:frontier_suit_legs`
- [ ] `item:frontier_suit_boots`
- [ ] `item:rail_rifle`

### Entities
- [ ] `entity:surveyor`
- [ ] `entity:cable_layer_drone`
- [ ] `entity:sentinel`
- [ ] `entity:gondola`
- [ ] `entity:grapple_hook`

### Systems
- [ ] `sys:outcrop_feature`
- [ ] `sys:hv_sway`
- [ ] `sys:outposts`
- [ ] `sys:catch_up`
- [ ] `sys:linked_stock_view`
- [ ] `sys:travel_tickets`

### Registries
- [ ] `recipe:flotation`
- [ ] `menu:froth_flotation_plant`
- [ ] `menu:cable_car_station`
- [ ] `menu:crate_launcher`
- [ ] `menu:depot`
- [ ] `menu:outpost_core`
- [ ] `saved:domain_stock`
- [ ] `ticket:outpost`
- [ ] `ticket:worker_travel`
- [ ] `damage:rail`
- [ ] `worldgen:outcrop_ore`
- [ ] `worldgen:tin_deposit`
- [ ] `worldgen:zinc_deposit`
- [ ] `worldgen:vantite_ore`
- [ ] `worldgen:vantite_deposit`
- [ ] `worldgen:iron_deposit`
- [ ] `worldgen:copper_deposit`
- [ ] `worldgen:gold_deposit`
- [ ] `worldgen:magma_vent`
- [ ] `worldgen:magma_vent_nether`
- [ ] `worldgen:vanilla_deposits_modifier`
- [ ] `worldgen:vantite_ore_modifier`
- [ ] `worldgen:magma_vent_modifier`
- [ ] `worldgen:magma_vent_nether_modifier`
- [ ] `structure:outpost_basic`
- [ ] `structure:sentinel_post`
- [ ] `structure:wind_tower`
- [ ] `structure:hydro`
- [ ] `template:outpost`
- [ ] `template:hv_link`
- [ ] `template:cable_car`
- [ ] `template:sentinel_post`
- [ ] `template:wind_tower`
- [ ] `template:hydro_turbine`
- [ ] `adv:materials/concentrate`
- [ ] `adv:materials/vantite`
- [ ] `adv:colony/frontier`
- [ ] `adv:logistics/sky_freight`
- [ ] `adv:logistics/special_delivery`
- [ ] `adv:workers/pylon_line`
- [ ] `adv:workers/sentinel_kill`
- [ ] `adv:equipment/swing_line`

### Wow
- [ ] `wow:W15`
- [ ] `wow:W16`
- [ ] `wow:W17`

### Tests
- [ ] `test:03#2`
- [ ] `test:03#3`
- [ ] `test:03#18`
- [ ] `test:03#19`
- [ ] `test:04#L14`
- [ ] `test:04#W16`
- [ ] `test:04#W17`
- [ ] `test:05#W15`
- [ ] `test:05#W15catchup`
- [ ] `test:06#18`
- [ ] `test:06#19`
- [ ] `test:06#20`
- [ ] `test:06#21`
- [ ] `test:06#22`
- [ ] `test:06#23`
- [ ] `test:07#4`
- [ ] `test:07#12`
- [ ] `test:07#13`
- [ ] `test:07#14`
- [ ] `test:07#15`
- [ ] `test:07#16`
- [ ] `test:selftest_m7`

## M8 - T5 Matter (0.9.0)

### Blocks
- [ ] `block:lattice_cluster`
- [ ] `block:lattice_geode_shell`
- [ ] `block:lattice_alloy_block`
- [ ] `block:lattice_block`
- [ ] `block:plasma_generator`
- [ ] `block:cryo_node`
- [ ] `block:superconducting_coil`
- [ ] `block:plasma_separator`
- [ ] `block:nanite_hive`
- [ ] `block:claim_marker`
- [ ] `block:matter_buffer`
- [ ] `block:mag_rail`
- [ ] `block:mag_rail_station`
- [ ] `block:lattice_rack`
- [ ] `block:replicator_foundry`

### Items
- [ ] `item:lattice_shard`
- [ ] `item:lattice_crystal`
- [ ] `item:lattice_alloy_ingot`
- [ ] `item:nanite_paste`
- [ ] `item:ai_core`
- [ ] `item:lattice_fuel_cell`
- [ ] `item:coolant_cell`
- [ ] `item:superconductor_spool`
- [ ] `item:core_upgrade_t5`
- [ ] `item:nanite_multitool`
- [ ] `item:lattice_blade`
- [ ] `item:lattice_carapace_helmet`
- [ ] `item:lattice_carapace_chest`
- [ ] `item:lattice_carapace_legs`
- [ ] `item:lattice_carapace_boots`
- [ ] `item:disassembler_beam`

### Entities
- [ ] `entity:replicator_walker`
- [ ] `entity:nanite_swarm`

### Systems
- [ ] `sys:quench`
- [ ] `sys:matter_values`
- [ ] `sys:nanite_claims`
- [ ] `sys:replication`
- [ ] `sys:mag_rail_sleds`

### Registries
- [ ] `recipe:plasma_separating`
- [ ] `menu:plasma_generator`
- [ ] `menu:cryo_node`
- [ ] `menu:superconducting_coil`
- [ ] `menu:plasma_separator`
- [ ] `menu:mag_rail_station`
- [ ] `menu:matter_buffer`
- [ ] `menu:nanite_hive`
- [ ] `menu:replicator_foundry`
- [ ] `payload:nanite_bubble`
- [ ] `component:matter_link`
- [ ] `component:matter_cache`
- [ ] `component:suit_enabled`
- [ ] `worldgen:lattice_geode`
- [ ] `worldgen:lattice_geode_modifier`
- [ ] `structure:matter_buffer`
- [ ] `template:replicator_walker`
- [ ] `template:matter_buffer`
- [ ] `damage:beam`
- [ ] `adv:materials/lattice`
- [ ] `adv:materials/plasma`
- [ ] `adv:colony/clean_pit`
- [ ] `adv:colony/not_built_by_you`
- [ ] `adv:logistics/float_on`
- [ ] `adv:logistics/matter_of_fact`
- [ ] `adv:workers/replicator_deploy`
- [ ] `adv:workers/claim_done`
- [ ] `adv:power/quench`
- [ ] `adv:equipment/ghost_matter`

### Wow
- [ ] `wow:W18`
- [ ] `wow:W19`
- [ ] `wow:W20`

### Tests
- [ ] `test:02#16`
- [ ] `test:02#17`
- [ ] `test:03#4`
- [ ] `test:03#5`
- [ ] `test:03#20`
- [ ] `test:03#21`
- [ ] `test:04#L15`
- [ ] `test:05#W18`
- [ ] `test:05#W18kill`
- [ ] `test:05#W19`
- [ ] `test:06#24`
- [ ] `test:06#25`
- [ ] `test:06#26`
- [ ] `test:06#27`
- [ ] `test:07#17`
- [ ] `test:07#18`
- [ ] `test:07#19`
- [ ] `test:07#20`
- [ ] `test:selftest_m8`

## M9 - T6 Ascendant (0.10.0)

### Blocks
- [ ] `block:singularity_core`
- [ ] `block:gyro_heart`
- [ ] `block:phase_node`
- [ ] `block:matter_compiler`
- [ ] `block:domain_archive`
- [ ] `block:matter_conduit`
- [ ] `block:seed_matrix`
- [ ] `block:restoration_projector`
- [ ] `block:forge_of_ascension`
- [ ] `block:bastion_block`
- [ ] `block:bastion_glass`

### Items
- [ ] `item:aegis_plate`
- [ ] `item:autarky_seed`
- [ ] `item:core_upgrade_t6`
- [ ] `item:aegis_multitool`
- [ ] `item:aegis_blade`
- [ ] `item:aegis_helmet`
- [ ] `item:aegis_chest`
- [ ] `item:aegis_legs`
- [ ] `item:aegis_boots`
- [ ] `item:singularity_lance`

### Entities
- [ ] `entity:rising_block`
- [ ] `entity:thrown_autarky_seed`

### Systems
- [ ] `sys:explosion_absorb`
- [ ] `sys:seed_unpack`
- [ ] `sys:restoration_field`
- [ ] `sys:aegis_rule`
- [ ] `sys:nanite_reform`
- [ ] `sys:bastion`
- [ ] `sys:forge_fitting`
- [ ] `sys:player_pose`

### Registries
- [ ] `recipe:compiling`
- [ ] `recipe:ascension_fitting`
- [ ] `menu:singularity_core`
- [ ] `menu:matter_compiler`
- [ ] `menu:domain_archive`
- [ ] `menu:matter_conduit`
- [ ] `menu:seed_matrix`
- [ ] `menu:restoration_projector`
- [ ] `menu:forge_of_ascension`
- [ ] `payload:domain_flash`
- [ ] `payload:seed_unpack`
- [ ] `payload:aegis_status`
- [ ] `payload:ascension_pose`
- [ ] `payload:beam_visual`
- [ ] `payload:reform_fx`
- [ ] `component:enchant_mode`
- [ ] `component:seed_contents`
- [ ] `attachment:reform_ready_at`
- [ ] `saved:snapshot`
- [ ] `structure:restoration_projector`
- [ ] `template:restoration_projector`
- [ ] `adv:materials/compiler`
- [ ] `adv:materials/aegis`
- [ ] `adv:colony/instant_base`
- [ ] `adv:colony/undo_crater`
- [ ] `adv:colony/ascendant`
- [ ] `adv:logistics/everywhere`
- [ ] `adv:power/phase_link`
- [ ] `adv:power/drink_the_blast`
- [ ] `adv:workers/full_cap`
- [ ] `adv:equipment/ascended`
- [ ] `adv:equipment/paid_in_full`
- [ ] `adv:equipment/not_today`
- [ ] `adv:equipment/unbroken`

### Wow
- [ ] `wow:W21`
- [ ] `wow:W22`
- [ ] `wow:W23`
- [ ] `wow:W24`
- [ ] `wow:W25`

### Tests
- [ ] `test:02#18`
- [ ] `test:02#19`
- [ ] `test:03#22`
- [ ] `test:03#23`
- [ ] `test:04#L16`
- [ ] `test:05#W21`
- [ ] `test:05#W21fail`
- [ ] `test:05#W25`
- [ ] `test:05#W25accept`
- [ ] `test:07#21`
- [ ] `test:07#22`
- [ ] `test:07#23`
- [ ] `test:07#24`
- [ ] `test:07#25`
- [ ] `test:07#26`
- [ ] `test:07#27`
- [ ] `test:07#28`
- [ ] `test:07#29`
- [ ] `test:07#30`
- [ ] `test:07#31`
- [ ] `test:07#32`
- [ ] `test:07#33`
- [ ] `test:07#34`
- [ ] `test:07#35`
- [ ] `test:07#36`
- [ ] `test:07#37`
- [ ] `test:selftest_m9`

## M10 - Polish and performance (0.11.0)

### Integration
- [ ] `int:jei_all_categories`
- [ ] `int:jei_info_pages`
- [ ] `int:jade_all_providers`
- [ ] `int:yacl_screen`
- [ ] `int:guide_all_chapters`
- [ ] `int:guide_3d_scenes`

### Data
- [ ] `data:advancements_68`
- [ ] `data:sounds_107_checked`
- [ ] `data:tags_all`
- [ ] `data:damage_types_5`
- [ ] `data:equipment_assets_6`
- [ ] `data:config_files_2`
- [ ] `data:creative_tabs_6`
- [ ] `data:key_mappings_3`

### Checks
- [ ] `check:02#21_grid_budget`
- [ ] `check:06#31_worker_budget`
- [ ] `check:04_section5_targets`
- [ ] `check:profile_t6_base`
- [ ] `check:multiplayer_span_sync`
- [ ] `check:multiplayer_transit_sync`
- [ ] `check:multiplayer_worker_anims`
- [ ] `check:multiplayer_ascension_pose`
- [ ] `check:soak_1h_clean`
- [ ] `check:coverage_zero_missing`
- [ ] `check:final_selftests_m1_m9`
- [ ] `check:marker_search_clean`
- [ ] `check:single_jar_in_instance`
- [ ] `check:fullpack_clean_log`
- [ ] `check:processes_stopped`

### Sounds (107)
- [ ] `sound:crank.turn`
- [ ] `sound:rope.creak`
- [ ] `sound:rope.squeal`
- [ ] `sound:rope.snap`
- [ ] `sound:wire.hum`
- [ ] `sound:wire.sizzle`
- [ ] `sound:wire.snap`
- [ ] `sound:arc.zap`
- [ ] `sound:lightning.pulse`
- [ ] `sound:surge.blow`
- [ ] `sound:boiler.gauge_tick`
- [ ] `sound:valve.scream`
- [ ] `sound:boiler.burst`
- [ ] `sound:steam.hiss`
- [ ] `sound:engine.chuff`
- [ ] `sound:turbine.whine`
- [ ] `sound:flywheel.spin`
- [ ] `sound:flywheel.roll`
- [ ] `sound:flywheel.impact`
- [ ] `sound:capacitor.charge`
- [ ] `sound:mast.strike_charge`
- [ ] `sound:coolant.frost`
- [ ] `sound:quench.blast`
- [ ] `sound:fuse.trip`
- [ ] `sound:plasma.hum`
- [ ] `sound:singularity.drink`
- [ ] `sound:gyro.spin`
- [ ] `sound:magma.bubble`
- [ ] `sound:waterwheel.splash`
- [ ] `sound:windmill.creak`
- [ ] `sound:chute_slide`
- [ ] `sound:belt_run`
- [ ] `sound:belt_jam`
- [ ] `sound:splitter_flip`
- [ ] `sound:capsule_launch`
- [ ] `sound:capsule_arrive`
- [ ] `sound:tube_hiss`
- [ ] `sound:cable_car_run`
- [ ] `sound:gondola_strike`
- [ ] `sound:launcher_fire`
- [ ] `sound:net_catch`
- [ ] `sound:crate_land`
- [ ] `sound:rail_hum`
- [ ] `sound:rack_creak`
- [ ] `sound:rack_collapse`
- [ ] `sound:loader_lift`
- [ ] `sound:ledger_request`
- [ ] `sound:matter_dissolve`
- [ ] `sound:conduit_send`
- [ ] `sound:archive_deliver`
- [ ] `sound:core_grow_roots`
- [ ] `sound:core_bloom`
- [ ] `sound:core_upgrade`
- [ ] `sound:proposal_ping`
- [ ] `sound:project_complete`
- [ ] `sound:pattern_scan`
- [ ] `sound:outpost_seed_plant`
- [ ] `sound:replicator_print`
- [ ] `sound:replicator_unfold`
- [ ] `sound:nanite_swarm_loop`
- [ ] `sound:nanite_bubble`
- [ ] `sound:seed_throw`
- [ ] `sound:seed_unpack_wave`
- [ ] `sound:rising_block`
- [ ] `sound:restoration_hum`
- [ ] `sound:restoration_return`
- [ ] `sound:entity.worker.step_walker`
- [ ] `sound:entity.worker.step_crab`
- [ ] `sound:entity.worker.rotor_loop`
- [ ] `sound:entity.worker.wind`
- [ ] `sound:entity.worker.crank`
- [ ] `sound:entity.worker.servo`
- [ ] `sound:entity.worker.weld`
- [ ] `sound:entity.worker.fork_lift`
- [ ] `sound:entity.worker.scan`
- [ ] `sound:entity.worker.recharge`
- [ ] `sound:entity.worker.recharge_done`
- [ ] `sound:entity.worker.rundown`
- [ ] `sound:entity.worker.hurt`
- [ ] `sound:entity.worker.death`
- [ ] `sound:entity.sentinel.arc_shot`
- [ ] `sound:entity.replicator.step`
- [ ] `sound:entity.replicator.deploy`
- [ ] `sound:entity.nanite_swarm.loop`
- [ ] `sound:entity.nanite_swarm.consume`
- [ ] `sound:block.worker_assembler.work`
- [ ] `sound:block.worker_assembler.done`
- [ ] `sound:item.sling.throw`
- [ ] `sound:item.steam_repeater.pump`
- [ ] `sound:item.steam_repeater.fire`
- [ ] `sound:item.steam_repeater.hiss`
- [ ] `sound:item.electric_drill.run`
- [ ] `sound:item.electric_chainsaw.run`
- [ ] `sound:item.shock_baton.hit`
- [ ] `sound:item.arc_rifle.fire`
- [ ] `sound:item.grapple.fire`
- [ ] `sound:item.grapple.attach`
- [ ] `sound:item.rail_rifle.charge`
- [ ] `sound:item.rail_rifle.fire`
- [ ] `sound:item.beam.loop`
- [ ] `sound:item.lance.charge`
- [ ] `sound:item.lance.fire`
- [ ] `sound:armor.jet.loop`
- [ ] `sound:aegis.absorb`
- [ ] `sound:aegis.reform`
- [ ] `sound:forge.fit`
- [ ] `sound:forge.seal`

