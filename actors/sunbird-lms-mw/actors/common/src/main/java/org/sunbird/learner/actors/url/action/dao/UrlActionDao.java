package org.sunbird.learner.actors.url.action.dao;

import java.util.List;
import org.sunbird.models.url.action.UrlAction;

public interface UrlActionDao {

  /**
   * Get list of URL actions.
   *
   * @return List of URL actions.
   */
  List<UrlAction> getUrlActions();
}
