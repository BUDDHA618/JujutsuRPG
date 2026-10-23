# Limitless animated prototype

Minecraft 1.20.1 / Forge 47.4.28 / JDK17. Build passes; live visual/gameplay QA pending.
K -> DEV: Gojo / limitless / awakened=yes / Apply / Restore CE.
Z Blue: instant launch, 5-block center travel, 4 damage on contact,
nearby mobs pulled toward caster for up to 40 ticks after activation. Cost20, cooldown80ticks.
C Red: instant launch, 10-block center travel, 10 damage and outward impulse.
Cost30, cooldown100ticks.
X Purple: instant launch, 30-block center travel,
1.5-block spherical radius (diameter3), 24 damage once per target. Cost60, cooldown200ticks.
Costs/damage/cooldowns are TEST DEFAULTS, not final balance.

All shots start in front of caster and sweep in half-block increments at 1.5blocks/tick.
Length specifies center travel through terrain; spherical endcaps extend by the radius.
Purple block selection uses block centers inside the sphere; entities use sphere vs AABB.
Blue/Red drop blocks; Purple removes them without drops.
Unbreakables, block entities, protected spawn and Forge break cancellation remain respected.

Visuals use client-rendered translucent spherical shells, bright core and three rotating jagged arcs.
No cast-time delay: next server tick starts the shot. No fullscreen effect or charging ring.
Geometry fades inside 2.3 blocks of the camera, reaching full opacity factor at4.3 blocks.
No external shader pack required; this is not dynamic world lighting or bloom.
Only one active cast per player. Aim captured at launch; death/logout/dimension cancels cast.
Existing DEV infinite CE does not disable cooldown, output, control or overload.

Manual verification in a test world:
- Z/C/X: immediate moving projectile, sphere/core/arcs visible ahead.
- Purple cuts a diameter3 corridor through a wall with 30-block center travel.
- Red penetrates10 and pushes targets away; Blue penetrates5 and pulls groups toward caster.
- Compare Red10 and Purple24 damage on fresh targets.
- Test upward/downward shots, low ceilings, world border, unloaded chunks and two clients.
- Check no casting with hidden/non-Limitless technique, zero CE or cooldown.
- Check protected blocks/chests/bedrock survive, PvP/team rules remain respected.
- Test first/third person and near-camera fade; check C key conflicts in Controls.
