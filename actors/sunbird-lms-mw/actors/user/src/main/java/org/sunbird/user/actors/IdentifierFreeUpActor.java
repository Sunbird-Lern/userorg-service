package org.sunbird.user.actors;

import java.util.List;
import java.util.Map;
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
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.UserFlagEnum;
import org.sunbird.learner.util.Util;
import scala.concurrent.Future;

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
    if (identifiers.contains(JsonKey.EMAIL)) {
      nullifyEmail(userDbMap);
      logger.info(
          context,
          String.format(
              "%s:%s:Nullified Email. WITH ID  %s",
              this.getClass().getSimpleName(), "freeUpUserIdentifier", userDbMap.get(JsonKey.ID)));
    }
    if (identifiers.contains(JsonKey.PHONE)) {
      nullifyPhone(userDbMap);
      logger.info(
          context,
          String.format(
              "%s:%s:Nullified Phone. WITH ID  %s",
              this.getClass().getSimpleName(), "freeUpUserIdentifier", userDbMap.get(JsonKey.ID)));
    }
    return updateUser(userDbMap, context);
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

  private Response updateUser(Map<String, Object> userDbMap, RequestContext context) {
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    return getCassandraOperation()
        .updateRecord(usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), userDbMap, context);
  }

  private void freeUpUserIdentifier(String id, List<String> identifiers, RequestContext context) {
    Map<String, Object> userDbMap = getUserById(id, context);
    Response response = processUserAttribute(userDbMap, identifiers, context);
    boolean esResponse = saveUserDetailsToEs(userDbMap, context);
    logger.info(
        context,
        "IdentifierFreeUpActor:freeUpUserIdentifier response got from ES for identifier freeup api :"
            + esResponse);
    sender().tell(response, self());
    logger.info(
        context,
        String.format(
            "%s:%s:USER MAP SUCCESSFULLY UPDATED IN CASSANDRA. WITH ID  %s",
            this.getClass().getSimpleName(), "freeUpUserIdentifier", userDbMap.get(JsonKey.ID)));
  }

  private boolean saveUserDetailsToEs(Map<String, Object> userDbMap, RequestContext context) {
    Future<Boolean> future =
        getEsUtil()
            .update(
                ProjectUtil.EsType.user.getTypeName(),
                (String) userDbMap.get(JsonKey.ID),
                userDbMap,
                context);
    return (boolean) ElasticSearchHelper.getResponseFromFuture(future);
  }

  private boolean isNullifyOperationValid(String identifier) {
    return StringUtils.isNotBlank(identifier);
  }

  private CassandraOperation getCassandraOperation() {
    return ServiceFactory.getInstance();
  }

  private ElasticSearchService getEsUtil() {
    return EsClientFactory.getInstance(JsonKey.REST);
  }
}
