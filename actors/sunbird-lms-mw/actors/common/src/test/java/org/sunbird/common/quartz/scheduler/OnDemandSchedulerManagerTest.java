package org.sunbird.common.quartz.scheduler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
  SchedulerManager.class,
})
@PowerMockIgnore("javax.management.*")
public class OnDemandSchedulerManagerTest {
  
  static OnDemandSchedulerManager onDemandSchedulerManager;
  static SchedulerManager schedulerManager;
  static Scheduler scheduler;
  
  @Test
  public void testTriggerScheduler() throws SchedulerException {
    PowerMockito.suppress(PowerMockito.constructor(SchedulerManager.class));
    PowerMockito.suppress(PowerMockito.methodsDeclaredIn(SchedulerManager.class));
    scheduler = mock(Scheduler.class);
    PowerMockito.mockStatic(SchedulerManager.class);
    String[] jobs = {"shadowuser"};
    onDemandSchedulerManager = spy(OnDemandSchedulerManager.class);
    when(scheduler.checkExists((JobKey) Mockito.anyObject())).thenReturn(false);
    onDemandSchedulerManager.triggerScheduler(jobs);
    verify(onDemandSchedulerManager).scheduleOnDemand(Mockito.anyString(), Mockito.anyString());
   
  }
  
}