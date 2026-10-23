# Limitless combat prototype

JDK17 / Forge 47.4.28 / Minecraft 1.20.1.
Provisional tuning, not final canonical balance or mastery progression.

K -> DEV: select Gojo, limitless, awakened=yes, Apply. Restore CE. Close menu.
Z: Blue. Aim up to 18 blocks. Attracts living targets within radius 5 for 3 seconds,
damages targets near its center (3 damage every 10 ticks), breaks nearby blocks with drops.
Cost 20 CE, output 20, cooldown 4 seconds.
X: Purple. 48-block moving sweep, radius 1.6, 24 damage once per target,
breaks blocks without drops. Cost 60 CE, output 60, cooldown 10 seconds.
Both require awakened Limitless and control >=1; clan alone does not grant the technique.
Only one cast per player at a time. Server owns direction, position, cost and effects.
Death, dimension change or logout ends the cast. Cooldown timestamps persist.

World interaction: skips unbreakable blocks and block entities; checks mayBuild,
spawn protection, world border, loaded chunks and Forge block-break cancellation.
PvP damage respects server PvP setting and friendly-fire/team rules.

Manual verification pending in a disposable world:
1. Before awakening/with another technique, both keys reject casting.
2. Blue attracts mobs, damages at center, pulls no caster, drops broken blocks.
3. Purple hits mobs once, cuts terrain, leaves bedrock/chests intact.
4. At zero CE neither fires; rapid key spam cannot bypass cooldown.
5. Verify protected spawn / protection mod cancellation and PvP=false.
6. Logout, die or change dimension during Blue; old cast must disappear.
7. Rejoin/restart; chosen technique and remaining game-time cooldown persist.
8. Test with two clients; particles and block changes visible to both.

Build passes on JDK17. No live gameplay test performed yet.
