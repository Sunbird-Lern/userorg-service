package org.sunbird.cache.platform

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Platform object provides a centralized way to access configuration values using Typesafe Config.
 * It merges system environment variables with the default application configuration.
 */
object Platform {
  val defaultConf: Config = ConfigFactory.load()
  val envConf: Config = ConfigFactory.systemEnvironment()
  val config: Config = envConf.withFallback(defaultConf)

  /**
   * Retrieves a string value for the given key.
   *
   * @param key The configuration key.
   * @param default The default value if the key is not found.
   * @return The configuration value or the default.
   */
  def getString(key: String, default: String): String =
    if (config.hasPath(key)) config.getString(key) else default

  /**
   * Retrieves an integer value for the given key.
   *
   * @param key The configuration key.
   * @param default The default value if the key is not found.
   * @return The configuration value or the default.
   */
  def getInteger(key: String, default: Integer): Integer =
    if (config.hasPath(key)) config.getInt(key) else default

  /**
   * Retrieves a boolean value for the given key.
   *
   * @param key The configuration key.
   * @param default The default value if the key is not found.
   * @return The configuration value or the default.
   */
  def getBoolean(key: String, default: Boolean): Boolean =
    if (config.hasPath(key)) config.getBoolean(key) else default

  /**
   * Retrieves a list of strings for the given key.
   *
   * @param key The configuration key.
   * @param default The default value if the key is not found.
   * @return The configuration value or the default.
   */
  def getStringList(key: String, default: java.util.List[String]): java.util.List[String] =
    if (config.hasPath(key)) config.getStringList(key) else default

  /**
   * Retrieves a long value for the given key.
   *
   * @param key The configuration key.
   * @param default The default value if the key is not found.
   * @return The configuration value or the default.
   */
  def getLong(key: String, default: Long): Long =
    if (config.hasPath(key)) config.getLong(key) else default

  /**
   * Retrieves a double value for the given key.
   *
   * @param key The configuration key.
   * @param default The default value if the key is not found.
   * @return The configuration value or the default.
   */
  def getDouble(key: String, default: Double): Double =
    if (config.hasPath(key)) config.getDouble(key) else default

}
