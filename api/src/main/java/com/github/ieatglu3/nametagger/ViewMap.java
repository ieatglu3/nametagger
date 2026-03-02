package com.github.ieatglu3.nametagger;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

// todo; replace with custom primitive impl
public final class ViewMap extends ConcurrentHashMap<Integer, TaggedEntity>
{

  @FunctionalInterface
  public interface EntryConsumer
  {
    void accept(int entityId, TaggedEntity taggedEntity);
  }

  /**
   * Applies the given action to each entry in this map
   * @param action the action to apply to each entry
   */
  public void forEachEntry(EntryConsumer action)
  {
    this.forEach(action::accept);
  }

  /**
   * Gets a collection of all entity IDs that have attached tag lists in this map
   * @return collection of entity IDs
   */
  public Collection<Integer> entityIds()
  {
    return this.keySet();
  }

}