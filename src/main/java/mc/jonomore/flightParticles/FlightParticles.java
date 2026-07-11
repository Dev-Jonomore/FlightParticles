package mc.jonomore.flightParticles;

import com.destroystokyo.paper.ParticleBuilder;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public final class FlightParticles extends JavaPlugin {

  public static final String FLIGHT_PERMISSION = "lobby.flight";

  private static final UUID NIT1KING = UUID.fromString("b96e4a52-a359-4f33-8a81-bc6236f321b8");
  private static final Color PLUM_COLOR = Color.fromRGB(110, 41, 112);

  private final NamespacedKey particleKey = new NamespacedKey(this, "flight_particle");
  private final Set<UUID> flyingPlayers = ConcurrentHashMap.newKeySet();
  private BukkitTask globalTask = null;

  @FlightPermission
  public static boolean mayRun(CommandSourceStack source) {
    return source.getSender().hasPermission(FLIGHT_PERMISSION);
  }

  @Override
  public void onLoad() {
    getLifecycleManager().registerEventHandler(
        LifecycleEvents.COMMANDS.newHandler(event -> FlightParticleCommandBrigadier.register(event.registrar(), this)));
  }

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(new FlightListener(this), this);
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
    String literal = player.getPersistentDataContainer().get(particleKey, PersistentDataType.STRING);
    if (literal == null) {
      return FlightParticleType.DUST;
    }
    try {
      return FlightParticleType.fromLiteral(literal);
    } catch (IllegalArgumentException _) {
      return FlightParticleType.DUST;
    }
  }

  void setPlayerParticleType(Player player, FlightParticleType type) {
    player.getPersistentDataContainer().set(particleKey, PersistentDataType.STRING, type.literal());
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
