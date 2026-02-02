package org.sunbird.actor.organisation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.organisation.validator.OrgTypeValidator;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.organisation.OrganisationType;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
        "javax.management.*",
        "javax.net.ssl.*",
        "javax.security.*",
        "jdk.internal.reflect.*",
        "javax.crypto.*"
})
public class OrgTypeValidatorTest {

    @Test
    public void testInitializeOrgTypeFromCache() {
        List<OrganisationType> orgTypeList = new ArrayList<>();
        OrganisationType orgType1 = new OrganisationType();
        orgType1.setName("School");
        orgType1.setValue(2);
        orgType1.setDisplayName("School");
        orgType1.setFlagNameList(Arrays.asList("isSchool"));
        orgTypeList.add(orgType1);

        OrganisationType orgType2 = new OrganisationType();
        orgType2.setName("Board");
        orgType2.setValue(5);
        orgType2.setDisplayName("Board");
        orgType2.setFlagNameList(Arrays.asList("isBoard"));
        orgTypeList.add(orgType2);

        OrgTypeValidator orgValidator = OrgTypeValidator.getInstance();
        orgValidator.initializeOrgTypeFromCache(orgTypeList);

        assertTrue(orgValidator.isOrgTypeInitialized());
        assertTrue(orgValidator.isOrgTypeExist("School"));

        assertEquals(2, orgValidator.getValueByType("school"));
        assertEquals("school",orgValidator.getTypeByValue(2));

        Map<String, Object> orgValueMap = new HashMap<>();
        orgValueMap.put(JsonKey.ORGANISATION_TYPE, 2);
        orgValueMap.put(JsonKey.ORG_SUB_TYPE, 5);
        orgValidator.updateOrganisationTypeFlags(orgValueMap);

        assertTrue((boolean) orgValueMap.get("isSchool"));
        assertTrue((boolean) orgValueMap.get("isBoard"));

        try {
            orgValidator.getValueByType("Department");
        } catch (ProjectCommonException pe) {
            assertTrue(pe.getResponseCode() == ResponseCode.invalidValue);
        }
        try {
            orgValidator.getTypeByValue(8);
        } catch (ProjectCommonException pe) {
            assertTrue(pe.getResponseCode() == ResponseCode.invalidValue);
        }
    }
}
