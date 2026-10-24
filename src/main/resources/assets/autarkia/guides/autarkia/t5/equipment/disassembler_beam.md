---
navigation:
  title: Disassembler Beam
  parent: t5/equipment.md
  position: 30
  icon: autarkia:disassembler_beam
item_ids:
  - autarkia:disassembler_beam
---

# Disassembler Beam

<ItemImage id="autarkia:disassembler_beam" scale="2" />

The Disassembler Beam takes blocks apart from a distance. Hold use and a violet beam removes up to 2 blocks per tick
along your line of sight. It also hurts the first creature in its path.

## Get it

The <ItemLink id="autarkia:fabricator" /> makes it. Fabricator recipes do not show in this book, so the table gives
the recipe.

| Inputs | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 6 <ItemLink id="autarkia:lattice_alloy_ingot" />, 4 <ItemLink id="autarkia:nanite_paste" />, 1 <ItemLink id="autarkia:ai_core" />, 4 <ItemLink id="autarkia:lattice_crystal" />, 2 <ItemLink id="autarkia:coolant_cell" /> | <ItemLink id="autarkia:fabricator" /> | 1,000 ticks (50 s) | 2,000 FE/t | 1 Disassembler Beam |

## Use it

1. Charge the beam. Put it into the item slot of a <ItemLink id="autarkia:battery_bank" /> (2,048 FE/t) or a
   <ItemLink id="autarkia:capacitor_bank" /> (512 FE/t).
2. Hold the beam in your main hand.
3. Look at the blocks you want to remove.
4. Hold use. The beam fires for up to 100 ticks (5 s). Each tick costs 2,000 FE.
5. Let go of use to stop.
6. After 100 ticks the beam overheats. It stops and cools down for 60 ticks (3 s).

To link it, sneak and use it on a <ItemLink id="autarkia:matter_buffer" />. The link works like the link of the
<ItemLink id="autarkia:nanite_multitool" />: drops with a matter value go into the cache for 20 FE per item and flow
to the buffer. Without a link the drops fall as items.

## What the beam does not take apart

The beam stops at such a block and leaves it as it is:

- Blocks with a block entity (chests, machines and similar)
- Bastion blocks and Bastion glass
- Blocks with a blast resistance above 50 (config `weapons.disassemblerMaxResistance`)
- Unbreakable blocks
- Blocks in the domain of another colony where you are not the owner or trusted
- Spawn-protected blocks, and blocks that a protection mod keeps

The drops come as if a netherite tool of the right type broke the block.

## Numbers

| Property | Value |
|---|---|
| Range | 24 blocks |
| Blocks per tick | 2 at most |
| Energy per beam tick | 2,000 FE |
| Longest burst | 100 ticks (5 s) |
| Overheat cooldown | 60 ticks (3 s) |
| Damage | 6 beam damage to the first valid creature, at most once per 10 ticks per target |
| Energy capacity | 20,000,000 FE |
| Charge rate | Up to 20,000 FE/t |
| Matter cache | 4,096 MU, 20 FE per item |
| Stack size | 1 |

## Problems

- The beam does not fire: the beam is in your off hand, it has less than 2,000 FE, or it is cooling down.
- The beam stops at a block: the block is on the list above.

## Next

- In Tier 6 the <ItemLink id="autarkia:singularity_lance" /> uses the beam in its recipe.
