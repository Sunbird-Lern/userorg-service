package org.sunbird.service.user;

import java.util.Map;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

public interface TenantMigrationService {
  public Response migrateUser(Request request, Map<String, Object> userDetails);
}
