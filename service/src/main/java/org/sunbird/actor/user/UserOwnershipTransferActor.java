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
import java.util.concurrent.CompletableFuture;

import static org.sunbird.validator.orgvalidator.BaseOrgRequestValidator.ERROR_CODE;

public class UserOwnershipTransferActor extends BaseActor {

    private final UserRoleService userRoleService = UserRoleServiceImpl.getInstance();
    private final UserService userService = UserServiceImpl.getInstance();

    @Override
    public void onReceive(Request request) throws Throwable {
        handleOwnershipTransfer(request);
    }

    private void handleOwnershipTransfer(Request request) {
        validateUserDetails(request.getRequest(), request.getRequestContext());
        String userId = (String) ((Map<String, Object>) request.getRequest().get(JsonKey.ACTION_BY))
                .get(JsonKey.USER_ID);
        validateActionByUserRole(userId, request);
        List<Map<String, Object>> objects = getObjectsFromRequest(request);
        if (!objects.isEmpty()) {
            objects.forEach(object -> sendInstructionEvent(request, object));
        } else {
            sendInstructionEvent(request, Collections.emptyMap());
        }
        Response response = sendResponse("Ownership transfer process is submitted successfully!");
        sender().tell(response, self());
    }

    private void validateUserDetails(Map<String, Object> data, RequestContext requestContext) {
        validateAndProceed(data, JsonKey.ACTION_BY, requestContext);
        validateAndProceed(data, JsonKey.FROM_USER, requestContext);
        validateAndProceed(data, JsonKey.TO_USER, requestContext);
    }

    private void validateAndProceed(Map<String, Object> data, String key, RequestContext requestContext) {
        if (data.containsKey(key)) {
            validateUser(data.get(key), key, requestContext, data);
        } else {
            throwInvalidRequestDataException(key + " key is not present in the data.");
        }
    }

    private void validateUser(Object userNode, String userLabel, RequestContext requestContext,
                              Map<String, Object> data) {
        if (userNode instanceof Map) {
            Map<String, Object> user = (Map<String, Object>) userNode;
            String userId = StringUtils.trimToNull(Objects.toString(user.get(JsonKey.USER_ID), ""));
            String userName = StringUtils.trimToNull(Objects.toString(user.get(JsonKey.USERNAME), ""));

            if (StringUtils.isBlank(StringUtils.trimToNull(userId)) ||
                    StringUtils.isBlank(StringUtils.trimToNull(userName))) {
                throwInvalidRequestDataException("User id / user name key is not present in the " + userLabel);
            }

            if (validUser(userId, requestContext)) {
                validateAndFilterRoles(user, userLabel, data);
            } else {
                throwClientErrorException();
            }
        }
    }

    private void validateAndFilterRoles(Map<String, Object> user, String userLabel, Map<String, Object> data) {
        if (!userLabel.equals(JsonKey.ACTION_BY) && !user.containsKey(JsonKey.ROLES)) {
            throwInvalidRequestDataException("Roles key is not present for " + userLabel);
        }

        if (user.containsKey(JsonKey.ROLES)) {
            Object roles = user.get(JsonKey.ROLES);
            if (roles instanceof List) {
                List<String> filteredRoles = filterRolesByOrganisationId((List<?>) roles,
                        (String) data.get(JsonKey.ORGANISATION_ID));
                if (filteredRoles.isEmpty()) {
                    throwInvalidRequestDataException("No roles match the specified organisationId in " + userLabel);
                }
                user.put(JsonKey.ROLES, filteredRoles);
            } else {
                throwDataTypeErrorException();
            }
        }
    }

    private List<String> filterRolesByOrganisationId(List<?> roles, String targetOrganisationId) {
        List<String> filteredRoles = new ArrayList<>();
        for (Object role : roles) {
            if (role instanceof Map) {
                Map<String, Object> roleMap = (Map<String, Object>) role;
                List<Map<String, Object>> scopeList = (List<Map<String, Object>>) roleMap.get("scope");
                if (scopeList != null && !scopeList.isEmpty()) {
                    for (Map<String, Object> scope : scopeList) {
                        String organisationId = (String) scope.get("organisationId");
                        if (targetOrganisationId.equals(organisationId)) {
                            filteredRoles.add((String) roleMap.get("role"));
                            break;
                        }
                    }
                }
            }
        }
        return filteredRoles;
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

    private void validateActionByUserRole(String userId, Request request) {
        List<Map<String, Object>> userRoles = userRoleService.getUserRoles(userId, request.getRequestContext());
        boolean hasOrgAdminRole = userRoles.stream().anyMatch(role -> JsonKey.ORG_ADMIN.equals(role.get(JsonKey.ROLE)));
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
        CompletableFuture.runAsync(() -> {
            try {
                InstructionEventGenerator.pushInstructionEvent(JsonKey.USER_TRANSFER_TOPIC, data);
            } catch (Exception e) {
                logger.error("Error pushing to instruction event", e);
            }
        });
    }

    private Map<String, Object> prepareEventData(Request request, Map<String, Object> object) {
        Map<String, Object> actor = Map.of("id", "ownership-transfer", "type", "System");
        Map<String, Object> edataBase = Map.of(
                JsonKey.ACTION, JsonKey.USER_OWNERSHIP_TRANSFER_ACTION,
                JsonKey.ORGANISATION_ID, request.getRequest().get(JsonKey.ORGANISATION_ID),
                JsonKey.CONTEXT, request.getRequest().get(JsonKey.CONTEXT),
                JsonKey.ACTION_BY, request.getRequest().get(JsonKey.ACTION_BY),
                JsonKey.FROM_USER_PROFILE, request.getRequest().get(JsonKey.FROM_USER),
                JsonKey.TO_USER_PROFILE, request.getRequest().get(JsonKey.TO_USER)
        );
        Map<String, Object> edata = new HashMap<>(edataBase);
        Map<String, Object> assetInformation = new HashMap<>(object);
        edata.put(JsonKey.ASSET_INFORMATION, assetInformation);

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> fromUserDetails = (Map<String, Object>) request.getRequest().get(JsonKey.FROM_USER);
        Map<String, Object> objectDetails = Map.of(JsonKey.ID, fromUserDetails.get(JsonKey.USER_ID), JsonKey.TYPE,
                JsonKey.USER);
        result.put("actor", actor);
        result.put(JsonKey.OBJECT, objectDetails);
        result.put(JsonKey.EDATA, edata);
        return result;
    }

    Response sendResponse(String statusMessage) {
        Response response = new Response();
        response.setId("api.user.ownership.transfer");
        response.setVer("1.0");
        response.setTs(String.valueOf(Calendar.getInstance().getTime().getTime()));
        ResponseParams params = new ResponseParams();
        params.setResmsgid(UUID.randomUUID().toString());
        params.setStatus(String.valueOf(ResponseParams.StatusType.SUCCESSFUL));
        response.setParams(params);
        response.setResponseCode(ResponseCode.OK);
        Map<String, Object> result = Map.of(JsonKey.STATUS, statusMessage);
        response.putAll(result);
        return response;
    }
}
