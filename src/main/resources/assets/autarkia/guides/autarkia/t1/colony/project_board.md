---
navigation:
  title: Project Board
  parent: t1/colony.md
  position: 30
  icon: autarkia:project_board
item_ids:
  - autarkia:project_board
---

# Project Board

<ItemImage id="autarkia:project_board" scale="2" />

The project board is a cork board for your base. It opens the Projects tab of your colony. Here you see what the
colony wants to build and why. Here you approve or deny each project.

## Get it

<RecipeFor id="autarkia:project_board" />

You need 7 planks, 1 book and 1 stick.

## Use it

1. Place the board inside your colony's domain.
2. Right-click the board. The Projects tab opens.
3. Click a project in the list on the left. Its card shows on the right.
4. Click Show to see the project as ghost blocks in the world for 30 seconds. Cyan ghosts are proposed, green ghosts
   are approved or building, red ghosts are blocked.
5. Click Approve to build the project, or Deny to refuse it.
6. If you have the Jade mod, look at the board. Jade shows the number of proposals and the top proposal.

## How a project starts

The colony reads its sensors every 5 seconds. When a need stays true for some time, the colony looks for a site and
proposes a project. Tier 1 knows these projects:

| Project | The colony proposes it when |
|---|---|
| Windmill | Power is 25% or more short for 60 seconds |
| Treadwheel | Power is 25% or more short for 60 seconds, and the windmill has no good site or half of the Porters are idle |
| Crate Cluster | The storage is 85% or more full for 30 seconds |
| Flywheel | Stored power lasts less than 30 seconds and supply is more than 1.2 times demand, for 60 seconds |
| Parallel Span | A rope span is at 90% or more of its load for 30 seconds |
| Winding Post | A worker is below 20% charge for 60 seconds, or the workers work 24 blocks or more (on average) from a charger |

"Power short" compares the supply with the demand of the machines in the domain. "Storage" means the crates in the
domain.

## The project card

| Line | Meaning |
|---|---|
| Name and state | The project type and its state, for example Proposed or Building |
| Reason | The sensor value that started the proposal, for example "Power 30% short" |
| Site | The position, the rotation, the site score and the number of ground edits |
| Cost | The budget points of the project and the budget that is left today |
| Priority | The rank of the project. The +10 and -10 buttons change it, up to 50 each way. |
| Bill of materials | Each item with the count needed, in stock and reserved. Green lines are complete. |
| Labor | The open jobs of the project, counted by job type |

## Buttons

| Button | When | What it does |
|---|---|---|
| Show | Always | Shows the ghost blocks for 30 seconds |
| Approve | Proposed | Approves the project |
| Deny | Proposed | Refuses the project. The colony proposes this type again after 72,000 ticks (3 days), and not in the same chunk for 7 days. |
| Rotate | Proposed | Turns the site 90 degrees, if the new rotation fits |
| Retry | Blocked | Opens the blocked jobs again |
| Cancel | Approved and later | Stops the project. Open jobs go, held items go back to storage, placed blocks stay. |
| +10, -10 | Always | Changes the priority |

Only the owner and trusted players can use the buttons. Other players can look and use Show.

## How a project is built

1. The project is approved, by you or by the autonomy setting.
2. The project waits until the crates hold every item of the bill of materials. The colony then reserves all items.
3. At Tier 1 only 1 project builds at a time. The others wait.
4. Workers level the site. They cut blocks first, then fill holes with dirt, stone or cobblestone from the crates.
5. Workers place the blocks of the plan.
6. Workers string the rope spans.
7. The colony tests the build. A Porter turns a new treadwheel. A new windmill or flywheel must run within 200 ticks.
8. A worker puts a sign next to an auto-approved Windmill, Treadwheel, Crate Cluster or Parallel Span. The sign shows
   the reason, "Fixed." and the colony day. The worker needs a sign in the crates. If there is none, it skips the sign.
9. The project is Done.

## Autonomy

The autonomy level decides who approves the proposals. Set it in the Settings tab of the Core or with a
<ItemLink id="autarkia:planning_tablet" />. A new colony starts at Assisted.

| Level | What happens |
|---|---|
| Manual | The colony builds nothing until you approve it. |
| Assisted | The colony approves a proposal when it is 1 day (24,000 ticks) old. |
| Autonomous | The colony approves each proposal at once. |

A project that the colony approves uses the daily budget when it starts. At Tier 1 the budget is 64 points each day.
Each new day adds 64 points to the points left, but the colony keeps at most 32 old points. A project that you approve
uses no budget.

## Jobs

The colony splits each project into jobs: cut, fill, place, string cable, crank and sign. Idle workers take the jobs.
The Jobs tab of the Core shows each open job. A failed job waits and tries again. After 3 failed tries the job is
blocked and the project goes to Blocked. Fix the cause, then click Retry.

## Problems

- "Place it inside your colony's domain": the board is outside the domain. Move it into the 33 x 33 square.
- "No projects yet. The colony proposes work when its sensors see a need.": the colony has no need now. This is good.
- "Project windmill blocked: no_path", or another project name and reason: a job failed 3 times. Read the reason.
  Clear the way for the workers, then click Retry.
- A project stays at "Waiting items": the crates do not hold the full bill of materials. Put the missing items into a
  crate.
- A proposal goes to "Expired": its need was gone for 12,000 ticks (10 minutes), or its site is not free now, or more
  than 8 proposals were open and it had the lowest priority. The colony searches again when the need comes back.
- "Only the owner or trusted players can do this": ask the owner to trust you in the Settings tab.

## Next

- Make a <ItemLink id="autarkia:planning_tablet" /> to see the projects from anywhere.
- Build more <ItemLink id="autarkia:crate" /> blocks. Projects take their items from crates.
- Make more Porters. See [Porters](../workers.md).
