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
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.*;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.jkiss.dbeaver.ext.import_config.wizards.idea.IdeaConfigXMLConstant.*;

public class IdeaDataSourceConfigService {

    public static final IdeaDataSourceConfigService INSTANCE = new IdeaDataSourceConfigService();
    private static final Log log = Log.getLog(IdeaDataSourceConfigService.class);

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
        uuidToDataSourceProps = mergeTwoMapProps(uuidToDataSourceProps, uuidToDataSourceFromDifferentXml);

        Map<String, Map<String, String>> sshIdToSshConfigMap = tryReadIdeaSshConfig(
            RuntimeUtils.getWorkingDirectory("JetBrains"));
        uuidToDataSourceProps = mergeSshConfigToIdeaConfigMap(uuidToDataSourceProps, sshIdToSshConfigMap);
        return uuidToDataSourceProps;
    }

    public ImportConnectionInfo buildIdeaConnectionFromProps(Map<String, String> conProps) {

        ImportDriverInfo driverInfo = buildDriverInfo(conProps);
        String url = conProps.get(JDBC_URL_TAG.getValue());
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

        configureSshConfig(connectionInfo, conProps);
        IdeaImportConfigPostProcessor postProcessor = postProcessors.get(driverInfo.getId());
        if (postProcessor != null) {
            postProcessor.postProcess(connectionInfo, conProps);
        }
        log.debug("load connection: " + connectionInfo);
        return connectionInfo;
    }

    private void configureSshConfig(ImportConnectionInfo connectionInfo, Map<String, String> conProps) {

        NetworkHandlerDescriptor sslHD = NetworkHandlerRegistry.getInstance().getDescriptor("ssh_tunnel");
        DBWHandlerConfiguration sshHandler = new DBWHandlerConfiguration(sslHD, null);
        sshHandler.setUserName(conProps.get(SSH_USERNAME_PATH.getValue()));
        sshHandler.setSavePassword(true);
        sshHandler.setProperty(DBWHandlerConfiguration.PROP_HOST, conProps.get(SSH_HOST_PATH.getValue()));
        sshHandler.setProperty(DBWHandlerConfiguration.PROP_PORT, conProps.get(SSH_PORT_PATH.getValue()));

        if (!CommonUtils.isEmpty(conProps.get(SSH_KEY_FILE_PATH.getValue()))) {
            sshHandler.setProperty("authType", "PUBLIC_KEY");
            sshHandler.setProperty("keyPath", conProps.get(SSH_KEY_FILE_PATH.getValue()));
        }
        sshHandler.setProperty("implementation", "sshj");
        sshHandler.setEnabled(true);
        connectionInfo.addNetworkHandler(sshHandler);
    }

    private Map<String, Map<String, String>> mergeSshConfigToIdeaConfigMap(
        Map<String, Map<String, String>> uuidToDataSourceProps,
        Map<String, Map<String, String>> sshIdToSshConfigMap
    ) {
        for (Map.Entry<String, Map<String, String>> configEntry : uuidToDataSourceProps.entrySet()) {

            Map<String, String> config = configEntry.getValue();
            String sshUuid = config.get(SSH_PROPERTIES_UUID_PATH.getValue());
            Map<String, String> sshConfig = sshIdToSshConfigMap.get(sshUuid);
            if (sshConfig == null) continue;
            config.putAll(sshConfig);
        }
        return uuidToDataSourceProps;
    }


    private Map<String, Map<String, String>> tryReadIdeaSshConfig(String pathToJetBrainsHomeDirectory) {
        try {
            return readIdeaSshConfig(pathToJetBrainsHomeDirectory);
        } catch (Exception e) {
            //can't read ssh config, move on
            log.warn("Could not read Idea ssh config", e);
        }
        return null;
    }

    private Map<String, Map<String, String>> mergeTwoMapProps(
        Map<String, Map<String, String>> uuidToDataSourceProps, Map<String,
        Map<String, String>> uuidToDataSourceFromDifferentXml
    ) {
        for (Map.Entry<String, Map<String, String>> uuidToDataSourceEntry : uuidToDataSourceProps.entrySet()) {
            Map<String, String> dataSourceProps = uuidToDataSourceProps.get(uuidToDataSourceEntry.getKey());
            if (dataSourceProps == null) {
                log.warn("Unexpectedly found data source properties for " + uuidToDataSourceEntry.getKey());
                dataSourceProps = new HashMap<>();
            }
            Map<String, String> mergeValue = uuidToDataSourceFromDifferentXml.get(uuidToDataSourceEntry.getKey());
            if (mergeValue != null) {
                dataSourceProps.putAll(mergeValue);
            }
        }
        return uuidToDataSourceProps;
    }

    private Map<String, Map<String, String>> readIdeaSshConfig(String pathToIdeaFolder) throws Exception {

        String pathToSshFile = "\\options\\sshConfigs.xml";
        try (Stream<Path> paths = Files.list(Paths.get(pathToIdeaFolder))) {
            List<Path> ideaFolders = paths
                .filter(Files::isDirectory)
                .filter(file -> file.getFileName().toString().toLowerCase().contains("idea"))
                .peek(System.out::println)
                .toList();

            Map<String, Map<String, String>> sshConfig = new HashMap<>();
            for (Path ideaFolder : ideaFolders) {
                File sshFile = new File(ideaFolder.toFile().getAbsolutePath() + pathToSshFile);
                if (sshFile.exists()) {
                    sshConfig.putAll(readIdeaConfig(sshFile.getAbsolutePath(), "UTF-8"));
                }
            }
            return sshConfig;
        }
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
            if (DATASOURCE_TAG.getValue().equals(element.getNodeName())) {
                if (uuid != null) {
                    uuidToDatasourceProps.put(uuid, conProps);
                }
                String uuidOfNewDataSource = attrs.getNamedItem(UUID_ATTRIBUTE.getValue()).getNodeValue();
                conProps = uuidToDatasourceProps.getOrDefault(uuidOfNewDataSource, new HashMap<>());
                uuid = uuidOfNewDataSource;
            }
            if (PROPERTIES_TAG.getValue().equals(element.getNodeName())) {
                Node value = attrs.getNamedItem("value");
                String name = attrs.getNamedItem("name").getNodeValue();
                if (name.startsWith(INTELIJ_CUSTOM_VALUE.getValue())) continue;
                conProps.put(name, value == null ? "" : value.getNodeValue());
            }
            //SSH_CONFIG_TAG - tag from sshConfig.xml
            if (SSH_CONFIG_TAG.getValue().equals(element.getNodeName())) {
                uuid = attrs.getNamedItem("id").getNodeValue();
            }
            //SSH_PROPERTIES_TAG - tag from dataSourceLocal.xml
            if (SSH_PROPERTIES_TAG.getValue().equals(element.getNodeName())) {
                Node sshEnabled = allElements.item(++i);
                conProps.put(SSH_PROPERTIES_ENABLE_PATH.getValue(),
                    sshEnabled.getChildNodes().item(0).getNodeValue());
                Node sshUuid = allElements.item(++i);
                conProps.put(SSH_PROPERTIES_UUID_PATH.getValue(),
                    sshUuid.getChildNodes().item(0).getNodeValue());
                continue;
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

        String name = conProps.get(DATABASE_NAME_PATH.getValue());
        String refDriverName = conProps.get(DRIVER_REF_TAG.getValue());

        //todo to think about predefine map from idea name to our driver for exceptional case
        DBPDriver driver = findDriver(name, refDriverName);
        if (driver == null) {
            driver = tryFindDriverByToken(name);
            if (driver == null) {
                driver = tryExtractDriverByUrl(conProps.get(JDBC_URL_TAG.getValue()));
            }
        }
        return new ImportDriverInfo(driver);
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
