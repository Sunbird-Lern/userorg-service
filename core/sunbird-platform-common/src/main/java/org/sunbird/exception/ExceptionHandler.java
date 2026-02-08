package org.sunbird.exception;

import org.sunbird.exception.BaseException;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.ResponseCode;
import org.sunbird.request.Request;

/**
 * Utility class to handle and rethrow exceptions as BaseException.
 */
public class ExceptionHandler {

  /**
   * Generic method to handle exceptions and throw them as BaseException. If the exception is already
   * of type BaseException, it's rethrown. Otherwise, a generic SERVER_ERROR BaseException is
   * thrown.
   *
   * @param request The original request context.
   * @param ex The exception to be handled.
   */
  public static void handleExceptions(Request request, Exception ex) {
    if (ex instanceof BaseException) {
      throw new BaseException((BaseException) ex);
    } else {
      throw new BaseException(
          IResponseMessage.SERVER_ERROR,
          IResponseMessage.SERVER_ERROR,
          ResponseCode.SERVER_ERROR.getCode());
    }
  }
}
