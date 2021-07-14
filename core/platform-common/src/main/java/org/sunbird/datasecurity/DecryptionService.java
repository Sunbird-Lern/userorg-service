package org.sunbird.datasecurity;

import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;

/**
 * This service will have data decryption methods. decryption logic will differ based on imp
 * classes.
 *
 * @author Manzarul
 */
public interface DecryptionService {

  String ALGORITHM = "AES";
  int ITERATIONS = 3;
  byte[] keyValue =
      new byte[] {'T', 'h', 'i', 's', 'A', 's', 'I', 'S', 'e', 'r', 'c', 'e', 'K', 't', 'e', 'y'};

  /**
   * This method will take input as key value pair , value can be any primitive or String or both or
   * can have another map as values. inner map will also have values as primitive or String or both
   *
   * @param data Map<String,Object>
   * @param context
   * @return Map<String,Object>
   * @throws Exception
   */
  Map<String, Object> decryptData(Map<String, Object> data, RequestContext context);

  /**
   * This method will take list of map as an input to decrypt the data, after decryption it will
   * return same map with decrypted values. values in side map can have primitive , String or
   * another map have primitive , String values.
   *
   * @param data List<Map<String,Object>>
   * @param context
   * @return List<Map<String,Object>>
   * @throws Exception
   */
  List<Map<String, Object>> decryptData(List<Map<String, Object>> data, RequestContext context);

  /**
   * Decrypt given data.
   *
   * @param data Input data
   * @param context
   * @return Decrypted data
   */
  String decryptData(String data, RequestContext context);

  /**
   * Decrypt given data.
   *
   * @param data Input data
   * @param context
   * @return Decrypted data
   */
  String decryptData(String data, boolean throwExceptionOnFailure, RequestContext context);
}
