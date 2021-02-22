package org.sunbird.learner.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.validator.location.LocationRequestValidator;
import scala.concurrent.Promise;

import javax.lang.model.element.PackageElement;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ServiceFactory.class,
        Util.class,
        ElasticSearchRestHighImpl.class,
        ProjectUtil.class,
        LocationRequestValidator.class,
        EsClientFactory.class
})
@PowerMockIgnore({
        "javax.management.*",
        "javax.net.ssl.*",
        "javax.security.*",
        "jdk.internal.reflect.*",
        "javax.crypto.*",
        "javax.script.*",
        "javax.xml.*",
        "com.sun.org.apache.xerces.*",
        "org.xml.*"
})

public class OrgManagementActorTest {

  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(OrganisationManagementActor.class);
  private static CassandraOperationImpl cassandraOperation;
  private static Map<String, Object> basicRequestData;
  private static final String ADD_MEMBER_TO_ORG =
          ActorOperations.ADD_MEMBER_ORGANISATION.getValue();
  private static final String GET_ORG_TYPE_LIST =
          ActorOperations.GET_ORG_TYPE_LIST.getValue();
  private static final String CREATE_ORG_TYPE =
          ActorOperations.CREATE_ORG_TYPE.getValue();
  private static final String GET_ORG_DETAILS = ActorOperations.GET_ORG_DETAILS.getValue();
  private static final String REMOVE_MEMBER_FROM_ORG =
          ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue();
  private static ElasticSearchService esService;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(Util.class);
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.mockStatic(EsClientFactory.class);

     cassandraOperation = mock(CassandraOperationImpl.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    basicRequestData = getBasicData();
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getEsResponse(false));
    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    when(cassandraOperation.getAllRecords(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.any()))
            .thenReturn(getAllRecords());
      when(cassandraOperation.getAllRecords(
              Mockito.anyString(),Mockito.anyString(),Mockito.any()))
              .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.deleteRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
            .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getRecordsByProperty(false))
            .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
            .thenReturn(getRecordsByProperty(false))
            .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getRecordsByProperty(false));

    when(Util.validateRoles(Mockito.anyList())).thenReturn("SUCCESS");
    when(Util.encryptData(Mockito.anyString())).thenReturn("userExtId");
    when(ProjectUtil.getUniqueIdFromTimestamp(Mockito.anyInt())).thenReturn("time");
    when(ProjectUtil.getFormattedDate()).thenReturn("date");
    when(ProjectUtil.getConfigValue(GeoLocationJsonKey.SUNBIRD_VALID_LOCATION_TYPES))
            .thenReturn("dummy");
    when(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_API_REQUEST_LOWER_CASE_FIELDS))
            .thenReturn("lowercase");
    PowerMockito.mockStatic(LocationRequestValidator.class);
  }

 // @Test
  public void testUpdateOrgStatus() {
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getRecordsByProperty(false));
    Request reqObj = new Request();
    Map<String, Object> requestData = new HashMap<>();
//    requestData.put(JsonKey.REQUESTED_BY, "as23-12asd234-123");
    requestData.put(JsonKey.ORGANISATION_ID, "orgId");
    requestData.put(JsonKey.STATUS,1);
    reqObj.setRequest(requestData);
    reqObj.setOperation(ActorOperations.UPDATE_ORG_STATUS.getValue());
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
            .thenReturn(getRecordsByProperty(false));
    boolean result = testScenario(reqObj,null);
    assertTrue(result);
  }


  //created
  @Test
  public void testUpdateOrgStatusFailure() {
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getRecordsByProperty(false));
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ORG_ID,"orgId");
    boolean result = testScenario(getRequest(map,ActorOperations.UPDATE_ORG_STATUS.getValue()),ResponseCode.invalidRequestData);
    assertTrue(result);
  }

  //created
