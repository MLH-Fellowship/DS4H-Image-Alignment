/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h.utils;

public class Pair<X, Y> {
  private X x;
  private Y y;
  
  public Pair(X x, Y y) {
    super();
    this.x = x;
    this.y = y;
  }
  
  public X getX() {
    return x;
  }
  
  public void setX(X x) {
    this.x = x;
  }
  
  public Y getY() {
    return y;
  }
  
  public void setY(Y y) {
    this.y = y;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((x == null) ? 0 : x.hashCode());
    result = prime * result + ((y == null) ? 0 : y.hashCode());
    return result;
  }
  
  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Pair other = (Pair) obj;
    if (x == null) {
      if (other.x != null) return false;
    } else if (!x.equals(other.x)) return false;
    if (y == null) {
      return other.y == null;
    } else return y.equals(other.y);
  }
  
  @Override
  public String toString() {
    return "Pair [x=" + x + ", y=" + y + "]";
  }
}
