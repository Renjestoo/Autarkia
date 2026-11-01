## Autarkia 0.15.30

- **Performance:** belts no longer load chunks along their line. A line that runs into an unloaded chunk waits until the chunk loads, and then continues.

Found in the code review. Build checked; not tested in game.
