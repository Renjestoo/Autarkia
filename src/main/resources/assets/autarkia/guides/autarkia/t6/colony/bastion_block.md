---
navigation:
  title: Bastion Block
  parent: t6/colony.md
  position: 50
  icon: autarkia:bastion_block
item_ids:
  - autarkia:bastion_block
---

# Bastion Block

<ItemImage id="autarkia:bastion_block" scale="2" />

The Bastion block is the tier 6 wall block. While your domain grid is powered, it cannot break: not by players, not by
explosions, not by a Wither. Without that power it is as hard as obsidian.

## Get it

<RecipeFor id="autarkia:bastion_block" />

8 <ItemLink id="autarkia:lattice_block" /> around 1 <ItemLink id="autarkia:aegis_plate" /> give 8 Bastion blocks.

## Use it

1. Build your walls from Bastion blocks inside your colony domain.
2. Ignite a <ItemLink id="autarkia:singularity_core" /> on a network with a node in the domain.
3. Keep at least 1,000,000 FE stored on that grid. The core buffer and the storage blocks of the grid count.
4. Look at a block with Jade. "Protected" means the block cannot break.

The block uses no FE per hit. It only needs the stored FE.

## Powered and unpowered

A Bastion block is powered when all of these are true:

- It lies in the domain of a colony.
- That colony's grid has an ONLINE Singularity Core.
- That grid stores at least 1,000,000 FE.

Only the grid of its own domain powers a Bastion block.

| Property | Powered | Unpowered |
|---|---|---|
| Break by a survival player | no progress | like obsidian |
| Blast resistance | 3,600,000 | 1,200 |
| Wither, Ender Dragon, other mobs | cannot break it | normal Minecraft rules |
| Pistons | cannot move it | cannot move it |
| Hardness | 50 | 50 |

A player in creative mode can always break it. A disassembler beam never takes it apart.

## Numbers

| Property | Value |
|---|---|
| Power condition | ONLINE Singularity Core and 1,000,000 FE stored |
| Hardness | 50 |
| Blast resistance | 3,600,000 powered, 1,200 unpowered |
| Mining tool | Diamond pickaxe or better |

## Jade

- "Protected (grid N FE)": the block is powered. N is the stored FE of the grid.
- "Unprotected": the block acts like obsidian.

## Problems

- "Unprotected": the block is outside your domain, no Singularity Core is ONLINE, or the grid holds less than
  1,000,000 FE.
- You cannot mine your own wall: mine it in creative mode, or stop the Singularity Core first.

## Next

- Use <ItemLink id="autarkia:bastion_glass" /> for windows with the same protection.
- A Wither that fails against powered Bastion walls gives the advancement "Unbroken Walls".
