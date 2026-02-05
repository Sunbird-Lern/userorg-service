package org.sunbird.url;

import org.sunbird.request.RequestContext;

/**
 * Interface for URL shortening service.
 */
public interface URLShortner {

  /**
   * Shortens the provided long URL.
   *
   * @param url The long URL to shorten.
   * @param context The request context for logging.
   * @return The shortened URL string, or the original URL if shortening fails or is disabled.
   */
  String shortUrl(String url, RequestContext context);
}
