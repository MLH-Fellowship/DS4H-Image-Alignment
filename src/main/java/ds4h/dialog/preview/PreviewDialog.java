package ds4h.dialog.preview;

import ds4h.dialog.main.CustomCanvas;
import ds4h.dialog.main.MainDialog;
import ds4h.dialog.preview.event.ChangeImagePreviewEvent;
import ds4h.dialog.preview.event.CloseDialogEvent;
import ds4h.dialog.preview.event.IPreviewDialogEvent;
import ds4h.image.model.manager.ImagesEditor;
import ds4h.image.model.manager.slide.SlideImage;
import ij.IJ;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.Overlay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class PreviewDialog extends ImageWindow {
    private final MainDialog mainDialog;
    private ImagesEditor imagesEditor;

    public PreviewDialog(ImagesEditor imagesEditor, MainDialog mainDialog) {
        super(imagesEditor.getCurrentImage(), new CustomCanvas(imagesEditor.getCurrentImage()));
        this.mainDialog = mainDialog;
        try {
            this.imagesEditor = (ImagesEditor) imagesEditor.clone();
        } catch (CloneNotSupportedException e) {
            IJ.showMessage(e.getMessage());
        }
        SlideImage startingSlideImage = this.getImagesEditor().getCurrentImage();
        int scrollbarStartingValue = this.getImagesEditor().getCurrentPosition();
        int scrollbarMaximum = this.getImagesEditor().getAllImagesCounterSum();
        String title = "Preview Image " + (this.getImagesEditor().getCurrentPosition() + 1) + "/" + this.getImagesEditor().getAllImagesCounterSum();
        this.setImage(startingSlideImage);
        this.setTitle(title);
        final CustomCanvas canvas = (CustomCanvas) getCanvas();
        final Panel all = new Panel();

        final JScrollBar scrollbar = new JScrollBar(Adjustable.HORIZONTAL, scrollbarStartingValue, 1, 0, scrollbarMaximum);
        scrollbar.setBlockIncrement(1);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        all.setLayout(new BoxLayout(all, BoxLayout.Y_AXIS));
        all.add(canvas);
        all.add(scrollbar);
        add(all);

        all.addMouseWheelListener(e -> {
            if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL && !e.isControlDown()) {
                int totalScrollAmount = e.getUnitsToScroll() < 0 ? -1 : 1;
                if (scrollbar.getValue() + totalScrollAmount > scrollbar.getMaximum()) {
                    scrollbar.setValue(scrollbar.getMaximum());
                    return;
                }
                if (scrollbar.getValue() + totalScrollAmount < scrollbar.getMinimum()) {
                    scrollbar.setValue(scrollbar.getMinimum());
                    return;
                }
                scrollbar.setValue(scrollbar.getValue() + totalScrollAmount);
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                onPreviewDialogEvent(new CloseDialogEvent());
            }
        });
        AtomicInteger scrollBarValue = new AtomicInteger();
        scrollbar.addAdjustmentListener(e -> {
            scrollbar.updateUI();
            if (scrollBarValue.get() != scrollbar.getValue()) {
                onPreviewDialogEvent(new ChangeImagePreviewEvent(scrollbar.getValue()));
                scrollBarValue.set(scrollbar.getValue());
            }
        });
        this.setResizable(false);
        WindowManager.getCurrentWindow().getCanvas().fitToWindow();
        this.pack();
    }

    private void onPreviewDialogEvent(IPreviewDialogEvent dialogEvent) {
        if (dialogEvent instanceof ChangeImagePreviewEvent) {
            ChangeImagePreviewEvent event = (ChangeImagePreviewEvent) dialogEvent;
            SwingUtilities.invokeLater(() -> {
                SlideImage previewSlideImage = this.getImagesEditor().getSlideImage(event.getIndex());
                this.changeImage(previewSlideImage, "Preview Image " + (event.getIndex() + 1) + "/" + this.getImagesEditor().getAllImagesCounterSum());
            });
        }
        if (dialogEvent instanceof CloseDialogEvent) {
            SwingUtilities.invokeLater(() -> {
                this.getMainDialog().setPreviewWindowCheckBox(false);
                this.getMainDialog().setVisible(true);
            });
        }
    }

    public void changeImage(SlideImage slideImage, String title) {
        SwingUtilities.invokeLater(() -> {
            this.setImage(slideImage);
            ImageWindow.centerNextImage();
            this.drawRois();
            this.setTitle(title);
            WindowManager.getCurrentWindow().getCanvas().fitToWindow();
            IJ.selectWindow(this.getImagePlus().getID());
            this.pack();
        });
    }

    public void drawRois() {
        Overlay over = new Overlay();
        over.drawBackgrounds(false);
        over.drawLabels(false);
        over.drawNames(true);
        over.setLabelColor(Color.CYAN);
        over.setStrokeColor(Color.CYAN);
        int strokeWidth = Math.max((int) (this.getCurrentImage().getWidth() * 0.0025), 3);
        Arrays.stream(this.getCurrentImage().getManager().getRoisAsArray()).forEach(over::add);
        over.setLabelFontSize(Math.round(strokeWidth * 1f), "scale");
        over.setStrokeWidth((double) strokeWidth);
        this.getImagePlus().setOverlay(over);
    }

    private SlideImage getCurrentImage() {
        return (SlideImage) this.getImagePlus();
    }

    public ImagesEditor getImagesEditor() {
        return imagesEditor;
    }

    public MainDialog getMainDialog() {
        return mainDialog;
    }
}
