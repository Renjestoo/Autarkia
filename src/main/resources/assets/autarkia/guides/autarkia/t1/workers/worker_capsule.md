---
navigation:
  title: Worker Capsule
  parent: t1/workers.md
  position: 10
  icon: autarkia:worker_capsule
item_ids:
  - autarkia:worker_capsule
---

# Worker Capsule

<ItemImage id="autarkia:worker_capsule" scale="2" />

The worker capsule holds one worker. Use it to carry a worker to a new place, or to keep a worker out of the colony for
some time. Your first Porter comes in a capsule when the Colony Core blooms.

## Get it

- The Colony Core gives you one capsule with a Porter in it when it blooms for the first time.
- You can craft an empty capsule:

<RecipeFor id="autarkia:worker_capsule" />

You need 4 <ItemLink id="autarkia:bronze_ingot" />, 4 glass and 1 <ItemLink id="autarkia:mainspring" />.

## Release a worker

1. Hold a capsule with a worker in it. The tooltip shows the worker and its spring charge.
2. Stand inside your colony's domain.
3. Point at the top face of a block.
4. Press use. The worker unfolds on that block and joins the colony.

## Pick up a worker

1. Hold an empty capsule.
2. Make sure that the worker is idle, returning, recharging or failed. A worker that does a job cannot go in.
3. Sneak and use the capsule on the worker.
4. The worker goes into the capsule with its spring, health, cargo, name and upgrades.

A worker in a capsule does not count against the worker cap. Use this to make room for a different worker.

## Numbers

| Property | Value |
|---|---|
| Stack size | 1 |
| Workers in one capsule | 1 |
| Worker cap at Tier 1 | 4 Porters |

## Problems

- "Outside your domain": you are not inside the domain of your colony, or the colony is not yours. Go into your
  domain.
- "Worker cap full": the colony has no room for this worker. Pick up another worker first, or upgrade the Core.
- "The worker must be idle, recharging or failed": the worker does a job now. Use Recall in the Workers tab of the
  Core, then try again.
- "This worker belongs to another colony": you are not the owner or a trusted player of its colony.
- "This worker cannot be captured": the Replicator Walker of Tier 5 cannot go into a capsule.

## Next

- Wind your Porter at a <ItemLink id="autarkia:winding_post" />.
- Make more Porters in a <ItemLink id="autarkia:worker_assembler" />.
