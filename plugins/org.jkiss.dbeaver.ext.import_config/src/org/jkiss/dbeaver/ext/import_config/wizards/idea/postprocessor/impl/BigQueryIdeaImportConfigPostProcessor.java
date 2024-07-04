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
package org.jkiss.dbeaver.ext.import_config.wizards.idea.postprocessor.impl;

import org.jkiss.dbeaver.ext.import_config.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.import_config.wizards.idea.postprocessor.api.IdeaImportConfigPostProcessor;

import java.util.Map;

public class BigQueryIdeaImportConfigPostProcessor implements IdeaImportConfigPostProcessor {
    @Override
    public void postProcess(ImportConnectionInfo connectionInfo, Map<String, String> connectionProps) {
        String authProvider = connectionProps.get("auth-provider");
//        connectionInfo.setProperty();
    }

    @Override
    public String getHandledDriverId() {
        //fixme const
        return "google_bigquery_jdbc_simba_pro";
    }
}
