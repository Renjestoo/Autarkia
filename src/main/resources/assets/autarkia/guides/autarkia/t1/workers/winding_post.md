---
navigation:
  title: Winding Post
  parent: t1/workers.md
  position: 20
  icon: autarkia:winding_post
item_ids:
  - autarkia:winding_post
---

# Winding Post

<ItemImage id="autarkia:winding_post" scale="2" />

The winding post is the charger for spring workers like the Porter. A Porter runs on a wound spring. Its energy unit
is tension (T). The post winds the spring with FE from your power network, or by hand.

## Get it

<RecipeFor id="autarkia:winding_post" />

You need 1 <ItemLink id="autarkia:bronze_gear" />, 1 <ItemLink id="autarkia:mainspring" /> and 5 logs.

The colony also proposes a Winding Post project when workers run low or work far from a charger.

## Use it

1. Place the post inside your colony's domain. The front of the post faces you.
2. Keep the block in front of the post free. The worker stands there while it winds.
3. Give the post FE from your power network. For example, place it next to a
   <ItemLink id="autarkia:rope_pulley" /> on a powered rope span. See [Power](../power.md).
4. Wait. A Porter with a low spring walks to the nearest free post by itself.
5. The post winds the Porter until the spring is full. Then the Porter goes back to work.
6. With no FE in the post, hold the use key on the post. This winds the docked worker by hand.

A Porter goes to a post when its spring falls below its return reserve. The reserve is 200 T plus 12 T for each block
of the way to the post.

## Numbers

| Property | Value |
|---|---|
| Winding with FE | 40 T/t |
| Power use | 80 FE/t (2 FE for each T, server option `windingFePerTension`) |
| Winding by hand | 10 T/t, only while the post has no FE |
| FE buffer | 4,000 FE |
| Largest FE input | 80 FE/t |
| Workers at one time | 1 |
| Full Porter spring (6,000 T) | 150 ticks (7.5 seconds) with FE, 600 ticks (30 seconds) by hand |

## Problems

- The Porter does not come to the post: another worker is docked, or the post is outside the domain. Place a second
  post.
- The post does not wind: it has no FE. Check the power network, or hold the use key on the post.
- A Porter stopped in the field with an empty spring: right-click it with an empty hand. This gives it 100 T and wakes
  it up. Then it walks to a post. If the post is far, pick up the Porter with a
  <ItemLink id="autarkia:worker_capsule" /> and release it near the post.

## Next

- Make more Porters in a <ItemLink id="autarkia:worker_assembler" />.
- Read about the colony projects at the <ItemLink id="autarkia:project_board" />.
