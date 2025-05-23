package executor.recursive;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;

import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import java.util.concurrent.ExecutorService;

public class RecursiveThreadPoolSubmission implements Runnable {

  private final ExecutorService executor;

  private final int maxDepth;
  private final int depth;

  public RecursiveThreadPoolSubmission(ExecutorService executor, int maxDepth, int depth) {
    this.executor = executor;
    this.maxDepth = maxDepth;
    this.depth = depth;
  }

  @Override
  public void run() {
    if (depth == maxDepth) {
      return;
    }
    AgentSpan span = startSpan(String.valueOf(depth));
    try (AgentScope scope = activateSpan(span)) {
      executor.submit(new RecursiveThreadPoolSubmission(executor, maxDepth, depth + 1));
    } finally {
      span.finish();
    }
  }
}
