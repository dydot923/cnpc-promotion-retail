package com.cnpc.promoretail.importcenter.model;

import java.util.List;

public record RawExcelRow(
        String sheetName,
        int rowNumber,
        List<String> cells
) {

    public RawExcelRow {
        cells = cells == null ? List.of() : List.copyOf(cells);
    }

    public String cell(int index) {
        if (index < 0 || index >= cells.size()) {
            return "";
        }
        return cells.get(index);
    }
}
