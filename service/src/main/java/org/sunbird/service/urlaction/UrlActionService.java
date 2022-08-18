package org.sunbird.service.urlaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.sunbird.dao.urlaction.UrlActionDao;
import org.sunbird.dao.urlaction.impl.UrlActionDaoImpl;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.urlaction.UrlAction;

public class UrlActionService {

  private static final UrlActionDao urlActionDao = UrlActionDaoImpl.getInstance();

  private UrlActionService() {}

  public static Map<String, Object> getUrlActionMap(String urlId) {
    Map<String, Object> response = new HashMap<>();
    List<UrlAction> urlActionList = urlActionDao.getUrlActions();

    if (CollectionUtils.isNotEmpty(urlActionList)) {
      for (UrlAction urlAction : urlActionList) {
        if (urlAction.getId().equals(urlId)) {
          response.put(JsonKey.ID, urlAction.getId());
          response.put(JsonKey.NAME, urlAction.getName());
          response.put(
              JsonKey.URL, urlAction.getUrl() != null ? urlAction.getUrl() : new ArrayList<>());
          return response;
        }
      }
    }

    return response;
  }
}
