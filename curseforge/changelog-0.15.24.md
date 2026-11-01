## Autarkia 0.15.24

- **Far drones after a restart:** Surveyors, Cable-Layer Drones and Sentinels far from the Core now wake up after a server restart. Before, they stayed frozen.
- **Sentinels:** a patrol keeps its chunk loaded, so a Sentinel no longer freezes on the way when no player is near.
- **Lost workers:** a worker that has not reported for an hour frees its worker slot, and the colony log says so. It signs back in if it returns.

Found in the code review. Build checked; not tested in game.
