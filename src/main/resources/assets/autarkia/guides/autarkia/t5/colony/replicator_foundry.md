---
navigation:
  title: Replicator Foundry
  parent: t5/colony.md
  position: 40
  icon: autarkia:replicator_foundry
item_ids:
  - autarkia:replicator_foundry
---

# Replicator Foundry

<ItemImage id="autarkia:replicator_foundry" scale="2" />

The Replicator Foundry prints Replicator Walkers for your colony. A walker carries an outpost kit to a free ore
deposit, digs in and becomes a new outpost. You see a base appear that you did not build.

## Get it

<RecipeFor id="autarkia:replicator_foundry" />

## Use it

1. Upgrade your colony to tier 5.
2. Place the foundry in your colony domain. It is 3 x 3 x 3 blocks.
3. Connect a span from your network to the foundry. A print takes 2,000 FE/t for 2,400 ticks.
4. Keep the walker parts in colony storage, and free matter in your <ItemLink id="autarkia:matter_buffer" /> blocks.
5. The colony proposes a print when all print rules are true. In Manual autonomy, approve the proposal.
6. Workers bring the missing parts into the foundry.
7. The foundry takes the matter for the outpost kit and prints the walker.
8. The walker steps out of the front with a full battery and walks to its deposit.

The foundry prints only on a colony order. You cannot start a print by hand.

## Walker recipe

The foundry runs this recipe. It does not show in this book.

| Slot | Input |
|---|---|
| Frame slot | 2 <ItemLink id="autarkia:walker_frame" /> |
| Part slots | 1 <ItemLink id="autarkia:ai_core" />, 8 <ItemLink id="autarkia:nanite_paste" />, 16 <ItemLink id="autarkia:lattice_alloy_ingot" />, 4 <ItemLink id="autarkia:advanced_circuit" />, 8 <ItemLink id="autarkia:servo" />, 1 <ItemLink id="autarkia:outpost_seed" /> |
| Matter | The outpost kit, priced in MU, taken from the matter buffers of the domain |
| Time and energy | 2,400 ticks (2 minutes) at 2,000 FE/t |

The frame slot takes only walker frames. Each of the 6 part slots keeps one kind of part.

## Print rules

All of these must be true:

- The colony is tier 5 or higher and active, and the foundry has power.
- Fewer than 2 walkers are active (config `replicator.maxPerDomain`).
- Outposts plus active walkers are fewer than 6.
- A known ore deposit is free: outside every domain, 64 to 512 blocks from the Core, and 64 blocks or more from every
  outpost.
- The walker parts are in storage, and the matter buffers hold the matter for the kit.
- At least 24,000 colony ticks (one colony day) have passed since the last print.

## Numbers

| Property | Value |
|---|---|
| Size | 3 x 3 x 3 |
| Energy buffer | 200,000 FE |
| Energy input | Up to 8,000 FE/t |
| Print | 2,400 ticks at 2,000 FE/t |
| Walkers at a time | 2 |
| Slots | 1 frame slot, 6 part slots |

## Problems

| Jade line | Meaning and what to do |
|---|---|
| "No print ordered" | The colony has not ordered a print. Check the print rules. |
| "Waiting for parts" | Parts are missing in the slots. Workers bring them. |
| "Waiting for kit matter" | The matter buffers hold too little free matter, or no deposit is free. |
| "Printing" | The print runs. |
| "No power" | The buffer has less than 2,000 FE. The print pauses. |
| "Walker cap full" | No room in the worker cap or in the walker limit. |
| "Needs a tier 5 colony" | Upgrade the colony. |
| "Outside a colony domain" | Move the foundry into your domain. |
| "Replication is switched off" | The config `replicator.enabled` is off. |

Jade also shows "Walkers N/2", "Print N%" and the energy buffer.

## Next

- Read about the Replicator Walker on the [workers page](../workers.md).
- The advancements "Not Built By You" and "A Base You Did Not Build" come when a walker founds an outpost.
