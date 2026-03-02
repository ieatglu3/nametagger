package com.github.ieatglu3.nametagger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

/**
 * Simple thread factory that creates named daemon threads
 * <p>
 * Default thread name format is {@code 'nametagger thread - {thread-number}'}, but you can provide a custom name supplier using {@link #named(IntFunction)}
 */
public final class NametaggerThreadFactory implements java.util.concurrent.ThreadFactory
{

  /**
   * Creates a thread factory with the given name supplier
   * @param nameSupplier name supplier
   * @return thread factory with the given name supplier
   */
  public static NametaggerThreadFactory named(IntFunction<String> nameSupplier)
  {
    return new NametaggerThreadFactory(nameSupplier);
  }

  /**
   * Creates a thread factory with the default name supplier {@code 'nametagger thread - {thread-number}'}
   * @return thread factory with the default name supplier
   */
  public static NametaggerThreadFactory create()
  {
    return named(i -> "nametagger thread - " + i);
  }

  private final AtomicInteger threadCount = new AtomicInteger(0);
  private final IntFunction<String> nameSupplier;

  private NametaggerThreadFactory(IntFunction<String> nameSupplier)
  {
    this.nameSupplier = nameSupplier;
  }

  @Override
  public Thread newThread(Runnable runnable)
  {
    final Thread thread = new Thread(runnable, this.nameSupplier.apply(this.threadCount.getAndIncrement()));
    thread.setDaemon(true);
    return thread;
  }
}