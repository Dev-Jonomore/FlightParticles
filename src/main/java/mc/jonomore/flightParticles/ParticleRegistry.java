package mc.jonomore.flightParticles;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Every configured {@link FlightParticleType}, in config order.
 *
 * <p>A bad entry is logged and skipped rather than failing the plugin, so one
 * typo cannot take every trail down with it.
 */
public final class ParticleRegistry {

  static final String PERMISSION_PREFIX = "flight.particle.";
  static final String PERMISSION_WILDCARD = PERMISSION_PREFIX + "*";

  private static final String FALLBACK_ID = "dust";
  // The id doubles as a NamespacedKey value and a command argument.
  private static final Pattern ID = Pattern.compile("[a-z0-9._-]+");

  private final Map<String, FlightParticleType> types;
  private final FlightParticleType defaultType;

  private ParticleRegistry(Map<String, FlightParticleType> types, FlightParticleType defaultType) {
    this.types = types;
    this.defaultType = defaultType;
  }

  public List<FlightParticleType> values() {
    return List.copyOf(types.values());
  }

  /** Null when no such particle is configured. */
  public FlightParticleType get(String literal) {
    return literal == null ? null : types.get(literal.toLowerCase(Locale.ROOT));
  }

  /** The trail new players start on and everyone falls back to; never permission-gated. */
  public FlightParticleType defaultType() {
    return defaultType;
  }

  public List<FlightParticleType> allowedFor(Permissible permissible) {
    return types.values().stream().filter(type -> type.isAllowed(permissible)).toList();
  }

  public static ParticleRegistry load(FlightParticles plugin) {
    Logger log = plugin.getLogger();
    Map<String, FlightParticleType> types = new LinkedHashMap<>();

    ConfigurationSection section = plugin.getConfig().getConfigurationSection("particles");
    if (section != null) {
      for (String id : section.getKeys(false)) {
        ConfigurationSection entry = section.getConfigurationSection(id);
        if (entry == null) {
          log.warning("Particle '" + id + "' is not a section; skipping.");
          continue;
        }
        try {
          types.put(id, parse(id, entry));
        } catch (IllegalArgumentException e) {
          log.warning("Skipping particle '" + id + "': " + e.getMessage());
        }
      }
    }

    String defaultId = plugin.getConfig().getString("default-particle", FALLBACK_ID);
    FlightParticleType defaultType = types.get(defaultId);
    if (defaultType == null && !types.isEmpty()) {
      log.warning("default-particle '" + defaultId + "' is not a configured particle; using the first one.");
      defaultType = types.values().iterator().next();
    }
    if (defaultType == null) {
      // Nothing usable in the file: fall back to a plain dust trail so the plugin still works.
      log.severe("No valid particles in config.yml; using a built-in Dust trail.");
      defaultType = new FlightParticleType(FALLBACK_ID, "Dust", Particle.DUST,
        ParticleSettings.dust(0.2, 0.0, 0.2, 1, 1.0f, 0x6E2970, 1.2f), null, false, null);
      types.put(FALLBACK_ID, defaultType);
    }
    if (defaultType.permission() != null) {
      // Everything falls back to the default, so it has to be usable by everyone.
      log.warning("The default particle '" + defaultType.literal() + "' cannot require a permission; ignoring it.");
      defaultType = new FlightParticleType(defaultType.literal(), defaultType.displayName(),
        defaultType.particle(), defaultType.defaultSettings(), null,
        defaultType.velocityTrail(), defaultType.data());
      types.put(defaultType.literal(), defaultType);
    }

    registerPermissions(plugin, types.values());
    return new ParticleRegistry(types, defaultType);
  }

  private static FlightParticleType parse(String id, ConfigurationSection entry) {
    if (!ID.matcher(id).matches()) {
      throw new IllegalArgumentException("ids may only contain a-z, 0-9, '.', '_' and '-'");
    }

    String particleName = entry.getString("particle");
    if (particleName == null) {
      throw new IllegalArgumentException("missing 'particle'");
    }
    NamespacedKey key = NamespacedKey.fromString(particleName.toLowerCase(Locale.ROOT));
    Particle particle = key == null ? null : Registry.PARTICLE_TYPE.get(key);
    if (particle == null) {
      throw new IllegalArgumentException("unknown particle '" + particleName + "'");
    }

    Float data = entry.contains("data") ? (float) entry.getDouble("data") : null;
    Class<?> dataType = particle.getDataType();
    if (dataType == Float.class) {
      if (data == null) {
        throw new IllegalArgumentException(particleName + " needs a float 'data' value");
      }
    } else {
      if (data != null) {
        throw new IllegalArgumentException(particleName + " does not take 'data'");
      }
      // Anything else that needs data (blocks, items, other colors) cannot be described here.
      if (dataType != Void.class && particle != Particle.DUST) {
        throw new IllegalArgumentException(particleName + " needs data that config cannot provide");
      }
    }

    String permission = entry.getString("permission", PERMISSION_PREFIX + id);
    if (permission.isBlank() || permission.equalsIgnoreCase("none")) {
      permission = null;
    }

    return new FlightParticleType(
      id,
      entry.getString("name", id),
      particle,
      parseSettings(entry.getConfigurationSection("settings"), particle == Particle.DUST),
      permission,
      entry.getBoolean("velocity-trail", false),
      data);
  }

  private static ParticleSettings parseSettings(ConfigurationSection s, boolean color) {
    double x = 0.1, y = 0.1, z = 0.1;
    int count = 1;
    float speed = 0.0f;
    int rgb = 0xFFFFFF;
    float size = 1.0f;

    if (s != null) {
      x = s.getDouble("offset.x", x);
      y = s.getDouble("offset.y", y);
      z = s.getDouble("offset.z", z);
      count = s.getInt("count", count);
      speed = (float) s.getDouble("speed", speed);
      if (color) {
        if (s.contains("color")) {
          OptionalInt parsed = HexColor.parse(s.getString("color"));
          if (parsed.isEmpty()) {
            throw new IllegalArgumentException("'color' must be a hex color like #6E2970");
          }
          rgb = parsed.getAsInt();
        }
        size = (float) s.getDouble("size", size);
      }
    }
    return color
      ? ParticleSettings.dust(x, y, z, count, speed, rgb, size)
      : ParticleSettings.of(x, y, z, count, speed);
  }

  /**
   * The nodes are only known once the config is read, so plugin.yml cannot declare them.
   * Matches its other nodes: op by default, and children of {@code flight.particle.*}.
   */
  private static void registerPermissions(FlightParticles plugin, Iterable<FlightParticleType> types) {
    var pm = plugin.getServer().getPluginManager();
    for (FlightParticleType type : types) {
      String node = type.permission();
      if (node == null) {
        continue;
      }
      Permission permission = pm.getPermission(node);
      if (permission == null) {
        permission = new Permission(node, "Use the " + type.displayName() + " flight trail.", PermissionDefault.OP);
        pm.addPermission(permission);
      }
      permission.addParent(PERMISSION_WILDCARD, true);
    }
  }
}
