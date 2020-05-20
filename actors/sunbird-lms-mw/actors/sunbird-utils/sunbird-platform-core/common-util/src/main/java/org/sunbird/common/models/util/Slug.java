/** */
package org.sunbird.common.models.util;

import java.net.URLDecoder;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import net.sf.junidecode.Junidecode;

/**
 * This class will remove the special character,space from the provided String.
 *
 * @author Manzarul
 */
public class Slug {

  private static final Pattern NONLATIN = Pattern.compile("[^\\w-\\.]");
  private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
  private static final Pattern DUPDASH = Pattern.compile("-+");

  public static String makeSlug(String input, boolean transliterate) {
    String origInput = input;
    String tempInputValue = "";
    // Validate the input
    if (input == null) {
      ProjectLogger.log("Provided input value is null");
      return input;
    }
    // Remove extra spaces
    tempInputValue = input.trim();
    // Remove URL encoding
    tempInputValue = urlDecode(tempInputValue);
    // If transliterate is required
    if (transliterate) {
      // Tranlisterate & cleanup
      String transliterated = transliterate(tempInputValue);
      tempInputValue = transliterated;
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
      ProjectLogger.log("Failed to cleanup the input " + origInput);
    }
  }

  public static String transliterate(String input) {
    return Junidecode.unidecode(input);
  }

  public static String urlDecode(String input) {
    String value = "";
    try {
      value = URLDecoder.decode(input, "UTF-8");
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
    }
    return value;
  }

  public static String removeDuplicateChars(String text) {
    Set<Character> set = new LinkedHashSet<>();
    StringBuilder ret = new StringBuilder(text.length());
    if (text.length() == 0) {
      return "";
    }
    for (int i = 0; i < text.length(); i++) {
      set.add(text.charAt(i));
    }
    Iterator<Character> itr = set.iterator();
    while (itr.hasNext()) {
      ret.append(itr.next());
    }
    return ret.toString();
  }

  public static String normalizeDashes(String text) {
    String clean = DUPDASH.matcher(text).replaceAll("-");
    // Special case that only dashes remain
    if ("-".equals(clean) || "--".equals(clean)) return "";
    int startIdx = (clean.startsWith("-") ? 1 : 0);
    int endIdx = (clean.endsWith("-") ? 1 : 0);
    clean = clean.substring(startIdx, (clean.length() - endIdx));
    return clean;
  }
}
