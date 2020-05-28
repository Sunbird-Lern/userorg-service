package org.sunbird.common;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.bean.ClaimStatus;
import org.sunbird.bean.ShadowUser;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.dto.SearchDTO;
import org.sunbird.feed.FeedUtil;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.UserFlagEnum;
import org.sunbird.learner.util.UserFlagUtil;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.UserType;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;
import org.sunbird.telemetry.util.TelemetryUtil;
import scala.concurrent.Future;

public class ShadowUserProcessor {
  private Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ObjectMapper mapper = new ObjectMapper();
  private Map<String, String> hashTagIdMap = new HashMap<>();
  private Util.DbInfo bulkUploadDbInfo = Util.dbInfoMap.get(JsonKey.BULK_OP_DB);
  private Map<String, String> extOrgIdMap = new HashMap<>();
  private String custodianOrgId;
  private SSOManager keyCloakService = SSOServiceFactory.getInstance();
  private Map<String, Map<String, Object>> processIdtelemetryCtxMap = new HashMap<>();
  private ElasticSearchService elasticSearchService = EsClientFactory.getInstance(JsonKey.REST);

  public void process() {
    processAllUnclaimedUser();
    ProjectLogger.log(
        "ShadowUserProcessor:process:successfully processed shadow user Stage3 ended",
        LoggerEnum.INFO.name());
  }

  private void processSingleShadowUser(ShadowUser shadowUser) {
    ProjectLogger.log(
        "ShadowUserProcessor:processSingleShadowUser:started claiming  shadow user with processId: "
            + shadowUser.getProcessId(),
        LoggerEnum.INFO.name());
    updateUser(shadowUser);
  }

  /**
   * this method will be called when the user is already claimed need to update the user
   *
   * @param shadowUser
   */
  public void processClaimedUser(ShadowUser shadowUser) {
    ProjectLogger.log(
        "ShadowUserProcessor:processClaimedUser:started claming shadow user with processId: "
            + shadowUser.getProcessId(),
        LoggerEnum.INFO.name());
    String orgId = getOrgId(shadowUser);
    Map<String, Object> esUser =
        (Map<String, Object>)
            ElasticSearchHelper.getResponseFromFuture(
                elasticSearchService.getDataByIdentifier(
                    ProjectUtil.EsType.user.getTypeName(), shadowUser.getUserId()));
    String userId = (String) esUser.get(JsonKey.ID);
    String rootOrgId = (String) esUser.get(JsonKey.ROOT_ORG_ID);
    ProjectLogger.log(
        "ShadowUserProcessor:processClaimedUser:started: flag value got from es "
            + esUser.get(JsonKey.FLAGS_VALUE),
        LoggerEnum.INFO.name());
    int flagsValue =
        null != esUser.get(JsonKey.FLAGS_VALUE)
            ? (int) esUser.get(JsonKey.FLAGS_VALUE)
            : 0; // since we are migrating the user from custodian org to non custodian org.
    ProjectLogger.log(
        "ShadowUserProcessor:processClaimedUser:Got Flag Value " + flagsValue,
        LoggerEnum.INFO.name());
    if (!((String) esUser.get(JsonKey.FIRST_NAME)).equalsIgnoreCase(shadowUser.getName())
        || ((int) esUser.get(JsonKey.STATUS)) != shadowUser.getUserStatus()) {
      updateUserInUserTable(flagsValue, shadowUser.getUserId(), rootOrgId, shadowUser);
      if(shadowUser.getUserStatus()==ProjectUtil.Status.INACTIVE.getValue()){
        deactivateUserFromKC(userId);
      }
    }
    deleteUserFromOrganisations(
        shadowUser, rootOrgId, (List<Map<String, Object>>) esUser.get(JsonKey.ORGANISATIONS));
    if (StringUtils.isNotBlank(orgId) && !getOrganisationIds(esUser).contains(orgId)) {
      registerUserToOrg(userId, orgId);
    }
    syncUserToES(userId);
    updateUserInShadowDb(userId, shadowUser, ClaimStatus.CLAIMED.getValue(), null);
  }

