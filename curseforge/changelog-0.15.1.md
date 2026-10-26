## Autarkia 0.15.1

### Textures

- **Ingots and nuggets:** new designs in the vanilla style. Each bar is tilted, has an outline in the colour of its metal and has 8 shades with a hue shift.
- **Ores:** tin, zinc and vantite ores, and their deepslate types, are now an ore overlay on the real vanilla stone or deepslate texture. They look like the vanilla ores and match the stone around them. The ore outcrops use the same overlays.
- **Blocks:** the wood, stone, metal and machine block textures have more shades and softer detail. They no longer look like 8-bit art.
- **Machine models:** all 43 material finishes of the 53 machine models were painted again with 16-shade hue-shifted ramps and soft clusters. The dithering, checkerboard patterns and bright bevel frames are gone. The item icons, the armour and the worker textures use the new finishes.

### Machine screens

- **Each screen has its own background** in the material of its machine (wood, brick, bronze, brass, steel, aegis and more). The Colony Core, Multimeter and Linked Stock View use framed panels.
- **No more text out of the panel or on top of other parts.** Long names and values stop before the next button or the panel edge, and end with "...".
- **Small windows:** the tall screens are now at most 240 GUI pixels high, so they fit a 720p window at the largest GUI scale:
  - Ledger Terminal and Domain Archive: 264 x 240, with a 2-row stock grid and a request or log column on the right.
  - Mag Rail Station: 236 x 234, with the Input and Output tabs on the title row and readable status lines.
  - Nanite Hive: 272 x 240, with wide marker buttons and a full line for each state text.
  - Seed Matrix: 304 x 220, with two columns of 4 pattern cards.
- **Machines that cannot run now say why** in place of "Condition missing":
  - Blast Smelter, Jaw Crusher, Froth Flotation Plant and Plasma Separator: "No foundation".
  - Sluice: "No water at the head". Crucible: "No heat source".
  - Worker Assembler: "Outside colony or tier low".
- **New status messages:**
  - Boiler: "No water", "No fuel", "Fire off: pick 1 to 4", "Heating up" and "Making steam".
  - Steam Turbine: "No boiler", "Low steam" and "Buffer full".
  - Plasma Generator: the reason when Ignite is not available ("No fuel cell to ignite", "Lit: ignite only when cold").
  - Warehouse Controller: "No power".
  - Matter Conduit: "No power", "No receiver", "Target full" and "No output block".
  - Matter Buffer: "Item not accepted" and "Buffer full".
  - Matter Compiler: "Not enough MU", "No foundation" and "No target". The status no longer covers the MU bar.
  - Pattern Loom: "Scanning N%", "Not in a T2+ colony domain", "Insert a blank pattern card", "The card is not blank", "Box too big" and "Ready". Record is only available when the loom is ready.
  - Ledger Terminal and Domain Archive: "The warehouse is empty", "Nothing stored in the domain", "No item matches the search", "No requests" and "No withdrawals yet". Request and deliver are only available when you select an item.
  - Linked Stock View: "Loading stock..." until the data comes. The order reasons are shorter, so they fit: "No transport" and "None at site".
- **Other screen fixes:**
  - Racks: the red "Collapse in N s" warning is on the title row and is fully readable.
  - Depot: a long site name in the title stops at the panel edge.
  - Restoration Projector: the size and offset values no longer touch their - and + buttons.
  - Capacitor Bank, Battery Bank and Superconducting Coil: input and output are on two lines.
  - Steam Turbine: the dial moved left, so all values fit in the panel.
  - Matter Buffer: wider panel, wider Take and Dissolve buttons.
  - Pattern Loom: wider - and + buttons. The card slot is now next to the name box.
  - Filter, Fabricator, Singularity Core, Crate Launcher and Forge of Ascension: their buttons and texts no longer overlap.
  - Ledger Terminal and Seed Matrix: the Sites and Pack buttons no longer touch the panel frame.
  - Multimeter: a wider panel, and each value has room. The stored energy has its own line.

### Colony Core screen

- **Projects:** the project card shows the most important parts first: name, reason, the blocked reason, cost and the bill of materials. Priority, site and labour follow when there is room.
- **The blocked reason is a readable text**, for example "Blocked: target obstructed".
- **More than 9 projects:** the list shows "and N more".
- **No manage rights:** the screen says "View only: you need manage rights to change this colony".
- **Rack warning:** it shows in the project card column and no longer covers the project buttons.
- **Jobs tab:** the job status is at the start of each line, so you always see it.
- **Workers tab:** the worker state is a readable text, for example "Failed: target obstructed" or "Not loaded". A failed worker shows in orange.
- **Overview tab:** put the mouse on a log line that is too long to see the full text.
- **Patterns tab:** long pattern names and instance lines stop before their buttons.

### Bug fixes

- **Worker Assembler:** the screen showed a wrong status. It now shows the real assembler status.
