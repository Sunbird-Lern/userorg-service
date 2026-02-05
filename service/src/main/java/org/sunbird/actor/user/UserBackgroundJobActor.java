package org.sunbird.actor.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.common.ProjectUtil;
import org.sunbird.util.user.UserUtil;
import scala.concurrent.Future;

public class UserBackgroundJobActor extends BaseActor {

  private final ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    switch (operation) {
      case "upsertUserDetailsToES":
        saveUserDataToES(request);
        break;
      case "upsertUserOrgDetailsToES":
        saveUserOrgDetailsToES(request);
        break;
      default:
        onReceiveUnsupportedOperation();
        break;
    }
  }

  private void saveUserOrgDetailsToES(Request request) {
    Map<String, Object> userDetails = request.getRequest();
    Map<String, Object> userOrgMap = new HashMap<>();
    userOrgMap.put(JsonKey.ID, userDetails.get(JsonKey.ID));
    userOrgMap.put(
        JsonKey.ORGANISATIONS,
        UserUtil.getActiveUserOrgDetails(
            (String) userDetails.get(JsonKey.ID), request.getRequestContext()));
    logger.info(request.getRequestContext(), "Updating saveUserOrgDetailsToES");
    upsertDataToElastic(
        ProjectUtil.EsType.user.getTypeName(),
        (String) userDetails.get(JsonKey.ID),
        userOrgMap,
        request.getRequestContext());
  }

  private void saveUserDataToES(Request request) {
    Map<String, Object> userDetails = request.getRequest();
    logger.info(request.getRequestContext(), "Updating saveUserDataToES");
    userDetails.remove(JsonKey.PASSWORD);
    ObjectMapper mapper = new ObjectMapper();
    User user = mapper.convertValue(userDetails, User.class);
    userDetails = mapper.convertValue(user, Map.class);
    upsertDataToElastic(
        ProjectUtil.EsType.user.getTypeName(),
        (String) userDetails.get(JsonKey.ID),
        userDetails,
        request.getRequestContext());
  }

  private void upsertDataToElastic(
      String typeName, String id, Map<String, Object> userDetails, RequestContext context) {

    Future<Boolean> bool = esUtil.upsert(typeName, id, userDetails, context);

    logger.info(
        context,
        "Getting ES save response for type , identifier=="
            + typeName
            + "  "
            + id
            + "  "
            + ElasticSearchHelper.getResponseFromFuture(bool));
  }
}
