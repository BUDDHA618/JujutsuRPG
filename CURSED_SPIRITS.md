# First cursed spirits

Summon via Creative -> Spawn Eggs (search Jogo / Hanami / Mahito).
Russian localization supports Джого / Ханами / Махито.
Entity IDs: jujutsurpg:jogo_curse, jujutsurpg:hanami_curse, jujutsurpg:mahito_curse.
No natural spawning yet. Non-peaceful difficulty required. Creative/spectator players ignored.

Jogo: volcanic cyclops, crater and glowing lava material; 70HP, 7 melee,
telegraphed flame line (7 damage plus 3 seconds burning), fire immune.
Hanami: tall bark-armored mask with branching eye growths and shoulder roots;
120HP, armor8, melee10; root burst causes 5 damage and Slowness II for3seconds.
Mahito: stitched skin, asymmetric eyes, long hair, deformed blade arm;
85HP, melee6; lunge and close-range 8 damage plus Weakness for3seconds.
All combat numbers are provisional test defaults. Special effects require server-side LOS.

Distinct articulated cuboid models, 128x128 material atlases, walking/head tracking,
breathing and melee arm animation. These are original simplified interpretations,
not imported anime meshes or high-poly sculpted replicas.

Build and packaging verified on JDK17. Live client/dedicated-server QA pending:
- Spawn all 3 eggs, verify texture/model/sounds, save/reload entity.
- Switch to survival: approach, melee, telegraph and special attack.
- Creative/spectator should not be attacked; Peaceful removes hostile mobs.
- Use Blue/Red/Purple to verify pull, knockback and damage against spirits.
- Verify no natural spawns and no lava/block placement from their specials.
