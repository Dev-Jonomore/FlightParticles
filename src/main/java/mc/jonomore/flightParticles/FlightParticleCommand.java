package mc.jonomore.flightParticles;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.strokkur.commands.Aliases;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.Literal;
import net.strokkur.commands.paper.Description;
import net.strokkur.commands.paper.Executor;
import org.bukkit.entity.Player;

@Command("flightparticle")
@Aliases({ "fp", "particles", "particle" })
@Description("Choose your flight trail particle")
@FlightPermission
public final class FlightParticleCommand {

  private final FlightParticles plugin;

  public FlightParticleCommand(FlightParticles plugin) {
    this.plugin = plugin;
  }

  @Executes
  void showCurrent(@Executor Player player) {
    FlightParticleType type = plugin.getPlayerParticleType(player);
    player.sendRichMessage(
        "<gray>Your flight trail particle: <white><type><gray>. Options: dust, flame, spark, soul, ehit, crit, ash, dragon",
        Placeholder.unparsed("type", type.displayName()));
  }

  @Executes("set")
  void setParticle(@Executor Player player, @Literal({ "dust", "flame", "spark", "soul", "ehit", "crit", "ash", "dragon" }) String particle) {
    FlightParticleType type = FlightParticleType.fromLiteral(particle);
    plugin.setPlayerParticleType(player, type);
    player.sendRichMessage(
        "<green>Flight trail particle set to <white><type><green>!",
        Placeholder.unparsed("type", type.displayName()));
  }

  @Executes("reset")
  void resetParticle(@Executor Player player) {
    FlightParticleType type = FlightParticleType.fromLiteral("dust");
    plugin.setPlayerParticleType(player, type);
    player.sendRichMessage(
      "<green>Flight trail particle set to <white><type><green>!",
      Placeholder.unparsed("type", type.displayName()));
  }
}