  private void deactivateUserFromKC(String userId){
    Map<String,Object>userMap=new HashMap<>();
    userMap.put(JsonKey.USER_ID,userId);
    try {
      ProjectLogger.log("ShadowUserProcessor:processClaimedUse:request Got to deactivate user account from KC:" + userMap, LoggerEnum.INFO.name());
      String status = keyCloakService.deactivateUser(userMap);
      ProjectLogger.log("ShadowUserProcessor:processClaimedUse:deactivate user account from KC:" + status, LoggerEnum.INFO.name());
    }catch (Exception e){
      ProjectLogger.log("ShadowUserProcessor:processClaimedUse:Error occurred while deactivate user account from KC:" + userId, LoggerEnum.ERROR.name());
    }
  }

  private boolean isRootOrgMatchedWithOrgId(String rootOrgId, String orgId) {
    if (StringUtils.equalsIgnoreCase(rootOrgId, orgId)) {
      return true;
    }
    return false;
  }

  private void deleteUserFromOrganisations(
      ShadowUser shadowUser, String rootOrgId, List<Map<String, Object>> organisations) {
    organisations
        .stream()
        .forEach(
            organisation -> {
              String orgId = (String) organisation.get(JsonKey.ORGANISATION_ID);
              if (!isRootOrgMatchedWithOrgId(rootOrgId, orgId)) {
                String id = (String) organisation.get(JsonKey.ID);
                updateStatusInUserOrg(shadowUser, id);
              }
            });
  }

  private void updateStatusInUserOrg(ShadowUser shadowUser, String id) {
    Map<String, Object> propertiesMap = new WeakHashMap<>();
    propertiesMap.put(JsonKey.ID, id);
    propertiesMap.put(JsonKey.IS_DELETED, true);
    propertiesMap.put(JsonKey.UPDATED_BY, shadowUser.getAddedBy());
    propertiesMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    Response response =
        cassandraOperation.updateRecord(JsonKey.SUNBIRD, JsonKey.USER_ORG, propertiesMap);
    ProjectLogger.log(
        "ShadowUserProcessor:updateStatusInUserOrg:response from cassandra in updating user org "
            .concat(response + ""),
        LoggerEnum.INFO.name());
  }

  private List<Map<String, Object>> getUserMatchedIdentifierFromES(ShadowUser shadowUser) {
    Map<String, Object> request = new WeakHashMap<>();
    Map<String, Object> filters = new WeakHashMap<>();
    Map<String, Object> or = new WeakHashMap<>();
    if (StringUtils.isNotBlank(shadowUser.getEmail())) {
      or.put(JsonKey.EMAIL, shadowUser.getEmail());
    }
    if (StringUtils.isNotBlank(shadowUser.getPhone())) {
      or.put(JsonKey.PHONE, shadowUser.getPhone());
    }
    filters.put(JsonKey.ES_OR_OPERATION, or);
    filters.put(JsonKey.ROOT_ORG_ID, getCustodianOrgId());
    request.put(JsonKey.FILTERS, filters);
    ProjectLogger.log(
        "ShadowUserProcessor:getUserMatchedIdentifierFromES:the filter prepared for elastic search with processId: "
            + shadowUser.getProcessId()
            + " :filters are:"
            + filters,
        LoggerEnum.INFO.name());
    SearchDTO searchDTO = ElasticSearchHelper.createSearchDTO(request);
    searchDTO.setFields(
        new ArrayList<String>(
            Arrays.asList(
                JsonKey.ID,
                JsonKey.CHANNEL,
                JsonKey.EMAIL,
                JsonKey.PHONE,
                JsonKey.ROOT_ORG_ID,
                JsonKey.FLAGS_VALUE,
                JsonKey.ORGANISATIONS,
                JsonKey.IS_DELETED,
                JsonKey.STATUS)));
    Map<String, Object> response =
        (Map<String, Object>)
            ElasticSearchHelper.getResponseFromFuture(
                elasticSearchService.search(searchDTO, JsonKey.USER));
    ProjectLogger.log(
        "ShadowUserProcessor:getUserMatchedIdentifierFromES:response got from elasticSearch is with processId: "
            + shadowUser.getProcessId()
            + " :response is"
            + response,
        LoggerEnum.INFO.name());
    return (List<Map<String, Object>>) response.get(JsonKey.CONTENT);
  }

