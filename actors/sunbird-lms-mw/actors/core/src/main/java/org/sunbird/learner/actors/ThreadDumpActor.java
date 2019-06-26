package org.sunbird.learner.actors;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/** @author Mahesh Kumar Gangula */
@ActorConfig(
  tasks = {},
  asyncTasks = {"takeThreadDump"}
)
public class ThreadDumpActor extends BaseActor {

  @Override
  public void onReceive(Request request) throws Throwable {
    takeThreadDump();
    Response response = new Response();
    response.setResponseCode(ResponseCode.success);
    sender().tell(response, self());
  }

  private void takeThreadDump() {
    final StringBuilder dump = new StringBuilder();
    final StringBuilder details = new StringBuilder();
    final StringBuilder info = new StringBuilder();
    final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    final ThreadInfo[] threadInfos =
        threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
    details.append("Thread id | Thread name | CPU time | User time | Blocked time\n\n");
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

      long cputime = threadMXBean.getThreadCpuTime(threadInfo.getThreadId());
      long usertime = threadMXBean.getThreadUserTime(threadInfo.getThreadId());
      long blockedtime = threadInfo.getBlockedTime();
      details.append(
          threadInfo.getThreadId()
              + " | "
              + threadInfo.getThreadName()
              + " | "
              + cputime
              + " | "
              + usertime
              + " | "
              + blockedtime
              + "\n\n");

      info.append(threadInfo.toString() + "\n\n");
    }

    System.out.println("=== thread-dump start ===");
    System.out.println(dump.toString());
    System.out.println("=== thread-dump end ===\n\n");

    System.out.println("=== thread-cpu start ===");
    System.out.println(details.toString());
    System.out.println("=== thread-cpu end ===\n\n");

    System.out.println("=== thread-info start ===");
    System.out.println(info.toString());
    System.out.println("=== thread-info end ===\n\n");
  }
}
