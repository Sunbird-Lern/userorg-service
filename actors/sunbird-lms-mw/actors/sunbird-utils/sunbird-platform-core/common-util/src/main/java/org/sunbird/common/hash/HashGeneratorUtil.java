package org.sunbird.common.hash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sunbird.common.models.util.datasecurity.OneWayHashing;

public class HashGeneratorUtil {
  private static List<Integer> primes = null;
  private static int MAX_NUMBER = 300;
  private static int numPrimes = 7;

  private static List<Integer> getPrimes() {
    List<Integer> list = new ArrayList<>();
    boolean prime[] = new boolean[MAX_NUMBER + 1];
    Arrays.fill(prime, true);
    for (int p = 2; p * p <= MAX_NUMBER; p++) {
      if (prime[p] == true) {
        for (int i = p * p; i <= MAX_NUMBER; i += p) prime[i] = false;
      }
    }
    for (int i = numPrimes; i <= MAX_NUMBER; i++) {
      if (prime[i] == true) {
        list.add(i);
      }
    }
    return list;
  }

  public static String getHashCode(String jsonString) {
    return OneWayHashing.encryptVal(jsonString);
  }
}
