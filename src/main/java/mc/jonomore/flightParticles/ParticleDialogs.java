package mc.jonomore.flightParticles;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * The per-particle settings screens.
 *
 * <p>Seven of the ten types share an identical shape -- three spread sliders, a
 * count, and a speed -- because only Dust carries a data object. Dust adds a
 * color and a size, so there is one builder here rather than ten dialogs.
 *
 * <p>Nothing is persisted until the player accepts. A dialog cannot mutate
 * itself, so "reset" and a rejected color both work by closing and re-showing
 * with different seed values.
 */
@SuppressWarnings("UnstableApiUsage")
public final class ParticleDialogs {

  private static final int BUTTON_WIDTH = 150;

  // Each re-show builds fresh buttons, but a player can click Reset repeatedly
  // within one screen, so single-use callbacks would strand them.
  static final ClickCallback.Options CALLBACK_OPTIONS = ClickCallback.Options.builder()
    .uses(ClickCallback.UNLIMITED_USES)
    .lifetime(Duration.ofMinutes(10))
    .build();

  private ParticleDialogs() {}

  /**
   * Opens the customizer for one particle.
   *
   * @param onExit run when the player backs out, on the main thread and one tick
   *               later -- wire it to the root dialog.
   */
  public static void openSettings(
      FlightParticles plugin, Player player, FlightParticleType type, Consumer<Player> onExit) {
    show(plugin, player, type, plugin.storage().getSettings(player, type), null, null, onExit);
  }

  private static void show(FlightParticles plugin, Player player, FlightParticleType type,
      ParticleSettings settings, String rawColor, Component error, Consumer<Player> onExit) {

    List<DialogBody> body = new ArrayList<>();
    DialogHelper.inputLegend().forEach(line -> body.add(DialogBody.plainMessage(line)));
    // Last, so it sits directly above the inputs it is complaining about.
    if (error != null) {
      body.add(DialogBody.plainMessage(error));
    }

    List<DialogInput> inputs = new ArrayList<>(DialogHelper.offsetInputs(settings));
    inputs.add(DialogHelper.countInput(settings.count()));
    inputs.add(DialogHelper.speedInput(settings.speed()));
    if (type == FlightParticleType.DUST) {
      inputs.add(DialogHelper.colorInput(rawColor != null ? rawColor : HexColor.format(settings.rgb())));
      inputs.add(DialogHelper.sizeInput(settings.size()));
    }

    ActionButton reset = ActionButton.builder(Component.text("Reset Settings", NamedTextColor.RED))
      .tooltip(Component.text("Reset to the original default settings."))
      .width(BUTTON_WIDTH)
      .action(DialogAction.customClick(
        (view, audience) -> reopen(plugin, player, type, type.defaultSettings(), null, null, onExit),
        CALLBACK_OPTIONS))
      .build();

    ActionButton accept = ActionButton.builder(Component.text("Save and use", NamedTextColor.GREEN))
      .tooltip(Component.text("Save these values and switch your trail to them."))
      .width(BUTTON_WIDTH)
      .action(DialogAction.customClick(
        (view, audience) -> accept(plugin, player, type, settings, view, onExit),
        CALLBACK_OPTIONS))
      .build();

    ActionButton back = ActionButton.builder(Component.text("Back", NamedTextColor.RED))
      .tooltip(Component.text("Discard these changes and go back."))
      .width(BUTTON_WIDTH)
      .action(DialogAction.customClick(
        (view, audience) -> plugin.getServer().getScheduler().runTask(plugin, () -> onExit.accept(player)),
        CALLBACK_OPTIONS))
      .build();

    Dialog dialog = Dialog.create(builder -> builder.empty()
      .base(DialogBase.builder(Component.text(type.displayName() + " Settings"))
        .body(body)
        .inputs(inputs)
        .canCloseWithEscape(true)
        .afterAction(DialogBase.DialogAfterAction.CLOSE)
        .build())
      .type(DialogType.multiAction(List.of(reset, accept))
        .exitAction(back)
        .columns(2)
        .build()));

    player.showDialog(dialog);
  }

  /** Escape hatch for every path that needs a different screen: the client is still tearing
   *  down the current one, and showing on the same tick can lose the replacement. */
  private static void reopen(FlightParticles plugin, Player player, FlightParticleType type,
      ParticleSettings settings, String rawColor, Component error, Consumer<Player> onExit) {
    plugin.getServer().getScheduler().runTask(plugin,
      () -> show(plugin, player, type, settings, rawColor, error, onExit));
  }

  private static void accept(FlightParticles plugin, Player player, FlightParticleType type,
      ParticleSettings seed, DialogResponseView view, Consumer<Player> onExit) {

    // The button being visible is not authorization; permissions can change while a dialog is open.
    if (!type.isAllowed(player) || !player.hasPermission(FlightParticles.SETTINGS_PERMISSION)) {
      player.sendRichMessage("<red>You can no longer customize that particle.");
      return;
    }

    ParticleSettings submitted = readNumbers(view, seed);

    int rgb = seed.rgb();
    if (type == FlightParticleType.DUST) {
      String raw = view.getText(DialogHelper.COLOR_KEY);
      OptionalInt parsed = HexColor.parse(raw);
      if (parsed.isEmpty()) {
        // Echo the near-miss back rather than the old color, or they lose what they were fixing.
        reopen(plugin, player, type, submitted, raw,
          Component.text("Not a hex color. Try #6E2970 or #6E2.").color(NamedTextColor.RED), onExit);
        return;
      }
      rgb = parsed.getAsInt();
    }

    ParticleSettings settings = new ParticleSettings(
      submitted.offsetX(), submitted.offsetY(), submitted.offsetZ(),
      submitted.count(), submitted.speed(), rgb, submitted.size());

    ParticleStorage storage = plugin.storage();
    storage.setSettings(player, type, settings);
    storage.selectCustom(player, type);

    player.sendRichMessage(
      "<green>Flight trail particle set to <dark_purple><b><type></b><green> with your settings!",
      Placeholder.unparsed("type", type.displayName()));
  }

  /** Any field the client omitted falls back to the seed; the record constructor clamps the rest. */
  private static ParticleSettings readNumbers(DialogResponseView view, ParticleSettings seed) {
    return new ParticleSettings(
      read(view, DialogHelper.OFFSET_X_KEY, seed.offsetX()),
      read(view, DialogHelper.OFFSET_Y_KEY, seed.offsetY()),
      read(view, DialogHelper.OFFSET_Z_KEY, seed.offsetZ()),
      (int) Math.round(read(view, DialogHelper.COUNT_KEY, seed.count())),
      (float) read(view, DialogHelper.SPEED_KEY, seed.speed()),
      seed.rgb(),
      (float) read(view, DialogHelper.SIZE_KEY, seed.size()));
  }

  private static double read(DialogResponseView view, String key, double fallback) {
    Float value = view.getFloat(key);
    return value == null ? fallback : value;
  }
}
