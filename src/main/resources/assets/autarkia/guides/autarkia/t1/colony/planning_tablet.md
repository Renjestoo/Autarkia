---
navigation:
  title: Planning Tablet
  parent: t1/colony.md
  position: 40
  icon: autarkia:planning_tablet
item_ids:
  - autarkia:planning_tablet
---

# Planning Tablet

<ItemImage id="autarkia:planning_tablet" scale="2" />

The planning tablet is a project board that you carry. It shows the colony's planned builds as ghost blocks around
you. It also has the autonomy switch.

## Get it

<RecipeFor id="autarkia:planning_tablet" />

You need 7 <ItemLink id="autarkia:bronze_ingot" />, 1 glass pane and 1 redstone dust.

## Use it

1. Stand inside the domain of your colony. You must be the owner or a trusted player.
2. Press use with the tablet in your hand. This links the tablet to your colony.
3. The Projects tab opens. It is the same as at a <ItemLink id="autarkia:project_board" />.
4. Use the three buttons at the bottom to set the autonomy: Manual, Assisted or Autonomous.
5. Close the screen. Keep the tablet in your main hand or off hand.
6. Walk inside the domain and look around. The blocks that the open projects still need show as ghost blocks.
   Planned rope spans show as glowing lines.

| Ghost color | Project state |
|---|---|
| Cyan | Proposed |
| Green | Approved, waiting for items or building |
| Red | Blocked |
Press use again from anywhere to open the Projects tab. The tablet stays linked. Ghost blocks show only while you are
inside the domain.

## Autonomy

| Level | What happens |
|---|---|
| Manual | The colony builds nothing until you approve it. |
| Assisted | The colony approves a proposal when it is 1 day (24,000 ticks) old. A new colony starts here. |
| Autonomous | The colony approves each proposal at once. |

Projects that the colony approves use its daily budget (64 points each day at Tier 1). Projects that you approve use
no budget. See <ItemLink id="autarkia:project_board" /> for the full project steps.

## Numbers

| Property | Value |
|---|---|
| Stack size | 1 |
| Ghost refresh | every 40 ticks (2 seconds) while you hold the tablet |
| Most ghost blocks for one project | 2,048, nearest first (server option `colony.ghostMaxBlocks`) |

## Problems

- "Use the tablet on a Colony Core to link it": the tablet has no link, or the colony is not active. Stand inside your
  own domain and press use. The colony must have bloomed.
- The screen closes: you took the tablet out of your hands. Hold it while the screen is open.
- No buttons show: you are not the owner or a trusted player of this colony.

## Next

- Approve the first projects and watch your Porters build them. See [Porters](../workers.md).
- Place <ItemLink id="autarkia:crate" /> blocks and fill them with the items in the bills of materials.
