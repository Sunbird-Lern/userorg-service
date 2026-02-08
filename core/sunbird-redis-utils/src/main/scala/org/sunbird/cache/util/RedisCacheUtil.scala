package org.sunbird.cache.util

import java.time.Duration
import org.apache.commons.lang3.StringUtils
import org.sunbird.cache.platform.Platform
import org.sunbird.keys.JsonKey
import org.sunbird.logging.LoggerUtil
import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
 * RedisCacheUtil provides utility methods to perform CRUD operations with Redis using Jedis.
 */
class RedisCacheUtil {

  private val logger: LoggerUtil = new LoggerUtil(classOf[RedisCacheUtil])

  implicit val className: String = "org.sunbird.cache.connector.RedisConnector"

  private val redis_host: String = Platform.getString(JsonKey.REDIS_HOST_VALUE, "localhost")
  private val redis_port: Int = Platform.getInteger(JsonKey.REDIS_PORT_VALUE, 6379)
  private val index: Int = Platform.getInteger(JsonKey.REDIS_INDEX_VALUE, 0)

  logger.info( s"RedisCacheUtil: Initializing with host: $redis_host, port: $redis_port, index: $index")

  private def buildPoolConfig: JedisPoolConfig = {
    val poolConfig = new JedisPoolConfig
    poolConfig.setMaxTotal(Platform.getInteger("redis.connection.max", 2))
    poolConfig.setMaxIdle(Platform.getInteger("redis.connection.idle.max", 2))
    poolConfig.setMinIdle(Platform.getInteger("redis.connection.idle.min", 1))
    poolConfig.setTestWhileIdle(true)
    poolConfig.setMinEvictableIdleTimeMillis(
      Duration.ofSeconds(Platform.getLong("redis.connection.minEvictableIdleTimeSeconds", 120)).toMillis)
    poolConfig.setTimeBetweenEvictionRunsMillis(
      Duration.ofSeconds(Platform.getLong("redis.connection.timeBetweenEvictionRunsSeconds", 300)).toMillis)
    poolConfig.setBlockWhenExhausted(true)
    poolConfig
  }

  protected var jedisPool: JedisPool = new JedisPool(buildPoolConfig, redis_host, redis_port)

  /**
   * Returns a Jedis connection for the specified database.
   *
   * @param database The database index.
   * @return A Jedis instance.
   */
  def getConnection(database: Int): Jedis = {
    val conn = jedisPool.getResource
    conn.select(database)
    conn
  }

  /**
   * Returns a Jedis connection for the default database defined in configuration.
   *
   * @return A Jedis instance.
   */
  def getConnection: Jedis = {
    val jedis = jedisPool.getResource
    if (index > 0) jedis.select(index)
    jedis
  }

  /**
   * Returns the Jedis connection back to the pool.
   *
   * @param jedis The Jedis instance.
   */
  protected def returnConnection(jedis: Jedis): Unit = {
    if (null != jedis) jedisPool.returnResource(jedis)
  }

  /** Resets the connection pool. */
  def resetConnection(): Unit = {
    jedisPool.close()
    jedisPool = new JedisPool(buildPoolConfig, redis_host, redis_port)
  }

  /** Closes the connection pool. */
  def closePool(): Unit = {
    jedisPool.close()
  }

  /**
   * Checks the connection by selecting a test database.
   *
   * @return True if connection is successful.
   */
  def checkConnection: Boolean = {
    try {
      val conn = getConnection(2)
      conn.close()
      true
    } catch {
      case _: Exception => false
    }
  }

  /**
   * Stores string data into cache for a given key.
   *
   * @param key The cache key.
   * @param data The data to store.
   * @param ttl Time to live in seconds.
   */
  def set(key: String, data: String, ttl: Int = 0): Unit = {
    val jedis = getConnection
    try {
      jedis.del(key)
      jedis.set(key, data)
      if (ttl > 0) jedis.expire(key, ttl)
    } catch {
      case e: Exception =>
        logger.error( s"RedisCacheUtil:set: Exception for key: $key", e)
        throw e
    } finally returnConnection(jedis)
  }

  /**
   * Retrieves string data from cache for a given key.
   *
   * @param key The cache key.
   * @param handler A fallback handler if key is not found.
   * @param ttl Time to live for fallback data.
   * @return The cached data or data from handler.
   */
  def get(key: String, handler: String => String = defaultStringHandler, ttl: Int = 0): String = {
    val jedis = getConnection
    try {
      var data = jedis.get(key)
      if (null != handler && (null == data || data.isEmpty)) {
        data = handler(key)
        if (null != data && !data.isEmpty)
          set(key, data, ttl)
      }
      data
    } catch {
      case e: Exception =>
        logger.error( s"RedisCacheUtil:get: Exception for key: $key", e)
        throw e
    } finally returnConnection(jedis)
  }

  /**
   * Asynchronously retrieves string data for a given key.
   *
   * @param key The cache key.
   * @param asyncHandler A fallback async handler.
   * @param ttl Time to live for fallback data.
   * @param ec Execution context.
   * @return Future of the string data.
   */
  def getAsync(key: String, asyncHandler: String => Future[String], ttl: Int = 0)(implicit ec: ExecutionContext): Future[String] = {
    val jedis = getConnection
    try {
      val data = jedis.get(key)
      if (null != asyncHandler && (null == data || data.isEmpty)) {
        val dataFuture: Future[String] = asyncHandler(key)
        dataFuture.map(value => {
          if (null != value && !value.isEmpty)
            set(key, value, ttl)
          value
        })
      } else Future {
        data
      }
    } catch {
      case e: Exception =>
        logger.error( s"RedisCacheUtil:getAsync: Exception for key: $key", e)
        throw e
    } finally returnConnection(jedis)
  }

