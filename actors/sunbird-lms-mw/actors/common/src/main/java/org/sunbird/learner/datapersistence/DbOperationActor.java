package org.sunbird.learner.datapersistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.telemetry.util.TelemetryWriter;
import scala.concurrent.Future;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ActorConfig(
  tasks = {
    "createData",
    "updateData",
    "deleteData",
    "readData",
    "readAllData",
    "searchData",
    "getMetrics"
  },
  asyncTasks = {}
)
public class DbOperationActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request actorMessage) throws Throwable {
    Util.initializeContext(actorMessage, TelemetryEnvKey.OBJECT_STORE);
    if (actorMessage.getOperation().equalsIgnoreCase(ActorOperations.CREATE_DATA.getValue())) {
      create(actorMessage);
    } else if (actorMessage
        .getOperation()
        .equalsIgnoreCase(ActorOperations.UPDATE_DATA.getValue())) {
      update(actorMessage);
    } else if (actorMessage
        .getOperation()
        .equalsIgnoreCase(ActorOperations.DELETE_DATA.getValue())) {
      delete(actorMessage);
    } else if (actorMessage.getOperation().equalsIgnoreCase(ActorOperations.READ_DATA.getValue())) {
      read(actorMessage);
    } else if (actorMessage
        .getOperation()
        .equalsIgnoreCase(ActorOperations.READ_ALL_DATA.getValue())) {
      readAllData(actorMessage);
    } else if (actorMessage
        .getOperation()
        .equalsIgnoreCase(ActorOperations.SEARCH_DATA.getValue())) {
      search(actorMessage);
    } else if (actorMessage
        .getOperation()
        .equalsIgnoreCase(ActorOperations.GET_METRICS.getValue())) {
      getMetrics(actorMessage);
    } else {
      onReceiveUnsupportedOperation(actorMessage.getOperation());
    }
    onReceiveUnsupportedOperation(actorMessage.getOperation());
  }

  private void getMetrics(Request actorMessage) {
    try {
      String ES_INDEX_NAME = "sunbirdplugin";
      String RAW_QUERY = "rawQuery";
      validateTableName(actorMessage);
      Map<String, Object> rawQueryMap =
          (Map<String, Object>) actorMessage.getRequest().get(RAW_QUERY);
      rawQueryMap.put(JsonKey.SIZE, 0);
      ObjectMapper mapper = new ObjectMapper();
      String rawQuery = mapper.writeValueAsString(rawQueryMap);
      Response response = esService.searchMetricsData(ES_INDEX_NAME, rawQuery);
      sender().tell(response, self());
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
      sender().tell(ex, self());
    }
  }

  private void search(Request reqObj) {
    SearchDTO searchDto = null;
    try {
      String ENTITY_NAME = "entityName";
      String ES_INDEX_NAME = "sunbirdplugin";
      Response response = new Response();
      String REQUIRED_FIELDS = "requiredFields";
      List<String> requiredFields = null;
      if (!StringUtils.isBlank((String) reqObj.getRequest().get(ENTITY_NAME))) {
        String esType = (String) reqObj.getRequest().get(ENTITY_NAME);
        if (reqObj.getRequest().containsKey(REQUIRED_FIELDS)) {
          requiredFields = (List<String>) reqObj.getRequest().get(REQUIRED_FIELDS);
          reqObj.getRequest().remove(REQUIRED_FIELDS);
        }

        reqObj.getRequest().remove(ENTITY_NAME);
        if (null != reqObj.getRequest().get(JsonKey.FILTERS)) {
          validateRequestData((Map<String, Object>) reqObj.getRequest().get(JsonKey.FILTERS));
        }
        searchDto = Util.createSearchDto(reqObj.getRequest());
        Future<Map<String, Object>> resultF = esService.search(searchDto, ES_INDEX_NAME);
        Map<String, Object> result =
            (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
        Map<String, Object> finalResult = new HashMap<>();
        if (!result.isEmpty()) {
          // filter the required fields like content or facet etc...
          if (null != requiredFields && !requiredFields.isEmpty()) {
            for (String attribute : requiredFields) {
              finalResult.put(attribute, result.get(attribute));
            }
            result = finalResult;
          }
          if (result.containsKey(JsonKey.CONTENT)) {
            List<Map<String, Object>> mapList =
                (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
            for (Map<String, Object> map : mapList) {
              map.remove(JsonKey.IDENTIFIER);
            }
          }
          response.put(JsonKey.RESPONSE, result);
        } else {
          response.put(JsonKey.RESPONSE, new HashMap<>());
        }
      } else {
        throw new ProjectCommonException(
            ResponseCode.tableOrDocNameError.getErrorCode(),
            ResponseCode.tableOrDocNameError.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }

      sender().tell(response, self());
      // create search telemetry event here ...
      generateSearchTelemetryEvent(
          searchDto,
          new String[] {ES_INDEX_NAME},
          (Map<String, Object>) response.get(JsonKey.RESPONSE),reqObj.getContext());
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
      sender().tell(ex, self());
    }
  }

  private void readAllData(Request reqObj) {
    try {
      Response response = null;
      validateTableName(reqObj);
      String ENTITY_NAME = "entityName";
      if (!StringUtils.isBlank((String) reqObj.getRequest().get(ENTITY_NAME))) {
        response =
            cassandraOperation.getAllRecords(
                JsonKey.SUNBIRD_PLUGIN, (String) reqObj.getRequest().get(ENTITY_NAME));
      } else {
        throw new ProjectCommonException(
            ResponseCode.tableOrDocNameError.getErrorCode(),
            ResponseCode.tableOrDocNameError.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      sender().tell(response, self());
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
      sender().tell(ex, self());
    }
  }

  private void read(Request reqObj) {
    try {
      String ENTITY_NAME = "entityName";
      Response response = null;
      validateTableName(reqObj);
      if (!StringUtils.isBlank((String) reqObj.getRequest().get(ENTITY_NAME))) {
        response =
            cassandraOperation.getRecordById(
                JsonKey.SUNBIRD_PLUGIN,
                (String) reqObj.getRequest().get(ENTITY_NAME),
                (String) reqObj.getRequest().get(JsonKey.ID));
      } else {
        throw new ProjectCommonException(
            ResponseCode.tableOrDocNameError.getErrorCode(),
            ResponseCode.tableOrDocNameError.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      sender().tell(response, self());
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
      sender().tell(ex, self());
    }
  }

  private void delete(Request reqObj) {
    try {
      validateTableName(reqObj);
      String ENTITY_NAME = "entityName";
      String ES_INDEX_NAME = "sunbirdplugin";
      String INDEXED = "indexed";
      Response response =
          cassandraOperation.deleteRecord(
              JsonKey.SUNBIRD_PLUGIN,
              (String) reqObj.getRequest().get(ENTITY_NAME),
              (String) reqObj.getRequest().get(JsonKey.ID));
      if (((String) response.get(JsonKey.RESPONSE)).equals(JsonKey.SUCCESS)
          && ((boolean) reqObj.getRequest().get(INDEXED))) {
        deleteDataFromElastic(
            ES_INDEX_NAME,
            (String) reqObj.getRequest().get(ENTITY_NAME),
            (String) reqObj.getRequest().get(JsonKey.ID));
      }
      sender().tell(response, self());
      generateTelemetryObjectStore(reqObj);
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
      sender().tell(ex, self());
    }
  }

  private void update(Request reqObj) {
    try {
      String PAYLOAD = "payload";
      String INDEXED = "indexed";
      String ES_INDEX_NAME = "sunbirdplugin";
      validateTableName(reqObj);
      Map<String, Object> payload = (Map<String, Object>) reqObj.getRequest().get(PAYLOAD);
      validateRequestData(payload);
      Response response = null;
      boolean esResult = false;
      String ENTITY_NAME = "entityName";
      if (!StringUtils.isBlank((String) reqObj.getRequest().get(ENTITY_NAME))
          && ((boolean) reqObj.getRequest().get(INDEXED))) {
        esResult =
            updateDataToElastic(
                ES_INDEX_NAME,
                (String) reqObj.getRequest().get(ENTITY_NAME),
                (String) payload.get(JsonKey.ID),
                payload);
        if (esResult) {
          response =
              cassandraOperation.updateRecord(
                  JsonKey.SUNBIRD_PLUGIN, (String) reqObj.getRequest().get(ENTITY_NAME), payload);
          if (!((String) response.get(JsonKey.RESPONSE)).equals(JsonKey.SUCCESS)) {
            deleteDataFromElastic(
                ES_INDEX_NAME,
                (String) reqObj.getRequest().get(ENTITY_NAME),
                (String) payload.get(JsonKey.ID));
            throw new ProjectCommonException(
                ResponseCode.esUpdateFailed.getErrorCode(),
                ResponseCode.esUpdateFailed.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode());
          }
        } else {
          throw new ProjectCommonException(
              ResponseCode.updateFailed.getErrorCode(),
              ResponseCode.updateFailed.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
      } else {
        Future<Map<String, Object>> dataF =
            esService.getDataByIdentifier(ES_INDEX_NAME, (String) payload.get(JsonKey.ID));
        Map<String, Object> data =
            (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(dataF);
        if (data.isEmpty() || ((boolean) reqObj.getRequest().get(INDEXED))) {
          response =
              cassandraOperation.updateRecord(
                  JsonKey.SUNBIRD_PLUGIN, (String) reqObj.getRequest().get(ENTITY_NAME), payload);
        } else {
          throw new ProjectCommonException(
              ResponseCode.updateFailed.getErrorCode(),
              ResponseCode.updateFailed.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
      }
      sender().tell(response, self());
      generateTelemetryObjectStore(reqObj);
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
      sender().tell(ex, self());
    }
  }

  private void create(Request reqObj) {
    try {
      String PAYLOAD = "payload";
      String ENTITY_NAME = "entityName";
      String INDEXED = "indexed";
      String ES_INDEX_NAME = "sunbirdplugin";
      validateTableName(reqObj);
      Map<String, Object> payload = (Map<String, Object>) reqObj.getRequest().get(PAYLOAD);
      validateRequestData(payload);
      Response response =
          cassandraOperation.insertRecord(
              JsonKey.SUNBIRD_PLUGIN, (String) reqObj.getRequest().get(ENTITY_NAME), payload);
      if (((String) response.get(JsonKey.RESPONSE)).equals(JsonKey.SUCCESS)
          && ((boolean) reqObj.getRequest().get(INDEXED))) {
        boolean esResult = false;
        if (!StringUtils.isBlank((String) reqObj.getRequest().get(ENTITY_NAME))) {
          esResult =
              insertDataToElastic(
                  ES_INDEX_NAME,
                  (String) reqObj.getRequest().get(ENTITY_NAME),
                  (String) payload.get(JsonKey.ID),
                  payload);
          if (!esResult) {
            deleteRecord(
                JsonKey.SUNBIRD_PLUGIN,
                (String) reqObj.getRequest().get(ENTITY_NAME),
                (String) payload.get(JsonKey.ID));
            throw new ProjectCommonException(
                ResponseCode.esUpdateFailed.getErrorCode(),
                ResponseCode.esUpdateFailed.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode());
          }
          response.put(JsonKey.DATA, payload);
        }
      }
      sender().tell(response, self());
      generateTelemetryObjectStore(reqObj);
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
      sender().tell(ex, self());
    }
  }

  private void validateRequestData(Map<String, Object> payload) {
    for (Map.Entry<String, Object> entry : payload.entrySet()) {
      if (entry.getValue() instanceof BigInteger) {
        payload.put(entry.getKey(), Integer.parseInt(String.valueOf(entry.getValue())));
      }
    }
  }

  private void validateTableName(Request reqObj) {
    String ENTITY_NAME = "entityName";
    if (!DataCacheHandler.getSunbirdPluginTableList().contains(reqObj.getRequest().get(ENTITY_NAME))) {
      throw new ProjectCommonException(
          ResponseCode.tableOrDocNameError.getErrorCode(),
          ResponseCode.tableOrDocNameError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  /**
   * Method to insert data to ES .
   *
   * @param index
   * @param type
   * @param identifier
   * @param data
   * @return
   */
  private boolean insertDataToElastic(
      String index, String type, String identifier, Map<String, Object> data) {
    ProjectLogger.log(
        "making call to ES for index ,identifier ,data==" + type + " " + identifier + data);
    Future<String> responseF = esService.save(index, identifier, data);
    String response = (String) ElasticSearchHelper.getResponseFromFuture(responseF);
    ProjectLogger.log(
        "Getting ES save response for type , identifier=="
            + type
            + "  "
            + identifier
            + "  "
            + response);
    if (!StringUtils.isBlank(response)) {
      ProjectLogger.log("Data is saved successfully ES ." + type + "  " + identifier);
      return true;
    }
    ProjectLogger.log(
        "unbale to save the data inside ES with identifier " + identifier, LoggerEnum.INFO.name());
    return false;
  }

  /**
   * Method to update data to ES .
   *
   * @param indexName
   * @param typeName
   * @param identifier
   * @param data
   * @return
   */
  private boolean updateDataToElastic(
      String indexName, String typeName, String identifier, Map<String, Object> data) {
    Future<Boolean> responseF = esService.update(indexName, identifier, data);
    boolean response = (boolean) ElasticSearchHelper.getResponseFromFuture(responseF);
    if (response) {
      return true;
    }
    ProjectLogger.log(
        "unable to save the data inside ES with identifier " + identifier, LoggerEnum.INFO.name());
    return false;
  }

  /**
   * Method to update data to ES .
   *
   * @param indexName
   * @param typeName
   * @param identifier
   * @return
   */
  private boolean deleteDataFromElastic(String indexName, String typeName, String identifier) {
    Future<Boolean> responseF = esService.delete(indexName, identifier);
    boolean response = (boolean) ElasticSearchHelper.getResponseFromFuture(responseF);
    if (response) {
      return true;
    }
    ProjectLogger.log(
        "unable to delete the data from ES with identifier " + identifier, LoggerEnum.INFO.name());
    return false;
  }

  private void deleteRecord(String keyspaceName, String tableName, String identifier) {
    cassandraOperation.deleteRecord(keyspaceName, tableName, identifier);
  }

  private void generateSearchTelemetryEvent(
      SearchDTO searchDto, String[] types, Map<String, Object> result, Map<String,Object> context) {
      Map<String, Object> params = new HashMap<>();
    params.put(JsonKey.QUERY, searchDto.getQuery());
    params.put(JsonKey.FILTERS, searchDto.getAdditionalProperties().get(JsonKey.FILTERS));
    params.put(JsonKey.SORT, searchDto.getSortBy());
    params.put(JsonKey.TOPN, generateTopNResult(result));
    params.put(JsonKey.SIZE, result.get(JsonKey.COUNT));
    params.put(JsonKey.TYPE, String.join(",", types));

    Request request = new Request();
    request.setRequest(telemetryRequestForSearch(context, params));
    TelemetryWriter.write(request);
  }

  private List<Map<String, Object>> generateTopNResult(Map<String, Object> result) {
    List<Map<String, Object>> dataMapList = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
    Integer topN = Integer.parseInt(PropertiesCache.getInstance().getProperty(JsonKey.SEARCH_TOP_N));
    int count = Math.min(topN, dataMapList.size());
    List<Map<String, Object>> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Map<String, Object> m = new HashMap<>();
      m.put(JsonKey.ID, dataMapList.get(i).get(JsonKey.ID));
      list.add(m);
    }
    return list;
  }

  private static Map<String, Object> telemetryRequestForSearch(
      Map<String, Object> telemetryContext, Map<String, Object> params) {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CONTEXT, telemetryContext);
    map.put(JsonKey.PARAMS, params);
    map.put(JsonKey.TELEMETRY_EVENT_TYPE, "SEARCH");
    return map;
  }

  private static void generateTelemetryObjectStore(Request reqObj) {
    String PAYLOAD = "payload";
    String ENTITY_NAME = "entityName";
    Map<String, Object> targetObject =
        TelemetryUtil.generateTargetObject(
            (String) ((Map<String, Object>) reqObj.getRequest().get(PAYLOAD)).get(JsonKey.ID),
            (String) reqObj.getRequest().get(ENTITY_NAME),
            JsonKey.CREATE,
            null);
    TelemetryUtil.telemetryProcessingCall(
        (Map<String, Object>) reqObj.getRequest().get(PAYLOAD), targetObject, new ArrayList<>(),reqObj.getContext());
  }
}
