package org.sunbird.actor.user;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.kafka.InstructionEventGenerator;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.response.ResponseParams;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.UserRoleService;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserRoleServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.PropertiesCache;
import org.sunbird.util.user.UserUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.sunbird.validator.orgvalidator.BaseOrgRequestValidator.ERROR_CODE;

public class UserOwnershipTransferActor extends BaseActor {

    private final UserRoleService userRoleService = UserRoleServiceImpl.getInstance();
    private final UserService userService = UserServiceImpl.getInstance();
    private final OrgService orgService = OrgServiceImpl.getInstance();

    @Override
    public void onReceive(Request request) throws Throwable {
        handleOwnershipTransfer(request);
    }

    private void handleOwnershipTransfer(Request request) {
        validateOrganizationId(request.getRequest(), request.getRequestContext());
        validateUserDetails(request, request.getRequestContext());
        List<Map<String, Object>> objects = getObjectsFromRequest(request);
        if (!objects.isEmpty()) {
            objects.forEach(object -> sendInstructionEvent(request, object));
        } else {
            sendInstructionEvent(request, Collections.emptyMap());
        }
        Response response = sendResponse("Ownership transfer process is submitted successfully!");
        sender().tell(response, self());
    }

    private void validateOrganizationId(Map<String, Object> requestData, RequestContext requestContext) {
        if (!requestData.containsKey(JsonKey.ORGANISATION_ID) ||
                StringUtils.isBlank((String) requestData.get(JsonKey.ORGANISATION_ID))) {
            throwInvalidRequestDataException("Organization ID is mandatory in the request.");
        }
        String orgId = (String) requestData.get(JsonKey.ORGANISATION_ID);
        if (!organisationExists(orgId, requestContext)) {
            throwInvalidRequestDataException("Organization with ID " + orgId + " does not exist.");
        }
    }

    private boolean organisationExists(String orgId, RequestContext context) {
        try {
            Map<String, Object> organisation = orgService.getOrgById(orgId, context);
            return MapUtils.isNotEmpty(organisation);
        } catch (Exception ex) {
            return false;
        }
    }

    private void validateUserDetails(Request request, RequestContext requestContext) {
        validateAndProceed(request, JsonKey.ACTION_BY, requestContext);
        validateAndProceed(request, JsonKey.FROM_USER, requestContext);
        validateAndProceed(request, JsonKey.TO_USER, requestContext);
    }

    private void validateAndProceed(Request request, String key, RequestContext requestContext) {
        if (request.getRequest().containsKey(key)) {
            validateUser(request.getRequest().get(key), key, requestContext, request);
        } else {
            throwInvalidRequestDataException(key + " key is not present in the data.");
        }
    }

    private void validateUser(Object userNode, String userLabel, RequestContext requestContext,
                              Request request) {
        if (userNode instanceof Map) {
            Map<String, Object> user = (Map<String, Object>) userNode;
            String userId = StringUtils.trimToNull(Objects.toString(user.get(JsonKey.USER_ID), ""));
            if (StringUtils.isBlank(StringUtils.trimToNull(userId)) || !userExists(userId, requestContext)) {
                throwInvalidRequestDataException("given user id under " + userLabel + " is not present or blank");
            } else {
                if (userLabel.equals(JsonKey.ACTION_BY)) {
                    validateActionByUserRole(userId, request);
                } else {
                    validateAndFilterRoles(user, userLabel, request.getRequest());
                }
                addUserInfo(userId, user, userLabel, requestContext);
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
                List<?> rolesList = (List<?>) roles;
                if (rolesList.isEmpty()) {
                    throwInvalidRequestDataException("Roles are empty in " + userLabel + " details.");
                } else {
                    List<String> filteredRoles = filterRolesByOrganisationId((List<?>) roles,
                            (String) data.get(JsonKey.ORGANISATION_ID));
                    user.put(JsonKey.ROLES, filteredRoles);
                }
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

    private void throwDataTypeErrorException() {
        throw new ProjectCommonException(
                ResponseCode.dataTypeError,
                ProjectUtil.formatMessage(
                        ResponseCode.dataTypeError.getErrorMessage(), JsonKey.ROLES, JsonKey.LIST),
                ERROR_CODE);
    }

    private void addUserInfo(String userId, Map<String, Object> userRequestData, String userLabel, RequestContext context) {
        User user = userService.getUserById(userId, context);
        userRequestData.put(JsonKey.USERNAME, UserUtil.getDecryptedData(user.getUserName(), context));
        if (userLabel.equals(JsonKey.TO_USER)) {
            userRequestData.put(JsonKey.FIRST_NAME, user.getFirstName());
            userRequestData.put(JsonKey.LAST_NAME, user.getLastName());
        }
    }

    private boolean userExists(String userId, RequestContext context) {
        try {
            return userService.getUserById(userId, context) != null;
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
                PropertiesCache propertiesCache = PropertiesCache.getInstance();
                InstructionEventGenerator.pushInstructionEvent(propertiesCache.getProperty(JsonKey.USER_OWNERSHIP_TRANSFER_TOPIC), data);
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

        Map<String, Object> cData = new HashMap<>();
        cData.put(JsonKey.ID,request.getRequestContext().getReqId());
        cData.put(JsonKey.TYPE, TelemetryEnvKey.REQUEST_UPPER_CAMEL);

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> fromUserDetails = (Map<String, Object>) request.getRequest().get(JsonKey.FROM_USER);
        Map<String, Object> objectDetails = Map.of(JsonKey.ID, fromUserDetails.get(JsonKey.USER_ID), JsonKey.TYPE,
                JsonKey.USER);
        result.put("actor", actor);
        result.put(JsonKey.OBJECT, objectDetails);
        result.put(JsonKey.CDATA, cData);
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