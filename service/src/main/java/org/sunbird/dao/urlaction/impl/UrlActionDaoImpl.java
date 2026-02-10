package org.sunbird.dao.urlaction.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.urlaction.UrlActionDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.urlaction.UrlAction;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;

public class UrlActionDaoImpl implements UrlActionDao {

  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private final ObjectMapper mapper = new ObjectMapper();
  private static UrlActionDao urlActionDao;

  public static UrlActionDao getInstance() {
    if (urlActionDao == null) {
      urlActionDao = new UrlActionDaoImpl();
    }
    return urlActionDao;
  }

  @Override
  public List<UrlAction> getUrlActions() {
    String KEYSPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);
    String TABLE_NAME = "url_action";
    Response urlActionResults = cassandraOperation.getAllRecords(KEYSPACE_NAME, TABLE_NAME, null);
    TypeReference<List<UrlAction>> urlActionType = new TypeReference<>() {};
    List<Map<String, Object>> urlActionMapList =
        (List<Map<String, Object>>) urlActionResults.get(JsonKey.RESPONSE);
    List<UrlAction> urlActionList = mapper.convertValue(urlActionMapList, urlActionType);
    return urlActionList;
  }
}
