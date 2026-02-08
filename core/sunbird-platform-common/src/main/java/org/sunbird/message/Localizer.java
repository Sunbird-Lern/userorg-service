package org.sunbird.message;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Utility class for managing localized response messages using resource bundles.
 */
public class Localizer {

  private ResourceBundle userResourceBundle = null;
  private static Localizer instance = null;

  private Localizer() {
    userResourceBundle = ResourceBundle.getBundle("responseMessages");
  }

  /**
   * Gets the singleton instance of the Localizer.
   *
   * @return The Localizer instance.
   */
  public static Localizer getInstance() {
    if (instance == null) {
      instance = new Localizer();
    }
    return instance;
  }

  /**
   * Creates a Locale object based on the base locale and extensions.
   *
   * @param baseLocale The base language code (e.g., "en").
   * @param localeExtensions Additional locale extensions.
   * @return A Locale object.
   */
  public Locale getLocale(String baseLocale, String localeExtensions) {
    return new Locale(baseLocale, localeExtensions);
  }

  /**
   * Retrieves a localized message for a given key and locale.
   *
   * @param key The message key in the resource bundle.
   * @param locale The desired locale. If null, the default bundle is used.
   * @return The localized message string.
   */
  public String getMessage(String key, Locale locale) {
    if (null == locale) {
      return userResourceBundle.getString(key);
    } else {
      userResourceBundle = ResourceBundle.getBundle("responseMessages", locale);
      return userResourceBundle.getString(key);
    }
  }
}
