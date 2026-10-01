package mc.jonomore.flightParticles;

import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * Collapses the DialogInput builder chains that every particle dialog would
 * otherwise repeat. Bounds live on {@link ParticleSettings} so the slider and
 * the clamp can never disagree.
 */
@SuppressWarnings("UnstableApiUsage")
public final class DialogHelper {

  public static final String OFFSET_X_KEY = "offsetX";
  public static final String OFFSET_Y_KEY = "offsetY";
  public static final String OFFSET_Z_KEY = "offsetZ";
  public static final String COUNT_KEY = "count";
  public static final String SPEED_KEY = "speed";
  public static final String COLOR_KEY = "color";
  public static final String SIZE_KEY = "size";

  private static final int INPUT_WIDTH = 200;

  private DialogHelper() {}

  public static DialogInput rangeInput(String key, Component label, float min, float max, float step, float initial) {
    return DialogInput.numberRange(key, label, min, max)
      .step(step)
      .initial(initial)
      .width(INPUT_WIDTH)
      .build();
  }

  /**
   * Signed, because at count 0 these are a direction vector and the sign is the
   * whole point. At count 1 and above they are a gaussian spread radius instead,
   * where a negative value just mirrors the distribution onto itself and renders
   * the same as its absolute value.
   */
  public static List<DialogInput> offsetInputs(ParticleSettings settings) {
    return List.of(
      offsetInput(OFFSET_X_KEY, "Spread X", settings.offsetX()),
      offsetInput(OFFSET_Y_KEY, "Spread Y", settings.offsetY()),
      offsetInput(OFFSET_Z_KEY, "Spread Z", settings.offsetZ()));
  }

  public static DialogInput countInput(int initial) {
    return rangeInput(COUNT_KEY, Component.text("Count"),
      ParticleSettings.COUNT_MIN, ParticleSettings.COUNT_MAX, 1f, initial);
  }

  /**
   * Stands in for the tooltips inputs do not have. Count is the only control
   * that changes what the others mean, so it is the only one worth the space.
   */
  public static List<Component> inputLegend() {
    return List.of(
      Component.text("Count 0 aims the trail — Spread is the direction it fires, Speed is how hard.")
        .color(NamedTextColor.GRAY),
      Component.text("Count 1+ scatters it — Spread is the radius, Speed is how much it drifts.")
        .color(NamedTextColor.GRAY));
  }

  public static DialogInput speedInput(float initial) {
    return rangeInput(SPEED_KEY, Component.text("Speed"),
      ParticleSettings.SPEED_MIN, ParticleSettings.SPEED_MAX, ParticleSettings.SPEED_STEP, initial);
  }

  public static DialogInput sizeInput(float initial) {
    return rangeInput(SIZE_KEY, Component.text("Size"),
      ParticleSettings.SIZE_MIN, ParticleSettings.SIZE_MAX, ParticleSettings.SIZE_STEP, initial);
  }

  /**
   * The only free-text field in the plugin, and so the only one that can fail
   * validation. The format lives in the label because inputs have no tooltips,
   * and this is the one field where guessing wrong costs a round trip. The seed
   * is truncated because a rejected value gets echoed back here, and a crafted
   * response is not bound by the client's maxLength.
   */
  public static DialogInput colorInput(String initial) {
    String seed = initial.length() > HexColor.MAX_INPUT_LENGTH
      ? initial.substring(0, HexColor.MAX_INPUT_LENGTH)
      : initial;
    return DialogInput.text(COLOR_KEY, Component.text("Color (#RRGGBB)"))
      .initial(seed)
      .maxLength(HexColor.MAX_INPUT_LENGTH)
      .width(INPUT_WIDTH)
      .build();
  }

  /**
   * For the root dialog's default/custom switch. The label is a Component so it
   * can carry the locked-state color; onTrue/onFalse are the strings a command
   * template would substitute, and go unread by custom-click callbacks.
   */
  public static DialogInput toggleInput(String key, Component label, boolean initial) {
    return DialogInput.bool(key, label, initial, "true", "false");
  }

  private static DialogInput offsetInput(String key, String label, double initial) {
    return rangeInput(key, Component.text(label),
      (float) ParticleSettings.OFFSET_MIN, (float) ParticleSettings.OFFSET_MAX,
      ParticleSettings.OFFSET_STEP, (float) initial);
  }
}
