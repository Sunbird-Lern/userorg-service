package org.sunbird.metrics.actors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.models.util.ProjectUtil.ReportTrackingStatus;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.metrics.actors.OrganisationMetricsUtil.ContentStatus;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {},
  asyncTasks = {"orgCreationMetricsData", "orgConsumptionMetricsData"}
)
public class OrganisationMetricsBackgroundActor extends BaseMetricsActor {

  private static ObjectMapper mapper = new ObjectMapper();
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private Util.DbInfo reportTrackingdbInfo = Util.dbInfoMap.get(JsonKey.REPORT_TRACKING_DB);
  private static Map<String, String> conceptsList = new HashMap<>();
  private DecryptionService decryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
          null);
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.ORG_CREATION_METRICS_DATA.getValue())) {
      orgCreationMetricsData(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.ORG_CONSUMPTION_METRICS_DATA.getValue())) {
      orgConsumptionMetricsData(request);
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  private void orgCreationMetricsData(Request actorMessage) {
    ProjectLogger.log(
        "OrganisationMetricsBackgroundActor: orgCreationMetricsData called.",
        LoggerEnum.INFO.name());
    try {
      String requestId = (String) actorMessage.getRequest().get(JsonKey.REQUEST_ID);
      Map<String, Object> requestData = getData(requestId);
      String orgId = (String) requestData.get(JsonKey.RESOURCE_ID);
      List<Object> headers = new ArrayList<>();
      headers.add("contentCreatedFor");
      headers.add("userId");
      headers.add("userName");
      headers.add("userCreatedOn");
      headers.add("contentName");
      headers.add("contentType");
      headers.add("contentLanguage");
      headers.add("contentSize");
      headers.add("contentCreatedOn");
      headers.add("contentReviewedOn");
      headers.add("contentLastPublishedOn");
      headers.add("contentLastUpdatedOn");
      headers.add("contentLastUpdatedStatus");
      headers.add("contentLastPublishedBy");
      headers.add("contentConceptsCovered");
      headers.add("contentDomain");
      headers.add("contentTagsCount");
      headers.add("contentCreationTimeSpent");
      headers.add("contentCreationTotalSessions");
      headers.add("contentCreationAvgTimePerSession");
      List<List<Object>> csvRecords = new ArrayList<>();
      csvRecords.add(headers);
      for (String operation : OrganisationMetricsUtil.operationList) {
        String requestStr = getRequestObject(operation, requestId);

        String baseSearchUrl = ProjectUtil.getConfigValue(JsonKey.SEARCH_SERVICE_API_BASE_URL);
        String ekStepResponse =
            makePostRequest(baseSearchUrl, JsonKey.EKSTEP_CONTENT_SEARCH_URL, requestStr);
        List<Map<String, Object>> ekstepData = getDataFromResponse(ekStepResponse, headers, orgId);
        List<Map<String, Object>> userData = getUserDetailsFromES(ekstepData);
        csvRecords.addAll(generateDataList(userData, headers));
      }
      String period = (String) requestData.get(JsonKey.PERIOD);
      String fileName =
          "CreationReport"
              + FILENAMESEPARATOR
              + orgId
              + FILENAMESEPARATOR
              + System.currentTimeMillis()
              + FILENAMESEPARATOR
              + period;

      saveData(csvRecords, requestId, "Creation Report");
      Request backGroundRequest = new Request();
      backGroundRequest.setOperation(ActorOperations.FILE_GENERATION_AND_UPLOAD.getValue());

      Map<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.REQUEST_ID, requestId);
      innerMap.put(JsonKey.FILE_NAME, fileName);
      innerMap.put(JsonKey.DATA, csvRecords);
      backGroundRequest.setRequest(innerMap);
      tellToAnother(backGroundRequest);
    } catch (Exception e) {
      ProjectLogger.log("Some error occurs", e);
      throw new ProjectCommonException(
          ResponseCode.internalError.getErrorCode(),
          ResponseCode.internalError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  @SuppressWarnings("unchecked")
  private void orgConsumptionMetricsData(Request actorMessage) {
    ProjectLogger.log(
        "OrganisationMetricsBackgroundActor: orgConsumptionMetricsData called.",
        LoggerEnum.INFO.name());
    try {
      String requestId = (String) actorMessage.getRequest().get(JsonKey.REQUEST_ID);
      Map<String, Object> requestData = getData(requestId);
      String periodStr = (String) requestData.get(JsonKey.PERIOD);
      String orgId = (String) requestData.get(JsonKey.RESOURCE_ID);
      Map<String, Object> orgData = OrganisationMetricsUtil.validateOrg(orgId);
      if (null == orgData) {
        throw new ProjectCommonException(
            ResponseCode.invalidOrgData.getErrorCode(),
            ResponseCode.invalidOrgData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      String orgHashId = (String) orgData.get(JsonKey.HASHTAGID);
      String channel =
          (String) (orgData.get(JsonKey.CHANNEL) == null ? "" : orgData.get(JsonKey.CHANNEL));

      List<Object> headers = new ArrayList<>();
      headers.add("userId");
      headers.add("userName");
      headers.add("userCreatedOn");
      headers.add("totalNumberOfVisitsByUser");
      headers.add("totalTimeSpentOnConsumingContent");
      headers.add("totalPiecesOfContentConsumed");
      headers.add("avgTimeSpentPerVisit");
      List<Map<String, Object>> consumptionData = new ArrayList<>();
      List<Map<String, Object>> usersData = getUserDetailsUsingOrg(orgId);
      for (Map<String, Object> userData : usersData) {
        String request =
            OrganisationMetricsUtil.getOrgMetricsRequest(
                actorMessage, periodStr, orgHashId, (String) userData.get(JsonKey.ID), channel);
        String analyticsBaseUrl = ProjectUtil.getConfigValue(JsonKey.ANALYTICS_API_BASE_URL);
        String esResponse =
            makePostRequest(analyticsBaseUrl, JsonKey.EKSTEP_METRICS_API_URL, request);
        Map<String, Object> ekstepData =
            getConsumptionDataFromResponse(esResponse, userData, (List<String>) (Object) headers);
        consumptionData.add(ekstepData);
      }
      List<List<Object>> csvRecords = new ArrayList<>();
      csvRecords.add(headers);
      csvRecords.addAll(generateDataList(consumptionData, headers));

      String fileName =
          "ConsumptionReport"
              + FILENAMESEPARATOR
              + orgId
              + FILENAMESEPARATOR
              + System.currentTimeMillis()
              + FILENAMESEPARATOR
              + periodStr;
      saveData(csvRecords, requestId, "Consumption Report");
      Request backGroundRequest = new Request();
      backGroundRequest.setOperation(ActorOperations.FILE_GENERATION_AND_UPLOAD.getValue());

      Map<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.REQUEST_ID, requestId);
      innerMap.put(JsonKey.FILE_NAME, fileName);
      innerMap.put(JsonKey.DATA, csvRecords);
      backGroundRequest.setRequest(innerMap);
      tellToAnother(backGroundRequest);
    } catch (Exception e) {
      ProjectLogger.log("Some error occurs", e);
      throw new ProjectCommonException(
          ResponseCode.internalError.getErrorCode(),
          ResponseCode.internalError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  private List<List<Object>> generateDataList(
      List<Map<String, Object>> aggregationMap, List<Object> headers) {
    List<List<Object>> result = new ArrayList<>();
    for (Map<String, Object> data : aggregationMap) {
      List<Object> dataResult = new ArrayList<>();
      for (Object header : headers) {
        dataResult.add(data.get(header));
      }
      result.add(dataResult);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getUserDetailsFromES(List<Map<String, Object>> ekstepData) {
    List<String> coursefields = new ArrayList<>();
    List<Map<String, Object>> userResult = new ArrayList<>();
    coursefields.add(JsonKey.USER_ID);
    coursefields.add(JsonKey.USERNAME);
    coursefields.add(JsonKey.CREATED_DATE);
    String userId = "";
    for (Map<String, Object> userData : ekstepData) {
      Map<String, Object> data = new HashMap<>();
      if (userData.containsKey("userId")) {
        userId = (String) userData.get("userId");
      } else {
        data.putAll(userData);
        userResult.add(data);
        continue;
      }
      Map<String, Object> filter = new HashMap<>();
      filter.put(JsonKey.IDENTIFIER, userId);
      try {
        Future<Map<String, Object>> resultF =
            esService.search(
                createESRequest(filter, null, coursefields), EsType.user.getTypeName());
        Map<String, Object> result =
            (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
        if (null != result && !result.isEmpty()) {
          List<Map<String, Object>> resultList =
              (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
          if (null != resultList && !resultList.isEmpty()) {
            for (Map<String, Object> dataObject : resultList) {
              data.putAll(dataObject);
            }
          }
          data.putAll(userData);
          userResult.add(data);
        }
      } catch (Exception e) {
        throw new ProjectCommonException(
            ResponseCode.esError.getErrorCode(),
            ResponseCode.esError.getErrorMessage(),
            ResponseCode.SERVER_ERROR.getResponseCode());
      }
    }
    // decrypt the userdata and return
    return decryptionService.decryptData(userResult);
  }

  private String getRequestObject(String operation, String requestId) {
    Request request = new Request();
    Map<String, Object> requestedData = getData(requestId);
    String orgId = (String) requestedData.get(JsonKey.RESOURCE_ID);
    String periodStr = (String) requestedData.get(JsonKey.PERIOD);
    Map<String, Object> dateMap = getStartAndEndDate(periodStr);
    Map<String, String> operationMap = new LinkedHashMap<>();
    switch (operation) {
      case "Create":
        operationMap.put("dateKey", "createdOn");
        operationMap.put("status", ContentStatus.Draft.name());
        break;

      case "Review":
        operationMap.put("dateKey", "lastSubmittedOn");
        operationMap.put("status", ContentStatus.Review.name());
        break;

      case "Publish":
        operationMap.put("dateKey", "lastPublishedOn");
        operationMap.put("status", ContentStatus.Live.name());
        break;

      default:
        operationMap.put("dateKey", "");
        operationMap.put("status", "");
        break;
    }
    Map<String, Object> requestObject = new HashMap<>();
    Map<String, Object> filterMap = new HashMap<>();
    List<String> fields = new ArrayList<>();
    Map<String, Object> dateValue = new HashMap<>();
    dateValue.put("min", dateMap.get(STARTDATE));
    dateValue.put("max", dateMap.get(ENDDATE));
    filterMap.put(operationMap.get("dateKey"), dateValue);
    List<String> contentType = new ArrayList<>();
    contentType.add("Story");
    contentType.add("Worksheet");
    contentType.add("Game");
    contentType.add("Collection");
    contentType.add("TextBook");
    contentType.add("TextBookUnit");
    contentType.add("Course");
    contentType.add("CourseUnit");
    filterMap.put("contentType", contentType);
    filterMap.put("createdFor", orgId);
    filterMap.put(JsonKey.STATUS, operationMap.get(JsonKey.STATUS));
    requestObject.put(JsonKey.FILTERS, filterMap);
    fields.add("createdBy");
    fields.add("createdFor");
    fields.add("createdOn");
    fields.add("lastUpdatedOn");
    fields.add("status");
    fields.add("lastPublishedBy");
    fields.add("name");
    fields.add("contentType");
    fields.add("size");
    fields.add("concepts");
    fields.add("domain");
    fields.add("tags");
    fields.add("language");
    fields.add("lastPublishedOn");
    fields.add("lastSubmittedOn");
    fields.add("me_creationTimespent");
    fields.add("me_creationSessions");
    fields.add("me_avgCreationTsPerSession");
    requestObject.put(JsonKey.FIELDS, fields);
    request.setRequest(requestObject);
    String requestStr = "";
    try {
      requestStr = mapper.writeValueAsString(request);
    } catch (JsonProcessingException e) {
      ProjectLogger.log("Error occurred", e);
    }
    return requestStr;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getDataFromResponse(
      String responseData, List<Object> headers, String orgId) {
    List<Map<String, Object>> result = new ArrayList<>();
    try {
      Map<String, Object> resultData = mapper.readValue(responseData, Map.class);
      resultData = (Map<String, Object>) resultData.get(JsonKey.RESULT);
      List<Map<String, Object>> resultList =
          (List<Map<String, Object>>) resultData.get(JsonKey.CONTENT);
      for (Map<String, Object> content : resultList) {
        Map<String, Object> data = new HashMap<>();
        for (String header : content.keySet()) {
          String headerName = StringUtils.capitalize(header);
          headerName = "content" + headerName;
          if (headers.contains(headerName)) {
            data.put(headerName, content.get(header));
          } else if ("status".equalsIgnoreCase(header)) {
            data.put("contentLastUpdatedStatus", content.get(header));
          } else if ("contentType".equalsIgnoreCase(header)) {
            data.put(header, content.get(header));
          } else if ("concepts".equalsIgnoreCase(header)) {
            List<String> concepts = getConcepts((List<String>) content.get(header));
            data.put("contentConceptsCovered", concepts);
          } else if ("tags".equalsIgnoreCase(header)) {
            List<String> tags = (List<String>) content.get(header);
            data.put("contentTagsCount", tags.size());
          } else if ("lastSubmittedOn".equals(header)) {
            data.put("contentReviewedOn", content.get(header));
          }
          if ("createdFor".equalsIgnoreCase(header)) {
            data.put(headerName, orgId);
          }
          if ("createdBy".equalsIgnoreCase(header) || "lastPublishedBy".equalsIgnoreCase(header)) {
            data.put("userId", content.get(header));
          }
        }
        result.add(data);
      }
    } catch (Exception e) {
      ProjectLogger.log("Error occurred", e);
    }
    return result;
  }

  private static List<String> getConcepts(List<String> data) {
    List<String> result = new ArrayList<>();
    for (String concept : data) {
      String conceptName = getConcept(concept);
      if (!StringUtils.isBlank(conceptName)) {
        result.add(conceptName);
      }
    }
    return result;
  }

  private static String getConcept(String data) {
    if (null == conceptsList || conceptsList.isEmpty()) {
      conceptList();
    }
    return conceptsList.get(data);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> conceptList() {
    List<String> domains = getDomains();
    for (String domain : domains) {
      String url = PropertiesCache.getInstance().getProperty((JsonKey.EKSTEP_CONCEPT_URL));
      url = StringUtils.replace(url, "{domain}", domain);
      String resposne = getDataFromEkstep(url);
      try {
        Map<String, Object> responseMap = mapper.readValue(resposne, Map.class);
        responseMap = (Map<String, Object>) responseMap.get(JsonKey.RESULT);
        List<Map<String, Object>> conceptData =
            (List<Map<String, Object>>) responseMap.get("concepts");
        for (Map<String, Object> concept : conceptData) {
          String id = (String) concept.get(JsonKey.IDENTIFIER);
          String name = (String) concept.get(JsonKey.NAME);
          conceptsList.put(id, name);
        }
      } catch (IOException e) {
        ProjectLogger.log("Error occurred", e);
      }
    }
    return conceptsList;
  }

  @SuppressWarnings("unchecked")
  private static List<String> getDomains() {
    String domainUrl = PropertiesCache.getInstance().getProperty((JsonKey.EKSTEP_DOMAIN_URL));
    String resposne = getDataFromEkstep(domainUrl);
    List<String> domainList = new ArrayList<>();
    try {
      Map<String, Object> responseMap = mapper.readValue(resposne, Map.class);
      responseMap = (Map<String, Object>) responseMap.get(JsonKey.RESULT);
      List<Map<String, Object>> domainData = (List<Map<String, Object>>) responseMap.get("domains");
      for (Map<String, Object> domain : domainData) {
        String id = (String) domain.get(JsonKey.IDENTIFIER);
        domainList.add(id);
      }
    } catch (IOException e) {
      ProjectLogger.log("Error occurred", e);
    }
    return domainList;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getUserDetailsUsingOrg(String orgId) {
    List<String> coursefields = new ArrayList<>();
    List<Map<String, Object>> userResult = new ArrayList<>();
    coursefields.add(JsonKey.USER_ID);
    coursefields.add(JsonKey.USER_NAME);
    coursefields.add(JsonKey.CREATED_DATE);
    Map<String, Object> filter = new HashMap<>();
    filter.put("organisations.organisationId", orgId);
    try {
      Future<Map<String, Object>> resultF =
          esService.search(createESRequest(filter, null, coursefields), EsType.user.getTypeName());
      Map<String, Object> result =
          (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);

      if (null != result && !result.isEmpty()) {
        List<Map<String, Object>> resultList =
            (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
        if (null != resultList && !resultList.isEmpty()) {
          for (Map<String, Object> dataObject : resultList) {
            userResult.add(dataObject);
          }
        }
      }
      // decrypt the userdata
      userResult = decryptionService.decryptData(userResult);
      return userResult;
    } catch (Exception e) {
      throw new ProjectCommonException(
          ResponseCode.esError.getErrorCode(),
          ResponseCode.esError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getConsumptionDataFromResponse(
      String responseData, Map<String, Object> userData, List<String> headers) {
    Map<String, Object> resultMap = new HashMap<>();
    try {
      Map<String, Object> resultData = mapper.readValue(responseData, Map.class);
      resultData = (Map<String, Object>) resultData.get(JsonKey.RESULT);
      Map<String, Object> result = (Map<String, Object>) resultData.get(JsonKey.SUMMARY);
      resultMap.put(headers.get(0), userData.get(JsonKey.ID));
      resultMap.put(headers.get(1), userData.get(JsonKey.USERNAME));
      resultMap.put(headers.get(2), userData.get(JsonKey.CREATED_DATE));
      resultMap.put(headers.get(3), result.get("m_total_sessions"));
      resultMap.put(headers.get(4), result.get("m_total_ts"));
      resultMap.put(headers.get(5), result.get("m_total_content_count"));
      resultMap.put(headers.get(6), result.get("m_avg_ts_session"));
    } catch (Exception e) {
      ProjectLogger.log("Error occurred", e);
    }
    return resultMap;
  }

  private void saveData(List<List<Object>> data, String requestId, String type) {
    Map<String, Object> dbReqMap = new HashMap<>();
    SimpleDateFormat format = ProjectUtil.getDateFormatter();
    format.setLenient(false);
    dbReqMap.put(JsonKey.ID, requestId);
    String dataStr = getJsonString(data);
    dbReqMap.put(JsonKey.DATA, dataStr);
    dbReqMap.put(JsonKey.STATUS, ReportTrackingStatus.GENERATING_DATA.getValue());
    dbReqMap.put(JsonKey.UPDATED_DATE, format.format(new Date()));
    dbReqMap.put(JsonKey.TYPE, type);
    cassandraOperation.updateRecord(
        reportTrackingdbInfo.getKeySpace(), reportTrackingdbInfo.getTableName(), dbReqMap);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getData(String requestId) {
    Response response =
        cassandraOperation.getRecordById(
            reportTrackingdbInfo.getKeySpace(), reportTrackingdbInfo.getTableName(), requestId);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (responseList.isEmpty()) {
      return new HashMap<>();
    }
    return responseList.get(0);
  }

  private String getJsonString(Object requestObject) {
    ObjectMapper mapper = new ObjectMapper();
    String data = "";
    try {
      data = mapper.writeValueAsString(requestObject);
    } catch (JsonProcessingException e) {
      throw new ProjectCommonException(
          ResponseCode.invalidJsonData.getErrorCode(),
          ResponseCode.invalidJsonData.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return data;
  }
}
