---
navigation:
  title: Outposts
  parent: t4/index.md
  position: 5
  icon: autarkia:outpost_seed
item_ids:
  - autarkia:core_upgrade_t4
  - autarkia:outpost_seed
---

# Outposts

<ItemLink id="autarkia:core_upgrade_t4" /> raises the Colony Core to tier 4. The fabricator makes it from 4 advanced
circuits, 8 vantite steel ingots and 4 servos in 200 ticks (32,000 FE).

| Tier 4 value | |
|---|---|
| Domain half-size | 48 blocks |
| Domain beacons | 3, half-size 16 |
| Worker cap | 20 |
| Active projects | 4 |
| Daily budget | 1,000 points |
| Pattern library slots | 24, capture up to 32 blocks |
| Outposts | 6 |

## An outpost grows

When a deposit is far from the Core and the colony is short of its metal, the Core proposes an outpost. Conditions:
the deposit is more than the domain half-size + 16 blocks and at most 512 blocks away, nobody claimed it, and the
stock of its ingot stayed below half of its target (64 when you set none) for 24,000 ticks.

1. The project needs an <ItemLink id="autarkia:outpost_seed" />. If there is none in storage, the fabricator makes
   one: 1 advanced circuit, 2 servos, 1 crate, 4 vantite steel ingots and 1 oak sapling in 400 ticks (64,000 FE).
2. A Porter or Hauler Crab carries the seed to the site. A Sentinel escorts it when one exists.
3. The carrier plants the seed. The outpost core grows in 200 ticks.
4. Workers build the outpost layout: the outpost core, a depot beside it, an HV pylon that feeds the core, a wind
   tower, a recharge dock and a jaw crusher on foundations. An enabled OUTPOST Pattern from your library replaces the
   built-in layout.
5. Cable-Layer Drones build the HV link: pylons about every 44 blocks on the surface, one at a time, from the outpost
   toward the Core. With the lamp option on, each pylon gets an industrial lamp beside its base.

<GameScene zoom="2" interactive={true}>
  <ImportStructure src="../structures/outpost_basic.nbt" />
</GameScene>

## Outpost rules

- An outpost is 64 to 512 blocks from the Core and at least 64 blocks from other outposts.
- Its own square has a half-size of 24 at tier 4 and 32 at tier 5 and 6. It is part of the domain.
- The outpost core keeps its chunk loaded. On a dedicated server the world still pauses when nobody is online, unless
  the server owner sets `pause-when-empty-seconds=0`.
- The outpost core charges one worker. It starts with the 64,000 FE of the seed and uses 256 FE/t.

## Catch-up

While the outpost is not loaded, it works on paper. Every 1,200 ticks it adds the items it made per tick while it
was loaded, times the time away (at most 72,000 ticks), times 0.75, times its power share. It needs 24,000 ticks of
loaded running first. Ore from a deposit lowers the deposit estimate. Items that do not fit go into a buffer of 4,096
items.

## More tier 4 projects

| Project | Trigger |
|---|---|
| HV link | An outpost has no power link, or its link runs at 80% load for 1,200 ticks (then a parallel line 3 blocks aside). |
| Cable car | Workers carry 256 or more items per day between an outpost depot and the main storage, with a straight path of at most 128 blocks. The line runs 2 gondolas. |
| Sentinel post | 3 or more hostile mobs in the domain for 2,400 ticks, or 8 or more colony blocks destroyed in one day. A catwalk with a recharge dock and a lamp, and a Sentinel order to the worker assembler. |
| Wind tower | Power short by 20% for 2,400 ticks and an HV network exists. |
| Hydro turbine | Power short by 20% for 2,400 ticks and a falling water column of 6 or more blocks exists. |
