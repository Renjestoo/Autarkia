"""Checklist content for STATUS.md: every content item, id (08a, 08b) and test id (12) per milestone."""

M = {}


def ms(key, title, groups):
    M[key] = (title, groups)


def ids(prefix, names):
    return [f"{prefix}:{n}" for n in names.split()]


ms("M0", "Toolchain and API spikes (0.1.0)", {
    "Toolchain": ["tool:jdk25", "tool:gradle_project", "tool:git_init", "tool:gradle_py", "tool:apidump", "tool:textures_py",
                  "tool:geo_py", "tool:data_py", "tool:status_py", "tool:coverage_py", "tool:packrun_py", "tool:install_jar_py"],
    "Skeleton": ["code:Autarkia", "code:AutarkiaClient", "code:registries", "code:config_specs", "code:payload_registrar",
                 "code:AnthologyBridge", "code:GhostBlocks", "code:fx_commands"],
    "Tests": ids("test", "T0.1 T0.2 T0.3 T0.4 T0.5 T0.6 T0.7 T0.8 T0.9"),
})

ms("M1", "T1 power and processing (0.2.0)", {
    "Systems": ["sys:grid_core", "sys:span_sync", "sys:catenary_renderer", "sys:rope_slip_wear", "sys:machine_recipe",
                "sys:transit_segments", "sys:jei_crushing_alloying", "sys:jade_power_machine", "sys:guide_structure_copy"],
    "Blocks": ids("block", "tin_ore deepslate_tin_ore raw_tin_block tin_block bronze_block hand_crank rope_pulley quern stamp_mill crucible crate tally_board chute foundation"),
    "Items": ids("item", "raw_tin tin_ingot tin_nugget bronze_ingot bronze_nugget crushed_iron crushed_copper crushed_gold crushed_tin bronze_gear mainspring rope_coil wrench multimeter"),
    "Registries": ids("recipe", "crushing alloying") + ids("payload", "span_list span_state multimeter_reading transit_delta")
    + ids("saved", "grid") + ids("menu", "stamp_mill crucible crate") + ids("component", "link_start")
    + ids("worldgen", "tin_ore_feature tin_ore_placed tin_ore_modifier"),
    "Tests": ids("test", "02#1 02#2 03#1 03#7 03#8 03#9 03#10 04#L1 04#L2 selftest_m1"),
})

ms("M2", "T1 physics and bronze equipment (0.3.0)", {
    "Blocks": ids("block", "treadwheel windmill stone_flywheel"),
    "Entities": ids("entity", "rolling_flywheel snapped_cable sling_shot"),
    "Items": ids("item", "bronze_pickaxe bronze_axe bronze_shovel bronze_hoe bronze_sword bronze_helmet bronze_chest bronze_legs bronze_boots bronze_sling"),
    "Systems": ["sys:hud_layer", "sys:mode_key", "sys:frozenlib_wind", "sys:equipment_asset_bronze"],
    "Registries": ids("component", "energy tool_mode") + ids("payload", "equipment_action") + ids("damage", "sling")
    + ids("adv", "power/root power/first_span power/rope_snap power/rolling_wheel"),
    "Wow": ["wow:W3", "wow:W4"],
    "Tests": ids("test", "02#3 02#4 02#5 02#20 07#1 07#2 07#3 selftest_m2"),
})

ms("M3", "Colony Core and Porter (0.4.0)", {
    "Blocks": ids("block", "colony_core project_board worker_assembler winding_post"),
    "Items": ids("item", "core_seed planning_tablet worker_capsule walker_frame field_manual"),
    "Entities": ids("entity", "porter"),
    "Systems": ["sys:colony_savedata", "sys:domain", "sys:ownership", "sys:sensors_S01_S09_S15_S20", "sys:planner", "sys:templates_1_6",
                "sys:site_search", "sys:cost_priority_budget", "sys:colony_clock", "sys:ghost_preview", "sys:autonomy", "sys:w2_sign",
                "sys:job_system", "sys:walker_rig", "sys:spring_energy", "sys:stuck_recovery", "sys:core_screen_tabs",
                "sys:debug_commands", "sys:guide_t1_chapter"],
    "Registries": ids("menu", "colony_core project_board planning_tablet worker_assembler") + ids("recipe", "worker_assembly")
    + ids("component", "colony_link") + ids("payload", "preview_request project_preview colony_sync colony_action worker_debug")
    + ids("saved", "colonies") + ids("memory", "assigned_job job_phase job_step fetch_source charger_pos needs_recharge stuck_level debris_targets guard_route")
    + ids("sensor", "job_board energy charger stuck debris guard_route")
    + ids("structure", "windmill treadwheel crate_cluster flywheel winding_post")
    + ids("template", "windmill treadwheel crate_cluster flywheel parallel_span winding_post")
    + ids("adv", "colony/root colony/first_proposal colony/it_fixed_itself workers/first_porter workers/self_built")
    + ids("trigger", "colony_event worker_event"),
    "Wow": ["wow:W1", "wow:W2"],
    "Tests": ids("test", "05#W1 05#W1fail 05#W2 05#W2manual 06#1 06#2 06#3 06#4 06#5 06#6 06#7 06#28 06#29 06#30 selftest_m3"),
})

