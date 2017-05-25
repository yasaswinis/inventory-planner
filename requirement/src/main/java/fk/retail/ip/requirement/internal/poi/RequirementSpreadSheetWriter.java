package fk.retail.ip.requirement.internal.poi;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetProtection;

import fk.retail.ip.core.enums.RequirementExcelHeaders;
import fk.retail.ip.core.poi.SpreadSheetWriter;

/**
 * Created by nidhigupta.m on 14/03/17.
 */
public class RequirementSpreadSheetWriter extends SpreadSheetWriter {

    protected void validateSheet(XSSFWorkbook wb)  {

        Sheet s = wb.getSheetAt(0);
        // hide 1st column with requirement_id header
        s.setColumnHidden(0,true);

        //set password for sheet
        s.protectSheet("uneditable");

        // protect sheet with some options
        XSSFSheet sheet = ((XSSFSheet)s);


        sheet.enableLocking();
        CTSheetProtection sheetProtection = sheet.getCTWorksheet().getSheetProtection();
        sheetProtection.setSelectLockedCells(false);
        sheetProtection.setSelectUnlockedCells(false);
        sheetProtection.setFormatCells(false);
        sheetProtection.setFormatColumns(false);
        sheetProtection.setFormatRows(false);
        sheetProtection.setInsertColumns(true);
        sheetProtection.setInsertRows(true);
        sheetProtection.setInsertHyperlinks(true);
        sheetProtection.setDeleteColumns(true);
        sheetProtection.setDeleteRows(true);
        sheetProtection.setSort(false);
        sheetProtection.setAutoFilter(false);
        sheetProtection.setPivotTables(true);
        sheetProtection.setObjects(true);
        sheetProtection.setScenarios(true);
        // removing the hidden column with -1
        int noOfColumns = sheet.getRow(0).getLastCellNum() - 1;
        int noOfRows = sheet.getLastRowNum();
        sheet.setAutoFilter(new CellRangeAddress(0, noOfRows, 0, noOfColumns));

    }

    protected void applyCellStyle(CellStyle editableStyle,CellStyle uneditableStyle,Cell cell, String columnName) {

        if (RequirementExcelHeaders.getLockedHeaders().contains(RequirementExcelHeaders.fromString(columnName))) {
            cell.setCellStyle(uneditableStyle);
        } else {
            cell.setCellStyle(editableStyle);
        }
    }
}
