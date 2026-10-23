# Limitless animated prototype

Minecraft 1.20.1 / Forge 47.4.28 / JDK17. Build passes; live visual/gameplay QA pending.
K -> DEV: Gojo / limitless / awakened=yes / Apply / Restore CE.
Z Blue: 16-tick charge, 5-block center travel, 4 damage on contact,
nearby mobs pulled toward caster for up to 40 ticks after charge. Cost20, cooldown80ticks.
C Red: 16-tick charge, 10-block center travel, 10 damage and outward impulse.
Cost30, cooldown100ticks.
X Purple: 36-tick charge combining red and blue rings, 30-block center travel,
1.5-block spherical radius (diameter3), 24 damage once per target. Cost60, cooldown200ticks.
Costs/damage/cooldowns are TEST DEFAULTS, not final balance.

All shots start in front of caster and sweep in half-block increments at 1.5blocks/tick.
Length specifies center travel through terrain; spherical endcaps extend by the radius.
Purple block selection uses block centers inside the sphere; entities use sphere vs AABB.
Blue/Red drop blocks; Purple removes them without drops.
Unbreakables, block entities, protected spawn and Forge break cancellation remain respected.

Charge is rendered with rotating colored rings and a white core; Purple merges red/blue.
Moving shots have concentric rings, colored core/trail, launch sound and endpoint pulse.
Aim is captured at release, so caster can aim during charge. Only one active cast per player.
Damage and destruction occur only after charge. Death/logout/dimension change cancels casting.
Existing DEV infinite CE does not disable cooldown, output, control or overload.

Manual verification in a test world:
- Z/C/X: visible charge at caster before any block damage, then moving projectile.
- Purple cuts a diameter3 corridor through a wall with 30-block center travel.
- Red penetrates10 and pushes targets away; Blue penetrates5 and pulls groups toward caster.
- Compare Red10 and Purple24 damage on fresh targets.
- Test upward/downward shots, low ceilings, world border, unloaded chunks and two clients.
- Check no casting with hidden/non-Limitless technique, zero CE or cooldown.
- Check protected blocks/chests/bedrock survive, PvP/team rules remain respected.
- Set particles to All for visual evaluation; check C key conflicts in Controls.
