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

import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public final class FlightParticles extends JavaPlugin {

  public static final String FLIGHT_PERMISSION = "lobby.flight";
  public static final String FLIGHT_COMMAND_PERMISSION = "flight.command";
  public static final String RESET_ALL_PERMISSION = "flight.command.resetall";

  private static final UUID NIT1KING = UUID.fromString("b96e4a52-a359-4f33-8a81-bc6236f321b8");
  private static final Color PLUM_COLOR = Color.fromRGB(110, 41, 112);

  private final NamespacedKey particleKey = new NamespacedKey(this, "flight_particle");
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
    getServer().getPluginManager().registerEvents(new FlightListener(this), this);
    subscribeToPermissionChanges();
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

  FlightParticleType getPlayerParticleType(Player player) {
    FlightParticleType type = getStoredParticleType(player);
    // A stored type the player no longer has permission for is not honored.
    return type != null && type.isAllowed(player) ? type : FlightParticleType.DUST;
  }

  void setPlayerParticleType(Player player, FlightParticleType type) {
    player.getPersistentDataContainer().set(particleKey, PersistentDataType.STRING, type.literal());
  }

  /** Clears the stored particle of every online player who may no longer use it, returning the count. */
  int resetUnpermittedParticles() {
    int reset = 0;
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!player.getPersistentDataContainer().has(particleKey, PersistentDataType.STRING)) {
        continue;
      }
      FlightParticleType type = getStoredParticleType(player);
      if (type != null && type.isAllowed(player)) {
        continue;
      }
      player.getPersistentDataContainer().remove(particleKey);
      reset++;
    }
    return reset;
  }

  /** The persisted particle, or null when absent or no longer a known type. */
  private FlightParticleType getStoredParticleType(Player player) {
    String literal = player.getPersistentDataContainer().get(particleKey, PersistentDataType.STRING);
    if (literal == null) {
      return null;
    }
    try {
      return FlightParticleType.fromLiteral(literal);
    } catch (IllegalArgumentException _) {
      return null;
    }
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

        spawnFlightTrail(player, getPlayerParticleType(player));
      }
      stopGlobalTaskIfEmpty();
    }, 0L, 2L);
  }

  private void spawnFlightTrail(Player player, FlightParticleType type) {
    Location location = player.getLocation().add(0, 0.1, 0);
    ParticleBuilder builder = type.particle().builder().location(location).receivers(32, true);

    switch (type) {
      case DUST -> builder
          .color(player.getUniqueId().equals(NIT1KING) ? Color.ORANGE : PLUM_COLOR, 1.2f)
          .offset(0.2, 0.0, 0.2);
      case FLAME, SOUL_FLAME -> builder
          .offset(0.15, 0.1, 0.15)
          .count(1)
          .extra(0.0);
      case SPARK -> builder
          .offset(0.4, 0.2, 0.4)
          .count(1)
          .extra(0.0);
      case ASH -> builder
          .offset(0.1, 0.1, 0.1)
          .count(1);
      case CRIT, ENCHANTED_HIT -> {
        Vector velocity = player.getVelocity().clone().multiply(-1);
        builder
          .offset(velocity.getX(), -0.1, velocity.getZ())
          .count(0);
      }
      case DRAGON_BREATH -> builder
        .offset(0.0, -0.5, 0.0)
        .count(0)
        .data(0.5f);
    }

    builder.spawn();
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
