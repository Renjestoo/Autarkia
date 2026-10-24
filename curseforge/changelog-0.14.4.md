## Autarkia 0.14.4

### Field Manual: rewritten from the start

- **Every item has its own page.** All 240 items, blocks, machines, tools and armor pieces have a dedicated page. Hold an item and press the guide key (G) to open its page.
- **Each page explains the item.** It shows what the item is, how to get it (crafting recipe, or the machine recipe as a table, or where it generates), how to use it step by step, its numbers, the status messages and what they mean, and what to do next.
- **Full progression.** Each tier chapter now starts with a numbered step list from the start of the tier to the Core Upgrade that opens the next tier. It also says what the colony and its workers do by themselves. The start page gives the path through all 6 tiers.
- **Pages no longer fail to open.** Six pages (Tier 1 power, storage and workers, Tier 2 steam and lightning, Tier 4 colony) did not open. Their 3D scenes placed machines in a preview world, and FrozenLib stopped the page when a machine block entity loaded. The guide now uses item pictures instead of 3D scenes.
- **New material chapters** for tin and bronze, zinc, brass and steel, pure metals, vantite and concentrates, and lattice.
- **Corrected facts.** The pages are written from the mod code, so some old statements are corrected. Examples: bronze armor gives 13 armor points for the full set, the Forge of Ascension needs 2 Aegis Plates, and the colony upgrades wood racks to steel racks only.

### Colony Core screen

- **Settings tab:** the project switches no longer run off the screen or cover the other controls. They show 10 per page in 2 wide columns, with page buttons. Long names show in full.
- **Overview tab:** the graph no longer covers the power line, and the graph legend fits in the panel.
- **Clear power readout:** the Core uses no power. Its two numbers describe the grid in the domain, so they now read "Power made" and "Power used" on the Overview tab and "Domain grid: 80 FE/t made, 0 FE/t used" in Jade. Power used counts only machines that work.
- **The colony log shows names, not ids.** For example, "Proposed Winding Post (priority 26)" instead of "Proposed winding_post P26".
- **Projects, Jobs and Workers tabs:** long lines stop at the edge of the panel or before the buttons.
