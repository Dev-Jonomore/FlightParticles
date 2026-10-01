package mc.jonomore.flightParticles;

import com.google.gson.Gson;

/**
 * Stores all configurable parameters for a single particle type's flight trail.
 * Serialised as JSON and stored in the player's PersistentDataContainer.
 */
public class ParticleSettings {

  private static final Gson GSON = new Gson();

  // ── Shared ──────────────────────────────────────────────────────────────
  public double offsetX;
  public double offsetY;
  public double offsetZ;
  public int    count;
  public double extra;     // speed / "extra" parameter

  // ── DUST-only ───────────────────────────────────────────────────────────
  public int   red;
  public int   green;
  public int   blue;
  public float size;

  // ── CRIT / ENCHANTED_HIT ────────────────────────────────────────────────
  public boolean useVelocityOffset;
  public double  velocityMultiplier;

  // ── DRAGON_BREATH ────────────────────────────────────────────────────────
  public float dataValue;

  // ── Serialization ────────────────────────────────────────────────────────

  public String toJson() {
    return GSON.toJson(this);
  }

  public static ParticleSettings fromJson(String json) {
    return GSON.fromJson(json, ParticleSettings.class);
  }

  // ── Defaults (mirrors the original hardcoded switch statement) ───────────

  public static ParticleSettings createDefault(FlightParticleType type) {
    ParticleSettings s = new ParticleSettings();
    switch (type) {
      case DUST -> {
        s.red = 110; s.green = 41; s.blue = 112; // PLUM_COLOR
        s.size    = 1.2f;
        s.offsetX = 0.2; s.offsetY = 0.0; s.offsetZ = 0.2;
        s.count   = 0;
        s.extra   = 0.0;
      }
      case FLAME, SOUL_FLAME -> {
        s.offsetX = 0.15; s.offsetY = 0.1; s.offsetZ = 0.15;
        s.count   = 1;
        s.extra   = 0.0;
      }
      case SPARK -> {
        s.offsetX = 0.4; s.offsetY = 0.2; s.offsetZ = 0.4;
        s.count   = 1;
        s.extra   = 0.0;
      }
      case ASH -> {
        s.offsetX = 0.1; s.offsetY = 0.1; s.offsetZ = 0.1;
        s.count   = 1;
        s.extra   = 0.0;
      }
      case CRIT, ENCHANTED_HIT -> {
        s.useVelocityOffset  = true;
        s.velocityMultiplier = 1.0;
        s.offsetX = 0.0; s.offsetY = 0.0; s.offsetZ = 0.0;
        s.count   = 0;
        s.extra   = 0.0;
      }
      case DRAGON_BREATH -> {
        s.offsetX  = 0.0; s.offsetY = -0.5; s.offsetZ = 0.0;
        s.count    = 0;
        s.extra    = 0.0;
        s.dataValue = 0.5f;
      }
    }
    return s;
  }
}
