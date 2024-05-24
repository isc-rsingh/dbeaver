package org.jkiss.dbeaver.ext.cratedb.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class CrateDataSourceProvider extends PostgreDataSourceProvider {

    @Override
    public DBPDataSource openDataSource(
        DBRProgressMonitor monitor,
        DBPDataSourceContainer container) throws DBException {
         return new CrateDataSource(monitor, container);
    }
    
    @Override
    public void init(DBPPlatform platform) {
        // TODO Auto-generated method stub
        super.init(platform);
    }
}
