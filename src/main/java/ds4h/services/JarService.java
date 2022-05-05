/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h.services;

import java.io.File;

/**
 * https://stackoverflow.com/questions/482560/can-you-tell-on-runtime-if-youre-running-java-from-within-a-jar
 */
public class JarService {
  private JarService() {
  }
  
  public static String getJarName() {
    return new File(JarService.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
  }
  
  public static boolean runningFromJar() {
    return getJarName().contains(".jar");
  }
}
