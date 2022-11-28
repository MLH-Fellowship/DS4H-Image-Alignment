package ds4h.dialog.align;

import ds4h.dialog.align.event.ReuseImageEvent;
import ds4h.dialog.align.event.SaveEvent;
import ds4h.services.loader.LibraryLoader;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class AlignDialog extends StackWindow {
    private final OnAlignDialogEventListener listener;

    public AlignDialog(ImagePlus img, OnAlignDialogEventListener listener) {
        super(img);
        this.listener = listener;
        this.setTitle("Output Stack");
        if (LibraryLoader.getOS().startsWith("Mac")) {
            handleMacLayout();;
        } else {
            setImageJMenuBar(this);
            setMenuBar(getMenuBar());
        }
    }

    private void handleMacLayout() {
        final Panel all = new Panel();
        final JMenuBar menuBar = getJMenuBar();
        final ImageCanvas canvas = getCanvas();
        setLayout(new BorderLayout());
        all.setLayout(new BorderLayout());
        all.add(menuBar, BorderLayout.NORTH);
        all.add(canvas, BorderLayout.CENTER);
        all.add(sliceSelector, BorderLayout.PAGE_END);
        add(all, BorderLayout.CENTER);
    }

    @Override
    public MenuBar getMenuBar() {
        final MenuBar menuBar = new MenuBar();
        final Menu fileMenu = new Menu("File");
        final MenuItem saveAsItem = new MenuItem("Save as...");
        final MenuItem reuseAsItem = new MenuItem("Reuse as source");
        saveAsItem.addActionListener(e -> {
            try {
                getListener().onAlignDialogEventListener(new SaveEvent());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        reuseAsItem.addActionListener(e -> {
            try {
                getListener().onAlignDialogEventListener(new ReuseImageEvent());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        fileMenu.add(saveAsItem);
        fileMenu.add(reuseAsItem);
        menuBar.add(fileMenu);
        return menuBar;
    }

    public JMenuBar getJMenuBar() {
        final JMenuBar menuBar = new JMenuBar();
        final JMenu fileMenu = new JMenu("File");
        final JMenuItem saveAsItem = new JMenuItem("Save as...");
        final JMenuItem reuseAsItem = new JMenuItem("Reuse as source");
        saveAsItem.addActionListener(e -> {
            try {
                getListener().onAlignDialogEventListener(new SaveEvent());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        reuseAsItem.addActionListener(e -> {
            try {
                getListener().onAlignDialogEventListener(new ReuseImageEvent());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        fileMenu.add(saveAsItem);
        fileMenu.add(reuseAsItem);
        menuBar.add(fileMenu);
        return menuBar;
    }

    public OnAlignDialogEventListener getListener() {
        return listener;
    }
}
