package ds4h.dialog.main;

import ds4h.dialog.main.event.*;
import ds4h.image.buffered.BufferedImage;
import ds4h.image.buffered.event.RoiSelectedEvent;
import ds4h.services.FileService;
import ds4h.utils.Pair;
import ij.IJ;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.plugin.frame.RoiManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.swing.SwingConstants.LEFT;

public class MainDialog extends ImageWindow {
    private static final String DIALOG_STATIC_TITLE = "DS4H Image Alignment.";
    private static final String SCALE_OPTION = "scale";
    public static BufferedImage currentImage = null; // Use a singleton instead or rethink the data flow
    private final OnMainDialogEventListener eventListener;

    private final JButton btnCopyCorners = new JButton("COPY CORNERS");
    private final JButton btnAlignImages = new JButton("ALIGN IMAGES VIA CORNERS");
    private final JButton btnAutoAlignImages = new JButton("AUTO ALIGN IMAGES");
    private final JCheckBox checkKeepOriginal = new JCheckBox("Keep all pixel data");
    private final JCheckBox checkShowPreview = new JCheckBox("Show preview window");
    private final JButton btnDeleteRoi = new JButton("DELETE CORNER");
    private final JButton btnPrevImage = new JButton("PREV IMAGE");
    private final JButton btnNextImage = new JButton("NEXT IMAGE");
    private final DefaultListModel<String> jListRoisModel = new DefaultListModel<>();
    public JList<String> jListRois;
    private BufferedImage image;
    private boolean mouseOverCanvas;
    private Rectangle oldRect = null;
    private Rectangle2D.Double lastBound;
    private Roi lastRoi;

    /**
     * I really hate myself for this tricky workaround, when Zooming the title gets changed
     * It doesn't make sense, this is only fast way I've found
     */
    private boolean titleHasToChange = true;

    public MainDialog(BufferedImage plus, OnMainDialogEventListener listener) {
        super(plus, new CustomCanvas(plus));
        this.image = plus;
        this.eventListener = listener;
        final CustomCanvas canvas = (CustomCanvas) getCanvas();
        // Remove the canvas from the window, to add it later
        this.removeAll();

        final Panel all = new Panel();
        all.setBackground(new Color(238, 238, 238));
        final GridBagLayout layout = new GridBagLayout();
        final GridBagConstraints allConstraints = new GridBagConstraints();
        allConstraints.fill = GridBagConstraints.BOTH;
        all.setLayout(layout);

        final Panel menu = new Panel(new FlowLayout(FlowLayout.LEFT));
        menu.add(getMenuPanel());
        menu.setBackground(new Color(238, 238, 238));
        allConstraints.gridx = 0;
        allConstraints.gridy = 0;
        allConstraints.gridwidth = 4;
        allConstraints.gridheight = 1;
        allConstraints.weightx = 1;
        allConstraints.weighty = 0.08;
        all.add(menu, allConstraints);

        final JPanel leftPanel = new JPanel();
        final GridBagLayout leftLayout = new GridBagLayout();
        final GridBagConstraints leftConstraints = new GridBagConstraints();
        leftPanel.setBackground(Color.GRAY);
        leftPanel.setLayout(leftLayout);

        final JPanel cornersPanel = this.getCornersPanel();
        leftConstraints.gridx = 0;
        leftConstraints.gridy = 0;
        leftConstraints.fill = GridBagConstraints.BOTH;
        leftConstraints.gridwidth = 1;
        leftConstraints.gridheight = 1;
        leftConstraints.weightx = 1;
        leftConstraints.weighty = 1;
        leftPanel.add(cornersPanel, leftConstraints);

        final JPanel actionsPanel = this.getActionsPanel();
        leftConstraints.gridx = 0;
        leftConstraints.gridy = 1;
        leftConstraints.fill = GridBagConstraints.BOTH;
        leftConstraints.gridwidth = 1;
        leftConstraints.gridheight = 1;
        leftConstraints.weightx = 1;
        leftConstraints.weighty = 1;
        leftPanel.add(actionsPanel, leftConstraints);

        final JPanel alignPanel = this.getAlignPanel();
        leftConstraints.gridx = 0;
        leftConstraints.gridy = 2;
        leftConstraints.fill = GridBagConstraints.BOTH;
        leftConstraints.gridwidth = 1;
        leftConstraints.gridheight = 1;
        leftConstraints.weightx = 1;
        leftConstraints.weighty = 1;
        leftPanel.add(alignPanel, leftConstraints);

        allConstraints.gridx = 0;
        allConstraints.gridy = 1;
        allConstraints.gridheight = 1;
        allConstraints.gridwidth = 1;
        allConstraints.weighty = 0.92;
        allConstraints.weightx = 0.04;
        allConstraints.fill = GridBagConstraints.BOTH;
        all.add(leftPanel, allConstraints);

        allConstraints.gridx = 1;
        allConstraints.gridy = 1;
        allConstraints.gridheight = 1;
        allConstraints.gridwidth = 3;
        allConstraints.weighty = 0.92;
        allConstraints.weightx = 0.96;
        allConstraints.fill = GridBagConstraints.BOTH;
        all.add(canvas, allConstraints);

        final GridBagLayout parentLayout = new GridBagLayout();
        final GridBagConstraints parentConstraints = new GridBagConstraints();
        parentConstraints.fill = GridBagConstraints.BOTH;
        parentConstraints.weightx = 1;
        parentConstraints.weighty = 1;
        parentConstraints.gridwidth = 1;
        parentConstraints.gridheight = 1;
        this.setLayout(parentLayout);
        this.add(all, parentConstraints);

        // Propagate all listeners
        Stream.<Component>of(all, leftPanel).forEach(component -> Arrays.stream(getKeyListeners()).forEach(component::addKeyListener));
        // Markers addition handlers
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(new KeyboardEventDispatcher());
        this.listenerROI();
        this.handleCanvasMouseListeners(canvas);
        MainDialog.currentImage = image;
        this.addEventListenerToImage();
        WindowManager.getCurrentWindow().getCanvas().fitToWindow();
        this.pack();
        this.setVisible(true);
    }

