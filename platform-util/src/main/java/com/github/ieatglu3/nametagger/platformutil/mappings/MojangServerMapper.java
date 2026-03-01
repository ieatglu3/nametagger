package com.github.ieatglu3.nametagger.platformutil.mappings;

import com.github.ieatglu3.nametagger.platformutil.Reflections;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Internal use
 */
public final class MojangServerMapper
{

  private static final Logger LOGGER = Logger.getLogger(MojangServerMapper.class.getName());

  private static SoftReference<MojangServerMapper> instance = null;

  public static synchronized MojangServerMapper get()
  {
    if (instance == null || instance.get() == null)
      instance = new SoftReference<>(new MojangServerMapper());
    return instance.get();
  }

  public final boolean obfuscated, versionedPackaging, isPaper, isEntityIdCounterFieldAtomic;
  public final String craftBukkitPackage, nmsPackage;

  public final Class<?> entityClass;
  public final Field entityIdCounterField;

  public MojangServerMapper()
  {

    final var bukkitServer = Reflections.callStaticMethod("org.bukkit.Bukkit", "getServer", new Class[0], new Object[0]);

    String cbPackage = bukkitServer.getClass().getPackage().getName();
    String versionStr = null;
    try {
      versionStr = cbPackage.replace(".", ",").split(",")[3];
    } catch (Exception ex) {
    }

    if (versionStr == null)
      this.nmsPackage = "net.minecraft.";
    else
      this.nmsPackage = "net.minecraft.server." + versionStr + ".";

    this.craftBukkitPackage = cbPackage + ".";

    this.versionedPackaging = versionStr != null;
    this.obfuscated = Reflections.classExists("net.minecraft.server.network.PlayerConnection");
    this.isPaper = Reflections.classExists("com.destroystokyo.paper.PaperConfig");
    this.entityClass = findFirstServerClass("world.entity.Entity", "Entity");

    Field nextEntityIdField = Reflections.findFirstField(this.entityClass, (index, field) -> {

      if (!Modifier.isStatic(field.getModifiers()))
        return false;

      if (this.versionedPackaging && field.getType() == int.class && field.getName().equals("entityCount"))
        return true;

      if (this.obfuscated && field.getType() == AtomicInteger.class)
        return true;

      // todo; more accurate mapping?
      return field.getType() == AtomicInteger.class;
    });

    if (nextEntityIdField == null)
      throw new RuntimeException("Failed to find the entity ID generator field in " + this.entityClass.getName());

    nextEntityIdField.setAccessible(true);
    this.entityIdCounterField = nextEntityIdField;
    this.isEntityIdCounterFieldAtomic = this.entityIdCounterField.getType() == AtomicInteger.class;
  }

  public Class<?> getServerClass(String name)
  {
    return Reflections.getClass(this.nmsPackage + name);
  }

  public Class<?> findFirstServerClass(String... names)
  {
    for (String name : names)
    {
      try
      {
        return getServerClass(name);
      }
      catch (RuntimeException ex) { }
    }
    throw new RuntimeException("None of the provided server class names (" + String.join(", ", names) + ") were found for server " + this.nmsPackage);
  }

}