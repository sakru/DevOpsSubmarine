/*
 * Gives armed enemies one shared way to attack.
 * Ships and submarines implement it, so Game can ask either one to fire through the same interface.
 */
public interface WeaponCarrier {
    void tryAttack(Game game);
}
