package org.sunbird.actor.organisation.validator;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.location.validator.LocationRequestValidator;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.location.Location;
import org.sunbird.request.RequestContext;
import org.sunbird.service.location.LocationService;
import org.sunbird.service.location.LocationServiceImpl;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgExternalServiceImpl;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.common.ProjectUtil;
import org.sunbird.utils.Slug;

public class OrganisationRequestValidator {

  private final LoggerUtil logger = new LoggerUtil(OrganisationRequestValidator.class);
  private final OrgService orgService = OrgServiceImpl.getInstance();
  private final LocationService locationService = new LocationServiceImpl();
  private final LocationRequestValidator validator = new LocationRequestValidator();

  public void validateExternalId(Map<String, Object> request, RequestContext context) {
    String channel = (String) request.get(JsonKey.CHANNEL);
    String passedExternalId = (String) request.get(JsonKey.EXTERNAL_ID);
    if (StringUtils.isNotBlank(passedExternalId)) {
      passedExternalId = passedExternalId.toLowerCase();
      if (!validateChannelExternalIdUniqueness(channel, passedExternalId, null, context)) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.errorDuplicateEntry,
            MessageFormat.format(
                ResponseCode.errorDuplicateEntry.getErrorMessage(),
                passedExternalId,
                JsonKey.EXTERNAL_ID));
      }
      request.put(JsonKey.EXTERNAL_ID, passedExternalId);
      request.put(JsonKey.PROVIDER, StringUtils.lowerCase(channel));
    } else {
      request.remove(JsonKey.EXTERNAL_ID);
      request.remove(JsonKey.PROVIDER);
    }
  }

  public void validateSlug(Map<String, Object> request, RequestContext context) {
    Boolean isTenant = (Boolean) request.get(JsonKey.IS_TENANT);
    String slug = Slug.makeSlug((String) request.getOrDefault(JsonKey.CHANNEL, ""), true);
    if (null != isTenant && isTenant) {
      String orgId = orgService.getOrgIdFromSlug(slug, context);
      if (StringUtils.isBlank(orgId)) {
        request.put(JsonKey.SLUG, slug);
      } else {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.errorDuplicateEntry,
            MessageFormat.format(
                ResponseCode.errorDuplicateEntry.getErrorMessage(), slug, JsonKey.SLUG));
      }
    } else {
      request.put(JsonKey.SLUG, slug);
    }
  }

  public void channelMandatoryValidation(Map<String, Object> request) {
    if (StringUtils.isBlank((String) request.get(JsonKey.CHANNEL))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing,
          MessageFormat.format(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.CHANNEL),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  public void validateOrgRequest(Map<String, Object> req, RequestContext context) {
    String orgId = (String) req.get(JsonKey.ORGANISATION_ID);
    String provider = (String) req.get(JsonKey.PROVIDER);
    String externalId = (String) req.get(JsonKey.EXTERNAL_ID);
    if (StringUtils.isBlank(orgId)) {
      if (StringUtils.isBlank(provider) || StringUtils.isBlank(externalId)) {
        throw new ProjectCommonException(
            ResponseCode.invalidRequestData,
            ResponseCode.invalidRequestData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      } else {
        // fetch orgid from database on basis of source and external id and put orgid
        // into request .
        OrgExternalServiceImpl orgExtService = new OrgExternalServiceImpl();
        String organisationId =
            orgExtService.getOrgIdFromOrgExternalIdAndProvider(
                (String) req.get(JsonKey.EXTERNAL_ID), (String) req.get(JsonKey.PROVIDER), context);
        if (StringUtils.isEmpty(organisationId)) {
          throw new ProjectCommonException(
              ResponseCode.invalidRequestData,
              ResponseCode.invalidRequestData.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
        req.put(JsonKey.ORGANISATION_ID, organisationId);
      }
    }
  }

  public void validateOrgType(String orgType, String orgSubType, String operation) {
    if (StringUtils.isBlank(orgType) && operation.equalsIgnoreCase(JsonKey.CREATE)) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing,
          MessageFormat.format(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.ORG_TYPE),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    if (StringUtils.isNotBlank(orgType) && !OrgTypeValidator.getInstance().isOrgTypeExist(orgType)) {
      throw new ProjectCommonException(
              ResponseCode.invalidValue,
              MessageFormat.format(
                      ResponseCode.invalidValue.getErrorMessage(), JsonKey.ORG_TYPE, orgType, OrgTypeValidator.getInstance().getOrgTypeNames()),
              ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    if (StringUtils.isNotBlank(orgSubType) && !OrgTypeValidator.getInstance().isOrgTypeExist(orgSubType)) {
      throw new ProjectCommonException(
              ResponseCode.invalidValue,
              MessageFormat.format(
                      ResponseCode.invalidValue.getErrorMessage(), JsonKey.ORG_TYPE, orgType, OrgTypeValidator.getInstance().getOrgTypeNames()),
              ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  public void isTenantIdValid(String id, RequestContext context) {
    Map<String, Object> orgDbMap = orgService.getOrgById(id, context);
    boolean isValid = MapUtils.isNotEmpty(orgDbMap) && (boolean) orgDbMap.get(JsonKey.IS_TENANT);

    if (!isValid) {
      logger.info("OrganisationManagementActor: no root org found with Id: " + id);
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData,
          MessageFormat.format(
              ResponseCode.invalidParameter.getErrorMessage(), JsonKey.ORGANISATION + JsonKey.ID),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  public void validateOrgLocation(Map<String, Object> request, RequestContext context) {
    List<String> locList = new ArrayList<>();
    List<Map<String, String>> orgLocationList =
        (List<Map<String, String>>) request.get(JsonKey.ORG_LOCATION);
    if (CollectionUtils.isEmpty(orgLocationList)) {
      // Request is from org upload
      List<String> locCodeList = (List<String>) request.get(JsonKey.LOCATION_CODE);
      if (CollectionUtils.isNotEmpty(locCodeList)) {
        locList = validator.getValidatedLocationIds(locCodeList, context);
        request.remove(JsonKey.LOCATION_CODE);
      } else {
        return;
      }
    } else {
      // If request orglocation is a list of map , which has location id
      List<String> finalLocList = getLocationCodeorIdList(orgLocationList, JsonKey.ID);
      // If request orglocation is a list of map , which has location code
      if (CollectionUtils.isEmpty(finalLocList)) {
        finalLocList = getLocationCodeorIdList(orgLocationList, JsonKey.CODE);
        if (CollectionUtils.isNotEmpty(finalLocList)) {
          locList = validator.getValidatedLocationIds(finalLocList, context);
        }
      } else {
        locList = finalLocList;
      }
    }
    List<String> locationIdsList = validator.getHierarchyLocationIds(locList, context);
    List<Map<String, String>> newOrgLocationList = new ArrayList<>();
    List<Location> locationList =
        locationService.locationSearch(JsonKey.ID, locationIdsList, context);
    locationList
        .stream()
        .forEach(
            location -> {
              Map<String, String> map = new HashMap<>();
              map.put(JsonKey.ID, location.getId());
              map.put(JsonKey.TYPE, location.getType());
              newOrgLocationList.add(map);
            });
    request.put(JsonKey.ORG_LOCATION, newOrgLocationList);
  }

  public List<String> getLocationCodeorIdList(
      List<Map<String, String>> orgLocationList, String key) {
    List<String> finalLocList = new ArrayList<>();
    orgLocationList
        .stream()
        .forEach(
            loc -> {
              if (loc.containsKey(key)) {
                finalLocList.add(loc.get(key));
              }
            });
    return finalLocList;
  }

  public boolean validateChannelUniqueness(
      String channel, String orgId, Boolean isTenant, RequestContext context) {
    if (StringUtils.isNotBlank(channel)) {
      Map<String, Object> filters = new HashMap<>();
      filters.put(JsonKey.CHANNEL, channel);
      filters.put(JsonKey.IS_TENANT, true);
      return validateChannelUniqueness(filters, orgId, isTenant, context);
    }
    return (orgId == null);
  }

  private boolean validateChannelUniqueness(
      Map<String, Object> filters, String orgId, Boolean isTenant, RequestContext context) {
    if (MapUtils.isNotEmpty(filters)) {
      List<Map<String, Object>> list = orgService.organisationSearch(filters, context);
      if (CollectionUtils.isEmpty(list)) {
        if (StringUtils.isBlank(orgId)) {
          return true;
        } else {
          if (isTenant) {
            return true;
          } else {
            return false;
          }
        }
      } else {
        if (StringUtils.isBlank(orgId)) {
          return false;
        } else {
          Map<String, Object> data = list.get(0);
          String id = (String) data.get(JsonKey.ID);
          if (isTenant) {
            return id.equalsIgnoreCase(orgId);
          } else {
            // for suborg channel should be valid
            return true;
          }
        }
      }
    }
    return true;
  }

  public boolean validateChannelExternalIdUniqueness(
      String channel, String externalId, String orgId, RequestContext context) {
    OrgExternalServiceImpl orgExternalService = new OrgExternalServiceImpl();
    if (StringUtils.isNotBlank(channel) && StringUtils.isNotBlank(externalId)) {
      String orgIdFromDb =
          orgExternalService.getOrgIdFromOrgExternalIdAndProvider(
              StringUtils.lowerCase(externalId), StringUtils.lowerCase(channel), context);
      if (StringUtils.isEmpty(orgIdFromDb)) {
        return true;
      } else {
        if (orgId == null) {
          return false;
        }
        return orgIdFromDb.equalsIgnoreCase(orgId);
      }
    }
    return false;
  }

  public void validateChannel(Map<String, Object> req, RequestContext context) {
    String channel = (String) req.get(JsonKey.CHANNEL);
    if (!req.containsKey(JsonKey.IS_TENANT) || !(Boolean) req.get(JsonKey.IS_TENANT)) {
      Map<String, Object> rootOrg = orgService.getRootOrgFromChannel(channel, context);
      if (MapUtils.isEmpty(rootOrg)) {
        logger.info(
            context, "OrganisationManagementActor:validateChannel: Invalid channel = " + channel);
        throw new ProjectCommonException(
            ResponseCode.invalidParameter,
            MessageFormat.format(ResponseCode.invalidParameter.getErrorMessage(), JsonKey.CHANNEL),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      Object status = rootOrg.get(JsonKey.STATUS);
      if (null != status && 1 != (Integer) status) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.errorInactiveOrg,
            ProjectUtil.formatMessage(
                ResponseCode.errorInactiveOrg.getErrorMessage(), JsonKey.CHANNEL, channel));
      }
    } else if (!validateChannelUniqueness(channel, null, null, context)) {
      logger.info(
          context, "OrganisationManagementActor:validateChannel: Channel validation failed");
      throw new ProjectCommonException(
          ResponseCode.errorDuplicateEntry,
          MessageFormat.format(
              ResponseCode.errorDuplicateEntry.getErrorMessage(), channel, JsonKey.CHANNEL),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }
}
