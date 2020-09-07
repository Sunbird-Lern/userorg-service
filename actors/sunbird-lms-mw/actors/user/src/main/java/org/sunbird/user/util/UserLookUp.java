package org.sunbird.user.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;

public class UserLookUp {
    private static LoggerUtil logger = new LoggerUtil(UserLookUp.class);
    private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private static EncryptionService encryptionService =
            org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
                    null);
    private static Util.DbInfo userLookUp = Util.dbInfoMap.get(JsonKey.USER_LOOK_UP);

    public Response add(String type, String value, String userId, boolean encrypt, RequestContext context) {
        if (encrypt) {
            try {
                value = encryptionService.encryptData(value, context);
            } catch (Exception e) {
                logger.info(context, "Exception occurred while encrypting email/phone " + e);
            }
        }
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put(JsonKey.TYPE, type);
        reqMap.put(JsonKey.VALUE, value);
        reqMap.put(JsonKey.USER_ID, userId);
        Response result =
                cassandraOperation.insertRecord(
                        userLookUp.getKeySpace(), userLookUp.getTableName(), reqMap, context);
        return result;
    }

    public Response insertRecords(List<Map<String, Object>> reqMap, RequestContext context) {
        Response result =
                cassandraOperation.batchInsert(userLookUp.getKeySpace(), userLookUp.getTableName(), reqMap, context);
        return result;
    }

    public Response insertIntoLookUp(Map<String, Object> reqMap, RequestContext context) {
        Response result =
                cassandraOperation.insertRecord(
                        userLookUp.getKeySpace(), userLookUp.getTableName(), reqMap, context);
        return result;
    }

    public Response insertExternalIdIntoUserLookup(List<Map<String, Object>> reqMap, String userId, RequestContext context) {
        Response result = null;
        if (CollectionUtils.isNotEmpty(reqMap)) {
            Map<String, Object> lookUp = new HashMap<>();
            Map<String, Object> externalId =
                    reqMap
                            .stream()
                            .filter(
                                    x -> ((String) x.get(JsonKey.ID_TYPE)).equals((String) x.get(JsonKey.PROVIDER)))
                            .findFirst()
                            .orElse(null);
            if (org.apache.commons.collections.MapUtils.isNotEmpty(externalId)) {
                lookUp.put(JsonKey.TYPE, JsonKey.EXTERNAL_ID_LOWER_CASE);
                lookUp.put(JsonKey.USER_ID, userId);
                // provider is the orgId, not the channel
                lookUp.put(
                        JsonKey.VALUE, externalId.get(JsonKey.ID) + "@" + externalId.get(JsonKey.PROVIDER));
            }
            result =
                    cassandraOperation.insertRecord(
                            userLookUp.getKeySpace(), userLookUp.getTableName(), lookUp, context);
        }
        return result;
    }

    public List<Map<String, Object>> getRecordByType(String type, String value, boolean encrypt, RequestContext context) {
        if (encrypt) {
            try {
                value = encryptionService.encryptData(value, context);
            } catch (Exception e) {
                logger.info(context, "Exception occurred while encrypting email/phone " + e);
            }
        }
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put(JsonKey.TYPE, type);
        reqMap.put(JsonKey.VALUE, value);
        Response response =
                cassandraOperation.getRecordsByCompositeKey(
                        userLookUp.getKeySpace(), userLookUp.getTableName(), reqMap, context);
        List<Map<String, Object>> userMapList =
                (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
        return userMapList;
    }

    public List<Map<String, Object>> getEmailByType(String email, RequestContext context) {
        String emailSetting = DataCacheHandler.getConfigSettings().get(JsonKey.EMAIL_UNIQUE);
        List<Map<String, Object>> userMapList = null;
        if (StringUtils.isNotBlank(emailSetting) && Boolean.parseBoolean(emailSetting)) {
            userMapList = getRecordByType(JsonKey.EMAIL, email, true, context);
        }
        return userMapList;
    }

    public void checkEmailUniqueness(String email, RequestContext context) {
        // Get email configuration if not found , by default email will be unique across
        // the application
        if (StringUtils.isNotBlank(email)) {
            List<Map<String, Object>> userMapList = getEmailByType(email, context);
            if (CollectionUtils.isNotEmpty(userMapList)) {
                ProjectCommonException.throwClientErrorException(ResponseCode.emailAlreadyExistError, null);
            }
        }
    }

    public void checkEmailUniqueness(User user, String opType, RequestContext context) {
        String email = user.getEmail();
        if (StringUtils.isNotBlank(email)) {
            List<Map<String, Object>> userMapList = getEmailByType(email, context);
            if (!userMapList.isEmpty()) {
                if (opType.equalsIgnoreCase(JsonKey.CREATE)) {
                    ProjectCommonException.throwClientErrorException(ResponseCode.emailInUse, null);
                } else {
                    Map<String, Object> userMap = userMapList.get(0);
                    if (!(((String) userMap.get(JsonKey.ID)).equalsIgnoreCase(user.getId()))) {
                        ProjectCommonException.throwClientErrorException(ResponseCode.emailInUse, null);
                    }
                }
            }
        }
    }

    public List<Map<String, Object>> getPhoneByType(String phone, RequestContext context) {
        String phoneSetting = DataCacheHandler.getConfigSettings().get(JsonKey.PHONE_UNIQUE);
        List<Map<String, Object>> userMapList = null;
        if (StringUtils.isNotBlank(phoneSetting) && Boolean.parseBoolean(phoneSetting)) {
            userMapList = getRecordByType(JsonKey.PHONE, phone, true, context);
        }
        return userMapList;
    }

    public void checkPhoneUniqueness(String phone, RequestContext context) {
        // Get Phone configuration if not found , by default phone will be unique across
        // the application
        if (StringUtils.isNotBlank(phone)) {
            List<Map<String, Object>> userMapList = getPhoneByType(phone, context);
            if (CollectionUtils.isNotEmpty(userMapList)) {
                ProjectCommonException.throwClientErrorException(ResponseCode.PhoneNumberInUse, null);
            }
        }
    }

    public void checkPhoneUniqueness(User user, String opType, RequestContext context) {
        // Get Phone configuration if not found , by default phone will be unique across
        // the application
        String phone = user.getPhone();
        if (StringUtils.isNotBlank(phone)) {
            List<Map<String, Object>> userMapList = getPhoneByType(phone, context);
            if (!userMapList.isEmpty()) {
                if (opType.equalsIgnoreCase(JsonKey.CREATE)) {
                    ProjectCommonException.throwClientErrorException(ResponseCode.PhoneNumberInUse, null);
                } else {
                    Map<String, Object> userMap = userMapList.get(0);
                    if (!(((String) userMap.get(JsonKey.ID)).equalsIgnoreCase(user.getId()))) {
                        ProjectCommonException.throwClientErrorException(ResponseCode.PhoneNumberInUse, null);
                    }
                }
            }
        }
    }

    public boolean checkUsernameUniqueness(String username, boolean isEncrypted, RequestContext context) {
        List<Map<String, Object>> userMapList =
                getRecordByType(JsonKey.USER_NAME_LOWER_CASE, username, !isEncrypted, context);
        if (CollectionUtils.isNotEmpty(userMapList)) {
            return false;
        }
        return true;
    }
}
