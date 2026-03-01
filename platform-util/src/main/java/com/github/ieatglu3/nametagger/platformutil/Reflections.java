package com.github.ieatglu3.nametagger.platformutil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class Reflections
{

  public interface FieldMatcher
  {
    boolean matches(int index, Field field);
  }

  public static Object callStaticMethod(String className, String methodName, Class<?>[] parameterTypes, Object[] args)
  {
    return callStaticMethod(getClass(className), methodName, parameterTypes, args);
  }

  public static Object callStaticMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object[] args)
  {
    try
    {
      return clazz.getDeclaredMethod(methodName, parameterTypes).invoke(null, args);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Field getFieldNullable(Class<?> clazz, String fieldName)
  {
    try
    {
      return clazz.getDeclaredField(fieldName);
    }
    catch (NoSuchFieldException e) {
      return null;
    }
  }

  public static Field getFieldByIndexNullable(Class<?> clazz, int index)
  {
    try
    {
      return clazz.getDeclaredFields()[index];
    }
    catch (Exception e) {
      return null;
    }
  }

  public static Field getFirstFieldByConcreteTypeNullable(Class<?> clazz, Class<?> type)
  {
    try
    {
      for (Field field : clazz.getDeclaredFields())
      {
        if (field.getType() == type)
          return field;
      }
      return null;
    }
    catch (Exception e) {
      return null;
    }
  }

  public static Field getFieldByIndexConcreteTypeNullable(Class<?> clazz, Class<?> type, int occurrenceIndex)
  {
    try
    {
      int currentOccurrence = 0;
      for (Field field : clazz.getDeclaredFields())
      {
        if (field.getType() == type)
        {
          if (currentOccurrence == occurrenceIndex)
            return field;
          currentOccurrence++;
        }
      }
      return null;
    }
    catch (Exception e) {
      return null;
    }
  }

  public static Field findFirstField(Class<?> clazz, FieldMatcher matcher)
  {
    final Field[] declaredFields = clazz.getDeclaredFields();
    for (int i = 0, declaredFieldsLength = declaredFields.length; i < declaredFieldsLength; i++)
    {
      final Field field = declaredFields[i];
      if (matcher.matches(i, field))
        return field;
    }
    return null;
  }

  public static Method getMethodNullable(Class<?> clazz, String name, Class<?>... params)
  {
    try
    {
      return clazz.getDeclaredMethod(name, params);
    }
    catch (NoSuchMethodException e) {
      return null;
    }
  }

  public static Class<?> getClass(String name)
  {
    try
    {
      return Class.forName(name);
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static Class<?> getClassNullable(String name)
  {
    try
    {
      return Class.forName(name);
    }
    catch (ClassNotFoundException e) {
      return null;
    }
  }

  public static boolean classExists(String name)
  {
    try
    {
      Class.forName(name);
      return true;
    }
    catch (ClassNotFoundException ignored) {
    }
    return false;
  }
}