# Server energy increment

Status: partial MVP, not the complete requested vertical slice.

Persistent independent fields: Capacity, Output, Control, Efficiency, Regeneration.
Legacy numeric energy values migrate in place, preserving reserve. No offline catch-up.
Strain and overload countdown persist through restart/death/relogin. Countdown uses active living ticks.
All ability spending checks finite values, reserve, control and rolling 20-tick output budget.
No physical damage penalty is applied at zero reserve.

TEST DEFAULTS (not final approved balance): capacity 100, output 100/sec,
control 1, efficiency 1, regeneration 1/sec, strain gain output/2,
strain recovery 5/sec. Existing reinforcement cost is still provisional.

Manual checks in a disposable world (operator):
- /jjk test-awaken prints completion; /jjk genetics reveals existing assignment.
- /jjkenergy spend 80 then spend 1: first succeeds, second is denied by reduced output.
- Wait 10 seconds: strain recovers below 40, output recovers.
- Empty reserve: reinforcement unavailable, normal melee remains possible.
- Compare reserve across logout/restart: no elapsed-offline regeneration.
- Test a pre-update world: reserve survives numeric-to-compound migration.

Build: gradlew.bat build using JDK 17. Automated tests and live server verification pending.
Still pending: season genetics/Six Eyes ledger, canonical independent pool, five skill
training curves, real awakening trial, mastery requirements and expanded stats UI.