ms("M4", "T2 power (0.5.0)", {
    "Blocks": ids("block", "wooden_pole waterwheel boiler pressure_valve steam_engine lightning_mast lightning_arrester capacitor_bank industrial_lamp catwalk catwalk_stairs hazard_block"),
    "Items": ids("item", "copper_wire_spool core_upgrade_t2"),
    "Systems": ["sys:heat_model", "sys:snap_arcs", "sys:lamp_ripple", "sys:boiler_burst", "sys:lightning_pulse", "sys:failure_events_02"],
    "Registries": ids("menu", "boiler energy_storage") + ids("payload", "lightning_pulse")
    + ids("structure", "waterwheel boiler_engine lightning_mast")
    + ids("template", "waterwheel boiler_engine span_upgrade lightning_mast lightning_arrester pressure_valve")
    + ids("adv", "power/red_hot power/surge power/storm_harvest power/boiler_burst"),
    "Wow": ["wow:W5", "wow:W7", "wow:W8", "wow:W9"],
    "Tests": ids("test", "02#6 02#7 02#9 02#10 02#11 02#12 02#13 selftest_m4"),
})

ms("M5", "T2 processing, belts, racks, Patterns (0.6.0)", {
    "Blocks": ids("block", "zinc_ore deepslate_zinc_ore raw_zinc_block zinc_block brass_block steel_block sluice blast_smelter plate_press wire_drawer belt belt_splitter sorting_chute wood_rack pattern_loom"),
    "Items": ids("item", "raw_zinc zinc_ingot zinc_nugget brass_ingot steel_ingot steel_nugget slag crushed_zinc washed_iron washed_copper washed_gold washed_tin washed_zinc steel_gear copper_coil steel_plate brass_plate pattern_card crab_frame wire_cutter steel_pickaxe steel_axe steel_shovel steel_sword steel_sledgehammer steel_helmet steel_chest steel_legs steel_boots steam_repeater"),
    "Entities": ids("entity", "hauler_crab falling_crate"),
    "Systems": ["sys:load_model", "sys:rack_collapse", "sys:crab_rig", "sys:collect_debris", "sys:pattern_capture", "sys:pattern_library", "sys:equipment_asset_steel"],
    "Registries": ids("recipe", "washing pressing drawing") + ids("menu", "sluice blast_smelter plate_press wire_drawer belt_splitter sorting_chute rack pattern_loom")
    + ids("component", "pattern_ref pressure") + ids("saved", "pattern") + ids("payload", "rack_state")
    + ids("worldgen", "zinc_ore_feature zinc_ore_placed zinc_ore_modifier")
    + ids("structure", "rack_row_wood") + ids("template", "wood_racks")
    + ids("adv", "materials/tin materials/bronze materials/crushed materials/washed materials/steel colony/pattern_recorded logistics/root logistics/belt_line logistics/sorted logistics/rack_collapse workers/debris_crab equipment/bronze_age equipment/full_pressure")
    + ids("trigger", "equipment_event"),
    "Wow": ["wow:W6"],
    "Tests": ids("test", "03#11 03#12 03#13 03#14 04#L3 04#L4 04#L5 04#L6 04#L7 04#L10 04#L11 04#L12 06#8 06#9 07#5 07#6 selftest_m5"),
})

