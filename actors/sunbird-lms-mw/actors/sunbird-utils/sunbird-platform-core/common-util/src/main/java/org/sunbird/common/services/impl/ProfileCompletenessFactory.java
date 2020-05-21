/** */
package org.sunbird.common.services.impl;

import org.sunbird.common.services.ProfileCompletenessService;

/** @author Manzarul */
public class ProfileCompletenessFactory {

  /** @return */
  public static ProfileCompletenessService getInstance() {
    return new ProfileCompletenessServiceImpl();
  }
}
