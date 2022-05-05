/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h.services.loader;

import java.io.IOException;

public interface LibraryLoader {
  /**
   * Useless, but I prefer it for a better readability
   *
   * @return simply the OS name
   */
  static String getOS() {
    return System.getProperty("os.name");
  }
  
  /**
   * Loads library/ies
   *
   * @throws IOException if it can't found the lib/s
   */
  void load() throws IOException;

  /**
   * It's unused for the time being, but it could be helpful in the future
   *
   * @return bitness of the jvm
   */
  default int getOsBitness() {
    return Integer.parseInt(System.getProperty("sun.arch.data.model"));
  }
}
