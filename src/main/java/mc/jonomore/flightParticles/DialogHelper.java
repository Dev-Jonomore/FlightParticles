package mc.jonomore.flightParticles;

import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * Collapses the DialogInput builder chains that every particle dialog would
 * otherwise repeat.
 */
@SuppressWarnings("UnstableApiUsage")
public final class DialogHelper {

  private DialogHelper() {}

  // Generic numeric slider -- every ParticleSettings numeric field routes through this.
  public static DialogInput rangeInput(String key, String label, float min, float max, float step, float initial) {
    return DialogInput.numberRange(key, Component.text(label), min, max)
      .step(step)
      .initial(initial)
      .width(200)
      .build();
  }

  // 0-255 preset -- Dust's RGB sliders.
  public static DialogInput colorComponentInput(String key, String label, int initial) {
    return rangeInput(key, label, 0f, 255f, 1f, initial);
  }

  // useVelocityOffset -- the one boolean field on ParticleSettings.
  // onTrue/onFalse only matter if a labelFormat elsewhere references them; "On"/"Off" is a safe default.
  public static DialogInput toggleInput(String key, String label, boolean initial) {
    return DialogInput.bool(key, Component.text(label), initial, "On", "Off");
  }

  // Offset X/Y/Z is identical across all 8 dialogs. Placeholder range +/-5 blocks, 0.05 step -- tune to taste.
  public static List<DialogInput> offsetInputs(double offsetX, double offsetY, double offsetZ) {
    return List.of(
      rangeInput("offsetX", "Offset X", -5f, 5f, 0.05f, (float) offsetX),
      rangeInput("offsetY", "Offset Y", -5f, 5f, 0.05f, (float) offsetY),
      rangeInput("offsetZ", "Offset Z", -5f, 5f, 0.05f, (float) offsetZ)
    );
  }

  // Count is shared shape too. Placeholder range 1-20.
  public static DialogInput countInput(int initial) {
    return rangeInput("count", "Count", 1f, 20f, 1f, initial);
  }
}