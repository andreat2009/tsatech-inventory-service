package com.newproject.inventory.dto;

import java.util.ArrayList;
import java.util.List;

public class StockReservationRequest {
    private List<StockLineRequest> lines = new ArrayList<>();

    public List<StockLineRequest> getLines() { return lines; }
    public void setLines(List<StockLineRequest> lines) { this.lines = lines; }
}
