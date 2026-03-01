package com.github.ieatglu3.nametagger.platformutil.entity;

@FunctionalInterface
public interface UnusedEntityIdProviderFactory
{
  DelegatedEntityIdProvider create(int offset) throws Exception;
}