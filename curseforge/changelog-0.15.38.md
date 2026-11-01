## Autarkia 0.15.38

- **Performance:** the outpost site search now runs in small steps, so a walker with no site no longer causes a lag spike every 5 seconds.
- **Fixed:** a Replicator Walker with no valid site now releases that deposit and tries the next one. It no longer floods the board log, and a failed search or pylon route rests for 5 minutes.

Found in the code review. Build checked; not tested in game.
