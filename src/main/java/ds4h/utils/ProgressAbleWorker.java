package ds4h.utils;

import javax.swing.*;

public abstract class ProgressAbleWorker<K, V> extends SwingWorker<K, V> {
    public final void startProgress() {
        setProgress(0);
    }

    public final void doneProgress() {
        setProgress(100);
    }
}
