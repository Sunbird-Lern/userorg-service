package org.sunbird.actor.location;

import java.util.Map;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.common.ProjectUtil;

public class LocationBackgroundActor extends BaseLocationActor {

  private final ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();

    switch (operation) {
      case "upsertLocationDataToES":
        upsertLocationDataToES(request);
        break;
      case "deleteLocationDataFromES":
        deleteLocationDataFromES(request);
        break;
      default:
        onReceiveUnsupportedOperation();
    }
  }

  private void deleteLocationDataFromES(Request request) {
    String locationId = (String) request.get(JsonKey.LOCATION_ID);
    esService.delete(ProjectUtil.EsType.location.getTypeName(), locationId, null);
  }

  private void upsertLocationDataToES(Request request) {
    Map<String, Object> location = (Map<String, Object>) request.getRequest().get(JsonKey.LOCATION);
    esService.upsert(
        ProjectUtil.EsType.location.getTypeName(),
        (String) location.get(JsonKey.ID),
        location,
        null);
  }
}
