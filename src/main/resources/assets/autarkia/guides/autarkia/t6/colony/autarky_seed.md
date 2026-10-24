---
navigation:
  title: Autarky Seed
  parent: t6/colony.md
  position: 40
  icon: autarkia:autarky_seed
item_ids:
  - autarkia:autarky_seed
---

# Autarky Seed

<ItemImage id="autarkia:autarky_seed" scale="2" />

The Autarky Seed is a packed base. You throw it, and a complete outpost grows where it lands: the ground levels, the
blocks rise from the soil, the cables connect and a crew of workers unfolds. The new base is an outpost of your colony.

## Get it

The Autarky Seed has no crafting recipe. Only the <ItemLink id="autarkia:seed_matrix" /> makes it.

1. Record the base at a <ItemLink id="autarkia:pattern_loom" />. The Pattern of card 1 must hold an outpost core. An
   <ItemLink id="autarkia:outpost_seed" /> grows one.
2. Put the cards into the Seed Matrix and set the layout.
3. Put 1 <ItemLink id="autarkia:ai_core" />, 16 <ItemLink id="autarkia:nanite_paste" /> and the parts of 2 Porters and 2
   Builder Drones into colony storage.
4. Keep enough free MU in the Matter Buffers for the materials of all Patterns.
5. Click Pack. After 1,200 ticks and 1,000,000 FE the seed is in the output slot.

The [Seed Matrix](seed_matrix.md) page lists every cost and every status line.

## Use it

1. Go to open ground 64 to 512 blocks from your Colony Core, and at least 64 blocks from your other outposts.
2. Look in the direction that the base must face. Your facing sets the rotation of the whole base.
3. Use the seed. You throw it like an ender pearl.
4. The base is centered on the place where the seed lands. Its floor is the middle height of the ground there.
5. Wait. Stay near, so that the chunks stay loaded.

## The unpack

| Tick | Step |
|---|---|
| 0 to 40 | A ring of light grows to the edge of the footprint. |
| From 40 | The ground levels: up to 64 block edits per tick. Blocks above the floor are cut, holes below it are filled with dirt. |
| From 140 | The blocks rise from the soil into place, bottom layer first, from the center outward. |
| After the last block | All cable spans connect at once with an arc. |
| 40 ticks later | The outpost is ready. Its core gets the 1,000,000 FE charge. The workers unfold. |

The whole unpack takes about 1,100 ticks. It pauses while a chunk of the footprint is not loaded.

- Each dirt fill uses one cut block. When no cut block is left, it uses 1 free MU.
- The leveling never touches block entities.
- Each worker of the crew unfolds only when the worker cap has room. Else its parts go into the depots of the outpost.
- You get the advancement "Instant Base".

## Tooltip

- "N Patterns, X x Y x Z, N blocks": the contents of the seed.
- "4 workers, 1000000 FE charge": the crew and the charge for the outpost core.
- "Empty seed": the seed holds nothing. You cannot throw it.

## Before you throw

The server checks these rules where the seed lands. If one fails, the seed drops as an item and you get the message
"The seed drops: reason".

- Your colony is active and at tier 6.
- The colony has a free outpost slot (6 at tier 6).
- The outpost core is 64 to 512 blocks from the Colony Core and 64 blocks or more from every other outpost.
- The outpost square does not overlap another colony.
- All chunks of the footprint are loaded.
- The footprint holds no block entity (no chest, no machine).
- Fluid covers less than half of the footprint.
- The layout still fits the outpost square, and all its Patterns still exist on the server.

## Problems

- "This seed holds no Patterns": the seed is empty.
- "An Autarky Seed needs an active tier 6 colony": upgrade your Core.
- "You manage no colony this seed can serve": you are not the owner or a trusted player of a colony in this dimension.
- "This colony has all the outposts its tier allows": remove an outpost first.
- "An outpost must be 64 to 512 blocks from the Core": throw it at another distance.
- "Too close to another outpost (64 blocks minimum)": throw it farther away.
- "The outpost area would overlap another colony": throw it away from the other colony.
- "The seed footprint is not fully loaded": go nearer, so that all chunks load.
- "A block entity stands in the seed footprint": remove chests and machines from the area.
- "Fluid covers half or more of the seed footprint": choose dry ground.
- "The seed layout does not fit this colony's outpost square": pack the seed again with a smaller layout.
- "A Pattern of this seed is missing on this server": the seed came from another server, or a Pattern is gone.
- "Protection refuses the outpost core space": a protection mod does not let the seed clear the space.
- "The 2 x 2 x 2 space for the outpost core must be free": the outpost core cannot grow there.
- "Seed unpacking is switched off (seed.enabled)": the server config turns seeds off.

## Jade

Look at the new outpost core during the unpack:

- "Seed: glow ring".
- "Seed: leveling, N edits".
- "Seed: leveling waits for matter (N edits)": the domain needs free MU for the dirt fills.
- "Seed: rising N/N".
- "Seed: connecting spans".
- "Seed unpack paused": a chunk is not loaded, or the server config turns seeds off.

## Next

- The new outpost works like any outpost of your colony.
- Pack the same cards again for the next base. The cards stay in the Seed Matrix.