//  @Test
  public void testUpdateOrgStatusSuccess() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getRecordsByProperty(false));
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ORG_ID,"OrgId");
    map.put(JsonKey.CHANNEL,"channel");
    map.put(JsonKey.EXTERNAL_ID,"externalId");
    map.put(JsonKey.STATUS,"0");
    boolean result = testScenario(getRequest(map, ActorOperations.UPDATE_ORG_STATUS.getValue()),null);
    assertTrue(result);
  }

  @Test
  public void testUpdateOrgTypeFailureWithExistingTypeName() {
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getSuccess());
    Request reqObj = new Request();
    Map<String, Object> requestData = new HashMap<>();
    requestData.put(JsonKey.ID, "as23-12asd234-123");
    requestData.put(JsonKey.NAME, "orgType");
    reqObj.setRequest(requestData);
    reqObj.setOperation(ActorOperations.UPDATE_ORG_TYPE.getValue());
    boolean result = testScenario(reqObj, ResponseCode.orgTypeAlreadyExist);
    assertTrue(result);
  }



  //created
  @Test
  public void testUpdateOrgTypeSuccess() {
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getSuccess());
    Request reqObj = new Request();
    Map<String, Object> requestData = new HashMap<>();
    requestData.put(JsonKey.ID, "1");
    requestData.put(JsonKey.NAME, "local");
    reqObj.setRequest(requestData);
    reqObj.setOperation(ActorOperations.UPDATE_ORG_TYPE.getValue());
    boolean result = testScenario(reqObj, null);
    assertTrue(result);
  }


 //  @Test
  public void testAddUserToOrgSuccessWithUserIdAndOrgId() {
       when(cassandraOperation.getRecordById(
               Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
               .thenReturn(getRecordsByProperty(false))
               .thenReturn(getRecordsByProperty(false));
       when(cassandraOperation.getRecordsByCompositeKey(
               Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
               .thenReturn(getRecordsByProperty(true));
       when(cassandraOperation.updateRecord(
               Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
               .thenReturn(getRecordsByProperty(false));
       when(cassandraOperation.insertRecord(
               Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
               .thenReturn(getSuccess());
       Map<String, Object> map = new HashMap<>();
       map.put(JsonKey.ORG_ID,"0131630445447741440");
       map.put(JsonKey.PROVIDER, "channel1004");
       map.put(JsonKey.EXTERNAL_ID, "externalId");
       map.put(JsonKey.USER_ID, "91b4a9e5-2eb1-4aa0-8f0a-ce171ed5ff3e");
       map.put(JsonKey.ROLES,"ADMIN");
       map.put(JsonKey.ORGANISATION_ID,"organisationId");
       boolean result = testScenario(getRequest(map,ActorOperations.ADD_MEMBER_ORGANISATION.getValue()),null);
    assertTrue(result);
  }

  @Test
  public void testAddUserToOrgFailureWithBlankUserIdAndOrgId() {
    Map<String, Object> reqMap = getRequestData(false, true, false, false, basicRequestData);
    Request request = getRequest(reqMap, ADD_MEMBER_TO_ORG);
    boolean result = testScenario(request, ResponseCode.usrValidationError);
    assertTrue(result);
  }

  @Test
  public void testAddUserToOrgFailureWithUserIdAndBlankOrgId() {
    Map<String, Object> reqMap = getRequestData(true, false, false, true, basicRequestData);
    reqMap.remove(JsonKey.PROVIDER);
    Request request = getRequest(reqMap, ADD_MEMBER_TO_ORG);
    boolean result = testScenario(request, ResponseCode.sourceAndExternalIdValidationError);
    assertTrue(result);
  }


  @Test
  public void testAddUserToOrgFailureWithInvalidOrg() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getEsResponse(true));
    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    boolean result =
            testScenario(
                    getRequest(
                            getRequestData(true, true, true, true, basicRequestData), ADD_MEMBER_TO_ORG),
                    ResponseCode.invalidOrgData);
    assertTrue(result);
  }

  @Test
  public void testAddUserToOrgFailureWithOrgNotFoundWithOrgId() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getEsResponse(true));
    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    boolean result =
            testScenario(
                    getRequest(
                            getRequestData(true, false, true, true, basicRequestData), ADD_MEMBER_TO_ORG),
                    ResponseCode.invalidOrgData);
    assertTrue(result);
  }

  @Test
  public void testAddUserToOrgFailureWithUserNotFoundWithUserExtId() {

    boolean result =
            testScenario(
                    getRequest(
                            getRequestData(false, false, true, true, basicRequestData), ADD_MEMBER_TO_ORG),
                    ResponseCode.invalidUsrData);
    assertTrue(result);
  }

  @Test
  public void testAddUserToOrgFailureWithOrgNotFoundWithOrgExtId() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getEsResponse(true));
    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    boolean result =
            testScenario(
                    getRequest(
                            getRequestData(true, false, true, true, basicRequestData), ADD_MEMBER_TO_ORG),
                    ResponseCode.invalidOrgData);
    assertTrue(result);
  }



  //created
  @Test
  public void testGetOrgDetailsFailWithoutExistingOrg() {
    Map<String, Object> reqMap = getRequestData(false, false, false, true, basicRequestData);
    Request request = getRequest(reqMap, GET_ORG_DETAILS);
    boolean result = testScenario(request, ResponseCode.orgDoesNotExist);
    assertTrue(result);
  }

  //created
  @Test
  public void testRemoveMemberOrgFailureWithUserInactive() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    Map<String,Object> map = getRequestDataForOrgUpdate();
    map.put(JsonKey.USER_ID,"userId");
    boolean result = testScenario(getRequest(map,ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue()),ResponseCode.userInactiveForThisOrg);
    assertTrue(result);
  }

  //created
  //  @Test
  public void testRemoveMemberOrgSuccess() {
        when(cassandraOperation.getRecordsByCompositeKey(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(getRecordsByProperty(false));
//        when(cassandraOperation.getAllRecords(
//                Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.any()))
//                .thenReturn(getAllRecords());
      when(cassandraOperation.deleteRecord(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
              .thenReturn(getRecordsByProperty(false));
    Map<String,Object> map = new HashMap<>();
    map.put(JsonKey.USER_ID,"userId");
    map.put(JsonKey.ORGANISATION_ID,"orgId");
    boolean result = testScenario(getRequest(map,ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue()),null);
    assertTrue(result);
  }


  //created
  @Test
  public void testCreateOrgSuccess() {
      when(cassandraOperation.getRecordsByCompositeKey(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
              .thenReturn(getRecordsByProperty(true));
      when(cassandraOperation.insertRecord(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
              .thenReturn(getSuccess());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    Map<String, Object> map = getRequestDataForOrgCreate(basicRequestData);
    map.remove(JsonKey.EXTERNAL_ID);
    boolean result = testScenario(getRequest(map,ActorOperations.CREATE_ORG.getValue()),null);
    assertTrue(result);
  }

  //created
  @Test
  public void testCreateOrgTypeSuccess() {
    when(cassandraOperation.getAllRecords(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
            .thenReturn(getAllRecords());
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getSuccess());
    Map<String,Object> map = new HashMap<>();
    map.put(JsonKey.NAME,"type");
    boolean result = testScenario(getRequest(map,ActorOperations.CREATE_ORG_TYPE.getValue()),null);
    assertTrue(result);
  }


  //created
 @Test
  public void testGetOrgTypeListSuccess() {
     when(cassandraOperation.getAllRecords(
             Mockito.anyString(), Mockito.anyString(), Mockito.any()))
             .thenReturn(getAllRecords());
      boolean result =
              testScenario(
                      getRequest(
                              getRequestData(false, true, false, true, basicRequestData), GET_ORG_TYPE_LIST),
                      null);
      assertTrue(result);
  }



  //created
  @Test
  public void testCreateOrgFailureWithChannelAlreadyUsed() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    Map<String, Object> map = getRequestDataForOrgCreate(basicRequestData);
    map.put(JsonKey.IS_ROOT_ORG, true);
    boolean result = testScenario(getRequest(map,ActorOperations.CREATE_ORG.getValue()),ResponseCode.channelUniquenessInvalid);
    assertTrue(result);
  }

   @Test
  public void testCreateOrgSuccessWithExternalIdAndProvider() {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getRecordsByProperty(true));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getSuccess());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    Map<String, Object> map = getRequestDataForOrgCreate(basicRequestData);
    map.put(JsonKey.CHANNEL,"channel1004");
    boolean result =
            testScenario(
                    getRequest(
                            map,
                            ActorOperations.CREATE_ORG.getValue()),
                    null);
    assertTrue(result);
  }


  @Test
  public void testCreateOrgSuccessWithoutExternalIdAndProvider() {

      when(cassandraOperation.getRecordsByCompositeKey(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
              .thenReturn(getRecordsByProperty(true));
      when(cassandraOperation.insertRecord(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
              .thenReturn(getSuccess());

    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    Map<String, Object> map = getRequestDataForOrgCreate(basicRequestData);
    map.remove(JsonKey.EXTERNAL_ID);
    map.remove(JsonKey.PROVIDER);
    boolean result = testScenario(getRequest(map, ActorOperations.CREATE_ORG.getValue()), null);
    assertTrue(result);
  }

  @Test
  public void testCreateOrgFailureWithoutChannel() {
      when(cassandraOperation.getRecordsByCompositeKey(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
              .thenReturn(getRecordsByProperty(true));
      when(cassandraOperation.insertRecord(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
              .thenReturn(getSuccess());
    Map<String, Object> map = getRequestDataForOrgCreate(basicRequestData);
    map.remove(JsonKey.CHANNEL);
    boolean result =
            testScenario(
                    getRequest(map, ActorOperations.CREATE_ORG.getValue()),
                    ResponseCode.mandatoryParamsMissing);
    assertTrue(result);
  }

  //created - unchecked
   @Test
  public void testCreateOrgFailureWithoutExtId() {
       when(cassandraOperation.getRecordsByCompositeKey(
               Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
               .thenReturn(getRecordsByProperty(true));
       when(cassandraOperation.insertRecord(
               Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
               .thenReturn(getSuccess());
    Map<String, Object> map = getRequestDataForOrgCreate(basicRequestData);
    map.remove(JsonKey.EXTERNAL_ID);
    boolean result =
            testScenario(
                    getRequest(map, ActorOperations.CREATE_ORG.getValue()),
                    ResponseCode.externalIdNotFound);
    assertFalse(result);
  }

  //created
  @Test
  public void testCreateOrgFailureWithDefaultValues() {
//      when(cassandraOperation.getRecordsByCompositeKey(
//              Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
//              .thenReturn(getRecordsByProperty(true));
      when(cassandraOperation.insertRecord(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
              .thenReturn(getSuccess());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    Map<String,Object> map = getRequestDataForOrgCreate(basicRequestData);
    boolean result = testScenario(getRequest(map,ActorOperations.CREATE_ORG.getValue()),ResponseCode.errorDuplicateEntry);
    assertTrue(result);
  }

  //created
  //  @Test
    public void testCreateOrgFailureWithSlugIsNotUnique() {
        when(cassandraOperation.getRecordsByCompositeKey(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(getRecordsByProperty(true));
        when(cassandraOperation.insertRecord(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(getSuccess());
        Promise<Map<String, Object>> promise = Futures.promise();
        promise.success(getValidateChannelEsResponse(false));

        when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
                .thenReturn(promise.future());
        Map<String,Object> map = getRequestDataForOrgCreate(basicRequestData);
        boolean result = testScenario(getRequest(map,ActorOperations.CREATE_ORG.getValue()),ResponseCode.slugIsNotUnique);
        assertTrue(result);
    }

  //created
  @Test
  public void testCreateOrgFailureWithEmailFormatError() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    Map<String,Object> map = getRequestDataForOrgCreate(basicRequestData);
    map.remove(JsonKey.EXTERNAL_ID);
    map.put(JsonKey.EMAIL,"abc");
    boolean result = testScenario(getRequest(map,ActorOperations.CREATE_ORG.getValue()),ResponseCode.emailFormatError);
    assertTrue(result);
  }

  @Test
  public void testUpdateOrgSuccess() {

    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getRecordsByProperty(false));
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    boolean result =
            testScenario(
                    getRequest(getRequestDataForOrgUpdate(), ActorOperations.UPDATE_ORG.getValue()), null);
    assertTrue(result);
  }


  @Test
  public void testUpdateOrgFailureWithDuplicateChannel() {

    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getRecordsByProperty(false));
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    Map<String, Object> map = getRequestDataForOrgUpdate();
    map.put(JsonKey.IS_ROOT_ORG, true);
    boolean result =
            testScenario(
                    getRequest(map, ActorOperations.UPDATE_ORG.getValue()),
                    ResponseCode.channelUniquenessInvalid);
    assertTrue(result);
  }

  //created
  @Test     //unchecked
  public void testUpdateOrgSuccessWithInvalidChannel() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(false));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    Map<String, Object> map = getRequestDataForOrgUpdate();
    map.put(JsonKey.IS_ROOT_ORG, true);
    boolean result = testScenario(getRequest(map,ActorOperations.UPDATE_ORG.getValue()),null);
    assertTrue(result);
  }

  //created
   @Test
  public void testUpdateOrgFailureWithInvalidData() {
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getRecordsByProperty(false))
            .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getRecordsByProperty(false));
     when(cassandraOperation.insertRecord(
             Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
             .thenReturn(getRecordsByProperty(false));
    Promise<Map<String, Object>> promise = Futures.promise();
//    promise.success(getValidateOrgEsResponse(false));
     promise.success(getValidateChannelEsResponse(true));
//     promise.success(getValidateExternalIdEsResponse(false))

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
    Map<String, Object> map = getRequestDataForOrgUpdate();
    map.put(JsonKey.IS_ROOT_ORG,true);
    map.put(JsonKey.EXTERNAL_ID,"externalId");
    boolean result = testScenario(getRequest(map,ActorOperations.UPDATE_ORG.getValue()),ResponseCode.channelUniquenessInvalid);
    assertTrue(result);
  }
  //created
  @Test
  public void testUpdateOrgFailureWithoutChannel() {
    Map<String, Object> map = getRequestDataForOrgUpdate();
    map.remove(JsonKey.CHANNEL);
    boolean result = testScenario(getRequest(map,ActorOperations.UPDATE_ORG.getValue()),ResponseCode.invalidChannel);
    assertTrue(result);
  }



  private Response getOrgStatus() {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.STATUS, 1);
    map.put(JsonKey.ID, "id");
    list.add(map);
    res.put(JsonKey.RESPONSE, list);
    return res;
  }

  private Response getSuccess() {
    Response res = new Response();
    res.setResponseCode(ResponseCode.OK);
    return res;
  }

  private Map<String, Object> getRequestDataForOrgUpdate() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CHANNEL, "channel1");
    map.put(JsonKey.ORGANISATION_ID, "orgId");
    return map;
  }

    private Map<String, Object> getRequestDataForMemberRemoveFromOrg() {
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKey.USER_ID,"userId");
        map.put(JsonKey.ORGANISATION_ID, "orgId");
        map.put(JsonKey.STATUS, 1);
        return map;
    }

  private Map<String, Object> getRequestDataForOrgCreate(Map<String, Object> map) {
    map.put(JsonKey.CHANNEL, "channel");
    map.put(JsonKey.IS_ROOT_ORG, false);
    map.put(JsonKey.EXTERNAL_ID, "externalId");
    return map;
  }


  private Map<String, Object> getRequestData(
          boolean userId, boolean orgId, boolean userExtId, boolean OrgExtId, Map<String, Object> map) {
    List<String> rolesList = new ArrayList<>();
    rolesList.add("dummyRole");
    map.put(JsonKey.ROLES, rolesList);
    if (userId) {
      map.put(JsonKey.USER_ID, "userId");
    }
    if (orgId) {
      map.put(JsonKey.ORGANISATION_ID, "orgId");
    }
    if (userExtId) {
      map.put(JsonKey.USER_EXTERNAL_ID, "userExtId");
    }
    if (OrgExtId) {
      map.put(JsonKey.EXTERNAL_ID, "externalId");
    }
    return map;
  }

  private Response getRecordsByProperty(boolean empty) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    if (!empty) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, "userId");
      map.put(JsonKey.IS_DELETED, true);
      list.add(map);
    }
    res.put(JsonKey.RESPONSE, list);
    return res;
  }

  private Response getAllRecords() {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "id");
    map.put(JsonKey.NAME, "orgType");
    list.add(map);
    res.put(JsonKey.RESPONSE, list);
    return res;
  }

  private Map<String, Object> getEsResponse(boolean empty) {
    Map<String, Object> response = new HashMap<>();
    List<Map<String, Object>> contentList = new ArrayList<>();
    if (!empty) {
      Map<String, Object> content = new HashMap<>();
      content.put(JsonKey.ORGANISATION_ID, "orgId");
      content.put(JsonKey.HASHTAGID, "hashtagId");
      contentList.add(content);
    }
    response.put(JsonKey.CONTENT, contentList);
    return response;
  }

  private Map<String, Object> getValidateChannelEsResponse(boolean isValidChannel) {
    Map<String, Object> response = new HashMap<>();
    List<Map<String, Object>> contentList = new ArrayList<>();
    if (isValidChannel) {
      Map<String, Object> content = new HashMap<>();
      content.put(JsonKey.STATUS, 1);
      content.put(JsonKey.ID, "id");
      contentList.add(content);
    }
    response.put(JsonKey.CONTENT, contentList);
    return response;
  }

  private Map<String, Object> getValidateOrgEsResponse(boolean isValidOrg) {
    Map<String, Object> response = new HashMap<>();
    List<Map<String, Object>> contentList = new ArrayList<>();
    if (isValidOrg) {
      Map<String, Object> content = new HashMap<>();
      content.put(JsonKey.STATUS, 1);
      content.put(JsonKey.ID, "id");
      contentList.add(content);
    }
    else {
      Map<String, Object> content = new HashMap<>();
      content.put(JsonKey.STATUS, 0);
      contentList.add(content);
    }
    response.put(JsonKey.CONTENT, contentList);
    return response;
  }

  private boolean testScenario(Request request, ResponseCode errorCode) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(request, probe.getRef());

    if (errorCode == null) {
      Response res = probe.expectMsgClass(duration("100 second"), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
              probe.expectMsgClass(duration("100 second"), ProjectCommonException.class);
      return res.getCode().equals(errorCode.getErrorCode())
              || res.getResponseCode() == errorCode.getResponseCode();
    }
  }

  private Request getRequest(Map<String, Object> requestData, String actorOperation) {
    Request reqObj = new Request();
    reqObj.setRequest(requestData);
    reqObj.setOperation(actorOperation);
    return reqObj;
  }

  private Map<String, Object> getBasicData() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.PROVIDER, "provider");
    map.put(JsonKey.USER_PROVIDER, "userProvider");
    map.put(JsonKey.USER_ID_TYPE, "userIdType");
    return map;
  }
}