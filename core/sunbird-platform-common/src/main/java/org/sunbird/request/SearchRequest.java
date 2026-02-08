package org.sunbird.request;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a search request containing a map of request parameters.
 */
public class SearchRequest {
  private Map<String, Object> request = new HashMap<>();

  /**
   * Gets the request parameters.
   *
   * @return A map of request parameters.
   */
  public Map<String, Object> getRequest() {
    return request;
  }

  /**
   * Sets the request parameters.
   *
   * @param request A map of request parameters.
   */
  public void setRequest(Map<String, Object> request) {
    this.request = request;
  }
}