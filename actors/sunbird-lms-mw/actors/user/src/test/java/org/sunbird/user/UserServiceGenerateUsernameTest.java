package org.sunbird.user;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserServiceImpl;

public class UserServiceGenerateUsernameTest {

  private static String englishName = "Some Random Name";
  private static String hindiName = "कोई अज्ञात नाम";
  private static String teluguName = "సుధీర్";
  private static String tamilName = "எந்த அறியப்படாத பெயர்";
  private static Pattern pattern;
  private static String userNameValidatorRegex = "(^([a-z])+[0-9]{4})";
  private static UserService userService = new UserServiceImpl();

  @Test
  public void testGenerateUsernamesSuccessWithEnglishName() {
    assertTrue(performTest(englishName));
  }

  @Test
  public void testGenerateUsernamesFailureWithBlankName() {
    List<String> result = userService.generateUsernames("", new ArrayList<String>());
    assertTrue(result == null);
  }

  @Test
  public void testGenerateUsernamesSuccessWithHindiName() {
    assertTrue(performTest(hindiName));
  }

  @Test
  public void testGenerateUsernamesSuccessWithTeluguName() {
    assertTrue(performTest(teluguName));
  }

  @Test
  public void testGenerateUsernamesSuccessWithTamilName() {
    assertTrue(performTest(tamilName));
  }

  private boolean performTest(String name) {
    List<String> result = userService.generateUsernames(name, new ArrayList<String>());
    pattern = Pattern.compile(userNameValidatorRegex);
    boolean flag = true;
    for (int i = 0; i < result.size(); i++) {
      Matcher matcher = pattern.matcher(result.get(i));
      if (!matcher.matches()) {
        flag = false;
        break;
      }
    }
    return flag;
  }
}
