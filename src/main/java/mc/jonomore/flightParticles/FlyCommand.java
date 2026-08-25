package mc.jonomore.flightParticles;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.paper.Description;
import net.strokkur.commands.paper.Executor;
import net.strokkur.commands.permission.Permission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command("fly")
@Description("Toggle your ability to fly")
public final class FlyCommand {
  @Executes
  void toggleFlight(@Executor Player player) {
    if (player.isFlying()) {
      player.setFlying(false);
      player.setAllowFlight(false);
    } else if (player.getAllowFlight()) {
      player.setAllowFlight(false);
    } else if (player.hasPermission("lobby.flight")) {
      player.setFlying(true);
      player.setAllowFlight(true);
    }
  }

  @Executes("on")
  @Permission("fly.command.others")
  void enableFlightOther(CommandSender sender, Player target) {
    if (target.hasPermission("lobby.flight")) {
      if (target.getAllowFlight()) {
        sender.sendRichMessage("<red><dark_purple><b><player></b></dark_purple> can already fly lil bro...get a life!</red>",
          Placeholder.component("player", target.displayName())
        );
      } else {
        target.setAllowFlight(true);
        sender.sendRichMessage("<yellow>You made <dark_purple><b><player></b></dark_purple> fly</yellow>!",
          Placeholder.component("player", target.displayName())
        );
      }
    } else {
      sender.sendRichMessage("<red><dark_purple><b><player></b></dark_purple> doesn't have flight perms lil bro!</red>",
        Placeholder.component("player", target.displayName())
      );
    }
  }

  @Executes("off")
  @Permission("fly.command.others")
  void disableFlightOther(CommandSender sender, Player target) {
    if (target.hasPermission("lobby.flight")) {
      if (!target.getAllowFlight()) {
        sender.sendRichMessage("<red><dark_purple><b><player></b></dark_purple> already can't fly lil bro...get a life!</red>",
          Placeholder.component("player", target.displayName())
        );
      } else {
        target.setAllowFlight(false);
        sender.sendRichMessage("<yellow>You cooked <dark_purple><b><player>'s</b></dark_purple> flight abilities</yellow>!",
          Placeholder.component("player", target.displayName())
        );
      }
    } else {
      sender.sendRichMessage("<red><dark_purple><b><player></b></dark_purple> doesn't have flight perms lil bro!</red>",
        Placeholder.component("player", target.displayName())
      );
    }
  }
}
