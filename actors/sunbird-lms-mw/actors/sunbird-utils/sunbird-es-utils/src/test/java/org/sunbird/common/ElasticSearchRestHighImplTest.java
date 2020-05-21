package org.sunbird.common;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.util.concurrent.FutureUtils;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ConnectionManager;
import scala.concurrent.Future;

/**
 * Test class for Elastic search Rest High level client Impl
 *
 * @author github.com/iostream04
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
@PrepareForTest({
  ConnectionManager.class,
  RestHighLevelClient.class,
  AcknowledgedResponse.class,
  GetRequestBuilder.class,
  HttpUtil.class,
  BulkProcessor.class,
  FutureUtils.class,
  SearchHit.class,
  SearchHits.class,
  Aggregations.class,
  ElasticSearchHelper.class
})
public class ElasticSearchRestHighImplTest {

  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  private static RestHighLevelClient client = null;

  @Before
  public void initBeforeTest() {
    mockBaseRules();
    mockRulesForSave(false);
  }

  @Test
  public void testSaveSuccess() {
    mockRulesForSave(false);
    Future<String> result = esService.save("test", "001", new HashMap<>());
    String res = (String) ElasticSearchHelper.getResponseFromFuture(result);
    assertEquals("001", res);
  }

  @Test
  public void testSaveFailureWithEmptyIndex() {

    Future<String> result = esService.save("", "001", new HashMap<>());
    String res = (String) ElasticSearchHelper.getResponseFromFuture(result);
    assertEquals("ERROR", res);
  }

  @Test
  public void testSaveFailureWithEmptyIdentifier() {
    Future<String> result = esService.save("test", "", new HashMap<>());
    String res = (String) ElasticSearchHelper.getResponseFromFuture(result);
    assertEquals("ERROR", res);
  }

  @Test
  public void testSaveFailure() {
    mockRulesForSave(true);
    Future<String> result = esService.save("test", "001", new HashMap<>());
    String res = (String) ElasticSearchHelper.getResponseFromFuture(result);
    assertEquals(null, res);
  }

  @Test
  public void testUpdateSuccess() {
    mockRulesForUpdate(false);
    Future<Boolean> result = esService.update("test", "001", new HashMap<>());
    boolean res = (boolean) ElasticSearchHelper.getResponseFromFuture(result);
    assertEquals(true, res);
  }

  @Test
  public void testUpdateFailure() {
    mockRulesForUpdate(true);
    Future<Boolean> result = esService.update("test", "001", new HashMap<>());
    Object res = ElasticSearchHelper.getResponseFromFuture(result);
    assertEquals(null, res);
  }

  @Test
  public void testUpdateFailureWithEmptyIndex() {
    try {
      esService.update("", "001", new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), ResponseCode.invalidData.getResponseCode());
    }
  }

  @Test
  public void testUpdateFailureWithEmptyIdentifier() {
    try {
      esService.update("test", "", new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), ResponseCode.invalidData.getResponseCode());
    }
  }

  @Test
  public void testGetDataByIdentifierFailureWithEmptyIndex() {
    try {
      esService.getDataByIdentifier("", "001");
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), ResponseCode.invalidData.getResponseCode());
    }
  }

  @Test
  public void testGetDataByIdentifierFailureWithEmptyIdentifier() {
    try {
      esService.getDataByIdentifier("test", "");
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), ResponseCode.invalidData.getResponseCode());
    }
  }

  @Test
  public void testGetDataByIdentifierFailure() {
    mockRulesForGet(true);
    Future<Map<String, Object>> result = esService.getDataByIdentifier("test", "001");
    Object res = ElasticSearchHelper.getResponseFromFuture(result);
    assertEquals(null, res);
  }

  @Test
  public void testDeleteSuccess() {
    mockRulesForDelete(false, false);
    Future<Boolean> result = esService.delete("test", "001");
    boolean res = (boolean) ElasticSearchHelper.getResponseFromFuture(result);
    assertEquals(true, res);
  }

  @Test
  public void testDeleteSuccessWithoutDelete() {
    mockRulesForDelete(false, true);
    Future<Boolean> result = esService.delete("test", "001");
    boolean res = (boolean) ElasticSearchHelper.getResponseFromFuture(result);
    assertEquals(false, res);
  }

  @Test
  public void testDeleteFailure() {
    mockRulesForDelete(true, false);
    Future<Boolean> result = esService.delete("test", "001");
    Object res = ElasticSearchHelper.getResponseFromFuture(result);
    assertEquals(null, res);
  }

  @Test
  public void testDeleteFailureWithEmptyIdentifier() {
    try {
      esService.delete("test", "");
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), ResponseCode.invalidData.getResponseCode());
    }
  }

  @Test
  public void testDeleteFailureWithEmptyIndex() {
    try {
      esService.delete("", "001");
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), ResponseCode.invalidData.getResponseCode());
    }
  }

  @Test
  public void testUpsertSuccess() {
    mockRulesForUpdate(false);
    Future<Boolean> result = esService.update("test", "001", new HashMap<>());
    boolean res = (boolean) ElasticSearchHelper.getResponseFromFuture(result);
    assertEquals(true, res);
  }

  @Test
  public void testUpsertFailure() {
    mockRulesForUpdate(true);
    Future<Boolean> result = esService.update("test", "001", new HashMap<>());
    Object res = ElasticSearchHelper.getResponseFromFuture(result);
    assertEquals(null, res);
  }

  @Test
  public void testUpsertFailureWithEmptyIndex() {
    try {
      esService.update("", "001", new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), ResponseCode.invalidData.getResponseCode());
    }
  }

  @Test
  public void testUpsertFailureWithEmptyIdentifier() {
    try {
      esService.update("test", "", new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), ResponseCode.invalidData.getResponseCode());
    }
  }

  @Test
  public void testBuilInsertSuccess() {
    mockRulesForBulk(false);
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.IDENTIFIER, "0001");
    list.add(map);
    Future<Boolean> result = esService.bulkInsert("test", list);
    boolean res = (boolean) ElasticSearchHelper.getResponseFromFuture(result);
    assertEquals(true, res);
  }

  @Test
  public void testBuilInsertFailure() {
    mockRulesForBulk(true);
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.IDENTIFIER, "0001");
    list.add(map);
    Future<Boolean> result = esService.bulkInsert("test", list);
    boolean res = (boolean) ElasticSearchHelper.getResponseFromFuture(result);
    assertEquals(false, res);
  }

  private void mockBaseRules() {
    client = mock(RestHighLevelClient.class);
    PowerMockito.mockStatic(ConnectionManager.class);
    try {
      doNothing().when(ConnectionManager.class, "registerShutDownHook");
    } catch (Exception e) {
      Assert.fail("Initialization of test case failed due to " + e.getLocalizedMessage());
    }
    when(ConnectionManager.getRestClient()).thenReturn(client);
  }

  private static void mockRulesForBulk(boolean fail) {
    Iterator<BulkItemResponse> itr = mock(Iterator.class);

    BulkResponse response = mock(BulkResponse.class);
    when(response.iterator()).thenReturn(itr);
    when(itr.hasNext()).thenReturn(false);

    if (!fail) {
      doAnswer(
              new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                  ((ActionListener<BulkResponse>) invocation.getArguments()[1])
                      .onResponse(response);
                  return null;
                }
              })
          .when(client)
          .bulkAsync(Mockito.any(), Mockito.any());
    } else {

      doAnswer(
              new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {

                  ((ActionListener<BulkResponse>) invocation.getArguments()[1])
                      .onFailure(new NullPointerException());
                  return null;
                }
              })
          .when(client)
          .bulkAsync(Mockito.any(), Mockito.any());
    }
  }

  private static void mockRulesForSave(boolean fail) {
    IndexResponse ir = mock(IndexResponse.class);
    when(ir.getId()).thenReturn("001");

    if (!fail) {

      doAnswer(
              new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                  ((ActionListener<IndexResponse>) invocation.getArguments()[1]).onResponse(ir);
                  return null;
                }
              })
          .when(client)
          .indexAsync(Mockito.any(), Mockito.any());
    } else {

      doAnswer(
              new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {

                  ((ActionListener<IndexResponse>) invocation.getArguments()[1])
                      .onFailure(new NullPointerException());
                  return null;
                }
              })
          .when(client)
          .indexAsync(Mockito.any(), Mockito.any());
    }
  }

  @SuppressWarnings("rawtypes")
  private static void mockRulesForUpdate(boolean fail) {
    UpdateResponse updateRes = mock(UpdateResponse.class);
    when(updateRes.getResult()).thenReturn(null);

    if (!fail) {

      doAnswer(
              new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                  ((ActionListener<UpdateResponse>) invocation.getArguments()[1])
                      .onResponse(updateRes);
                  return null;
                }
              })
          .when(client)
          .updateAsync(Mockito.any(), Mockito.any());
    } else {

      doAnswer(
              new Answer() {
                @SuppressWarnings("unchecked")
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {

                  ((ActionListener<UpdateResponse>) invocation.getArguments()[1])
                      .onFailure(new NullPointerException());
                  return null;
                }
              })
          .when(client)
          .updateAsync(Mockito.any(), Mockito.any());
    }
  }

  @SuppressWarnings("rawtypes")
  private static void mockRulesForGet(boolean fail) {
    GetResponse getResponse = mock(GetResponse.class);
    Map<String, Object> map = new HashMap<>();
    map.put("test", "any");
    when(getResponse.getSourceAsMap()).thenReturn(map);
    when(getResponse.isExists()).thenReturn(true);

    if (!fail) {

      doAnswer(
              new Answer() {
                @SuppressWarnings("unchecked")
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                  ((ActionListener<GetResponse>) invocation.getArguments()[1])
                      .onResponse(getResponse);
                  return null;
                }
              })
          .when(client)
          .getAsync(Mockito.any(), Mockito.any());
    } else {

      doAnswer(
              new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {

                  ((ActionListener<GetResponse>) invocation.getArguments()[1])
                      .onFailure(new NullPointerException());
                  return null;
                }
              })
          .when(client)
          .getAsync(Mockito.any(), Mockito.any());
    }
  }

  private static void mockRulesForDelete(boolean fail, boolean notFound) {
    DeleteResponse delResponse = mock(DeleteResponse.class);

    if (!fail) {
      if (notFound) {
        when(delResponse.getResult()).thenReturn(DocWriteResponse.Result.NOT_FOUND);
      }

      doAnswer(
              new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                  ((ActionListener<DeleteResponse>) invocation.getArguments()[1])
                      .onResponse(delResponse);
                  return null;
                }
              })
          .when(client)
          .deleteAsync(Mockito.any(), Mockito.any());
    } else {

      doAnswer(
              new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {

                  ((ActionListener<DeleteResponse>) invocation.getArguments()[1])
                      .onFailure(new NullPointerException());
                  return null;
                }
              })
          .when(client)
          .deleteAsync(Mockito.any(), Mockito.any());
    }
  }
}
