package ds4h.dialog.loading;

import ds4h.utils.Utilities;
import ij.IJ;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

import static javax.swing.SwingConstants.CENTER;

public class LoadingDialog extends JDialog {
  public LoadingDialog() {
    super();
    ImageIcon loading = null;
    try {
      byte[] bytes = Utilities.inputStreamToByteArray(getClass().getResourceAsStream("/spinner.gif"));
      loading = new ImageIcon(bytes);
    } catch (IOException e) {
      IJ.showMessage(e.getMessage());
    }
    this.setLayout(new BorderLayout(0, 10));
    this.add(new JLabel("", loading, CENTER), BorderLayout.CENTER);
    this.add(new JLabel("Working in progress...", CENTER), BorderLayout.SOUTH);
    this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    this.setUndecorated(true);
    this.setModalityType(ModalityType.APPLICATION_MODAL);
    this.setSize(400, 200);
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    this.setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
  }
  
  public void showDialog() {
    SwingUtilities.invokeLater(() -> setVisible(true));
  }
  
  public void hideDialog() {
    SwingUtilities.invokeLater(() -> setVisible(false));
  }
}
