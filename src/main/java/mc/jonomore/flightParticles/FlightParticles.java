package mc.jonomore.flightParticles;

import com.destroystokyo.paper.ParticleBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FlightParticles extends JavaPlugin implements Listener {

  private final Set<UUID> flyingPlayers = ConcurrentHashMap.newKeySet();
  private final ParticleBuilder flightTrail = Particle.DUST.builder();
  private BukkitTask globalTask = null;

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
    Player player = event.getPlayer();

    if (event.isFlying()) {
      flyingPlayers.add(player.getUniqueId());
      startGlobalTaskIfNeeded();
    } else {
      flyingPlayers.remove(player.getUniqueId());
      stopGlobalTaskIfEmpty();
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    if (event.getPlayer().hasPermission("lobby.flight")) event.getPlayer().setAllowFlight(true);
  }

  @EventHandler
  public void onPlayerRespawn(PlayerRespawnEvent event) {
    if (event.getPlayer().hasPermission("lobby.flight")) event.getPlayer().setAllowFlight(true);
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    // Prevent stale memory references if a player disconnects mid-flight
    if (flyingPlayers.remove(event.getPlayer().getUniqueId())) {
      stopGlobalTaskIfEmpty();
    }
  }

  /**
   * Instantiates the single repeating scheduler task if it is not already running.
   */
  private void startGlobalTaskIfNeeded() {
    if (globalTask != null) return;

    // Run every 2 ticks (10 times a second)
    globalTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
      for (UUID uuid : flyingPlayers) {
        Player player = Bukkit.getPlayer(uuid);

        // Validation check to prune invalid or un-tracked player states
        if (player == null || !player.isOnline() || !player.isFlying()) {
          flyingPlayers.remove(uuid);
          continue;
        }

        flightTrail.color(uuid.equals(UUID.fromString("b96e4a52-a359-4f33-8a81-bc6236f321b8")) ? Color.ORANGE : Color.fromRGB(110, 41, 112))
          .location(player.getLocation().add(0, 0.1, 0))
          .offset(0.2, 0.0, 0.2)
          .extra(0.02)
          .receivers(32, true)
          .spawn();
      }
      stopGlobalTaskIfEmpty();
    }, 0L, 2L);
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
