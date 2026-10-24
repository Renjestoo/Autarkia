---
navigation:
  title: Capsule Tube
  parent: t3/logistics.md
  position: 40
  icon: autarkia:capsule_tube
item_ids:
  - autarkia:capsule_tube
---

# Capsule Tube

<ItemImage id="autarkia:capsule_tube" scale="2" />

The capsule tube is the glass pipe of the capsule network. Capsules with one stack of items fly through it at 8 blocks per second. The tubes need no power. The <ItemLink id="autarkia:capsule_station" /> at each end sends and receives the capsules.

## Get it

<RecipeFor id="autarkia:capsule_tube" />

One craft gives 8 tubes.

## Use it

1. Place the tubes in a line. A tube connects on all 6 faces to the tubes next to it.
2. Make branches where you want. A network can split and join.
3. Put a <ItemLink id="autarkia:capsule_station" /> at each end. Click the face of the last tube with the station. The mouth of the station then points into the tube.
4. Look at a tube with Jade to check the network.

A tube connects to a station only at the mouth of the station. All tubes and stations that touch form one network.

## Numbers

| Property | Value |
|---|---|
| Capsule speed | 8 blocks per second (config) |
| Tubes per network | 512 (config 64 to 2,048) |
| Stations per network | 32 |
| Capsules per network | 64 (config 8 to 256) |
| Capsules per tube block | 1 for each direction |

Capsules in opposite directions pass each other. A capsule waits at the block edge while the next tube block holds a capsule that goes the same way.

## Jade

Jade shows "Network: N tubes, N stations, N capsules. State".

| State | Meaning |
|---|---|
| Running | capsules move |
| Clogged | a capsule cannot unload; the tubes glow red, hiss and smoke |
| Network too large | more than 512 tubes or more than 32 stations; the network stops |
| Chunks not loaded | a part of the network is in an unloaded chunk; the capsules wait |
| No station | the tubes have no station |

## Problems

- "Network too large": split the network into two networks, or remove stations.
- "Chunks not loaded": keep all chunks of the network loaded, or make the network shorter.
- "No station": place a station with its mouth into the tube.
- Breaking a tube drops the items of the capsules inside it.

## Next

- <ItemLink id="autarkia:capsule_station" /> explains modes, filters and routing.
