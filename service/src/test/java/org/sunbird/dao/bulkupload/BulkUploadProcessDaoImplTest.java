package org.sunbird.dao.bulkupload;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.sql.Timestamp;
import java.util.Calendar;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.dao.bulkupload.impl.BulkUploadProcessDaoImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.model.bulkupload.BulkUploadProcess;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;

@PrepareForTest({ServiceFactory.class, CassandraOperationImpl.class})
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class BulkUploadProcessDaoImplTest {
  private static CassandraOperationImpl cassandraOperation;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
  }

  @Test
  public void testCreate() {
    BulkUploadProcessDao bulkUploadProcessDao = BulkUploadProcessDaoImpl.getInstance();
    BulkUploadProcess bulkUploadProcess = new BulkUploadProcess();
    bulkUploadProcess.setId("processId");
    bulkUploadProcess.setObjectType("objectType");
    bulkUploadProcess.setUploadedBy("requestedBy");
    bulkUploadProcess.setUploadedDate(ProjectUtil.getFormattedDate());
    bulkUploadProcess.setCreatedBy("requestedBy");
    bulkUploadProcess.setCreatedOn(new Timestamp(Calendar.getInstance().getTime().getTime()));
    bulkUploadProcess.setProcessStartTime(ProjectUtil.getFormattedDate());
    bulkUploadProcess.setTaskCount(3);
    bulkUploadProcess.setStatus(ProjectUtil.BulkProcessStatus.IN_PROGRESS.getValue());
    bulkUploadProcess.setOrganisationId("someOrgId");
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(new Response());
    Response response = bulkUploadProcessDao.create(getBulkUploadProcess(), new RequestContext());
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getResult().get("id"), "processId");
  }

  /*@Test
  public void update() {
  }

  @Test
  public void read() {
  }*/

  public BulkUploadProcess getBulkUploadProcess() {
    BulkUploadProcess bulkUploadProcess = new BulkUploadProcess();
    bulkUploadProcess.setId("processId");
    bulkUploadProcess.setObjectType("objectType");
    bulkUploadProcess.setUploadedBy("requestedBy");
    bulkUploadProcess.setUploadedDate(ProjectUtil.getFormattedDate());
    bulkUploadProcess.setCreatedBy("requestedBy");
    bulkUploadProcess.setCreatedOn(new Timestamp(Calendar.getInstance().getTime().getTime()));
    bulkUploadProcess.setProcessStartTime(ProjectUtil.getFormattedDate());
    bulkUploadProcess.setTaskCount(3);
    bulkUploadProcess.setStatus(ProjectUtil.BulkProcessStatus.IN_PROGRESS.getValue());
    bulkUploadProcess.setOrganisationId("someOrgId");
    return bulkUploadProcess;
  }
}
