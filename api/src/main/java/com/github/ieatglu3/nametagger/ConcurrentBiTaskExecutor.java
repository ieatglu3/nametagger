package com.github.ieatglu3.nametagger;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple concurrent task executor
 * @param <A> context A
 * @param <B> context B
 */
public final class ConcurrentBiTaskExecutor<A, B>
{
  public static final Logger LOGGER = Logger.getLogger(ConcurrentBiTaskExecutor.class.getName());
  private final ConcurrentLinkedDeque<BiConsumer<A, B>> tasks = new ConcurrentLinkedDeque<>();

  private volatile boolean shutdown = false;

  /**
   * Shuts down the executor, preventing new tasks from being submitted, already submitted tasks will still be executed
   */
  public void shutdown()
  {
    this.shutdown = true;
  }

  /**
   * Submits a task to be executed on the next tick with the provided context
   * @param task task
   */
  public void submit(BiConsumer<A, B> task)
  {
    if (this.shutdown)
      throw new IllegalStateException("Cannot submit task to shutdown executor");
    this.tasks.add(task);
  }

  /**
   * Executes all pending tasks
   * @param ctxA context to execute tasks with
   * @param ctxB context to execute tasks with
   */
  public void executeAll(A ctxA, B ctxB)
  {
    BiConsumer<A, B> task;
    while ((task = this.tasks.poll()) != null)
    {
      try
      {
        task.accept(ctxA, ctxB);
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Exception while executing task", e);
      }
    }
  }
}