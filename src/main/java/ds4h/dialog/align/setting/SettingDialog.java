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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SettingDialog extends JDialog {
    private static final String MESSAGE = "The Rigid Transformation is used by default.\n Check one of these checkboxes to change it";
    private final SettingEvent settingEvent;
    private final JTextPane textPane;
    private final JCheckBox checkIsAffine;
    private final JCheckBox checkIsProjective;
    private final JButton okButton;


    public SettingDialog(JFrame frame, String title, boolean isModal) {
        super(frame, title, isModal);
        this.settingEvent = new SettingEvent();
        this.checkIsAffine = new JCheckBox("Check for Affine Transformation");
        this.checkIsProjective = new JCheckBox("Check for Projective Transformation");
        this.textPane = new JTextPane();
        this.okButton = new JButton("OK");
        this.initCheckboxes();
    }

    public boolean initIsSuccessFul() {
        final boolean[] isSuccessFul = {true};
        // add text
        this.getTextPane().setText(MESSAGE);
        this.getTextPane().setDisabledTextColor(Color.BLACK);
        this.getTextPane().setEnabled(false);
        this.add(this.getTextPane(), BorderLayout.NORTH);
        // add checkboxes
        JPanel checkPanel = new JPanel(new GridLayout(0, 1));
        checkPanel.add(this.getCheckIsProjective());
        checkPanel.add(this.getCheckIsAffine());
        this.add(checkPanel, BorderLayout.CENTER);
        // add button
        this.add(this.getOkButton(), BorderLayout.SOUTH);
        this.setLocationRelativeTo(null); // centers the frame
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                isSuccessFul[0] = false;
            }
        });
        this.pack();
        this.setVisible(true);
        return isSuccessFul[0];
    }

    public SettingEvent getEvent() {
        return this.settingEvent;
    }

    public JButton getOkButton() {
        return this.okButton;
    }

    private void initCheckboxes() {
        this.getCheckIsAffine().setToolTipText("Evaluate only translation on X,Y axis");
        this.getCheckIsAffine().setSelected(false);
        this.getCheckIsAffine().setEnabled(true);
        this.getCheckIsAffine().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                this.getEvent().setAffine(true);
                this.getEvent().setProjective(false);
                this.getCheckIsProjective().setSelected(false);
            }
        });
        this.getCheckIsProjective().setToolTipText("Project model needs four points");
        this.getCheckIsProjective().setSelected(false);
        this.getCheckIsProjective().setEnabled(true);
        this.getCheckIsProjective().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                this.getEvent().setProjective(true);
                this.getEvent().setAffine(false);
                this.getCheckIsAffine().setSelected(false);
            }
        });
    }

    private JCheckBox getCheckIsAffine() {
        return this.checkIsAffine;
    }

    private JCheckBox getCheckIsProjective() {
        return this.checkIsProjective;
    }

    private JTextPane getTextPane() {
        return this.textPane;
    }
}
