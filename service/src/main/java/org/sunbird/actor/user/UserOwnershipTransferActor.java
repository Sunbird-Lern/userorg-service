package org.sunbird.actor.user;

import org.apache.commons.lang.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.kafka.InstructionEventGenerator;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.response.ResponseParams;
import org.sunbird.service.user.UserRoleService;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserRoleServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.util.ProjectUtil;

import java.text.MessageFormat;
import java.util.*;

import static org.sunbird.validator.orgvalidator.BaseOrgRequestValidator.ERROR_CODE;

public class UserOwnershipTransferActor extends BaseActor {

    private static final String USER_TRANSFER_TOPIC = "user_transfer_topic";
    private final UserRoleService userRoleService = UserRoleServiceImpl.getInstance();
    private final UserService userService = UserServiceImpl.getInstance();

    @Override
    public void onReceive(Request request) throws Throwable {
        handleOwnershipTransfer(request);
    }

    private void handleOwnershipTransfer(Request request) {
        validateUserDetails(request.getRequest(), request.getRequestContext());
        String userId = (String) ((Map<String, Object>) request.getRequest().get("actionBy")).get("userId");
        validateUserRole(userId, request);
        List<Map<String, Object>> objects = getObjectsFromRequest(request);
        objects.forEach(object -> sendInstructionEvent(request, object));
        sendResponse("Ownership transfer process is submitted successfully!");
    }

    private void validateUserDetails(Map<String, Object> data, RequestContext requestContext) {
        validateAndProceed(data, "actionBy", requestContext);
        validateAndProceed(data, "fromUser", requestContext);
        validateAndProceed(data, "toUser", requestContext);
    }

    private void validateAndProceed(Map<String, Object> data, String key, RequestContext requestContext) {
        if (data.containsKey(key)) {
            validateUser(data.get(key), key, requestContext);
        } else {
            throwInvalidRequestDataException(key + " key is not present in the data.");
        }
    }

    private void validateUser(Object userNode, String userLabel, RequestContext requestContext) {
        if (userNode instanceof Map) {
            Map<String, Object> user = (Map<String, Object>) userNode;
            String userId = StringUtils.trimToNull(Objects.toString(user.get("userId"), ""));
            String userName = StringUtils.trimToNull(Objects.toString(user.get("userName"), ""));

            if (StringUtils.isBlank(StringUtils.trimToNull(userId)) ||
                    StringUtils.isBlank(StringUtils.trimToNull(userName))) {
                throwInvalidRequestDataException("User id / user name key is not present in the " + userLabel);
            }

            if (validUser(userId, requestContext)) {
                validateRoles(user, userLabel);
            } else {
                throwClientErrorException();
            }
        }
    }

    private void validateRoles(Map<String, Object> user, String userLabel) {
        if (!userLabel.equals("actionBy") && !user.containsKey(JsonKey.ROLES)) {
            throwInvalidRequestDataException("Roles key is not present for " + userLabel);
        }
        if (user.containsKey(JsonKey.ROLES)) {
            Object roles = user.get(JsonKey.ROLES);
            if (roles instanceof List) {
                List<?> rolesList = (List<?>) roles;
                if (rolesList.isEmpty()) {
                    throwInvalidRequestDataException("Roles are empty in " + userLabel + " details.");
                }
            } else {
                throwDataTypeErrorException();
            }
        }
    }

    private void throwInvalidRequestDataException(String message) {
        throw new ProjectCommonException(
                ResponseCode.invalidRequestData,
                message,
                ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    private void throwClientErrorException() {
        ProjectCommonException.throwClientErrorException(
                ResponseCode.invalidParameter,
                MessageFormat.format(ResponseCode.invalidParameter.getErrorMessage(), JsonKey.USER_ID));
    }

    private void throwDataTypeErrorException() {
        throw new ProjectCommonException(
                ResponseCode.dataTypeError,
                ProjectUtil.formatMessage(
                        ResponseCode.dataTypeError.getErrorMessage(), JsonKey.ROLES, JsonKey.LIST),
                ERROR_CODE);
    }

    private boolean validUser(String userId, RequestContext context) {
        return StringUtils.isNotBlank(userId) && userExists(userId, context);
    }

    private boolean userExists(String userId, RequestContext context) {
        try {
            userService.getUserById(userId, context);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void validateUserRole(String userId, Request request) {
        List<Map<String, Object>> userRoles = userRoleService.getUserRoles(userId, request.getRequestContext());
        boolean hasOrgAdminRole = userRoles.stream().anyMatch(role -> JsonKey.ORG_ADMIN.equals(role.get("role")));
        if (!hasOrgAdminRole) {
            throw new ProjectCommonException(
                    ResponseCode.cannotTransferOwnership,
                    ResponseCode.cannotTransferOwnership.getErrorMessage(),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
        }
    }

    private List<Map<String, Object>> getObjectsFromRequest(Request request) {
        return Optional.ofNullable((List<Map<String, Object>>) request.getRequest().get("objects"))
                .orElse(Collections.emptyList());
    }

    private void sendInstructionEvent(Request request, Map<String, Object> object) {
        Map<String, Object> data = prepareEventData(request, object);
        try {
            InstructionEventGenerator.pushInstructionEvent(USER_TRANSFER_TOPIC, data);
        } catch (Exception e) {
            logger.error("Error pushing to instruction event", e);
        }
    }

    private Map<String, Object> prepareEventData(Request request, Map<String, Object> object) {
        Map<String, Object> actor = Map.of("id", "ownership-transfer", "type", "System");
        Map<String, Object> context = Map.of(
                "channel", "01309282781705830427",
                "pdata", Map.of("id", "org.sunbird.platform", "ver", "1.0"),
                "env", "dev"
        );
        Map<String, Object> edataBase = Map.of(
                "action", "ownership-transfer",
                "organisationId", request.getRequest().get("organisationId"),
                "context", request.getRequest().get("context"),
                "actionBy", request.getRequest().get("actionBy"),
                "fromUserProfile", request.getRequest().get("fromUser"),
                "toUserProfile", request.getRequest().get("toUser")
        );
        Map<String, Object> edata = new HashMap<>(edataBase);
        Map<String, Object> assetInformation = new HashMap<>(object);
        edata.put("assetInformation", assetInformation);

        Map<String, Object> result = new HashMap<>();
        result.put("eid", JsonKey.BE_JOB_REQUEST);
        result.put("ets", System.currentTimeMillis());
        result.put("mid", "LP." + System.currentTimeMillis() + "." + UUID.randomUUID());
        result.put("actor", actor);
        result.put("context", context);

        // Use 'fromUser' details for 'object'
        Map<String, Object> fromUserDetails = (Map<String, Object>) request.getRequest().get("fromUser");
        Map<String, Object> objectDetails = Map.of("id", fromUserDetails.get("userId"), "type", JsonKey.USER);
        result.put("object", objectDetails);
        result.put("edata", edata);

        return result;
    }

    private void sendResponse(String statusMessage) {
        Response response = new Response();
        response.setId("api.user.ownership.transfer");
        response.setVer("1.0");
        response.setTs(String.valueOf(Calendar.getInstance().getTime().getTime()));
        ResponseParams params = new ResponseParams();
        params.setResmsgid(UUID.randomUUID().toString());
        params.setStatus(String.valueOf(ResponseParams.StatusType.SUCCESSFUL));
        response.setParams(params);
        response.setResponseCode(ResponseCode.OK);
        Map<String, Object> result = Map.of("status", statusMessage);
        response.putAll(result);
        sender().tell(response, self());
    }
}
