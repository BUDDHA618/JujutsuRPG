# DEV Builder, first version

Open K -> DEV -> Строитель. Operator permission2 required on every server request.
Click Обновить библиотеку. First use copies technical_college.nbt into
<game directory>/config/jujutsurpg/buildings. With IDE runClient: run/config/jujutsurpg/buildings.
On a dedicated server files belong on SERVER, not client. No restart/reload needed for new files.
Supported: compressed vanilla structure NBT with palette, blocks, size. Not .schem/.litematic.
Names: ASCII letters/digits/underscore/hyphen, extension .nbt. Max100 library entries.

Choose a file (button cycles files), enter X/Y/Z (template origin/corner), rotate90,
click Preview. Close with Осмотреть to fly around. Reopen Builder to confirm Build.
The blue-tinted translucent block models are client-only; preview does not modify world.
Preview shows up to4096 exposed block models within32blocks of camera; fly around for large builds.
Nearer terrain can occlude ghost blocks; no x-ray overlay. No AI chat integration yet.
Build uses last server-approved preview; after editing coordinates request preview again.

Import is additive: air ignored, entities ignored, block entities and custom NBT ignored.
This means campus imported here has no students/beds with data/lectern content.
Use existing /place structure for the complete campus with embedded NPCs.
No .schem support yet. Limits:256 per side,500000 records,200000 non-air blocks,
8MB compressed input,128MB NBT accounting. Only within256blocks of operator, world border/height.

One global build/undo job at a time;512records/tick, no chunk force-loading.
Unloaded/protected/unbreakable blocks and block entities are skipped.
Break and place Forge cancellation hooks are checked. Block placing does not generate item drops.
No full neighbor-update sweep: terrain/fluid/plant interactions still require gameplay QA.

Undo keeps one changed-block snapshot per operator in RAM, not on disk.
Works only in original dimension, before logout/server restart/new build.
A changed destination is restored only if it still matches the builder's placed block.
Block entities are never overwritten or restored; undo respects protection hooks.
Disconnect interrupts an active job and discards history; it does NOT roll back partial placement.
For initial testing use a disposable creative world. No claim of crash-atomic transaction.

Verified: JDK17 build and real Minecraft NbtIo bounded-read of campus119777 non-air blocks.
Live client rendering, controls, multiplayer/protection and undo QA still pending.
Manual test: preview small NBT, check world unchanged, rotate/reposition, build, undo;
add a second file while staying online and refresh; ensure non-operator request has no effect.
