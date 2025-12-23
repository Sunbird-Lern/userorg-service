package org.sunbird.service.user;

import static org.mockito.ArgumentMatchers.nullable;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.dispatch.Futures;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.organisation.validator.OrgTypeValidator;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.dao.user.UserDao;
import org.sunbird.dao.user.UserOrgDao;
import org.sunbird.dao.user.UserRoleDao;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.dao.user.impl.UserOrgDaoImpl;
import org.sunbird.dao.user.impl.UserRoleDaoImpl;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.UserUtility;
import org.sunbird.util.Util;
import org.sunbird.util.user.UserUtil;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  UserUtil.class,
  ServiceFactory.class,
  CassandraOperationImpl.class,
  DataCacheHandler.class,
  UserDao.class,
  UserDaoImpl.class,
  UserOrgDao.class,
  UserOrgDaoImpl.class,
  UserUtility.class,
  Util.class,
  ElasticSearchRestHighImpl.class,
  EsClientFactory.class,
  ElasticSearchHelper.class,
  UserRoleDao.class,
  UserRoleDaoImpl.class,
  OrgTypeValidator.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserProfileReadTest {

  private String tncConfig =
      "{\"latestVersion\":\"v1\",\"v1\":{\"url\":\"http://dev/terms.html\"},\"v2\":{\"url\":\"http://dev/terms.html\"},\"v4\":{\"url\":\"http://dev/terms.html\"}}";
  private String groupsConfig =
      "{\"latestVersion\":\"v1\",\"v1\":{\"url\":\"http://dev/terms.html\"},\"v2\":{\"url\":\"http://dev/terms.html\"},\"v4\":{\"url\":\"http://dev/terms.html\"}}";

  private String orgAdminTnc =
      "{\"latestVersion\":\"v1\",\"v1\":{\"url\":\"http://dev/terms.html\"},\"v2\":{\"url\":\"http://dev/terms.html\"},\"v4\":{\"url\":\"http://dev/terms.html\"}}";

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(DataCacheHandler.class);
    Map<String, String> config = new HashMap<>();
    config.put(JsonKey.TNC_CONFIG, tncConfig);
    config.put("groups", groupsConfig);
    config.put("orgAdminTnc", orgAdminTnc);
    when(DataCacheHandler.getConfigSettings()).thenReturn(config);
    OrgTypeValidator orgTypeValidator = mock(OrgTypeValidator.class);
    PowerMockito.mockStatic(OrgTypeValidator.class);
    when(OrgTypeValidator.getInstance()).thenReturn(orgTypeValidator);
    when(orgTypeValidator.getValueByType(JsonKey.ORG_TYPE_SCHOOL)).thenReturn(2);
  }

  @Test
  public void getUserProfileDataTest2() throws JsonProcessingException {
    PowerMockito.mockStatic(EsClientFactory.class);
    ElasticSearchRestHighImpl esSearch = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esSearch);
    Map<String, Object> esRespone = new HashMap<>();
    esRespone.put(JsonKey.CONTENT, new ArrayList<>());
    esRespone.put(JsonKey.LOCATION_TYPE, "STATE");
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esRespone);

    when(esSearch.search(Mockito.any(SearchDTO.class), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());

    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperation cassandraOperationImpl = mock(CassandraOperation.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    List<Map<String, Object>> resp = new ArrayList<>();
    Map<String, Object> resMap = new HashMap<>();
    resMap.put(JsonKey.USER_ID, "1234");
    resMap.put(JsonKey.IS_DELETED, false);
    resp.add(resMap);
    response.put(JsonKey.RESPONSE, resp);
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);

    Response response2 = new Response();
    List<Map<String, Object>> resp2 = new ArrayList<>();
    Map<String, Object> userList2 = new HashMap<>();
    userList2.put(JsonKey.USER_ID, "1234");
    userList2.put(JsonKey.ORG_NAME, "rootOrg");
    userList2.put(JsonKey.IS_DELETED, false);
    userList2.put(JsonKey.ORGANISATION_ID, "4578963210");
    List<String> roles = new ArrayList<>();
    roles.add("PUBLIC");
    roles.add("ORG_ADMIN");
    userList2.put(JsonKey.ROLES, roles);
    userList2.put("dob", "1992-11-24");

    Map<String, Object> userList3 = new HashMap<>();
    userList3.put(JsonKey.USER_ID, "1234");
    userList3.put(JsonKey.ORG_NAME, "subOrg");
    userList3.put(JsonKey.IS_DELETED, false);
    userList3.put(JsonKey.ORGANISATION_ID, "457896321012");
    userList3.put(JsonKey.ROLES, roles);
    userList3.put("dob", "1992-11-24");

    resp2.add(userList2);
    resp2.add(userList3);
    response2.put(JsonKey.RESPONSE, resp2);
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response2);

    UserDao userDao = PowerMockito.mock(UserDao.class);
    PowerMockito.mockStatic(UserDaoImpl.class);
    Mockito.when(UserDaoImpl.getInstance()).thenReturn(userDao);
    PowerMockito.mockStatic(UserUtility.class);
    PowerMockito.mockStatic(Util.class);
    Mockito.when(UserUtility.decryptUserData(Mockito.anyMap()))
        .thenReturn(getUserDbMap("1234567890"));
    Map<String, Object> userDetails = getValidUserResponse("1234567890");
    String profileLocation = "[{\"id\":\"4567891231\",\"type\":\"state\"}]";

    userDetails.put(JsonKey.PROFILE_LOCATION, profileLocation);
    Mockito.when(userDao.getUserDetailsById(Mockito.anyString(), Mockito.any()))
        .thenReturn(userDetails);

    UserOrgDao userOrgDao = PowerMockito.mock(UserOrgDao.class);
    PowerMockito.mockStatic(UserOrgDaoImpl.class);
    Mockito.when(UserOrgDaoImpl.getInstance()).thenReturn(userOrgDao);
    Mockito.when(userOrgDao.getUserOrgListByUserId(Mockito.anyString(), Mockito.any()))
        .thenReturn(response2);

    Map<String, Object> org = new HashMap<>();
    org.put(JsonKey.ID, "4578963210");
    org.put(JsonKey.ORGANISATION_ID, "4578963210");
    org.put(JsonKey.LOCATION_ID, "987542312459");
    org.put(JsonKey.ORG_NAME, "org name");
    org.put(JsonKey.HASHTAGID, "4578963210");
    org.put(JsonKey.CHANNEL, "channel");
    org.put(JsonKey.ORGANISATION_TYPE, 2);
    org.put(JsonKey.ORG_LOCATION, "{orgLocation:45");
    List<String> locIds = new ArrayList<>();
    locIds.add("location1");
    locIds.add("location2");
    org.put(JsonKey.LOCATION_IDS, locIds);
    List<Map<String, Object>> orgList = new ArrayList<>();
    orgList.add(org);
    Response orgRes = new Response();
    orgRes.getResult().put(JsonKey.RESPONSE, orgList);

    Map<String, Object> locn = new HashMap<>();
    locn.put(JsonKey.ID, "location1");
    locn.put(JsonKey.CODE, "code1");
    locn.put(JsonKey.NAME, "locn 1");
    locn.put(JsonKey.TYPE, "state");
    locn.put(JsonKey.PARENT_ID, null);

    Map<String, Object> locn2 = new HashMap<>();
    locn2.put(JsonKey.ID, "location2");
    locn2.put(JsonKey.CODE, "code2");
    locn2.put(JsonKey.NAME, "locn 2");
    locn2.put(JsonKey.TYPE, "district");
    locn2.put(JsonKey.PARENT_ID, "location1");

    Map<String, Object> block = new HashMap<>();
    block.put(JsonKey.ID, "blockId");
    block.put(JsonKey.CODE, "block1");
    block.put(JsonKey.NAME, "block1");
    block.put(JsonKey.TYPE, "block");
    block.put(JsonKey.PARENT_ID, "location2");

    Map<String, Object> cluster = new HashMap<>();
    cluster.put(JsonKey.ID, "clusterId");
    cluster.put(JsonKey.CODE, "cluster1");
    cluster.put(JsonKey.NAME, "cluster1");
    cluster.put(JsonKey.TYPE, "cluster");
    cluster.put(JsonKey.PARENT_ID, "blockId");

    Map<String, Object> school = new HashMap<>();
    school.put(JsonKey.ID, "schoolId");
    school.put(JsonKey.CODE, "school1");
    school.put(JsonKey.NAME, "school1");
    school.put(JsonKey.TYPE, "school");
    school.put(JsonKey.PARENT_ID, "clusterId");

    List<Map<String, Object>> locnList = new ArrayList<>();
    locnList.add(locn);
    locnList.add(locn2);
    locnList.add(block);
    locnList.add(cluster);
    locnList.add(school);
    Response locnResponse = new Response();
    locnResponse.getResult().put(JsonKey.RESPONSE, locnList);

    Mockito.when(
            cassandraOperationImpl.getPropertiesValueById(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.any()))
        .thenReturn(orgRes)
        .thenReturn(orgRes)
        .thenReturn(orgRes)
        .thenReturn(orgRes)
        .thenReturn(locnResponse)
        .thenReturn(locnResponse);

    UserProfileReadService userProfileReadService = new UserProfileReadService();

    List<Map<String, String>> externalIds = new ArrayList<>();
    Map<String, String> externalId = new HashMap<>();
    externalId.put(JsonKey.ID, "extid");
    externalId.put(JsonKey.ID_TYPE, "4578963210");
    externalId.put(JsonKey.PROVIDER, "4578963210");
    externalId.put(JsonKey.ORIGINAL_EXTERNAL_ID, "extid1");
    externalId.put(JsonKey.ORIGINAL_ID_TYPE, "4578963210");
    externalId.put(JsonKey.ORIGINAL_PROVIDER, "4578963210");
    externalIds.add(externalId);

    Map<String, String> externalId1 = new HashMap<>();
    externalId1.put(JsonKey.ID, "extid1@test.com");
    externalId1.put(JsonKey.ID_TYPE, "DECLARED_EMAIL");
    externalId1.put(JsonKey.PROVIDER, "4578963210");
    externalId1.put(JsonKey.ORIGINAL_EXTERNAL_ID, "extid1@test.com");
    externalId1.put(JsonKey.ORIGINAL_ID_TYPE, "DECLARED_EMAIL");
    externalId1.put(JsonKey.ORIGINAL_PROVIDER, "4578963210");
    externalIds.add(externalId1);

    Map<String, String> externalId2 = new HashMap<>();
    externalId2.put(JsonKey.ID, "district");
    externalId2.put(JsonKey.ID_TYPE, "DECLARED_DISTRICT");
    externalId2.put(JsonKey.PROVIDER, "4578963210");
    externalId2.put(JsonKey.ORIGINAL_EXTERNAL_ID, "district");
    externalId2.put(JsonKey.ORIGINAL_ID_TYPE, "DECLARED_DISTRICT");
    externalId2.put(JsonKey.ORIGINAL_PROVIDER, "4578963210");
    externalIds.add(externalId2);

    PowerMockito.mockStatic(UserUtil.class);
    when(UserUtil.getExternalIds(Mockito.anyString(), Mockito.anyBoolean(), Mockito.any()))
        .thenReturn(externalIds);

    PowerMockito.mockStatic(UserRoleDaoImpl.class);
    UserRoleDao userRoleDao = PowerMockito.mock(UserRoleDao.class);
    Mockito.when(UserRoleDaoImpl.getInstance()).thenReturn(userRoleDao);
    List<Map<String, Object>> userRoleDetails = new ArrayList<>();
    Map<String, Object> userRoleMap = new HashMap<>();
    userRoleMap.put("role", "CONTENT_CREATOR");
    userRoleMap.put("userid", "4a3ded8a-d731-4f58-a722-e63b00925cd0");
    userRoleMap.put("scope", "[{\"orgId\":\"4578963210\"}]");
    userRoleDetails.add(userRoleMap);
    Map<String, Object> userRoleMap1 = new HashMap<>();
    userRoleMap1.put("role", "COURSE_CREATOR");
    userRoleMap1.put("userid", "4a3ded8a-d731-4f58-a722-e63b00925cd0");
    userRoleMap1.put("scope", "[{\"orgId\":\"4578963210\"}]");
    userRoleDetails.add(userRoleMap1);
    Mockito.when(
            userRoleDao.getUserRoles(Mockito.anyString(), nullable(String.class), Mockito.any()))
        .thenReturn(userRoleDetails);

    Response response1 =
        userProfileReadService.getUserProfileData(getProfileReadRequest("1234567890"));
    Assert.assertNotNull(response1);
  }

  private Request getProfileReadRequest(String userId) {
    Request reqObj = new Request();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.REQUESTED_BY, "1234567890");
    innerMap.put(JsonKey.PRIVATE, false);
    innerMap.put(JsonKey.FIELDS, "topic,organisations,roles,locations,declarations,externalIds");
    Map<String, Object> reqMap = getUserProfileRequest(userId);
    reqObj.setRequest(reqMap);
    reqObj.setContext(innerMap);
    reqObj.setOperation(ActorOperations.GET_USER_PROFILE_V3.getValue());
    return reqObj;
  }

  private Map<String, Object> getUserProfileRequest(String userId) {
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, userId);
    reqMap.put(JsonKey.ROOT_ORG_ID, "validRootOrgId");
    return reqMap;
  }

  private Map<String, Object> getValidUserResponse(String userid) throws JsonProcessingException {
    User user = new User();
    user.setId(userid);
    user.setEmail("anyEmail@gmail.com");
    user.setChannel("channel");
    user.setPhone("9876543210");
    user.setRootOrgId("4578963210");
    user.setMaskedEmail("any****@gmail.com");
    user.setMaskedPhone("987*****0");
    user.setIsDeleted(false);
    user.setFlagsValue(3);
    user.setUserType("TEACHER");
    user.setUserId(userid);
    user.setFirstName("Demo Name");
    user.setUserName("validUserName");
    // {'groupsTnc': '{"tncAcceptedOn":"2021-01-04 19:45:29:725+0530","version":"3.9.0"}'}
    Map<String, String> tncMap = new HashMap<>();
    tncMap.put("tncAcceptedOn", "2021-01-04 19:45:29:725+0530");
    tncMap.put("version", "3.9.0");
    ObjectMapper mapper = new ObjectMapper();
    String tnc = mapper.writeValueAsString(tncMap);
    Map<String, String> groupTncMap = new HashMap<>();
    groupTncMap.put("groupsTnc", tnc);
    user.setAllTncAccepted(groupTncMap);
    ArrayList<String> locationList =
        new ArrayList<String>() {
          {
            add("location1");
            add("location2");
          }
        };
    user.setLocationIds(locationList);
    ObjectMapper mapper1 = new ObjectMapper();
    Map<String, Object> result = mapper.convertValue(user, Map.class);
    return result;
  }

  private Map<String, Object> getUserDbMap(String userid) throws JsonProcessingException {
    Map<String, Object> userDbMap = new HashMap<>();
    String[] locationIds = new String[] {"location1", "location2"};
    userDbMap.put(JsonKey.USERNAME, "validUserName");
    userDbMap.put(JsonKey.CHANNEL, "channel");
    userDbMap.put(JsonKey.EMAIL, "anyEmail@gmail.com");
    userDbMap.put(JsonKey.ROOT_ORG_ID, "4578963210");
    userDbMap.put(JsonKey.PHONE, "9876543210");
    userDbMap.put(JsonKey.FLAGS_VALUE, 3);
    userDbMap.put(JsonKey.USER_TYPE, "TEACHER");
    userDbMap.put(JsonKey.MASKED_PHONE, "987*****0");
    userDbMap.put(JsonKey.USER_ID, userid);
    userDbMap.put(JsonKey.ID, userid);
    userDbMap.put(JsonKey.FIRST_NAME, "Demo Name");
    userDbMap.put(JsonKey.IS_DELETED, false);
    userDbMap.put(JsonKey.LOCATION_IDS, locationIds);
    // {'groupsTnc': '{"tncAcceptedOn":"2021-01-04 19:45:29:725+0530","version":"3.9.0"}'}
    Map<String, String> tncMap = new HashMap<>();
    tncMap.put("tncAcceptedOn", "2021-01-04 19:45:29:725+0530");
    tncMap.put("version", "3.9.0");
    ObjectMapper mapper = new ObjectMapper();
    String tnc = mapper.writeValueAsString(tncMap);
    Map<String, String> groupTncMap = new HashMap<>();
    groupTncMap.put("groupsTnc", tnc);
    userDbMap.put(JsonKey.ALL_TNC_ACCEPTED, groupTncMap);
    return userDbMap;
  }
}
