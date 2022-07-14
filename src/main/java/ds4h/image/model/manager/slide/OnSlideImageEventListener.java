package ds4h.image.model.manager.slide;


import ds4h.image.model.manager.slide.event.SlideImageEvent;

public interface OnSlideImageEventListener {
    void onBufferedImageEventListener(SlideImageEvent event);
}
