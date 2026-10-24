---
navigation:
  title: Zinc, Brass and Steel
  parent: t2/index.md
  position: 4
  icon: autarkia:steel_ingot
---

# Zinc, Brass and Steel

Tier 2 adds a new metal, zinc, and two alloys, brass and steel. It also adds a longer ore chain that gives up to two
ingots for each raw ore.

- <ItemLink id="autarkia:zinc_ore" /> lies in the Overworld from Y 0 to 64. It drops <ItemLink id="autarkia:raw_zinc" />.
- <ItemLink id="autarkia:brass_ingot" />: 3 copper ingots and 1 zinc ingot make 4 brass ingots in the
  <ItemLink id="autarkia:crucible" /> or the <ItemLink id="autarkia:blast_smelter" />.
- <ItemLink id="autarkia:steel_ingot" />: 1 iron ingot and 2 coal make 1 steel ingot in the blast smelter.
- The <ItemLink id="autarkia:plate_press" /> makes <ItemLink id="autarkia:brass_plate" /> and
  <ItemLink id="autarkia:steel_plate" />. Steel plates make the <ItemLink id="autarkia:steel_gear" />.
- The <ItemLink id="autarkia:wire_drawer" /> makes the <ItemLink id="autarkia:copper_coil" />.

## The ore chain

The chain works for iron, copper, gold, tin and zinc. The numbers are for one raw ore.

| Step | Machine | Result |
|---|---|---|
| 1. Smelt raw ore | Furnace | 1 ingot |
| 2. Crush raw ore | <ItemLink id="autarkia:quern" /> or <ItemLink id="autarkia:stamp_mill" /> | 1 crushed ore, plus 1 more at 50% (1.5 on average) |
| 3. Wash crushed ore | <ItemLink id="autarkia:sluice" /> | 1 washed ore for each crushed ore, plus a nugget of a second metal at 10% |
| 4. Smelt washed ore | <ItemLink id="autarkia:blast_smelter" /> | 4 ingots from 3 washed ore, plus slag at 30% |

- Raw ore in a furnace gives 1 ingot.
- Crushed ore or washed ore in a furnace also gives 1 ingot each: 1.5 ingots for each raw ore.
- The full chain (crush, wash, blast smelter) gives 2 ingots for each raw ore on average.

| Washed ore | Second metal from the sluice |
|---|---|
| <ItemLink id="autarkia:washed_iron" /> | <ItemLink id="autarkia:zinc_nugget" /> |
| <ItemLink id="autarkia:washed_copper" /> | Gold nugget |
| <ItemLink id="autarkia:washed_gold" /> | Copper nugget |
| <ItemLink id="autarkia:washed_tin" /> | Iron nugget |
| <ItemLink id="autarkia:washed_zinc" /> | Copper nugget |

<ItemLink id="autarkia:slag" /> is the waste of the blast smelter. 4 slag make 2 <ItemLink id="autarkia:foundation" />.

## Storage

Nine ingots make a storage block, and nine nuggets make an ingot: <ItemLink id="autarkia:zinc_block" />,
<ItemLink id="autarkia:brass_block" />, <ItemLink id="autarkia:steel_block" />,
<ItemLink id="autarkia:zinc_nugget" /> and <ItemLink id="autarkia:steel_nugget" />. Nine raw zinc make a
<ItemLink id="autarkia:raw_zinc_block" />.

<SubPages />
