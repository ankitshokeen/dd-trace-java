package datadog.trace.instrumentation.playws1;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.captureSpan;
import static datadog.trace.instrumentation.playws.PlayWSClientDecorator.DECORATE;

import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import play.shaded.ahc.org.asynchttpclient.AsyncHandler;
import play.shaded.ahc.org.asynchttpclient.HttpResponseBodyPart;
import play.shaded.ahc.org.asynchttpclient.HttpResponseHeaders;
import play.shaded.ahc.org.asynchttpclient.HttpResponseStatus;
import play.shaded.ahc.org.asynchttpclient.Response;

public class AsyncHandlerWrapper implements AsyncHandler {
  private final AsyncHandler delegate;
  private final AgentSpan span;
  private final AgentScope.Continuation continuation;

  private final Response.ResponseBuilder builder = new Response.ResponseBuilder();

  public AsyncHandlerWrapper(final AsyncHandler delegate, final AgentSpan span) {
    this.delegate = delegate;
    this.span = span;
    continuation = captureSpan(span);
  }

  @Override
  public State onBodyPartReceived(final HttpResponseBodyPart content) throws Exception {
    builder.accumulate(content);
    return delegate.onBodyPartReceived(content);
  }

  @Override
  public State onStatusReceived(final HttpResponseStatus status) throws Exception {
    builder.reset();
    builder.accumulate(status);
    return delegate.onStatusReceived(status);
  }

  @Override
  public State onHeadersReceived(final HttpResponseHeaders httpHeaders) throws Exception {
    builder.accumulate(httpHeaders);
    return delegate.onHeadersReceived(httpHeaders);
  }

  @Override
  public Object onCompleted() throws Exception {
    final Response response = builder.build();
    if (response != null) {
      DECORATE.onResponse(span, response);
    }
    DECORATE.beforeFinish(span);
    span.finish();

    if (continuation != null) {
      try (final AgentScope scope = continuation.activate()) {
        return delegate.onCompleted();
      }
    } else {
      return delegate.onCompleted();
    }
  }

  @Override
  public void onThrowable(final Throwable throwable) {
    DECORATE.onError(span, throwable);
    DECORATE.beforeFinish(span);
    span.finish();

    if (continuation != null) {
      try (final AgentScope scope = continuation.activate()) {
        delegate.onThrowable(throwable);
      }
    } else {
      delegate.onThrowable(throwable);
    }
  }
}
