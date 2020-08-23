package org.sunbird.cassandraimpl;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.*;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionManagerImpl;
import org.sunbird.helper.CassandraConnectionMngrFactory;
import org.sunbird.helper.ServiceFactory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  Cluster.class,
  Uninterruptibles.class,
  PreparedStatement.class,
  BoundStatement.class,
  Session.class,
  Metadata.class,
  CassandraConnectionMngrFactory.class,
  ResultSet.class,
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
  Select.SelectionOrAlias.class,
  CassandraConnectionManagerImpl.class,
  PropertiesCache.class,
  CassandraOperationImpl.class
})
@PowerMockIgnore("javax.management.*")
public class CassandraOperationImplTest {
  private static CassandraConnectionManager connectionManager;

  @BeforeClass
  public static void init() {}

  @Before
  public void setUp() {
    connectionManager = PowerMockito.mock(CassandraConnectionManagerImpl.class);
    PowerMockito.mockStatic(CassandraConnectionMngrFactory.class);
  }

  @Test
  public void testInsertRecordSuccess() throws Exception {
    Map<String, Object> address = new HashMap<>();
    address.put("id", "1234567890");
    address.put("addrLine1", "Line 1");
    address.put("addrLine2", "Line 2");

    when(CassandraConnectionMngrFactory.getInstance()).thenReturn(connectionManager);

    Session session = PowerMockito.mock(Session.class);
    ResultSet resultSet = PowerMockito.mock(ResultSet.class);
    when(connectionManager.getSession(Mockito.anyString())).thenReturn(session);
    PreparedStatement statement = PowerMockito.mock(PreparedStatement.class);
    when(session.prepare(Mockito.anyString())).thenReturn(statement);

    BoundStatement boundStatement = PowerMockito.mock(BoundStatement.class);
    PowerMockito.whenNew(BoundStatement.class).withAnyArguments().thenReturn(boundStatement);
    when(statement.bind()).thenReturn(boundStatement);

    session.execute(Mockito.any(BoundStatement.class));

    when(session.execute(boundStatement.bind(Mockito.any()))).thenReturn(resultSet);

    CassandraOperation cassandraOperation = ServiceFactory.getInstance();

    Response response = cassandraOperation.insertRecord("sunbird", "address1", address, null);
    assertEquals(ResponseCode.success.getErrorCode(), response.get("response"));
  }

  @Test
  public void testInsertRecordFailureDBInsertionError() throws Exception {
    Map<String, Object> address = new HashMap<>();
    address.put("id", "1234567890");
    address.put("addrLine1", "Line 1");
    address.put("addrLine2", "Line 2");

    when(CassandraConnectionMngrFactory.getInstance()).thenReturn(connectionManager);

    Session session = PowerMockito.mock(Session.class);
    ResultSet resultSet = PowerMockito.mock(ResultSet.class);
    when(connectionManager.getSession(Mockito.anyString())).thenReturn(session);
    PreparedStatement statement = PowerMockito.mock(PreparedStatement.class);
    when(session.prepare(Mockito.anyString())).thenReturn(statement);

    BoundStatement boundStatement = PowerMockito.mock(BoundStatement.class);
    PowerMockito.whenNew(BoundStatement.class).withAnyArguments().thenReturn(boundStatement);
    when(statement.bind()).thenReturn(boundStatement);

    session.execute(Mockito.any(BoundStatement.class));

    when(session.execute(boundStatement.bind(Mockito.any())))
        .thenThrow(
            new ProjectCommonException(
                ResponseCode.dbInsertionError.getErrorCode(),
                ResponseCode.dbInsertionError.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode()));

    CassandraOperation cassandraOperation = ServiceFactory.getInstance();

    try {
      cassandraOperation.insertRecord("sunbird", "address1", address, null);
    } catch (Exception ex) {
      assertEquals("DB insert operation failed.", ex.getMessage());
    }
  }

  @Test
  public void testInsertRecordFailureUnKnownIdentifierError() throws Exception {
    Map<String, Object> address = new HashMap<>();
    address.put("id", "1234567890");
    address.put("addrLine1", "Line 1");
    address.put("addrLine2", "Line 2");

    when(CassandraConnectionMngrFactory.getInstance()).thenReturn(connectionManager);

    Session session = PowerMockito.mock(Session.class);
    ResultSet resultSet = PowerMockito.mock(ResultSet.class);
    when(connectionManager.getSession(Mockito.anyString())).thenReturn(session);
    PreparedStatement statement = PowerMockito.mock(PreparedStatement.class);
    when(session.prepare(Mockito.anyString())).thenReturn(statement);

    BoundStatement boundStatement = PowerMockito.mock(BoundStatement.class);
    PowerMockito.whenNew(BoundStatement.class).withAnyArguments().thenReturn(boundStatement);
    when(statement.bind()).thenReturn(boundStatement);

    session.execute(Mockito.any(BoundStatement.class));
    Exception e =
        new ProjectCommonException(
            "Undefined column name xyz",
            "Undefined column name xyz",
            ResponseCode.SERVER_ERROR.getResponseCode());
    when(session.execute(boundStatement.bind(Mockito.any()))).thenThrow(e);

    CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    try {
      cassandraOperation.insertRecord("sunbird", "address1", address, null);
    } catch (Exception ex) {
      assertEquals("Invalid property xyz.", ex.getMessage());
    }
  }
}
