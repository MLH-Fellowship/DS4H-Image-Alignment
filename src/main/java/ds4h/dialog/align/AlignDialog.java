package ds4h.dialog.align;

import ds4h.dialog.align.event.ExitEvent;
import ds4h.dialog.align.event.ReuseImageEvent;
import ds4h.dialog.align.event.SaveEvent;
import ij.ImagePlus;
import ij.gui.StackWindow;

import java.awt.*;

public class AlignDialog extends StackWindow {
  public AlignDialog(ImagePlus img, OnAlignDialogEventListener listener) {
    super(img);
    final MenuBar menuBar = new MenuBar();
    final Menu fileMenu = new Menu("File");
    final MenuItem saveAsItem = new MenuItem("Save as...");
    final MenuItem reuseAsItem = new MenuItem("Reuse as source");
    final MenuItem exitItem = new MenuItem("Exit");
    saveAsItem.addActionListener(e -> listener.onAlignDialogEventListener(new SaveEvent()));
    reuseAsItem.addActionListener(e -> listener.onAlignDialogEventListener(new ReuseImageEvent()));
    exitItem.addActionListener(e -> listener.onAlignDialogEventListener(new ExitEvent()));
    fileMenu.add(saveAsItem);
    fileMenu.add(reuseAsItem);
    fileMenu.addSeparator();
    fileMenu.add(exitItem);
    menuBar.add(fileMenu);
    this.setMenuBar(menuBar);
  }
}
