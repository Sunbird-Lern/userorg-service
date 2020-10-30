package org.sunbird.user.actors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;
import org.sunbird.user.dao.UserDao;
import org.sunbird.user.dao.impl.UserDaoImpl;
import org.sunbird.user.util.UserLookUp;
import org.sunbird.user.util.UserUtil;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {"updateUserName"},
  asyncTasks = {},
  dispatcher = "most-used-two-dispatcher"
)
public class UpdateUserNameActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);
  private UserDao userDao = UserDaoImpl.getInstance();
  private UserLookUp userLookUp = new UserLookUp();

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    if ("updateUserName".equalsIgnoreCase(operation)) {
      updateUserName(request);
    } else {
      onReceiveUnsupportedOperation("UpdateUserNameActor");
    }
  }

  private void updateUserName(Request request) {
    RequestContext context = request.getRequestContext();
    Map<String, Object> reqMap = request.getRequest();
    List<Map<String, Object>> userRespList = new ArrayList<>();
    boolean dryRun = true;
    if (null != reqMap.get("dryRun")) {
      dryRun = (boolean) reqMap.get("dryRun");
    }

    List<String> userIds = (List<String>) reqMap.get(JsonKey.USERIDS);
    if (userIds.size() > 100) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.errorMaxSizeExceeded,
          MessageFormat.format(
              ResponseCode.errorMaxSizeExceeded.getErrorMessage(), JsonKey.USERIDS, 100));
    }
    boolean finalDryRun = dryRun;
    // Iterate requested userids and validate for duplicate username
    userIds
        .stream()
        .forEach(
            userId -> {
              User user = userDao.getUserById(userId, context);
              Map<String, Object> searchQueryMap = new HashMap<>();
              searchQueryMap.put(JsonKey.USERNAME, user.getUserName());
              // ES search call on userName , this userlist might contains duplicate entry for same
              // username
              List<User> userList = userDao.searchUser(searchQueryMap, context);
              if (CollectionUtils.isNotEmpty(userList) && userList.size() > 1) {
                List<String> userIdList = new ArrayList<>();
                userList.forEach(
                    user1 -> {
                      userIdList.add(user1.getId());
                    });
                logger.info("users with duplicate userName : " + userIdList);
                // Iterate on each duplicate user list
                userList
                    .stream()
                    .forEach(
                        usr -> {
                          // Check for entry in user lookup table
                          List<Map<String, Object>> userLookupRes =
                              userLookUp.getRecordByType(
                                  JsonKey.USER_LOOKUP_FILED_USER_NAME,
                                  usr.getUserName(),
                                  false,
                                  context);
                          if (CollectionUtils.isNotEmpty(userLookupRes)) {
                            Map<String, Object> usrLookup = userLookupRes.get(0);
                            String usrId = (String) usrLookup.get(JsonKey.USER_ID);
                            if (usrId.equalsIgnoreCase(usr.getId())) {
                              logger.info(
                                  "User entry exist for userName in userlookup for userId : "
                                      + usrLookup.get(JsonKey.USER_ID));
                            } else if (!usrId.equalsIgnoreCase(usr.getId())) {
                              // There is no entry for this userId so update username
                              updateUserName(request, context, userRespList, finalDryRun, usr);
                            }
                          }

                          if (CollectionUtils.isEmpty(userLookupRes)) {
                            // If empty then generate user name and update user , userlookup and es
                            updateUserName(request, context, userRespList, finalDryRun, usr);
                          }
                        });
              } else {
                logger.info(
                    "For the given username only single or no user exist in ES. "
                        + user.getUserName());
              }
            });
    Response response = new Response();
    response.getResult().put(JsonKey.RESPONSE, userRespList);
    sender().tell(response, self());
  }

  private void updateUserName(
      Request request,
      RequestContext context,
      List<Map<String, Object>> userRespList,
      boolean finalDryRun,
      User usr) {
    // generate user name and update user , userlookup and es
    logger.info("User entry for userName does not exist in userlookup for userId : " + usr.getId());
    String firstName = usr.getFirstName();
    String lastName = usr.getLastName();

    String name = String.join(" ", firstName, StringUtils.isNotBlank(lastName) ? lastName : "");
    name = UserUtil.transliterateUserName(name);
    String userName = null;
    while (StringUtils.isBlank(userName)) {
      userName = UserUtil.getUsername(name, context);
      logger.info(context, "Generated userName : " + userName);
    }
    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.ID, usr.getId());
    userMap.put(JsonKey.USERNAME, userName);
    try {
      userMap = UserUtility.encryptUserData(userMap);
    } catch (Exception ex) {
      logger.error(context, "Exception occurred while encrypting user data:" + userMap, ex);
      ProjectCommonException.throwServerErrorException(ResponseCode.userDataEncryptionError, null);
    }
    try {
      if (finalDryRun) {
        Map<String, Object> userOpMap = new HashMap<>(userMap);
        logger.info("usermap which update user and userlookup table is :: " + userOpMap);
        UserUtility.decryptUserData(userOpMap);
        userRespList.add(userOpMap);
      } else {
        // fetch before updating user table (id, username) for validation
        Map<String, Object> userResMap = new HashMap<>();
        List<String> fields = new ArrayList<>();
        fields.add(JsonKey.ID);
        fields.add(JsonKey.USERNAME);
        Response res =
            cassandraOperation.getPropertiesValueById(
                usrDbInfo.getKeySpace(),
                usrDbInfo.getTableName(),
                (String) userMap.get(JsonKey.ID),
                fields,
                context);

        List<Map<String, Object>> usrList = (List<Map<String, Object>>) res.get(JsonKey.RESPONSE);
        if (CollectionUtils.isNotEmpty(usrList)) {
          Map<String, Object> userOpMap = new HashMap<>(usrList.get(0));
          UserUtility.decryptUserData(userOpMap);
          userResMap.put("beforeUserTableUpdateResponse", userOpMap);
        }

        cassandraOperation.updateRecord(
            usrDbInfo.getKeySpace(),
            usrDbInfo.getTableName(),
            userMap,
            request.getRequestContext());

        // print user map as after result
        Map<String, Object> opMap = new HashMap<>(userMap);
        UserUtility.decryptUserData(opMap);
        userResMap.put("afterUserTableUpdateResponse", opMap);

        insertIntoUserLookUp(userMap, request.getRequestContext());

        Future<Boolean> responseF =
            esUtil.update(ProjectUtil.EsType.user.getTypeName(), usr.getId(), userMap, context);
        boolean esResponse = (boolean) ElasticSearchHelper.getResponseFromFuture(responseF);
        userResMap.put("esResponse", esResponse);
        userRespList.add(userResMap);
        if (esResponse) {
          logger.info(context, "unable to save the user data to ES with identifier " + usr.getId());
        } else {
          logger.info(context, "saved the user data to ES with identifier " + usr.getId());
        }
      }
    } catch (Exception ex) {
      logger.error(
          context, "Exception occurred while updating userName in user and userlookup table.", ex);
    }
  }

  private Response insertIntoUserLookUp(Map<String, Object> userMap, RequestContext context) {
    List<Map<String, Object>> list = new ArrayList<>();
    if (userMap.get(JsonKey.USERNAME) != null) {
      Map<String, Object> lookUp = new HashMap<>();
      lookUp.put(JsonKey.TYPE, JsonKey.USER_LOOKUP_FILED_USER_NAME);
      lookUp.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
      lookUp.put(JsonKey.VALUE, userMap.get(JsonKey.USERNAME));
      list.add(lookUp);
    }
    Response response = null;
    if (CollectionUtils.isNotEmpty(list)) {
      UserLookUp userLookUp = new UserLookUp();
      response = userLookUp.insertRecords(list, context);
    }
    return response;
  }
}
