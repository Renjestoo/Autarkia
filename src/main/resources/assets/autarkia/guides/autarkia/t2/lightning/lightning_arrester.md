---
navigation:
  title: Lightning Arrester
  parent: t2/lightning.md
  position: 10
  icon: autarkia:lightning_arrester
item_ids:
  - autarkia:lightning_arrester
---

# Lightning Arrester

<ItemImage id="autarkia:lightning_arrester" scale="2" />

The lightning arrester is a copper spike on a ceramic base. It protects your network from lightning pulses. It is cheap,
so put one on every pole of a line in the open.

## Get it

<RecipeFor id="autarkia:lightning_arrester" />

## Use it

There are two ways to use it:

1. Place it on the block directly above the top of a <ItemLink id="autarkia:wooden_pole" />. A lightning pulse that
   reaches this pole stops there. The pulse does not go on along the spans of this pole.
2. Place it directly next to a machine. The pulse does not destroy this machine. The pulse goes on to the next node.
   For a big machine, put the arrester next to the block where you placed the machine. A block directly above that
   block also works.

An arrester on the top of a machine that has spans also stops the pulse at that machine.

## Numbers

| Property | Value |
|---|---|
| On top of a node | stops the pulse at that node |
| Next to a machine | protects that machine; the pulse goes on |

## Problems

- A machine blew up in a storm: the pulse reached it before an arrester. Put arresters on the poles between the struck
  pole and the machine, or one next to the machine.
- The pulse went around the arrester: it runs along every span of the network. An arrester stops only the branch at its
  own pole. Protect each branch.

## What the colony does

After a lightning surge destroyed a machine, the colony proposes a Lightning Arresters project. It places an arrester
on top of every pole of that network in the domain.

## Next

- Catch the bolts before they reach your wires with a <ItemLink id="autarkia:lightning_mast" />.
