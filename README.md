# DevOpsSubmarine

Java terminal game · DEV26M_PJ-2 · Vyacheslav Kuznetsov

Run `./run.sh` with JDK 17+ and an ANSI terminal. The camera follows the submarine and adapts to the terminal size.

## Playing

- W/S or arrows: depth. A/D: brake/accelerate. Space/F: torpedo. 0: quit.
- R: repair one internal module in 5 seconds; press again after completion for the next. Hull and crew cannot be healed.
- Easy / Normal / Hard allow 1 / 2 / 4 enemy submarines. Both sides reload in 3 seconds.
- Map: 240×47, with a waterline and submerged ship hulls that can be hit from depth 0.
- Automatic sonar reveals terrain and tracks discovered enemies. Enemy submarines scan before lining up a shot.
- Hull damage stays local. A breached section or a direct NAV hit destroys a vessel; Hull% shows its weakest section.
- Yellow marks hull damage; red marks damaged modules or breaches. Modules have 50 HP and stop at 0%.
- Each vessel has 100 sailors. White dots move and repair; red dots are injured or dead. Explosions damage nearby crew.
- Mines and depth charges deal up to 50 damage; torpedoes deal 50 and can hit a module behind the hull. Seabed contact deals 5 damage at most once per second.

## Results

Enter a captain name before playing (default: `Captain`). Finished sessions save automatically, including restarts and quits.

The menu can list results, search by captain name, delete a result by ID, and show totals. Rankings use kills, then distance. N/P change pages.

Results live in `scores.tsv`, excluded from Git. This file store avoids a database dependency for a local game. Invalid files produce an error instead of being overwritten.

## Structure

`Game` owns the simulation. `Menu` handles interaction; `Scores` validates, searches and saves records. These rules stay outside the menu so they can be used without terminal input.

`EnemyShip`, `EnemySub` and `EnemyMine` extend `Enemy`. Polymorphism lets the update loop call their own behaviour through superclass references. `WeaponCarrier` provides the shared attack interface.

`Submarine` uses composition to share rooms, damage, crew and repairs between player and enemy vessels. This avoids separate damage systems. `Projectile` shares movement between torpedoes and depth charges.

`Renderer` draws on the main thread to avoid competing terminal writes. Keyboard input and explosion calculations use worker threads and queues; only the main thread changes game state.
