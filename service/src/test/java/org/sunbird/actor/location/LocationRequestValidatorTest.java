package org.sunbird.actor.location;

import akka.actor.ActorRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import org.sunbird.client.location.LocationClient;
import org.sunbird.client.location.impl.LocationClientImpl;
import org.sunbird.models.location.Location;
import org.sunbird.util.ProjectUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProjectUtil.class, LocationClient.class, LocationClientImpl.class, ActorRef.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class LocationRequestValidatorTest {

  private static LocationClientImpl locationClient;

  @BeforeClass
  public static void before() {
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.when(ProjectUtil.getConfigValue(Mockito.anyString()))
        .thenReturn("state,district,block,cluster,school;");
  }

  @Before
  public void beforeEachTest() throws Exception {
    locationClient = Mockito.mock(LocationClientImpl.class);
    PowerMockito.whenNew(LocationClientImpl.class).withNoArguments().thenReturn(locationClient);
    Location location = new Location();
    location.setCode("code");
    location.setId("id");
    location.setName("Name");
    location.setType("state");
    PowerMockito.when(
            locationClient.getLocationById(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(location);
  }

  @Test
  public void getValidatedLocationSetTest() {
    LocationRequestValidator validator = new LocationRequestValidator();
    Location location = new Location();
    location.setCode("code");
    location.setId("id");
    location.setName("Name");
    location.setType("state");
    List<Location> locList = new ArrayList<>();
    Set<String> loc = validator.getValidatedLocationSet(null, locList);
    Assert.assertNotNull(loc);
  }
}
