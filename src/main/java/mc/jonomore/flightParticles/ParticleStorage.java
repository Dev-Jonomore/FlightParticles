package mc.jonomore.flightParticles;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Reads and writes the player's trail choice and per-particle settings.
 *
 * <p>Two keys, both nested containers: {@code selection} holds what is being
 * rendered, {@code settings} holds a map of particle literal to that particle's
 * tuned values. Nesting rather than one top-level key per particle keeps the
 * shared PDC namespace clean, gives the schema stamp somewhere to live, and
 * makes a full wipe a single removal.
 *
 * <p>Missing sub-keys fall back to the type's defaults, so adding a field later
 * does not invalidate anything already stored.
 */
public final class ParticleStorage {

  /**
   * What the player picked, before permissions are applied.
   */
  public record Selection(FlightParticleType type, boolean custom, boolean enabled) {
    public Selection withType(FlightParticleType type) {
      return new Selection(type, custom, enabled);
    }

    public Selection withCustom(boolean custom) {
      return new Selection(type, custom, enabled);
    }

    public Selection withEnabled(boolean enabled) {
      return new Selection(type, custom, enabled);
    }
  }

  private static final int SCHEMA = 1;

  private final FlightParticles plugin;
  private final NamespacedKey selectionKey;
  private final NamespacedKey settingsKey;
  /** Pre-container format: a bare particle literal under the original key. */
  private final NamespacedKey legacyKey;

  private final NamespacedKey schemaKey;
  private final NamespacedKey idKey;
  private final NamespacedKey customKey;
  private final NamespacedKey enabledKey;

  private final NamespacedKey offsetXKey;
  private final NamespacedKey offsetYKey;
  private final NamespacedKey offsetZKey;
  private final NamespacedKey countKey;
  private final NamespacedKey speedKey;
  private final NamespacedKey rgbKey;
  private final NamespacedKey sizeKey;

  public ParticleStorage(FlightParticles plugin) {
    this.plugin = plugin;
    selectionKey = new NamespacedKey(plugin, "selection");
    settingsKey = new NamespacedKey(plugin, "settings");
    legacyKey = new NamespacedKey(plugin, "flight_particle");

    schemaKey = new NamespacedKey(plugin, "schema");
    idKey = new NamespacedKey(plugin, "id");
    customKey = new NamespacedKey(plugin, "custom");
    enabledKey = new NamespacedKey(plugin, "enabled");

    offsetXKey = new NamespacedKey(plugin, "offset_x");
    offsetYKey = new NamespacedKey(plugin, "offset_y");
    offsetZKey = new NamespacedKey(plugin, "offset_z");
    countKey = new NamespacedKey(plugin, "count");
    speedKey = new NamespacedKey(plugin, "speed");
    rgbKey = new NamespacedKey(plugin, "rgb");
    sizeKey = new NamespacedKey(plugin, "size");
  }

  /** A fresh selection of the configured default trail, for players with nothing (usable) stored. */
  private Selection defaultSelection() {
    return new Selection(plugin.particles().defaultType(), false, true);
  }

  private NamespacedKey typeKey(FlightParticleType type) {
    return new NamespacedKey(plugin, type.literal());
  }

  public Selection getSelection(Player player) {
    PersistentDataContainer root = player.getPersistentDataContainer();
    PersistentDataContainer stored = root.get(selectionKey, PersistentDataType.TAG_CONTAINER);
    if (stored == null) {
      return legacySelection(root);
    }

    FlightParticleType type = parseType(stored.get(idKey, PersistentDataType.STRING));
    if (type == null) {
      return defaultSelection();
    }
    return new Selection(
      type,
      stored.getOrDefault(customKey, PersistentDataType.BOOLEAN, false),
      stored.getOrDefault(enabledKey, PersistentDataType.BOOLEAN, true));
  }

  public void setSelection(Player player, Selection selection) {
    PersistentDataContainer root = player.getPersistentDataContainer();
    PersistentDataContainer stored = root.getAdapterContext().newPersistentDataContainer();

    stored.set(schemaKey, PersistentDataType.INTEGER, SCHEMA);
    stored.set(idKey, PersistentDataType.STRING, selection.type().literal());
    stored.set(customKey, PersistentDataType.BOOLEAN, selection.custom());
    stored.set(enabledKey, PersistentDataType.BOOLEAN, selection.enabled());

    root.set(selectionKey, PersistentDataType.TAG_CONTAINER, stored);
    // Any write is a migration; the legacy key must not outlive the new one.
    root.remove(legacyKey);
  }

