package com.github.ieatglu3.nametagger;

import com.github.retrooper.packetevents.util.Vector3d;

import java.util.Objects;

/**
 * Vector class representing a 3D vector with double precision:
 * For methods which require a three-dimensional vector, such as tag offsets and position updates
 */
public final class Vec
{

  public static final Vec ZERO = new Vec(0, 0, 0);

  private final double x;
  private final double y;
  private final double z;

  /**
   * @param x
   * @param y
   * @param z
   */
  public Vec(double x, double y, double z)
  {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  /**
   * Creates a new Vec instance with the specified x, y, and z components
   *
   * @param x
   * @param y
   * @param z
   * @return a new Vec instance
   */
  public static Vec of(double x, double y, double z)
  {
    return new Vec(x, y, z);
  }

  /**
   * Creates a new Vec instance from a {@link Vector3d}
   *
   * @param vec the Vec to convert
   * @return a new Vec instance with the same components as the input {@link Vector3d}
   */
  public static Vec from(Vector3d vec)
  {
    return new Vec(vec.x, vec.y, vec.z);
  }

  /**
   * Converts this Vec instance to a {@link Vector3d}
   *
   * @return a new {@link Vector3d}
   */
  public Vector3d toPacketEventsVector3d()
  {
    return new Vector3d(this.x, this.y, this.z);
  }

  /**
   * Adds another Vec to this Vec and returns the result as a new Vec
   *
   * @param other the Vec to add
   * @return a new resulting Vec
   */
  public Vec add(Vec other)
  {
    return new Vec(this.x + other.x, this.y + other.y, this.z + other.z);
  }

  /**
   * Subtracts another Vec from this Vec and returns the result as a new Vec.
   *
   * @param other the Vec to subtract
   * @return a new resulting Vec
   */
  public Vec subtract(Vec other)
  {
    return new Vec(this.x - other.x, this.y - other.y, this.z - other.z);
  }

  /**
   * Adds the specified x, y, and z components to this Vec and returns the result as a new Vec
   *
   * @param x
   * @param y
   * @param z
   * @return a new resulting Vec
   */
  public Vec add(double x, double y, double z)
  {
    return new Vec(this.x + x, this.y + y, this.z + z);
  }

  /**
   * Subtracts the specified x, y, and z components from this Vec and returns the result as a new Vec
   *
   * @param x
   * @param y
   * @param z
   * @return a new resulting Vec
   */
  public Vec subtract(double x, double y, double z)
  {
    return new Vec(this.x - x, this.y - y, this.z - z);
  }

  public double x()
  {
    return x;
  }

  public double y()
  {
    return y;
  }

  public double z()
  {
    return z;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == this)
      return true;
    if (obj == null || obj.getClass() != this.getClass())
      return false;
    final var that = (Vec) obj;
    return Double.doubleToLongBits(this.x) == Double.doubleToLongBits(that.x) &&
      Double.doubleToLongBits(this.y) == Double.doubleToLongBits(that.y) &&
      Double.doubleToLongBits(this.z) == Double.doubleToLongBits(that.z);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(x, y, z);
  }

  @Override
  public String toString()
  {
    return "Vec[" + "x=" + x + ", " + "y=" + y + ", " + "z=" + z + ']';
  }
}