package org.jkiss.dbeaver.ext.cratedb.model;

import org.jkiss.dbeaver.ext.postgresql.edit.PostgreTableColumnManager;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;

import java.util.Map;

public class CrateTableColumnManager extends PostgreTableColumnManager {

    @Override
    protected ColumnModifier[] getSupportedModifiers(
        PostgreTableColumn column,
        Map<String, Object> options) {
        return new ColumnModifier[] {
            DataTypeModifier, NotNullModifier
        };
    }
//    @Override
//    protected ColumnModifier[] getSupportedModifiers(
//        PostgreTableColumnManager column, Map<String, Object> options
//    ) {
//        return new ColumnModifier[]{
//            DataTypeModifier,  NotNullModifier
//        };
//    }

}
