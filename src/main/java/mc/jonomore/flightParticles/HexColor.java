package mc.jonomore.flightParticles;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hex is the dialog's color format; {@link ParticleSettings} stores the packed
 * int. Keeping the conversion at the dialog edge means the stored value can
 * never be unparseable.
 */
public final class HexColor {

  public static final int MAX_INPUT_LENGTH = 7;

  private static final Pattern PATTERN = Pattern.compile("^#?([0-9a-fA-F]{6}|[0-9a-fA-F]{3})$");

  private HexColor() {}

  /** Empty when the input is neither #RRGGBB nor #RGB; the caller re-shows the dialog with an error. */
  public static OptionalInt parse(String raw) {
    if (raw == null) {
      return OptionalInt.empty();
    }
    // Pasting from chat or Discord drags whitespace along more often than not.
    Matcher matcher = PATTERN.matcher(raw.trim());
    if (!matcher.matches()) {
      return OptionalInt.empty();
    }
    String digits = matcher.group(1);
    if (digits.length() == 3) {
      StringBuilder expanded = new StringBuilder(6);
      for (char digit : digits.toCharArray()) {
        expanded.append(digit).append(digit);
      }
      digits = expanded.toString();
    }
    // Six hex digits is 0xFFFFFF at most, so this cannot overflow or exceed Color's range.
    return OptionalInt.of(Integer.parseInt(digits, 16));
  }

  public static String format(int rgb) {
    return String.format("#%06X", rgb & 0xFFFFFF);
  }
}
