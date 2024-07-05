/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.ext.import_config.wizards.idea.page;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportData;
import org.jkiss.dbeaver.ext.import_config.wizards.idea.ConfigImportWizardIdea;
import org.jkiss.dbeaver.ext.import_config.wizards.idea.IdeaDataSourceConfigService;

import java.io.File;
import java.util.Map;


public class ConfigImportWizardPageIdeaConnections extends ConfigImportWizardPage {
    private static final Log log = Log.getLog(ConfigImportWizardPageIdeaConnections.class);
    IdeaDataSourceConfigService ideaDataSourceConfigService = IdeaDataSourceConfigService.INSTANCE;


    public ConfigImportWizardPageIdeaConnections() {
        super("Connections");
        setTitle("Connections");
        setDescription("Import connections");
    }

    @Override
    protected void loadConnections(ImportData importData) throws DBException {
        setErrorMessage(null);
        try {
            tryLoadConnection(importData);
        } catch (Exception e) {
            setErrorMessage(e.getMessage());
        }
    }

    private void tryLoadConnection(ImportData importData) throws Exception {

        ConfigImportWizardIdea wizard = (ConfigImportWizardIdea) getWizard();
        File ideaDirectory = wizard.getInputFile();
        Map<String, Map<String, String>> uuidToDataSourceProps = ideaDataSourceConfigService.buildIdeaConfigProps(
                ideaDirectory.getPath(), wizard.getInputFileEncoding());
        for (Map<String, String> dataSourceProps : uuidToDataSourceProps.values()) {
            ImportConnectionInfo connectionInfo = ideaDataSourceConfigService.buildIdeaConnectionFromProps(dataSourceProps);
            importData.addDriver(connectionInfo.getDriverInfo());
            importData.addConnection(connectionInfo);
        }
    }
}
