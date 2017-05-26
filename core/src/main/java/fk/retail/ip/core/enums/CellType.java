package fk.retail.ip.core.enums;

/**
 * Created by agarwal.vaibhav on 04/05/17.
 */
public class CellType {
    /*This stores the information based on column type. Like which column has to be parsed with which logic.
    * If it has to be parsed as an integer or double or string value*/
    public static ColumnType getType(String columnName) {
        switch(columnName) {
            case "IPC Quantity Override":
                return ColumnType.INTEGER;
            case "CDO Price Override":
                return ColumnType.DOUBLE;
            case "CDO Quantity Override":
                return ColumnType.INTEGER;
            case "BizFin Quantity Recommendation":
                return ColumnType.INTEGER;
            case "New SLA":
                return ColumnType.INTEGER;
            default:
                return ColumnType.STRING;

        }
    }
}
