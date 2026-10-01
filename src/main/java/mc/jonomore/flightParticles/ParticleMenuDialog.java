package mc.jonomore.flightParticles;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The root menu: one action per particle, a default/custom switch, reset, and
 * the visibility toggle.
 *
 * <p>Every change re-shows the menu one tick later, because the client is still
 * tearing down the current screen when the click arrives. Picking a particle
 * with the switch on hands off to {@link ParticleDialogs}, whose exit action
 * comes back here.
 */
public final class ParticleMenuDialog {

  private static final String CUSTOM_KEY = "custom";
  private static final int BUTTON_WIDTH = 150;

  private ParticleMenuDialog() {}

  public static void show(FlightParticles plugin, Player p) {
    ParticleStorage storage = plugin.storage();
    ParticleStorage.Selection selection = storage.getSelection(p);
    boolean canCustomize = p.hasPermission(FlightParticles.SETTINGS_PERMISSION);

    // Mirrors the renderer: a trail whose permission was revoked draws as Dust.
    FlightParticleType current = selection.type().isAllowed(p) ? selection.type() : plugin.particles().defaultType();
    List<DialogBody> body = getBodyLines(selection, canCustomize, current);

    // A boolean input can be neither disabled nor colored per option, so a locked
    // switch is marked red and its value is ignored in the callback instead.
    Component toggleLabel = Component.text("Use custom settings",
      canCustomize ? NamedTextColor.WHITE : NamedTextColor.RED);

    List<ActionButton> actions = new ArrayList<>();
    for (FlightParticleType type : plugin.particles().values()) {
      actions.add(particleButton(plugin, p, type, storage.hasCustomSettings(p, type)));
    }

    actions.add(ActionButton.builder(Component.text("Reset Trail", NamedTextColor.RED))
      .tooltip(Component.text("Go back to the default trail."))
      .width(BUTTON_WIDTH)
      .action(DialogAction.customClick(
        (_, _) -> choose(plugin, p, plugin.particles().defaultType(), false),
        ParticleDialogs.CALLBACK_OPTIONS))
      .build());

    boolean enabled = selection.enabled();
    actions.add(ActionButton.builder(enabled
        ? Component.text("Hide Trail", NamedTextColor.RED)
        : Component.text("Show Trail", NamedTextColor.GREEN))
      .tooltip(Component.text(enabled ? "Stop showing your flight trail." : "Show your flight trail again."))
      .width(BUTTON_WIDTH)
      .action(DialogAction.customClick(
        (_, _) -> {
          // Read back rather than trusting the click: a stale dialog may show the old state.
          ParticleStorage fresh = plugin.storage();
          fresh.setEnabled(p, !fresh.getSelection(p).enabled());
          reopen(plugin, p);
        },
        ParticleDialogs.CALLBACK_OPTIONS))
      .build());

    // Two columns, not three: twelve buttons at 150 wide still fit at GUI scale 4.
    Dialog dialog = Dialog.create(builder -> builder.empty()
      .base(DialogBase.builder(Component.text("Flight Particles"))
        .body(body)
        .inputs(List.of(DialogHelper.toggleInput(CUSTOM_KEY, toggleLabel, selection.custom())))
        .canCloseWithEscape(true)
        .afterAction(DialogBase.DialogAfterAction.CLOSE)
        .build())
      .type(DialogType.multiAction(actions)
        .columns(2)
        .build()));

    p.showDialog(dialog);
  }

  private static @NonNull List<DialogBody> getBodyLines(ParticleStorage.Selection selection, boolean canCustomize, FlightParticleType current) {
    boolean tuned = selection.custom() && canCustomize;

    return List.of(DialogBody.plainMessage(
      Component.text("Current particle: ", NamedTextColor.GRAY)
        .append(Component.text(current.displayName() + (tuned ? " (Custom)" : ""), NamedTextColor.YELLOW))
        .append(Component.text(" — Trail is ", NamedTextColor.GRAY))
        .append(selection.enabled()
          ? Component.text("visible", NamedTextColor.GREEN)
          : Component.text("hidden", NamedTextColor.RED))));
  }

  private static ActionButton particleButton(
      FlightParticles plugin, Player p, FlightParticleType type, boolean hasCustom) {

    boolean allowed = type.isAllowed(p);
    Component label = Component.text(
      (type == plugin.particles().defaultType() ? type.displayName() + " (Default)" : type.displayName())
        + (hasCustom ? " *" : ""),
      allowed ? NamedTextColor.WHITE : NamedTextColor.RED);

    Component tooltip = Component.text(allowed
      ? (hasCustom ? "Use this particle. The * means you have tuned it." : "Use this particle.")
      : "You haven't unlocked this particle.");

    return ActionButton.builder(label)
      .tooltip(tooltip)
      .width(BUTTON_WIDTH)
      .action(DialogAction.customClick(
        (view, _) -> onParticleClick(plugin, p, type, view),
        ParticleDialogs.CALLBACK_OPTIONS))
      .build();
  }

  private static void onParticleClick(
      FlightParticles plugin, Player p, FlightParticleType type, DialogResponseView view) {

    // The button being visible is not authorization; permissions can change while a dialog is open.
    if (!type.isAllowed(p)) {
      p.sendRichMessage("<red>This particle is locked!");
      reopen(plugin, p);
      return;
    }

    // The client sends whatever it likes for the switch, and a locked one is ignored.
    Boolean requested = view.getBoolean(CUSTOM_KEY);
    boolean custom = requested != null && requested;
    if (custom && !p.hasPermission(FlightParticles.SETTINGS_PERMISSION)) {
      p.sendRichMessage("<red>You don't have access to custom particle settings.");
      custom = false;
    }

    choose(plugin, p, type, custom);
  }

  /** Custom hands off to the tuning screen, which saves on accept; a preset applies immediately. */
  private static void choose(FlightParticles plugin, Player p, FlightParticleType type, boolean custom) {
    if (!type.isAllowed(p)) {
      p.sendRichMessage("<red>This particle is locked!");
      reopen(plugin, p);
      return;
    }

    if (custom && p.hasPermission(FlightParticles.SETTINGS_PERMISSION)) {
      // Deferred like the rest: this runs inside the click, before the screen has closed.
      plugin.getServer().getScheduler().runTask(plugin,
        () -> ParticleDialogs.openSettings(plugin, p, type, q -> show(plugin, q)));
      return;
    }

    plugin.storage().selectPreset(p, type);
    p.sendRichMessage(
      "<green>Flight trail particle set to <dark_purple><b><type></b><green>!",
      Placeholder.unparsed("type", type.displayName()));
    reopen(plugin, p);
  }

  /** The client is still tearing down the current screen; showing on the same tick can lose the new one. */
  private static void reopen(FlightParticles plugin, Player p) {
    plugin.getServer().getScheduler().runTask(plugin, () -> show(plugin, p));
  }
}