  /**
   * Increments the value of a key by 1.
   *
   * @param key The cache key.
   * @return The new value.
   */
  def incrementAndGet(key: String): Double = {
    val jedis = getConnection
    val inc = 1.0
    try jedis.incrByFloat(key, inc)
    catch {
      case e: Exception =>
        logger.error( s"RedisCacheUtil:incrementAndGet: Exception for key: $key", e)
        throw e
    } finally returnConnection(jedis)
  }

  /**
   * Stores a list of strings for a given key.
   *
   * @param key The cache key.
   * @param data The list of data.
   * @param ttl Time to live.
   * @param isPartialUpdate If true, adds to existing list without deleting.
   */
  def saveList(key: String, data: List[String], ttl: Int = 0, isPartialUpdate: Boolean = false): Unit = {
    val jedis = getConnection
    try {
      if (!isPartialUpdate)
        jedis.del(key)
      data.foreach(entry => jedis.sadd(key, entry))
      if (ttl > 0 && !isPartialUpdate) jedis.expire(key, ttl)
    } catch {
      case e: Exception =>
        logger.error( s"RedisCacheUtil:saveList: Exception for key: $key", e)
        throw e
    } finally returnConnection(jedis)
  }

  /**
   * Adds data to an existing list.
   *
   * @param key The cache key.
   * @param data The list of strings to add.
   */
  def addToList(key: String, data: List[String]): Unit = {
    saveList(key, data, 0, isPartialUpdate = true)
  }

  /**
   * Retrieves a list of strings from cache for a given key.
   *
   * @param key The cache key.
   * @param handler A fallback handler.
   * @param ttl Time to live for fallback data.
   * @return The list of strings.
   */
  def getList(key: String, handler: String => List[String] = defaultListHandler, ttl: Int = 0): List[String] = {
    val jedis = getConnection
    try {
      var data = jedis.smembers(key).asScala.toList
      if (null != handler && (null == data || data.isEmpty)) {
        data = handler(key)
        if (null != data && !data.isEmpty)
          saveList(key, data, ttl, isPartialUpdate = false)
      }
      data
    } catch {
      case e: Exception =>
        logger.error( s"RedisCacheUtil:getList: Exception for key: $key", e)
        throw e
    } finally returnConnection(jedis)
  }

  /**
   * Asynchronously retrieves a list of strings for a given key.
   *
   * @param key The cache key.
   * @param asyncHandler A fallback async handler.
   * @param ttl Time to live for fallback data.
   * @param ec Execution context.
   * @return Future of the list of strings.
   */
  def getListAsync(key: String, asyncHandler: String => Future[List[String]], ttl: Int = 0)(implicit ec: ExecutionContext): Future[List[String]] = {
    val jedis = getConnection
    try {
      val data = jedis.smembers(key).asScala.toList
      if (null != asyncHandler && (null == data || data.isEmpty)) {
        val dataFuture = asyncHandler(key)
        dataFuture.map(value => {
          if (null != value && !value.isEmpty)
            saveList(key, value, ttl, isPartialUpdate = false)
          value
        })
      } else Future {
        data
      }
    } catch {
      case e: Exception =>
        logger.error( s"RedisCacheUtil:getListAsync: Exception for key: $key", e)
        throw e
    } finally returnConnection(jedis)
  }

  /**
   * Removes specific entries from an existing list.
   *
   * @param key The cache key.
   * @param data The entries to remove.
   */
  def removeFromList(key: String, data: List[String]): Unit = {
    val jedis = getConnection
    try data.foreach(entry => jedis.srem(key, entry))
    catch {
      case e: Exception =>
        logger.error( s"RedisCacheUtil:removeFromList: Exception for key: $key", e)
        throw e
    } finally returnConnection(jedis)
  }

  /**
   * Deletes specific keys from cache.
   *
   * @param keys The keys to delete.
   */
  def delete(keys: String*): Unit = {
    val jedis = getConnection
    try jedis.del(keys.map(_.asInstanceOf[String]): _*)
    catch {
      case e: Exception =>
        logger.error( s"RedisCacheUtil:delete: Exception for keys: ${keys.mkString(",")}", e)
        throw e
    } finally returnConnection(jedis)
  }

  /**
   * Deletes all keys matching the given pattern.
   *
   * @param pattern The pattern to match (e.g., "prefix*").
   */
  def deleteByPattern(pattern: String): Unit = {
    if (StringUtils.isNotBlank(pattern) && !StringUtils.equalsIgnoreCase(pattern, "*")) {
      val jedis = getConnection
      try {
        val keys = jedis.keys(pattern)
        if (keys != null && !keys.isEmpty)
          jedis.del(keys.toArray.map(_.asInstanceOf[String]): _*)
      } catch {
        case e: Exception =>
          logger.error( s"RedisCacheUtil:deleteByPattern: Exception for pattern: $pattern", e)
          throw e
      } finally returnConnection(jedis)
    }
  }

  private def defaultStringHandler(objKey: String): String = ""

  private def defaultListHandler(objKey: String): List[String] = List()

}
