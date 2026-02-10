package org.sunbird.cassandraimpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.DataType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.CassandraPropertyReader;
import org.sunbird.common.Constants;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

/**
 * Unit tests for {@link CassandraOperationImpl}.
 * Uses Mockito and Reflection to mock dependencies like Cassandra Session and static Singletons.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CassandraOperationImpl.class})
public class CassandraOperationImplTest {

  private CassandraOperationImpl cassandraOperation;

  @Mock private CassandraConnectionManager connectionManager;

  @Mock private Session session;

  @Mock private PreparedStatement preparedStatement;

  @Mock private ColumnDefinitions columnDefinitions;

  @Mock private ResultSet resultSet;

  @Mock private RequestContext requestContext;

  @Mock private CassandraPropertyReader propertyReader;

  @Mock private BoundStatement boundStatement;

  @Before
  public void setUp() throws Exception {
    // Inject Mock ConnectionManager into Factory using Reflection
    setSingletonInstance(CassandraConnectionMngrFactory.class, "instance", connectionManager);

    // Inject Mock PropertyReader into Factory using Reflection
    setSingletonInstance(CassandraPropertyReader.class, "cassandraPropertyReader", propertyReader);
    when(propertyReader.readProperty(anyString())).thenAnswer(i -> i.getArgument(0));
    when(propertyReader.readPropertyValue(anyString())).thenAnswer(i -> i.getArgument(0));

    // Initialize concrete implementation
    cassandraOperation = new CassandraOperationImplConcrete();
    // Inject connection manager into the operation instance
    setField(cassandraOperation, "connectionManager", connectionManager);

    // Setup basic session behavior
    when(connectionManager.getSession(anyString())).thenReturn(session);
    when(session.prepare(anyString())).thenReturn(preparedStatement);

    // Setup PreparedStatement to allow BoundStatement creation (mocking real driver behavior)
    when(preparedStatement.getVariables()).thenReturn(columnDefinitions);
    when(columnDefinitions.size()).thenReturn(10);

    // Setup BoundStatement binding
    when(preparedStatement.bind()).thenReturn(boundStatement);
    when(preparedStatement.bind(any())).thenReturn(boundStatement); // Catch-all for varargs
    when(boundStatement.bind(any())).thenReturn(boundStatement);

    // Mock execution
    when(session.execute(any(BoundStatement.class))).thenReturn(resultSet);
    when(session.execute(any(Statement.class))).thenReturn(resultSet);

    // Setup ResultSet to return success
    when(resultSet.iterator()).thenReturn(Collections.emptyIterator());
    when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
    when(columnDefinitions.asList()).thenReturn(Collections.emptyList());
    when(columnDefinitions.getType(anyInt())).thenReturn(DataType.text());

    // Mock BoundStatement constructor to return our mock
    PowerMockito.whenNew(BoundStatement.class).withAnyArguments().thenReturn(boundStatement);
  }

  private void setSingletonInstance(Class<?> clazz, String fieldName, Object instance)
      throws Exception {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(null, instance);
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  @Test
  public void testInsertRecordSuccess() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    Map<String, Object> request = new HashMap<>();
    request.put("id", "123");
    request.put("name", "John");
    when(columnDefinitions.size()).thenReturn(request.size());

    Response response =
        cassandraOperation.insertRecord(keyspaceName, tableName, request, requestContext);

    assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    verify(session, times(1)).execute(any(BoundStatement.class));
  }

  @Test
  public void testUpdateRecordSuccess() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    Map<String, Object> request = new HashMap<>();
    request.put("id", "123");
    request.put("name", "John");

    Response response =
        cassandraOperation.updateRecord(keyspaceName, tableName, request, requestContext);

    assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    verify(session, times(1)).execute(any(BoundStatement.class));
  }

  @Test
  public void testDeleteRecordSuccess() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    String identifier = "123";

    Response response =
        cassandraOperation.deleteRecord(keyspaceName, tableName, identifier, requestContext);

    assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    verify(session, times(1)).execute(any(Statement.class));
  }

  @Test
  public void testGetRecordById() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    String identifier = "123";

    Response response =
        cassandraOperation.getRecordById(keyspaceName, tableName, identifier, requestContext);

    assertNotNull(response);
    List<?> result = (List<?>) response.get(Constants.RESPONSE);
    assertEquals(0, result.size());
  }

  @Test
  public void testGetRecordsByProperty() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    String propertyName = "name";
    String propertyValue = "John";

    Response response =
        cassandraOperation.getRecordsByProperty(
            keyspaceName, tableName, propertyName, propertyValue, requestContext);

    assertNotNull(response);
    List<?> result = (List<?>) response.get(Constants.RESPONSE);
    assertEquals(0, result.size());
  }

  @Test
  public void testBatchInsert() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    List<Map<String, Object>> records = new ArrayList<>();
    Map<String, Object> record1 = new HashMap<>();
    record1.put("id", "1");
    records.add(record1);

    Response response =
        cassandraOperation.batchInsert(keyspaceName, tableName, records, requestContext);

    assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    // batchInsert uses session.execute(BatchStatement) which is a Statement
    verify(session, times(1)).execute(any(Statement.class));
  }

  @Test
  public void testBatchUpdate() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    List<Map<String, Map<String, Object>>> list = new ArrayList<>();
    Map<String, Map<String, Object>> record = new HashMap<>();
    Map<String, Object> pk = new HashMap<>();
    pk.put("id", "1");
    Map<String, Object> nonPk = new HashMap<>();
    nonPk.put("name", "updated");
    record.put(JsonKey.PRIMARY_KEY, pk);
    record.put(JsonKey.NON_PRIMARY_KEY, nonPk);
    list.add(record);

    Response response =
        cassandraOperation.batchUpdate(keyspaceName, tableName, list, requestContext);

    assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    verify(session, times(1)).execute(any(Statement.class));
  }

  @Test
  public void testUpsertRecord() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    Map<String, Object> request = new HashMap<>();
    request.put("id", "123");
    when(columnDefinitions.size()).thenReturn(request.size());

    Response response =
        cassandraOperation.upsertRecord(keyspaceName, tableName, request, requestContext);

    assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    verify(session, times(1)).execute(any(BoundStatement.class));
  }

  @Test
  public void testDeleteRecordsBulk() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    List<String> ids = new ArrayList<>();
    ids.add("1");
    ids.add("2");

    when(resultSet.wasApplied()).thenReturn(true);

    boolean result =
        cassandraOperation.deleteRecords(keyspaceName, tableName, ids, requestContext);

    assertEquals(true, result);
    verify(session, times(1)).execute(any(Statement.class));
  }

  @Test
  public void testGetRecordsByProperties() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    Map<String, Object> properties = new HashMap<>();
    properties.put("name", "John");

    Response response =
        cassandraOperation.getRecordsByProperties(
            keyspaceName, tableName, properties, requestContext);

    assertNotNull(response);
    verify(session, times(1)).execute(any(Statement.class));
  }

  @Test
  public void testInsertRecordWithTTL() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    Map<String, Object> request = new HashMap<>();
    request.put("id", "123");
    int ttl = 100;

    Response response =
        cassandraOperation.insertRecordWithTTL(
            keyspaceName, tableName, request, ttl, requestContext);

    // Expecting empty list because CassandraUtil.createResponse returns based on ResultSet rows (empty here)
    // insertRecordWithTTL does NOT explicitly set SUCCESS like insertRecord does.
    assertNotNull(response);
    List<?> result = (List<?>) response.get(Constants.RESPONSE);
    assertEquals(0, result.size());

    verify(session, times(1)).execute(any(Statement.class));
  }

  @Test
  public void testUpdateRecordWithTTL() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    Map<String, Object> request = new HashMap<>();
    request.put("name", "John");
    Map<String, Object> compositeKey = new HashMap<>();
    compositeKey.put("id", "123");
    int ttl = 100;

    Response response =
        cassandraOperation.updateRecordWithTTL(
            keyspaceName, tableName, request, compositeKey, ttl, requestContext);

    // Expecting empty list, similar to insertRecordWithTTL
    assertNotNull(response);
    List<?> result = (List<?>) response.get(Constants.RESPONSE);
    assertEquals(0, result.size());

    verify(session, times(1)).execute(any(Statement.class));
  }

  @Test
  public void testBatchInsertLogged() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    List<Map<String, Object>> records = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put("id", "1");
    records.add(record);

    Response response =
        cassandraOperation.batchInsertLogged(keyspaceName, tableName, records, requestContext);

    assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    verify(session, times(1)).execute(any(Statement.class)); // BatchStatement extends Statement
  }

  @Test
  public void testGetRecordsByCompositeKey() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    Map<String, Object> compositeKey = new HashMap<>();
    compositeKey.put("id", "123");
    compositeKey.put("type", "admin");

    Response response =
        cassandraOperation.getRecordsByCompositeKey(
            keyspaceName, tableName, compositeKey, requestContext);

    assertNotNull(response);
    verify(session, times(1)).execute(any(Statement.class));
  }

  @Test
  public void testGetRecordsByIdsWithSpecifiedColumns() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    List<String> properties = new ArrayList<>();
    properties.add("name");
    List<String> ids = new ArrayList<>();
    ids.add("1");

    Response response =
        cassandraOperation.getRecordsByIdsWithSpecifiedColumns(
            keyspaceName, tableName, properties, ids, requestContext);

    assertNotNull(response);
    verify(session, times(1)).execute(any(Statement.class));
  }

  @Test
  public void testSearchValueInList() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    String key = "roles";
    String value = "admin";

    Response response =
        cassandraOperation.searchValueInList(keyspaceName, tableName, key, value, requestContext);

    assertNotNull(response);
    verify(session, times(1)).execute(any(Statement.class));
  }

  @Test(expected = ProjectCommonException.class)
  public void testInsertRecordFailure() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    Map<String, Object> request = new HashMap<>();
    request.put("id", "123");

    when(session.execute(any(BoundStatement.class)))
        .thenThrow(new RuntimeException("DB Error"));

    cassandraOperation.insertRecord(keyspaceName, tableName, request, requestContext);
  }

  @Test
  public void testInsertRecordFailureUnknownIdentifier() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    Map<String, Object> request = new HashMap<>();
    request.put("id", "123");
    when(columnDefinitions.size()).thenReturn(request.size());

    // Simulate "Unknown identifier" exception
    when(session.execute(any(BoundStatement.class)))
        .thenThrow(new RuntimeException("Unknown identifier column_x"));

    try {
      cassandraOperation.insertRecord(keyspaceName, tableName, request, requestContext);
      fail("Should throw ProjectCommonException");
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.invalidPropertyError.getErrorCode(), e.getErrorCode());
    }
  }

  // Concrete implementation for testing abstract class
  private static class CassandraOperationImplConcrete extends CassandraOperationImpl {
    @Override
    public Response getRecordsWithLimit(
        String keyspace,
        String table,
        Map<String, Object> filters,
        List<String> fields,
        Integer limit,
        RequestContext requestContext) {
      return null;
    }

    @Override
    public Response updateAddMapRecord(
        String keySpace,
        String table,
        Map<String, Object> primaryKey,
        String column,
        String key,
        Object value,
        RequestContext requestContext) {
      return null;
    }

    @Override
    public Response updateRemoveMapRecord(
        String keySpace,
        String table,
        Map<String, Object> primaryKey,
        String column,
        String key,
        RequestContext requestContext) {
      return null;
    }
  }
}