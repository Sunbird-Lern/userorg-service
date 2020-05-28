/*
package org.sunbird.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.TableMetadata;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.helper.CassandraConnectionManagerImpl;
import org.sunbird.helper.CassandraConnectionMngrFactory;
import org.sunbird.helper.ServiceFactory;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CassandraStandaloneTest {

  private CassandraOperation operation = ServiceFactory.getInstance();
  private static Map<String, Object> address = null;
  private static Map<String, Object> dummyAddress = null;
  private static PropertiesCache cach = PropertiesCache.getInstance();
  private static String host = cach.getProperty("contactPoint");
  private static String port = cach.getProperty("port");
  private static String cassandraKeySpace = cach.getProperty("keyspace");
  private static CassandraConnectionManagerImpl connectionManager =
      (CassandraConnectionManagerImpl)
          CassandraConnectionMngrFactory.getObject(
              cach.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE));

  @BeforeClass
  public static void init() {

    address = new HashMap<>();
    address.put(JsonKey.ID, "123");
    address.put(JsonKey.ADDRESS_LINE1, "Line 1");
    address.put(JsonKey.USER_ID, "USR1");

    dummyAddress = new HashMap<>();
    dummyAddress.put(JsonKey.ID, "12345");
    dummyAddress.put(JsonKey.ADDRESS_LINE1, "Line 111");
    dummyAddress.put(JsonKey.USER_ID, "USR111");
    dummyAddress.put("DummyColumn", "USR111");

    connectionManager.createConnection(host, port, "cassandra", "password", cassandraKeySpace);
  }

  @Test
  public void testConnectionWithoutUserNameAndPassword() {
    boolean bool = connectionManager.createConnection(host, port, null, null, cassandraKeySpace);
    assertEquals(true, bool);
  }

  @Test
  public void testConnection() {
    boolean bool =
        connectionManager.createConnection(host, port, "cassandra", "password", cassandraKeySpace);
    assertEquals(true, bool);
  }

  @Test(expected = ProjectCommonException.class)
  public void testFailedConnection() {
    connectionManager.createConnection("127.0.0.1", "9042", "cassandra", "pass", "eySpace");
  }

  @Test(expected = ProjectCommonException.class)
  public void testFailedSessionCheck() {
    connectionManager.getSession("Keyspace");
  }

  @Test
  public void testAInsertOp() {
    Response response = operation.insertRecord(cassandraKeySpace, "address", address);
    assertEquals("SUCCESS", response.get("response"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testAInsertFailedOp() {
    operation.insertRecord(cassandraKeySpace, "address1", address);
  }

  @Test(expected = ProjectCommonException.class)
  public void testAInsertFailedOpWithInvalidProperty() {
    operation.insertRecord(cassandraKeySpace, "address", dummyAddress);
  }

  @Test
  public void testBUpdateOp() {
    address.put(JsonKey.CITY, "city");
    address.put(JsonKey.ADD_TYPE, "addrType");
    Response response = operation.updateRecord(cassandraKeySpace, "address", address);
    assertEquals("SUCCESS", response.get("response"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testBUpdateFailedOp() {
    dummyAddress.put(JsonKey.CITY, "city");
    dummyAddress.put(JsonKey.ADD_TYPE, "addrType");
    operation.updateRecord(cassandraKeySpace, "address1", address);
  }

  @Test(expected = ProjectCommonException.class)
  public void testBUpdateFailedOpWithInvalidProperty() {
    dummyAddress.put(JsonKey.CITY, "city");
    dummyAddress.put(JsonKey.ADD_TYPE, "addrType");
    operation.updateRecord(cassandraKeySpace, "address", dummyAddress);
  }

  @Test
  public void testBgetAllRecordsOp() {
    Response response = operation.getAllRecords(cassandraKeySpace, "address");
    assertTrue(((List<?>) response.get("response")).size() > 0);
  }

  @Test(expected = ProjectCommonException.class)
  public void testBgetAllRecordsFailedOp() {
    operation.getAllRecords(cassandraKeySpace, "Dummy Table Name");
  }

  @Test
  public void testCgetPropertiesValueByIdOp() {
    Response response =
        operation.getPropertiesValueById(
            cassandraKeySpace, "address", "123", JsonKey.ID, JsonKey.CITY, JsonKey.ADD_TYPE);
    assertTrue(
        ((String) ((List<Map<String, Object>>) response.get("response")).get(0).get(JsonKey.ID))
            .equals("123"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testCgetPropertiesValueByIdFailedOp() {
    operation.getPropertiesValueById(cassandraKeySpace, "address", "123", "Dummy Column");
  }

  @Test
  public void testDgetRecordByIdOp() {
    Response response = operation.getRecordById(cassandraKeySpace, "address", "123");
    assertTrue(
        ((String) ((List<Map<String, Object>>) response.get("response")).get(0).get(JsonKey.CITY))
            .equals("city"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testDgetRecordByIdFailedOp() {
    operation.getRecordById(cassandraKeySpace, "Dummy Table Name", "12345");
  }

  @Test
  public void testFgetRecordsByPropertiesOp() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.USER_ID, "USR1");
    map.put(JsonKey.ADD_TYPE, "addrType");
    Response response = operation.getRecordsByProperties(cassandraKeySpace, "address", map);
    assertTrue(
        ((String) ((List<Map<String, Object>>) response.get("response")).get(0).get(JsonKey.ID))
            .equals("123"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testFgetRecordsByPropertiesFailed2Op() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ADDRESS_TYPE, "add");
    map.put(JsonKey.ADDRESS_LINE1, "line1");
    List<String> list = new ArrayList<>();
    list.add("USR1");
    map.put("dummy", list);
    operation.getRecordsByProperties(cassandraKeySpace, "address", map);
  }

  @Test(expected = ProjectCommonException.class)
  public void testFgetRecordsByPropertiesFailedOp() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ADDRESS_TYPE, "add");
    map.put(JsonKey.ADDRESS_LINE1, "line1");
    operation.getRecordsByProperties(cassandraKeySpace, "address", map);
  }

  @Test
  public void testFgetRecordsByPropertyFrListOp() {
    List<Object> list = new ArrayList<>();
    list.add("123");
    list.add("321");
    Response response =
        operation.getRecordsByProperty(cassandraKeySpace, "address", JsonKey.ID, list);
    assertTrue(
        ((String) ((List<Map<String, Object>>) response.get("response")).get(0).get(JsonKey.ID))
            .equals("123"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testFgetRecordsByPropertyFrListFailedOp() {
    List<Object> list = new ArrayList<>();
    list.add("123");
    list.add("321");
    operation.getRecordsByProperty(cassandraKeySpace, "address", JsonKey.ADD_TYPE, list);
  }

  @Test
  public void testFgetRecordsByPropertyOp() {
    Response response =
        operation.getRecordsByProperty(cassandraKeySpace, "address", JsonKey.ADD_TYPE, "addrType");
    assertTrue(
        ((String) ((List<Map<String, Object>>) response.get("response")).get(0).get(JsonKey.ID))
            .equals("123"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testFgetRecordsByPropertyFailedOp() {
    operation.getRecordsByProperty(cassandraKeySpace, "address", JsonKey.ADDRESS_LINE1, "Line1");
  }

  @Test
  public void testGgetRecordByIdOp() {
    Response response = operation.getRecordById(cassandraKeySpace, "address", "123");
    assertTrue(
        ((String) ((List<Map<String, Object>>) response.get("response")).get(0).get(JsonKey.CITY))
            .equals("city"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testGgetRecordByIdOpFailed() {
    operation.getRecordById(cassandraKeySpace, "address1", "123");
  }

  @Test
  public void testHUpsertOp() {
    address.put("Country", "country");
    Response response = operation.upsertRecord(cassandraKeySpace, "address", address);
    assertEquals("SUCCESS", response.get("response"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testHUpsertOpFailed() {
    address.put("Country", "country");
    Response response = operation.upsertRecord(cassandraKeySpace, "address1", address);
    assertEquals("SUCCESS", response.get("response"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testHUpsertOpFailedWithInvalidParameter() {
    // address.put("Country", "country");
    operation.upsertRecord(cassandraKeySpace, "address", dummyAddress);
    // assertEquals("SUCCESS", response.get("response"));
  }

  @Test
  public void testZDeleteOp() {
    Response response = operation.deleteRecord(cassandraKeySpace, "address", "123");
    assertEquals("SUCCESS", response.get("response"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testZDeleteFailedOp() {
    operation.deleteRecord(cassandraKeySpace, "address1", "123");
  }

  @Test
  public void testZaDeleteFailedOp() {
    boolean bool = connectionManager.createConnection(host, port, null, null, cassandraKeySpace);
    assertTrue(bool);
  }

  @Test
  public void testZgetTableList() {
    List<String> tableList = connectionManager.getTableList(cassandraKeySpace);
    assertTrue(tableList.contains(JsonKey.USER));
  }

  @Test
  public void testZgetCluster() {
    Cluster cluster = connectionManager.getCluster(cassandraKeySpace);
    Collection<TableMetadata> tables =
        cluster.getMetadata().getKeyspace(cassandraKeySpace).getTables();
    List<String> tableList = tables.stream().map(tm -> tm.getName()).collect(Collectors.toList());
    assertTrue(tableList.contains(JsonKey.USER));
  }

  @Test(expected = ProjectCommonException.class)
  public void testZgetClusterWithInvalidKeySpace() {
    connectionManager.getCluster("sun");
  }

  @AfterClass
  public static void shutdownhook() {
    connectionManager.registerShutDownHook();
    address = null;
  }
}
*/
