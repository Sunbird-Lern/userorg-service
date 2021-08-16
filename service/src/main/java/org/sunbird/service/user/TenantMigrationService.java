package org.sunbird.service.user;

import org.sunbird.request.Request;

public interface TenantMigrationService {
  public void migrateUser(Request request, boolean notify);
}
