package org.sunbird.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class CassandraUtilTest {

  @Test
  public void testGetPreparedStatement() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", "123");
    map.put("name", "John");
    map.put("email", "john@example.com");

    String query = CassandraUtil.getPreparedStatement(keyspaceName, tableName, map);

    // Expected format: INSERT INTO sunbird.user(id,name,email) VALUES (?,?,?);
    // Since map iteration order might vary if not LinkedHashMap, checking for containment is safer.

    assertTrue(query.startsWith("INSERT INTO sunbird.user("));
    assertTrue(query.contains("id"));
    assertTrue(query.contains("name"));
    assertTrue(query.contains("email"));
    assertTrue(query.contains(") VALUES (?,?,?);"));
  }

  @Test
  public void testGetUpdateQueryStatement() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", "123");
    map.put("name", "John");
    map.put("email", "john@example.com");

    String query = CassandraUtil.getUpdateQueryStatement(keyspaceName, tableName, map);

    // Expected format: UPDATE sunbird.user SET ... = ? ... where id = ?

    assertTrue(query.startsWith("UPDATE sunbird.user SET "));
    assertTrue(query.contains("name = ?"));
    assertTrue(query.contains("email = ?"));
    assertTrue(query.contains("where id = ?"));
  }

  @Test
  public void testGetSelectStatement() {
    String keyspaceName = "sunbird";
    String tableName = "user";
    List<String> properties = new ArrayList<>();
    properties.add("id");
    properties.add("name");

    String query = CassandraUtil.getSelectStatement(keyspaceName, tableName, properties);

    // Using string concatenation in assertion might fail due to spaces, so be careful.
    // The implementation:
    // query.append(Constants.FROM + keyspaceName + Constants.DOT + tableName + Constants.WHERE + Constants.IDENTIFIER + Constants.EQUAL + " ?; ");
    // Constants.FROM = " FROM "
    // Constants.WHERE = " where "
    // Constants.EQUAL = " = "

    String expected = "SELECT id,name FROM sunbird.user where id =  ?; ";
    assertEquals(expected, query);
  }

  @Test
  public void testGetSelectStatementVarArgs() {
      String keyspaceName = "sunbird";
      String tableName = "user";

      String query = CassandraUtil.getSelectStatement(keyspaceName, tableName, "id", "name");

      String expected = "SELECT id,name FROM sunbird.user where id =  ?; ";
      assertEquals(expected, query);
  }

  @Test
  public void testProcessExceptionForUnknownIdentifier() {
    String errorMsg = "Unknown identifier abc";
    Exception e = new Exception(errorMsg);
    String processedMsg = CassandraUtil.processExceptionForUnknownIdentifier(e);

    assertNotNull(processedMsg);
  }
}