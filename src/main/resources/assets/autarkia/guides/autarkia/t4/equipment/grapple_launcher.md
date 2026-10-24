---
navigation:
  title: Grapple Launcher
  parent: t4/equipment.md
  position: 30
  icon: autarkia:grapple_launcher
item_ids:
  - autarkia:grapple_launcher
---

# Grapple Launcher

<ItemImage id="autarkia:grapple_launcher" scale="2" />

The grapple launcher fires a hook on a cable. The hook holds on the first block it hits, and the launcher pulls you
to it. Use it to climb cliffs, cross ravines and reach high pylons. It runs on FE.

## Get it

| Inputs | Machine | Time | Output |
|---|---|---|---|
| 3 <ItemLink id="autarkia:vantite_steel_ingot" />, 2 <ItemLink id="autarkia:servo" />, 1 <ItemLink id="autarkia:advanced_circuit" />, 1 <ItemLink id="autarkia:battery_bank" />, 1 tripwire hook, 1 <ItemLink id="autarkia:hv_cable_spool" /> | <ItemLink id="autarkia:fabricator" /> | 300 ticks, 334 FE/t (100,200 FE) | 1 Grapple Launcher |

The launcher comes out empty. Charge it in a <ItemLink id="autarkia:battery_bank" /> or a
<ItemLink id="autarkia:capacitor_bank" />.

## Use it

1. Look at a block up to 48 blocks away.
2. Press and hold use. The hook flies straight at 3 blocks per tick. A shot costs 2,000 FE.
3. When the hook holds, keep use held. The launcher pulls you to the hook at 1.2 blocks per tick for 20 FE/t.
4. Sneak while you hold use to hang in place for 5 FE/t.
5. Release use to let go. For 40 ticks after you let go, you take no fall damage.
6. After a release the launcher has a cooldown of 10 ticks.

The hook ignores entities. A new shot removes your old hook. Without FE for the pull, the hook lets go and you get the
same 40 ticks of fall protection.

## Numbers

| Property | Value |
|---|---|
| Energy | 1,000,000 FE |
| Charge rate | Up to 5,000 FE/t |
| Shot | 2,000 FE |
| Hook speed | 3 blocks per tick |
| Range | 48 blocks |
| Pull | 1.2 blocks per tick, 20 FE/t |
| Hang | 5 FE/t |
| Fall protection | 40 ticks after you let go |
| Cooldown | 10 ticks |

## Problems

- Nothing happens when you press use: the launcher has less than 2,000 FE, or the cooldown runs. Charge it or wait.
- The hook flies away and is gone: it hit no block within 48 blocks.

## Next

- Pull yourself to a hook 40 or more blocks away for the "Swing Line" advancement.
- The <ItemLink id="autarkia:frontier_suit_boots" /> also protect you from falls.
