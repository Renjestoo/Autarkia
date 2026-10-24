---
navigation:
  title: Matter Compiler
  parent: t6/processing.md
  position: 10
  icon: autarkia:matter_compiler
item_ids:
  - autarkia:matter_compiler
---

# Matter Compiler

<ItemImage id="autarkia:matter_compiler" scale="2" />

The matter compiler writes items from MU (matter units). You choose a target item. The compiler takes MU from its feed
slot or from the Matter Buffers of your domain and makes the target. It is the only source of the
<ItemLink id="autarkia:aegis_plate" />.

## Get it

<RecipeFor id="autarkia:matter_compiler" />

You can make it while your colony is still at tier 5. You need it for the Core Upgrade to tier 6.

## Use it

1. Put 4 <ItemLink id="autarkia:foundation" /> blocks in a 2 x 2 square. Place the compiler on them. It is 2 x 2 x 2.
2. Connect it to your network. It uses 16,384 FE/t while it works.
3. Open it. Click the target slot with an item on the cursor, for example an iron ingot. The compiler now makes that
   item.
4. Give it MU. Put items with a matter value in the feed slot, or keep free MU in the
   <ItemLink id="autarkia:matter_buffer" /> blocks of your domain.
5. Take the results out of the 4 output slots.

## The screen

- Feed slot (left): items with a matter value. The compiler changes them into MU in its internal buffer.
- MU bar (far left): the internal buffer, up to 65,536 MU. A yellow mark shows the MU of one target item.
- Target slot (top middle): the item to make. Click it with an empty hand for the next target. Right-click it for the
  previous target. The mouse wheel also goes through the targets.
- "x" button: clears the target.
- 4 output slots (right).
- Energy bar.

Hold the mouse over the target slot. It shows "Target: name (N MU, N s)".

## How it gets MU

- The feed slot takes only items with a matter value. Raw ore in the feed slot always counts as 8 times its ingot. One
  raw iron gives 288 MU: enough for 8 iron ingots.
- When the buffer has too little MU, the compiler takes free MU from the Matter Buffers of its domain, the fullest
  buffer first. It does this only when the domain has enough free MU for the missing part.
- The domain archive lists these withdrawals with the reason "Compiler".

## Targets

The compiler makes 21 items:

- The ingots of copper, tin, zinc, bronze, brass, iron, gold, steel, vantite, vantite steel and netherite.
- The nuggets of copper, tin, zinc, bronze, iron, gold and steel.
- The <ItemLink id="autarkia:lattice_crystal" />, the <ItemLink id="autarkia:lattice_alloy_ingot" /> and the aegis
  plate.

Some examples:

| Target | MU | Time | FE |
|---|---|---|---|
| Iron ingot | 36 | 20 ticks | 327,680 |
| Lattice crystal | 864 | 27 ticks | 442,368 |
| Lattice alloy ingot | 1,188 | 38 ticks | 622,592 |
| Netherite ingot | 4,096 | 128 ticks | 2,097,152 |
| Aegis plate | 32,768 | 1,024 ticks (51.2 s) | 16,777,216 |

Time = MU / 32 ticks (rounded up), but at least 20 ticks. FE = 16,384 FE/t x time.

## Numbers

| Property | Value |
|---|---|
| Size | 2 x 2 x 2, on foundation |
| Power | 16,384 FE/t while it works |
| Internal buffer | 65,536 MU |
| Speed | 32 MU per tick, at least 20 ticks per item |
| Output slots | 4 |

## Jade

- "Matter: N / 65536 MU": the internal buffer.
- "Target: name (N MU)" or "Target: none".
- "Needs a foundation under every bottom block": a block under the compiler is not foundation.

## Problems

- "Condition missing": the compiler is not on foundation, or it has too little MU. Feed it, or fill your Matter
  Buffers.
- "No power": the compiler has less than 16,384 FE.
- "Output full": empty the output slots.
- "Idle": choose a target.
- An item does not go into the feed slot: it has no matter value.

## Next

- Make the <ItemLink id="autarkia:aegis_plate" />. You need 2 of them for the
  <ItemLink id="autarkia:core_upgrade_t6" />.
