package org.sunbird.actor.organisation.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.organisation.OrganisationType;
import org.sunbird.request.RequestContext;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class OrgTypeValidator {
    private static OrgTypeValidator instance = null;
    private ConcurrentMap<String, OrganisationType> orgTypeMap = null;
    private LoggerUtil logger = new LoggerUtil(this.getClass());

    private OrgTypeValidator() {
    }

    public static OrgTypeValidator getInstance() {
        if (instance == null) {
            instance = new OrgTypeValidator();
        }
        return instance;
    }

    public boolean isOrgTypeInitialized() {
        return !MapUtils.isEmpty(orgTypeMap);
    }

    public void initializeOrgTypeFromCache(List<OrganisationType> organisationTypeList) {
        if (orgTypeMap == null) {
            orgTypeMap = new ConcurrentHashMap<String, OrganisationType>();
        } else {
            orgTypeMap.clear();
        }
        orgTypeMap = organisationTypeList.stream().collect(Collectors.toConcurrentMap(OrganisationType::getName, orgType -> orgType));
        RequestContext reqContext = new RequestContext();
        reqContext.setReqId(UUID.randomUUID().toString());
        reqContext.setDebugEnabled("false");
        try {
            logger.info(reqContext, "OrgType Map is initialized. " + (new ObjectMapper()).writeValueAsString(orgTypeMap));
        } catch (JsonProcessingException e) {
            logger.error(reqContext, "OrgTypeValidator: Failed to initialize using List of OrganizationType. ", e);
        }
    }

    public boolean isOrgTypeExist(String orgType) {
        boolean retValue = false;
        if (MapUtils.isNotEmpty(orgTypeMap)) {
            retValue = orgTypeMap.containsKey(orgType.toLowerCase());
        }
        return retValue;
    }

    public List<String> getOrgTypeNames() {
        if (MapUtils.isNotEmpty(orgTypeMap)) {
            return new ArrayList<String>(orgTypeMap.keySet());
        } else {
            return Collections.emptyList();
        }
    }

    public List<Integer> getOrgTypeValues() {
        if(MapUtils.isNotEmpty(orgTypeMap)) {
            return orgTypeMap.values().stream().map(OrganisationType::getValue).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public int getValueByType(String orgType) {
        int orgValue = -1;
        if (MapUtils.isNotEmpty(orgTypeMap)) {
            if (orgTypeMap.containsKey(orgType)) {
                orgValue = orgTypeMap.get(orgType).getValue();
            }
        }
        if (orgValue == -1) {
            throw new ProjectCommonException(ResponseCode.invalidValue, MessageFormat
                    .format(ResponseCode.invalidValue.getErrorMessage(), JsonKey.ORG_TYPE, orgType, getOrgTypeNames()),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
        }
        return orgValue;
    }

    public String getTypeByValue(int orgTypeValue) {
        String orgTypeName = "";
        if(MapUtils.isNotEmpty(orgTypeMap)) {
            for(OrganisationType orgTypeObj : orgTypeMap.values()) {
                if(orgTypeObj.getValue() == orgTypeValue) {
                    orgTypeName = orgTypeObj.getName();
                    break;
                }
            }
        }
        if(StringUtils.isBlank(orgTypeName)) {
            throw new ProjectCommonException(ResponseCode.invalidValue, MessageFormat
                    .format(ResponseCode.invalidValue.getErrorMessage(), JsonKey.ORG_TYPE, orgTypeValue, getOrgTypeValues()),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
        }
        return orgTypeName;
    }

    public void updateOrganisationTypeFlags(Map<String, Object> orgObject) {
        if(MapUtils.isEmpty(orgTypeMap)) {
            return;
        }
        if(MapUtils.isEmpty(orgObject)) {
            return;
        }
        if(orgObject.get(JsonKey.ORGANISATION_TYPE) != null) {
            int orgTypeValue = (int) orgObject.get(JsonKey.ORGANISATION_TYPE);
            for (OrganisationType orgTypeObj : orgTypeMap.values()) {
                if ((orgTypeValue & orgTypeObj.getValue()) == orgTypeObj.getValue()) {
                    for (String flagName : orgTypeObj.getFlagNameList()) {
                        orgObject.put(flagName, true);
                    }
                    break;
                }
            }
        }

        if (orgObject.get(JsonKey.ORG_SUB_TYPE) != null) {
            int orgSubTypeValue = (int) orgObject.get(JsonKey.ORG_SUB_TYPE);
            for (OrganisationType orgTypeObj : orgTypeMap.values()) {
                if ((orgSubTypeValue & orgTypeObj.getValue()) == orgTypeObj.getValue()) {
                    for(String flagName : orgTypeObj.getFlagNameList()) {
                        orgObject.put(flagName, true);
                    }
                    break;
                }
            }
        }
    }
}
