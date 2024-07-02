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
package org.jkiss.dbeaver.ext.import_config.wizards.idea;

import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizard;
import org.jkiss.dbeaver.ext.import_config.wizards.idea.page.ConfigImportWizardPageIdeaConnections;
import org.jkiss.dbeaver.ext.import_config.wizards.idea.page.ConfigImportWizardPageIdesSettings;
import org.jkiss.dbeaver.model.connection.DBPDriver;

import java.io.File;

public class ConfigImportWizardIdea extends ConfigImportWizard {

    //private ConfigImportWizardPageIdeaDriver pageDriver;
    private ConfigImportWizardPageIdesSettings pageSettings;

    public enum ImportType {
        CSV,
        XML
    }

    @Override
    protected ConfigImportWizardPageIdeaConnections createMainPage() {
        return new ConfigImportWizardPageIdeaConnections();
    }

    @Override
    public void addPages() {
        //pageDriver = new ConfigImportWizardPageIdeaDriver();
        pageSettings = new ConfigImportWizardPageIdesSettings();

       // addPage(pageDriver);
        addPage(pageSettings);
        super.addPages();
    }

    public DBPDriver getDriver() {
        return null;
    }

    public ConfigImportWizardIdea.ImportType getImportType() {
        return pageSettings.getImportType();
    }

    public File getInputFile() {
        return pageSettings.getInputFile();
    }

    public String getInputFileEncoding() {
        return pageSettings.getInputFileEncoding();
    }

}