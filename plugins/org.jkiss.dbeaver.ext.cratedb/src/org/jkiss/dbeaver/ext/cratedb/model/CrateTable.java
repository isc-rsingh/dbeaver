package org.jkiss.dbeaver.ext.cratedb.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableRegular;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;

public class CrateTable extends PostgreTableRegular {

    public CrateTable(PostgreSchema catalog) {
        super(catalog);
    }

    public CrateTable(PostgreSchema catalog, ResultSet dbResult) {
        super(catalog, dbResult);
    }

    @Override
    public PostgreTableColumn createTableColumn(
        DBRProgressMonitor monitor,
        PostgreSchema schema,
        JDBCResultSet dbResult) throws DBException {
        return new CrateTableColumn(monitor, this, dbResult);
    }

}
