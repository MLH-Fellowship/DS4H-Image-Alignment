/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h.services.loader;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface OpenCVUtility {
  /**
   *
   * @return the current version
   */
  static String getVersion() {
    return "455";
  }

  /**
   *
   * @return the res folder
   */
  static String getDir() {
    return "/opencv/";
  }

  /**
   *
   * @param prefix like 'libopencv'
   * @param ext like '.dylib'
   * @return all the libraries full path list
   */
  static List<String> loadLibraries(String prefix, String ext) {
    return Arrays.stream(OpenCVLibraries.values())
          .map(Enum::name)
          .map(name -> getDir() + prefix + name.toLowerCase() + getVersion() + ext)
          .collect(Collectors.toList());
  }
}
