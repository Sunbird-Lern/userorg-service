package org.sunbird.service.organisation.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.dao.organisation.OrgDao;
import org.sunbird.dao.organisation.impl.OrgDaoImpl;
import org.sunbird.dto.SearchDTO;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgExternalService;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.PropertiesCache;
import org.sunbird.util.Util;
import scala.concurrent.Future;

public class OrgServiceImpl implements OrgService {

  public LoggerUtil logger = new LoggerUtil(this.getClass());
  private ObjectMapper mapper = new ObjectMapper();
  private OrgDao orgDao = OrgDaoImpl.getInstance();
  private static OrgService orgService;
  private OrgExternalService orgExternalService = new OrgExternalServiceImpl();;
  private String contentType = "application/json";

  public static OrgService getInstance() {
    if (orgService == null) {
      orgService = new OrgServiceImpl();
    }
    return orgService;
  }

  @Override
  public Map<String, Object> getOrgById(String orgId, RequestContext context) {
    Map<String, Object> org = orgDao.getOrgById(orgId, context);
    org.putAll(Util.getOrgDefaultValue());
    return org;
  }

  @Override
  public List<Map<String, Object>> getOrgByIds(List<String> orgIds, RequestContext context) {
    return getOrgByIds(orgIds, Collections.emptyList(), context);
  }

  @Override
  public List<Map<String, Object>> getOrgByIds(
      List<String> orgIds, List<String> fields, RequestContext context) {
    return orgDao.getOrgByIds(orgIds, fields, context);
  }

  @Override
  public Map<String, Object> getOrgByExternalIdAndProvider(
      String externalId, String provider, RequestContext context) {
    String orgId =
        orgExternalService.getOrgIdFromOrgExternalIdAndProvider(externalId, provider, context);
    return getOrgById(orgId, context);
  }

  @Override
  public Response createOrganisation(Map<String, Object> orgMap, RequestContext context) {
    return orgDao.create(orgMap, context);
  }

  @Override
  public Response updateOrganisation(Map<String, Object> orgMap, RequestContext context) {
    return orgDao.update(orgMap, context);
  }

