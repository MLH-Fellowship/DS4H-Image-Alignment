package ds4h.dialog.main.event;

import ds4h.utils.Pair;

import java.math.BigDecimal;

public class AddRoiEvent implements IMainDialogEvent {
  private final Pair<BigDecimal, BigDecimal> coordinates;
  
  public AddRoiEvent(Pair<BigDecimal, BigDecimal> coordinates) {
    this.coordinates = coordinates;
  }
  
  public Pair<BigDecimal, BigDecimal> getClickCoordinates() {
    return this.coordinates;
  }
}
