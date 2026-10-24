---
navigation:
  title: Seed Matrix
  parent: t6/colony.md
  position: 30
  icon: autarkia:seed_matrix
item_ids:
  - autarkia:seed_matrix
---

# Seed Matrix

<ItemImage id="autarkia:seed_matrix" scale="2" />

The Seed Matrix packs a whole base into one <ItemLink id="autarkia:autarky_seed" />. You give it 1 to 8 recorded
pattern cards and set their layout. The matrix takes the materials and a crew of 4 workers from your colony and makes
the seed. The cards stay in the matrix, so you can pack the same base again.

## Get it

<RecipeFor id="autarkia:seed_matrix" />

## Before you start

- A Pattern is a recorded build. Record it at a <ItemLink id="autarkia:pattern_loom" /> on a
  <ItemLink id="autarkia:pattern_card" />.
- Card 1 must hold an OUTPOST Pattern. A Pattern is OUTPOST when its box holds an outpost core. An
  <ItemLink id="autarkia:outpost_seed" /> grows an outpost core.
- The other cards can hold any Pattern: generators, machine lines, storage, defense.

## Use it

1. Place the matrix inside the domain of your tier 6 colony. It is 3 x 3 x 3. Its center is the bottom middle block.
2. Connect it to your network.
3. Open it. Put the recorded cards into the 8 card slots on the left. Card 1 goes into the top slot.
4. Set the layout of each card. A new card starts east of the cards before it.
5. Put the pack items into the storage of your colony (crates, racks and depots of the domain).
6. Keep free MU in the <ItemLink id="autarkia:matter_buffer" /> blocks of the domain.
7. Click Pack. You must be the owner or a trusted player.
8. Wait 1,200 ticks. Take the Autarky Seed out of the output slot (bottom right).

## The layout

Each card row shows "x N", "z N" and the rotation in degrees.

- The - and + buttons next to x move the Pattern 1 block along the x axis.
- The - and + buttons next to z move it 1 block along the z axis.
- The R button turns it 90 degrees clockwise.
- Hold Shift for steps of 8 blocks. Each offset goes from -96 to 96.

The layout rules:

- No two footprints can overlap.
- The whole footprint is at most 96 x 48 x 96 blocks.
- Every block must lie in the outpost square around the outpost core of card 1. At tier 6 this square is 65 x 65
  (32 blocks on each side of the core).

Under the rows the screen shows the size of the footprint and its block count. The state line is green when the matrix
is ready or packing, and red for a problem.

## What a pack costs

| Part | Cost |
|---|---|
| From colony storage | 1 <ItemLink id="autarkia:ai_core" />, 16 <ItemLink id="autarkia:nanite_paste" /> |
| Crew parts (2 Porters) | 2 <ItemLink id="autarkia:walker_frame" />, 4 <ItemLink id="autarkia:mainspring" />, 8 <ItemLink id="autarkia:bronze_gear" />, 8 <ItemLink id="autarkia:bronze_ingot" /> |
| Crew parts (2 Builder Drones) | 2 <ItemLink id="autarkia:drone_frame" />, 4 <ItemLink id="autarkia:basic_circuit" />, 8 <ItemLink id="autarkia:servo" />, 8 <ItemLink id="autarkia:copper_coil" />, 8 <ItemLink id="autarkia:steel_plate" /> |
| Matter | the materials of all Patterns, priced in MU, from the Matter Buffers |
| Energy | 1,000,000 FE at 834 FE/t |
| Time | 1,200 ticks |

The matrix takes all items at once when you click Pack. It takes the matter in 20 equal parts, one part every 60 ticks.
The packing waits while power or matter is missing.

## Numbers

| Property | Value |
|---|---|
| Size | 3 x 3 x 3 |
| Card slots | 8 |
| Pack time | 1,200 ticks |
| Power | 834 FE/t, 1,000,000 FE in total |
| FE buffer | 166,800 FE, input up to 3,336 FE/t |
| Mining tool | Diamond pickaxe or better |

## Jade and status lines

- "Patterns N/8": the cards with a recorded Pattern.
- "Matter N%": the paid part of the matter.
- "Buffer: N / 166800 FE".
- "Ready to pack": click Pack.
- "Packing": the pack runs.
- "Insert recorded pattern cards": the card slots are empty.
- "A card holds no recorded Pattern": a card is blank, or its Pattern is not on this server.
- "Card 1 must be an OUTPOST Pattern with an outpost core": put an outpost card into the top slot.
- "Two footprints overlap": move a card with the x and z buttons.
- "Footprint over 96 x 48 x 96": use fewer or smaller Patterns.
- "Footprint leaves the outpost square": move the cards nearer to the outpost core.
- "Outside a colony domain": move the matrix into your domain.
- "Needs a tier 6 colony": upgrade the Core first.
- "Take the seed out first": the output slot is full.
- "Colony storage lacks pack items": the next line names the first missing item, for example "Lacks ai_core x1". Put
  the missing items into colony storage. Click Pack again.
- "Waiting for matter": the Matter Buffers have too little free MU.
- "No power": the matrix has too little FE.

## Problems

- The Pack button is grey: the state is not "Ready to pack" or "Colony storage lacks pack items".
- The layout buttons disappear: a pack runs. You cannot change the layout during a pack.
- If you break the matrix, it drops the cards, the seed and the items of a running pack.

## Next

- Throw the <ItemLink id="autarkia:autarky_seed" />.
