/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h;

import ds4h.dialog.project.ProjectDialog;
import ij.plugin.PlugIn;
import javax.swing.*;

public class DS4H implements PlugIn {
  
  @Override
  public void run(String s) {
    SwingUtilities.invokeLater(ProjectDialog::new);
  }
  
  public static void main(String[] args) {
    DS4H ds4H = new DS4H();
    ds4H.run("");
  }
}
