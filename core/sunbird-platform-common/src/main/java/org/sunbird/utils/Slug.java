package org.sunbird.utils;

import java.net.URLDecoder;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import net.sf.junidecode.Junidecode;
import org.sunbird.logging.LoggerUtil;

/**
 * Utility class for slugifying strings.
 * Removes special characters, spaces, and handles transliteration.
 */
public class Slug {
  private static final LoggerUtil logger = new LoggerUtil(Slug.class);

  private static final Pattern NONLATIN = Pattern.compile("[^\\w-\\.]");
  private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
  private static final Pattern DUPDASH = Pattern.compile("-+");

  private Slug() {}

  /**
   * Creates a slug from the input string.
   *
   * @param input The string to slugify.
   * @param transliterate Whether to transliterate characters to ASCII.
   * @return The slugified string.
   */
  public static String makeSlug(String input, boolean transliterate) {
    String origInput = input;
    // Validate the input
    if (input == null) {
      logger.debug("Slug:makeSlug: Provided input value is null.");
      return input;
    }
    // Remove extra spaces
    String tempInputValue = input.trim();
    // Remove URL encoding
    tempInputValue = urlDecode(tempInputValue);
    // If transliterate is required
    if (transliterate) {
      // Transliterate & cleanup
      tempInputValue = transliterate(tempInputValue);
    }
    // Replace all whitespace with dashes
    tempInputValue = WHITESPACE.matcher(tempInputValue).replaceAll("-");
    // Remove all accent chars
    tempInputValue = Normalizer.normalize(tempInputValue, Form.NFD);
    // Remove all non-latin special characters
    tempInputValue = NONLATIN.matcher(tempInputValue).replaceAll("");
    // Remove any consecutive dashes
    tempInputValue = normalizeDashes(tempInputValue);
    // Validate before returning
    validateResult(tempInputValue, origInput);
    // Slug is always lowercase
    return tempInputValue.toLowerCase(Locale.ENGLISH);
  }

  private static void validateResult(String input, String origInput) {
    // Check if we are not left with a blank
    if (input.length() == 0) {
      logger.debug(
          "Slug:validateResult: Failed to cleanup the input, resulted in empty string. Original input: "
              + origInput);
    }
  }

  /**
   * Transliterates the input string to ASCII.
   *
   * @param input The string to transliterate.
   * @return The transliterated string.
   */
  public static String transliterate(String input) {
    return Junidecode.unidecode(input);
  }

  /**
   * Decodes a URL encoded string.
   *
   * @param input The URL encoded string.
   * @return The decoded string, or the original if decoding fails.
   */
  public static String urlDecode(String input) {
    String value = "";
    try {
      value = URLDecoder.decode(input, "UTF-8");
    } catch (Exception ex) {
      logger.error("Slug:urlDecode: Exception occurred while decoding url: " + ex.getMessage(), ex);
    }
    return value;
  }

  /**
   * Removes duplicate characters from a string.
   *
   * @param text The input string.
   * @return The string with unique characters preserving order.
   */
  public static String removeDuplicateChars(String text) {
    if (text == null || text.length() == 0) {
      return "";
    }
    Set<Character> set = new LinkedHashSet<>();
    StringBuilder ret = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      set.add(text.charAt(i));
    }
    Iterator<Character> itr = set.iterator();
    while (itr.hasNext()) {
      ret.append(itr.next());
    }
    return ret.toString();
  }

  /**
   * Normalizes dashes in the text (removes duplicates and leading/trailing dashes).
   *
   * @param text The input text.
   * @return The text with normalized dashes.
   */
  public static String normalizeDashes(String text) {
    String clean = DUPDASH.matcher(text).replaceAll("-");
    // Special case that only dashes remain
    if ("-".equals(clean) || "--".equals(clean)) {
      return "";
    }
    int startIdx = (clean.startsWith("-") ? 1 : 0);
    int endIdx = (clean.endsWith("-") ? 1 : 0);
    clean = clean.substring(startIdx, (clean.length() - endIdx));
    return clean;
  }
}
