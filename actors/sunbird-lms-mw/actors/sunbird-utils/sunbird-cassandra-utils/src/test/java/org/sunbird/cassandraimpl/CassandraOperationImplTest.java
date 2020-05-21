package org.sunbird.cassandraimpl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.reset;
import static org.powermock.api.mockito.PowerMockito.when;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.*;
import com.google.common.util.concurrent.Uninterruptibles;
import java.text.MessageFormat;
import java.util.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.Constants;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.CassandraConnectionManagerImpl;
import org.sunbird.helper.CassandraConnectionMngrFactory;
import org.sunbird.helper.ServiceFactory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
/** @author kirti. Junit test cases */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
  Cluster.class,
  Uninterruptibles.class,
  PreparedStatement.class,
  BoundStatement.class,
  Session.class,
  Metadata.class,
  CassandraConnectionMngrFactory.class,
  ResultSet.class,
  CassandraUtil.class,
  Cluster.Builder.class,
  Select.class,
  Row.class,
  ColumnDefinitions.class,
  String.class,
  Select.Where.class,
  Select.Builder.class,
  QueryBuilder.class,
  Select.Selection.class,
  Delete.Where.class,
  Delete.Selection.class,
  Update.class,
  Update.Assignments.class,
  Update.Where.class,
  Using.class,
  Iterator.class,
  Row.class,
  Select.SelectionOrAlias.class
})
@PowerMockIgnore("javax.management.*")
public class CassandraOperationImplTest {

  private static Iterator iterator;
  private static Row row;
  private static Cluster cluster;
  private static Update update;
  private static Session session = PowerMockito.mock(Session.class);
  private static PreparedStatement statement;
  private static ResultSet resultSet;
  private static Select selectQuery;
  private static Select.Where where;
  private static Delete.Where deleteWhere;
  private static Select.Builder selectBuilder;
  private static Metadata metadata;
  private static CassandraOperation operation;
  private static Map<String, Object> address = null;
  private static Map<String, Object> dummyAddress = null;
  private static PropertiesCache cach = PropertiesCache.getInstance();
  private static String host = cach.getProperty("contactPoint");
  private static String port = cach.getProperty("port");
  private static String cassandraKeySpace = cach.getProperty("keyspace");
  private static final Cluster.Builder builder = PowerMockito.mock(Cluster.Builder.class);
  private static BoundStatement boundStatement;
  private static Select.Selection selectSelection;
  private static Delete.Selection deleteSelection;
  private static Delete delete;

