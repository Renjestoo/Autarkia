## Autarkia 0.14.6

- **Surveyors get survey jobs by themselves.** Before this fix, no colony rule opened survey jobs, so the colony never found deposits and never proposed an Outpost project. Now a Tier 4 colony with a Surveyor opens one survey job (radius 64) every 1,200 ticks when it is short of a metal (an ingot below half of its stock target, 64 without a target), has a free outpost slot and no open Outpost project, and knows no deposit that can get an outpost.
- The survey areas are 128 blocks apart in a square spiral around the Core, up to the maximum outpost distance (512 blocks). Each new job goes to the next area. After the last area, the list starts again.
- The colony does not make Outpost Seeds. For an Outpost project, keep one in colony storage. The Field Manual pages Frontier Workers, Outpost Seed and Tier 4 now tell this.
