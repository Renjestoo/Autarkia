---
navigation:
  title: Lightning
  parent: t2/index.md
  position: 3
  icon: autarkia:lightning_mast
item_ids:
  - autarkia:lightning_mast
  - autarkia:lightning_arrester
---

# Lightning

Metal wire attracts lightning. A bolt within 16 blocks goes to the highest pole and sends a white pulse along the
wires at 2 blocks per tick. Each span it passes gets hot. The first machine it reaches blows up and drops itself.

## Arrester

A <ItemLink id="autarkia:lightning_arrester" /> on top of a pole stops the pulse at that pole. Next to a machine it
protects that machine.

<RecipeFor id="autarkia:lightning_arrester" />

## Lightning mast

A <ItemLink id="autarkia:lightning_mast" /> pulls every bolt within 48 blocks to itself. It needs open sky. Each strike
puts 120,000 FE into the capacitor banks of its network, split equally. In a thunderstorm it can also call strikes
by itself.

<GameScene zoom="3" interactive={true}>
  <ImportStructure src="/structures/lightning_mast.nbt" />
  <Block id="minecraft:light" x="0" y="4" z="0" />
</GameScene>
