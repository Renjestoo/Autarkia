---
navigation:
  title: The Colony
  parent: t1/index.md
  position: 5
  icon: autarkia:core_seed
---

# The Colony

The colony is the heart of Autarkia. It is a Colony Core that watches its land, finds what it needs and sends workers
to build it. You give it items and decisions. It does the building.

## The colony loop

1. Plant a <ItemLink id="autarkia:core_seed" />. After 10 seconds the Colony Core blooms. It claims a 33 x 33 domain
   and gives you a Porter in a <ItemLink id="autarkia:worker_capsule" /> and a <ItemLink id="autarkia:field_manual" />.
2. Build machines, generators, rope spans and <ItemLink id="autarkia:crate" /> blocks inside the domain.
3. Every 5 seconds the Core reads its sensors: power supply and demand, stored power, span load, storage fill and the
   charge of the workers.
4. When a need stays for some time, the colony finds a site and proposes a project. For example, power that is 25%
   short for 60 seconds gives a Windmill proposal.
5. Read the proposal at a <ItemLink id="autarkia:project_board" /> or with a
   <ItemLink id="autarkia:planning_tablet" />. Approve or deny it. Or let the autonomy setting approve it.
6. Put the items of the bill of materials into crates. The project waits until the crates hold every item.
7. The colony splits the project into jobs. The Porters level the site, place the blocks, string the rope and test
   the build.
8. The project is done and the need is gone. After some projects that the colony approved itself, a worker puts up a
   sign that says what the colony fixed.

## Autonomy

| Level | What happens |
|---|---|
| Manual | The colony builds nothing until you approve it. |
| Assisted | The colony approves a proposal when it is 1 day old. A new colony starts here. |
| Autonomous | The colony approves each proposal at once. |

Projects that the colony approves use its daily budget. At Tier 1 the budget is 64 points each day, and only 1 project
builds at a time.

## Who controls the colony

The owner and the trusted players can approve projects, cancel jobs, recall workers and change settings. Only the
owner renames the colony and trusts other players. Trust players in the Settings tab of the Core.

<SubPages />
