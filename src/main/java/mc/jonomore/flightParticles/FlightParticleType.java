package mc.jonomore.flightParticles;

import org.bukkit.Particle;
import org.bukkit.permissions.Permissible;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public enum FlightParticleType {
  DUST("dust", "Dust", Particle.DUST,
    ParticleSettings.dust(0.2, 0.0, 0.2, 1, 1.0f, 0x6E2970, 1.2f)),
  FLAME("flame", "Flame", Particle.FLAME,
    ParticleSettings.of(0.15, 0.1, 0.15, 1, 0.0f)),
  SPARK("spark", "Electric Spark", Particle.ELECTRIC_SPARK,
    ParticleSettings.of(0.4, 0.2, 0.4, 1, 0.0f)),
  SOUL_FLAME("soul", "Soul Flame", Particle.SOUL_FIRE_FLAME,
    ParticleSettings.of(0.15, 0.1, 0.15, 1, 0.0f)),
  ENCHANTED_HIT("ehit", "Enchanted Hit", Particle.ENCHANTED_HIT,
    ParticleSettings.of(0.2, 0.1, 0.2, 1, 0.0f)),
  CRIT("crit", "Crit", Particle.CRIT,
    ParticleSettings.of(0.2, 0.1, 0.2, 1, 0.0f)),
  ASH("ash", "Ash", Particle.ASH,
    ParticleSettings.of(0.1, 0.1, 0.1, 1, 1.0f)),
  DRAGON_BREATH("dragon", "Dragon Breath", Particle.DRAGON_BREATH,
    ParticleSettings.of(0.0, -0.5, 0.0, 0, 1.0f)),
  END_ROD("end", "End Rod", Particle.END_ROD,
    ParticleSettings.of(0.12, 0.06, 0.12, 1, 0.0f)),
  CHERRY_LEAVES("cherry", "Cherry Blossom", Particle.CHERRY_LEAVES,
    ParticleSettings.of(0.35, 0.15, 0.35, 1, 0.0f));

  private static final String PERMISSION_PREFIX = "flight.particle.";

  private final String literal;
  private final String displayName;
  private final Particle particle;
  private final ParticleSettings defaultSettings;

  FlightParticleType(String literal, String displayName, Particle particle, ParticleSettings defaultSettings) {
    this.literal = literal;
    this.displayName = displayName;
    this.particle = particle;
    this.defaultSettings = defaultSettings;
  }

  public String literal() {
    return literal;
  }

  public String displayName() {
    return displayName;
  }

  public Particle particle() {
    return particle;
  }

  /**
   * The values the customizer opens with, and -- unless {@link #hasDynamicDefault()}
   * -- exactly what the default trail renders. Values that the renderer used to
   * leave to {@code ParticleBuilder}'s own defaults are spelled out here, because
   * a slider has no way to express "unset".
   */
  public ParticleSettings defaultSettings() {
    return defaultSettings;
  }

  /**
   * True when the default trail cannot be expressed as settings at all, because
   * it is recomputed from the player's velocity every tick. Dragon Breath used
   * to belong here too, but once count could reach 0 its default became an
   * ordinary direction vector like any other.
   */
  public boolean hasDynamicDefault() {
    return this == CRIT || this == ENCHANTED_HIT;
  }

  public String permission() {
    return this == DUST ? null : PERMISSION_PREFIX + literal;
  }

  public boolean isAllowed(Permissible permissible) {
    String perm = permission();
    return perm == null || permissible.hasPermission(perm);
  }

  public static List<FlightParticleType> allowedFor(Permissible permissible) {
    return Arrays.stream(values())
      .filter(type -> type.isAllowed(permissible))
      .collect(Collectors.toList());
  }

  public static FlightParticleType fromLiteral(String literal) {
    return Arrays.stream(values())
      .filter(type -> type.literal.equals(literal.toLowerCase(Locale.ROOT)))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unknown particle literal: " + literal));
  }
}
