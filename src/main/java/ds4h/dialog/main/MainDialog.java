package ds4h.dialog.main;

import ds4h.dialog.main.event.*;
import ds4h.image.buffered.BufferedImage;
import ds4h.image.buffered.event.RoiSelectedEvent;
import ds4h.services.FileService;
import ds4h.utils.Pair;
import ij.IJ;
import ij.Prefs;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.plugin.Zoom;
import ij.plugin.frame.RoiManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
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
  private final JButton btnCopyCorners;
  private final JCheckBox checkShowPreview;
  private final JButton btnDeleteRoi;
  private final JButton btnPrevImage;
  private final JButton btnNextImage;
  private final JButton btnAlignImages;
  private final JCheckBox checkKeepOriginal;
  private final DefaultListModel<String> jListRoisModel;
  public JList<String> jListRois;
  private BufferedImage image;
  private boolean mouseOverCanvas;
  private Rectangle oldRect = null;
  // a simple debounce variable that can put "on hold" a key_release event
  private boolean debounce = false;
  private Rectangle2D.Double lastBound;
  private Roi lastRoi;
  
  public MainDialog(BufferedImage plus, OnMainDialogEventListener listener) {
    super(plus, new CustomCanvas(plus));
    this.image = plus;
    final CustomCanvas canvas = (CustomCanvas) getCanvas();
    this.checkShowPreview = new JCheckBox("Show preview window");
    this.checkShowPreview.setToolTipText("Show a preview window");
    this.btnDeleteRoi = new JButton("DELETE CORNER");
    this.btnDeleteRoi.setToolTipText("Delete current corner point selected");
    this.btnDeleteRoi.setEnabled(false);
    this.btnPrevImage = new JButton("PREV IMAGE");
    this.btnPrevImage.setToolTipText("Select previous image in the stack");
    this.btnNextImage = new JButton("NEXT IMAGE");
    this.btnNextImage.setToolTipText("Select next image in the stack");
    this.btnAlignImages = new JButton("ALIGN IMAGES VIA CORNERS");
    this.btnAlignImages.setToolTipText("Align the images based on the added corner points");
    this.btnAlignImages.setEnabled(false);
    final JButton btnAutoAlignment = new JButton("AUTO ALIGN IMAGES");
    btnAutoAlignment.setToolTipText("Align the images automatically without thinking what it is needed to be done");
    btnAutoAlignment.setEnabled(true);
    this.checkKeepOriginal = new JCheckBox("Keep all pixel data");
    this.checkKeepOriginal.setToolTipText("Keep the original images boundaries, applying stitching where necessary. NOTE: this operation is resource-intensive.");
    this.checkKeepOriginal.setSelected(true);
    this.checkKeepOriginal.setEnabled(false);
    // Remove the canvas from the window, to add it later
    this.removeAll();
    this.setTitle(DIALOG_STATIC_TITLE);
    // Training panel (left side of the GUI)
    final JPanel cornersJPanel = new JPanel();
    cornersJPanel.setBorder(BorderFactory.createTitledBorder("Corners"));
    final GridBagLayout trainingLayout = new GridBagLayout();
    final GridBagConstraints trainingConstraints = new GridBagConstraints();
    trainingConstraints.anchor = GridBagConstraints.NORTHWEST;
    trainingConstraints.fill = GridBagConstraints.HORIZONTAL;
    trainingConstraints.gridwidth = 1;
    trainingConstraints.gridheight = 1;
    trainingConstraints.gridx = 0;
    trainingConstraints.gridy = 0;
    cornersJPanel.setLayout(trainingLayout);
    JLabel cornerLabel = new JLabel("Press \"C\" to add a corner point");
    cornerLabel.setForeground(Color.gray);
    cornersJPanel.add(cornerLabel, trainingConstraints);
    trainingConstraints.gridy++;
    trainingConstraints.gridy++;
    this.jListRois = new JList<>();
    JScrollPane scrollPane = new JScrollPane(this.jListRois);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setPreferredSize(new Dimension(180, 180));
    scrollPane.setMinimumSize(new Dimension(180, 180));
    scrollPane.setMaximumSize(new Dimension(180, 180));
    cornersJPanel.add(scrollPane, trainingConstraints);
    trainingConstraints.insets = new Insets(5, 0, 10, 0);
    trainingConstraints.gridy++;
    this.jListRois.setBackground(Color.white);
    this.jListRois.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.btnCopyCorners = new JButton();
    this.btnCopyCorners.setText("COPY CORNERS");
    this.btnCopyCorners.setEnabled(false);
    cornersJPanel.add(this.btnCopyCorners, trainingConstraints);
    cornersJPanel.setLayout(trainingLayout);
    // Options panel
    JPanel actionsJPanel = new JPanel();
    actionsJPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
    final GridBagLayout actionsLayout = new GridBagLayout();
    final GridBagConstraints actionsConstraints = new GridBagConstraints();
    actionsConstraints.anchor = GridBagConstraints.NORTHWEST;
    actionsConstraints.fill = GridBagConstraints.HORIZONTAL;
    actionsConstraints.weightx = 1;
    actionsConstraints.gridx = 0;
    actionsConstraints.insets = new Insets(5, 5, 6, 6);
    actionsJPanel.setLayout(actionsLayout);
    final JLabel changeImageLabel = new JLabel("Press \"A\" or \"D\" to change image", LEFT);
    changeImageLabel.setForeground(Color.gray);
    actionsJPanel.add(changeImageLabel, actionsConstraints);
    actionsJPanel.add(checkShowPreview, actionsConstraints);
    actionsJPanel.add(btnDeleteRoi, actionsConstraints);
    actionsJPanel.add(btnPrevImage, actionsConstraints);
    actionsJPanel.add(btnNextImage, actionsConstraints);
    actionsJPanel.setLayout(actionsLayout);
    // Options panel
    final JPanel alignJPanel = new JPanel();
    alignJPanel.setBorder(BorderFactory.createTitledBorder("Alignment"));
    final GridBagLayout alignLayout = new GridBagLayout();
    final GridBagConstraints alignConstraints = new GridBagConstraints();
    alignConstraints.anchor = GridBagConstraints.NORTHWEST;
    alignConstraints.fill = GridBagConstraints.HORIZONTAL;
    alignConstraints.weightx = 1;
    alignConstraints.gridx = 0;
    alignConstraints.insets = new Insets(5, 5, 6, 6);
    alignJPanel.setLayout(alignLayout);
    alignJPanel.add(this.checkKeepOriginal, actionsConstraints);
    alignJPanel.add(this.btnAlignImages, actionsConstraints);
    alignJPanel.add(btnAutoAlignment, actionsConstraints);
    alignJPanel.setLayout(alignLayout);
    // Buttons panel
    final JPanel buttonsPanel = new JPanel();
    buttonsPanel.setBackground(Color.GRAY);
    buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
    buttonsPanel.add(cornersJPanel);
    buttonsPanel.add(actionsJPanel);
    buttonsPanel.add(alignJPanel);
    final GridBagLayout layout = new GridBagLayout();
    final GridBagConstraints allConstraints = new GridBagConstraints();
    final Panel all = new Panel();
    all.setLayout(layout);
    // sets little of padding to ensure that the @ImagePlus text is shown and not covered by the panel
    allConstraints.insets = new Insets(5, 0, 0, 0);
    allConstraints.anchor = GridBagConstraints.NORTHWEST;
    allConstraints.gridwidth = 1;
    allConstraints.gridheight = 1;
    allConstraints.gridx = 0;
    allConstraints.gridy = 0;
    allConstraints.weightx = 0;
    allConstraints.weighty = 0;
    all.add(buttonsPanel, allConstraints);
    allConstraints.gridx++;
    allConstraints.weightx = 1;
    allConstraints.weighty = 1;
    // this is just a cheap trick i made 'cause i don't properly know java swing: let's fake the background of the window so the it seems the column on the left is full length vertically
    all.setBackground(new Color(238, 238, 238));
    all.add(canvas, allConstraints);
    final GridBagLayout winBagLayout = new GridBagLayout();
    final GridBagConstraints winBagConstraints = new GridBagConstraints();
    winBagConstraints.insets = new Insets(5, 0, 0, 0);
    winBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    winBagConstraints.fill = GridBagConstraints.BOTH;
    winBagConstraints.weightx = 1;
    winBagConstraints.weighty = 1;
    this.setLayout(winBagLayout);
    this.add(all, winBagConstraints);
    // Propagate all listeners
    Stream.<Component>of(all, buttonsPanel).forEach(component -> Arrays.stream(getKeyListeners()).forEach(component::addKeyListener));
    this.eventListener = listener;
    this.btnCopyCorners.addActionListener(e -> this.eventListener.onMainDialogEvent(new CopyCornersEvent()));
    this.checkShowPreview.addItemListener(e -> this.eventListener.onMainDialogEvent(new PreviewImageEvent(this.checkShowPreview.isSelected())));
    this.btnDeleteRoi.addActionListener(e -> {
      final int[] indices = this.jListRois.getSelectedIndices();
      this.eventListener.onMainDialogEvent(new DeleteRoisEvent(indices));
    });
    this.btnPrevImage.addActionListener(e -> this.eventListener.onMainDialogEvent(new ChangeImageEvent(ChangeImageEvent.ChangeDirection.PREV)));
    this.btnNextImage.addActionListener(e -> this.eventListener.onMainDialogEvent(new ChangeImageEvent(ChangeImageEvent.ChangeDirection.NEXT)));
    this.btnAlignImages.addActionListener(e -> this.eventListener.onMainDialogEvent(new AlignEvent(this.checkKeepOriginal.isSelected())));
    btnAutoAlignment.addActionListener(e -> this.eventListener.onMainDialogEvent(new AutoAlignEvent()));
    // Markers addition handlers
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(new KeyboardEventDispatcher());
    this.listenerROI();
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
    // Rois list handling
    this.jListRois.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    this.jListRoisModel = new DefaultListModel<>();
    this.jListRois.setModel(this.jListRoisModel);
    this.jListRois.addListSelectionListener(e -> {
      final int[] indices = this.jListRois.getSelectedIndices();
      if (indices.length > 1) {
        this.eventListener.onMainDialogEvent(new SelectedRoisEvent(indices));
      }
      if (indices.length == 1) {
        this.eventListener.onMainDialogEvent(new SelectedRoiEvent(indices[0]));
      }
      this.btnDeleteRoi.setEnabled(indices.length != 0);
    });
    final MenuBar menuBar = new MenuBar();
    final Menu fileMenu = new Menu("File");
    MenuItem menuItem = new MenuItem("Open file...");
    menuItem.addActionListener(e -> this.eventListener.onMainDialogEvent(new OpenFileEvent()));
    fileMenu.add(menuItem);
    menuItem = new MenuItem("Add images to current stack");
    menuItem.addActionListener(e -> FileService.promptForFiles().forEach(path -> this.eventListener.onMainDialogEvent(new AddFileEvent(path))));
    fileMenu.add(menuItem);
    menuItem = new MenuItem("Remove image...");
    menuItem.addActionListener(e -> eventListener.onMainDialogEvent(new RemoveImageEvent()));
    fileMenu.add(menuItem);
    fileMenu.addSeparator();
    menuItem = new MenuItem("Exit");
    menuItem.addActionListener(e -> eventListener.onMainDialogEvent(new ExitEvent()));
    fileMenu.add(menuItem);
    final Menu aboutMenu = new Menu("?");
    menuItem = new MenuItem("About...");
    menuItem.addActionListener(e -> eventListener.onMainDialogEvent(new OpenAboutEvent()));
    aboutMenu.add(menuItem);
    MainDialog.currentImage = image;
    menuBar.add(fileMenu);
    menuBar.add(aboutMenu);
    this.addEventListenerToImage();
    this.setMenuBar(menuBar);
    new Zoom().run(SCALE_OPTION);
    this.pack();
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
    if (roisToDelete.size() > 0) {
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
      jListRoisModel.set(index, MessageFormat.format("{0} - {1},{2}", index + 1, finalBounds.x, finalBounds.y));
    }
  }
  
  
  /**
   * Change the actual image displayed in the main view, based on the given BufferedImage istance
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
      this.drawRois(image.getManager());
      this.addEventListenerToImage();
      // Let's call the zoom plugin to scale the image to fit in the user window
      // The zoom scaling command works on the current active window: to be 100% sure it will work, we need to forcefully select the preview window.
      IJ.selectWindow(this.getImagePlus().getID());
      new Zoom().run(SCALE_OPTION);
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
    this.checkKeepOriginal.setEnabled(enabled);
  }
  
  public void setCopyCornersEnabled(boolean enabled) {
    this.btnCopyCorners.setEnabled(enabled);
  }
  
  @Override
  public void setTitle(String title) {
    super.setTitle(DIALOG_STATIC_TITLE + " " + title);
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
      if (!debounce) {
        debounce = true;
        new Thread(() -> {
          try {
            ChangeImageEvent.ChangeDirection direction = null;
            if (isReleased && e.getKeyCode() == KeyEvent.VK_A) {
              direction = ChangeImageEvent.ChangeDirection.PREV;
            }
            if (isReleased && e.getKeyCode() == KeyEvent.VK_D) {
              direction = ChangeImageEvent.ChangeDirection.NEXT;
            }
            if (direction != null) {
              eventListener.onMainDialogEvent(new ChangeImageEvent(direction));
              e.consume();
            }
          } catch (Exception e1) {
            IJ.showMessage(e1.getMessage());
          }
        }).start();
      }
      return false;
    }
  }
}
