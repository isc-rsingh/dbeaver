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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportConfigurationException;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportDriverInfo;
import org.jkiss.dbeaver.ext.import_config.wizards.idea.postprocessor.api.IdeaImportConfigPostProcessor;
import org.jkiss.dbeaver.ext.import_config.wizards.idea.postprocessor.impl.BigQueryIdeaImportConfigPostProcessor;
import org.jkiss.dbeaver.ext.import_config.wizards.idea.postprocessor.impl.OracleIdeaImportPostProcessor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdeaDataSourceConfigService {

    public static final IdeaDataSourceConfigService INSTANCE = new IdeaDataSourceConfigService();
    private static final Log log = Log.getLog(IdeaDataSourceConfigService.class);

    private static final String DATASOURCE_TAG = "data-source";
    private static final String PROPERTIES_TAG = "property";

    private final Map<String, IdeaImportConfigPostProcessor> postProcessors = new HashMap<>();

    private IdeaDataSourceConfigService() {

        //fixme there is need registry all implementation of IdeaImportConfigPostProcessor.
        //to think how it can be separated
        BigQueryIdeaImportConfigPostProcessor bigQueryPostProcessor = new BigQueryIdeaImportConfigPostProcessor();
        postProcessors.put(bigQueryPostProcessor.getHandledDriverId(), bigQueryPostProcessor);

        OracleIdeaImportPostProcessor oracleIdeaImportPostProcessor = new OracleIdeaImportPostProcessor();
        postProcessors.put(oracleIdeaImportPostProcessor.getHandledDriverId(), oracleIdeaImportPostProcessor);
    }

    public Map<String, Map<String, String>> buildIdeaConfigProps(String pathToIdeaFolder, String encoding) throws Exception {
        Map<String, Map<String, String>> uuidToDataSourceProps = new HashMap<>();
        //fixme path for other OS
        uuidToDataSourceProps.putAll(readIdeaConfig(pathToIdeaFolder + "/dataSources.xml", encoding));

        Map<String, Map<String, String>> uuidToDataSourceFromDifferentXml = readIdeaConfig(pathToIdeaFolder + "/dataSources.local.xml", encoding);
        //merge two maps of maps
        for (Map.Entry<String, Map<String, String>> uuidToDataSourceEntry : uuidToDataSourceProps.entrySet()) {
            Map<String, String> dataSourceProps = uuidToDataSourceProps.get(uuidToDataSourceEntry.getKey());
            if (dataSourceProps == null) {
                log.warn("Unexpectedly found data source properties for " + uuidToDataSourceEntry.getKey());
                dataSourceProps = new HashMap<>();
            }
            dataSourceProps.putAll(uuidToDataSourceFromDifferentXml.get(uuidToDataSourceEntry.getKey()));
        }
        return uuidToDataSourceProps;
    }

    public ImportConnectionInfo buildIdeaConnectionFromProps(Map<String, String> conProps) {

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
                "",
                conProps.get("user-name"),
                ""
        );

        IdeaImportConfigPostProcessor postProcessor = postProcessors.get(driverInfo.getId());
        if (postProcessor != null) {
            postProcessor.postProcess(connectionInfo, conProps);
        }
        log.debug("load connection: " + connectionInfo);
        return connectionInfo;
    }

    private Map<String, Map<String, String>> readIdeaConfig(String fileName, String encoding) throws Exception {

        try (InputStream dataSourceXmlIs = new FileInputStream(fileName)) {
            try (Reader reader = new InputStreamReader(dataSourceXmlIs, encoding)) {
                return importXML(reader);
            }
        }
    }

    private Map<String, Map<String, String>> importXML(Reader reader) throws XMLException {
        Document document = XMLUtils.parseDocument(reader);
        Map<String, String> conProps = new HashMap<>();
        Map<String, Map<String, String>> uuidToDatasourceProps = new HashMap<>();
        // * - for getting all element
        NodeList allElements = document.getElementsByTagName("*");
        if (allElements.getLength() == 0) {
            throw new ImportConfigurationException("No elements found");
        }

        String uuid = null;
        for (int i = 0; i < allElements.getLength(); i++) {
            Node element = allElements.item(i);
            NamedNodeMap attrs = element.getAttributes();
            if (DATASOURCE_TAG.equals(element.getNodeName())) {
                if (uuid != null) {
                    uuidToDatasourceProps.put(uuid, conProps);
                }
                String uuidOfNewDataSource = attrs.getNamedItem("uuid").getNodeValue();
                conProps = uuidToDatasourceProps.getOrDefault(uuidOfNewDataSource, new HashMap<>());
                uuid = uuidOfNewDataSource;
            }
            if (PROPERTIES_TAG.equals(element.getNodeName())) {
                Node value = attrs.getNamedItem("value");
                String name = attrs.getNamedItem("name").getNodeValue();
                if (name.startsWith("com.intellij")) continue;
                conProps.put(name, value == null ? "" : value.getNodeValue());
            }

            for (int j = 0; j < attrs.getLength(); j++) {
                Attr attr = (Attr) attrs.item(j);
                if (attr == null) continue;
                String key = String.format("%s.%s", element.getNodeName(), attr.getName());
                conProps.put(key, attr.getValue());
            }
            if (isNodeHasTextValue(element)) {
                conProps.put(element.getNodeName(), element.getChildNodes().item(0).getNodeValue());
            }
        }
        uuidToDatasourceProps.put(uuid, conProps);
        return uuidToDatasourceProps;
    }

    private URI parseURL(String url) {
        String cleanURI = url.substring(5);
        return URI.create(cleanURI);
    }

    private static boolean isNodeHasTextValue(Node element) {
        return element.hasChildNodes() && element.getChildNodes().getLength() > 0 &&
                !element.getChildNodes().item(0).getNodeValue().isBlank();
    }

    private ImportDriverInfo buildDriverInfo(Map<String, String> conProps) {

        String name = conProps.get("database-info.dbms");
        String refDriverName = conProps.get("driver-ref");

        //todo to think about predefine map from idea name to our driver for exceptional case
        DBPDriver driver = findDriver(name, refDriverName);
        if (driver == null) {

            //try 2
            driver = tryFindDriverByToken(name);
            if (driver != null) return new ImportDriverInfo(driver);

            //try 3
            driver = tryExtractDriverByUrl(conProps.get("jdbc-url"));
            if (driver != null) return new ImportDriverInfo(driver);

            //fixme mock
            return new ImportDriverInfo("postgres-jdbc",
                    "PostgreSQL",
                    "jdbc:postgresql://{host}[:{port}]/[{database}]",
                    "org.postgresql.Driver");
        } else {
            return new ImportDriverInfo(driver.getId(), driver.getName(), driver.getSampleURL(),
                    driver.getDriverClassName());
        }
    }

    private DBPDriver tryExtractDriverByUrl(String url) {

        URI uri = parseURL(url);
        String scheme = uri.getScheme();
        return findDriver(scheme, null);
    }

    private @Nullable DBPDriver tryFindDriverByToken(String name) {
        DBPDriver driver;
        List<String> nameTokens = Arrays.stream(name.split("_")).toList();
        if (nameTokens.size() > 1) {
            for (String nameToken : nameTokens) {
                driver = findDriver(nameToken, null);
                if (driver != null) {
                    return driver;
                }
            }
        }
        return null;
    }

    private DBPDriver findDriver(String name, String refDriverName) {
        DataSourceProviderRegistry dataSourceProviderRegistry = DataSourceProviderRegistry.getInstance();
        List<DataSourceProviderDescriptor> dataSourceProviders = dataSourceProviderRegistry.getDataSourceProviders();
        for (DataSourceProviderDescriptor dataSourceProvider : dataSourceProviders) {
            List<DriverDescriptor> drivers = dataSourceProvider.getDrivers();
            for (DriverDescriptor driver : drivers) {
                if (driver.getName().equalsIgnoreCase(name) || driver.getId().equalsIgnoreCase(name)
                        || driver.getName().equalsIgnoreCase(refDriverName)
                        || driver.getId().equalsIgnoreCase(refDriverName)) {
                    while (driver.getReplacedBy() != null) {
                        driver = driver.getReplacedBy();
                    }
                    return driver;
                }
            }
            if (dataSourceProvider.getId().equalsIgnoreCase(name)
                    || dataSourceProvider.getName().equalsIgnoreCase(name)
                    || dataSourceProvider.getId().equalsIgnoreCase(refDriverName)
                    || dataSourceProvider.getName().equalsIgnoreCase(refDriverName)) {
                if (!drivers.isEmpty()) {
                    DriverDescriptor driverDescriptor = drivers.get(0);
                    while (driverDescriptor.getReplacedBy() != null) {
                        driverDescriptor = driverDescriptor.getReplacedBy();
                    }
                    return driverDescriptor;
                }
            }
        }
        return null;
    }
}
