package org.sunbird.url;

import org.sunbird.request.RequestContext;

/**
 * Interface for URL shortening operations.
 */
public interface URLShortner {

  /**
   * Shortens a given URL.
   *
   * @param url     The URL string to be shortened.
   * @param context The request context for logging.
   * @return The shortened URL, or the original URL if shortening fails or is disabled.
   */
  String shortUrl(String url, RequestContext context);
}
