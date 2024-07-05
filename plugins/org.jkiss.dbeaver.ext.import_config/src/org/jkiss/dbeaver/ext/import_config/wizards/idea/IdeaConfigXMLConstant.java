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

public enum IdeaConfigXMLConstant {

    DATASOURCE_TAG("data-source"),
    PROPERTIES_TAG("property"),
    JDBC_URL("jdbc-url"),
    DRIVER_REF("driver-ref"),
    UUID_ATTRIBUTE("uuid"),


    DATABASE_NAME_PATH("database-info.dbms"),

    INTELIJ_CUSTOM_VALUE("com.intellij"),
    ;
    private final String tagName;

    IdeaConfigXMLConstant(String tagName) {
        this.tagName = tagName;
    }

    public String getTagName() {
        return tagName;
    }

}
