## Autarkia 0.15.13

This update fixes the most serious problems from a full review of the mod. These fixes passed the build, but they are not yet tested in game, so please report anything that behaves oddly.

### Colony

- **Building sites level correctly.** When a site needed both cuts and fills, the fills never started, so buildings could stand over holes. Outposts now level their site too. A project saved in the middle of its cuts by an older version continues at its fills.
- **Projects finish every phase.** After a Core was replanted, or after a job was cancelled, a project could finish as done with spans or commissioning missing. The Core now reopens the jobs when it blooms, and a phase stays open until its goal is met.

### Workers

- **Electric workers recover from 0 FE.** A worker that ran out of charge stayed failed forever, because electric workers cannot be wound by hand. It now goes to its charger. A drone lands first and then limps there. Spring workers still wait for hand winding.

### Machines and items

- **Full input stacks no longer jam machines.** A machine stopped when a stack spilled into a slot that its recipe did not use, for example more than 64 ore in a Blast Smelter. A machine now accepts more of a recipe's own input, and each item takes one input slot. You can no longer put two stacks of the same item into one machine.
- **The rack menu no longer loses or duplicates items.** Shift-click into an opened crate could destroy the items it merged. A shift-click out to a nearly full inventory could copy items.

### Power

- **Machines next to a live pole get power.** A machine placed or replaced beside a connector stayed unpowered until a span changed or the chunk reloaded.
- **Snaps and lightning no longer load chunks along power lines.** One snap or bolt on a long line could stall the server. A lightning pulse now stops at an unloaded node, and a machine across a chunk border connects when its chunk loads.

### Multiplayer

- **The wrench respects rights and tools.** Sneak-wrench now works only on machines and crates. Colony protection and the Gyro Heart lock apply. Ores and plain blocks, including catwalks, need their normal tool.
- **Only colony members use a colony's grid through Aegis gear.** Before this fix, any player near a tether point could charge Aegis items and have hits paid from that colony's grid, and a Reform could use its matter.
- **The linked stock view shows only your own colony.** Before this fix, any player with a ledger or depot menu could see all depots on the server and order items out of them.
- **Recall refuses a worker from another colony.** A modified client could move another colony's worker.

### Dependencies

- **GuideME no longer needs one exact version.** Autarkia now accepts GuideME from 26.3.4-alpha up to below 26.4. The old exact pin stopped the game from loading with a newer GuideME.
