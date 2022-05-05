/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h.observer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public interface Observable {
  /**
   *
   * @return
   */
  PropertyChangeSupport getSupport();
  
  /**
   *
   * @param propertyName
   * @param oldValue
   * @param newValue
   */
  default void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    getSupport().firePropertyChange(propertyName, oldValue, newValue);
  }
  
  /**
   *
   * @param listener
   */
  default void addPropertyChangeListener(PropertyChangeListener listener) {
    getSupport().addPropertyChangeListener(listener);
  }
  
  /**
   *
   * @param pcl
   */
  default void removePropertyChangeListener(PropertyChangeListener pcl) {
    getSupport().removePropertyChangeListener(pcl);
  }
  
  /**
   *
   * @param propertyName
   * @param listener
   */
  default void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    getSupport().removePropertyChangeListener(propertyName, listener);
  }
}
