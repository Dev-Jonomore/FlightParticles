package mc.jonomore.flightParticles;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.strokkur.commands.*;
import net.strokkur.commands.paper.Description;
import net.strokkur.commands.paper.Executor;
import net.strokkur.commands.permission.Permission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

@Command("flightparticle")
@Aliases({ "fp", "particles", "particle", "flightperms" })
@Description("Choose and customize your flight trail particle")
@FlightPermission
public final class FlightParticleCommand {

  private final FlightParticles plugin;

  public FlightParticleCommand(FlightParticles plugin) {
    this.plugin = plugin;
  }

  @Executes
  void showCurrent(@Executor Player player) {
    ParticleMenuDialog.show(plugin, player);
  }

  @ParticleSuggestions
  public static CompletableFuture<Suggestions> provide(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
    FlightParticleType.allowedFor(ctx.getSource().getSender()).stream()
      .map(FlightParticleType::literal)
      .forEach(builder::suggest);
    return builder.buildFuture();
  }

  @Executes("set")
  void setParticle(@Executor Player player, @ParticleSuggestions String particle) {
    FlightParticleType type = FlightParticleType.fromLiteral(particle);
    if (type == null || !type.isAllowed(player)) {
      player.sendRichMessage("<red>This particle is locked!");
      return;
    }
    plugin.setPlayerParticleType(player, type);
    player.sendRichMessage(
        "<green>Flight trail particle set to <dark_purple><b><type><green>!",
        Placeholder.unparsed("type", type.displayName()));
  }

  /** Direct entry into one particle's customizer, bypassing the menu. */
  @Executes("customize")
  void customizeParticle(@Executor Player player, @ParticleSuggestions String particle) {
    FlightParticleType type;
    try {
      type = FlightParticleType.fromLiteral(particle);
    } catch (IllegalArgumentException _) {
      player.sendRichMessage("<red>Unknown particle.");
      return;
    }
    if (!type.isAllowed(player)) {
      player.sendRichMessage("<red>This particle is locked!");
      return;
    }
    if (!player.hasPermission(FlightParticles.SETTINGS_PERMISSION)) {
      player.sendRichMessage("<red>You don't have access to custom particle settings.");
      return;
    }
    // Reached without the menu, so backing out just closes.
    ParticleDialogs.openSettings(plugin, player, type, _ -> {});
  }

  /** Join already migrates; this is for the players who were online when the plugin updated. */
  @Executes("migrate")
  @Permission(FlightParticles.RESET_ALL_PERMISSION)
  void migrateAll(CommandSender sender) {
    int migrated = plugin.migrateLegacyParticles();
    sender.sendRichMessage(
      "<green>Migrated <yellow><count><green> online player(s) to the new particle format.",
      Placeholder.unparsed("count", String.valueOf(migrated)));
  }

  @Subcommand("reset")
  class ResetSub {

    @Executes
    void resetParticle(@Executor Player player) {
      FlightParticleType type = FlightParticleType.fromLiteral("dust");
      plugin.setPlayerParticleType(player, type);
      player.sendRichMessage(
        "<green>Flight trail particle set to <dark_purple><b><type><green>!",
        Placeholder.unparsed("type", type.displayName()));
    }

    @Executes("--all")
    @Permission(FlightParticles.RESET_ALL_PERMISSION)
    void resetAllParticles(CommandSender sender) {
      int reset = plugin.resetUnpermittedParticles();
      sender.sendRichMessage(
        "<green>Reset <yellow><count><green> online player(s) whose particle is no longer permitted.",
        Placeholder.unparsed("count", String.valueOf(reset)));
    }
  }
}
