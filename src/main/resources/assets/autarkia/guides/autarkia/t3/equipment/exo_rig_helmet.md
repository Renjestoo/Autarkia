---
navigation:
  title: Exo Rig Helmet
  parent: t3/equipment.md
  position: 50
  icon: autarkia:exo_rig_helmet
item_ids:
  - autarkia:exo_rig_helmet
---

# Exo Rig Helmet

<ItemImage id="autarkia:exo_rig_helmet" scale="2" />

The exo rig helmet is the head part of the Tier 3 power suit. It is the power bus of the suit: it shares the FE between all worn exo rig parts. It also gives you more air under water.

## Get it

| Input | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 1 <ItemLink id="autarkia:steel_helmet" />, 4 <ItemLink id="autarkia:steel_plate" />, 1 <ItemLink id="autarkia:basic_circuit" />, 1 glass pane, 1 <ItemLink id="autarkia:capacitor_bank" /> | <ItemLink id="autarkia:fabricator" /> | 200 ticks | 100 FE/t | 1 exo rig helmet (empty) |

## Use it

1. Charge the helmet in a <ItemLink id="autarkia:battery_bank" /> or a <ItemLink id="autarkia:capacitor_bank" />.
2. Wear it.
3. Every second, the helmet shares the FE between all worn suit parts. A part with more FE gives to a part with less FE.
4. Push the suit key (default N) to switch all worn suit parts on or off.

The functions work while the suit is on and the part has FE. The tooltip shows "Suit: on" or "Suit: off".

## Numbers

| Property | Value |
|---|---|
| Armor | 3 |
| Toughness | 2 |
| Durability | 363 |
| Capacity | 500,000 FE |
| Charge rate | up to 2,000 FE/t |
| Oxygen | +1 oxygen bonus |
| FE sharing | every second, between all worn suit parts |
| Repair | steel plate in an anvil |

## Problems

- "No suit pieces worn": you pushed the suit key without a suit part on.
- "Suit off": the suit key switched the parts off. Push it again for "Suit on".
- The parts do not share FE: the helmet itself has 0 FE or the suit is off.

## Next

- <ItemLink id="autarkia:exo_rig_chest" />, <ItemLink id="autarkia:exo_rig_legs" /> and <ItemLink id="autarkia:exo_rig_boots" /> are the other parts.
- <ItemLink id="autarkia:frontier_suit_helmet" /> is the Tier 4 upgrade. Its recipe uses this helmet and keeps its FE.
