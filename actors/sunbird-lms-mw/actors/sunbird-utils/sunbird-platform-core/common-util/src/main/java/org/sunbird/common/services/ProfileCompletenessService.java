/** */
package org.sunbird.common.services;

import java.util.Map;

/**
 * This interface will have method to compute the profile completeness.
 *
 * @author Manzarul
 */
public interface ProfileCompletenessService {

  /**
   * This method will compute the user profile completeness percentage based on attribute weighted
   * settings. it will provide completeness percentage value and list of all missing keys.
   *
   * @param profileData Map<String,Object>
   * @return Map<String,Object>
   */
  Map<String, Object> computeProfile(Map<String, Object> profileData);
}
