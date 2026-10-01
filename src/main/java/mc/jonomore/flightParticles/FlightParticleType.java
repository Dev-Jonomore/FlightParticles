package mc.jonomore.flightParticles;

import org.bukkit.Particle;
import org.bukkit.permissions.Permissible;

/**
 * One trail option, as defined under {@code particles} in config.yml.
 *
 * @param literal         the id used in commands and storage; unique and lowercase
 * @param permission      the node required to use it, or null when everyone may
 * @param velocityTrail   true when the default trail is recomputed from the player's
 *                        velocity every tick, so it cannot be expressed as settings
 * @param data            the single float some particles take as data (Dragon Breath's
 *                        power), or null when the particle takes none
 */
public record FlightParticleType(
    String literal,
    String displayName,
    Particle particle,
    ParticleSettings defaultSettings,
    String permission,
    boolean velocityTrail,
    Float data) {

  /** Only Dust carries a color and size; they are ignored for every other particle. */
  public boolean hasColor() {
    return particle == Particle.DUST;
  }

  public boolean hasDynamicDefault() {
    return velocityTrail;
  }

  public boolean isAllowed(Permissible permissible) {
    return permission == null || permissible.hasPermission(permission);
  }
}
