package controllers.feed.validator;

import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.exception.ProjectCommonException;

public class FeedRequestValidatorTest {

  @Test
  public void userIdValidationTestSuccess() {
    Assert.assertTrue(FeedRequestValidator.userIdValidation("123-456-789"));
  }

  @Test(expected = ProjectCommonException.class)
  public void userIdValidationTestFailure() {
    Assert.assertTrue(FeedRequestValidator.userIdValidation("123-456-789"));
  }
}
