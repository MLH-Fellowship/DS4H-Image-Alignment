package ds4h.dialog.project;

import ds4h.image.model.Project;
import ds4h.image.registration.ImageAlignment;
import ds4h.services.ProjectService;
import ij.IJ;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ProjectDialog extends JPanel implements ActionListener {
    private static final String NEW_PROJECT = "New Project (Default, select two or more images to start the project)";
    private static final String LOAD_PROJECT = "Load Project (the format is a zip file)";
    private static final String CONTINUE = "Continue";
    private final JFrame frame = new JFrame("Project Settings");
    private final ImageAlignment imageAlignment = new ImageAlignment();

    private boolean isNew = true;

    public ProjectDialog() {
        super(new BorderLayout());
        JRadioButton newProject = new JRadioButton(NEW_PROJECT);
        newProject.setActionCommand(NEW_PROJECT);
        newProject.setSelected(true);

        JRadioButton loadProject = new JRadioButton(LOAD_PROJECT);
        loadProject.setActionCommand(LOAD_PROJECT);

        JButton continueButton = new JButton(CONTINUE);
        continueButton.setEnabled(true);
        continueButton.setActionCommand(CONTINUE);

        ButtonGroup group = new ButtonGroup();
        group.add(newProject);
        group.add(loadProject);

        newProject.addActionListener(this);
        loadProject.addActionListener(this);
        continueButton.addActionListener(this);

        JPanel radioPanel = new JPanel(new GridLayout(0, 1));
        radioPanel.add(newProject);
        radioPanel.add(loadProject);
        radioPanel.add(continueButton);

        add(radioPanel, BorderLayout.LINE_START);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        this.frameSetup();
    }

    private void frameSetup() {
        this.getFrame().setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        //Create and set up the content pane.
        this.setOpaque(true); //content panes must be opaque
        this.getFrame().setContentPane(this);
        this.getFrame().setLocationRelativeTo(null);
        //Display the window.
        this.getFrame().pack();
        this.getFrame().setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case CONTINUE:
                this.continueHandler();
                break;
            case LOAD_PROJECT:
                this.setNew(false);
                break;
            case NEW_PROJECT:
                this.setNew(true);
                break;
            default:
                this.run();
                break;
        }
    }

    private void continueHandler() {
        this.getFrame().dispose();
        if (isNew) {
            this.run();
        } else {
            this.loadAndRun();
        }
    }

    private void loadAndRun() {
        Project project = ProjectService.load();
        if (project == null) {
            return;
        }
        this.getImageAlignment().initialize(project.getFilePaths());
        this.getImageAlignment().applyCorners(project.getImagesIndexesWithRois());
    }

    private void run() {
        this.getImageAlignment().run();
        deleteTempFilesOnExit(this.getImageAlignment());
    }

    private static void deleteTempFilesOnExit(ImageAlignment imageAlignment) {
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
            imageAlignment.getTempImages().forEach(tempImage -> {
            try {
                Files.deleteIfExists(Paths.get(tempImage));
            } catch (IOException e) {
                IJ.showMessage(e.getMessage());
            }
        })));
    }

    public void setNew(boolean aNew) {
        this.isNew = aNew;
    }

    public JFrame getFrame() {
        return this.frame;
    }

    public ImageAlignment getImageAlignment() {
        return this.imageAlignment;
    }
}
