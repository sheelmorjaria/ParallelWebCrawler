package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;



/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final ProfilingState state;
  private final Object delegate;


  
  ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState state) {
    this.clock = Objects.requireNonNull(clock);
    this.delegate = Objects.requireNonNull(delegate);
    this.state = Objects.requireNonNull(state);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable{
    Object object = new Object();
    if (method.getAnnotation(Profiled.class) != null) {
      Instant start = clock.instant();

      try {

        object = method.invoke(delegate, args);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }catch (InvocationTargetException e) {
        throw e.getTargetException();
      }
      finally{
        Instant end = clock.instant();
        Duration duration = Duration.between(start, end);
        state.record(delegate.getClass(), method, duration);
      }
    } else {
      try {

        object = method.invoke(delegate, args);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw e.getCause();
      }
    }

    return object;
  }
}
