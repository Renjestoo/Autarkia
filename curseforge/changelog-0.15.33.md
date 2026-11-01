## Autarkia 0.15.33

- **Performance:** warehouses, crate launchers and cable car link checks no longer load chunks.
- **Changed:** racks in unloaded chunks do not count in the warehouse stock until they load. A crate launcher does not fire through an unloaded chunk. A cable car link through an unloaded chunk reports blocked at that position.

Found in the code review. Build checked; not tested in game.