    private void handleCanvasMouseListeners(CustomCanvas canvas) {
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                // Check if the Rois in the image have changed position by user input; if so, update the list and notify the controller
                if (jListRois.getSelectedIndices().length > 1) return;
                Rectangle bounds = getImagePlus().getRoi().getBounds();
                if (!bounds.equals(oldRect)) {
                    oldRect = (Rectangle) bounds.clone();
                    eventListener.onMainDialogEvent(new MovedRoiEvent());
                }
            }
        });
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mouseOverCanvas = true;
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseOverCanvas = false;
                super.mouseExited(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleOutOfBoundsRois();
                super.mouseReleased(e);
            }
        });
    }

    private JPanel getAlignPanel() {
        final JPanel alignJPanel = new JPanel();
        final GridBagLayout alignLayout = new GridBagLayout();
        final GridBagConstraints alignConstraints = new GridBagConstraints();
        alignJPanel.setLayout(alignLayout);

        alignJPanel.setBorder(BorderFactory.createTitledBorder("Alignment"));

        btnAlignImages.setToolTipText("Align the images based on the added corner points");
        btnAlignImages.setEnabled(false);
        btnAutoAlignImages.setToolTipText("Align the images automatically without thinking what it is needed to be done");
        btnAutoAlignImages.setEnabled(true);
        checkKeepOriginal.setToolTipText("Keep the original images boundaries, applying stitching where necessary. NOTE: this operation is resource-intensive.");
        checkKeepOriginal.setSelected(true);
        checkKeepOriginal.setEnabled(false);

        btnAlignImages.addActionListener(e -> this.eventListener.onMainDialogEvent(new AlignEvent(checkKeepOriginal.isSelected())));
        btnAutoAlignImages.addActionListener(e -> this.eventListener.onMainDialogEvent(new AutoAlignEvent(checkKeepOriginal.isSelected())));

        alignConstraints.gridx = 0;
        alignConstraints.gridy = 0;
        alignConstraints.fill = GridBagConstraints.BOTH;
        alignConstraints.gridwidth = 1;
        alignConstraints.gridheight = 1;
        alignJPanel.add(checkKeepOriginal, alignConstraints);
        alignConstraints.gridx = 0;
        alignConstraints.gridy = 1;
        alignConstraints.fill = GridBagConstraints.BOTH;
        alignConstraints.gridwidth = 1;
        alignConstraints.gridheight = 1;
        alignJPanel.add(btnAlignImages, alignConstraints);
        alignConstraints.gridx = 0;
        alignConstraints.gridy = 2;
        alignConstraints.fill = GridBagConstraints.BOTH;
        alignConstraints.gridwidth = 1;
        alignConstraints.gridheight = 1;
        alignJPanel.add(btnAutoAlignImages, alignConstraints);

        return alignJPanel;
    }

    private JPanel getActionsPanel() {
        final JPanel actionsJPanel = new JPanel();
        final GridBagLayout actionsLayout = new GridBagLayout();
        final GridBagConstraints actionsConstraints = new GridBagConstraints();

        actionsJPanel.setLayout(actionsLayout);
        actionsJPanel.setBorder(BorderFactory.createTitledBorder("Actions"));

        checkShowPreview.setToolTipText("Show a preview window");
        btnDeleteRoi.setToolTipText("Delete selected current corner point");
        btnDeleteRoi.setEnabled(false);
        btnPrevImage.setToolTipText("Select previous image in the stack");
        btnNextImage.setToolTipText("Select next image in the stack");

        final JLabel changeImageLabel = new JLabel("Press \"A\" or \"D\" to change image", LEFT);
        changeImageLabel.setForeground(Color.gray);

        actionsConstraints.gridx = 0;
        actionsConstraints.gridy = 0;
        actionsConstraints.fill = GridBagConstraints.BOTH;
        actionsConstraints.gridwidth = 1;
        actionsConstraints.gridheight = 1;
        actionsJPanel.add(changeImageLabel, actionsConstraints);
        actionsConstraints.gridx = 0;
        actionsConstraints.gridy = 1;
        actionsConstraints.fill = GridBagConstraints.BOTH;
        actionsConstraints.gridwidth = 1;
        actionsConstraints.gridheight = 1;
        actionsJPanel.add(checkShowPreview, actionsConstraints);
        actionsConstraints.gridx = 0;
        actionsConstraints.gridy = 2;
        actionsConstraints.fill = GridBagConstraints.BOTH;
        actionsConstraints.gridwidth = 1;
        actionsConstraints.gridheight = 1;
        actionsJPanel.add(btnDeleteRoi, actionsConstraints);
        actionsConstraints.gridx = 0;
        actionsConstraints.gridy = 3;
        actionsConstraints.fill = GridBagConstraints.BOTH;
        actionsConstraints.gridwidth = 1;
        actionsConstraints.gridheight = 1;
        actionsJPanel.add(btnPrevImage, actionsConstraints);
        actionsConstraints.gridx = 0;
        actionsConstraints.gridy = 4;
        actionsConstraints.fill = GridBagConstraints.BOTH;
        actionsConstraints.gridwidth = 1;
        actionsConstraints.gridheight = 1;
        actionsJPanel.add(btnNextImage, actionsConstraints);

        this.jListRois.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.jListRois.setModel(this.jListRoisModel);
        this.jListRois.addListSelectionListener(e -> {
            final int[] indices = this.jListRois.getSelectedIndices();
            if (indices.length > 1) {
                this.eventListener.onMainDialogEvent(new SelectedRoisEvent(indices));
            }
            if (indices.length == 1) {
                this.eventListener.onMainDialogEvent(new SelectedRoiEvent(indices[0]));
            }
            btnDeleteRoi.setEnabled(indices.length != 0);
        });

        checkShowPreview.addItemListener(e -> this.eventListener.onMainDialogEvent(new PreviewImageEvent(checkShowPreview.isSelected())));
        btnDeleteRoi.addActionListener(e -> {
            final int[] indices = this.jListRois.getSelectedIndices();
            this.eventListener.onMainDialogEvent(new DeleteRoisEvent(indices));
        });
        btnPrevImage.addActionListener(e -> {
            titleHasToChange = true;
            this.eventListener.onMainDialogEvent(new ChangeImageEvent(ChangeImageEvent.ChangeDirection.PREV));
        });
        btnNextImage.addActionListener(e -> {
            titleHasToChange = true;
            this.eventListener.onMainDialogEvent(new ChangeImageEvent(ChangeImageEvent.ChangeDirection.NEXT));
        });

        return actionsJPanel;
    }

    private JPanel getCornersPanel() {
        final JPanel cornersJPanel = new JPanel();
        final GridBagLayout cornersLayout = new GridBagLayout();
        final GridBagConstraints cornersConstraints = new GridBagConstraints();
        cornersJPanel.setLayout(cornersLayout);
        cornersJPanel.setBorder(BorderFactory.createTitledBorder("Corners"));
        JLabel cornerLabel = new JLabel("Press \"C\" to add a corner point");
        cornerLabel.setForeground(Color.gray);

        this.jListRois = new JList<>();
        JScrollPane scrollPane = new JScrollPane(this.jListRois);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(180, 180));
        scrollPane.setMinimumSize(new Dimension(180, 180));
        scrollPane.setMaximumSize(new Dimension(180, 180));
        this.jListRois.setBackground(Color.white);
        this.jListRois.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        btnCopyCorners.setEnabled(false);
        btnCopyCorners.setToolTipText("Select from which image you'll copy-paste the corners");
        btnCopyCorners.addActionListener(e -> this.eventListener.onMainDialogEvent(new CopyCornersEvent()));

        cornersConstraints.gridx = 0;
        cornersConstraints.gridy = 0;
        cornersConstraints.fill = GridBagConstraints.BOTH;
        cornersConstraints.gridwidth = 1;
        cornersConstraints.gridheight = 1;
        cornersJPanel.add(cornerLabel, cornersConstraints);
        cornersConstraints.gridx = 0;
        cornersConstraints.gridy = 1;
        cornersConstraints.fill = GridBagConstraints.BOTH;
        cornersConstraints.gridwidth = 1;
        cornersConstraints.gridheight = 1;
        cornersJPanel.add(scrollPane, cornersConstraints);
        cornersConstraints.gridx = 0;
        cornersConstraints.gridy = 2;
        cornersConstraints.fill = GridBagConstraints.BOTH;
        cornersConstraints.gridwidth = 1;
        cornersConstraints.gridheight = 1;
        cornersJPanel.add(btnCopyCorners, cornersConstraints);

        return cornersJPanel;
    }

    private JPanel getMenuPanel() {
        final JMenu fileMenu = new JMenu("File");
        final JMenuItem loadProjectItem = new JMenuItem("Load Project");
        loadProjectItem.addActionListener(e -> this.eventListener.onMainDialogEvent(new LoadProjectEvent()));
        fileMenu.add(loadProjectItem);
        final JMenuItem saveProjectItem = new JMenuItem("Save Project");
        saveProjectItem.addActionListener(e -> this.eventListener.onMainDialogEvent(new SaveProjectEvent()));
        fileMenu.add(saveProjectItem);
        fileMenu.addSeparator();
        final JMenuItem addToCurrentStackItem = new JMenuItem("Add images to current stack");
        addToCurrentStackItem.addActionListener(e -> FileService.promptForFiles().forEach(path -> this.eventListener.onMainDialogEvent(new AddFileEvent(path))));
        fileMenu.add(addToCurrentStackItem);
        final JMenuItem removeImageItem = new JMenuItem("Remove image...");
        removeImageItem.addActionListener(e -> this.eventListener.onMainDialogEvent(new RemoveImageEvent()));
        fileMenu.add(removeImageItem);
        fileMenu.addSeparator();
        final JMenu aboutMenu = new JMenu("?");
        final JMenuItem aboutItem = new JMenuItem("About...");
        aboutItem.addActionListener(e -> this.eventListener.onMainDialogEvent(new OpenAboutEvent()));
        aboutMenu.add(aboutItem);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(aboutMenu);
        final JPanel menuPanel = new JPanel();
        menuPanel.add(menuBar);
        return menuPanel;
    }

    private void handleOutOfBoundsRois() {
        final Roi[] roisAsArray = this.image.getManager().getRoisAsArray();
        final List<Integer> roisToDelete = new ArrayList<>();
        for (int index = 0; index < this.image.getManager().getRoisAsArray().length; index++) {
            final Roi roi = roisAsArray[index];
            Rectangle2D.Double bounds = roi.getFloatBounds();
            if (bounds.getX() < image.getWidth() && bounds.getY() < image.getHeight()) {
                continue;
            }
            roisToDelete.add(index);
        }
        if (!roisToDelete.isEmpty()) {
            String indexesJoinedMessage = roisToDelete.stream().map(value -> String.valueOf(value + 1)).collect(Collectors.joining(", "));
            String message = roisToDelete.size() == 1 ? String.format("Corner %s is going to be deleted", indexesJoinedMessage) : String.format("Corners %s are going to be deleted", indexesJoinedMessage);
            int answer = JOptionPane.showOptionDialog(null, message, "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[]{"OK"}, new String[]{"OK"});
            if (answer == 0) {
                int[] indices = roisToDelete.stream().mapToInt(value -> value).toArray();
                this.eventListener.onMainDialogEvent(new DeleteRoisEvent(indices));
            }
        }
    }

    private void listenerROI() {
        Roi.addRoiListener((imagePlus, event) -> {
            if (event != RoiListener.MOVED) {
                return;
            }
            final int[] indices = this.jListRois.getSelectedIndices();
            // IF MORE THAN ONE IS SELECTED IN JLIST THEN APPLY TRANSLATION TO ALL AT ONCE
            if (indices.length > 1) {
                // FIND THE ROI THAT HAS BEEN CLICKED DIRECTLY
                final Roi selectedRoi = imagePlus.getRoi();
                final Rectangle2D.Double bounds = selectedRoi.getFloatBounds();
                Roi[] roisAsArray = this.image.getManager().getRoisAsArray();
                if (isNotTheSameRoi(selectedRoi)) {
                    this.lastBound = null;
                }
                if (this.lastBound == null) {
                    this.lastBound = (Rectangle2D.Double) bounds.clone();
                }
                final double translationX = bounds.getX() - this.lastBound.getX();
                final double translationY = bounds.getY() - this.lastBound.getY();
                if (isNotTheSameBounds(bounds)) {
                    this.lastBound = (Rectangle2D.Double) bounds.clone();
                    this.handleMultipleRoisTranslation(translationX, translationY, roisAsArray, indices);
                }
                this.lastRoi = selectedRoi;
            }
        });
    }

    private boolean isNotTheSameBounds(Rectangle2D bounds) {
        return !Objects.equals(bounds, this.lastBound);
    }

    private boolean isNotTheSameRoi(Roi selectedRoi) {
        return !Objects.equals(selectedRoi, this.lastRoi);
    }

    private void handleMultipleRoisTranslation(double translationX, double translationY, Roi[] roisAsArray, int[] indices) {
        // THEN REGISTER THE CHANGE FOR ALL
        for (int index : indices) {
            final Roi roi = roisAsArray[index];
            final Rectangle2D.Double bounds = roi.getFloatBounds();
            roi.setLocation(bounds.x + translationX, bounds.y + translationY);
            final Rectangle2D.Double finalBounds = roi.getFloatBounds();
            this.jListRoisModel.set(index, MessageFormat.format("{0} - {1},{2}", index + 1, finalBounds.x, finalBounds.y));
        }
    }

    /**
     * Change the actual image displayed in the main view, based on the given BufferedImage instance
     *
     * @param image
     */
    public void changeImage(BufferedImage image) {
        if (image != null) {
            MainDialog.currentImage = image;
            this.setImage(image);
            image.backupRois();
            image.getManager().reset();
            this.image = image;
            this.btnDeleteRoi.setEnabled(jListRois.getSelectedIndices().length != 0);
            this.image.restoreRois();
            WindowManager.getCurrentWindow().getCanvas().fitToWindow();
            this.drawRois(image.getManager());
            this.addEventListenerToImage();
            IJ.selectWindow(this.getImagePlus().getID());
            this.pack();
        }
    }

    /**
     * Adds an event listener to the current image
     */
    private void addEventListenerToImage() {
        MainDialog root = this;
        this.image.addEventListener(event -> {
            if (event instanceof RoiSelectedEvent) {
                // if a roi is marked as selected, select the appropriate ROI in the listbox in the left of the window
                RoiSelectedEvent roiSelectedEvent = (RoiSelectedEvent) event;
                int index = Arrays.asList(root.image.getManager().getRoisAsArray()).indexOf(roiSelectedEvent.getRoiSelected());
                this.eventListener.onMainDialogEvent(new SelectedRoiFromOvalEvent(index));
            }
        });
    }

    /**
     * Update the Roi List based on the given RoiManager instance
     * // THIS PIECE OF CODE IS REPEATED A LOT OF TIMES //
     * // TAKE IT INTO ACCOUNT WHEN MAKING THE SERIOUS REFACTORING //
     *
     * @param manager
     */
    public void drawRois(RoiManager manager) {
        Prefs.useNamesAsLabels = true;
        Prefs.noPointLabels = false;
        int strokeWidth = Math.max((int) (this.image.getWidth() * 0.0025), 30);
        Overlay over = new Overlay();
        over.drawBackgrounds(false);
        over.drawLabels(false);
        over.drawNames(true);
        over.setLabelFontSize(Math.round(strokeWidth * 1f), SCALE_OPTION);
        over.setLabelColor(Color.CYAN);
        over.setStrokeWidth((double) strokeWidth);
        over.setStrokeColor(Color.CYAN);
        Arrays.stream(this.image.getManager().getRoisAsArray()).forEach(over::add);
        this.renameRois();
        this.image.getManager().setOverlay(over);
        this.refreshROIList(manager);
        this.btnDeleteRoi.setEnabled(this.jListRois.getSelectedIndices().length != 0);
    }

    private void renameRois() {
        for (int index = 0; index < this.image.getManager().getRoisAsArray().length; index++) {
            this.image.getManager().rename(index, String.valueOf(index + 1));
        }
    }

    public void refreshROIList(RoiManager manager) {
        this.jListRoisModel.removeAllElements();
        final String pattern = "{0} - {1},{2}";
        int index = 0;
        for (final Roi roi : manager.getRoisAsArray()) {
            final int x = (int) roi.getXBase() + (int) (roi.getFloatWidth() / 2);
            final int y = (int) roi.getYBase() + (int) (roi.getFloatHeight() / 2);
            this.jListRoisModel.add(index, MessageFormat.format(pattern, index + 1, x, y));
            index++;
        }
    }

    public void setPreviewWindowCheckBox(boolean value) {
        this.checkShowPreview.setSelected(value);
    }

    public void setNextImageButtonEnabled(boolean enabled) {
        this.btnNextImage.setEnabled(enabled);
    }

    public void setPrevImageButtonEnabled(boolean enabled) {
        this.btnPrevImage.setEnabled(enabled);
    }

    public void setAlignButtonEnabled(boolean enabled) {
        this.btnAlignImages.setEnabled(enabled);
    }

    public void setAutoAlignButtonEnabled(boolean enabled) {
        this.btnAutoAlignImages.setEnabled(enabled);
        this.checkKeepOriginal.setEnabled(enabled);
    }

    public void setCopyCornersEnabled(boolean enabled) {
        this.btnCopyCorners.setEnabled(enabled);
    }

    @Override
    public synchronized void mouseWheelMoved(MouseWheelEvent e) {
        titleHasToChange = false;
        super.mouseWheelMoved(e);
    }

    @Override
    public void windowStateChanged(WindowEvent e) {
        titleHasToChange = false;
        super.windowStateChanged(e);
    }

    @Override
    public void setTitle(String title) {
        if (titleHasToChange) {
            super.setTitle(DIALOG_STATIC_TITLE + " " + title);
        }
    }

    private class KeyboardEventDispatcher implements KeyEventDispatcher {
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            boolean isReleased = e.getID() == KeyEvent.KEY_RELEASED;
            if (isReleased && e.getKeyCode() == KeyEvent.VK_C && mouseOverCanvas) {
                Point point = getCanvas().getCursorLoc();
                Pair<BigDecimal, BigDecimal> clickCoordinates = new Pair<>(BigDecimal.valueOf(point.getX()), BigDecimal.valueOf(point.getY()));
                eventListener.onMainDialogEvent(new AddRoiEvent(clickCoordinates));
            }
            return false;
        }
    }
}
