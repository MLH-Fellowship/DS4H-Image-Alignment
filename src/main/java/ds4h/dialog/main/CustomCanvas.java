package ds4h.dialog.main;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class CustomCanvas extends ImageCanvas {
  public CustomCanvas(ImagePlus imp) {
    super(imp);
    final Dimension dim = new Dimension(Math.min(512, imp.getWidth()), Math.min(512, imp.getHeight()));
    setMinimumSize(dim);
    setSize(dim.width, dim.height);
    setDstDimensions(dim.width, dim.height);
    addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent ke) {
        repaint();
      }
    });
  }
  
  public void setDstDimensions(int width, int height) {
    super.dstWidth = width;
    super.dstHeight = height;
    // adjust srcRect: can it grow/shrink?
    int w = Math.min((int) (width / magnification), imp.getWidth());
    int h = Math.min((int) (height / magnification), imp.getHeight());
    int x = srcRect.x;
    if (x + w > imp.getWidth()) x = w - imp.getWidth();
    int y = srcRect.y;
    if (y + h > imp.getHeight()) y = h - imp.getHeight();
    srcRect.setRect(x, y, w, h);
    repaint();
  }
  
  @Override
  public void paint(Graphics g) {
    final Rectangle srcRect = getSrcRect();
    final double mag = getMagnification();
    final int dw = (int) (srcRect.width * mag);
    final int dh = (int) (srcRect.height * mag);
    g.setClip(0, 0, dw, dh);
    super.paint(g);
    final int w = getWidth();
    final int h = getHeight();
    g.setClip(0, 0, w, h);
    // Paint away the outside
    g.setColor(getBackground());
    g.fillRect(dw, 0, w - dw, h);
    g.fillRect(0, dh, w, h - dh);
  }
}
