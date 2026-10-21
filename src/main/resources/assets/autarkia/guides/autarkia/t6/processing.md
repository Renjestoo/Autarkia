---
navigation:
  title: The Matter Compiler
  parent: t6/index.md
  position: 2
  icon: autarkia:matter_compiler
item_ids:
  - autarkia:matter_compiler
  - autarkia:aegis_plate
---

# The Matter Compiler

The <ItemLink id="autarkia:matter_compiler" /> is 2 x 2 x 2 and uses 16,384 FE/t. It writes items from matter units.

- Its internal buffer holds 65,536 MU.
- The feed slot turns an item into MU. It takes only items with a matter value. Raw ore in the feed slot always counts
  as 8 x its ingot value.
- After the feed, it pulls free MU from the Matter Buffers of the domain, the fullest buffer first.
- Pick the target in the ghost slot. The time per item is the MU cost / 32 ticks, at least 20 ticks.

It compiles 21 items: every ingot and nugget of the matter table (for example iron, gold, vantite steel or netherite),
the lattice crystal, the lattice alloy ingot and the <ItemLink id="autarkia:aegis_plate" />. Raw ores and ore blocks
are never results, so no matter loop exists. Alloys have the matter value of their inputs, so alloying gives no free
matter.

## The 8x ore line

Put raw ore into the feed slot: it gives 8 x its ingot value. Compile the ingots back: 8 ingots per raw ore, with no
byproducts.

## Aegis plate

The <ItemLink id="autarkia:aegis_plate" /> costs 32,768 MU and comes only from the matter compiler. It goes into the
Aegis gear, the tier 6 machines, Bastion blocks, the matter conduit and the Core upgrade to tier 6.

<RecipeFor id="autarkia:matter_compiler" />
