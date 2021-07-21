package org.sunbird.learner.organisation.service.impl;

import java.util.Map;
import org.sunbird.learner.organisation.dao.OrgDao;
import org.sunbird.learner.organisation.dao.impl.OrgDaoImpl;
import org.sunbird.learner.organisation.service.OrgService;
import org.sunbird.request.RequestContext;

public class OrgServiceImpl implements OrgService {

  private OrgDao orgDao = OrgDaoImpl.getInstance();
  private static OrgService orgService = null;

  public static OrgService getInstance() {
    if (orgService == null) {
      orgService = new OrgServiceImpl();
    }
    return orgService;
  }

  @Override
  public Map<String, Object> getOrgById(String orgId, RequestContext context) {
    return orgDao.getOrgById(orgId, context);
  }

  @Override
  public Map<String, Object> getOrgByExternalIdAndProvider(
      String externalId, String provider, RequestContext context) {
    return orgDao.getOrgByExternalId(externalId, provider, context);
  }
}
