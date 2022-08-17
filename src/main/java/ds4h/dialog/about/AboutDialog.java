package ds4h.dialog.about;

import javax.swing.*;
import java.awt.*;

public class AboutDialog extends JDialog {
    private final JPanel contentPane = new JPanel();
    private final JLabel labelTitle = new JLabel();
    private final JLabel labelVersion = new JLabel();
    private final JLabel labelSupervisors = new JLabel();
    private final JLabel labelSupervisor1 = new JLabel();
    private final JLabel labelSupervisor2 = new JLabel();
    private final JLabel labelAuthors = new JLabel();
    private final JLabel labelAuthor1 = new JLabel();
    private final JLabel labelAuthor2 = new JLabel();

    private final JLabel labelAuthor3 = new JLabel();
    private final JLabel copyright = new JLabel();
    private final JLabel license = new JLabel();

    public AboutDialog() {
        this.setContentPane(this.getContentPane());
        this.setModal(true);
        this.setResizable(true);
        // call onCancel() when cross is clicked
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setMinimumSize(new Dimension(500, 450));
        this.setPreferredSize(new Dimension(500, 450));
        this.setResizable(false);
        this.setTitle("About...");
        this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));

        getContentPane().setBorder(BorderFactory.createEmptyBorder(16,16,16,16));

        getLabelTitle().setText("DS4H Image Alignment");
        getLabelTitle().setVisible(true);
        getContentPane().add(getLabelTitle());
        getContentPane().add(Box.createRigidArea(new Dimension(0,16)));

        getLabelVersion().setText("v1.1");
        getLabelVersion().setVisible(true);
        getContentPane().add(getLabelVersion());
        getContentPane().add(Box.createRigidArea(new Dimension(0,32)));

        getLabelSupervisors().setText("Head of the Project");
        getLabelSupervisors().setVisible(true);
        getContentPane().add(getLabelSupervisors());
        getContentPane().add(Box.createRigidArea(new Dimension(0,16)));

        getLabelSupervisor1().setText("Prof.ssa Antonella Carbonaro - antonella.carbonaro@unibo.it");
        getLabelSupervisor1().setVisible(true);
        getContentPane().add(getLabelSupervisor1());
        getContentPane().add(Box.createRigidArea(new Dimension(0,16)));

        getLabelSupervisor2().setText("Prof. Filippo Piccinini - f.piccinini@unibo.it");
        getLabelSupervisor2().setVisible(true);
        getContentPane().add(getLabelSupervisor2());
        getContentPane().add(Box.createRigidArea(new Dimension(0,32)));

        getLabelAuthors().setText("Made By");
        getLabelAuthors().setVisible(true);
        getContentPane().add(getLabelAuthors());
        getContentPane().add(Box.createRigidArea(new Dimension(0,16)));

        getLabelAuthor3().setText("Matteo Belletti - matteobellettifc@gmail.com");
        getLabelAuthor3().setVisible(true);
        getContentPane().add(getLabelAuthor3());
        getContentPane().add(Box.createRigidArea(new Dimension(0,16)));

        getLabelAuthor2().setText("Marco Edoardo Duma - marcoedoardo.duma@studio.unibo.it");
        getLabelAuthor2().setVisible(true);
        getContentPane().add(getLabelAuthor2());
        getContentPane().add(Box.createRigidArea(new Dimension(0,16)));

        getLabelAuthor1().setText("Stefano Belli - stefano.belli4@studio.unibo.it");
        getLabelAuthor1().setVisible(true);
        getContentPane().add(getLabelAuthor1());
        getContentPane().add(Box.createRigidArea(new Dimension(0,32)));

        getCopyright().setText("Copyright (©) 2019 Data Science for Health (DS4H) Group. All rights reserved");
        getCopyright().setVisible(true);
        getContentPane().add(getCopyright());
        getContentPane().add(Box.createRigidArea(new Dimension(0,16)));

        getLicense().setText("License: GNU General Public License version 3");
        getLicense().setVisible(true);
        getContentPane().add(getLicense());

    }

    @Override
    public JPanel getContentPane() {
        return contentPane;
    }

    public JLabel getLabelAuthors() {
        return labelAuthors;
    }

    public JLabel getLicense() {
        return license;
    }

    public JLabel getCopyright() {
        return copyright;
    }

    public JLabel getLabelTitle() {
        return labelTitle;
    }

    public JLabel getLabelVersion() {
        return labelVersion;
    }

    public JLabel getLabelSupervisors() {
        return labelSupervisors;
    }

    public JLabel getLabelSupervisor1() {
        return labelSupervisor1;
    }

    public JLabel getLabelSupervisor2() {
        return labelSupervisor2;
    }

    public JLabel getLabelAuthor1() {
        return labelAuthor1;
    }

    public JLabel getLabelAuthor2() {
        return labelAuthor2;
    }

    public JLabel getLabelAuthor3() {
        return labelAuthor3;
    }
}
