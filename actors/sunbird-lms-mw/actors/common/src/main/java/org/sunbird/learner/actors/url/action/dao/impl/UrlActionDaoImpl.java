package org.sunbird.learner.actors.url.action.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.url.action.dao.UrlActionDao;
import org.sunbird.models.url.action.UrlAction;

public class UrlActionDaoImpl implements UrlActionDao {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ObjectMapper mapper = new ObjectMapper();
  private static UrlActionDao urlActionDao;
  private static final String KEYSPACE_NAME = "sunbird";
  private static final String TABLE_NAME = "url_action";

  public static UrlActionDao getInstance() {
    if (urlActionDao == null) {
      urlActionDao = new UrlActionDaoImpl();
    }
    return urlActionDao;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<UrlAction> getUrlActions() {
    Response urlActionResults = cassandraOperation.getAllRecords(KEYSPACE_NAME, TABLE_NAME);
    TypeReference<List<UrlAction>> urlActionType = new TypeReference<List<UrlAction>>() {};
    List<Map<String, Object>> urlActionMapList =
        (List<Map<String, Object>>) urlActionResults.get(JsonKey.RESPONSE);
    List<UrlAction> urlActionList = mapper.convertValue(urlActionMapList, urlActionType);
    return urlActionList;
  }
}
