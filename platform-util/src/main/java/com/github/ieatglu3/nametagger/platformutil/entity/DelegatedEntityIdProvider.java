package com.github.ieatglu3.nametagger.platformutil.entity;

import com.github.ieatglu3.nametagger.UnusedEntityIdProvider;

public abstract class DelegatedEntityIdProvider implements UnusedEntityIdProvider
{
  public final String name;
  protected DelegatedEntityIdProvider(String name)
  {
    this.name = name;
  }
}