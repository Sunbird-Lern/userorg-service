package org.sunbird.common.models.util.datasecurity.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.models.util.datasecurity.DataMaskingService;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;

/** @author Amit Kumar */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
    encryptionService = ServiceFactory.getEncryptionServiceInstance(null);
    decryptionService = ServiceFactory.getDecryptionServiceInstance(null);
    maskingService = ServiceFactory.getMaskingServiceInstance(null);
    try {
      encryptedData = encryptionService.encryptData(data);
      decryptedData = decryptionService.decryptData(encryptedData);
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataDecryptionFrMap() {
    try {
      assertEquals(
          decryptionService
              .decryptData(encryptionService.encryptData(map2))
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
          decryptionService.decryptData(encryptionService.encryptData(map2)).get(JsonKey.LOCATION),
          null);
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataDecryptionFrMapWithEmptyValue() {
    try {
      map2.put(JsonKey.LOCATION, "");
      assertEquals(
          decryptionService.decryptData(encryptionService.encryptData(map2)).get(JsonKey.LOCATION),
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
              .decryptData(encryptionService.encryptData(mapList2))
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
        assertNotEquals(encryptionService.encryptData(map).get(JsonKey.FIRST_NAME), "Amit");
      }
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataEncryptionFrListMap() {
    try {
      if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
        assertNotEquals(
            encryptionService.encryptData(mapList).get(0).get(JsonKey.FIRST_NAME), "Amit");
      }
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataEncryptionFrMapWithNullValue() {
    try {
      if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
        map.put(JsonKey.LAST_NAME, null);
        assertEquals(encryptionService.encryptData(map).get(JsonKey.LAST_NAME), null);
      }
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataEncryptionFrMapWithEmptyValue() {
    try {
      if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
        map.put(JsonKey.LAST_NAME, "");
        assertNotEquals(encryptionService.encryptData(map).get(JsonKey.LAST_NAME), "");
      }
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataEncryption() {
    try {
      assertEquals(encryptedData, encryptionService.encryptData(data));
    } catch (Exception e) {
    }
  }

  @Test
  public void testDataDecryption() {
    try {
      assertEquals(decryptedData, decryptionService.decryptData(encryptedData));
    } catch (Exception e) {
    }
  }

  @Test
  public void testADataEncryption() {
    try {
      if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
        assertNotEquals("Hello", encryptionService.encryptData("Hello"));
      } else {
        assertEquals("Hello", encryptionService.encryptData("Hello"));
      }
    } catch (Exception e) {
    }
  }

  @Test
  public void testADataDecryption() {
    try {
      assertEquals("Hello", decryptionService.decryptData(encryptionService.encryptData("Hello")));
    } catch (Exception e) {
    }
  }

  @Test
  public void testBDataDecryption() {
    try {
      if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
        assertNotEquals(
            encryptionService.encryptData("Hello"),
            decryptionService.decryptData(encryptionService.encryptData("Hello")));
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
