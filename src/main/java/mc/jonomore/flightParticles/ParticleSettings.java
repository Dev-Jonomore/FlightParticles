package mc.jonomore.flightParticles;

import org.bukkit.Color;

/**
 * Every tunable value for one particle.
 *
 * <p>The canonical constructor clamps, so an out-of-range instance cannot exist.
 * That is deliberate: dialog responses, persisted values, and legacy data all
 * funnel through here, and none of them are trustworthy on their own.
 */
public record ParticleSettings(
    double offsetX, double offsetY, double offsetZ,
    int count,
    float speed,
    int rgb,
    float size) {

  public static final double OFFSET_MIN = -2.0;
  public static final double OFFSET_MAX = 2.0;
  public static final float OFFSET_STEP = 0.05f;

  // Zero is not "no particles" -- it switches offset from a spread radius to a
  // direction vector and speed from drift to launch velocity.
  public static final int COUNT_MIN = 0;
  public static final int COUNT_MAX = 8;

  public static final float SPEED_MIN = 0.0f;
  public static final float SPEED_MAX = 2.0f;
  public static final float SPEED_STEP = 0.05f;

  public static final float SIZE_MIN = 0.1f;
  public static final float SIZE_MAX = 4.0f;
  public static final float SIZE_STEP = 0.1f;

  // Carried by every non-Dust type so the record stays a single shape; never read.
  private static final int INERT_RGB = 0xFFFFFF;
  private static final float INERT_SIZE = 1.0f;

  public ParticleSettings {
    offsetX = clamp(offsetX, OFFSET_MIN, OFFSET_MAX);
    offsetY = clamp(offsetY, OFFSET_MIN, OFFSET_MAX);
    offsetZ = clamp(offsetZ, OFFSET_MIN, OFFSET_MAX);
    count = (int) clamp(count, COUNT_MIN, COUNT_MAX);
    speed = (float) clamp(speed, SPEED_MIN, SPEED_MAX);
    rgb = rgb & 0xFFFFFF;
    size = (float) clamp(size, SIZE_MIN, SIZE_MAX);
  }

  /** Settings for a particle that carries no data object -- everything except Dust. */
  public static ParticleSettings of(double offsetX, double offsetY, double offsetZ, int count, float speed) {
    return new ParticleSettings(offsetX, offsetY, offsetZ, count, speed, INERT_RGB, INERT_SIZE);
  }

  /** Settings for {@link FlightParticleType#DUST}, the only type with a data object. */
  public static ParticleSettings dust(
      double offsetX, double offsetY, double offsetZ, int count, float speed, int rgb, float size) {
    return new ParticleSettings(offsetX, offsetY, offsetZ, count, speed, rgb, size);
  }

  public Color color() {
    return Color.fromRGB(rgb);
  }

  /** NaN folds to the minimum -- a modified client can put one in any float field. */
  private static double clamp(double value, double min, double max) {
    if (Double.isNaN(value)) {
      return min;
    }
    return Math.min(max, Math.max(min, value));
  }
}
