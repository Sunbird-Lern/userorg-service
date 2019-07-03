package org.sunbird.userorg;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.sunbird.common.exception.ProjectCommonException.throwServerErrorException;
import static org.sunbird.common.models.util.JsonKey.*;
import static org.sunbird.common.models.util.LoggerEnum.ERROR;
import static org.sunbird.common.models.util.LoggerEnum.INFO;
import static org.sunbird.common.models.util.ProjectLogger.log;
import static org.sunbird.common.models.util.ProjectUtil.getConfigValue;
import static org.sunbird.common.responsecode.ResponseCode.errorProcessingRequest;

public class UserOrgServiceImpl implements UserOrgService {

    private SSOManager ssoManager = SSOServiceFactory.getInstance();
    private ObjectMapper mapper=new ObjectMapper();
    private static final String FORWARD_SLASH = "/";

    private static Map<String, String> getdefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(AUTHORIZATION, BEARER + getConfigValue(SUNBIRD_AUTHORIZATION));
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private Response getUserOrgResponse(String requestAPI, HttpMethod requestType, Map<String, Object> requestMap, Map<String,String> headers)
    {
        Response response=null;
        String requestUrl= getConfigValue(SUNBIRD_API_BASE_URL) + getConfigValue(requestAPI);
        HttpResponse<String> httpResponse=null;
        String responseBody=null;
        log(
                "UserOrgServiceImpl:getResponse:Sending "+ requestType +" Request, Request URL: "
                        + requestUrl,
                INFO.name());
        try {
            String reqBody = mapper.writeValueAsString(requestMap);
            if(HttpMethod.POST.equals(requestType)) {
                httpResponse = Unirest.post(requestUrl).headers(headers).body(reqBody).asString();
            }
             if (HttpMethod.GET.equals(requestType)){
                requestUrl = FORWARD_SLASH+requestMap.get(USER_ID);
                httpResponse = Unirest.get(requestUrl).headers(headers).asString();
            }
            log(
                    "UserOrgServiceImpl:getResponse"+ requestType +"Request , Status : "
                            + httpResponse.getStatus()+" "+httpResponse.getStatusText(),
                    ERROR.name());
            if (StringUtils.isBlank(httpResponse.getBody())){
                throwServerErrorException(
                        ResponseCode.SERVER_ERROR, errorProcessingRequest.getErrorMessage());
            }
            responseBody = httpResponse.getBody();
            response = mapper.readValue(responseBody, Response.class);
            if (!ResponseCode.OK.equals(response.getResponseCode())) {

                throw new ProjectCommonException(
                        response.getResponseCode().name(),
                        response.getParams().getErrmsg(),
                        response.getResponseCode().getResponseCode());
            }
        }

       catch(ProjectCommonException e)
       {
           log(
                   "UserOrgServiceImpl:getResponse:"+ requestType +"Request , Status : "
                           + e.getCode()+" "+e.getMessage()+",Response Body :"+responseBody,
                   ERROR.name());
           throw e;
       }
        catch (Exception e) {
            log(
                    "UserOrgServiceImpl:getResponse:Exception occurred with error message = "
                            + e.getMessage()+", Response Body : "+responseBody,
                    e);
            throwServerErrorException(ResponseCode.SERVER_ERROR);
        }
        return response;

    }

    private Map<String, Object> getRequestMap(Map<String, Object> filterlist)
    {
        Map<String, Object> requestMap = new HashMap<>();
        Map<String, Object> request = new HashMap<>();
        requestMap.put(JsonKey.REQUEST, request);
        request.put(FILTERS, filterlist);
        return requestMap;
    }

    @Override
    public Map<String,Object> getOrganisationById(String id)
    {
        Map<String, Object> filterlist = new HashMap<>();
        filterlist.put(ID,id);
        Map<String, Object> requestMap=getRequestMap(filterlist);
        Map<String, String> headers = getdefaultHeaders();
        Response response = getUserOrgResponse(SUNBIRD_GET_ORGANISATION_API,HttpMethod.POST,requestMap,headers);
        Map<String,Object> orgDetails=(Map<String,Object>)response.get(RESPONSE);
        List<Map<String, Object>> list = (List<Map<String, Object>>) orgDetails.get(CONTENT);
        return list.get(0);

    }

    @Override
    public List<Map<String,Object>> getOrganisationsByIds(List<String> ids)
    {
        Map<String, Object> filterlist = new HashMap<>();
        filterlist.put(ID,ids);
        Map<String, Object> requestMap=getRequestMap(filterlist);
        Map<String, String> headers = getdefaultHeaders();
        Response response = getUserOrgResponse(SUNBIRD_GET_ORGANISATION_API,HttpMethod.POST,requestMap,headers);
        Map<String,Object> orgMap=(Map<String,Object>)response.get(RESPONSE);
        List<Map<String, Object>> orglist = (List<Map<String, Object>>) orgMap.get(CONTENT);
        return orglist;

    }
    @Override
    public Map<String,Object> getUserById(String id)
    {
        Map<String, Object> filterlist = new HashMap<>();
        filterlist.put(ID,id);
        Map<String, Object> requestMap=getRequestMap(filterlist);
        Map<String, String> headers = getdefaultHeaders();
        headers.put(
                "x-authenticated-user-token",
                ssoManager.login(
                        getConfigValue(JsonKey.SUNBIRD_SSO_USERNAME),
                        getConfigValue(JsonKey.SUNBIRD_SSO_PASSWORD)));

        Response response = getUserOrgResponse(SUNBIRD_GET_SINGLE_USER_API,HttpMethod.GET,requestMap,headers);
        Map<String,Object> userMap=(Map<String,Object>)response.get(RESPONSE);
        return userMap;
    }

    @Override
    public List<Map<String,Object>> getUsersByIds(List<String> ids)
    {
        Map<String, Object> filterlist = new HashMap<>();
        filterlist.put(ID,ids);
        Map<String, Object> requestMap=getRequestMap(filterlist);
        Map<String, String> headers = getdefaultHeaders();
        headers.put(
                "x-authenticated-user-token",
                ssoManager.login(
                        getConfigValue(JsonKey.SUNBIRD_SSO_USERNAME),
                        getConfigValue(JsonKey.SUNBIRD_SSO_PASSWORD)));
        Response response = getUserOrgResponse(SUNBIRD_GET_MULTIPLE_USER_API,HttpMethod.POST,requestMap,headers);
        Map<String,Object> userMap=(Map<String,Object>)response.get(RESPONSE);
        List<Map<String, Object>> userlist = (List<Map<String, Object>>) userMap.get(CONTENT);
        return userlist;
    }

}
