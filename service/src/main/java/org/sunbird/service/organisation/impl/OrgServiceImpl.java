package org.sunbird.service.organisation.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.dao.organisation.OrgDao;
import org.sunbird.dao.organisation.impl.OrgDaoImpl;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.organisation.Organisation;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgExternalService;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.common.ProjectUtil;
import org.sunbird.common.PropertiesCache;
import scala.concurrent.Future;

import javax.ws.rs.core.MediaType;

public class OrgServiceImpl implements OrgService {

  private final LoggerUtil logger = new LoggerUtil(this.getClass());
  private static Map<Integer, List<Integer>> orgStatusTransition = new HashMap<>();
  private final ObjectMapper mapper = new ObjectMapper();
  private final OrgDao orgDao = OrgDaoImpl.getInstance();
  private static OrgService orgService;
  private final OrgExternalService orgExternalService = new OrgExternalServiceImpl();

  public static OrgService getInstance() {
    if (orgService == null) {
      orgService = new OrgServiceImpl();
    }
    return orgService;
  }

  static {
    initializeOrgStatusTransition();
  }

  @Override
  public Map<String, Object> getOrgById(String orgId, RequestContext context) {
    return orgDao.getOrgById(orgId, context);
  }

  @Override
  public Organisation getOrgObjById(String orgId, RequestContext context) {
    Map<String, Object> orgMap = orgDao.getOrgById(orgId, context);
    if (MapUtils.isEmpty(orgMap)) {
      return null;
    } else {
      return mapper.convertValue(orgMap, Organisation.class);
    }
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

  public List<Organisation> organisationObjSearch(
      Map<String, Object> filters, RequestContext context) {
    List<Organisation> orgList = new ArrayList<>();
    ObjectMapper objectMapper = new ObjectMapper();
    List<Map<String, Object>> orgMapList = organisationSearch(filters, context);
    if (CollectionUtils.isNotEmpty(orgMapList)) {
      for (Map<String, Object> orgMap : orgMapList) {
        orgMap.put(JsonKey.CONTACT_DETAILS, String.valueOf(orgMap.get(JsonKey.CONTACT_DETAILS)));
        orgList.add(objectMapper.convertValue(orgMap, Organisation.class));
      }
      return orgList;
    } else {
      return Collections.emptyList();
    }
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
    return Collections.emptyMap();
  }

  @Override
  public String getRootOrgIdFromChannel(String channel, RequestContext context) {
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.IS_TENANT, true);
    filters.put(JsonKey.CHANNEL, channel);

    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    Future<Map<String, Object>> esResultF = orgDao.search(searchDTO, context);
    Map<String, Object> esResult =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResultF);
    if (MapUtils.isNotEmpty(esResult)
        && CollectionUtils.isNotEmpty((List) esResult.get(JsonKey.CONTENT))) {
      Map<String, Object> esContent =
          ((List<Map<String, Object>>) esResult.get(JsonKey.CONTENT)).get(0);
      if (null == esContent.get(JsonKey.STATUS) || (1 != (int) esContent.get(JsonKey.STATUS))) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.errorInactiveOrg,
            ProjectUtil.formatMessage(ResponseCode.errorInactiveOrg.getErrorMessage(), JsonKey.CHANNEL, channel));
      }
      return (String) esContent.get(JsonKey.ID);
    } else {
      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue,
          ProjectUtil.formatMessage(ResponseCode.invalidParameterValue.getErrorMessage(), channel, JsonKey.CHANNEL),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  @Override
  public String getChannel(String rootOrgId, RequestContext context) {
    String channel = "";
    Map<String, Object> resultFrRootOrg = getOrgById(rootOrgId, context);
    if (MapUtils.isNotEmpty(resultFrRootOrg)
        && StringUtils.isNotBlank((String) resultFrRootOrg.get(JsonKey.CHANNEL))) {
      channel = (String) resultFrRootOrg.get(JsonKey.CHANNEL);
    }
    return channel;
  }

  /** @param req Map<String,Object> */
  public boolean registerChannel(
      Map<String, Object> req, String operationType, RequestContext context) {
    if (Boolean.parseBoolean(ProjectUtil.getConfigValue(JsonKey.CHANNEL_REGISTRATION_DISABLED)))
      return true;

    Map<String, String> headerMap = new HashMap<>();
    headerMap.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    ProjectUtil.setTraceIdInHeader(headerMap, context);
    String reqString = "";
    String regStatus = "";
    try {
      logger.info(context, "start call for registering the channel for org id ==" + req.get(JsonKey.ID));
      String contentServiceBaseUrl = PropertiesCache.getInstance().readProperty(JsonKey.SUNBIRD_CONTENT_SERVICE_API_BASE_URL);
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
      logger.info(context, "Channel request data = " + reqString + " for operation : " + operationType);
      if (JsonKey.CREATE.equalsIgnoreCase(operationType)) {
        regStatus =
            HttpClientUtil.post(
                (contentServiceBaseUrl + PropertiesCache.getInstance().readProperty(JsonKey.SUNBIRD_CHANNEL_CREATE_API_URL)),
                reqString,
                headerMap,
                context);
      } else if (JsonKey.UPDATE.equalsIgnoreCase(operationType)) {
        regStatus =
            HttpClientUtil.patch(
                (contentServiceBaseUrl + PropertiesCache.getInstance().readProperty(JsonKey.SUNBIRD_CHANNEL_UPDATE_API_URL))
                    + "/"
                    + req.get(JsonKey.ID),
                reqString,
                headerMap,
                context);
      }
      logger.info(context, "Call end for channel registration/update for org id ==" + req.get(JsonKey.HASHTAGID));
    } catch (Exception e) {
      logger.error(context, "Exception occurred while registering/update channel." + e.getMessage(), e);
    }
    return regStatus.contains("OK");
  }

  @Override
  public String saveOrgToEs(String id, Map<String, Object> data, RequestContext context) {
    return orgDao.saveOrgToEs(id, data, context);
  }

  /**
   * This method will take org current state and next state and check is it possible to move
   * organization from current state to next state if possible to move then return true else false.
   *
   * @param currentState String
   * @param nextState String
   * @return boolean
   */
  public boolean checkOrgStatusTransition(Integer currentState, Integer nextState) {
    List<Integer> list = orgStatusTransition.get(currentState);
    if (null == list) {
      return false;
    }
    return list.contains(nextState);
  }

  /**
   * This method will a map of organization state transaction. which will help us to move the
   * organization status from one Valid state to another state.
   */
  private static void initializeOrgStatusTransition() {
    orgStatusTransition.put(
        ProjectUtil.OrgStatus.ACTIVE.getValue(),
        Arrays.asList(
            ProjectUtil.OrgStatus.ACTIVE.getValue(),
            ProjectUtil.OrgStatus.INACTIVE.getValue(),
            ProjectUtil.OrgStatus.BLOCKED.getValue(),
            ProjectUtil.OrgStatus.RETIRED.getValue()));
    orgStatusTransition.put(
        ProjectUtil.OrgStatus.INACTIVE.getValue(),
        Arrays.asList(ProjectUtil.OrgStatus.ACTIVE.getValue(), ProjectUtil.OrgStatus.INACTIVE.getValue()));
    orgStatusTransition.put(
        ProjectUtil.OrgStatus.BLOCKED.getValue(),
        Arrays.asList(
            ProjectUtil.OrgStatus.ACTIVE.getValue(),
            ProjectUtil.OrgStatus.BLOCKED.getValue(),
            ProjectUtil.OrgStatus.RETIRED.getValue()));
    orgStatusTransition.put(
        ProjectUtil.OrgStatus.RETIRED.getValue(),
        Arrays.asList(ProjectUtil.OrgStatus.RETIRED.getValue()));
    orgStatusTransition.put(
        null,
        Arrays.asList(
            ProjectUtil.OrgStatus.ACTIVE.getValue(),
            ProjectUtil.OrgStatus.INACTIVE.getValue(),
            ProjectUtil.OrgStatus.BLOCKED.getValue(),
            ProjectUtil.OrgStatus.RETIRED.getValue()));
  }
}
