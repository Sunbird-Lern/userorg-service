package controllers.datapersistence;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchUtil;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import play.libs.F.Promise;
import play.mvc.Result;

public class DbOperationController extends BaseController {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static CassandraConnectionManager manager = CassandraConnectionMngrFactory
      .getObject(PropertiesCache.getInstance().getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE));
  private static final String PAYLOAD = "payload";
  private static final String TABLE_NAME = "tableName";
  private static final String DOCUMENT_NAME = "documentName";
  private static final String ES_INDEX_NAME = "sunbirdplugin";
  private static List<String> tableList = null;

  static {
    tableList = manager.getTableList(JsonKey.SUNBIRD_PLUGIN);
  }

  /**
   * This method will allow create Data in DB.
   * 
   * @return Promise<Result>
   */
  public Promise<Result> create() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("getting create data request = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      validateTableName(reqObj);
      Map<String, Object> payload = (Map<String, Object>) reqObj.getRequest().get(PAYLOAD);
      validateRequestData(payload);
      Response response = cassandraOperation.insertRecord(JsonKey.SUNBIRD_PLUGIN,
          (String) reqObj.getRequest().get(TABLE_NAME), payload);
      if (((String) response.get(JsonKey.RESPONSE)).equals(JsonKey.SUCCESS)) {
        boolean esResult = false;
        if (!ProjectUtil.isStringNullOREmpty((String) reqObj.getRequest().get(DOCUMENT_NAME))) {
          esResult =
              insertDataToElastic(ES_INDEX_NAME, (String) reqObj.getRequest().get(DOCUMENT_NAME),
                  (String) payload.get(JsonKey.ID), payload);
          if (!esResult) {
            deleteRecord(JsonKey.SUNBIRD_PLUGIN, (String) reqObj.getRequest().get(TABLE_NAME),
                (String) payload.get(JsonKey.ID));
            response.put(JsonKey.RESPONSE, JsonKey.FAILURE);
          }
        }
      }
      return Promise.<Result>pure(createCommonResponse(response, null, request()));
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  private void validateRequestData(Map<String, Object> payload) {
    for(Map.Entry<String, Object> entry : payload.entrySet()){
      if(entry.getValue() instanceof BigInteger){
        payload.put(entry.getKey(), Integer.parseInt(String.valueOf(entry.getValue())));
      }
    }
  }

  private void validateTableName(Request reqObj) {
    if (!tableList.contains((String) reqObj.getRequest().get(TABLE_NAME))) {
      throw new ProjectCommonException(ResponseCode.tableOrDocNameError.getErrorCode(),
          ResponseCode.tableOrDocNameError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

  }

  /**
   * This method will allow update Data in DB.
   * 
   * @return Promise<Result>
   */
  public Promise<Result> update() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("getting update data request = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      validateTableName(reqObj);
      Map<String, Object> payload = (Map<String, Object>) reqObj.getRequest().get(PAYLOAD);
      validateRequestData(payload);
      Response response = new Response();
      boolean esResult = false;
      if (!ProjectUtil.isStringNullOREmpty((String) reqObj.getRequest().get(DOCUMENT_NAME))) {
        esResult =
            updateDataToElastic(ES_INDEX_NAME, (String) reqObj.getRequest().get(DOCUMENT_NAME),
                (String) payload.get(JsonKey.ID), payload);
        if (esResult) {
          response = cassandraOperation.updateRecord(JsonKey.SUNBIRD_PLUGIN,
              (String) reqObj.getRequest().get(TABLE_NAME), payload);
          if (!((String) response.get(JsonKey.RESPONSE)).equals(JsonKey.SUCCESS)) {
            deleteDataFromElastic(ES_INDEX_NAME, (String) reqObj.getRequest().get(DOCUMENT_NAME),
                (String) payload.get(JsonKey.ID));
            response.put(JsonKey.RESPONSE, JsonKey.FAILURE);
          }
        }
      }
      return Promise.<Result>pure(createCommonResponse(response, null, request()));
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will allow to delete Data in DB.
   * 
   * @return Promise<Result>
   */
  public Promise<Result> delete() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("getting delete data request = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      validateTableName(reqObj);
      Response response = cassandraOperation.deleteRecord(JsonKey.SUNBIRD_PLUGIN,
          (String) reqObj.getRequest().get(TABLE_NAME),
          (String) reqObj.getRequest().get(JsonKey.ID));
      if (((String) response.get(JsonKey.RESPONSE)).equals(JsonKey.SUCCESS)
          && !ProjectUtil.isStringNullOREmpty((String) reqObj.getRequest().get(DOCUMENT_NAME))) {
        deleteDataFromElastic(ES_INDEX_NAME, (String) reqObj.getRequest().get(DOCUMENT_NAME),
            (String) reqObj.getRequest().get(JsonKey.ID));
      }
      return Promise.<Result>pure(createCommonResponse(response, null, request()));
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }


  /**
   * This method will allow to read Data from DB/ES.
   * 
   * @return Promise<Result>
   */
  public Promise<Result> read() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("getting read data request = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      Response response = new Response();
      validateTableName(reqObj);
      if (!ProjectUtil.isStringNullOREmpty((String) reqObj.getRequest().get(DOCUMENT_NAME))) {
        Map<String, Object> result = ElasticSearchUtil.getDataByIdentifier(ES_INDEX_NAME,
            (String) reqObj.getRequest().get(DOCUMENT_NAME),
            (String) reqObj.getRequest().get(JsonKey.ID));
        result.remove(JsonKey.IDENTIFIER);
        if (!result.isEmpty()) {
          response.put(JsonKey.RESPONSE, result);
        } else {
          response.put(JsonKey.RESPONSE, new HashMap<>());
        }
      } else if (!ProjectUtil.isStringNullOREmpty((String) reqObj.getRequest().get(TABLE_NAME))) {
        response = cassandraOperation.getRecordById(JsonKey.SUNBIRD_PLUGIN,
            (String) reqObj.getRequest().get(TABLE_NAME),
            (String) reqObj.getRequest().get(JsonKey.ID));
      } else {
        throw new ProjectCommonException(ResponseCode.tableOrDocNameError.getErrorCode(),
            ResponseCode.tableOrDocNameError.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      return Promise.<Result>pure(createCommonResponse(response, null, request()));
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }


  /**
   * This method will allow to read all Data from DB/ES.
   * 
   * @return Promise<Result>
   */
  public Promise<Result> readAll() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("getting read data request = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      Response response = new Response();
      validateTableName(reqObj);
      if (!ProjectUtil.isStringNullOREmpty((String) reqObj.getRequest().get(DOCUMENT_NAME))) {
        Map<String, Object> result = ElasticSearchUtil.complexSearch(new SearchDTO(), ES_INDEX_NAME,
            (String) reqObj.getRequest().get(DOCUMENT_NAME));
        if (!result.isEmpty()) {
          List<Map<String, Object>> mapList =
              (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
          for (Map<String, Object> map : mapList) {
            map.remove(JsonKey.IDENTIFIER);
          }
          response.put(JsonKey.RESPONSE, result);
        } else {
          response.put(JsonKey.RESPONSE, new HashMap<>());
        }
      } else if (!ProjectUtil.isStringNullOREmpty((String) reqObj.getRequest().get(TABLE_NAME))) {
        response = cassandraOperation.getAllRecords(JsonKey.SUNBIRD_PLUGIN,
            (String) reqObj.getRequest().get(TABLE_NAME));
      } else {
        throw new ProjectCommonException(ResponseCode.tableOrDocNameError.getErrorCode(),
            ResponseCode.tableOrDocNameError.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      return Promise.<Result>pure(createCommonResponse(response, null, request()));
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }


  /**
   * This method will allow to search Data from ES.
   * 
   * @return Promise<Result>
   */
  public Promise<Result> search() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("getting read data request = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      Response response = new Response();
      if (!ProjectUtil.isStringNullOREmpty((String) reqObj.getRequest().get(DOCUMENT_NAME))) {
        String esType = (String) reqObj.getRequest().get(DOCUMENT_NAME);
        reqObj.getRequest().remove(DOCUMENT_NAME);
        if(null !=  reqObj.getRequest().get(JsonKey.FILTERS)){
          validateRequestData((Map<String, Object>) reqObj.getRequest().get(JsonKey.FILTERS));
        }
        SearchDTO searchDto = Util.createSearchDto(reqObj.getRequest());
        Map<String, Object> result = ElasticSearchUtil.complexSearch(searchDto, ES_INDEX_NAME, esType);
        if (!result.isEmpty()) {
          List<Map<String, Object>> mapList =
              (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
          for (Map<String, Object> map : mapList) {
            map.remove(JsonKey.IDENTIFIER);
          }
          response.put(JsonKey.RESPONSE, result);
        } else {
          response.put(JsonKey.RESPONSE, new HashMap<>());
        }
      } else {
        throw new ProjectCommonException(ResponseCode.tableOrDocNameError.getErrorCode(),
            ResponseCode.tableOrDocNameError.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      return Promise.<Result>pure(createCommonResponse(response, null, request()));
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
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
  private boolean insertDataToElastic(String index, String type, String identifier,
      Map<String, Object> data) {
    ProjectLogger
        .log("making call to ES for type ,identifier ,data==" + type + " " + identifier + data);
    String response = ElasticSearchUtil.createData(index, type, identifier, data);
    ProjectLogger.log("Getting ES save response for type , identiofier==" + type + "  " + identifier
        + "  " + response);
    if (!ProjectUtil.isStringNullOREmpty(response)) {
      ProjectLogger.log("Data is saved successfully ES ." + type + "  " + identifier);
      return true;
    }
    ProjectLogger.log("unbale to save the data inside ES with identifier " + identifier,
        LoggerEnum.INFO.name());
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
  private boolean updateDataToElastic(String indexName, String typeName, String identifier,
      Map<String, Object> data) {
    boolean response = ElasticSearchUtil.updateData(indexName, typeName, identifier, data);
    if (response) {
      return true;
    }
    ProjectLogger.log("unbale to save the data inside ES with identifier " + identifier,
        LoggerEnum.INFO.name());
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
  private boolean deleteDataFromElastic(String indexName, String typeName, String identifier) {
    boolean response = ElasticSearchUtil.removeData(indexName, typeName, identifier);
    if (response) {
      return true;
    }
    ProjectLogger.log("unbale to delete the data from ES with identifier " + identifier,
        LoggerEnum.INFO.name());
    return false;

  }

  private void deleteRecord(String keyspaceName, String tableName, String identifier) {
    cassandraOperation.deleteRecord(keyspaceName, tableName, identifier);
  }
}
