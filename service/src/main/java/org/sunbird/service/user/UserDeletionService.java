package org.sunbird.service.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.dao.user.UserDao;
import org.sunbird.dao.user.UserDeletionStatusDao;
import org.sunbird.dao.user.UserOwnershipTransferDao;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.dao.user.impl.UserDeletionStatusDaoImpl;
import org.sunbird.dao.user.impl.UserOwnershipTransferDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.impl.UserExternalIdentityServiceImpl;
import org.sunbird.service.user.impl.UserLookUpServiceImpl;
import org.sunbird.sso.SSOManager;
import org.sunbird.util.ProjectUtil;

public class UserDeletionService {

  private final UserLookUpServiceImpl userLookUpService = new UserLookUpServiceImpl();
  private final UserExternalIdentityService userExternalIdentityService =
      UserExternalIdentityServiceImpl.getInstance();

  public Response deleteUser(
      String userId,
      SSOManager ssoManager,
      User user,
      Map<String, Object> userMapES,
      RequestContext context) {
    ObjectMapper mapper = new ObjectMapper();
    List<Map<String, Object>> deletionReqMap = new ArrayList<>();
    Map<String, Object> deletionStatus = new HashMap<>();
    try {
      ssoManager.removeUser(userMapES, context);
      deletionStatus.put(JsonKey.TYPE, JsonKey.CREDENTIALS_STATUS);
      deletionStatus.put(JsonKey.USER_ID, userId);
      deletionStatus.put(JsonKey.VALUE, true);
      deletionReqMap.add(deletionStatus);
    } catch (Exception ex) {
      deletionStatus.put(JsonKey.TYPE, JsonKey.CREDENTIALS_STATUS);
      deletionStatus.put(JsonKey.USER_ID, userId);
      deletionStatus.put(JsonKey.VALUE, false);
      deletionReqMap.add(deletionStatus);

      throw new ProjectCommonException(
          ResponseCode.userStatusError,
          MessageFormat.format(ResponseCode.userStatusError.getErrorMessage(), "delete"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    try {
      Map<String, Object> userLookUpData = mapper.convertValue(user, Map.class);
      List<String> identifiers = new ArrayList<>();
      identifiers.add(JsonKey.EMAIL);
      identifiers.add(JsonKey.PHONE);
      identifiers.add(JsonKey.USER_LOOKUP_FILED_EXTERNAL_ID);
      userLookUpService.removeEntryFromUserLookUp(userLookUpData, identifiers, context);
      deletionStatus.put(JsonKey.TYPE, JsonKey.USER_LOOK_UP_STATUS);
      deletionStatus.put(JsonKey.USER_ID, userId);
      deletionStatus.put(JsonKey.VALUE, true);
      deletionReqMap.add(deletionStatus);
    } catch (Exception ex) {
      deletionStatus.put(JsonKey.TYPE, JsonKey.USER_LOOK_UP_STATUS);
      deletionStatus.put(JsonKey.USER_ID, userId);
      deletionStatus.put(JsonKey.VALUE, false);
      deletionReqMap.add(deletionStatus);

      throw new ProjectCommonException(
          ResponseCode.userStatusError,
          MessageFormat.format(ResponseCode.userStatusError.getErrorMessage(), "delete"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    try {
      List<Map<String, String>> dbUserExternalIds =
          userExternalIdentityService.getUserExternalIds(userId, context);
      if (dbUserExternalIds != null && !dbUserExternalIds.isEmpty()) {
        userExternalIdentityService.deleteUserExternalIds(dbUserExternalIds, context);
      }
      deletionStatus.put(JsonKey.TYPE, JsonKey.USER_EXTERNAL_ID_STATUS);
      deletionStatus.put(JsonKey.USER_ID, userId);
      deletionStatus.put(JsonKey.VALUE, true);
      deletionReqMap.add(deletionStatus);
    } catch (Exception ex) {
      deletionStatus.put(JsonKey.TYPE, JsonKey.USER_EXTERNAL_ID_STATUS);
      deletionStatus.put(JsonKey.USER_ID, userId);
      deletionStatus.put(JsonKey.VALUE, false);
      deletionReqMap.add(deletionStatus);

      throw new ProjectCommonException(
          ResponseCode.userStatusError,
          MessageFormat.format(ResponseCode.userStatusError.getErrorMessage(), "delete"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    try {
      User updatedUser = mapper.convertValue(userMapES, User.class);
      UserDao userDao = UserDaoImpl.getInstance();
      userDao.updateUser(updatedUser, context);
      deletionStatus.put(JsonKey.TYPE, JsonKey.USER_TABLE_STATUS);
      deletionStatus.put(JsonKey.USER_ID, userId);
      deletionStatus.put(JsonKey.VALUE, true);
      deletionReqMap.add(deletionStatus);
    } catch (Exception ex) {
      deletionStatus.put(JsonKey.TYPE, JsonKey.USER_TABLE_STATUS);
      deletionStatus.put(JsonKey.USER_ID, userId);
      deletionStatus.put(JsonKey.VALUE, false);
      deletionReqMap.add(deletionStatus);

      throw new ProjectCommonException(
          ResponseCode.userStatusError,
          MessageFormat.format(ResponseCode.userStatusError.getErrorMessage(), "delete"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    try {
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
      ownerShipTransferDetails.put(JsonKey.STATUS, 0);
      //      }
      ownershipTransferDao.createUserOwnershipTransfer(ownerShipTransferDetails, context);
      deletionStatus.put(JsonKey.TYPE, JsonKey.USER_OWNERSHIP_TRANSFER_STATUS);
      deletionStatus.put(JsonKey.USER_ID, userId);
      deletionStatus.put(JsonKey.VALUE, true);
      deletionReqMap.add(deletionStatus);
    } catch (Exception ex) {
      deletionStatus.put(JsonKey.TYPE, JsonKey.USER_OWNERSHIP_TRANSFER_STATUS);
      deletionStatus.put(JsonKey.USER_ID, userId);
      deletionStatus.put(JsonKey.VALUE, false);
      deletionReqMap.add(deletionStatus);

      throw new ProjectCommonException(
          ResponseCode.userStatusError,
          MessageFormat.format(ResponseCode.userStatusError.getErrorMessage(), "delete"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    /* TRIGGER BACKGROUND ACTOR
       2. send notification to tenant org_admin users via background actor
       3. trigger kafka events for user-cache-updater
        - to remove consent entry in consent tables
    */

    UserDeletionStatusDao userDeletionStatusDao = UserDeletionStatusDaoImpl.getInstance();
    Response userDeletionStatusResponse =
        userDeletionStatusDao.insertRecords(deletionReqMap, context);

    return userDeletionStatusResponse;
  }
}
