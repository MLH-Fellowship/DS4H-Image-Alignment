/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h.dialog.align.setting;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class SettingDialog extends JDialog {
  private final static String MESSAGE = "Check one of these checkboxes to apply \nRigid or Projective Model \nIf nothing is selected then Affine Model\nis used instead";
  private final SettingEvent settingEvent;
  private final JTextPane textPane;
  private final JCheckBox checkIsRigid;
  private final JCheckBox checkIsProjective;
  private final JButton okButton;
  
  
  public SettingDialog(JFrame frame, String title, boolean isModal) {
    super(frame, title, isModal);
    this.settingEvent = new SettingEvent();
    this.checkIsRigid = new JCheckBox("Check for Rigid Transformation");
    this.checkIsProjective = new JCheckBox("Check for Projective Transformation");
    this.textPane = new JTextPane();
    this.okButton = new JButton("OK");
    this.initCheckboxes();
  }
  
  public void init() {
    // add text
    this.getTextPane().setText(MESSAGE);
    this.getTextPane().setDisabledTextColor(Color.BLACK);
    this.getTextPane().setEnabled(false);
    this.add(this.getTextPane(), BorderLayout.NORTH);
    // add checkboxes
    JPanel checkPanel = new JPanel(new GridLayout(0, 1));
    checkPanel.add(this.getCheckIsProjective());
    checkPanel.add(this.getCheckIsRigid());
    this.add(checkPanel, BorderLayout.CENTER);
    // add button
    this.add(this.getOkButton(), BorderLayout.SOUTH);
    this.setLocationRelativeTo(null); // centers the frame
    this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    this.pack();
    this.setVisible(true);
  }
  
  public SettingEvent getEvent() {
    return this.settingEvent;
  }
  
  public JButton getOkButton() {
    return this.okButton;
  }
  
  private void initCheckboxes() {
    this.getCheckIsRigid().setToolTipText("Evaluate only translation on X,Y axis");
    this.getCheckIsRigid().setSelected(false);
    this.getCheckIsRigid().setEnabled(true);
    this.getCheckIsRigid().addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        this.getEvent().setRigid(true);
        this.getCheckIsProjective().setSelected(false);
      }
    });
    this.getCheckIsProjective().setToolTipText("Project model needs four points");
    this.getCheckIsProjective().setSelected(false);
    this.getCheckIsProjective().setEnabled(true);
    this.getCheckIsProjective().addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        this.getEvent().setProjective(true);
        this.getCheckIsRigid().setSelected(false);
      }
    });
  }
  
  private JCheckBox getCheckIsRigid() {
    return this.checkIsRigid;
  }
  
  private JCheckBox getCheckIsProjective() {
    return this.checkIsProjective;
  }
  
  private JTextPane getTextPane() {
    return this.textPane;
  }
}
