# DevOpsSubmarine

Java console game · DEV26M_PJ-2 · Vyacheslav Kuznetsov

## Run

JDK 17+, ANSI terminal. Start with `./run.sh`.

## Controls

- W/S, arrows: depth.
- A/D: brake/accelerate.
- Space/F: torpedo; reload takes 3 seconds for both sides.
- 0: quit.

## Game

- Map: 200×43; camera follows the submarine and resizes with the terminal.
- 12 levels; at most two enemy submarines.
- Yellow: hull damage. Red: damaged module. Modules stop working at 0%.
- Engine: movement. Sonar: off-screen contacts. Torpedo room: firing.
- Explosions: circular blast area with falling damage over distance.

## Code

- `Main`: game loop, 25 ticks/s.
- `Game`: movement, spawning, collisions and damage.
- `Controller`: keyboard input queue.
- `Renderer`, `Camera`, `TerminalSize`: display and viewport.
- `Submarine`: rooms, collision shape and module condition.
- `Enemy.java`: superclass `Enemy`, three subclasses and `DepthCharge`.
- `WeaponCarrier`: polymorphic attacks for ships and enemy submarines.
- `Projectile`, `Torpedo`: projectile movement and impacts.
- `Explosion`, `ExplosionWorker`: animation and blast calculations; worker thread `explosions`.
- `SeaFloor`, `Weapons`: terrain and tuning constants.

## Tests

Build with `./run.sh`, then `javac -cp build -d build tests/*.java`.
Run a check with `java -cp build CameraTest` (or another class from `tests/`).
