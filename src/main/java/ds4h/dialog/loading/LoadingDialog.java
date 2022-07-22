package ds4h.dialog.loading;

import ds4h.utils.ProgressAbleWorker;
import ds4h.utils.Utilities;
import ij.IJ;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

import static javax.swing.SwingConstants.CENTER;

public class LoadingDialog extends JDialog {
  private final ProgressAbleWorker<Void, Void> worker = new ProgressAbleWorker<Void, Void>() {
    @Override
    protected Void doInBackground() throws Exception {
      Thread.sleep(500);
      return null;
    }
  };

  public LoadingDialog() {
    super();
    this.createDialog();
  }

  private void createDialog() {
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
    // This modality type assures that it doesn't block the execution
    this.setModalityType(ModalityType.MODELESS);
    this.setSize(400, 200);
    Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
    this.setLocation(dimension.width / 2 - this.getSize().width / 2, dimension.height / 2 - this.getSize().height / 2);
    this.getWorker().addPropertyChangeListener(evt -> {
      if ("progress".equals(evt.getPropertyName())) {
        this.setVisible((Integer) evt.getNewValue() == 0);
      }
    });
    this.getWorker().execute();
  }

  public void showDialog() {
      repaint();
      this.getWorker().startProgress();
  }
  
  public void hideDialog() {
    repaint();
    this.getWorker().doneProgress();
  }


  public ProgressAbleWorker<Void, Void> getWorker() {
    return this.worker;
  }
}