  /**
   * this method will search in ES with phone/email, if multiple user's found(cond: if a user
   * provide email of himself and phone of other user) it will mark user claimStatus to multimatch.
   * else it will get the user_id from ES response and then it will fetch all the record from
   * shadow_user table who are not claimed, rejected and failed with same user_id. if found multiple
   * records after removing the users of same channel from multiMatchRecords (cond: to avoid
   * changing of claimStatus to MULTIMATCH if respective user update happened )then will update
   * respective and rest of the user's claimStatus to MULTIMATCH. else if filterRecords is empty
   * then the respective user claimStatus will be marked as ELIGIBLE IN shadow_user table
   *
   * @param shadowUser
   */
  private void updateUser(ShadowUser shadowUser) {
    List<Map<String, Object>> esUser = getUserMatchedIdentifierFromES(shadowUser);
    ProjectLogger.log(
        "ShadowUserProcessor:updateUser:GOT ES RESPONSE FOR USER WITH SIZE " + esUser.size(),
        LoggerEnum.INFO.name());
    if (CollectionUtils.isNotEmpty(esUser)) {
      if (esUser.size() == 1) {
        ProjectLogger.log(
            "ShadowUserProcessor:updateUser:Got single user:"
                + esUser
                + " :with processId"
                + shadowUser.getProcessId(),
            LoggerEnum.INFO.name());
        Map<String, Object> userMap = esUser.get(0);
        if (!isSame(shadowUser, userMap)) {
          ProjectLogger.log(
              "ShadowUserProcessor:updateUser: provided user details doesn't match with existing user details with processId"
                  + shadowUser.getProcessId()
                  + userMap,
              LoggerEnum.INFO.name());
          List<String> userIds = new ArrayList<>();
          userIds.add((String) userMap.get(JsonKey.ID));
          updateUserInShadowDb(null, shadowUser, ClaimStatus.ELIGIBLE.getValue(), userIds);
          ProjectLogger.log(
              "ShadowUserProcessor:updateUser: calling FeedUtil.saveFeed method for user id "
                  + userIds.get(0),
              LoggerEnum.INFO.name());
          FeedUtil.saveFeed(shadowUser, userIds);
        }
      } else if (esUser.size() > 1) {
        ProjectLogger.log(
            "ShadowUserProcessor:updateUser:GOT response from ES :" + esUser,
            LoggerEnum.INFO.name());
        updateUserInShadowDb(
            null, shadowUser, ClaimStatus.MULTIMATCH.getValue(), getMatchingUserIds(esUser));
      }
    } else {
      ProjectLogger.log(
          "ShadowUserProcessor:updateUser:SKIPPING SHADOW USER:" + shadowUser.toString(),
          LoggerEnum.INFO.name());
    }
    esUser.clear();
  }

  private void generateTelemetry(String userId, String rootOrgId, ShadowUser shadowUser) {
   // ExecutionContext.getCurrent()
    //    .setRequestContext(getTelemetryContextByProcessId((String) shadowUser.getProcessId()));
    ProjectLogger.log(
        "ShadowUserProcessor:generateTelemetry:generate telemetry:" + shadowUser.toString(),
        LoggerEnum.INFO.name());
    Map<String, Object> targetObject = new HashMap<>();
    Map<String, String> rollUp = new HashMap<>();
    rollUp.put("l1", rootOrgId);
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    //ExecutionContext.getCurrent().getRequestContext().put(JsonKey.ROLLUP, rollUp);
    TelemetryUtil.generateCorrelatedObject(
        shadowUser.getProcessId(), JsonKey.PROCESS_ID, null, correlatedObject);
    targetObject =
        TelemetryUtil.generateTargetObject(
            userId, StringUtils.capitalize(JsonKey.USER), JsonKey.MIGRATION_USER_OBJECT, null);
    //TelemetryUtil.telemetryProcessingCall(
    //    mapper.convertValue(shadowUser, Map.class), targetObject, correlatedObject);
  }

  /**
   * this method will be used to get all the userIds got from ES response by searching with
   * phone/email + custoRootOrgId
   *
   * @param esUser
   * @return
   */
  private List<String> getMatchingUserIds(List<Map<String, Object>> esUser) {
    ProjectLogger.log(
        "ShadowUserProcessor:getMatchingUserIds:GOT response from counting matchingUserIds:"
            + esUser.size(),
        LoggerEnum.INFO.name());
    List<String> matchingUserIds = new ArrayList<>();
    esUser
        .stream()
        .forEach(
            singleEsUser -> {
              matchingUserIds.add((String) singleEsUser.get(JsonKey.ID));
            });
    return matchingUserIds;
  }

