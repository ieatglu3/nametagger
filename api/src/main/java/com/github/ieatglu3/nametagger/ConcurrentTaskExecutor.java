package com.github.ieatglu3.nametagger;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple concurrent task executor
 * @param <T> context
 */
public final class ConcurrentTaskExecutor<T>
{
  public static final Logger LOGGER = Logger.getLogger(ConcurrentTaskExecutor.class.getName());
  private final ConcurrentLinkedDeque<Consumer<T>> tasks = new ConcurrentLinkedDeque<>();

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
  public void submit(Consumer<T> task)
  {
    if (this.shutdown)
      throw new IllegalStateException("Cannot submit task to shutdown executor");
    this.tasks.add(task);
  }

  /**
   * Executes all pending tasks
   * @param ctx context to execute tasks with
   */
  public void executeAll(T ctx)
  {
    Consumer<T> task;
    while ((task = this.tasks.poll()) != null)
    {
      try
      {
        task.accept(ctx);
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Exception while executing task", e);
      }
    }
  }
}