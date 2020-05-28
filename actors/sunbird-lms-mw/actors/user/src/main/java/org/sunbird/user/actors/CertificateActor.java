package org.sunbird.user.actors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.background.BackgroundOperations;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.HttpUtilResponse;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.models.util.datasecurity.DataMaskingService;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.models.certificate.Certificate;
import org.sunbird.models.user.User;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserServiceImpl;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.*;

/** This class helps in interacting with adding and validating the user-certificate details */
@ActorConfig(
  tasks = {"validateCertificate", "addCertificate", "getSignUrl", "mergeUserCertificate"},
  asyncTasks = {}
)
public class CertificateActor extends UserBaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private Util.DbInfo certDbInfo = Util.dbInfoMap.get(JsonKey.USER_CERT);
  private Util.DbInfo userDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
  private UserService userService = UserServiceImpl.getInstance();
  private DecryptionService decryptionService =
          org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
                  "");
  private DataMaskingService maskingService =
          org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getMaskingServiceInstance("");

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    if (request.getOperation().equals(ActorOperations.VALIDATE_CERTIFICATE.getValue())) {
      getCertificate(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.ADD_CERTIFICATE.getValue())) {
      addCertificate(request);
    } else if (request.getOperation().equalsIgnoreCase(ActorOperations.GET_SIGN_URL.getValue())) {
      getSignUrl(request);
    } else if (ActorOperations.MERGE_USER_CERTIFICATE.getValue().equals(request.getOperation())) {
      mergeUserCertificate(request);
    } else {
      unSupportedMessage();
    }
  }

  /**
   * This method merges the certificates from custodian-user to non-custodian-user
   *
   * @param certRequest
   */
  private void mergeUserCertificate(Request certRequest) {
    Response userResult = new Response();
    Map request = certRequest.getRequest();
    String mergeeId = (String) request.get(JsonKey.FROM_ACCOUNT_ID);
    String mergerId = (String) request.get(JsonKey.TO_ACCOUNT_ID);
    Response response =
        cassandraOperation.getRecordsByProperty(
            certDbInfo.getKeySpace(), certDbInfo.getTableName(), JsonKey.USER_ID, mergeeId);
    Map<String, Object> record = response.getResult();
    if (null != record && null != record.get(JsonKey.RESPONSE)) {
      List responseList = (List) record.get(JsonKey.RESPONSE);
      if (!responseList.isEmpty()) {
        responseList.forEach(
            responseMap -> {
              Map<String, Object> responseDetails = (Map<String, Object>) responseMap;
              responseDetails.put(JsonKey.USER_ID, mergerId);
            });
        cassandraOperation.batchUpdateById(
            certDbInfo.getKeySpace(), certDbInfo.getTableName(), responseList);
        ProjectLogger.log(
            "CertificateActor:getCertificate: cert details merged to user id : " + mergerId,
            LoggerEnum.INFO.name());
      } else {
        ProjectLogger.log(
            "CertificateActor:getCertificate: cert details unavailable for user id : " + mergeeId,
            LoggerEnum.INFO.name());
      }
      userResult.setResponseCode(ResponseCode.success);
      sender().tell(userResult, self());
      sendMergeNotification(mergeeId, mergerId);
      triggerMergeCertTelemetry(request, certRequest.getContext());
    }
  }

  private void sendMergeNotification(String mergeeId, String mergerId) {
    ProjectLogger.log(
        "CertificateActor:sendMergeNotification start sending merge notification to user "
            + mergeeId
            + " -"
            + mergerId,
        LoggerEnum.INFO.name());
    List<String> ids = new ArrayList<String>();
    ids.add(mergeeId);
    ids.add(mergerId);
    List<String> attributeList = new ArrayList<>();
    attributeList.add(JsonKey.EMAIL);
    attributeList.add(JsonKey.PHONE);
    attributeList.add(JsonKey.PREV_USED_EMAIL);
    attributeList.add(JsonKey.PREV_USED_PHONE);
    attributeList.add(JsonKey.FIRST_NAME);
    attributeList.add(JsonKey.ID);
    List<HashMap<String, Object>> userList = getUserByIdentifiers(ids, attributeList);
    HashMap<String, Object> mergeeUserMap = new HashMap<>();
    HashMap<String, Object> mergerUserMap = new HashMap<>();
    if (userList != null && userList.size() == 2) {
      mergeeUserMap = getMergeeUserDetails(userList, mergeeId);
      mergerUserMap = getMergerUserDetails(userList, mergerId);
    }
    if (MapUtils.isNotEmpty(mergeeUserMap)) {
      mergeeUserMap = createUserData(mergeeUserMap, false);
    } else {
      ProjectLogger.log(
          "CertificateActor:sendMergeNotification mergee user account not found , so email or sms can't be sent "
              + mergeeId,
          LoggerEnum.INFO.name());
      return;
    }
    if (MapUtils.isNotEmpty(mergerUserMap)) {
      mergerUserMap = createUserData(mergerUserMap, true);
    } else {
      ProjectLogger.log(
          "CertificateActor:sendMergeNotification merger user account not found , so email or sms can't be sent "
              + mergeeId,
          LoggerEnum.INFO.name());
      return;
    }

    Request emailReq = createNotificationData(mergeeUserMap, mergerUserMap);
    tellToAnother(emailReq);
  }

  private HashMap<String, Object> getMergeeUserDetails(
      List<HashMap<String, Object>> userList, String mergeeId) {
    return getUserDetails(userList, mergeeId);
  }

  private HashMap<String, Object> getMergerUserDetails(
      List<HashMap<String, Object>> userList, String mergerId) {
    return getUserDetails(userList, mergerId);
  }

  private HashMap<String, Object> getUserDetails(
      List<HashMap<String, Object>> userList, String userId) {
    HashMap<String, Object> usermap = userList.get(0);
    if (userId.equalsIgnoreCase((String) usermap.get(JsonKey.ID))) {
      return usermap;
    } else {
      return userList.get(1);
    }
  }

  private HashMap<String, Object> createUserData(
      HashMap<String, Object> userMap, boolean isMergerAccount) {
      String MASK_IDENTIFIER = "maskIdentifier";
    if (isMergerAccount) {
      if (StringUtils.isNotBlank((String) userMap.get(JsonKey.EMAIL))) {
        String deceryptedEmail = decryptionService.decryptData((String) userMap.get(JsonKey.EMAIL));
        String maskEmail = maskingService.maskEmail(deceryptedEmail);
        userMap.put(JsonKey.EMAIL, deceryptedEmail);
        userMap.put(MASK_IDENTIFIER, maskEmail);
      } else {
        String deceryptedPhone = decryptionService.decryptData((String) userMap.get(JsonKey.PHONE));
        String maskPhone = maskingService.maskPhone(deceryptedPhone);
        userMap.put(JsonKey.PHONE, deceryptedPhone);
        userMap.put(MASK_IDENTIFIER, maskPhone);
      }
    } else {
      if (StringUtils.isNotBlank((String) userMap.get(JsonKey.PREV_USED_EMAIL))) {
        String deceryptedEmail =
            decryptionService.decryptData((String) userMap.get(JsonKey.PREV_USED_EMAIL));
        String maskEmail = maskingService.maskEmail(deceryptedEmail);
        userMap.put(JsonKey.PREV_USED_EMAIL, deceryptedEmail);
        userMap.put(MASK_IDENTIFIER, maskEmail);
      } else {
        String deceryptedPhone =
            decryptionService.decryptData((String) userMap.get(JsonKey.PREV_USED_PHONE));
        String maskPhone = maskingService.maskPhone(deceryptedPhone);
        userMap.put(JsonKey.PREV_USED_PHONE, deceryptedPhone);
        userMap.put(MASK_IDENTIFIER, maskPhone);
      }
    }
    return userMap;
  }

  private Request createNotificationData(
      HashMap<String, Object> mergeeUserData, HashMap<String, Object> mergerUserData) {
    Request request = new Request();
    Map<String, Object> requestMap = new HashMap<>();
    List<String> identifiers = new ArrayList<>();
    requestMap.put(JsonKey.NAME, mergerUserData.get(JsonKey.FIRST_NAME));
    requestMap.put(JsonKey.FIRST_NAME, mergerUserData.get(JsonKey.FIRST_NAME));
    if (StringUtils.isNotBlank((String) mergerUserData.get(JsonKey.EMAIL))) {
      identifiers.add((String) mergerUserData.get(JsonKey.EMAIL));
      requestMap.put(JsonKey.RECIPIENT_EMAILS, identifiers);
    } else {
      identifiers.add((String) mergerUserData.get(JsonKey.PHONE));
      requestMap.put(JsonKey.RECIPIENT_PHONES, identifiers);
      requestMap.put(JsonKey.MODE, "sms");
    }
    requestMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, "accountMerge");
    String MASK_IDENTIFIER = "maskIdentifier";
    String body =
        MessageFormat.format(
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_ACCOUNT_MERGE_BODY),
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_INSTALLATION),
            mergerUserData.get(MASK_IDENTIFIER),
            mergeeUserData.get(MASK_IDENTIFIER));
    requestMap.put(JsonKey.BODY, body);
    requestMap.put(JsonKey.SUBJECT, ProjectUtil.getConfigValue("sunbird_account_merge_subject"));
    request.getRequest().put(JsonKey.EMAIL_REQUEST, requestMap);
    request.setOperation(BackgroundOperations.emailService.name());
    return request;
  }

  private List<HashMap<String, Object>> getUserByIdentifiers(
      List<String> identifiers, List<String> attributes) {
    Response response =
        cassandraOperation.getRecordsByIdsWithSpecifiedColumns(
            userDbInfo.getKeySpace(), userDbInfo.getTableName(), attributes, identifiers);
    Map<String, Object> record = response.getResult();
    if (record != null && record.get(JsonKey.RESPONSE) != null) {
      List responseList = (List) record.get(JsonKey.RESPONSE);
      ProjectLogger.log(
          "CertificateActor:getUserByIdentifier user found with id:" + identifiers,
          LoggerEnum.INFO.name());
      return responseList;
    }
    ProjectLogger.log(
        "CertificateActor:getUserByIdentifier user not found with id:" + identifiers,
        LoggerEnum.INFO.name());
    return null;
  }

  /**
   * This method validates the access-code of the certificate and retrieve certificate details.
   *
   * @param userRequest
   */
  private void getCertificate(Request userRequest) throws IOException {
    Map request = userRequest.getRequest();
    String certificatedId = (String) request.get(JsonKey.CERT_ID);
    String accessCode = (String) request.get(JsonKey.ACCESS_CODE);
    Map<String, Object> responseDetails = getCertificateDetails(certificatedId);
    if (responseDetails.get(JsonKey.ACCESS_CODE.toLowerCase()).equals(accessCode)) {
      Map userResponse = new HashMap<String, Object>();
      Response userResult = new Response();
      Map recordStore = (Map<String, Object>) responseDetails.get(JsonKey.STORE);
      String jsonData = (String) recordStore.get(JsonKey.JSON_DATA);
      ObjectMapper objectMapper = new ObjectMapper();
      userResponse.put(JsonKey.JSON, objectMapper.readValue(jsonData, Map.class));
      userResponse.put(JsonKey.PDF, recordStore.get(JsonKey.PDF_URL));
      userResponse.put(JsonKey.COURSE_ID, recordStore.get(JsonKey.COURSE_ID));
      userResponse.put(JsonKey.BATCH_ID, recordStore.get(JsonKey.BATCH_ID));
      ProjectLogger.log(
          "CertificateActor:getCertificate: userMap got with certificateId "
                  .concat(certificatedId + "")
              + " and response got "
              + userResponse,
          LoggerEnum.INFO.name());
      userResult.put(JsonKey.RESPONSE, userResponse);
      sender().tell(userResult, self());
    } else {
      ProjectLogger.log(
          "CertificateActor:getCertificate: access code is incorrect : " + accessCode,
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.invalidParameter.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.invalidParameter.getErrorMessage(), JsonKey.ACCESS_CODE),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private Map getCertificateDetails(String certificatedId) {
    Map<String, Object> responseDetails = null;
    Response response =
        cassandraOperation.getRecordById(
            certDbInfo.getKeySpace(), certDbInfo.getTableName(), certificatedId);
    Map<String, Object> record = response.getResult();
    if (null != record && null != record.get(JsonKey.RESPONSE)) {
      List responseList = (List) record.get(JsonKey.RESPONSE);
      if (CollectionUtils.isNotEmpty(responseList)) {
        responseDetails = (Map<String, Object>) responseList.get(0);
        if(responseDetails.get(JsonKey.IS_DELETED) != null && (boolean)responseDetails.get(JsonKey.IS_DELETED)) {
          ProjectLogger.log(
                  "CertificateActor:getCertificate: certificate is deleted : ",
                  LoggerEnum.ERROR.name());
          ProjectCommonException.throwClientErrorException(
                  ResponseCode.errorUnavailableCertificate, null);
        }
      } else {
        ProjectLogger.log(
            "CertificateActor:getCertificate: cert id is incorrect : " + certificatedId,
            LoggerEnum.ERROR.name());
        throw new ProjectCommonException(
            ResponseCode.invalidParameter.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.invalidParameter.getErrorMessage(), JsonKey.CERT_ID),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
    return responseDetails;
  }

  private void addCertificate(Request request) throws JsonProcessingException {
    Response response = null;
    Map<String, String> storeMap = new HashMap<>();
    Map<String, Object> telemetryMap = new HashMap();
    Map<String, Object> certAddReqMap = request.getRequest();
    String userId = (String) certAddReqMap.get(JsonKey.USER_ID);
    String oldCertId = (String)certAddReqMap.get(JsonKey.OLD_ID);
    User user = userService.getUserById(userId);
    assureUniqueCertId((String) certAddReqMap.get(JsonKey.ID));
    populateStoreData(storeMap, certAddReqMap);
    certAddReqMap.put(JsonKey.STORE, storeMap);
    certAddReqMap = getRequiredRequest(certAddReqMap);
    certAddReqMap.put(JsonKey.CREATED_AT, getTimeStamp());
    if(StringUtils.isNotBlank(oldCertId)) {
      getCertificateDetails(oldCertId);
      HashMap<String, Object> certUpdatedMap = new HashMap<>(certAddReqMap);
      response  = reIssueCert(certAddReqMap, certUpdatedMap);
      telemetryMap.put(JsonKey.OLD_ID, oldCertId);
    } else {
      certAddReqMap.put(JsonKey.IS_DELETED,false);
      response =
              cassandraOperation.insertRecord(
                      certDbInfo.getKeySpace(), certDbInfo.getTableName(), certAddReqMap);
    }

    ProjectLogger.log(
        "CertificateActor:addCertificate:successfully added certificate in records with userId"
            + certAddReqMap.get(JsonKey.USER_ID)
            + " and certId:"
            + certAddReqMap.get(JsonKey.CERT_ID),
        LoggerEnum.INFO.name());
    sender().tell(response, self());
    telemetryMap.put(JsonKey.USER_ID, userId);
    telemetryMap.put(JsonKey.CERT_ID, certAddReqMap.get(JsonKey.ID));
    telemetryMap.put(JsonKey.ROOT_ORG_ID, user.getRootOrgId());
    triggerAddCertTelemetry(telemetryMap, request.getContext());
  }

  private Response reIssueCert(Map<String, Object> certAddReqMap, Map<String, Object> certUpdateReqMap) {
    Map<String, Object> cassandraInput = new HashMap<>();
    cassandraInput.put(JsonKey.INSERT, certAddReqMap);
    certAddReqMap.put(JsonKey.IS_DELETED,false);
    certUpdateReqMap.put(JsonKey.ID, certAddReqMap.get(JsonKey.OLD_ID));
    certUpdateReqMap.remove(JsonKey.OLD_ID);
    certUpdateReqMap.put(JsonKey.IS_DELETED,true);
    cassandraInput.put(JsonKey.UPDATE, certUpdateReqMap);
    return cassandraOperation.performBatchAction(certDbInfo.getKeySpace(), certDbInfo.getTableName(),cassandraInput);
  }

  private void populateStoreData(
      Map<String, String> storeMap, Map<String, Object> certAddRequestMap)
      throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    storeMap.put(JsonKey.PDF_URL, (String) certAddRequestMap.get(JsonKey.PDF_URL));
    storeMap.put(
        JsonKey.JSON_DATA,
        objectMapper.writeValueAsString(certAddRequestMap.get(JsonKey.JSON_DATA)));
    String batchId = (String) certAddRequestMap.get(JsonKey.BATCH_ID);
    String courseId = (String) certAddRequestMap.get(JsonKey.COURSE_ID);
    storeMap.put(JsonKey.BATCH_ID, StringUtils.isNotBlank(batchId) ? batchId : StringUtils.EMPTY);
    storeMap.put(JsonKey.COURSE_ID, StringUtils.isNotBlank(batchId) ? courseId : StringUtils.EMPTY);
    ProjectLogger.log(
        "CertificateActor:populateStoreMapWithUrlAndIds: store map after populated: " + storeMap,
        LoggerEnum.INFO.name());
  }

  private Map<String, Object> getRequiredRequest(Map<String, Object> certAddReqMap) {
    ObjectMapper objectMapper = new ObjectMapper();
    Certificate certificate = objectMapper.convertValue(certAddReqMap, Certificate.class);
    return objectMapper.convertValue(certificate, Map.class);
  }

  private Timestamp getTimeStamp() {
    return new Timestamp(Calendar.getInstance().getTime().getTime());
  }

  public void getSignUrl(Request request) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      HashMap<String, Object> certReqMap = new HashMap<>();
      certReqMap.put(JsonKey.REQUEST, request.getRequest());
      String requestBody = objectMapper.writeValueAsString(certReqMap);
      String completeUrl =
          ProjectUtil.getConfigValue(JsonKey.SUNBIRD_CERT_SERVICE_BASE_URL)
              + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_CERT_DOWNLOAD_URI);
      ProjectLogger.log(
          "CertificateActor:getSignUrl complete url found: " + completeUrl, LoggerEnum.INFO.name());

      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("Content-Type", "application/json");
      HttpUtilResponse httpResponse = HttpUtil.doPostRequest(completeUrl, requestBody, headerMap);
      if (httpResponse != null && httpResponse.getStatusCode() == 200) {
        HashMap<String, Object> val =
            (HashMap<String, Object>) objectMapper.readValue(httpResponse.getBody(), Map.class);
        HashMap<String, Object> resultMap = (HashMap<String, Object>) val.get(JsonKey.RESULT);
        Response response = new Response();
        response.put(JsonKey.SIGNED_URL, resultMap.get(JsonKey.SIGNED_URL));
        sender().tell(response, self());
      } else {
        throw new ProjectCommonException(
            ResponseCode.invalidParameter.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.invalidParameter.getErrorMessage(), JsonKey.PDF_URL),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }

    } catch (Exception e) {
      ProjectLogger.log(
          "CertificateActor:getSignUrl exception occurred :" + e, LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  private void assureUniqueCertId(String certificatedId) {
    if (isIdentityPresent(certificatedId)) {
      ProjectLogger.log(
          "CertificateActor:addCertificate:provided certificateId exists in record "
              .concat(certificatedId),
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.invalidParameter.getErrorCode(),
          ResponseMessage.Message.DATA_ALREADY_EXIST,
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    ProjectLogger.log(
        "CertificateActor:addCertificate:successfully certId not found in records creating new record",
        LoggerEnum.INFO.name());
  }

  private boolean isIdentityPresent(String certificateId) {
    Response response =
        cassandraOperation.getRecordById(
            certDbInfo.getKeySpace(), certDbInfo.getTableName(), certificateId);
    Map<String, Object> record = response.getResult();
    if (null != record && null != record.get(JsonKey.RESPONSE)) {
      List responseList = (List) record.get(JsonKey.RESPONSE);
      if (!responseList.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private void triggerMergeCertTelemetry(Map telemetryMap, Map<String,Object> context) {
    ProjectLogger.log(
        "UserMergeActor:triggerMergeCertTelemetry: generating telemetry event for merge certificate");
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, String> rollUp = new HashMap<>();
    rollUp.put("l1", (String) telemetryMap.get(JsonKey.ROOT_ORG_ID));
    context.put(JsonKey.ROLLUP, rollUp);
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) telemetryMap.get(JsonKey.FROM_ACCOUNT_ID),
            TelemetryEnvKey.USER,
            JsonKey.MERGE_CERT,
            null);
    TelemetryUtil.generateCorrelatedObject(
        (String) telemetryMap.get(JsonKey.FROM_ACCOUNT_ID),
        JsonKey.FROM_ACCOUNT_ID,
        null,
        correlatedObject);
    TelemetryUtil.generateCorrelatedObject(
        (String) telemetryMap.get(JsonKey.TO_ACCOUNT_ID),
        JsonKey.TO_ACCOUNT_ID,
        null,
        correlatedObject);
    telemetryMap.remove(JsonKey.ID);
    telemetryMap.remove(JsonKey.USER_ID);
    telemetryMap.remove(JsonKey.ROOT_ORG_ID);
    TelemetryUtil.telemetryProcessingCall(telemetryMap, targetObject, correlatedObject, context);
  }

  private void triggerAddCertTelemetry(Map telemetryMap, Map<String,Object> context) {
    ProjectLogger.log(
        "UserMergeActor:triggerAddCertTelemetry: generating telemetry event for add certificate");
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, String> rollUp = new HashMap<>();
    rollUp.put("l1", (String) telemetryMap.get(JsonKey.ROOT_ORG_ID));
    context.put(JsonKey.ROLLUP, rollUp);
    if(null != telemetryMap.get(JsonKey.OLD_ID)){
      TelemetryUtil.generateCorrelatedObject(
              (String) telemetryMap.get(JsonKey.OLD_ID), JsonKey.OLD_CERTIFICATE, null, correlatedObject);
    }
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) telemetryMap.get(JsonKey.CERT_ID), JsonKey.CERTIFICATE, JsonKey.CREATE, null);
    TelemetryUtil.generateCorrelatedObject(
        (String) telemetryMap.get(JsonKey.USER_ID), JsonKey.USER, null, correlatedObject);
    TelemetryUtil.generateCorrelatedObject(
        (String) telemetryMap.get(JsonKey.CERT_ID), JsonKey.CERTIFICATE, null, correlatedObject);
    telemetryMap.remove(JsonKey.ROOT_ORG_ID);
    TelemetryUtil.telemetryProcessingCall(telemetryMap, targetObject, correlatedObject, context);
  }
}
