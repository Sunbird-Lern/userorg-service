package org.sunbird.actor.organisation;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.location.LocationRequestValidator;
import org.sunbird.actor.organisation.validator.OrganisationRequestValidator;
import org.sunbird.request.RequestContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        LocationRequestValidator.class,})
@PowerMockIgnore({
        "javax.management.*",
        "javax.net.ssl.*",
        "javax.security.*",
        "jdk.internal.reflect.*",
        "javax.crypto.*"
})
public class OrganisationRequestValidatorTest {
    private static LocationRequestValidator locationRequestValidator;
    @BeforeClass
    public static void beforeClass() throws Exception {
        locationRequestValidator = mock(LocationRequestValidator.class);
        whenNew(LocationRequestValidator.class).withNoArguments().thenReturn(locationRequestValidator);
        when(locationRequestValidator.getValidatedLocationIds(Mockito.any(), Mockito.any())).thenReturn(getLocationIdsLists());
        when(locationRequestValidator.getHierarchyLocationIds(Mockito.any(), Mockito.any())).thenReturn(getLocationIdsLists());
    }

    @Test
    public void validateOrgLocationTest() {
        OrganisationRequestValidator validator = new OrganisationRequestValidator();
        Map requestMap = new HashMap<String,Object>();
        validator.validateOrgLocation(requestMap, new RequestContext());
    }

    public static List<String> getLocationIdsLists() {
        List<String> locationIds = new ArrayList<>();
        locationIds.add("location1");
        locationIds.add("location2");
        return locationIds;
    }
}
