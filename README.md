# DevOpsSubmarine

Java console game · DEV26M_PJ-2 · Vyacheslav Kuznetsov

Run: `./run.sh` — JDK 17+, ANSI terminal.
Menu: New game → Easy (1 submarine), Normal (2), Hard (4). Harder modes spawn enemies more often.
Menus appear over the scene. After death: frozen last frame, session statistics, restart (1) or exit (0).

## Controls

- W/S or arrows: depth. A/D: brake/accelerate.
- Space/F: torpedo. R: repair one module in 5 seconds; press again after completion for the next. 0: quit.

## Game

- Map: 240×47; waterline and submerged ship hulls. Surface torpedoes can hit ships.
- Hull damage is local: a breached section or a hit to NAV destroys the vessel. Hits elsewhere do not add together.
- Seabed contact: 5 hull damage, at most once per second; vessels stay above the ground.
- 12 levels; both sides reload in 3 seconds.
- Yellow: hull damage. Red: module damage or a hull breach. Hull% shows the weakest section; modules stop at 0%.
- Crew: 100 per vessel, with individual HP. White dots move, flee damage and repair; red dots are injured or dead. Hull cannot be repaired.
- Enemy vessels use the same local hull damage and module rules.
- Engine: movement. Sonar: visibility and contacts. Torpedo room: firing.
- Automatic sonar waves reveal terrain and track discovered boats, mines and depth charges until removed.
- Enemies scan for the player, align their torpedo tubes, then fire. Cyan waves belong to enemies.
- Circular explosions; damage decreases with distance.
- Mines and depth charges: up to 50 damage, blast radius 2 columns / 1 row. Torpedoes: 50 damage; nearby modules behind the hull take the hit too.

## Code

- `Main`, `Menu`: startup, difficulty selection and session statistics.
- `Game`: movement, spawning, collisions and damage.
- `Controller`: keyboard input queue.
- `Renderer`, `Camera`, `TerminalSize`: display and viewport.
- `Submarine`: vessel geometry, modules, crew and repairs.
- `Sonar`: automatic waves and explored terrain.
- `Enemy.java`: superclass `Enemy`, three subclasses and `DepthCharge`.
- `WeaponCarrier`: polymorphic attacks for ships and enemy submarines.
- `Projectile`, `Torpedo`: projectile movement and impacts.
- `Explosion`, `ExplosionWorker`: blast animation and calculations; thread `explosions`.
- `SeaFloor`, `Weapons`: terrain and tuning constants.

## Tests

`javac -cp build -d build tests/*.java`, then `java -cp build CameraTest` (or another test).
