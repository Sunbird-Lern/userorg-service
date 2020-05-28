package org.sunbird.user.actors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.UserFlagEnum;
import org.sunbird.learner.util.Util;
import scala.concurrent.Future;

import java.util.List;
import java.util.Map;

@ActorConfig(
  tasks = {"freeUpUserIdentity"},
  asyncTasks = {}
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
    freeUpUserIdentifier(id, identifiers);
  }

  private Map<String, Object> getUserById(String id) {
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Response response =
        getCassandraOperation().getRecordById(usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), id);
    List<Map<String, Object>> responseList = (List) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isEmpty(responseList)) {
      ProjectLogger.log(
          String.format(
              "%s:%s:User not found with provided Id:%s",
              this.getClass().getSimpleName(), "getById", id),
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidUserId);
    }
    return responseList.get(0);
  }

  private Response processUserAttribute(Map<String, Object> userDbMap, List<String> identifiers) {

    if (identifiers.contains(JsonKey.EMAIL)) {
      nullifyEmail(userDbMap);
      ProjectLogger.log(
          String.format(
              "%s:%s:Nullified Email. WITH ID  %s",
              this.getClass().getSimpleName(), "freeUpUserIdentifier", userDbMap.get(JsonKey.ID)),
          LoggerEnum.INFO.name());
    }
    if (identifiers.contains(JsonKey.PHONE)) {
      nullifyPhone(userDbMap);
      ProjectLogger.log(
          String.format(
              "%s:%s:Nullified Phone. WITH ID  %s",
              this.getClass().getSimpleName(), "freeUpUserIdentifier", userDbMap.get(JsonKey.ID)),
          LoggerEnum.INFO.name());
    }
    return updateUser(userDbMap);
  }

  private void nullifyEmail(Map<String, Object> userDbMap) {
    if (isNullifyOperationValid((String) userDbMap.get(JsonKey.EMAIL))) {
      userDbMap.replace(JsonKey.PREV_USED_EMAIL, userDbMap.get(JsonKey.EMAIL));
      userDbMap.replace(JsonKey.EMAIL, null);
      userDbMap.replace(JsonKey.MASKED_EMAIL, null);
      userDbMap.replace(JsonKey.EMAIL_VERIFIED, false);
      userDbMap.put(
          JsonKey.FLAGS_VALUE, calculateFlagvalue(UserFlagEnum.EMAIL_VERIFIED, userDbMap));
    }
  }

  private void nullifyPhone(Map<String, Object> userDbMap) {
    if (isNullifyOperationValid((String) userDbMap.get(JsonKey.PHONE))) {
      userDbMap.replace(JsonKey.PREV_USED_PHONE, userDbMap.get(JsonKey.PHONE));
      userDbMap.replace(JsonKey.PHONE, null);
      userDbMap.replace(JsonKey.MASKED_PHONE, null);
      userDbMap.replace(JsonKey.PHONE_VERIFIED, false);
      userDbMap.replace(JsonKey.COUNTRY_CODE, null);
      userDbMap.put(
          JsonKey.FLAGS_VALUE, calculateFlagvalue(UserFlagEnum.PHONE_VERIFIED, userDbMap));
    }
  }

  private int calculateFlagvalue(UserFlagEnum identifier, Map<String, Object> userDbMap) {
    int flagsValue = 0;
    if (userDbMap.get(JsonKey.FLAGS_VALUE) != null
        && (int) userDbMap.get(JsonKey.FLAGS_VALUE) > 0) {
      flagsValue = (int) userDbMap.get(JsonKey.FLAGS_VALUE);
      flagsValue = flagsValue - identifier.getUserFlagValue();
    }
    return flagsValue;
  }

  private Response updateUser(Map<String, Object> userDbMap) {
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    return getCassandraOperation().updateRecord(
        usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), userDbMap);
  }

  private void freeUpUserIdentifier(String id, List<String> identifiers) {
    Map<String, Object> userDbMap = getUserById(id);
    Response response = processUserAttribute(userDbMap, identifiers);
    boolean esResponse = saveUserDetailsToEs(userDbMap);
    ProjectLogger.log(
        "IdentifierFreeUpActor:freeUpUserIdentifier response got from ES for identifier freeup api :"
            + esResponse,
        LoggerEnum.INFO.name());
    sender().tell(response, self());
    ProjectLogger.log(
        String.format(
            "%s:%s:USER MAP SUCCESSFULLY UPDATED IN CASSANDRA. WITH ID  %s",
            this.getClass().getSimpleName(), "freeUpUserIdentifier", userDbMap.get(JsonKey.ID)),
        LoggerEnum.INFO.name());
  }

  private boolean saveUserDetailsToEs(Map<String, Object> userDbMap) {
    Future<Boolean> future =
        getEsUtil().update(
            ProjectUtil.EsType.user.getTypeName(), (String) userDbMap.get(JsonKey.ID), userDbMap);
    return (boolean) ElasticSearchHelper.getResponseFromFuture(future);
  }

  private boolean isNullifyOperationValid(String identifier) {
    return StringUtils.isNotBlank(identifier);
  }


  private CassandraOperation getCassandraOperation()
  {
    return ServiceFactory.getInstance();
  }

  private ElasticSearchService getEsUtil(){
    return EsClientFactory.getInstance(JsonKey.REST);
  }
}
