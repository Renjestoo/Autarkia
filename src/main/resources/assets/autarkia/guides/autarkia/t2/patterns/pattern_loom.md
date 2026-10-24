---
navigation:
  title: Pattern Loom
  parent: t2/patterns.md
  position: 10
  icon: autarkia:pattern_loom
item_ids:
  - autarkia:pattern_loom
---

# Pattern Loom

<ItemImage id="autarkia:pattern_loom" scale="2" />

The pattern loom records a build into a Pattern on a blank <ItemLink id="autarkia:pattern_card" />. It needs no power.
It works only inside the domain of a Tier 2 or higher colony.

## Get it

<RecipeFor id="autarkia:pattern_loom" />

## Use it

1. Place the loom inside your domain, near the build that you want to record.
2. Open the loom and put a blank pattern card into its card slot.
3. A glowing outline shows the box. At first it is 8 x 8 x 8 blocks in front of the loom.
4. Set the box with the Offset and Size buttons until the outline holds the whole build.
5. Set the toggles. Type a name for the Pattern, or leave the name empty.
6. Click Record. A glow plane sweeps up through the box. The card gets the Pattern.
7. A chat message shows the name, the number of blocks and the result.

You must be the owner or a trusted player of the colony.

## Screen

- Card slot: the blank card goes in, the recorded card comes out here.
- "Offset" row: the X, Y and Z distance of the box corner from the loom. Use - and + to move it, from -24 to 24.
- "Size" row: the X, Y and Z size of the box. Use - and + to change it, from 1 to the tier maximum.
- "Max size 16": the largest size for each axis at your colony tier.
- Protect (on at first): the colony watches the build. The planner never builds a project into the box.
- Library (on at first): the Pattern goes into the library of the colony.
- Strict air (off at first): the Pattern also records the empty blocks of the box.
- Pattern name: up to 32 letters. Without a name the Pattern gets "Pattern" and a number.
- Record: starts the scan.
- "Scanning 40%": the scan runs.

Jade shows "No card", "Blank card", or the Pattern on the card, for example "Pattern Smelter 8x5x8, 120 blocks". While
the loom scans, it shows "Scanning 40%".

## What a Pattern keeps

- Every block in the box.
- The settings of Autarkia machines, for example the filter of a <ItemLink id="autarkia:belt_splitter" />.
- The spans between two nodes in the box.
- Blocks of other mods. Their contents are not kept: the loom records them empty.

Fire, flowing water and flowing lava are not recorded. The colony gives each Pattern a role from what is in the box:
Line (machines and belts), Generator, Storage, Defense, Outpost or Generic.

## Numbers

| Property | Value |
|---|---|
| Largest box at Tier 2 | 16 x 16 x 16 |
| Box at first | 8 x 8 x 8 |
| Offset | -24 to 24 for each axis |
| Library at Tier 2 | 4 Patterns |
| Scan speed | 4,096 blocks per tick |
| Power | none |

## Problems

- "Patterns need a T2 colony": upgrade the Core with the <ItemLink id="autarkia:core_upgrade_t2" />.
- "Put a blank pattern card in the loom": the slot is empty or the card has a Pattern already.
- "Only the owner or trusted players of this domain can record": ask the owner to trust you.
- "The box is larger than the colony tier allows": make the box smaller.
- "Part of the box is not loaded": go nearer to the box.
- "The loom is already scanning": wait until the scan ends.
- The chat message ends with "Not added to the library.": the library is full, or Library was off.

## Next

- Add a recorded card to the library later: see the <ItemLink id="autarkia:pattern_card" />.
