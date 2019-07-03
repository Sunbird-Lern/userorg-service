package org.sunbird.learner.actors.coursebatch.dao;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.coursebatch.dao.impl.CourseBatchDaoImpl;
import org.sunbird.models.course.batch.CourseBatch;

/** Created by rajatgupta on 08/04/19. */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceFactory.class})
@PowerMockIgnore("javax.management.*")
public class CourseBatchDaoTest {
  private CassandraOperation cassandraOperation;
  private CourseBatchDao courseBatchDao;

  @BeforeClass
  public static void setUp() {}

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    courseBatchDao = new CourseBatchDaoImpl();
  }

  @Test
  public void createCourseBatchSuccess() {

    CourseBatch courseBatch = new CourseBatch();
    courseBatch.setId(JsonKey.ID);
    courseBatch.setCountDecrementStatus(false);
    courseBatch.setCountIncrementStatus(false);
    courseBatch.setStatus(ProjectUtil.ProgressStatus.STARTED.getValue());
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(new Response());
    Response response = courseBatchDao.create(courseBatch);

    Assert.assertNotEquals(null, response);
  }

  @Test
  public void updateCourseBatchSuccess() {
    Map<String, Object> courseBatch = new HashMap<>();
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(new Response());
    Response response = courseBatchDao.update(courseBatch);
    Assert.assertNotEquals(null, response);
  }

  @Test
  public void readCourseBatchSuccess() {
    Response response = new Response();
    Map<String, Object> courseBatchMap = new HashMap<>();
    courseBatchMap.put(JsonKey.ID, JsonKey.BATCH_ID);
    response.put(JsonKey.RESPONSE, Arrays.asList(courseBatchMap));
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(response);
    CourseBatch courseBatch = courseBatchDao.readById(JsonKey.BATCH_ID);
    Assert.assertNotEquals(null, courseBatch);
  }

  @Test
  public void readCourseBatchFailure() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, Arrays.asList());
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(response);
    try {
      courseBatchDao.readById(JsonKey.BATCH_ID);
    } catch (ProjectCommonException e) {
      Assert.assertEquals(ResponseCode.invalidCourseBatchId.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void deleteCourseBatchSuccess() {
    when(cassandraOperation.deleteRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(new Response());
    Response response = courseBatchDao.delete(JsonKey.BATCH_ID);
    Assert.assertNotEquals(null, response);
  }
}
