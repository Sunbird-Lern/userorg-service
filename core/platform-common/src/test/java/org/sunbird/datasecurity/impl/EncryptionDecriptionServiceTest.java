package org.sunbird.datasecurity.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.datasecurity.DataMaskingService;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.datasecurity.EncryptionService;
import org.sunbird.keys.JsonKey;
import org.sunbird.util.PropertiesCache;

/** @author Amit Kumar */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesCache.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
@Ignore
public class EncryptionDecriptionServiceTest {

  private static String data = "hello sunbird";
  private static String encryptedData = "";
  private static String decryptedData = "";
  private static EncryptionService encryptionService = null;
  private static DecryptionService decryptionService = null;
  private static DataMaskingService maskingService = null;
  private static Map<String, Object> map = null;
  private static List<Map<String, Object>> mapList = null;
  private static Map<String, Object> map2 = null;
  private static List<Map<String, Object>> mapList2 = null;
  private static String sunbirdEncryption = "";

  @BeforeClass
  public static void setUp() {

    PowerMockito.mockStatic(PropertiesCache.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    when(PropertiesCache.getInstance()).thenReturn(propertiesCache);
    PowerMockito.when(propertiesCache.getProperty(Mockito.anyString())).thenReturn("anyString");

    sunbirdEncryption = System.getenv(JsonKey.SUNBIRD_ENCRYPTION);
    if (StringUtils.isBlank(sunbirdEncryption)) {
      sunbirdEncryption = PropertiesCache.getInstance().getProperty(JsonKey.SUNBIRD_ENCRYPTION);
    }
    map = new HashMap<>();
    map.put(JsonKey.FIRST_NAME, "Amit");
    map.put(JsonKey.LAST_NAME, "KUMAR");
    mapList = new ArrayList<>();
    mapList.add(map);
    map2 = new HashMap<>();
    map2.put(JsonKey.EMAIL, "amit.ec006@gmail.com");
    map2.put(JsonKey.FIRST_NAME, "Amit");
    map2.put(JsonKey.LAST_NAME, "KUMAR");
    mapList2 = new ArrayList<>();
    mapList2.add(map2);
    encryptionService = ServiceFactory.getEncryptionServiceInstance();
    decryptionService = ServiceFactory.getDecryptionServiceInstance();
    maskingService = ServiceFactory.getMaskingServiceInstance();
    try {
      encryptedData = encryptionService.encryptData(data, null);
      decryptedData = decryptionService.decryptData(encryptedData, null);
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataDecryptionFrMap() {
    try {
      assertEquals(
          decryptionService
              .decryptData(encryptionService.encryptData(map2, null), null)
              .get(JsonKey.FIRST_NAME),
          "Amit");
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataDecryptionFrMapWithNullValue() {
    try {
      map2.put(JsonKey.LOCATION, null);
      assertEquals(
          decryptionService
              .decryptData(encryptionService.encryptData(map2, null), null)
              .get(JsonKey.LOCATION),
          null);
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataDecryptionFrMapWithEmptyValue() {
    try {
      map2.put(JsonKey.LOCATION, "");
      assertEquals(
          decryptionService
              .decryptData(encryptionService.encryptData(map2, null), null)
              .get(JsonKey.LOCATION),
          "");
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataDecryptionFrMapWithMapList() {
    try {
      map2.put(JsonKey.LOCATION, "");
      assertEquals(
          decryptionService
              .decryptData(encryptionService.encryptData(mapList2, null), null)
              .get(0)
              .get(JsonKey.LOCATION),
          "");
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataEncryptionFrMap() {
    try {
      if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
        assertNotEquals(encryptionService.encryptData(map, null).get(JsonKey.FIRST_NAME), "Amit");
      }
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataEncryptionFrListMap() {
    try {
      if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
        assertNotEquals(
            encryptionService.encryptData(mapList, null).get(0).get(JsonKey.FIRST_NAME), "Amit");
      }
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataEncryptionFrMapWithNullValue() {
    try {
      if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
        map.put(JsonKey.LAST_NAME, null);
        assertEquals(encryptionService.encryptData(map, null).get(JsonKey.LAST_NAME), null);
      }
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataEncryptionFrMapWithEmptyValue() {
    try {
      if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
        map.put(JsonKey.LAST_NAME, "");
        assertNotEquals(encryptionService.encryptData(map, null).get(JsonKey.LAST_NAME), "");
      }
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataEncryption() {
    try {
      assertEquals(encryptedData, encryptionService.encryptData(data, null));
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataDecryption() {
    try {
      assertEquals(decryptedData, decryptionService.decryptData(encryptedData, null));
    } catch (Exception e) {
    }
  }

  @Test
  public void testADataEncryption() {
    try {
      if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
        assertNotEquals("Hello", encryptionService.encryptData("Hello", null));
      } else {
        assertEquals("Hello", encryptionService.encryptData("Hello", null));
      }
    } catch (Exception e) {
    }
  }

  @Test
  public void testADataDecryption() {
    try {
      assertEquals(
          "Hello",
          decryptionService.decryptData(encryptionService.encryptData("Hello", null), null));
    } catch (Exception e) {
    }
  }

  @Test
  public void testBDataDecryption() {
    try {
      if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
        assertNotEquals(
            encryptionService.encryptData("Hello", null),
            decryptionService.decryptData(encryptionService.encryptData("Hello", null), null));
      }
    } catch (Exception e) {
    }
  }

  @Test
  public void testEmptyPhoneMasking() {
    assertEquals(maskingService.maskPhone(""), "");
  }

  @Test
  public void testNullPhoneMasking() {
    assertEquals(maskingService.maskPhone(null), null);
  }

  @Test
  public void testPhoneMasking() {
    assertEquals(maskingService.maskPhone("1234567890"), "******7890");
  }

  @Test
  public void testEmptyEmailMasking() {
    assertEquals(maskingService.maskEmail(""), "");
  }

  @Test
  public void testNullEmailMasking() {
    assertEquals(maskingService.maskEmail(null), null);
  }

  @Test
  public void testEmailMasking() {
    assertEquals(maskingService.maskEmail("amit.ec006@gmail.com"), "am********@gmail.com");
  }

  @Test
  public void testEmptyDataMasking() {
    assertEquals(maskingService.maskData(""), "");
  }

  @Test
  public void testNullDataMasking() {
    assertEquals(maskingService.maskData(null), null);
  }

  @Test
  public void testDataMasking() {
    assertEquals(maskingService.maskData("qwerty"), "**erty");
  }

  @Test
  public void testDataOfLengthLessThanEqualTo4Masking() {
    assertEquals(maskingService.maskData("qwer"), "qwer");
  }
}
