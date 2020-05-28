/** */
package org.sunbird.common.models.util.datasecurity;

import java.util.List;
import java.util.Map;

/**
 * This service will have the data encryption logic. these logic will differ based on implementation
 * class.
 *
 * @author Manzarul
 */
public interface EncryptionService {

  String ALGORITHM = "AES";
  int ITERATIONS = 3;
  byte[] keyValue =
      new byte[] {'T', 'h', 'i', 's', 'A', 's', 'I', 'S', 'e', 'r', 'c', 'e', 'K', 't', 'e', 'y'};

  /**
   * This method will take input as key value pair , value can be any primitive or String or both or
   * can have another map as values. inner map will also have values as primitive or String or both
   *
   * @param data Map<String,Object>
   * @return Map<String,Object>
   * @throws Exception
   */
  Map<String, Object> encryptData(Map<String, Object> data) throws Exception;

  /**
   * This method will take list of map as an input to encrypt the data, after encryption it will
   * return same map with encrypted values. values in side map can have primitive , String or
   * another map have primitive , String values.
   *
   * @param data List<Map<String,Object>>
   * @return List<Map<String,Object>>
   * @throws Exception
   */
  List<Map<String, Object>> encryptData(List<Map<String, Object>> data) throws Exception;

  /**
   * This method will take String as an input and encrypt the String and return back.
   *
   * @param data String
   * @return String
   * @throws Exception
   */
  String encryptData(String data) throws Exception;
}
