---
navigation:
  title: Ore Processing Machines
  parent: t2/index.md
  position: 4
  icon: autarkia:blast_smelter
---

# Ore Processing Machines

Tier 2 brings four machines that turn ore and ingots into better materials. Three of them need power. The sluice
needs water.

- The <ItemLink id="autarkia:plate_press" /> presses brass and steel ingots into plates (48 FE/t).
- The <ItemLink id="autarkia:wire_drawer" /> draws copper into copper coils and copper wire spools (40 FE/t).
- The <ItemLink id="autarkia:sluice" /> washes crushed ore into washed ore with water. It needs no power.
- The <ItemLink id="autarkia:blast_smelter" /> is a heavy machine on foundation (64 FE/t). It makes 4 ingots from 3
  washed ore, steel from iron and coal, and brass from copper and zinc.

## How they work together

1. Make brass ingots in the <ItemLink id="autarkia:crucible" />: 3 copper ingots and 1 zinc ingot.
2. Build the plate press and the wire drawer. They need brass ingots and a bronze gear.
3. Press brass plates and draw copper coils. They make the sluice and the blast smelter.
4. Crush raw ore in a <ItemLink id="autarkia:quern" /> or a <ItemLink id="autarkia:stamp_mill" />.
5. Wash the crushed ore in the sluice.
6. Smelt the washed ore in the blast smelter.
7. Make steel in the blast smelter, then steel plates in the plate press.

Each machine shows its state in Jade: "Running", "Idle", "No power", "Output full" or "Condition missing".
Hoppers and belts can fill the input slots and empty the output slots of all four machines.

Read [Zinc, Brass and Steel](materials.md) for the materials and the full ore chain.

<SubPages />