ms("M6", "T3 Electric Works (0.7.0)", {
    "Blocks": ids("block", "steel_pylon wall_insulator transformer fuse_box steam_turbine battery_bank jaw_crusher magnetic_separator electrolyzer fabricator steel_rack warehouse_controller ledger_terminal capsule_tube capsule_station recharge_dock domain_beacon prop_beam"),
    "Items": ids("item", "insulated_cable_spool pure_iron pure_copper pure_gold pure_tin pure_zinc basic_circuit servo drone_frame worker_upgrade_speed worker_upgrade_cargo worker_upgrade_armor worker_upgrade_range core_upgrade_t3 survey_lens electric_drill electric_chainsaw shock_baton exo_rig_helmet exo_rig_chest exo_rig_legs exo_rig_boots arc_rifle"),
    "Entities": ids("entity", "builder_drone loader lineman"),
    "Systems": ["sys:failure_memory", "sys:pattern_repair", "sys:duplicate_line", "sys:drone_rig", "sys:fe_battery", "sys:worker_upgrades", "sys:warehouse_requests", "sys:capsule_routing", "sys:area_mining"],
    "Registries": ids("recipe", "separating electrolyzing fabricating") + ids("menu", "steam_turbine fuse_box jaw_crusher magnetic_separator electrolyzer fabricator capsule_station warehouse_controller ledger_terminal")
    + ids("payload", "stock_view_request stock_view ledger_request ledger_status") + ids("damage", "shock arc")
    + ids("structure", "recharge_dock turbine_hall battery_row")
    + ids("template", "fuse_box rack_upgrade duplicate_line pattern_repair recharge_dock steam_turbine battery_bank")
    + ids("adv", "materials/pure colony/self_repair colony/second_line colony/learned logistics/pneumatic logistics/fetch workers/drone_repair workers/fuse_unordered workers/loader_delivery equipment/live_wire equipment/clean_cut"),
    "Wow": ["wow:W10", "wow:W11", "wow:W12", "wow:W13", "wow:W14"],
    "Tests": ids("test", "02#8 02#14 02#15 03#15 03#16 03#17 04#L8 04#L9 04#L13 04#W12 04#W14 05#W10 05#W10accept 05#W11 05#W13 05#W13decay 06#10 06#11 06#12 06#13 06#14 06#15 06#16 06#17 07#7 07#8 07#9 07#10 07#11 selftest_m6"),
})

ms("M7", "T4 Frontier (0.8.0)", {
    "Blocks": ids("block", "vantite_ore raw_vantite_block vantite_block vantite_steel_block ore_outcrop magma_vent hv_pylon hydro_turbine wind_tower magma_tap froth_flotation_plant cable_car_station crate_launcher catch_net depot outpost_core"),
    "Items": ids("item", "raw_vantite vantite_ingot vantite_steel_ingot crushed_vantite washed_vantite pure_vantite iron_concentrate copper_concentrate gold_concentrate tin_concentrate zinc_concentrate vantite_concentrate advanced_circuit hv_cable_spool outpost_seed core_upgrade_t4 vantite_excavator grapple_launcher vantite_blade frontier_suit_helmet frontier_suit_chest frontier_suit_legs frontier_suit_boots rail_rifle"),
    "Entities": ids("entity", "surveyor cable_layer_drone sentinel gondola grapple_hook"),
    "Systems": ["sys:outcrop_feature", "sys:hv_sway", "sys:outposts", "sys:catch_up", "sys:linked_stock_view", "sys:travel_tickets"],
    "Registries": ids("recipe", "flotation") + ids("menu", "froth_flotation_plant cable_car_station crate_launcher depot outpost_core")
    + ids("saved", "domain_stock") + ids("ticket", "outpost worker_travel") + ids("damage", "rail")
    + ids("worldgen", "outcrop_ore tin_deposit zinc_deposit vantite_ore vantite_deposit iron_deposit copper_deposit gold_deposit magma_vent magma_vent_nether vanilla_deposits_modifier vantite_ore_modifier magma_vent_modifier magma_vent_nether_modifier")
    + ids("structure", "outpost_basic sentinel_post wind_tower hydro")
    + ids("template", "outpost hv_link cable_car sentinel_post wind_tower hydro_turbine")
    + ids("adv", "materials/concentrate materials/vantite colony/frontier logistics/sky_freight logistics/special_delivery workers/pylon_line workers/sentinel_kill equipment/swing_line"),
    "Wow": ["wow:W15", "wow:W16", "wow:W17"],
    "Tests": ids("test", "03#2 03#3 03#18 03#19 04#L14 04#W16 04#W17 05#W15 05#W15catchup 06#18 06#19 06#20 06#21 06#22 06#23 07#4 07#12 07#13 07#14 07#15 07#16 selftest_m7"),
})