  /** Switches to a particle's stock settings, leaving visibility alone. */
  public void selectPreset(Player player, FlightParticleType type) {
    setSelection(player, getSelection(player).withType(type).withCustom(false));
  }

  /** Switches to the player's tuned settings for a particle, leaving visibility alone. */
  public void selectCustom(Player player, FlightParticleType type) {
    setSelection(player, getSelection(player).withType(type).withCustom(true));
  }

  /** Shows or hides the trail without disturbing which particle is chosen. */
  public void setEnabled(Player player, boolean enabled) {
    setSelection(player, getSelection(player).withEnabled(enabled));
  }

  /** The player's tuned values for a particle, or its defaults when it has never been customized. */
  public ParticleSettings getSettings(Player player, FlightParticleType type) {
    ParticleSettings defaults = type.defaultSettings();
    PersistentDataContainer stored = settingsFor(player, type);
    if (stored == null) {
      return defaults;
    }
    return new ParticleSettings(
      stored.getOrDefault(offsetXKey, PersistentDataType.DOUBLE, defaults.offsetX()),
      stored.getOrDefault(offsetYKey, PersistentDataType.DOUBLE, defaults.offsetY()),
      stored.getOrDefault(offsetZKey, PersistentDataType.DOUBLE, defaults.offsetZ()),
      stored.getOrDefault(countKey, PersistentDataType.INTEGER, defaults.count()),
      stored.getOrDefault(speedKey, PersistentDataType.FLOAT, defaults.speed()),
      stored.getOrDefault(rgbKey, PersistentDataType.INTEGER, defaults.rgb()),
      stored.getOrDefault(sizeKey, PersistentDataType.FLOAT, defaults.size()));
  }

  public boolean hasCustomSettings(Player player, FlightParticleType type) {
    return settingsFor(player, type) != null;
  }

  public void setSettings(Player player, FlightParticleType type, ParticleSettings settings) {
    PersistentDataContainer root = player.getPersistentDataContainer();
    PersistentDataContainer all = root.getOrDefault(
      settingsKey, PersistentDataType.TAG_CONTAINER, root.getAdapterContext().newPersistentDataContainer());
    PersistentDataContainer one = root.getAdapterContext().newPersistentDataContainer();

    one.set(offsetXKey, PersistentDataType.DOUBLE, settings.offsetX());
    one.set(offsetYKey, PersistentDataType.DOUBLE, settings.offsetY());
    one.set(offsetZKey, PersistentDataType.DOUBLE, settings.offsetZ());
    one.set(countKey, PersistentDataType.INTEGER, settings.count());
    one.set(speedKey, PersistentDataType.FLOAT, settings.speed());
    // Only Dust reads these; writing them everywhere would just be noise in the NBT.
    if (type.hasColor()) {
      one.set(rgbKey, PersistentDataType.INTEGER, settings.rgb());
      one.set(sizeKey, PersistentDataType.FLOAT, settings.size());
    }

    all.set(typeKey(type), PersistentDataType.TAG_CONTAINER, one);
    root.set(settingsKey, PersistentDataType.TAG_CONTAINER, all);
  }

  public void clearSettings(Player player, FlightParticleType type) {
    PersistentDataContainer root = player.getPersistentDataContainer();
    PersistentDataContainer all = root.get(settingsKey, PersistentDataType.TAG_CONTAINER);
    if (all == null) {
      return;
    }
    all.remove(typeKey(type));
    root.set(settingsKey, PersistentDataType.TAG_CONTAINER, all);
  }

  /** Rewrites a pre-container particle string into the current format. Returns whether anything changed. */
  public boolean migrate(Player player) {
    PersistentDataContainer root = player.getPersistentDataContainer();
    if (!root.has(legacyKey, PersistentDataType.STRING)) {
      return false;
    }
    setSelection(player, legacySelection(root));
    return true;
  }

  private Selection legacySelection(PersistentDataContainer root) {
    FlightParticleType type = parseType(root.get(legacyKey, PersistentDataType.STRING));
    return type == null ? defaultSelection() : new Selection(type, false, true);
  }

  private PersistentDataContainer settingsFor(Player player, FlightParticleType type) {
    PersistentDataContainer all =
      player.getPersistentDataContainer().get(settingsKey, PersistentDataType.TAG_CONTAINER);
    return all == null ? null : all.get(typeKey(type), PersistentDataType.TAG_CONTAINER);
  }

  /** Null for absent or no longer known literals; both mean "fall back". */
  private FlightParticleType parseType(String literal) {
    return plugin.particles().get(literal);
  }
}
