package mc.jonomore.flightParticles;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public final class FlightListener implements Listener {

  private final FlightParticles plugin;

  public FlightListener(FlightParticles plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
    Player player = event.getPlayer();

    if (event.isFlying()) {
      plugin.onPlayerStartFlying(player.getUniqueId());
    } else {
      plugin.onPlayerStopFlying(player.getUniqueId());
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    plugin.applyFlightPermission(event.getPlayer());
    // Cheap no-op for anyone already on the container format.
    plugin.storage().migrate(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerPostRespawn(PlayerPostRespawnEvent event) {
    plugin.applyFlightPermission(event.getPlayer());
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    plugin.onPlayerStopFlying(event.getPlayer().getUniqueId());
  }
}
