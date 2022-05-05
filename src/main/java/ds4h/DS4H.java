/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h;

import ds4h.image.registration.ImageAlignment;
import ij.IJ;
import ij.plugin.PlugIn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DS4H implements PlugIn {
  private static void deleteTempFilesOnExit(ImageAlignment imageAlignment) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      imageAlignment.getTempImages().forEach(tempImage -> {
        try {
          Files.deleteIfExists(Paths.get(tempImage));
        } catch (IOException e) {
          IJ.showMessage(e.getMessage());
        }
      });
    }));
  }
  
  @Override
  public void run(String s) {
    ImageAlignment imageAlignment = new ImageAlignment();
    imageAlignment.run();
    deleteTempFilesOnExit(imageAlignment);
  }
  
  public static void main(String[] args) {
    ImageAlignment imageAlignment = new ImageAlignment();
    imageAlignment.run();
    DS4H.deleteTempFilesOnExit(imageAlignment);
  }
}