  private void updateUserInUserTable(
      int flagValue, String userId, String rootOrgId, ShadowUser shadowUser) {
    Map<String, Object> propertiesMap = new WeakHashMap<>();
    propertiesMap.put(JsonKey.FIRST_NAME, shadowUser.getName());
    propertiesMap.put(JsonKey.ID, userId);
    if (!(UserFlagUtil.assignUserFlagValues(flagValue).get(JsonKey.STATE_VALIDATED))) {
      ProjectLogger.log(
          "ShadowUserProcessor:updateUserInUserTable: updating Flag Value", LoggerEnum.INFO.name());
      propertiesMap.put(
          JsonKey.FLAGS_VALUE, flagValue + UserFlagEnum.STATE_VALIDATED.getUserFlagValue());
    }
    propertiesMap.put(JsonKey.UPDATED_BY, shadowUser.getAddedBy());
    propertiesMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    if (shadowUser.getUserStatus() == ProjectUtil.Status.ACTIVE.getValue()) {
      propertiesMap.put(JsonKey.IS_DELETED, false);
      propertiesMap.put(JsonKey.STATUS, ProjectUtil.Status.ACTIVE.getValue());
    } else {
      propertiesMap.put(JsonKey.IS_DELETED, true);
      propertiesMap.put(JsonKey.STATUS, ProjectUtil.Status.INACTIVE.getValue());
    }
    propertiesMap.put(JsonKey.USER_TYPE, UserType.TEACHER.getTypeName());
    propertiesMap.put(JsonKey.CHANNEL, shadowUser.getChannel());
    propertiesMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
    ProjectLogger.log(
        "ShadowUserProcessor:updateUserInUserTable: properties map formed for user update: "
            + propertiesMap,
        LoggerEnum.INFO.name());
    Response response =
        cassandraOperation.updateRecord(
            usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), propertiesMap);
    ProjectLogger.log(
        "ShadowUserProcessor:updateUserInUserTable:user is updated with shadow user:RESPONSE FROM CASSANDRA IS:"
            + response.getResult(),
        LoggerEnum.INFO.name());
    generateTelemetry(userId, rootOrgId, shadowUser);
  }

  /**
   * this method
   *
   * @return
   */
  private String getCustodianOrgId() {
    if (StringUtils.isNotBlank(custodianOrgId)) {
      ProjectLogger.log(
          "ShadowUserProcessor:getCustodianOrgId:CUSTODIAN ORD ID FOUND in cache:" + custodianOrgId,
          LoggerEnum.INFO.name());
      return custodianOrgId;
    }
    Response response =
        cassandraOperation.getRecordById(
            JsonKey.SUNBIRD, JsonKey.SYSTEM_SETTINGS_DB, JsonKey.CUSTODIAN_ORG_ID);
    List<Map<String, Object>> result = new ArrayList<>();
    if (!((List) response.getResult().get(JsonKey.RESPONSE)).isEmpty()) {
      result = ((List) response.getResult().get(JsonKey.RESPONSE));
      Map<String, Object> resultMap = result.get(0);
      custodianOrgId = (String) resultMap.get(JsonKey.VALUE);
      ProjectLogger.log(
          "ShadowUserProcessor:getCustodianOrgId:CUSTODIAN ORD ID FOUND in DB:" + custodianOrgId,
          LoggerEnum.INFO.name());
    }

    if (StringUtils.isBlank(custodianOrgId)) {
      ProjectLogger.log(
          "ShadowUserProcessor:getCustodianOrgId:No CUSTODIAN ORD ID FOUND PLEASE HAVE THAT IN YOUR ENVIRONMENT",
          LoggerEnum.ERROR.name());
      System.exit(-1);
    }
    return custodianOrgId;
  }

  private FutureCallback<ResultSet> getSyncCallback() {
    return new FutureCallback<ResultSet>() {
      @Override
      public void onSuccess(ResultSet result) {
        Map<String, String> columnMap = CassandraUtil.fetchColumnsMapping(result);
        try {
          Iterator<Row> resultIterator = result.iterator();
          while (resultIterator.hasNext()) {
            Row row = resultIterator.next();
            Map<String, Object> doc = syncDataForEachRow(row, columnMap);
            ShadowUser singleShadowUser = mapper.convertValue(doc, ShadowUser.class);
            processSingleShadowUser(singleShadowUser);
            ProjectLogger.log(
                "ShadowUserProcessor:getSyncCallback:SUCCESS:SYNC CALLBACK SUCCESSFULLY PROCESSED for Shadow user: "
                    + singleShadowUser.toString(),
                LoggerEnum.INFO.name());
          }
          ProjectLogger.log(
              "ShadowUserProcessor:getSyncCallback:SUCCESS:SYNC CALLBACK SUCCESSFULLY MIGRATED  ALL Shadow user",
              LoggerEnum.INFO.name());

        } catch (Exception e) {
          ProjectLogger.log(
              "ShadowUserProcessor:getSyncCallback:SUCCESS:ERROR OCCURRED WHILE GETTING SYNC CALLBACKS"
                  + e,
              LoggerEnum.ERROR.name());
        }
      }

      @Override
      public void onFailure(Throwable t) {
        ProjectLogger.log(
            "ShadowUserProcessor:getSyncCallback:FAILURE:ERROR OCCURRED WHILE GETTING SYNC CALLBACKS"
                + t,
            LoggerEnum.ERROR.name());
      }
    };
  }

  private Map<String, Object> syncDataForEachRow(Row row, Map<String, String> columnMap) {
    Map<String, Object> rowMap = new HashMap<>();
    columnMap
        .entrySet()
        .forEach(
            entry -> {
              Object value = row.getObject(entry.getValue());
              rowMap.put(entry.getKey(), value);
            });
    ProjectLogger.log(
        "ShadowUserProcessor:syncDataForEachRow:row map returned " + rowMap,
        LoggerEnum.INFO.name());
    return rowMap;
  }

  private void processAllUnclaimedUser() {

    ProjectLogger.log(
        "ShadowUserProcessor:processAllUnclaimedUser:started processing all unclaimed user",
        LoggerEnum.INFO.name());
    getUnclaimedRowsFromShadowUserDb();
  }

  private void getUnclaimedRowsFromShadowUserDb() {
    Map<String, Object> propertiesMap = new WeakHashMap<>();
    propertiesMap.put(JsonKey.CLAIM_STATUS, ClaimStatus.UNCLAIMED.getValue());
    cassandraOperation.applyOperationOnRecordsAsync(
        JsonKey.SUNBIRD, JsonKey.SHADOW_USER, propertiesMap, null, getSyncCallback());
    propertiesMap.clear();
  }

  private boolean isSame(ShadowUser shadowUser, Map<String, Object> esUserMap) {
    String orgId = getOrgId(shadowUser);
    if (!shadowUser.getName().equalsIgnoreCase((String) esUserMap.get(JsonKey.FIRST_NAME))) {
      return false;
    }
    if (StringUtils.isNotBlank(orgId) && !getOrganisationIds(esUserMap).contains(orgId)) {
      return false;
    }
    if (shadowUser.getUserStatus() != (int) (esUserMap.get(JsonKey.STATUS))) {
      return false;
    }
    if (StringUtils.isBlank(orgId)) {
      return false;
    }
    return true;
  }

  private void updateUserInShadowDb(
      String userId, ShadowUser shadowUser, int claimStatus, List<String> matchingUserIds) {
    Map<String, Object> propertiesMap = new HashMap<>();
    propertiesMap.put(JsonKey.CLAIM_STATUS, claimStatus);
    propertiesMap.put(JsonKey.PROCESS_ID, shadowUser.getProcessId());
    propertiesMap.put(JsonKey.USER_ID, userId);
    propertiesMap.put(JsonKey.USER_IDs, matchingUserIds);
    Map<String, Object> compositeKeysMap = new HashMap<>();
    compositeKeysMap.put(JsonKey.CHANNEL, shadowUser.getChannel());
    compositeKeysMap.put(JsonKey.USER_EXT_ID, shadowUser.getUserExtId());
    Response response =
        cassandraOperation.updateRecord(
            JsonKey.SUNBIRD, JsonKey.SHADOW_USER, propertiesMap, compositeKeysMap);
    ProjectLogger.log(
        "ShadowUserProcessor:updateUserInShadowDb:update:with processId: "
            + shadowUser.getProcessId()
            + " :and response is:"
            + response,
        LoggerEnum.INFO.name());
  }

  private String getOrgId(ShadowUser shadowUser) {
    if (StringUtils.isNotBlank(shadowUser.getOrgExtId())) {
      String orgId =
          extOrgIdMap.get(shadowUser.getChannel().concat(":").concat(shadowUser.getOrgExtId()));
      if (StringUtils.isNotBlank(orgId)) {
        return orgId;
      }
      Map<String, Object> request = new HashMap<>();
      Map<String, Object> filters = new HashMap<>();
      filters.put(JsonKey.EXTERNAL_ID, shadowUser.getOrgExtId().toLowerCase());
      filters.put(JsonKey.CHANNEL, shadowUser.getChannel());
      request.put(JsonKey.FILTERS, filters);
      ProjectLogger.log(
          "ShadowUserProcessor:getOrgId: request map prepared to query elasticsearch for org id :"
              + filters
              + "with processId"
              + shadowUser.getProcessId(),
          LoggerEnum.INFO.name());
      SearchDTO searchDTO = ElasticSearchHelper.createSearchDTO(request);
      Map<String, Object> response =
          (Map<String, Object>)
              ElasticSearchHelper.getResponseFromFuture(
                  elasticSearchService.search(
                      searchDTO, ProjectUtil.EsType.organisation.getTypeName()));
      List<Map<String, Object>> orgData =
          ((List<Map<String, Object>>) response.get(JsonKey.CONTENT));
      if (CollectionUtils.isNotEmpty(orgData)) {
        Map<String, Object> orgMap = orgData.get(0);
        extOrgIdMap.put(
            shadowUser.getChannel().concat(":").concat(shadowUser.getOrgExtId()),
            (String) orgMap.get(JsonKey.ID));
        return (String) orgMap.get(JsonKey.ID);
      }
    }
    return StringUtils.EMPTY;
  }

  private List<String> getOrganisationIds(Map<String, Object> dbUser) {
    List<String> organisationsIds = new ArrayList<>();
    ((List<Map<String, Object>>) dbUser.get(JsonKey.ORGANISATIONS))
        .stream()
        .forEach(
            organisation -> {
              organisationsIds.add((String) organisation.get(JsonKey.ORGANISATION_ID));
            });
    return organisationsIds;
  }

  private void syncUserToES(String userId) {
    Map<String, Object> fullUserDetails = Util.getUserDetails(userId, null);
    try {
      Future<Boolean> future = elasticSearchService.update(JsonKey.USER, userId, fullUserDetails);
      if ((boolean) ElasticSearchHelper.getResponseFromFuture(future)) {
        ProjectLogger.log(
            "ShadowUserMigrationScheduler:updateUserStatus: data successfully updated to elastic search with userId:"
                .concat(userId + ""),
            LoggerEnum.INFO.name());
      }
    } catch (Exception e) {
      e.printStackTrace();
      ProjectLogger.log(
          "ShadowUserMigrationScheduler:syncUserToES: data failed to updates in elastic search with userId:"
              .concat(userId + ""),
          LoggerEnum.ERROR.name());
    }
  }

  private void registerUserToOrg(String userId, String organisationId) {
    Map<String, Object> reqMap = new WeakHashMap<>();
    List<String> roles = new ArrayList<>();
    roles.add(ProjectUtil.UserRole.PUBLIC.getValue());
    reqMap.put(JsonKey.ROLES, roles);
    String hashTagId = hashTagIdMap.get(organisationId);
    if (StringUtils.isBlank(hashTagId)) {
      hashTagId = Util.getHashTagIdFromOrgId(organisationId);
      hashTagIdMap.put(organisationId, hashTagId);
    }
    reqMap.put(JsonKey.HASHTAGID, hashTagId);
    reqMap.put(JsonKey.ID, ProjectUtil.getUniqueIdFromTimestamp(1));
    reqMap.put(JsonKey.USER_ID, userId);
    reqMap.put(JsonKey.ORGANISATION_ID, organisationId);
    reqMap.put(JsonKey.ORG_JOIN_DATE, ProjectUtil.getFormattedDate());
    reqMap.put(JsonKey.IS_DELETED, false);
    Util.DbInfo usrOrgDb = Util.dbInfoMap.get(JsonKey.USR_ORG_DB);
    try {
      Response response =
          cassandraOperation.insertRecord(usrOrgDb.getKeySpace(), usrOrgDb.getTableName(), reqMap);
      ProjectLogger.log(
          "ShadowUserProcessor:registerUserToOrg:user status while registration with org is:"
              + response.getResult(),
          LoggerEnum.INFO.name());

    } catch (Exception e) {
      ProjectLogger.log(
          "ShadowUserProcessor:registerUserToOrg:user is failed to register with org" + userId,
          LoggerEnum.ERROR.name());
    }
  }

  private Map<String, Object> getTelemetryContextByProcessId(String processId) {

    if (MapUtils.isNotEmpty(processIdtelemetryCtxMap.get(processId))) {
      return processIdtelemetryCtxMap.get(processId);
    }
    Map<String, String> contextMap = new HashMap<>();
    Map<String, Object> telemetryContext = new HashMap<>();
    Response response =
        cassandraOperation.getRecordById(
            bulkUploadDbInfo.getKeySpace(), bulkUploadDbInfo.getTableName(), processId);
    List<Map<String, Object>> result = new ArrayList<>();
    if (!((List) response.getResult().get(JsonKey.RESPONSE)).isEmpty()) {
      result = ((List) response.getResult().get(JsonKey.RESPONSE));
      Map<String, Object> responseMap = result.get(0);
      contextMap = (Map<String, String>) responseMap.get(JsonKey.CONTEXT_TELEMETRY);
      telemetryContext.putAll(contextMap);
      processIdtelemetryCtxMap.put(processId, telemetryContext);
    }
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:getFullRecordFromProcessId:got single row data from bulk_upload_process with processId:"
            + processId,
        LoggerEnum.INFO.name());
    return telemetryContext;
  }

  /**
   * METHOD WILL RETURN THE LIST OF SHADOW USER WHICH IS PRE EXISTING WITH USERID this method will
   * give all the record which are not claimed failed and rejected i.e will update the claim status
   * of the user who are ELIGIBLE.
   *
   * @param userId
   * @return
   */
  public List<ShadowUser> getMultiMatchRecords(String userId) {
    List<ShadowUser> shadowUsersList = new ArrayList<>();
    Response response =
        cassandraOperation.searchValueInList(
            JsonKey.SUNBIRD, JsonKey.SHADOW_USER, JsonKey.USERIDS, userId, null);
    if (!((List) response.getResult().get(JsonKey.RESPONSE)).isEmpty()) {
      ((List) response.getResult().get(JsonKey.RESPONSE))
          .stream()
          .forEach(
              shadowMap -> {
                ShadowUser shadowUser = mapper.convertValue(shadowMap, ShadowUser.class);
                if (shadowUser.getClaimStatus() != ClaimStatus.CLAIMED.getValue()
                    && shadowUser.getClaimStatus() != ClaimStatus.REJECTED.getValue()
                    && shadowUser.getClaimStatus() != ClaimStatus.FAILED.getValue()) {
                  shadowUsersList.add(shadowUser);
                }
              });
    }
    return shadowUsersList;
  }

  /**
   * userExtId, channel , userIds, claimStatus all the user found with same id in shadow_user table
   * there claim status will be updated to multimatch
   *
   * @param shadowUserList
   */
  private void changeStatusToMultiMatch(List<ShadowUser> shadowUserList) {
    shadowUserList
        .stream()
        .forEach(
            shadowUser -> {
              if (shadowUser.getClaimStatus() != ClaimStatus.CLAIMED.getValue()) {
                updateUserInShadowDb(
                    null, shadowUser, ClaimStatus.MULTIMATCH.getValue(), shadowUser.getUserIds());
                // TODO DELETE ENTRY FROM ALERTS TABLE
              }
            });
  }

  /**
   * this method will return all the user who doesn't belong to the same provided channel This
   * filtering will be needed to avoid update of claimStatus to MULTIMATCH of same user while
   * updating.
   *
   * @param channel
   * @param shadowUserList
   */
  private List<ShadowUser> getDiffChannelUsers(String channel, List<ShadowUser> shadowUserList) {

    List<ShadowUser> filterShadowUser = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(shadowUserList)) {
      shadowUserList
          .stream()
          .forEach(
              singleShadowUser -> {
                if (!StringUtils.equalsIgnoreCase(singleShadowUser.getChannel(), channel)) {
                  filterShadowUser.add(singleShadowUser);
                }
              });
    }
    return filterShadowUser;
  }



}
