package com.github.ieatglu3.nametagger.platformutil.entity;

import com.github.ieatglu3.nametagger.platformutil.mappings.MojangServerMapper;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

public enum EntityIdProvider
{

  Mojang((offset) -> {
    final MojangServerMapper mapper = MojangServerMapper.get();
    if (mapper.isEntityIdCounterFieldAtomic)
      return new MojangAtomicEntityIdProvider();
    else
      return new MojangUnsafeEntityIdProvider();
  }),
  MojangAtomic((offset) -> new MojangAtomicEntityIdProvider()),
  MojangUnsafe((offset) -> new MojangUnsafeEntityIdProvider()),
  Naive(NaiveEntityIdProvider::new);

  private final UnusedEntityIdProviderFactory factory;
  EntityIdProvider(UnusedEntityIdProviderFactory factory)
  {
    this.factory = factory;
  }

  public DelegatedEntityIdProvider get(int offset) throws Exception
  {
    return this.factory.create(offset);
  }

  public DelegatedEntityIdProvider getUnchecked(int offset)
  {
    try {
      return this.get(offset);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create UnusedEntityIdProvider for " + this.name(), e);
    }
  }

  static final class MojangUnsafeEntityIdProvider extends DelegatedEntityIdProvider
  {

    private final Field nextIdField;
    private final AtomicInteger justHopeThisOffsetPreventsCollisions = new AtomicInteger(10_000);
    public MojangUnsafeEntityIdProvider() throws Exception
    {
      super("Mojang (unsafe)");
      final MojangServerMapper mapper = MojangServerMapper.get();
      if (mapper.isEntityIdCounterFieldAtomic)
        throw new Exception("Server's entity ID generator is atomic, cannot use unsafe provider");
      this.nextIdField = mapper.entityIdCounterField;
    }

    @Override
    public int next()
    {
      try
      {
        final int nextId = this.nextIdField.getInt(null);
        this.nextIdField.setInt(null, nextId + 1);
        return nextId + this.justHopeThisOffsetPreventsCollisions.incrementAndGet();
      }
      catch (IllegalAccessException e) {
        throw new RuntimeException("Failed to access Servers's entity ID generator", e);
      }
    }
  }

  static final class MojangAtomicEntityIdProvider extends DelegatedEntityIdProvider
  {

    private final AtomicInteger nextId;
    public MojangAtomicEntityIdProvider() throws Exception
    {
      super("Mojang (atomic)");
      final MojangServerMapper mapper = MojangServerMapper.get();
      if (!mapper.isEntityIdCounterFieldAtomic)
        throw new Exception("Server's entity ID generator is not atomic");
      this.nextId = (AtomicInteger) mapper.entityIdCounterField.get(null);
    }

    @Override
    public int next()
    {
      return this.nextId.incrementAndGet();
    }
  }

  static final class NaiveEntityIdProvider extends DelegatedEntityIdProvider
  {

    private final AtomicInteger nextId;
    public NaiveEntityIdProvider(int offset)
    {
      super("Naive");
      this.nextId = new AtomicInteger(offset);
    }

    @Override
    public int next()
    {
      return this.nextId.incrementAndGet();
    }
  }
}