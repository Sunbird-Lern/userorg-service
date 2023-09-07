package org.sunbird.service.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.dao.user.UserDao;
import org.sunbird.dao.user.UserOwnershipTransferDao;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.dao.user.impl.UserOwnershipTransferDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.impl.UserExternalIdentityServiceImpl;
import org.sunbird.service.user.impl.UserLookUpServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.sso.SSOManager;
import org.sunbird.sso.SSOServiceFactory;
import org.sunbird.util.ProjectUtil;

public class UserStatusService {

  private final UserService userService = UserServiceImpl.getInstance();
  private final UserLookUpServiceImpl userLookUpService = new UserLookUpServiceImpl();
  private final UserExternalIdentityService userExternalIdentityService =
      UserExternalIdentityServiceImpl.getInstance();

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
      try {
        List<Map<String, String>> dbUserExternalIds =
            userExternalIdentityService.getUserExternalIds(userId, context);
        if (dbUserExternalIds != null && !dbUserExternalIds.isEmpty()) {
          userExternalIdentityService.deleteUserExternalIds(dbUserExternalIds, context);
        }
      } catch (Exception ex) {
        throw new ProjectCommonException(
            ResponseCode.userStatusError,
            MessageFormat.format(ResponseCode.userStatusError.getErrorMessage(), "delete"),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }

      List<String> identifiers = new ArrayList<>();
      identifiers.add(JsonKey.EMAIL);
      identifiers.add(JsonKey.PHONE);
      identifiers.add(JsonKey.USER_LOOKUP_FILED_EXTERNAL_ID);
      Map<String, Object> userLookUpData = mapper.convertValue(user, Map.class);
      userLookUpService.removeEntryFromUserLookUp(userLookUpData, identifiers, context);
      ssoManager.removeUser(userMapES, context);

      /* TRIGGER BACKGROUND ACTOR
         1. Insert record into sunbird.user_ownership_transfer with status as 0 (submitted)
         2. send notification to tenant org_admin users via background actor
         3. trigger kafka events for user-cache-updater
          - to remove consent entry in consent tables
      */

      UserOwnershipTransferDao ownershipTransferDao = UserOwnershipTransferDaoImpl.getInstance();
      Map<String, Object> ownerShipTransferDetails = new HashMap<>();
      ownerShipTransferDetails.put(JsonKey.ORGANISATION_ID, user.getOrganisationId());
      ownerShipTransferDetails.put(JsonKey.USER_ID, userId);
      ownerShipTransferDetails.put(JsonKey.USER_NAME, user.getUserName());
      ownerShipTransferDetails.put(JsonKey.ROLES, user.getRoles());
      ownerShipTransferDetails.put(JsonKey.CREATED_BY, userId);
      ownerShipTransferDetails.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
      //      if(user.getRoles() != null && user.getRoles().equals(new
      // ArrayList<>(List.of("PUBLIC"))))
      //      {
      ownerShipTransferDetails.put(JsonKey.STATUS, 1);
      //      }
      ownershipTransferDao.createUserOwnershipTransfer(ownerShipTransferDetails, context);

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
