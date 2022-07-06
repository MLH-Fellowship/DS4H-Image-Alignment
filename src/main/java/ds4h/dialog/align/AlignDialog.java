package ds4h.dialog.align;

import ds4h.dialog.align.event.ReuseImageEvent;
import ds4h.dialog.align.event.SaveEvent;
import ij.ImagePlus;
import ij.gui.StackWindow;

import java.awt.*;

public class AlignDialog extends StackWindow {
    private final OnAlignDialogEventListener listener;

    public AlignDialog(ImagePlus img, OnAlignDialogEventListener listener) {
        super(img);
        this.listener = listener;
        this.setTitle("Output Stack");
        setImageJMenuBar(this);
        setMenuBar(getMenuBar());
    }


    @Override
    public MenuBar getMenuBar() {
        final MenuBar menuBar = new MenuBar();
        final Menu fileMenu = new Menu("File");
        final MenuItem saveAsItem = new MenuItem("Save as...");
        final MenuItem reuseAsItem = new MenuItem("Reuse as source");
        saveAsItem.addActionListener(e -> getListener().onAlignDialogEventListener(new SaveEvent()));
        reuseAsItem.addActionListener(e -> getListener().onAlignDialogEventListener(new ReuseImageEvent()));
        fileMenu.add(saveAsItem);
        fileMenu.add(reuseAsItem);
        menuBar.add(fileMenu);
        return menuBar;
    }

    public OnAlignDialogEventListener getListener() {
        return listener;
    }
}
