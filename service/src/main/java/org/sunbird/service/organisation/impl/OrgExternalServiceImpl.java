package org.sunbird.service.organisation.impl;

import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.dao.organisation.OrgDao;
import org.sunbird.dao.organisation.OrgExternalDao;
import org.sunbird.dao.organisation.impl.OrgDaoImpl;
import org.sunbird.dao.organisation.impl.OrgExternalDaoImpl;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgExternalService;

public class OrgExternalServiceImpl implements OrgExternalService {
  private final OrgExternalDao orgExtDao = new OrgExternalDaoImpl();
  private final OrgDao orgDao = OrgDaoImpl.getInstance();
  private static OrgExternalService orgExternalService;

  public static OrgExternalService getInstance() {
    if (orgExternalService == null) {
      orgExternalService = new OrgExternalServiceImpl();
    }
    return orgExternalService;
  }

  @Override
  public String getOrgIdFromOrgExternalIdAndProvider(
      String externalId, String provider, RequestContext context) {
    return orgExtDao.getOrgIdFromOrgExternalIdAndProvider(externalId, provider, context);
  }

  @Override
  public Map<String, Object> getOrgByOrgExternalIdAndProvider(
      String externalId, String provider, RequestContext context) {
    String orgId = getOrgIdFromOrgExternalIdAndProvider(externalId, provider, context);
    if (StringUtils.isNotBlank(orgId)) {
      return orgDao.getOrgById(orgId, context);
    }
    return Collections.emptyMap();
  }

  @Override
  public Response addOrgExtId(Map<String, Object> orgExtMap, RequestContext context) {
    return orgExtDao.addOrgExtId(orgExtMap, context);
  }

  @Override
  public void deleteOrgExtId(Map<String, String> orgExtMap, RequestContext context) {
    orgExtDao.deleteOrgExtId(orgExtMap, context);
  }
}
