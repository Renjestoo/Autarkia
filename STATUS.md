# Autarkia build status

Every line is checked only with evidence (test result, log line or screenshot path).
Progress: 28 / 940 checked.

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
- [ ] `tool:coverage_py`
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
- [ ] `sys:grid_core`
- [ ] `sys:span_sync`
- [ ] `sys:catenary_renderer`
- [ ] `sys:rope_slip_wear`
- [ ] `sys:machine_recipe`
- [ ] `sys:transit_segments`
- [ ] `sys:jei_crushing_alloying`
- [ ] `sys:jade_power_machine`
- [ ] `sys:guide_structure_copy`

### Blocks
- [ ] `block:tin_ore`
- [ ] `block:deepslate_tin_ore`
- [ ] `block:raw_tin_block`
- [ ] `block:tin_block`
- [ ] `block:bronze_block`
- [ ] `block:hand_crank`
- [ ] `block:rope_pulley`
- [ ] `block:quern`
- [ ] `block:stamp_mill`
- [ ] `block:crucible`
- [ ] `block:crate`
- [ ] `block:tally_board`
- [ ] `block:chute`
- [ ] `block:foundation`

### Items
- [ ] `item:raw_tin`
- [ ] `item:tin_ingot`
- [ ] `item:tin_nugget`
- [ ] `item:bronze_ingot`
- [ ] `item:bronze_nugget`
- [ ] `item:crushed_iron`
- [ ] `item:crushed_copper`
- [ ] `item:crushed_gold`
- [ ] `item:crushed_tin`
- [ ] `item:bronze_gear`
- [ ] `item:mainspring`
- [ ] `item:rope_coil`
- [ ] `item:wrench`
- [ ] `item:multimeter`

### Registries
- [ ] `recipe:crushing`
- [ ] `recipe:alloying`
- [ ] `payload:span_list`
- [ ] `payload:span_state`
- [ ] `payload:multimeter_reading`
- [ ] `payload:transit_delta`
- [ ] `saved:grid`
- [ ] `menu:stamp_mill`
- [ ] `menu:crucible`
- [ ] `menu:crate`
- [ ] `component:link_start`
- [ ] `worldgen:tin_ore_feature`
- [ ] `worldgen:tin_ore_placed`
- [ ] `worldgen:tin_ore_modifier`

### Tests
- [ ] `test:02#1`
- [ ] `test:02#2`
- [ ] `test:03#1`
- [ ] `test:03#7`
- [ ] `test:03#8`
- [ ] `test:03#9`
- [ ] `test:03#10`
- [ ] `test:04#L1`
- [ ] `test:04#L2`
- [ ] `test:selftest_m1`

## M2 - T1 physics and bronze equipment (0.3.0)

### Blocks
- [ ] `block:treadwheel`
- [ ] `block:windmill`
- [ ] `block:stone_flywheel`

### Entities
- [ ] `entity:rolling_flywheel`
- [ ] `entity:snapped_cable`
- [ ] `entity:sling_shot`

### Items
- [ ] `item:bronze_pickaxe`
- [ ] `item:bronze_axe`
- [ ] `item:bronze_shovel`
- [ ] `item:bronze_hoe`
- [ ] `item:bronze_sword`
- [ ] `item:bronze_helmet`
- [ ] `item:bronze_chest`
- [ ] `item:bronze_legs`
- [ ] `item:bronze_boots`
- [ ] `item:bronze_sling`

### Systems
- [ ] `sys:hud_layer`
- [ ] `sys:mode_key`
- [ ] `sys:frozenlib_wind`
- [ ] `sys:equipment_asset_bronze`

### Registries
- [ ] `component:energy`
- [ ] `component:tool_mode`
- [ ] `payload:equipment_action`
- [ ] `damage:sling`
- [ ] `adv:power/root`
- [ ] `adv:power/first_span`
- [ ] `adv:power/rope_snap`
- [ ] `adv:power/rolling_wheel`

### Wow
- [ ] `wow:W3`
- [ ] `wow:W4`

### Tests
- [ ] `test:02#3`
- [ ] `test:02#4`
- [ ] `test:02#5`
- [ ] `test:02#20`
- [ ] `test:07#1`
- [ ] `test:07#2`
- [ ] `test:07#3`
- [ ] `test:selftest_m2`

## M3 - Colony Core and Porter (0.4.0)

### Blocks
- [ ] `block:colony_core`
- [ ] `block:project_board`
- [ ] `block:worker_assembler`
- [ ] `block:winding_post`

### Items
- [ ] `item:core_seed`
- [ ] `item:planning_tablet`
- [ ] `item:worker_capsule`
- [ ] `item:walker_frame`
- [ ] `item:field_manual`

### Entities
- [ ] `entity:porter`

