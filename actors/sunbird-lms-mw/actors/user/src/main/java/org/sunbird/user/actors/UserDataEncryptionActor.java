package org.sunbird.user.actors;

import java.util.ArrayList;
import java.util.List;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;

@ActorConfig(
  tasks = {"encryptUserData", "decryptUserData"},
  asyncTasks = {}
)
public class UserDataEncryptionActor extends BaseActor {

  @Override
  public void onReceive(Request actorMessage) throws Throwable {
    String operation = actorMessage.getOperation();
    switch (operation) {
      case "encryptUserData":
        encryptUserData(actorMessage);
        break;
      case "decryptUserData":
        decryptUserData(actorMessage);
        break;
      default:
        onReceiveUnsupportedOperation(actorMessage.getOperation());
        break;
    }
  }

  private void decryptUserData(Request actorMessage) {
    triggerBackgroundOperation(actorMessage, ActorOperations.BACKGROUND_DECRYPTION.getValue());
  }

  private void encryptUserData(Request actorMessage) {
    triggerBackgroundOperation(actorMessage, ActorOperations.BACKGROUND_ENCRYPTION.getValue());
  }

  @SuppressWarnings("unchecked")
  private void triggerBackgroundOperation(Request actorMessage, String backgroundOperation) {
    Response resp = new Response();
    resp.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(resp, self());

    List<String> userIds = (List<String>) actorMessage.getRequest().get(JsonKey.USER_IDs);
    int size = userIds.size();
    int startIndex = 0;
    while (startIndex < size) {
      Request backgroundRequest = new Request();
      backgroundRequest.setOperation(backgroundOperation);
      int lastIndex = lastIndexOfUserIdsBatch(size, startIndex);
      List<String> userIdsList = getUserIdsForEncryption(userIds, startIndex, lastIndex);
      backgroundRequest.getRequest().put(JsonKey.USER_IDs, userIdsList);
      startIndex = lastIndex + 1;
      tellToAnother(backgroundRequest);
    }
  }

  private int lastIndexOfUserIdsBatch(int size, int startIndex) {
    int maximumSizeAllowed =
        Integer.parseInt(
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_USER_MAX_ENCRYPTION_LIMIT).trim());
    int maximumIndex = startIndex + maximumSizeAllowed - 1;
    if (maximumIndex < size) {
      return maximumIndex;
    } else {
      return size - 1;
    }
  }

  private List<String> getUserIdsForEncryption(
      List<String> userIds, int startIndex, int lastIndex) {
    List<String> userIdsList = new ArrayList<>();
    for (int index = startIndex; index <= lastIndex; index++) {
      userIdsList.add(userIds.get(index));
    }
    return userIdsList;
  }
}
