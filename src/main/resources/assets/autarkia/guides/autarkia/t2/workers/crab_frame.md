---
navigation:
  title: Crab Frame
  parent: t2/workers.md
  position: 10
  icon: autarkia:crab_frame
item_ids:
  - autarkia:crab_frame
---

# Crab Frame

<ItemImage id="autarkia:crab_frame" scale="2" />

The crab frame is the body of a Hauler Crab. In Tier 3 it is also the body of the Loader. You put the frame and the
parts into the <ItemLink id="autarkia:worker_assembler" />, and the assembler builds the worker.

## Get it

<RecipeFor id="autarkia:crab_frame" />

## Use it

1. Upgrade your colony to Tier 2 with the <ItemLink id="autarkia:core_upgrade_t2" />.
2. Place a <ItemLink id="autarkia:worker_assembler" /> inside the domain and give it power.
3. Put the crab frame and the parts of the Hauler Crab into the assembler.
4. Wait 900 ticks (45 seconds). The Hauler Crab comes out and joins the colony.

The Hauler Crab needs these parts:

| Input | Machine | Time | Power | Output |
|---|---|---|---|---|
| 1 crab frame, 3 <ItemLink id="autarkia:mainspring" />, 4 <ItemLink id="autarkia:steel_gear" />, 2 <ItemLink id="autarkia:copper_coil" />, 4 <ItemLink id="autarkia:brass_plate" /> | <ItemLink id="autarkia:worker_assembler" /> | 900 ticks | 40 FE/t | 1 Hauler Crab |

The colony must be Tier 2 or higher.

## What the Hauler Crab does

- It fetches, carries and delivers items for the colony.
- Every 5 seconds, when debris lies in the domain, the colony gives a crab a job to collect it. Debris is snapped cable
  ends, falling crates and items that spilled from them. The crab collects the debris and puts it into the colony
  storage.
- It places no blocks.

Look at a crab with Jade to see its job, for example "Collect debris", and its spring, for example
"Spring 9000 / 12000 T".

## Numbers

| Property | Value |
|---|---|
| Spring | 12,000 T |
| Spring use | 1 T/s idle, 24 T/s moving, 12 T/s more while it carries |
| Collecting one piece of debris | 30 T |
| Cargo | 4 stacks |
| Health | 30 |
| Armor | 4 |
| Repair | use a <ItemLink id="autarkia:brass_plate" /> on it: 6 health |
| Cap cost | 1 |
| Most crabs | 4 at Tier 2 |

## Problems

- Jade on the assembler shows "Core tier too low": the colony is still Tier 1.
- "Worker cap full": the colony has as many workers as its cap allows.
- "Waiting for parts": a part is missing in the assembler.
- "No power": the assembler gets no FE.
- "Outside a colony domain": move the assembler into the domain.
- The crab stops: its spring is empty. Put a <ItemLink id="autarkia:winding_post" /> near its work, or use the crab
  with an empty hand to wind it by 100 T.

## Next

- In Tier 3, the same frame builds the Loader for the warehouse.
