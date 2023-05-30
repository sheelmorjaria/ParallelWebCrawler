package com.udacity.webcrawler.profiler;

import javax.inject.Inject;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {    
    // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    Objects.requireNonNull(klass);
    if(!isProfiledClass(klass)) {
      throw new IllegalArgumentException(klass.getName() + " is not profiled");
    }
    ProfilingMethodInterceptor profilingMethodInterceptor = new ProfilingMethodInterceptor(clock, delegate, state);
    Object proxy = Proxy.newProxyInstance(ProfilerImpl.class.getClassLoader(), new Class[]{klass}, profilingMethodInterceptor);
    return (T) proxy;
  }
   
  public Boolean isProfiledClass(Class<?> klass) {
  List<Method> methods = List.of(klass.getDeclaredMethods());
  if(methods.isEmpty()) {
    return false;
  }

  return methods.stream().anyMatch(method -> method.getAnnotation(Profiled.class)!=null);

  }


    
  @Override
  public void writeData(Path path) {
    // Write the ProfilingState data to the given file path. If a file already exists at that
    // path, the new data should be appended to the existing file.
    Objects.requireNonNull(path);
    try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      writeData(writer);
    } catch (IOException e) {
      e.printStackTrace();
     throw new UncheckedIOException(e);
     }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
