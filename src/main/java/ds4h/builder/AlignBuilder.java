package ds4h.builder;

import ds4h.dialog.align.AlignDialog;
import ds4h.dialog.main.event.RegistrationEvent;

import java.util.List;

public interface AlignBuilder {
    void setTempImages(List<String> tempImages);

    void init();

    boolean check();

    RegistrationEvent getEvent();

    void alignKeepOriginal();

    void align();

    void build();

    AlignDialog getAlignDialog();

    List<String> getTempImages();
}