### Systems
- [ ] `sys:colony_savedata`
- [ ] `sys:domain`
- [ ] `sys:ownership`
- [ ] `sys:sensors_S01_S09_S15_S20`
- [ ] `sys:planner`
- [ ] `sys:templates_1_6`
- [ ] `sys:site_search`
- [ ] `sys:cost_priority_budget`
- [ ] `sys:colony_clock`
- [ ] `sys:ghost_preview`
- [ ] `sys:autonomy`
- [ ] `sys:w2_sign`
- [ ] `sys:job_system`
- [ ] `sys:walker_rig`
- [ ] `sys:spring_energy`
- [ ] `sys:stuck_recovery`
- [ ] `sys:core_screen_tabs`
- [ ] `sys:debug_commands`
- [ ] `sys:guide_t1_chapter`

### Registries
- [ ] `menu:colony_core`
- [ ] `menu:project_board`
- [ ] `menu:planning_tablet`
- [ ] `menu:worker_assembler`
- [ ] `recipe:worker_assembly`
- [ ] `component:colony_link`
- [ ] `payload:preview_request`
- [ ] `payload:project_preview`
- [ ] `payload:colony_sync`
- [ ] `payload:colony_action`
- [ ] `payload:worker_debug`
- [ ] `saved:colonies`
- [ ] `memory:assigned_job`
- [ ] `memory:job_phase`
- [ ] `memory:job_step`
- [ ] `memory:fetch_source`
- [ ] `memory:charger_pos`
- [ ] `memory:needs_recharge`
- [ ] `memory:stuck_level`
- [ ] `memory:debris_targets`
- [ ] `memory:guard_route`
- [ ] `sensor:job_board`
- [ ] `sensor:energy`
- [ ] `sensor:charger`
- [ ] `sensor:stuck`
- [ ] `sensor:debris`
- [ ] `sensor:guard_route`
- [ ] `structure:windmill`
- [ ] `structure:treadwheel`
- [ ] `structure:crate_cluster`
- [ ] `structure:flywheel`
- [ ] `structure:winding_post`
- [ ] `template:windmill`
- [ ] `template:treadwheel`
- [ ] `template:crate_cluster`
- [ ] `template:flywheel`
- [ ] `template:parallel_span`
- [ ] `template:winding_post`
- [ ] `adv:colony/root`
- [ ] `adv:colony/first_proposal`
- [ ] `adv:colony/it_fixed_itself`
- [ ] `adv:workers/first_porter`
- [ ] `adv:workers/self_built`
- [ ] `trigger:colony_event`
- [ ] `trigger:worker_event`

### Wow
- [ ] `wow:W1`
- [ ] `wow:W2`

### Tests
- [ ] `test:05#W1`
- [ ] `test:05#W1fail`
- [ ] `test:05#W2`
- [ ] `test:05#W2manual`
- [ ] `test:06#1`
- [ ] `test:06#2`
- [ ] `test:06#3`
- [ ] `test:06#4`
- [ ] `test:06#5`
- [ ] `test:06#6`
- [ ] `test:06#7`
- [ ] `test:06#28`
- [ ] `test:06#29`
- [ ] `test:06#30`
- [ ] `test:selftest_m3`

## M4 - T2 power (0.5.0)

### Blocks
- [ ] `block:wooden_pole`
- [ ] `block:waterwheel`
- [ ] `block:boiler`
- [ ] `block:pressure_valve`
- [ ] `block:steam_engine`
- [ ] `block:lightning_mast`
- [ ] `block:lightning_arrester`
- [ ] `block:capacitor_bank`
- [ ] `block:industrial_lamp`
- [ ] `block:catwalk`
- [ ] `block:catwalk_stairs`
- [ ] `block:hazard_block`

### Items
- [ ] `item:copper_wire_spool`
- [ ] `item:core_upgrade_t2`

### Systems
- [ ] `sys:heat_model`
- [ ] `sys:snap_arcs`
- [ ] `sys:lamp_ripple`
- [ ] `sys:boiler_burst`
- [ ] `sys:lightning_pulse`
- [ ] `sys:failure_events_02`

### Registries
- [ ] `menu:boiler`
- [ ] `menu:energy_storage`
- [ ] `payload:lightning_pulse`
- [ ] `structure:waterwheel`
- [ ] `structure:boiler_engine`
- [ ] `structure:lightning_mast`
- [ ] `template:waterwheel`
- [ ] `template:boiler_engine`
- [ ] `template:span_upgrade`
- [ ] `template:lightning_mast`
- [ ] `template:lightning_arrester`
- [ ] `template:pressure_valve`
- [ ] `adv:power/red_hot`
- [ ] `adv:power/surge`
- [ ] `adv:power/storm_harvest`
- [ ] `adv:power/boiler_burst`

### Wow
- [ ] `wow:W5`
- [ ] `wow:W7`
- [ ] `wow:W8`
- [ ] `wow:W9`