  @Override
  public List<Map<String, Object>> organisationSearch(
      Map<String, Object> filters, RequestContext context) {
    Map<String, Object> searchRequestMap = new HashMap<>();
    searchRequestMap.put(JsonKey.FILTERS, filters);
    Response response = orgDao.search(searchRequestMap, context);

    List<Map<String, Object>> orgResponseList = new ArrayList<>();
    if (response != null) {
      orgResponseList = (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
    }
    return orgResponseList;
  }

  @Override
  public Future<Map<String, Object>> searchOrg(SearchDTO searchDTO, RequestContext context) {
    return orgDao.search(searchDTO, context);
  }

  public void createOrgExternalIdRecord(
      String channel, String externalId, String orgId, RequestContext context) {
    if (StringUtils.isNotBlank(channel) && StringUtils.isNotBlank(externalId)) {
      Map<String, Object> orgExtIdRequest = new WeakHashMap<>(3);
      orgExtIdRequest.put(JsonKey.PROVIDER, StringUtils.lowerCase(channel));
      orgExtIdRequest.put(JsonKey.EXTERNAL_ID, StringUtils.lowerCase(externalId));
      orgExtIdRequest.put(JsonKey.ORG_ID, orgId);
      orgExternalService.addOrgExtId(orgExtIdRequest, context);
    }
  }

  public void deleteOrgExternalIdRecord(String channel, String externalId, RequestContext context) {
    if (StringUtils.isNotBlank(channel) && StringUtils.isNotBlank(externalId)) {
      Map<String, String> orgExtIdRequest = new WeakHashMap<>(3);
      orgExtIdRequest.put(JsonKey.PROVIDER, StringUtils.lowerCase(channel));
      orgExtIdRequest.put(JsonKey.EXTERNAL_ID, StringUtils.lowerCase(externalId));
      orgExternalService.deleteOrgExtId(orgExtIdRequest, context);
    }
  }

  public String getOrgIdFromSlug(String slug, RequestContext context) {
    if (!StringUtils.isBlank(slug)) {
      Map<String, Object> filters = new HashMap<>();
      filters.put(JsonKey.SLUG, slug);
      filters.put(JsonKey.IS_TENANT, true);
      List<Map<String, Object>> list = orgService.organisationSearch(filters, context);
      if (CollectionUtils.isNotEmpty(list)) {
        Map<String, Object> esContent = list.get(0);
        return (String) esContent.getOrDefault(JsonKey.ID, "");
      }
    }
    return "";
  }

  /*
   * This method will fetch root org details from elastic search based on channel value.
   */
  public Map<String, Object> getRootOrgFromChannel(String channel, RequestContext context) {
    if (StringUtils.isNotBlank(channel)) {
      Map<String, Object> filterMap = new HashMap<>();
      filterMap.put(JsonKey.CHANNEL, channel);
      filterMap.put(JsonKey.IS_TENANT, true);
      List<Map<String, Object>> list = orgService.organisationSearch(filterMap, context);
      if (CollectionUtils.isNotEmpty(list)) {
        return list.get(0);
      }
    }
    return new HashMap();
  }

  /** @param req Map<String,Object> */
  public boolean registerChannel(Map<String, Object> req, RequestContext context) {
    Map<String, String> headerMap = new HashMap<>();
    String header = System.getenv(JsonKey.EKSTEP_AUTHORIZATION);
    if (StringUtils.isBlank(header)) {
      header = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION);
    } else {
      header = JsonKey.BEARER + header;
    }
    headerMap.put(JsonKey.AUTHORIZATION, header);
    headerMap.put("Content-Type", contentType);
    headerMap.put("user-id", "");
    ProjectUtil.setTraceIdInHeader(headerMap, context);
    String reqString = "";
    String regStatus = "";
    try {
      logger.info(
          context, "start call for registering the channel for org id ==" + req.get(JsonKey.ID));
      String ekStepBaseUrl = System.getenv(JsonKey.EKSTEP_BASE_URL);
      if (StringUtils.isBlank(ekStepBaseUrl)) {
        ekStepBaseUrl = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_BASE_URL);
      }
      Map<String, Object> map = new HashMap<>();
      Map<String, Object> reqMap = new HashMap<>();
      Map<String, Object> channelMap = new HashMap<>();
      channelMap.put(JsonKey.NAME, req.get(JsonKey.CHANNEL));
      channelMap.put(JsonKey.DESCRIPTION, req.get(JsonKey.DESCRIPTION));
      channelMap.put(JsonKey.CODE, req.get(JsonKey.ID));
      if (req.containsKey(JsonKey.LICENSE)
          && StringUtils.isNotBlank((String) req.get(JsonKey.LICENSE))) {
        channelMap.put(JsonKey.DEFAULT_LICENSE, req.get(JsonKey.LICENSE));
      }

      String defaultFramework = (String) req.get(JsonKey.DEFAULT_FRAMEWORK);
      if (StringUtils.isNotBlank(defaultFramework))
        channelMap.put(JsonKey.DEFAULT_FRAMEWORK, defaultFramework);
      reqMap.put(JsonKey.CHANNEL, channelMap);
      map.put(JsonKey.REQUEST, reqMap);

      reqString = mapper.writeValueAsString(map);
      logger.info(
          context, "Util:registerChannel: Channel registration request data = " + reqString);
      regStatus =
          HttpClientUtil.post(
              (ekStepBaseUrl
                  + PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_CHANNEL_REG_API_URL)),
              reqString,
              headerMap,
              context);
      logger.info(context, "end call for channel registration for org id ==" + req.get(JsonKey.ID));
    } catch (Exception e) {
      logger.error(
          context, "Exception occurred while registering channel in ekstep." + e.getMessage(), e);
    }

    return regStatus.contains("OK");
  }

  /** @param req Map<String,Object> */
  public boolean updateChannel(Map<String, Object> req, RequestContext context) {
    Map<String, String> headers = new HashMap<>();
    headers.put("content-type", contentType);
    headers.put("accept", contentType);
    Map<String, String> headerMap = new HashMap<>();
    String header = System.getenv(JsonKey.EKSTEP_AUTHORIZATION);
    if (StringUtils.isBlank(header)) {
      header = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION);
    } else {
      header = JsonKey.BEARER + header;
    }
    headerMap.put(JsonKey.AUTHORIZATION, header);
    headerMap.put("Content-Type", contentType);
    headerMap.put("user-id", "");
    ProjectUtil.setTraceIdInHeader(headers, context);
    String reqString = "";
    String regStatus = "";
    try {
      logger.info(
          context, "start call for updateChannel for hashTag id ==" + req.get(JsonKey.HASHTAGID));
      String ekStepBaseUrl = System.getenv(JsonKey.EKSTEP_BASE_URL);
      if (StringUtils.isBlank(ekStepBaseUrl)) {
        ekStepBaseUrl = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_BASE_URL);
      }
      Map<String, Object> map = new HashMap<>();
      Map<String, Object> reqMap = new HashMap<>();
      Map<String, Object> channelMap = new HashMap<>();
      channelMap.put(JsonKey.NAME, req.get(JsonKey.CHANNEL));
      channelMap.put(JsonKey.DESCRIPTION, req.get(JsonKey.DESCRIPTION));
      channelMap.put(JsonKey.CODE, req.get(JsonKey.HASHTAGID));
      String license = (String) req.get(JsonKey.LICENSE);
      if (StringUtils.isNotBlank(license)) {
        channelMap.put(JsonKey.DEFAULT_LICENSE, license);
      }
      reqMap.put(JsonKey.CHANNEL, channelMap);
      map.put(JsonKey.REQUEST, reqMap);

      reqString = mapper.writeValueAsString(map);

      regStatus =
          HttpClientUtil.patch(
              (ekStepBaseUrl
                      + PropertiesCache.getInstance()
                          .getProperty(JsonKey.EKSTEP_CHANNEL_UPDATE_API_URL))
                  + "/"
                  + req.get(JsonKey.ID),
              reqString,
              headerMap,
              context);
      logger.info(
          context, "end call for channel update for org id ==" + req.get(JsonKey.HASHTAGID));
    } catch (Exception e) {
      logger.error(
          context, "Exception occurred while updating channel in ekstep. " + e.getMessage(), e);
    }
    return regStatus.contains("OK");
  }
}
