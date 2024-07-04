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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.import_config.wizards.idea.postprocessor.api.IdeaImportConfigPostProcessor;

import java.util.Map;

public class OracleIdeaImportPostProcessor implements IdeaImportConfigPostProcessor {
    @Override
    public void postProcess(ImportConnectionInfo connectionInfo, Map<String, String> connectionProps) {
        String url = connectionInfo.getUrl();
        String sidServiceValue;
        //todo there could be other connection types
        if (isSidConnectionType(url)){
            //fixme const??
            sidServiceValue = "SID";
        }else{
            sidServiceValue = "SERVICE";
        }
        connectionInfo.setProviderProperty("@dbeaver-sid-service@", sidServiceValue);
        connectionInfo.setProviderProperty("@dbeaver-connection-type@", "BASIC");
    }

    @Override
    public String getHandledDriverId() {
        return "oracle_thin";
    }

    private boolean isSidConnectionType(@Nullable String url) {
        if (url == null) return true;

        //sid: jdbc:oracle:thin:@localhost:1521:orcl
        //service name: jdbc:oracle:thin:@//localhost:1521/orcl
        int i = url.indexOf("@");
        if (i + 1 > url.length() && url.charAt(i + 1) == '/') return false;
        return true;
    }
}
