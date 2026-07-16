package mc.jonomore.flightParticles;

import org.bukkit.Particle;
import org.bukkit.permissions.Permissible;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public enum FlightParticleType {
  DUST("dust", "Dust", Particle.DUST),
  FLAME("flame", "Flame", Particle.FLAME),
  SPARK("spark", "Electric Spark", Particle.ELECTRIC_SPARK),
  SOUL_FLAME("soul", "Soul Flame", Particle.SOUL_FIRE_FLAME),
  ENCHANTED_HIT("ehit", "Enchanted Hit", Particle.ENCHANTED_HIT),
  CRIT("crit", "Crit", Particle.CRIT),
  ASH("ash", "Ash", Particle.ASH),
  DRAGON_BREATH("dragon", "Dragon Breath", Particle.DRAGON_BREATH);

  private static final String PERMISSION_PREFIX = "flight.particle.";

  private final String literal;
  private final String displayName;
  private final Particle particle;

  FlightParticleType(String literal, String displayName, Particle particle) {
    this.literal = literal;
    this.displayName = displayName;
    this.particle = particle;
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