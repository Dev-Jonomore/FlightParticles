package mc.jonomore.flightParticles;

import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * A template for defining particle trail customization dialogs.
 */
@SuppressWarnings("UnstableApiUsage")
public interface ParticleDialogTemplate {

  /**
   * The particle type this template applies to.
   */
  FlightParticleType particleType();

  /**
   * Returns the dialog's registry key.
   */
  default Key dialogKey() {
    return Key.key("flightparticles", particleType().literal());
  }

  /**
   * Returns the action key triggered when the player saves this dialog.
   */
  default Key saveActionKey() {
    return Key.key("flightparticles", "save_" + particleType().literal());
  }

  /**
   * The title shown in the dialog.
   */
  default Component title() {
    return Component.text("Customize " + particleType().displayName());
  }

  /**
   * The inputs shown in the dialog.
   */
  List<DialogInput> inputs();

  /**
   * Saves the inputs from the response view to the settings.
   *
   * @param player   the player who saved the dialog
   * @param view     the response view containing input values
   * @param settings the settings to update
   */
  void saveSettings(Player player, DialogResponseView view, ParticleSettings settings);
}
