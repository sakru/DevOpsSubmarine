# DevOpsSubmarine

Java console game · DEV26M_PJ-2 · Vyacheslav Kuznetsov

Run: `./run.sh` — JDK 17+, ANSI terminal.
Menu: New game → Easy (2 submarines), Normal (4), Hard (8). Harder modes spawn enemies more often.
After a session: time, distance, kills, level and reason for ending.

## Controls

- W/S or arrows: depth. A/D: brake/accelerate.
- Space/F: torpedo. 0: quit.

## Game

- Map: 200×43; camera follows the submarine and resizes with the terminal.
- 12 levels; both sides reload in 3 seconds.
- Yellow: hull damage. Red: damaged module. Modules have 50 HP and stop at 0%.
- Engine: movement. Sonar: off-screen contacts. Torpedo room: firing.
- Circular explosions; damage decreases with distance.

## Code

- `Main`, `Menu`: startup, difficulty selection and session statistics.
- `Game`: movement, spawning, collisions and damage.
- `Controller`: keyboard input queue.
- `Renderer`, `Camera`, `TerminalSize`: display and viewport.
- `Submarine`: rooms, collision shape and module condition.
- `Enemy.java`: superclass `Enemy`, three subclasses and `DepthCharge`.
- `WeaponCarrier`: polymorphic attacks for ships and enemy submarines.
- `Projectile`, `Torpedo`: projectile movement and impacts.
- `Explosion`, `ExplosionWorker`: blast animation and calculations; thread `explosions`.
- `SeaFloor`, `Weapons`: terrain and tuning constants.

## Tests

`javac -cp build -d build tests/*.java`, then `java -cp build CameraTest` (or another test).
