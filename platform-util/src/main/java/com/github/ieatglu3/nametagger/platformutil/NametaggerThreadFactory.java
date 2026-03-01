package com.github.ieatglu3.nametagger.platformutil;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

/**
 * Simple thread factory
 */
public final class NametaggerThreadFactory implements java.util.concurrent.ThreadFactory
{

  /**
   * Creates a thread factory with the given name supplier, the supplier will be passed an incrementing integer starting from 0
   * @param nameSupplier name supplier
   * @return thread factory with the given name supplier
   */
  public static NametaggerThreadFactory named(IntFunction<String> nameSupplier)
  {
    return new NametaggerThreadFactory(nameSupplier);
  }

  /**
   * Creates a thread factory with the default name supplier; "Nametagger Thread {thread-number}"
   * @return thread factory with the default name supplier
   */
  public static NametaggerThreadFactory create()
  {
    return named(i -> "Nametagger Thread " + i);
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
    return new Thread(runnable, this.nameSupplier.apply(this.threadCount.getAndIncrement()));
  }
}