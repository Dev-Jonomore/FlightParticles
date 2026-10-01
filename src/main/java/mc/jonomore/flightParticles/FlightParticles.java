package mc.jonomore.flightParticles;

import com.destroystokyo.paper.ParticleBuilder;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.util.Vector;

public final class FlightParticles extends JavaPlugin {

  public static final String FLIGHT_PERMISSION = "lobby.flight";
  public static final String FLIGHT_COMMAND_PERMISSION = "flight.command";
  public static final String RESET_ALL_PERMISSION = "flight.command.resetall";
  public static final String SETTINGS_PERMISSION = "flight.settings";

  private final ParticleStorage storage = new ParticleStorage(this);
  private ParticleRegistry particles;
  private final Set<UUID> flyingPlayers = ConcurrentHashMap.newKeySet();
  private BukkitTask globalTask = null;

  @FlightPermission
  public static boolean mayRun(CommandSourceStack source) {
    return source.getSender().hasPermission(FLIGHT_COMMAND_PERMISSION);
  }

  @Override
  public void onLoad() {
    getLifecycleManager().registerEventHandler(
        LifecycleEvents.COMMANDS.newHandler(event -> {
          FlightParticleCommandBrigadier.register(event.registrar(), this);
          FlyCommandBrigadier.register(event.registrar());
        })
    );
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    particles = ParticleRegistry.load(this);
    getServer().getPluginManager().registerEvents(new FlightListener(this), this);
    subscribeToPermissionChanges();
  }

  public ParticleStorage storage() {
    return storage;
  }

  public ParticleRegistry particles() {
    return particles;
  }

  /** Syncs allowFlight with {@link #FLIGHT_PERMISSION}; creative and spectator manage flight themselves. */
  void applyFlightPermission(Player player) {
    GameMode mode = player.getGameMode();
    if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
      return;
    }
    boolean allowed = player.hasPermission(FLIGHT_PERMISSION);
    if (player.getAllowFlight() != allowed) {
      player.setAllowFlight(allowed);
    }
  }

  private void subscribeToPermissionChanges() {
    RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
    if (provider == null) {
      getLogger().warning("LuckPerms not found — flight permission changes will only apply on join or respawn.");
      return;
    }

    // Recalculate rather than node-mutate, so group and inheritance changes are covered too.
    provider.getProvider().getEventBus().subscribe(this, UserDataRecalculateEvent.class, event -> {
      UUID uuid = event.getUser().getUniqueId();
      // LuckPerms fires this off the main thread; flight state must not be touched there.
      getServer().getScheduler().runTask(this, () -> {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
          applyFlightPermission(player);
        }
      });
    });
  }

  void onPlayerStartFlying(UUID uuid) {
    flyingPlayers.add(uuid);
    startGlobalTaskIfNeeded();
  }

  void onPlayerStopFlying(UUID uuid) {
    if (flyingPlayers.remove(uuid)) {
      stopGlobalTaskIfEmpty();
    }
  }

  /**
   * The selection as it will actually render. A stored type the player may no
   * longer use falls back to Dust, and custom settings fall back to defaults
   * without being deleted -- a temporary rank lapse should not cost someone
   * their tuning.
   */
  ParticleStorage.Selection getEffectiveSelection(Player player) {
    ParticleStorage.Selection selection = storage.getSelection(player);
    FlightParticleType type = selection.type().isAllowed(player) ? selection.type() : particles.defaultType();
    boolean custom = selection.custom()
        && type == selection.type()
        && player.hasPermission(SETTINGS_PERMISSION);
    return selection.withType(type).withCustom(custom);
  }

  FlightParticleType getPlayerParticleType(Player player) {
    return getEffectiveSelection(player).type();
  }

  void setPlayerParticleType(Player player, FlightParticleType type) {
    storage.selectPreset(player, type);
  }

  /** Clears the stored particle of every online player who may no longer use it, returning the count. */
  int resetUnpermittedParticles() {
    int reset = 0;
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (storage.getSelection(player).type().isAllowed(player)) {
        continue;
      }
      // Only the selection is walked back; saved settings survive.
      storage.selectPreset(player, particles.defaultType());
      reset++;
    }
    return reset;
  }

  /** Rewrites every online player still on the pre-container format, returning the count. */
  int migrateLegacyParticles() {
    int migrated = 0;
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (storage.migrate(player)) {
        migrated++;
      }
    }
    return migrated;
  }

  /**
   * Instantiates the single repeating scheduler task if it is not already
   * running.
   */
  private void startGlobalTaskIfNeeded() {
    if (globalTask != null)
      return;

    // Run every 2 ticks (10 times a second)
    globalTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
      for (UUID uuid : flyingPlayers) {
        Player player = Bukkit.getPlayer(uuid);

        // Validation check to prune invalid or un-tracked player states
        if (player == null || !player.isOnline() || !player.isFlying()) {
          flyingPlayers.remove(uuid);
          continue;
        }

        // Spectators stay tracked so the trail resumes when they leave the gamemode.
        if (player.getGameMode() == GameMode.SPECTATOR) {
          continue;
        }

        ParticleStorage.Selection selection = getEffectiveSelection(player);
        if (!selection.enabled()) {
          continue;
        }

        spawnFlightTrail(player, selection.type(), selection.custom());
      }
      stopGlobalTaskIfEmpty();
    }, 0L, 2L);
  }

  private void spawnFlightTrail(Player player, FlightParticleType type, boolean custom) {
    Location location = player.getLocation().add(0, 0.1, 0);
    ParticleBuilder builder = type.particle().builder().location(location).receivers(32, true);

    if (!custom && type.hasDynamicDefault()) {
      applyDynamicDefault(player, type, builder);
    } else {
      applySettings(type, custom ? storage.getSettings(player, type) : type.defaultSettings(), builder);
    }

    builder.spawn();
  }

  /**
   * The defaults that cannot come from sliders, because they are recomputed from
   * the player's velocity every tick.
   */
  private void applyDynamicDefault(Player player, FlightParticleType type, ParticleBuilder builder) {
    Vector velocity = player.getVelocity().clone().multiply(-1);
    builder
      .offset(velocity.getX(), -0.1, velocity.getZ())
      .count(0);
  }

  private static void applySettings(FlightParticleType type, ParticleSettings settings, ParticleBuilder builder) {
    builder
      .offset(settings.offsetX(), settings.offsetY(), settings.offsetZ())
      .count(settings.count())
      .extra(settings.speed());

    if (type.hasColor()) {
      builder.color(settings.color(), settings.size());
    } else if (type.data() != null) {
      // A fixed characteristic of this trail rather than a tunable, so both modes get it.
      builder.data(type.data());
    }
  }

  /**
   * Terminating function that kills the global loop to preserve CPU cycles.
   */
  private void stopGlobalTaskIfEmpty() {
    if (flyingPlayers.isEmpty() && globalTask != null) {
      globalTask.cancel();
      globalTask = null;
    }
  }
}
