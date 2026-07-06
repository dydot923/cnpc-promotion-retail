package com.cnpc.promoretail.importcenter.excel;

import com.alibaba.excel.EasyExcel;
import com.cnpc.promoretail.importcenter.model.RawExcelRow;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EasyExcelWorkbookReader {

    public List<RawExcelRow> readSheet(Path file, int sheetNo, int headRowNumber) {
        RawExcelRowReadListener listener = new RawExcelRowReadListener();
        try (InputStream inputStream = Files.newInputStream(file)) {
            EasyExcel.read(inputStream, listener)
                    .sheet(sheetNo)
                    .headRowNumber(headRowNumber)
                    .doRead();
            return listener.rows();
        } catch (IOException exception) {
            throw new IllegalStateException("读取 Excel 文件失败: " + file, exception);
        }
    }

    public List<RawExcelRow> readSheet(Path file, String sheetName, int headRowNumber) {
        RawExcelRowReadListener listener = new RawExcelRowReadListener();
        try (InputStream inputStream = Files.newInputStream(file)) {
            EasyExcel.read(inputStream, listener)
                    .sheet(sheetName)
                    .headRowNumber(headRowNumber)
                    .doRead();
            return listener.rows();
        } catch (IOException exception) {
            throw new IllegalStateException("读取 Excel 文件失败: " + file, exception);
        }
    }
}
