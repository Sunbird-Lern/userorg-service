package org.sunbird.learner.organisation.service.impl;

import java.util.Map;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.request.RequestContext;
import org.sunbird.learner.organisation.dao.OrgDao;
import org.sunbird.learner.organisation.dao.impl.OrgDaoImpl;
import org.sunbird.learner.organisation.service.OrgService;

public class OrgServiceImpl implements OrgService {

  private LoggerUtil logger = new LoggerUtil(OrgServiceImpl.class);
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
  public Map<String, Object> esGetOrgByExternalId(
      String externalId, String provider, RequestContext context) {
    return orgDao.esGetOrgByExternalId(externalId, provider, context);
  }
}