ms("M8", "T5 Matter (0.9.0)", {
    "Blocks": ids("block", "lattice_cluster lattice_geode_shell lattice_alloy_block lattice_block plasma_generator cryo_node superconducting_coil plasma_separator nanite_hive claim_marker matter_buffer mag_rail mag_rail_station lattice_rack replicator_foundry"),
    "Items": ids("item", "lattice_shard lattice_crystal lattice_alloy_ingot nanite_paste ai_core lattice_fuel_cell coolant_cell superconductor_spool core_upgrade_t5 nanite_multitool lattice_blade lattice_carapace_helmet lattice_carapace_chest lattice_carapace_legs lattice_carapace_boots disassembler_beam"),
    "Entities": ids("entity", "replicator_walker nanite_swarm"),
    "Systems": ["sys:quench", "sys:matter_values", "sys:nanite_claims", "sys:replication", "sys:mag_rail_sleds"],
    "Registries": ids("recipe", "plasma_separating") + ids("menu", "plasma_generator cryo_node superconducting_coil plasma_separator mag_rail_station matter_buffer nanite_hive replicator_foundry")
    + ids("payload", "nanite_bubble") + ids("component", "matter_link matter_cache suit_enabled")
    + ids("worldgen", "lattice_geode lattice_geode_modifier") + ids("structure", "matter_buffer")
    + ids("template", "replicator_walker matter_buffer") + ids("damage", "beam")
    + ids("adv", "materials/lattice materials/plasma colony/clean_pit colony/not_built_by_you logistics/float_on logistics/matter_of_fact workers/replicator_deploy workers/claim_done power/quench equipment/ghost_matter"),
    "Wow": ["wow:W18", "wow:W19", "wow:W20"],
    "Tests": ids("test", "02#16 02#17 03#4 03#5 03#20 03#21 04#L15 05#W18 05#W18kill 05#W19 06#24 06#25 06#26 06#27 07#17 07#18 07#19 07#20 selftest_m8"),
})

ms("M9", "T6 Ascendant (0.10.0)", {
    "Blocks": ids("block", "singularity_core gyro_heart phase_node matter_compiler domain_archive matter_conduit seed_matrix restoration_projector forge_of_ascension bastion_block bastion_glass"),
    "Items": ids("item", "aegis_plate autarky_seed core_upgrade_t6 aegis_multitool aegis_blade aegis_helmet aegis_chest aegis_legs aegis_boots singularity_lance"),
    "Entities": ids("entity", "rising_block thrown_autarky_seed"),
    "Systems": ["sys:explosion_absorb", "sys:seed_unpack", "sys:restoration_field", "sys:aegis_rule", "sys:nanite_reform", "sys:bastion", "sys:forge_fitting", "sys:player_pose"],
    "Registries": ids("recipe", "compiling ascension_fitting") + ids("menu", "singularity_core matter_compiler domain_archive matter_conduit seed_matrix restoration_projector forge_of_ascension")
    + ids("payload", "domain_flash seed_unpack aegis_status ascension_pose beam_visual reform_fx") + ids("component", "enchant_mode seed_contents")
    + ids("attachment", "reform_ready_at") + ids("saved", "snapshot") + ids("structure", "restoration_projector")
    + ids("template", "restoration_projector")
    + ids("adv", "materials/compiler materials/aegis colony/instant_base colony/undo_crater colony/ascendant logistics/everywhere power/phase_link power/drink_the_blast workers/full_cap equipment/ascended equipment/paid_in_full equipment/not_today equipment/unbroken"),
    "Wow": ["wow:W21", "wow:W22", "wow:W23", "wow:W24", "wow:W25"],
    "Tests": ids("test", "02#18 02#19 03#22 03#23 04#L16 05#W21 05#W21fail 05#W25 05#W25accept 07#21 07#22 07#23 07#24 07#25 07#26 07#27 07#28 07#29 07#30 07#31 07#32 07#33 07#34 07#35 07#36 07#37 selftest_m9"),
})

ms("M10", "Polish and performance (0.11.0)", {
    "Integration": ["int:jei_all_categories", "int:jei_info_pages", "int:jade_all_providers", "int:yacl_screen", "int:guide_all_chapters", "int:guide_3d_scenes"],
    "Data": ["data:advancements_68", "data:sounds_107_checked", "data:tags_all", "data:damage_types_5", "data:equipment_assets_6", "data:config_files_2", "data:creative_tabs_6", "data:key_mappings_3"],
    "Checks": ["check:02#21_grid_budget", "check:06#31_worker_budget", "check:04_section5_targets", "check:profile_t6_base",
               "check:multiplayer_span_sync", "check:multiplayer_transit_sync", "check:multiplayer_worker_anims", "check:multiplayer_ascension_pose",
               "check:soak_1h_clean", "check:coverage_zero_missing", "check:final_selftests_m1_m9", "check:marker_search_clean",
               "check:single_jar_in_instance", "check:fullpack_clean_log", "check:processes_stopped"],
})
