package com.cnpc.promoretail.importcenter.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.cnpc.promoretail.importcenter.model.RawExcelRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

final class RawExcelRowReadListener extends AnalysisEventListener<Map<Integer, String>> {

    private final List<RawExcelRow> rows = new ArrayList<>();

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        if (data == null || data.isEmpty()) {
            return;
        }
        int maxColumnIndex = data.keySet().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(-1);
        List<String> cells = IntStream.rangeClosed(0, maxColumnIndex)
                .mapToObj(index -> normalize(data.get(index)))
                .toList();
        rows.add(new RawExcelRow(
                context.readSheetHolder().getSheetName(),
                context.readRowHolder().getRowIndex() + 1,
                cells
        ));
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // Rows are consumed by ImportCenterService after EasyExcel finishes streaming the sheet.
    }

    List<RawExcelRow> rows() {
        return List.copyOf(rows);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
