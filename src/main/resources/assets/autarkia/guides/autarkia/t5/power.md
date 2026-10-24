---
navigation:
  title: Plasma and Superconductors
  parent: t5/index.md
  position: 3
  icon: autarkia:plasma_generator
---

# Plasma and Superconductors

Tier 5 power is large and cold. The <ItemLink id="autarkia:plasma_generator" /> burns
<ItemLink id="autarkia:lattice_fuel_cell" /> and gives 32,768 FE/t, but its ignition takes 1,000,000 FE from the
network first.

Superconductor lines carry the energy. Spans from the <ItemLink id="autarkia:superconductor_spool" /> carry 65,536
FE/t over up to 32 blocks with no loss. They hang between <ItemLink id="autarkia:cryo_node" /> poles or machine ports.
The <ItemLink id="autarkia:superconducting_coil" /> stores 400,000,000 FE.

Everything superconducting must stay cold. Cryo nodes and coils use <ItemLink id="autarkia:coolant_cell" /> units all
the time. Without coolant a span or a coil warms up: frost turns to steam, then to hissing, and at full temperature it
quenches with a blast. A quenched span is gone; a quenched coil loses all its energy.

How the parts work together:

1. Fabricate coolant cells and fuel cells.
2. Build a line of cryo nodes and fill them with coolant.
3. Charge a superconducting coil from your Tier 4 power.
4. Ignite the plasma generator from the coil.
5. Keep coolant in every node and coil.

<SubPages />
