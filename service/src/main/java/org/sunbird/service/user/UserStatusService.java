package org.sunbird.service.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.dao.user.UserDao;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.sso.SSOManager;
import org.sunbird.sso.SSOServiceFactory;
import org.sunbird.util.ProjectUtil;

public class UserStatusService {

  private UserService userService = UserServiceImpl.getInstance();

  public Response updateUserStatus(
      Map<String, Object> userMapES, String operation, RequestContext context) {
    String userId = (String) userMapES.get(JsonKey.USER_ID);
    boolean isBlocked = (Boolean) userMapES.get(JsonKey.IS_DELETED);
    User user = userService.getUserById(userId, context);

    if (operation.equals(ActorOperations.BLOCK_USER.getValue())
        && Boolean.TRUE.equals(user.getIsDeleted())) {
      throw new ProjectCommonException(
          ResponseCode.userAlreadyInactive,
          ResponseCode.userAlreadyInactive.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    if (operation.equals(ActorOperations.UNBLOCK_USER.getValue())
        && Boolean.FALSE.equals(user.getIsDeleted())) {
      throw new ProjectCommonException(
          ResponseCode.userAlreadyActive,
          ResponseCode.userAlreadyActive.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    ObjectMapper mapper = new ObjectMapper();
    User updatedUser = mapper.convertValue(userMapES, User.class);
    SSOManager ssoManager = SSOServiceFactory.getInstance();
    if (isBlocked) {
      ssoManager.deactivateUser(userMapES, context);
    } else {
      ssoManager.activateUser(userMapES, context);
    }
    UserDao userDao = UserDaoImpl.getInstance();
    return userDao.updateUser(updatedUser, context);
  }

  public Map<String, Object> getUserMap(String userId, String updatedBy, boolean blockUser) {
    Map<String, Object> esUserMap = new HashMap<>();
    esUserMap.put(JsonKey.IS_DELETED, blockUser);
    esUserMap.put(
        JsonKey.STATUS,
        blockUser ? ProjectUtil.Status.INACTIVE.getValue() : ProjectUtil.Status.ACTIVE.getValue());
    esUserMap.put(JsonKey.ID, userId);
    esUserMap.put(JsonKey.USER_ID, userId);
    esUserMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    esUserMap.put(JsonKey.UPDATED_BY, updatedBy);
    return esUserMap;
  }
}