  private static KeyspaceMetadata keyspaceMetadata;
  private static Map<String, Object> otp = null;
  private static CassandraConnectionManagerImpl connectionManager =
      (CassandraConnectionManagerImpl)
          CassandraConnectionMngrFactory.getObject(
              cach.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE));

  @BeforeClass
  public static void init() {
    PowerMockito.mockStatic(Select.SelectionOrAlias.class);
    PowerMockito.mockStatic(Using.class);
    PowerMockito.mockStatic(Cluster.class);
    PowerMockito.mockStatic(Update.Assignments.class);
    PowerMockito.mockStatic(Update.Where.class);
    iterator = PowerMockito.mock(Iterator.class);
    cluster = PowerMockito.mock(Cluster.class);
    update = PowerMockito.mock(Update.class);
    when(cluster.connect(Mockito.anyString())).thenReturn(session);
    metadata = PowerMockito.mock(Metadata.class);
    when(cluster.getMetadata()).thenReturn(metadata);
    when(Cluster.builder()).thenReturn(builder);
    when(builder.addContactPoint(Mockito.anyString())).thenReturn(builder);
    when(builder.withPort(Mockito.anyInt())).thenReturn(builder);
    when(builder.withProtocolVersion(Mockito.any())).thenReturn(builder);
    when(builder.withRetryPolicy(Mockito.any())).thenReturn(builder);
    when(builder.withTimestampGenerator(Mockito.any())).thenReturn(builder);
    when(builder.withPoolingOptions(Mockito.any())).thenReturn(builder);
    when(builder.build()).thenReturn(cluster);
    connectionManager.createConnection(host, port, "cassandra", "password", cassandraKeySpace);
  }

  @Before
  public void setUp() throws Exception {

    reset(session);
    address = new HashMap<>();
    address.put(JsonKey.ID, "123");
    address.put(JsonKey.ADDRESS_LINE1, "Line 1");
    address.put(JsonKey.USER_ID, "USR1");

    otp = new HashMap<>();
    otp.put(JsonKey.TYPE, "email");
    otp.put(JsonKey.KEY, "amit@example.com");
    otp.put(JsonKey.OTP, "987456");
    otp.put(JsonKey.CREATED_ON, System.currentTimeMillis());

    dummyAddress = new HashMap<>();
    dummyAddress.put(JsonKey.ID, "12345");
    dummyAddress.put(JsonKey.ADDRESS_LINE1, "Line 111");
    dummyAddress.put(JsonKey.USER_ID, "USR111");
    dummyAddress.put("DummyColumn", "USR111");

    statement = PowerMockito.mock(PreparedStatement.class);
    selectQuery = PowerMockito.mock(Select.class);
    where = PowerMockito.mock(Select.Where.class);
    selectBuilder = PowerMockito.mock(Select.Builder.class);
    PowerMockito.mockStatic(QueryBuilder.class);
    selectSelection = PowerMockito.mock(Select.Selection.class);
    deleteSelection = PowerMockito.mock(Delete.Selection.class);
    deleteWhere = PowerMockito.mock(Delete.Where.class);
    delete = PowerMockito.mock(Delete.class);
    operation = ServiceFactory.getInstance();
    resultSet = PowerMockito.mock(ResultSet.class);
    keyspaceMetadata = PowerMockito.mock(KeyspaceMetadata.class);
    Update.Assignments assignments = PowerMockito.mock(Update.Assignments.class);
    when(QueryBuilder.select()).thenReturn(selectSelection);
    Update.Where where2 = PowerMockito.mock(Update.Where.class);
    when(update.where()).thenReturn(where2);
    when(QueryBuilder.update(cassandraKeySpace, "otp")).thenReturn(update);
    when(update.with()).thenReturn(assignments);
    when(deleteSelection.from(Mockito.anyString(), Mockito.anyString())).thenReturn(delete);
    when(delete.where(QueryBuilder.eq(Constants.IDENTIFIER, "123"))).thenReturn(deleteWhere);
    when(selectQuery.where()).thenReturn(where);
    when(metadata.getKeyspace("sunbird")).thenReturn(keyspaceMetadata);
    when(cluster.connect(Mockito.anyString())).thenReturn(session);
    boundStatement = PowerMockito.mock(BoundStatement.class);
    PowerMockito.whenNew(BoundStatement.class)
        .withArguments(Mockito.any(PreparedStatement.class))
        .thenReturn(boundStatement);
    when(session.prepare(Mockito.anyString())).thenReturn(statement);
    when(selectSelection.all()).thenReturn(selectBuilder);
    when(selectBuilder.from(Mockito.anyString(), Mockito.anyString())).thenReturn(selectQuery);
    when(session.execute(selectQuery)).thenReturn(resultSet);

    ColumnDefinitions cd = PowerMockito.mock(ColumnDefinitions.class);
    String str = "qwertypower(king";
    when(resultSet.getColumnDefinitions()).thenReturn(cd);
    when(cd.toString()).thenReturn(str);
    when(str.substring(8, resultSet.getColumnDefinitions().toString().length() - 1))
        .thenReturn(str);
  }

  // @Test
  public void testInsertRecordSuccess() throws Exception {
    statement = PowerMockito.mock(PreparedStatement.class);
    boundStatement = PowerMockito.mock(BoundStatement.class);
    PowerMockito.whenNew(BoundStatement.class).withArguments(statement).thenReturn(boundStatement);
    // when(session.execute(boundStatement.bind("123"))).thenReturn(resultSet);
    Response response = operation.insertRecord(cassandraKeySpace, "address1", address);
    assertEquals(ResponseCode.success.getErrorCode(), response.get("response"));
  }

  // @Test
  public void testInsertRecordFailure() throws Exception {
    Object[] array = new Object[2];
    when(session.execute(boundStatement.bind(array)))
        .thenThrow(
            new ProjectCommonException(
                ResponseCode.dbInsertionError.getErrorCode(),
                ResponseCode.dbInsertionError.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode()));

    Throwable exception = null;
    try {
      operation.insertRecord(cassandraKeySpace, "address", address);
    } catch (Exception ex) {
      exception = ex;
    }
    assertEquals(ResponseCode.dbInsertionError.getErrorMessage(), exception.getMessage());
  }

  // @Test
  public void testInsertRecordFailureWithInvalidProperty() throws Exception {

    when(session.execute(boundStatement.bind("123")))
        .thenThrow(
            new ProjectCommonException(
                ResponseCode.invalidPropertyError.getErrorCode(),
                JsonKey.UNKNOWN_IDENTIFIER,
                ResponseCode.CLIENT_ERROR.getResponseCode()));

    Throwable exception = null;
    try {
      operation.insertRecord(cassandraKeySpace, "address", address);
    } catch (Exception exp) {
      exception = exp;
    }
    Object[] args = {""};
    assertEquals(
        new MessageFormat(ResponseCode.invalidPropertyError.getErrorMessage()).format(args),
        exception.getMessage());
  }

  @Test
  public void testUpdateRecordSuccess() {

    address.put(JsonKey.CITY, "city");
    address.put(JsonKey.ADD_TYPE, "addrType");

    when(session.execute(boundStatement)).thenReturn(resultSet);
    Response response = operation.updateRecord(cassandraKeySpace, "address", address);
    assertEquals(ResponseCode.success.getErrorCode(), response.get("response"));
  }

  @Test
  public void testUpdateRecordWithTTLSuccess() {
    when(resultSet.iterator()).thenReturn(iterator);
    Map<String, Object> compositeKey = new HashMap<>();
    compositeKey.put(JsonKey.TYPE, JsonKey.EMAIL);
    compositeKey.put(JsonKey.KEY, "amit@example.com");
    when(session.execute(update)).thenReturn(resultSet);
    Response response =
        operation.updateRecordWithTTL(cassandraKeySpace, "otp", otp, compositeKey, 120);
    assertNotNull(response.get("response"));
  }

  @Test
  public void testUpdateRecordFailure() throws Exception {

    dummyAddress.put(JsonKey.CITY, "city");
    dummyAddress.put(JsonKey.ADD_TYPE, "addrType");

    when(session.prepare(Mockito.anyString()))
        .thenThrow(
            new ProjectCommonException(
                ResponseCode.dbUpdateError.getErrorCode(),
                ResponseCode.dbUpdateError.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode()));

    Throwable exception = null;
    try {
      operation.updateRecord(cassandraKeySpace, "address", dummyAddress);

    } catch (Exception ex) {
      exception = ex;
    }
    assertEquals(ResponseCode.dbUpdateError.getErrorMessage(), exception.getMessage());
  }

  @Test
  public void testUpdateRecordFailureWithInvalidProperty() throws Exception {

    dummyAddress.put(JsonKey.CITY, "city");
    dummyAddress.put(JsonKey.ADD_TYPE, "addrType");

    when(session.prepare(Mockito.anyString()))
        .thenThrow(
            new ProjectCommonException(
                ResponseCode.invalidPropertyError.getErrorCode(),
                JsonKey.UNKNOWN_IDENTIFIER,
                ResponseCode.CLIENT_ERROR.getResponseCode()));

    Throwable exception = null;
    try {
      operation.updateRecord(cassandraKeySpace, "address", dummyAddress);
    } catch (Exception exp) {
      exception = exp;
    }
    Object[] args = {""};
    assertEquals(
        new MessageFormat(ResponseCode.invalidPropertyError.getErrorMessage()).format(args),
        exception.getMessage());
  }

  @Test
  public void testGetAllRecordsSuccess() throws Exception {
    Iterator<Row> rowItr = Mockito.mock(Iterator.class);
    Mockito.when(resultSet.iterator()).thenReturn(rowItr);
    PowerMockito.whenNew(BoundStatement.class)
        .withArguments(Mockito.any(PreparedStatement.class))
        .thenReturn(boundStatement);

    Response response = operation.getAllRecords(cassandraKeySpace, "address");
    assertTrue(response.getResult().size() > 0);
  }

  @Test
  public void testGetAllRecordsFailure() throws Exception {

    when(session.execute(selectQuery))
        .thenThrow(
            new ProjectCommonException(
                ResponseCode.SERVER_ERROR.getErrorCode(),
                ResponseCode.SERVER_ERROR.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode()));

    List<Row> rows = new ArrayList<>();
    Row row = Mockito.mock(Row.class);
    rows.add(row);
    when(resultSet.all()).thenReturn(rows);

    PowerMockito.whenNew(BoundStatement.class)
        .withArguments(Mockito.any(PreparedStatement.class))
        .thenReturn(boundStatement);

    Throwable exception = null;
    try {
      operation.getAllRecords(cassandraKeySpace, "address");
    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((ProjectCommonException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getResponseCode());
  }

  // @Test
  public void testGetPropertiesValueSuccessById() throws Exception {
    Iterator<Row> rowItr = Mockito.mock(Iterator.class);
    Mockito.when(resultSet.iterator()).thenReturn(rowItr);
    when(session.execute(boundStatement.bind("123"))).thenReturn(resultSet);
    PowerMockito.whenNew(BoundStatement.class)
        .withArguments(Mockito.any(PreparedStatement.class))
        .thenReturn(boundStatement);
    Response response =
        operation.getPropertiesValueById(
            cassandraKeySpace, "address", "123", JsonKey.ID, JsonKey.CITY, JsonKey.ADD_TYPE);
    assertTrue(response.getResult().size() > 0);
  }

  @Test
  public void testGetPropertiesValueFailureById() throws Exception {

    Throwable exception = null;
    PowerMockito.whenNew(BoundStatement.class)
        .withArguments(Mockito.any(PreparedStatement.class))
        .thenThrow(
            new ProjectCommonException(
                ResponseCode.SERVER_ERROR.getErrorCode(),
                ResponseCode.SERVER_ERROR.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode()));

    try {
      operation.getPropertiesValueById(
          cassandraKeySpace, "address", "123", JsonKey.ID, JsonKey.CITY, JsonKey.ADD_TYPE);
    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((ProjectCommonException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getResponseCode());
  }

  @Test
  public void testGetRecordSuccessById() {
    Iterator<Row> rowItr = Mockito.mock(Iterator.class);
    Mockito.when(resultSet.iterator()).thenReturn(rowItr);
    when(session.execute(boundStatement.bind("123"))).thenReturn(resultSet);
    when(session.execute(where)).thenReturn(resultSet);
    when(selectBuilder.from(Mockito.anyString(), Mockito.anyString())).thenReturn(selectQuery);
    when(selectSelection.all()).thenReturn(selectBuilder);

    Response response = operation.getRecordById(cassandraKeySpace, "address", "123");
    assertTrue(response.getResult().size() > 0);
  }

  @Test
  public void testGetRecordWithTTLById() {
    Select.SelectionOrAlias alias = PowerMockito.mock(Select.SelectionOrAlias.class);
    Select select = PowerMockito.mock(Select.class);
    when(selectSelection.from("sunbird", "otp")).thenReturn(select);
    when(select.where()).thenReturn(where);
    when(selectSelection.ttl("otp")).thenReturn(alias);
    when(alias.as("otp_ttl")).thenReturn(selectSelection);
    Iterator<Row> rowItr = Mockito.mock(Iterator.class);
    Mockito.when(resultSet.iterator()).thenReturn(rowItr);
    when(session.execute(where)).thenReturn(resultSet);
    when(selectBuilder.from(Mockito.anyString(), Mockito.anyString())).thenReturn(selectQuery);
    when(selectSelection.all()).thenReturn(selectBuilder);
    Map<String, Object> key = new HashMap<>();
    key.put(JsonKey.TYPE, JsonKey.EMAIL);
    key.put(JsonKey.KEY, "amit@example.com");
    List<String> ttlFields = new ArrayList<>();
    ttlFields.add(JsonKey.OTP);
    List<String> fields = new ArrayList<>();
    fields.add(JsonKey.CREATED_ON);
    fields.add(JsonKey.TYPE);
    fields.add(JsonKey.OTP);
    fields.add(JsonKey.KEY);
    Response response =
        operation.getRecordWithTTLById(cassandraKeySpace, "otp", key, ttlFields, fields);
    assertTrue(response.getResult().size() > 0);
  }

  @Test
  public void testGetRecordFailureById() throws Exception {

    Throwable exception = null;
    PowerMockito.whenNew(BoundStatement.class)
        .withArguments(Mockito.any(PreparedStatement.class))
        .thenThrow(
            new ProjectCommonException(
                ResponseCode.SERVER_ERROR.getErrorCode(),
                ResponseCode.SERVER_ERROR.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode()));

    try {
      operation.getRecordById(cassandraKeySpace, "address", "123");
    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((ProjectCommonException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getResponseCode());
  }

  @Test
  public void testGetRecordSuccessByProperties() throws Exception {

    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.USER_ID, "USR1");
    map.put(JsonKey.ADD_TYPE, "addrType");

    when(session.execute(boundStatement.bind("123"))).thenReturn(resultSet);
    Iterator<Row> rowItr = Mockito.mock(Iterator.class);
    Mockito.when(resultSet.iterator()).thenReturn(rowItr);

    Response response = operation.getRecordsByProperties(cassandraKeySpace, "address", map);
    assertTrue(response.getResult().size() > 0);
  }

  @Test
  public void testGetRecordFailureByProperties() throws Exception {

    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.USER_ID, "USR1");
    map.put(JsonKey.ADD_TYPE, "addrType");

    List<Row> rows = new ArrayList<>();
    Row row = Mockito.mock(Row.class);
    rows.add(row);
    when(resultSet.all()).thenReturn(rows);

    when(selectSelection.all())
        .thenThrow(
            new ProjectCommonException(
                ResponseCode.SERVER_ERROR.getErrorCode(),
                ResponseCode.SERVER_ERROR.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode()));

    Throwable exception = null;
    try {
      operation.getRecordsByProperties(cassandraKeySpace, "address", map);
    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((ProjectCommonException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getResponseCode());
  }

  @Test
  public void testGetRecordForListSuccessByProperties() throws Exception {

    List<Object> list = new ArrayList<>();
    list.add("123");
    list.add("321");

    when(session.execute(boundStatement.bind("123"))).thenReturn(resultSet);
    Iterator<Row> rowItr = Mockito.mock(Iterator.class);
    Mockito.when(resultSet.iterator()).thenReturn(rowItr);
    Response response =
        operation.getRecordsByProperty(cassandraKeySpace, "address", JsonKey.ID, list);
    assertTrue(response.getResult().size() > 0);
  }

  @Test
  public void testGetRecordForListFailreByProperties() throws Exception {

    List<Object> list = new ArrayList<>();
    list.add("123");
    list.add("321");

    List<Row> rows = new ArrayList<>();
    Row row = Mockito.mock(Row.class);
    rows.add(row);
    when(resultSet.all()).thenReturn(rows);

    when(selectSelection.all())
        .thenThrow(
            new ProjectCommonException(
                ResponseCode.SERVER_ERROR.getErrorCode(),
                ResponseCode.SERVER_ERROR.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode()));

    Throwable exception = null;
    try {
      operation.getRecordsByProperty(cassandraKeySpace, "address", JsonKey.ID, list);
    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((ProjectCommonException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getResponseCode());
  }

  @Test
  public void testGetRecordsSuccessByProperty() throws Exception {

    List<Row> rows = new ArrayList<>();
    Row row = Mockito.mock(Row.class);
    rows.add(row);
    when(resultSet.all()).thenReturn(rows);
    Iterator<Row> rowItr = Mockito.mock(Iterator.class);
    Mockito.when(resultSet.iterator()).thenReturn(rowItr);
    when(session.execute(boundStatement.bind("123"))).thenReturn(resultSet);
    Response response =
        operation.getRecordsByProperty(cassandraKeySpace, "address", JsonKey.ADD_TYPE, "addrType");
    assertTrue(response.getResult().size() > 0);
  }

  @Test
  public void testGetRecordsFailureByProperty() throws Exception {

    List<Row> rows = new ArrayList<>();
    Row row = Mockito.mock(Row.class);
    rows.add(row);
    when(resultSet.all()).thenReturn(rows);

    when(selectSelection.all())
        .thenThrow(
            new ProjectCommonException(
                ResponseCode.SERVER_ERROR.getErrorCode(),
                ResponseCode.SERVER_ERROR.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode()));

    Throwable exception = null;
    try {
      operation.getRecordsByProperty(cassandraKeySpace, "address", JsonKey.ADD_TYPE, "addrType");
    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((ProjectCommonException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getResponseCode());
  }

  @Test
  public void testGetRecordsSuccessById() {
    Iterator<Row> rowItr = Mockito.mock(Iterator.class);
    Mockito.when(resultSet.iterator()).thenReturn(rowItr);
    when(session.execute(where)).thenReturn(resultSet);
    when(selectSelection.all()).thenReturn(selectBuilder);

    Response response = operation.getRecordById(cassandraKeySpace, "address", "123");
    assertTrue(response.getResult().size() > 0);
  }

  @Test
  public void testGetRecordsFailureById() throws Exception {

    List<Row> rows = new ArrayList<>();
    Row row = Mockito.mock(Row.class);
    rows.add(row);
    when(resultSet.all()).thenReturn(rows);

    when(selectSelection.all())
        .thenThrow(
            new ProjectCommonException(
                ResponseCode.SERVER_ERROR.getErrorCode(),
                ResponseCode.SERVER_ERROR.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode()));

    Throwable exception = null;
    try {
      operation.getRecordById(cassandraKeySpace, "address1", "123");

    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((ProjectCommonException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getResponseCode());
  }

  @Test
  public void testDeleteRecordSuccess() throws Exception {

    when(QueryBuilder.delete()).thenReturn(deleteSelection);
    Response response = new Response();
    response.put(JsonKey.RESPONSE, Constants.SUCCESS);
    operation.deleteRecord(cassandraKeySpace, "address", "123");
    assertEquals("SUCCESS", response.get("response"));
  }

  @Test
  public void testDeleteRecordFailure() {

    when(QueryBuilder.delete())
        .thenThrow(
            new ProjectCommonException(
                ResponseCode.SERVER_ERROR.getErrorCode(),
                ResponseCode.SERVER_ERROR.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode()));

    Throwable exception = null;
    try {
      operation.deleteRecord(cassandraKeySpace, "address", "123");

    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((ProjectCommonException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getResponseCode());
  }

  @Test
  public void testGetTableListSuccess() throws Exception {

    Collection<TableMetadata> tables = new ArrayList<>();
    TableMetadata table = Mockito.mock(TableMetadata.class);
    tables.add(table);
    when(keyspaceMetadata.getTables()).thenReturn(tables);

    List<String> tableList = connectionManager.getTableList(cassandraKeySpace);
    assertTrue(tableList.size() > 0);
  }

  @Test
  public void testGetClusterSuccess() throws Exception {

    Cluster cluster = connectionManager.getCluster("sunbird");
    assertTrue(cluster != null);
  }

  @Test
  public void testGetClusterFailureWithInvalidKeySpace() {

    Throwable exception = null;
    try {
      connectionManager.getCluster("sun");
    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue("cassandra cluster value is null for this sun".equals(exception.getMessage()));
  }
}
