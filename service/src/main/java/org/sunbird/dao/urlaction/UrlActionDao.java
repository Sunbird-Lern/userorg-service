package org.sunbird.dao.urlaction;

import org.sunbird.model.urlaction.UrlAction;

import java.util.List;

public interface UrlActionDao {

  /**
   * Get list of URL actions.
   *
   * @return List of URL actions.
   */
  List<UrlAction> getUrlActions();
}
