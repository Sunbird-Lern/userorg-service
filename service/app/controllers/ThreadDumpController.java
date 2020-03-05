package controllers;

import org.sunbird.actor.router.BackgroundRequestRouter;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** @author Mahesh Kumar Gangula */
public class ThreadDumpController extends BaseController {

  public CompletionStage<Result> threaddump(Http.Request httpRequest) {
    final StringBuilder dump = new StringBuilder();
    final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    final ThreadInfo[] threadInfos =
        threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
    for (ThreadInfo threadInfo : threadInfos) {
      dump.append('"');
      dump.append(threadInfo.getThreadName());
      dump.append("\" ");
      final Thread.State state = threadInfo.getThreadState();
      dump.append("\n   java.lang.Thread.State: ");
      dump.append(state);
      final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
      for (final StackTraceElement stackTraceElement : stackTraceElements) {
        dump.append("\n        at ");
        dump.append(stackTraceElement);
      }
      dump.append("\n\n");
    }
    System.out.println("=== thread-dump start ===");
    System.out.println(dump.toString());
    System.out.println("=== thread-dump end ===");
    Request request = new Request();
    request.setOperation("takeThreadDump");
    request.setEnv(getEnvironment());
    if ("off".equalsIgnoreCase(BackgroundRequestRouter.getMode())) {
      actorResponseHandler(
          SunbirdMWService.getBackgroundRequestRouter(), request, timeout, null, httpRequest);
    }
    return CompletableFuture.supplyAsync(() -> ok("successful"));
  }
}
