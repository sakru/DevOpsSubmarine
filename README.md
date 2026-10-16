# DevOpsSubmarine

A submarine game in the terminal, written in Java for the DEV26M_PJ-2 final project.

Move up and down, fire torpedoes, and avoid enemy submarines, mines and depth
charges dropped by surface ships. The seabed scrolls as you move forward.
There are 12 difficulty levels. The game ends when the hull reaches zero.

Author: Vyacheslav Kuznetsov

## Running the game

You need JDK 17 or newer and a terminal with ANSI support.

```sh
./run.sh
```

The script compiles the Java files into `build/` and starts the game.
You can set `JAVA_HOME` to choose a JDK.

The field is 160 × 36 characters. With borders and status lines, the terminal
needs at least 162 columns and 40 rows.

- W/S or up/down arrows: change depth
- Space or F: fire a torpedo
- 0: quit

## How the code is organised

`Main` runs the game loop. `Controller` handles keyboard input, `Game` updates
the game state, and `Renderer` draws it. Movement and collision rules are in
`Game`, so they can run without the terminal display.

`Enemy` is the abstract superclass for `EnemyShip`, `EnemySub` and `EnemyMine`.
They share position and alive/dead state, but each overrides `update()` and
`sprite()`. The enemies are stored in an `ArrayList<Enemy>`. When the game loops
through it, polymorphism calls the matching implementation for each enemy.
The same collection is searched for collisions and cleaned up when enemies die.

Ships and enemy submarines implement the `WeaponCarrier` interface. The game
calls `tryAttack()` through this interface. Mines explode on contact instead,
so they do not need to implement it.

`Projectile` contains the common movement code for `Torpedo` and `DepthCharge`.
Depth charges stay in their drop column; torpedoes also move with the scrolling
world. `SeaFloor` generates the terrain, `Explosion` calculates blast damage,
and `Weapons` contains speeds, cooldowns and damage values.

## Still to do

The game has keyboard controls, but it does not yet have the collection menu
required by the assignment. Adding, removing and searching enemies will need
to use the same game state. Input errors also need clearer messages.

## Movement test

After building the game, run:

```sh
java -cp build tests/ProjectileMovementTest.java
```

This checks falling speed, scrolling and the previous projectile positions
used for collision detection.
