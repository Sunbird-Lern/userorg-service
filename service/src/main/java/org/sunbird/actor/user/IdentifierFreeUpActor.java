package org.sunbird.actor.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.Constants;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.service.user.impl.UserLookUpServiceImpl;
import org.sunbird.util.Util;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;

@ActorConfig(
  tasks = {"freeUpUserIdentity"},
  asyncTasks = {},
  dispatcher = "most-used-two-dispatcher"
)

/**
 * this Actor class is being used to free Up used User Identifier for now it only free Up user
 * Email, Phone.
 */
public class IdentifierFreeUpActor extends BaseActor {

  @Override
  public void onReceive(Request request) {
    String id = (String) request.get(JsonKey.ID);
    List<String> identifiers = (List) request.get(JsonKey.IDENTIFIER);
    RequestContext context = request.getRequestContext();
    freeUpUserIdentifier(id, identifiers, context);
  }

  private Map<String, Object> getUserById(String id, RequestContext context) {
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Response response =
        getCassandraOperation()
            .getRecordById(usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), id, context);
    List<Map<String, Object>> responseList = (List) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isEmpty(responseList)) {
      logger.info(
          context,
          String.format(
              "%s:%s:User not found with provided Id:%s",
              this.getClass().getSimpleName(), "getById", id));
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidUserId);
    }
    return responseList.get(0);
  }

  private Response processUserAttribute(
      Map<String, Object> userDbMap, List<String> identifiers, RequestContext context) {
    String userId = (String) userDbMap.get(JsonKey.ID);
    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.ID, userId);
    if (identifiers.contains(JsonKey.EMAIL)) {
      nullifyEmail(userDbMap, userMap);
      logger.info(
          context,
          String.format(
              "%s:%s:Nullified Email. WITH ID  %s",
              this.getClass().getSimpleName(), "freeUpUserIdentifier", userId));
    }
    if (identifiers.contains(JsonKey.PHONE)) {
      nullifyPhone(userDbMap, userMap);
      logger.info(
          context,
          String.format(
              "%s:%s:Nullified Phone. WITH ID  %s",
              this.getClass().getSimpleName(), "freeUpUserIdentifier", userId));
    }

    Response response = new Response();
    if (userMap.size() > 1) {
      response = updateUser(userMap, context);
      response.getResult().put(JsonKey.USER, userMap);
    } else {
      response.put(Constants.RESPONSE, Constants.SUCCESS);
    }
    return response;
  }

  private void nullifyEmail(Map<String, Object> userDbMap, Map<String, Object> updatedUserMap) {
    if (StringUtils.isNotBlank((String) userDbMap.get(JsonKey.EMAIL))) {
      updatedUserMap.put(JsonKey.PREV_USED_EMAIL, userDbMap.get(JsonKey.EMAIL));
      updatedUserMap.put(JsonKey.EMAIL, null);
      updatedUserMap.put(JsonKey.MASKED_EMAIL, null);
      updatedUserMap.put(JsonKey.FLAGS_VALUE, calculateFlagvalue(userDbMap));
    }
  }

  private void nullifyPhone(Map<String, Object> userDbMap, Map<String, Object> updatedUserMap) {
    if (StringUtils.isNotBlank((String) userDbMap.get(JsonKey.PHONE))) {
      updatedUserMap.put(JsonKey.PREV_USED_PHONE, userDbMap.get(JsonKey.PHONE));
      updatedUserMap.put(JsonKey.PHONE, null);
      updatedUserMap.put(JsonKey.MASKED_PHONE, null);
      updatedUserMap.put(JsonKey.COUNTRY_CODE, null);
      updatedUserMap.put(JsonKey.FLAGS_VALUE, calculateFlagvalue(userDbMap));
    }
  }

  private int calculateFlagvalue(Map<String, Object> userDbMap) {
    int flagsValue = 0;
    if (userDbMap.get(JsonKey.FLAGS_VALUE) != null
        && (int) userDbMap.get(JsonKey.FLAGS_VALUE) >= 4) {
      flagsValue = 4;
    }
    return flagsValue;
  }

  private Response updateUser(Map<String, Object> userDbMap, RequestContext context) {
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    return getCassandraOperation()
        .updateRecord(usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), userDbMap, context);
  }

  private void freeUpUserIdentifier(String id, List<String> identifiers, RequestContext context) {
    Map<String, Object> userDbMap = getUserById(id, context);
    Map<String, Object> userLookUpData = new HashMap<>(userDbMap);
    Response response = processUserAttribute(userDbMap, identifiers, context);
    removeEntryFromUserLookUp(userLookUpData, identifiers, context);
    Map<String, Object> updatedUserMap =
        (Map<String, Object>) response.getResult().remove(JsonKey.USER);
    if (MapUtils.isNotEmpty(updatedUserMap)) {
      saveUserDetailsToEs(updatedUserMap, context);
    }
    sender().tell(response, self());
    logger.info(
        context,
        String.format(
            "%s:%s:USER MAP SUCCESSFULLY UPDATED IN CASSANDRA. WITH ID  %s",
            this.getClass().getSimpleName(), "freeUpUserIdentifier", userDbMap.get(JsonKey.ID)));
  }

  /**
   * removing entry from user_lookup table
   *
   * @param userDbMap
   * @param identifiers
   * @param context
   */
  private void removeEntryFromUserLookUp(
      Map<String, Object> userDbMap, List<String> identifiers, RequestContext context) {
    logger.info(
        context,
        "IdentifierFreeUpActor:removeEntryFromUserLookUp remove following identifiers from lookUp table "
            + identifiers);
    List<Map<String, String>> reqMap = new ArrayList<>();
    Map<String, String> deleteLookUp = new HashMap<>();
    if (identifiers.contains(JsonKey.EMAIL)
        && StringUtils.isNotBlank((String) userDbMap.get(JsonKey.EMAIL))) {
      deleteLookUp.put(JsonKey.TYPE, JsonKey.EMAIL);
      deleteLookUp.put(JsonKey.VALUE, (String) userDbMap.get(JsonKey.EMAIL));
      reqMap.add(deleteLookUp);
    }
    if (identifiers.contains(JsonKey.PHONE)
        && StringUtils.isNotBlank((String) userDbMap.get(JsonKey.PHONE))) {
      deleteLookUp = new HashMap<>();
      deleteLookUp.put(JsonKey.TYPE, JsonKey.PHONE);
      deleteLookUp.put(JsonKey.VALUE, (String) userDbMap.get(JsonKey.PHONE));
      reqMap.add(deleteLookUp);
    }
    if (CollectionUtils.isNotEmpty(reqMap)) {
      UserLookUpServiceImpl userLookUp = new UserLookUpServiceImpl();
      userLookUp.deleteRecords(reqMap, context);
    }
  }

  private void saveUserDetailsToEs(Map<String, Object> userDbMap, RequestContext context) {
    getEsUtil()
        .update(
            ProjectUtil.EsType.user.getTypeName(),
            (String) userDbMap.get(JsonKey.ID),
            userDbMap,
            context);
  }

  private CassandraOperation getCassandraOperation() {
    return ServiceFactory.getInstance();
  }

  private ElasticSearchService getEsUtil() {
    return EsClientFactory.getInstance(JsonKey.REST);
  }
}
