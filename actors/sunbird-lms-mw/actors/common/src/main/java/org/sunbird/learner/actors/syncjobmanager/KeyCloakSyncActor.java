package org.sunbird.learner.actors.syncjobmanager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;

/** @author Amit Kumar */
@ActorConfig(
  tasks = {"syncKeycloak"},
  asyncTasks = {}
)
public class KeyCloakSyncActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private boolean isSSOEnabled =
      Boolean.parseBoolean(PropertiesCache.getInstance().getProperty(JsonKey.IS_SSO_ENABLED));
  private SSOManager ssoManager = SSOServiceFactory.getInstance();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request actorMessage) throws Throwable {
    String requestedOperation = actorMessage.getOperation();
    if (requestedOperation.equalsIgnoreCase(ActorOperations.SYNC_KEYCLOAK.getValue())) {
      // return SUCCESS to controller and run the sync process in background
      Response response = new Response();
      response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
      sender().tell(response, self());
      syncData(actorMessage);
    } else {
      onReceiveUnsupportedOperation(actorMessage.getOperation());
    }
  }

  private void syncData(Request message) {
    long startTime = System.currentTimeMillis();
    Map<String, Object> req = message.getRequest();
    Map<String, Object> responseMap = new HashMap<>();
    List<Map<String, Object>> reponseList = null;
    Map<String, Object> dataMap = (Map<String, Object>) req.get(JsonKey.DATA);
    List<Object> userIds = null;
    if (dataMap.containsKey(JsonKey.OBJECT_IDS) && null != dataMap.get(JsonKey.OBJECT_IDS)) {
      userIds = (List<Object>) dataMap.get(JsonKey.OBJECT_IDS);
    }
    Util.DbInfo dbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);

    if (null != userIds && !userIds.isEmpty()) {
      Response response =
          cassandraOperation.getRecordsByProperty(
              dbInfo.getKeySpace(), dbInfo.getTableName(), JsonKey.ID, userIds, null);
      reponseList = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    }
    if (null != reponseList && !reponseList.isEmpty()) {
      for (Map<String, Object> map : reponseList) {
        responseMap.put((String) map.get(JsonKey.ID), map);
      }
    } else {
      Response response =
          cassandraOperation.getAllRecords(dbInfo.getKeySpace(), dbInfo.getTableName(), null);
      reponseList = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
      if (null != reponseList) {
        for (Map<String, Object> map : reponseList) {
          responseMap.put((String) map.get(JsonKey.ID), map);
        }
      }
    }

    Iterator<Entry<String, Object>> itr = responseMap.entrySet().iterator();
    while (itr.hasNext()) {
      updateUserDetails(itr.next());
    }

    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
  }

  private void updateUserDetails(Entry<String, Object> entry) {
    String userId = entry.getKey();
    Map<String, Object> userMap = (Map<String, Object>) entry.getValue();
    // Decrypt user data
    UserUtility.decryptUserData(userMap);
    Util.DbInfo dbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    if (isSSOEnabled) {
      try {
        String res = ssoManager.syncUserData(userMap);
        if (!(!StringUtils.isBlank(res) && res.equalsIgnoreCase(JsonKey.SUCCESS))) {
          if (null == userMap.get(JsonKey.EMAIL_VERIFIED)) {
            Map<String, Object> map = new HashMap<>();
            if (ssoManager.isEmailVerified(userId)) {
              map.put(JsonKey.EMAIL_VERIFIED, true);
              map.put(JsonKey.ID, userId);
            } else {
              map.put(JsonKey.EMAIL_VERIFIED, false);
              map.put(JsonKey.ID, userId);
            }
            cassandraOperation.updateRecord(dbInfo.getKeySpace(), dbInfo.getTableName(), map, null);
            esService.update(ProjectUtil.EsType.user.getTypeName(), userId, map, null);
          }
        }
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    } else {
      logger.info("SSO is disabled , cann't sync user data to keycloak.");
    }
  }
}
