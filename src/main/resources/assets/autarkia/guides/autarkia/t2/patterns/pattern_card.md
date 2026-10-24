---
navigation:
  title: Pattern Card
  parent: t2/patterns.md
  position: 20
  icon: autarkia:pattern_card
item_ids:
  - autarkia:pattern_card
---

# Pattern Card

<ItemImage id="autarkia:pattern_card" scale="2" />

The pattern card holds one Pattern, a recorded build. A new card is blank. The <ItemLink id="autarkia:pattern_loom" />
records a Pattern onto it. A recorded card shines and you can add it to the library of the colony.

## Get it

One craft gives 4 cards.

<RecipeFor id="autarkia:pattern_card" />

## Use it

1. Put a blank card into the card slot of a <ItemLink id="autarkia:pattern_loom" />.
2. Record a build. The card gets the Pattern.
3. Take the recorded card out of the loom.
4. To add the Pattern to the library later, hold the card in your main hand.
5. Open the Colony Core, go to the Patterns tab and click "Insert held card".

The tooltip of the card shows "Blank card", or the Pattern: for example "Pattern Smelter 8x5x8, 120 blocks". That is
the name, the size of the box and the number of blocks.

## The Patterns tab

The Patterns tab of the Core screen shows the library:

- "Library 1 / 4": the Patterns in the library and the number of places.
- Each row shows the name, the role and the number of watched builds of this Pattern.
- Remove: takes the Pattern out of the library. Its builds are not watched any more.
- Watch and Unwatch: switch the watch of a build on or off.
- The role button: changes the role of the Pattern. The roles are Generic, Line, Generator, Storage, Defense and
  Outpost. The colony uses the role later to choose Patterns for new work.

## Numbers

| Property | Value |
|---|---|
| Patterns on one card | 1 |
| Library at Tier 2 | 4 Patterns |
| Cards from one craft | 4 |

## Problems

- "Hold a recorded pattern card": the card in your main hand is blank, or you hold no card.
- "This Pattern is already in the library": the library has this Pattern.
- "The library is full": remove a Pattern first. A higher colony tier has more places.
- "Missing pattern": the save file of this Pattern is not on this server.

## Next

- From Tier 3, the colony repairs watched builds from their Patterns.
