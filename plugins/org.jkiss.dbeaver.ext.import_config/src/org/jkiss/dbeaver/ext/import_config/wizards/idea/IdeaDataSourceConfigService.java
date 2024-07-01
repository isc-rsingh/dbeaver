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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportConfigurationException;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportDriverInfo;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.*;

import java.io.Reader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class IdeaDataSourceConfigService {

    public static final IdeaDataSourceConfigService INSTANCE = new IdeaDataSourceConfigService();
    private static final Log log = Log.getLog(IdeaDataSourceConfigService.class);

    private IdeaDataSourceConfigService() {
    }

    public Map<String, String> importXML(Reader reader) throws XMLException {
        Document document = XMLUtils.parseDocument(reader);
        Map<String, String> conProps = new HashMap<>();
        // * - for getting all element
        NodeList allElements = document.getElementsByTagName("*");
        if (allElements.getLength() == 0) {
            throw new ImportConfigurationException("No elements found");
        }

        for (int i = 0; i < allElements.getLength(); i++) {
            Node element = allElements.item(i);
            NamedNodeMap attrs = element.getAttributes();
            for (int j = 0; j < attrs.getLength(); j++) {
                Attr attr = (Attr) attrs.item(i);
                if(attr == null) continue;
                String key = String.format("%s.%s", element.getNodeName(), attr.getName());
                //fixme there could be overriding for two independence item with same name and tag
                conProps.put(key, attr.getValue());
            }
            //fixme only first node
            if (element.hasChildNodes()) {
                conProps.put(element.getNodeName(), element.getChildNodes().item(0).getNodeValue());
            }
        }
        return conProps;
    }

    public ImportConnectionInfo buildIdeaConnectionFromProps(Map<String, String> conProps) {

        //todo to think about JAXB unmarshaller
        ImportDriverInfo driverInfo = buildDriverInfo(conProps);
        String url = conProps.get("jdbc-url");
        URI uri = parseURL(url);
        ImportConnectionInfo connectionInfo = new ImportConnectionInfo(
                driverInfo,
                conProps.get("data-source.uuid"),
                conProps.get("data-source.name"),
                url,
                uri.getHost(),
                String.valueOf(uri.getPort()),
                //fixme database
                "postgres",
                conProps.get("user-name"),
                ""
        );

        log.debug("load connection: " + connectionInfo);
        return connectionInfo;
    }

    private URI parseURL(String url) {
        String cleanURI = url.substring(5);
        return URI.create(cleanURI);
    }

    private ImportDriverInfo buildDriverInfo(Map<String, String> conProps) {
//        ImportDriverInfo driverInfo = new ImportDriverInfo(driver.getId(), driver.getName(), driver.getSampleURL(),
//                driver.getDriverClassName());
        //todo to determinate the driver
        return new ImportDriverInfo("postgres-jdbc",
                "PostgreSQL",
                "jdbc:postgresql://{host}[:{port}]/[{database}]",
                "org.postgresql.Driver");

    }
}
