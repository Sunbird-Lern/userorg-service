package org.sunbird.actor.organisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.PropertiesCache;

public class OrganisationBackgroundActor extends BaseActor {
  private final ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public void onReceive(Request request) throws Throwable {
    if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.UPSERT_ORGANISATION_TO_ES.getValue())) {
      upsertOrganisationDataToES(request);
    } else {
      onReceiveUnsupportedOperation();
    }
  }

  private void upsertOrganisationDataToES(Request request) {
    Map<String, Object> organisation =
        (Map<String, Object>) request.getRequest().get(JsonKey.ORGANISATION);
    // making call to register tag
    if (((String) request.getRequest().get(JsonKey.OPERATION_TYPE)).equals(JsonKey.INSERT)) {
      Map<String, String> headerMap = new HashMap<>();
      String header = ProjectUtil.getConfigValue(JsonKey.EKSTEP_AUTHORIZATION);
      header = JsonKey.BEARER + header;
      headerMap.put(JsonKey.AUTHORIZATION, header);
      headerMap.put("Content-Type", "application/json");
      registerTag(
          (String) organisation.get(JsonKey.ID), "{}", headerMap, request.getRequestContext());
    }
    organisation.remove(JsonKey.CONTACT_DETAILS);
    String orgLocation = (String) organisation.get(JsonKey.ORG_LOCATION);
    List<Map<String, Object>> orgLocationList = new ArrayList<>();
    if (StringUtils.isNotBlank(orgLocation)) {
      try {
        orgLocationList = mapper.readValue(orgLocation, List.class);
      } catch (Exception e) {
        logger.info(
            request.getRequestContext(),
            "Exception occurred while converting orgLocation to List<Map<String,String>>.");
      }
    }
    organisation.put(JsonKey.ORG_LOCATION, orgLocationList);

    esService.upsert(
        ProjectUtil.EsType.organisation.getTypeName(),
        (String) organisation.get(JsonKey.ID),
        organisation,
        null);
  }

  private String registerTag(
      String tagId, String body, Map<String, String> header, RequestContext context) {
    String tagStatus = "";
    try {
      logger.info(
          context, "OrganisationBackgroundActor:registertag ,call started with tagid = " + tagId);
      String analyticsBaseUrl = ProjectUtil.getConfigValue(JsonKey.ANALYTICS_API_BASE_URL);
      ProjectUtil.setTraceIdInHeader(header, context);
      tagStatus =
          HttpClientUtil.post(
              analyticsBaseUrl
                  + PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_TAG_API_URL)
                  + "/"
                  + tagId,
              body,
              header,
              context);
      logger.info(
          context,
          "OrganisationBackgroundActor:registertag  ,call end with id and status = "
              + tagId
              + ", "
              + tagStatus);
    } catch (Exception e) {
      logger.error(
          context,
          "OrganisationBackgroundActor:registertag ,call failure with error message = "
              + e.getMessage(),
          e);
    }
    return tagStatus;
  }
}
