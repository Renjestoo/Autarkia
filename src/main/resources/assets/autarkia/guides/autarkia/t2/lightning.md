---
navigation:
  title: Lightning
  parent: t2/index.md
  position: 3
  icon: autarkia:lightning_mast
---

# Lightning

Metal wire attracts lightning. Rope does not. When a bolt strikes within 16 blocks of a node with a copper span, the
bolt moves to the highest such node. Then a white pulse runs along the wires at 2 blocks per tick.

- Each span that the pulse passes gets 30% hotter. A span that is hot already can snap.
- Living things within 1.5 blocks of the struck node take 5 damage.
- The first unprotected machine that the pulse reaches is destroyed by a surge. It drops as an item, and the pulse
  stops.
- A <ItemLink id="autarkia:capacitor_bank" /> on the way breaks, and the pulse stops.

Two blocks protect you:

- The <ItemLink id="autarkia:lightning_arrester" /> stops the pulse at a pole, or protects the machine next to it.
- The <ItemLink id="autarkia:lightning_mast" /> pulls every bolt within 48 blocks to itself. Its strikes never start a
  pulse. Each strike puts 120,000 FE into the capacitor banks of its network.

Build a mast early. It turns the danger into free energy.

<SubPages />
