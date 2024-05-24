package org.jkiss.dbeaver.ext.cratedb.model;

import org.jkiss.dbeaver.ext.postgresql.model.generic.PostgreMetaModel;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class CrateMetaModel extends PostgreMetaModel {
   
    public CrateMetaModel() {
        System.out.println("CrateMetaModel.CrateMetaModel()");
    }

    @Override
    public boolean supportsNotNullColumnModifiers(
        DBSObject object) {
        return false;
    }
}
