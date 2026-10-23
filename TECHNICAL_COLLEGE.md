# Technical college prototype

112x112-block temple-inspired campus, not an exact anime reconstruction.
Main hall/classroom, dormitory with beds, library, torii gate, garden/pool and training targets.
Vanilla jigsaw structure/data pack registration, one fixed layout with random rotation.
Overworld plains, sunflower plains, forest and birch forest. Random spread48chunks,
separation24, one candidate per region subject to valid biome. New chunks only.
Terrain follows surface height with a six-block terrace; mountainous terrain edges still need QA.
No 1000x1000 template or retroactive changes to explored chunks.

12 embedded persistent Student Mage NPCs, 40HP, melee5, CE bolt6 every35ticks (test defaults).
Patrol around persisted home within36blocks; target CurseSpirit only. Right-click for greeting.
Students are mortal, no automatic respawn; no quests/classes/trading yet.
Student spawn egg is in Creative Spawn Eggs.

Operator test commands:
/locate structure jujutsurpg:technical_college
/place structure jujutsurpg:technical_college
/summon jujutsurpg:student_mage
Manual /place overwrites a large area: use a disposable world with space.

Build JDK17 and binary NBT/palette/resource checks passed. Live worldgen/client QA pending.
Verify /locate and /place, rotation, terrain foundation, rooms/lighting/pathfinding,
12 NPCs, save/reload/patrol, curse combat and no attacks on players.
Generator source: tools/build_campus.py (standard library only).
