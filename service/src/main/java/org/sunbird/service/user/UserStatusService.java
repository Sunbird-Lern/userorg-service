package org.sunbird.service.user;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.user.UserDao;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.impl.UserLookUpServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.sso.SSOManager;
import org.sunbird.sso.SSOServiceFactory;
import org.sunbird.util.ProjectUtil;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class UserStatusService {

  private final UserService userService = UserServiceImpl.getInstance();
  private final UserLookUpServiceImpl userLookUp = new UserLookUpServiceImpl();
  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  @Inject
  @Named("user_external_identity_management_actor")
  private ActorRef userExternalIdManagementActor;

  public Response updateUserStatus(
      Map<String, Object> userMapES, String operation, RequestContext context) {
    String userId = (String) userMapES.get(JsonKey.USER_ID);
    boolean isBlocked = (Boolean) userMapES.get(JsonKey.IS_BLOCKED);
    boolean isDeleted =
        ((int) userMapES.get(JsonKey.STATUS) == ProjectUtil.Status.DELETED.getValue());
    User user = userService.getUserById(userId, context);

    if (operation.equals(ActorOperations.BLOCK_USER.getValue())
        && ProjectUtil.Status.DELETED.getValue() != user.getStatus()
        && Boolean.TRUE.equals(user.getIsDeleted())) {
      throw new ProjectCommonException(
          ResponseCode.userStatusError,
          MessageFormat.format(ResponseCode.userStatusError.getErrorMessage(), "inactive"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    if (operation.equals(ActorOperations.UNBLOCK_USER.getValue())
        && ProjectUtil.Status.DELETED.getValue() != user.getStatus()
        && Boolean.FALSE.equals(user.getIsDeleted())) {
      throw new ProjectCommonException(
          ResponseCode.userStatusError,
          MessageFormat.format(ResponseCode.userStatusError.getErrorMessage(), "active"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    ObjectMapper mapper = new ObjectMapper();
    User updatedUser = mapper.convertValue(userMapES, User.class);
    SSOManager ssoManager = SSOServiceFactory.getInstance();

    if (isDeleted) {
      List<String> identifiers = new ArrayList<>();
      identifiers.add(JsonKey.EMAIL);
      identifiers.add(JsonKey.PHONE);
      identifiers.add(JsonKey.USER_LOOKUP_FILED_EXTERNAL_ID);
      Map<String, Object> userLookUpData = mapper.convertValue(user, Map.class);
      userLookUp.removeEntryFromUserLookUp(userLookUpData, identifiers, context);
      ssoManager.removeUser(userMapES, context);

      try {
        List<Map<String, String>> dbResExternalIds;
        Map<String, Object> req = new HashMap<>();
        req.put(JsonKey.USER_ID, userId);
        Response response =
            cassandraOperation.getRecordById(
                ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE),
                JsonKey.USR_EXT_IDNT_TABLE,
                req,
                context);
        if (null != response && null != response.getResult()) {
          dbResExternalIds = (List<Map<String, String>>) response.getResult().get(JsonKey.RESPONSE);
          if (dbResExternalIds != null && !dbResExternalIds.isEmpty()) {

            for (Map<String, String> extIdMap : dbResExternalIds) {
              extIdMap.put(JsonKey.OPERATION, JsonKey.REMOVE);
            }

            Timeout t = new Timeout(Duration.create(5, TimeUnit.SECONDS));

            Map<String, Object> userExtIdsReq = new HashMap<>();
            userExtIdsReq.put(JsonKey.ID, userId);
            userExtIdsReq.put(JsonKey.USER_ID, userId);
            userExtIdsReq.put(JsonKey.EXTERNAL_IDS, dbResExternalIds);

            Request userRequest = new Request();
            userRequest.setOperation(
                ActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS.getValue());
            userExtIdsReq.put(JsonKey.OPERATION_TYPE, JsonKey.REMOVE);
            userRequest.getRequest().putAll(userExtIdsReq);

            if (null != userExternalIdManagementActor) {
              Future<Object> future = Patterns.ask(userExternalIdManagementActor, userRequest, t);
              Await.result(future, t.duration());
            }
          }
        }
      } catch (Exception ex) {
        throw new ProjectCommonException(
            ResponseCode.userStatusError,
            MessageFormat.format(ResponseCode.userStatusError.getErrorMessage(), "delete"),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }

      // trigger kafka events for user-cache-updater
    } else if (isBlocked) {
      ssoManager.deactivateUser(userMapES, context);
    } else {
      ssoManager.activateUser(userMapES, context);
    }
    UserDao userDao = UserDaoImpl.getInstance();
    return userDao.updateUser(updatedUser, context);
  }

  public Map<String, Object> getUserMap(
      String userId, String updatedBy, boolean blockUser, boolean deleteUser) {
    Map<String, Object> esUserMap = new HashMap<>();
    esUserMap.put(JsonKey.IS_BLOCKED, blockUser);
    if (deleteUser) {
      esUserMap.put(JsonKey.STATUS, ProjectUtil.Status.DELETED.getValue());
      esUserMap.put(JsonKey.MASKED_EMAIL, "");
      esUserMap.put(JsonKey.MASKED_PHONE, "");
      esUserMap.put(JsonKey.FIRST_NAME, "");
      esUserMap.put(JsonKey.LAST_NAME, "");
      esUserMap.put(JsonKey.DOB, "");
      esUserMap.put(JsonKey.PHONE, "");
      esUserMap.put(JsonKey.PREV_USED_EMAIL, "");
      esUserMap.put(JsonKey.PREV_USED_PHONE, "");
      esUserMap.put(JsonKey.PROFILE_LOCATION, "");
      esUserMap.put(JsonKey.RECOVERY_EMAIL, "");
      esUserMap.put(JsonKey.RECOVERY_PHONE, "");
      esUserMap.put(JsonKey.USER_NAME, "");
    } else {
      esUserMap.put(
          JsonKey.STATUS,
          blockUser
              ? ProjectUtil.Status.INACTIVE.getValue()
              : ProjectUtil.Status.ACTIVE.getValue());
    }
    esUserMap.put(JsonKey.ID, userId);
    esUserMap.put(JsonKey.USER_ID, userId);
    esUserMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    esUserMap.put(JsonKey.UPDATED_BY, updatedBy);
    return esUserMap;
  }
}