### Tests
- [ ] `test:02#6`
- [ ] `test:02#7`
- [ ] `test:02#9`
- [ ] `test:02#10`
- [ ] `test:02#11`
- [ ] `test:02#12`
- [ ] `test:02#13`
- [ ] `test:selftest_m4`

## M5 - T2 processing, belts, racks, Patterns (0.6.0)

### Blocks
- [ ] `block:zinc_ore`
- [ ] `block:deepslate_zinc_ore`
- [ ] `block:raw_zinc_block`
- [ ] `block:zinc_block`
- [ ] `block:brass_block`
- [ ] `block:steel_block`
- [ ] `block:sluice`
- [ ] `block:blast_smelter`
- [ ] `block:plate_press`
- [ ] `block:wire_drawer`
- [ ] `block:belt`
- [ ] `block:belt_splitter`
- [ ] `block:sorting_chute`
- [ ] `block:wood_rack`
- [ ] `block:pattern_loom`

### Items
- [ ] `item:raw_zinc`
- [ ] `item:zinc_ingot`
- [ ] `item:zinc_nugget`
- [ ] `item:brass_ingot`
- [ ] `item:steel_ingot`
- [ ] `item:steel_nugget`
- [ ] `item:slag`
- [ ] `item:crushed_zinc`
- [ ] `item:washed_iron`
- [ ] `item:washed_copper`
- [ ] `item:washed_gold`
- [ ] `item:washed_tin`
- [ ] `item:washed_zinc`
- [ ] `item:steel_gear`
- [ ] `item:copper_coil`
- [ ] `item:steel_plate`
- [ ] `item:brass_plate`
- [ ] `item:pattern_card`
- [ ] `item:crab_frame`
- [ ] `item:wire_cutter`
- [ ] `item:steel_pickaxe`
- [ ] `item:steel_axe`
- [ ] `item:steel_shovel`
- [ ] `item:steel_sword`
- [ ] `item:steel_sledgehammer`
- [ ] `item:steel_helmet`
- [ ] `item:steel_chest`
- [ ] `item:steel_legs`
- [ ] `item:steel_boots`
- [ ] `item:steam_repeater`

### Entities
- [ ] `entity:hauler_crab`
- [ ] `entity:falling_crate`

### Systems
- [ ] `sys:load_model`
- [ ] `sys:rack_collapse`
- [ ] `sys:crab_rig`
- [ ] `sys:collect_debris`
- [ ] `sys:pattern_capture`
- [ ] `sys:pattern_library`
- [ ] `sys:equipment_asset_steel`

### Registries
- [ ] `recipe:washing`
- [ ] `recipe:pressing`
- [ ] `recipe:drawing`
- [ ] `menu:sluice`
- [ ] `menu:blast_smelter`
- [ ] `menu:plate_press`
- [ ] `menu:wire_drawer`
- [ ] `menu:belt_splitter`
- [ ] `menu:sorting_chute`
- [ ] `menu:rack`
- [ ] `menu:pattern_loom`
- [ ] `component:pattern_ref`
- [ ] `component:pressure`
- [ ] `saved:pattern`
- [ ] `payload:rack_state`
- [ ] `worldgen:zinc_ore_feature`
- [ ] `worldgen:zinc_ore_placed`
- [ ] `worldgen:zinc_ore_modifier`
- [ ] `structure:rack_row_wood`
- [ ] `template:wood_racks`
- [ ] `adv:materials/tin`
- [ ] `adv:materials/bronze`
- [ ] `adv:materials/crushed`
- [ ] `adv:materials/washed`
- [ ] `adv:materials/steel`
- [ ] `adv:colony/pattern_recorded`
- [ ] `adv:logistics/root`
- [ ] `adv:logistics/belt_line`
- [ ] `adv:logistics/sorted`
- [ ] `adv:logistics/rack_collapse`
- [ ] `adv:workers/debris_crab`
- [ ] `adv:equipment/bronze_age`
- [ ] `adv:equipment/full_pressure`
- [ ] `trigger:equipment_event`

### Wow
- [ ] `wow:W6`

### Tests
- [ ] `test:03#11`
- [ ] `test:03#12`
- [ ] `test:03#13`
- [ ] `test:03#14`
- [ ] `test:04#L3`
- [ ] `test:04#L4`
- [ ] `test:04#L5`
- [ ] `test:04#L6`
- [ ] `test:04#L7`
- [ ] `test:04#L10`
- [ ] `test:04#L11`
- [ ] `test:04#L12`
- [ ] `test:06#8`
- [ ] `test:06#9`
- [ ] `test:07#5`
- [ ] `test:07#6`
- [ ] `test:selftest_m5`

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
- [ ] `sys:area_mining`

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
- [ ] `check:todo_search_clean`
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

