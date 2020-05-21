package org.sunbird.user.actors;

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
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.request.Request;
import org.sunbird.user.dao.AddressDao;
import org.sunbird.user.dao.impl.AddressDaoImpl;

@ActorConfig(
  tasks = {"insertUserAddress", "updateUserAddress"},
  asyncTasks = {"insertUserAddress", "updateUserAddress"}
)
public class AddressManagementActor extends BaseActor {

  private EncryptionService encryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);
  private AddressDao addressDao = AddressDaoImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    switch (operation) {
      case "insertUserAddress":
        insertAddress(request);
        break;

      case "updateUserAddress":
        updateAddress(request);
        break;

      default:
        onReceiveUnsupportedOperation("AddressManagementActor");
    }
  }

  @SuppressWarnings("unchecked")
  private void insertAddress(Request request) {
    Map<String, Object> requestMap = request.getRequest();
    List<Map<String, Object>> addressList =
        (List<Map<String, Object>>) requestMap.get(JsonKey.ADDRESS);
    Response response = new Response();
    List<String> errMsgs = new ArrayList<>();
    List<Map<String, Object>> responseAddressList = new ArrayList<>();
    try {
      String encUserId = encryptionService.encryptData((String) requestMap.get(JsonKey.ID));
      String encCreatedById =
          encryptionService.encryptData((String) requestMap.get(JsonKey.CREATED_BY));
      for (int i = 0; i < addressList.size(); i++) {
        try {
          Map<String, Object> address = addressList.get(i);
          responseAddressList.add(createAddress(encUserId, encCreatedById, address));
        } catch (ProjectCommonException e) {
          errMsgs.add(e.getMessage());
          ProjectLogger.log(
              "AddressManagementActor:insertAddress: Exception occurred with error message = "
                  + e.getMessage(),
              e);
        } catch (Exception e) {
          errMsgs.add("Error occurred while inserting address details.");
          ProjectLogger.log(
              "AddressManagementActor:insertAddress: Generic exception occurred with error message = "
                  + e.getMessage(),
              e);
        }
      }
    } catch (Exception e) {
      errMsgs.add(e.getMessage());
      ProjectLogger.log(e.getMessage(), e);
    }
    response.put(JsonKey.ADDRESS, responseAddressList);
    response.put(JsonKey.KEY, JsonKey.ADDRESS);
    if (CollectionUtils.isNotEmpty(errMsgs)) {
      response.put(JsonKey.ERROR_MSG, errMsgs);
    } else {
      response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    }
    sender().tell(response, self());
  }

  @SuppressWarnings("unchecked")
  private void updateAddress(Request request) {
    Map<String, Object> requestMap = request.getRequest();
    List<Map<String, Object>> addressList =
        (List<Map<String, Object>>) requestMap.get(JsonKey.ADDRESS);
    Response response = new Response();
    List<String> errMsgs = new ArrayList<>();
    List<Map<String, Object>> responseAddressList = new ArrayList<>();
    try {
      String encUserId = encryptionService.encryptData((String) requestMap.get(JsonKey.ID));
      String encCreatedById =
          encryptionService.encryptData((String) requestMap.get(JsonKey.CREATED_BY));
      for (int i = 0; i < addressList.size(); i++) {
        try {
          Map<String, Object> address = addressList.get(i);
          if (BooleanUtils.isTrue((boolean) address.get(JsonKey.IS_DELETED))
              && !StringUtils.isBlank((String) address.get(JsonKey.ID))) {
            addressDao.deleteAddress((String) address.get(JsonKey.ID));
            continue;
          }
          if (!address.containsKey(JsonKey.ID)) {
            responseAddressList.add(createAddress(encUserId, encCreatedById, address));
          } else {
            address.put(JsonKey.UPDATED_BY, encCreatedById);
            address.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
            address.remove(JsonKey.USER_ID);
            addressDao.updateAddress(address);
            responseAddressList.add(address);
          }
        } catch (ProjectCommonException e) {
          errMsgs.add(e.getMessage());
          ProjectLogger.log(
              "AddressManagementActor:updateAddress: Exception occurred with error message = "
                  + e.getMessage(),
              e);
        } catch (Exception e) {
          errMsgs.add("Error occurred while updating address details.");
          ProjectLogger.log(
              "AddressManagementActor:updateAddress: Generic exception occurred with error message = "
                  + e.getMessage(),
              e);
        }
      }
    } catch (Exception e) {
      errMsgs.add(e.getMessage());
      ProjectLogger.log(e.getMessage(), e);
    }
    response.put(JsonKey.ADDRESS, responseAddressList);
    if (CollectionUtils.isNotEmpty(errMsgs)) {
      response.put(JsonKey.KEY, JsonKey.ADDRESS);
      response.put(JsonKey.ERROR_MSG, errMsgs);
    } else {
      response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    }
    sender().tell(response, self());
  }

  private Map<String, Object> createAddress(
      String encUserId, String encCreatedById, Map<String, Object> address) {
    address.put(JsonKey.CREATED_BY, encCreatedById);
    address.put(JsonKey.USER_ID, encUserId);
    address.put(JsonKey.ID, ProjectUtil.getUniqueIdFromTimestamp(1));
    address.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
    addressDao.createAddress(address);
    return address;
  }
}
