package org.sunbird.user.service;

import akka.actor.ActorRef;
import java.util.List;
import java.util.Map;
import org.sunbird.common.request.Request;
import org.sunbird.models.user.User;

public interface UserService {

  User getUserById(String userId);

  void validateUserId(Request request, String managedById);

  void validateUploader(Request request);

  Map<String, Object> esGetPublicUserProfileById(String userId);

  Map<String, Object> esGetPrivateUserProfileById(String userId);

  void syncUserProfile(
      String userId, Map<String, Object> userDataMap, Map<String, Object> userPrivateDataMap);

  String getRootOrgIdFromChannel(String channel);

  String getCustodianChannel(Map<String, Object> userMap, ActorRef actorRef);

  List<Map<String, Object>> esSearchUserByFilters(Map<String, Object> filters);

  List<String> generateUsernames(String name, List<String> excludedUsernames);

  List<String> getEncryptedList(List<String> dataList);

  String getCustodianOrgId(ActorRef actorRef);

  Map<String, Object> fetchEncryptedToken(String parentId, List<Map<String, Object>> respList);

  void appendEncryptedToken(Map<String, Object> encryptedTokenList, List<Map<String, Object>> respList);
}
