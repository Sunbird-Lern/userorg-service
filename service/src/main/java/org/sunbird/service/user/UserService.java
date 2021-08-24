package org.sunbird.service.user;

import akka.actor.ActorRef;
import java.util.List;
import java.util.Map;
import org.sunbird.model.user.User;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface UserService {

  Response createUser(Map<String, Object> user, RequestContext context);

  Response updateUser(Map<String, Object> user, RequestContext context);

  User getUserById(String userId, RequestContext context);

  void validateUserId(Request request, String managedById, RequestContext context);

  void validateUploader(Request request, RequestContext context);

  Map<String, Object> esGetPublicUserProfileById(String userId, RequestContext context);

  String getRootOrgIdFromChannel(String channel, RequestContext context);

  String getCustodianChannel(
      Map<String, Object> userMap, ActorRef actorRef, RequestContext context);

  List<String> generateUsernames(
      String name, List<String> excludedUsernames, RequestContext context);

  List<String> getEncryptedList(List<String> dataList, RequestContext context);

  String getCustodianOrgId(ActorRef actorRef, RequestContext context);

  Map<String, Object> fetchEncryptedToken(
      String parentId, List<Map<String, Object>> respList, RequestContext context);

  void appendEncryptedToken(
      Map<String, Object> encryptedTokenList,
      List<Map<String, Object>> respList,
      RequestContext context);

  List<Map<String, Object>> searchUserNameInUserLookup(
      List<String> encUserNameList, RequestContext context);

  Response userLookUpByKey(String key, String value, List<String> fields, RequestContext context);

  Response saveUserAttributes(
      Map<String, Object> userMap, ActorRef actorRef, RequestContext context);

  String getDecryptedEmailPhoneByUserId(String userId, String type, RequestContext context);

  List<Map<String, Object>> getDecryptedEmailPhoneByUserIds(
      List<String> userIds, String type, RequestContext context);

  List<Map<String, Object>> getUserEmailsBySearchQuery(
      Map<String, Object> searchQuery, RequestContext context);

  boolean updateUserDataToES(String identifier, Map<String, Object> data, RequestContext context);
}
