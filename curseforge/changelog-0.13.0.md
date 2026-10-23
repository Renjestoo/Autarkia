## Autarkia 0.13.0

- **Cable rule:** a rope ends only on a rope sheave, metal cables only on ceramic bushings. Machines with a turning shaft (tier 1 machines, the water wheel, the steam engine) have both; electric machines have only a bushing. A transformer joins rope and metal. The colony builders follow the rule and place a transformer when a new machine cannot take the network's cable.
- **Belts take no cable:** a node next to any belt of a line powers the whole line.
- **Cable terminals reworked:** every terminal now sits on a solid part, clear of turning wheels, rings and flywheels, and each cable leaves from the terminal that faces its other end. Ropes end on the rim of the sheave. The rope pulley has a real wheel.
- **Gauges:** the boiler needle shows the steam pressure, the steam turbine needle shows the rotor speed.
- **Animations:** the singularity core rings hover up and down while they turn; the magma vent, molten crucible metal and phase node crystal are animated; a warm cryo node shows melted frost; an empty gyro heart no longer wobbles.
- Spans strung before 0.13.0 keep their old ends and are not removed, even when they break the new rule.
