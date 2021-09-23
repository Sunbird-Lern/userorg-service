package org.sunbird.service.user;

import java.util.List;
import java.util.Map;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface TenantMigrationService {
    void validateChannelAndGetRootOrgId(Request request);
    void validateUserCustodianOrgId(String rootOrgId);
    String validateOrgExternalIdOrOrgIdAndGetOrgId(
            Map<String, Object> migrateReq, RequestContext context);
    void deactivateUserFromKC(String userId, RequestContext context);
    Response migrateUser(Map<String, Object> migrateUserDetails, RequestContext context);
    Response updateUserOrg(Request request, List<Map<String, Object>> userOrgList);
}