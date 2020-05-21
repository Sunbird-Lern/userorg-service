package org.sunbird.badge.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.sunbird.badge.model.BadgeClassExtension;
import org.sunbird.badge.service.BadgeClassExtensionService;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CassandraOperationImpl.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class BadgeClassExtensionServiceImplTest {
  private CassandraOperation mockDBService;
  private BadgeClassExtensionService badgeClassExtensionServiceImpl;

  private static final String VALUE_BADGE_ID = "java-se-8-programmer";
  private static final String VALUE_ISSUER_ID = "oracle-university";
  private static final String VALUE_ROOT_ORG_ID = "AP";
  private static final String VALUE_TYPE = "user";
  private static final String VALUE_SUBTYPE = "award";
  private static final ArrayList<String> VALUE_ROLES_LIST =
      new ArrayList<>(Arrays.asList("roleId1"));

  @Before
  public void setUp() {
    mockDBService = PowerMockito.mock(CassandraOperationImpl.class);
    badgeClassExtensionServiceImpl = new BadgeClassExtensionServiceImpl(mockDBService);
  }

  @Test
  public void testSaveSuccess() {
    PowerMockito.when(mockDBService.upsertRecord(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(new Response());

    boolean thrown = false;

    try {
      BadgeClassExtension badgeClassExtension =
          new BadgeClassExtension(
              VALUE_BADGE_ID,
              VALUE_ISSUER_ID,
              VALUE_ROOT_ORG_ID,
              VALUE_TYPE,
              VALUE_SUBTYPE,
              VALUE_ROLES_LIST);
      badgeClassExtensionServiceImpl.save(badgeClassExtension);
    } catch (Exception e) {
      thrown = true;
    }

    Assert.assertEquals(false, thrown);
  }

  @Test
  public void testGetSuccess() {
    Response response = new Response();
    response.put(
        JsonKey.RESPONSE, new ArrayList<Map<String, Object>>(Arrays.asList(new HashMap<>())));

    PowerMockito.when(
            mockDBService.getRecordById(Mockito.any(), Mockito.any(), Mockito.anyString()))
        .thenReturn(response);

    BadgeClassExtension badgeClassExtension = badgeClassExtensionServiceImpl.get(VALUE_BADGE_ID);

    Assert.assertTrue(null != badgeClassExtension);
  }

  @Test
  public void testSearchSuccessNonEmpty() {
    HashMap<String, Object> badgeMap = new HashMap<>();
    badgeMap.put(JsonKey.ID, VALUE_BADGE_ID);
    badgeMap.put(BadgingJsonKey.ISSUER_ID, VALUE_ISSUER_ID);
    badgeMap.put(JsonKey.ROOT_ORG_ID, VALUE_ROOT_ORG_ID);
    badgeMap.put(JsonKey.TYPE, VALUE_TYPE);
    badgeMap.put(JsonKey.SUBTYPE, VALUE_SUBTYPE);
    badgeMap.put(JsonKey.ROLES, VALUE_ROLES_LIST);

    Response response = new Response();
    response.put(JsonKey.RESPONSE, new ArrayList<Map<String, Object>>(Arrays.asList(badgeMap)));

    PowerMockito.when(
            mockDBService.getRecordsByProperties(Mockito.any(), Mockito.any(), Mockito.anyMap()))
        .thenReturn(response);

    List<BadgeClassExtension> badgeClassExtList =
        badgeClassExtensionServiceImpl.search(
            new ArrayList<>(),
            new ArrayList<>(),
            VALUE_ROOT_ORG_ID,
            VALUE_TYPE,
            VALUE_SUBTYPE,
            VALUE_ROLES_LIST);

    Assert.assertTrue(null != badgeClassExtList);
    Assert.assertEquals(1, badgeClassExtList.size());
    Assert.assertEquals(VALUE_BADGE_ID, badgeClassExtList.get(0).getBadgeId());
    Assert.assertEquals(VALUE_ROOT_ORG_ID, badgeClassExtList.get(0).getRootOrgId());
    Assert.assertEquals(VALUE_TYPE, badgeClassExtList.get(0).getType());
    Assert.assertEquals(VALUE_SUBTYPE, badgeClassExtList.get(0).getSubtype());
    Assert.assertEquals(VALUE_ROLES_LIST, badgeClassExtList.get(0).getRoles());
  }

  @Test
  public void testSearchSuccessEmpty() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, new ArrayList<Map<String, Object>>());

    PowerMockito.when(
            mockDBService.getRecordsByProperties(Mockito.any(), Mockito.any(), Mockito.anyMap()))
        .thenReturn(response);

    List<BadgeClassExtension> badgeClassExtList =
        badgeClassExtensionServiceImpl.search(
            new ArrayList<>(),
            new ArrayList<>(),
            VALUE_ROOT_ORG_ID,
            VALUE_TYPE,
            VALUE_SUBTYPE,
            VALUE_ROLES_LIST);

    Assert.assertTrue(null != badgeClassExtList);
    Assert.assertEquals(0, badgeClassExtList.size());
  }

  @Test
  public void testGetFailureBadgeListNull() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, new ArrayList<Map<String, Object>>());

    PowerMockito.when(
            mockDBService.getRecordById(Mockito.any(), Mockito.any(), Mockito.anyString()))
        .thenReturn(new Response());

    boolean thrown = false;

    try {
      badgeClassExtensionServiceImpl.get(VALUE_BADGE_ID);
    } catch (ProjectCommonException exception) {
      thrown = true;
      Assert.assertEquals(
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode(), exception.getResponseCode());
    }

    Assert.assertEquals(true, thrown);
  }

  @Test
  public void testGetFailureBadgeListEmpty() {

    Response response = new Response();
    response.put(JsonKey.RESPONSE, new ArrayList<Map<String, Object>>());

    PowerMockito.when(
            mockDBService.getRecordById(Mockito.any(), Mockito.any(), Mockito.anyString()))
        .thenReturn(response);

    boolean thrown = false;

    try {
      badgeClassExtensionServiceImpl.get(VALUE_BADGE_ID);
    } catch (ProjectCommonException exception) {
      thrown = true;
      Assert.assertEquals(
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode(), exception.getResponseCode());
    }

    Assert.assertEquals(true, thrown);
  }

  @Test
  public void testDeleteSuccess() {
    PowerMockito.when(
            mockDBService.deleteRecord(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(new Response());

    boolean thrown = false;

    try {
      badgeClassExtensionServiceImpl.delete(VALUE_BADGE_ID);
    } catch (Exception e) {
      thrown = true;
    }

    Assert.assertEquals(false, thrown);
  }
}
