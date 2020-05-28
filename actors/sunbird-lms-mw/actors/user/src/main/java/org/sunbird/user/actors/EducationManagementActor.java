package org.sunbird.user.actors;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.user.dao.AddressDao;
import org.sunbird.user.dao.EducationDao;
import org.sunbird.user.dao.impl.AddressDaoImpl;
import org.sunbird.user.dao.impl.EducationDaoImpl;

@ActorConfig(
  tasks = {"insertUserEducation", "updateUserEducation"},
  asyncTasks = {"insertUserEducation", "updateUserEducation"}
)
public class EducationManagementActor extends BaseActor {

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    switch (operation) {
      case "insertUserEducation":
        insertEducation(request);
        break;

      case "updateUserEducation":
        updateEducation(request);
        break;

      default:
        onReceiveUnsupportedOperation("EducationManagementActor");
    }
  }

  @SuppressWarnings("unchecked")
  private void insertEducation(Request request) {
    Map<String, Object> requestMap = request.getRequest();
    List<Map<String, Object>> reqList =
        (List<Map<String, Object>>) requestMap.get(JsonKey.EDUCATION);
    Response response = new Response();
    List<String> errMsgs = new ArrayList<>();
    List<Map<String, Object>> responseEducationList = new ArrayList<>();
    try {
      for (int i = 0; i < reqList.size(); i++) {
        try {
          Map<String, Object> educationDetailsMap = reqList.get(i);
          String createdBy = (String) requestMap.get(JsonKey.ID);
          Response addrResponse = null;
          educationDetailsMap.put(JsonKey.ID, ProjectUtil.getUniqueIdFromTimestamp(i));
          if (educationDetailsMap.containsKey(JsonKey.ADDRESS)) {
            addrResponse = upsertEducationAddressDetails(educationDetailsMap, createdBy);
          }
          responseEducationList.add(
              insertEducationDetails(requestMap, educationDetailsMap, addrResponse, createdBy));
        } catch (ProjectCommonException e) {
          errMsgs.add(e.getMessage());
          ProjectLogger.log(
              "EducationManagementActor:insertEducation: Exception occurred with error message = "
                  + e.getMessage(),
              e);
        } catch (Exception e) {
          errMsgs.add("Error occurred while inserting education details.");
          ProjectLogger.log(
              "EducationManagementActor:insertEducation: Generic exception occurred with error message = "
                  + e.getMessage(),
              e);
        }
      }
    } catch (Exception e) {
      errMsgs.add(e.getMessage());
      ProjectLogger.log(e.getMessage(), e);
    }
    response.put(JsonKey.EDUCATION, responseEducationList);
    response.put(JsonKey.KEY, JsonKey.EDUCATION);
    if (CollectionUtils.isNotEmpty(errMsgs)) {
      response.put(JsonKey.ERROR_MSG, errMsgs);
    } else {
      response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    }
    sender().tell(response, self());
  }

  @SuppressWarnings("unchecked")
  private void updateEducation(Request request) {
    Map<String, Object> requestMap = request.getRequest();
    List<Map<String, Object>> reqList =
        (List<Map<String, Object>>) requestMap.get(JsonKey.EDUCATION);
    Response response = new Response();
    List<String> errMsgs = new ArrayList<>();
    List<Map<String, Object>> responseEducationList = new ArrayList<>();
    try {
      for (int i = 0; i < reqList.size(); i++) {
        try {
          Map<String, Object> educationDetailsMap = reqList.get(i);
          String createdBy = (String) requestMap.get(JsonKey.ID);
          Response addrResponse = null;
          if (BooleanUtils.isTrue((boolean) educationDetailsMap.get(JsonKey.IS_DELETED))
              && !StringUtils.isBlank((String) educationDetailsMap.get(JsonKey.ID))) {
            deleteEducationDetails(educationDetailsMap);
            continue;
          }
          if (educationDetailsMap.containsKey(JsonKey.ADDRESS)) {
            addrResponse = upsertEducationAddressDetails(educationDetailsMap, createdBy);
          }
          if (StringUtils.isBlank((String) educationDetailsMap.get(JsonKey.ID))) {
            educationDetailsMap.put(JsonKey.ID, ProjectUtil.getUniqueIdFromTimestamp(i));
            responseEducationList.add(
                insertEducationDetails(requestMap, educationDetailsMap, addrResponse, createdBy));
          } else {
            responseEducationList.add(
                updateEducationDetails(educationDetailsMap, addrResponse, createdBy));
          }
        } catch (ProjectCommonException e) {
          errMsgs.add(e.getMessage());
          ProjectLogger.log(
              "EducationManagementActor:updateEducation: Exception occurred with error message = "
                  + e.getMessage(),
              e);
        } catch (Exception e) {
          errMsgs.add("Error occurred while updating education details.");
          ProjectLogger.log(
              "EducationManagementActor:updateEducation: Generic exception occurred with error message = "
                  + e.getMessage(),
              e);
        }
      }
    } catch (Exception e) {
      errMsgs.add(e.getMessage());
      ProjectLogger.log(e.getMessage(), e);
    }
    response.put(JsonKey.EDUCATION, responseEducationList);
    if (CollectionUtils.isNotEmpty(errMsgs)) {
      response.put(JsonKey.KEY, JsonKey.EDUCATION);
      response.put(JsonKey.ERROR_MSG, errMsgs);
    } else {
      response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    }
    sender().tell(response, self());
  }

  private Map<String, Object> updateEducationDetails(
      Map<String, Object> educationDetailsMap, Response addrResponse, String createdBy) {
    if (null != addrResponse
        && ((String) addrResponse.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      educationDetailsMap.put(JsonKey.ADDRESS_ID, addrResponse.get(JsonKey.ADDRESS_ID));
      educationDetailsMap.remove(JsonKey.ADDRESS);
    }
    validateYearOfPassingAndPercentageDetails(educationDetailsMap);
    educationDetailsMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    educationDetailsMap.put(JsonKey.UPDATED_BY, createdBy);
    educationDetailsMap.remove(JsonKey.USER_ID);
    getEducationDao().upsertEducation(educationDetailsMap);
    educationDetailsMap.put(JsonKey.ADDRESS, addrResponse.get(JsonKey.ADDRESS));
    return educationDetailsMap;
  }

  @SuppressWarnings("unchecked")
  private void deleteEducationDetails(Map<String, Object> educationDetailsMap) {
    String addrsId = null;
    if (educationDetailsMap.containsKey(JsonKey.ADDRESS)
        && null != educationDetailsMap.get(JsonKey.ADDRESS)) {
      addrsId =
          (String) ((Map<String, Object>) educationDetailsMap.get(JsonKey.ADDRESS)).get(JsonKey.ID);
    } else {
      addrsId = getAddressId((String) educationDetailsMap.get(JsonKey.ID));
    }
    if (null != addrsId) {
      // delete eductaion address
      getAddressDao().deleteAddress(addrsId);
    }
    getEducationDao().deleteEducation((String) educationDetailsMap.get(JsonKey.ID));
  }

  @SuppressWarnings("unchecked")
  private String getAddressId(String id) {
    String addressId = null;
    // eduDbInfo
    try {
      Response res = getEducationDao().getPropertiesValueById(JsonKey.ADDRESS_ID, id);
      if (!((List<Map<String, Object>>) res.get(JsonKey.RESPONSE)).isEmpty()) {
        addressId =
            (String)
                (((List<Map<String, Object>>) res.get(JsonKey.RESPONSE)).get(0))
                    .get(JsonKey.ADDRESS_ID);
      }
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
    }
    return addressId;
  }

  private Map<String, Object> insertEducationDetails(
      Map<String, Object> requestMap,
      Map<String, Object> educationDetailsMap,
      Response addrResponse,
      String createdBy) {

    if (null != addrResponse
        && ((String) addrResponse.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      educationDetailsMap.put(JsonKey.ADDRESS_ID, addrResponse.get(JsonKey.ADDRESS_ID));
      educationDetailsMap.remove(JsonKey.ADDRESS);
    }
    validateYearOfPassingAndPercentageDetails(educationDetailsMap);
    educationDetailsMap.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
    educationDetailsMap.put(JsonKey.CREATED_BY, createdBy);
    educationDetailsMap.put(JsonKey.USER_ID, requestMap.get(JsonKey.ID));
    getEducationDao().createEducation(educationDetailsMap);
    if(null!=addrResponse) {
      educationDetailsMap.put(JsonKey.ADDRESS, addrResponse.get(JsonKey.ADDRESS));
    }
    return educationDetailsMap;
  }

  private void validateYearOfPassingAndPercentageDetails(Map<String, Object> educationDetailsMap) {
    try {
      if (null != educationDetailsMap.get(JsonKey.YEAR_OF_PASSING)) {
        educationDetailsMap.put(
            JsonKey.YEAR_OF_PASSING,
            ((BigInteger) educationDetailsMap.get(JsonKey.YEAR_OF_PASSING)).intValue());
      } else {
        educationDetailsMap.put(JsonKey.YEAR_OF_PASSING, 0);
      }
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
      educationDetailsMap.put(JsonKey.YEAR_OF_PASSING, 0);
    }
    try {
      if (null != educationDetailsMap.get(JsonKey.PERCENTAGE)) {
        educationDetailsMap.put(
            JsonKey.PERCENTAGE,
            Double.parseDouble(String.valueOf(educationDetailsMap.get(JsonKey.PERCENTAGE))));
      } else {
        educationDetailsMap.put(JsonKey.PERCENTAGE, Double.parseDouble(String.valueOf("0")));
      }
    } catch (Exception ex) {
      educationDetailsMap.put(JsonKey.PERCENTAGE, Double.parseDouble(String.valueOf("0")));
      ProjectLogger.log(ex.getMessage(), ex);
    }
  }

  @SuppressWarnings("unchecked")
  private Response upsertEducationAddressDetails(
      Map<String, Object> educationDetailsMap, String createdBy) {
    Response addrResponse = null;
    Map<String, Object> address = (Map<String, Object>) educationDetailsMap.get(JsonKey.ADDRESS);
    address.remove(JsonKey.IS_DELETED);
    String addrId = null;
    if (!address.containsKey(JsonKey.ID)) {
      addrId = ProjectUtil.getUniqueIdFromTimestamp(3);
      address.put(JsonKey.ID, addrId);
      address.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
      address.put(JsonKey.CREATED_BY, createdBy);
    } else {
      addrId = (String) address.get(JsonKey.ID);
      address.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
      address.put(JsonKey.UPDATED_BY, createdBy);
      address.remove(JsonKey.USER_ID);
    }
    addrResponse = getAddressDao().upsertAddress(address);
    addrResponse.put(JsonKey.ADDRESS_ID, addrId);
    addrResponse.put(JsonKey.ADDRESS, address);
    return addrResponse;
  }

  private EducationDao getEducationDao(){
    return EducationDaoImpl.getInstance();
  }
  private AddressDao getAddressDao(){
    return AddressDaoImpl.getInstance();
  }
}
