// ============================================================================
//
// Copyright (C) 2006-2009 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core.model.process.statsandlogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.talend.core.database.conn.DatabaseConnStrUtil;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.language.LanguageManager;
import org.talend.core.model.components.IComponent;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.param.ERepositoryCategoryType;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.EConnectionType;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.utils.ContextParameterUtils;
import org.talend.core.model.utils.TalendTextUtils;
import org.talend.designer.core.DesignerPlugin;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.designer.core.model.components.EmfComponent;
import org.talend.designer.core.model.process.DataConnection;
import org.talend.designer.core.model.process.DataNode;
import org.talend.designer.core.ui.editor.process.Process;
import org.talend.designer.core.ui.preferences.StatsAndLogsConstants;
import org.talend.repository.model.ComponentsFactoryProvider;

/**
 * DOC nrousseau class global comment. Detailled comment <br/>
 * 
 */
public class StatsAndLogsManager {

    public static final String TPREJOB = "tPrejob"; //$NON-NLS-1$

    public static final String PRE_STA_LOG_CON = "preStaLogCon"; //$NON-NLS-1$

    private static final String LOG_UNIQUE_NAME = "talendLogs"; //$NON-NLS-1$

    private static final String STAT_UNIQUE_NAME = "talendStats"; //$NON-NLS-1$

    private static final String METER_UNIQUE_NAME = "talendMeter"; //$NON-NLS-1$

    public static boolean isStatsAndLogsActivated(IProcess process) {
        String dbOutput = null;
        boolean dbFlag = ((Boolean) process.getElementParameter(EParameterName.ON_DATABASE_FLAG.getName()).getValue())
                && process.getElementParameter(EParameterName.ON_DATABASE_FLAG.getName()).isShow(process.getElementParameters());
        if (!dbFlag) {
            dbOutput = null;
        } else {
            dbOutput = (String) process.getElementParameter(EParameterName.DB_TYPE.getName()).getValue(); //$NON-NLS-1$
            if (dbOutput == null || dbOutput.equals("")) { //$NON-NLS-1$
                dbOutput = null;
                dbFlag = false;
            }
        }
        boolean file = ((Boolean) process.getElementParameter(EParameterName.ON_FILES_FLAG.getName()).getValue())
                && process.getElementParameter(EParameterName.ON_FILES_FLAG.getName()).isShow(process.getElementParameters());

        boolean console = ((Boolean) process.getElementParameter(EParameterName.ON_CONSOLE_FLAG.getName()).getValue())
                && process.getElementParameter(EParameterName.ON_CONSOLE_FLAG.getName()).isShow(process.getElementParameters());

        if (!file && !dbFlag && !console) {
            return false;
        }
        return true;
    }

    public static List<DataNode> getStatsAndLogsNodes(Process process) {
        List<DataNode> nodeList = new ArrayList<DataNode>();

        String dbOutput = null;

        boolean dbFlag = ((Boolean) process.getElementParameter(EParameterName.ON_DATABASE_FLAG.getName()).getValue())
                && process.getElementParameter(EParameterName.ON_DATABASE_FLAG.getName()).isShow(process.getElementParameters());
        if (!dbFlag) {
            dbOutput = null;
        } else {
            dbOutput = (String) process.getElementParameter(EParameterName.DB_TYPE.getName()).getValue(); //$NON-NLS-1$
            dbOutput = OracleComponentHelper.filterOracleComponentName(dbOutput);
            if (dbOutput == null || dbOutput.equals("")) { //$NON-NLS-1$
                dbOutput = null;
                dbFlag = false;
            }
        }
        boolean file = ((Boolean) process.getElementParameter(EParameterName.ON_FILES_FLAG.getName()).getValue())
                && process.getElementParameter(EParameterName.ON_FILES_FLAG.getName()).isShow(process.getElementParameters());

        boolean console = ((Boolean) process.getElementParameter(EParameterName.ON_CONSOLE_FLAG.getName()).getValue())
                && process.getElementParameter(EParameterName.ON_CONSOLE_FLAG.getName()).isShow(process.getElementParameters());

        if (!file && !dbFlag && !console) {
            return nodeList;
        }

        boolean useStats = ((Boolean) process.getElementParameter(EParameterName.ON_STATCATCHER_FLAG.getName()).getValue())
                && process.getElementParameter(EParameterName.ON_STATCATCHER_FLAG.getName()).isShow(
                        process.getElementParameters());

        boolean useLogs = ((Boolean) process.getElementParameter(EParameterName.ON_LOGCATCHER_FLAG.getName()).getValue())
                && process.getElementParameter(EParameterName.ON_LOGCATCHER_FLAG.getName())
                        .isShow(process.getElementParameters());

        boolean useMetter = ((Boolean) process.getElementParameter(EParameterName.ON_METERCATCHER_FLAG.getName()).getValue()) //$NON-NLS-1$
                && process.getElementParameter(EParameterName.ON_METERCATCHER_FLAG.getName()).isShow(
                        process.getElementParameters());

        String basePath = (String) process.getElementParameter(EParameterName.FILE_PATH.getName()).getValue();
        if (LanguageManager.getCurrentLanguage().equals(ECodeLanguage.PERL)) {
            basePath = basePath.replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            basePath = basePath.replace("\\", "/") + "+ \"/\" +"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        DataNode connectionNode = null;
        String connectionUID = "connectionStatsLogs";//$NON-NLS-1$

        DataNode commitNode = null;
        String connectionUID2;
        IComponent commitComponent = null;

        String subString = null;
        /*
         * maybe, need create every of committing node for log/stat/metter.
         */
        String[] javaDbComponents = StatsAndLogsConstants.DB_OUTPUT_COMPONENTS;
        for (String dbComponent : javaDbComponents) {
            String commitComponentName = null;
            if (OracleComponentHelper.filterOracleConnectionType(
                    (String) process.getElementParameter(EParameterName.DB_TYPE.getName()).getValue()).equals(dbComponent)) {
                if (dbComponent.endsWith("Output")) {//$NON-NLS-1$
                    subString = dbComponent.substring(0, dbComponent.lastIndexOf("Output"));//$NON-NLS-1$
                    commitComponentName = subString + "Commit";//$NON-NLS-1$
                } else {
                    commitComponentName = "tOracleCommit";//$NON-NLS-1$
                }
                commitComponent = ComponentsFactoryProvider.getInstance().get(commitComponentName);
                if (commitComponent != null) {
                    connectionUID2 = connectionUID + "_Commit";//$NON-NLS-1$ 
                    commitNode = new DataNode(commitComponent, connectionUID2);
                    commitNode.setSubProcessStart(true);
                    commitNode.setActivate(true);
                    commitNode.getElementParameter(EParameterName.CONNECTION.getName()).setValue(connectionUID);
                    IElementParameter elementParameter = commitNode.getElementParameter("CLOSE");
                    if (elementParameter != null) {
                        elementParameter.setValue(Boolean.FALSE); //$NON-NLS-1$
                    }

                    commitNode.setProcess(process);
                    nodeList.add(commitNode);
                }
            }
        }

        if (useLogs) {
            DataNode logsNode = createLogsNode(file, console, dbOutput);
            if (LanguageManager.getCurrentLanguage().equals(ECodeLanguage.PERL)) {
                logsNode.getElementParameter("FILENAME").setValue(//$NON-NLS-1$
                        "File::Spec->catfile(" + basePath + "," //$NON-NLS-1$ //$NON-NLS-2$
                                + process.getElementParameter(EParameterName.FILENAME_LOGS.getName()).getValue() + ")"); //$NON-NLS-1$
            } else {
                logsNode.getElementParameter("FILENAME").setValue(//$NON-NLS-1$
                        basePath + process.getElementParameter(EParameterName.FILENAME_LOGS.getName()).getValue()); //$NON-NLS-1$
            }
            if (commitNode != null) {
                connectionNode = addConnection(connectionNode, process, connectionUID, logsNode, nodeList, commitNode);
            } else {
                useNoConnectionComponentDB(logsNode, process, connectionUID);
            }
            logsNode.getElementParameter("TABLE").setValue(//$NON-NLS-1$
                    process.getElementParameter(EParameterName.TABLE_LOGS.getName()).getValue());
            logsNode.getElementParameter(EParameterName.CATCH_RUNTIME_ERRORS.getName()).setValue(
                    process.getElementParameter(EParameterName.CATCH_RUNTIME_ERRORS.getName()).getValue());
            logsNode.getElementParameter(EParameterName.CATCH_USER_ERRORS.getName()).setValue(
                    process.getElementParameter(EParameterName.CATCH_USER_ERRORS.getName()).getValue());
            logsNode.getElementParameter(EParameterName.CATCH_USER_WARNING.getName()).setValue(
                    process.getElementParameter(EParameterName.CATCH_USER_WARNING.getName()).getValue());

            logsNode.setProcess(process);
            nodeList.add(logsNode);
        }

        if (useStats) {
            DataNode statsNode = createStatsNode(file, console, dbOutput);
            if (LanguageManager.getCurrentLanguage().equals(ECodeLanguage.PERL)) {
                statsNode.getElementParameter("FILENAME").setValue(//$NON-NLS-1$
                        "File::Spec->catfile(" + basePath + "," //$NON-NLS-1$ //$NON-NLS-2$
                                + process.getElementParameter(EParameterName.FILENAME_STATS.getName()).getValue() + ")"); //$NON-NLS-1$
            } else {
                statsNode.getElementParameter("FILENAME").setValue(//$NON-NLS-1$
                        basePath + process.getElementParameter(EParameterName.FILENAME_STATS.getName()).getValue()); //$NON-NLS-1$
            }
            if (commitNode != null) {
                connectionNode = addConnection(connectionNode, process, connectionUID, statsNode, nodeList, commitNode);
            } else {
                useNoConnectionComponentDB(statsNode, process, connectionUID);
            }
            statsNode.getElementParameter("TABLE").setValue(//$NON-NLS-1$
                    process.getElementParameter(EParameterName.TABLE_STATS.getName()).getValue());

            statsNode.setProcess(process);
            nodeList.add(statsNode);
        }

        if (useMetter) {
            DataNode meterNode = createMetterNode(file, console, dbOutput);
            if (LanguageManager.getCurrentLanguage().equals(ECodeLanguage.PERL)) {
                meterNode.getElementParameter("FILENAME").setValue(//$NON-NLS-1$
                        "File::Spec->catfile(" + basePath + "," //$NON-NLS-1$ //$NON-NLS-2$
                                + process.getElementParameter(EParameterName.FILENAME_METTER.getName()).getValue() + ")"); //$NON-NLS-1$
            } else {
                meterNode.getElementParameter("FILENAME").setValue(//$NON-NLS-1$
                        basePath + process.getElementParameter(EParameterName.FILENAME_METTER.getName()).getValue());
            }
            if (commitNode != null) {
                connectionNode = addConnection(connectionNode, process, connectionUID, meterNode, nodeList, commitNode);
            } else {
                useNoConnectionComponentDB(meterNode, process, connectionUID);
            }
            meterNode.getElementParameter("TABLE").setValue(//$NON-NLS-1$
                    process.getElementParameter(EParameterName.TABLE_METER.getName()).getValue());

            meterNode.setProcess(process);
            nodeList.add(meterNode);
        }

        return nodeList;
    }

    private static void useNoConnectionComponentDB(DataNode dataNode, Process process, String connectionUID) {
        dataNode.getElementParameter(EParameterName.HOST.getName()).setValue(
                process.getElementParameter(EParameterName.HOST.getName()).getValue());
        dataNode.getElementParameter(EParameterName.PORT.getName()).setValue(
                process.getElementParameter(EParameterName.PORT.getName()).getValue());
        dataNode.getElementParameter(EParameterName.SCHEMA_DB.getName()).setValue(
                process.getElementParameter(EParameterName.SCHEMA_DB.getName()).getValue());
        dataNode.getElementParameter(EParameterName.DBNAME.getName()).setValue(
                process.getElementParameter(EParameterName.DBNAME.getName()).getValue());
        dataNode.getElementParameter(EParameterName.DB_VERSION.getName()).setValue(
                process.getElementParameter(EParameterName.DB_VERSION.getName()).getValue());
        dataNode.getElementParameter(EParameterName.PROPERTIES.getName()).setValue(
                process.getElementParameter(EParameterName.PROPERTIES.getName()).getValue());
        dataNode.getElementParameter(EParameterName.USER.getName()).setValue(
                process.getElementParameter(EParameterName.USER.getName()).getValue());
        dataNode.getElementParameter(EParameterName.PASS.getName()).setValue(
                process.getElementParameter(EParameterName.PASS.getName()).getValue());

        dataNode.getElementParameter(EParameterName.CONNECTION_TYPE.getName()).setValue(
                OracleComponentHelper.filterOracleConnectionType((String) process.getElementParameter(
                        EParameterName.DB_TYPE.getName()).getValue()));
        IElementParameter param = dataNode.getElementParameter(EParameterName.USE_EXISTING_CONNECTION.getName());
        if (param != null) {
            param.setValue(Boolean.FALSE);
        }
        dataNode.getMetadataFromConnector(connectionUID);
        if (process.getElementParameter(EParameterName.DB_TYPE.getName()).getValue().toString().indexOf("Access") != -1) {//$NON-NLS-1$
            dataNode.getElementParameter(EParameterName.DBNAME.getName()).setValue(
                    process.getElementParameter(EParameterName.DBFILE.getName()).getValue());
        }

    }

    private static DataNode addConnection(DataNode connectionNode, Process process, String connectionUID, DataNode dataNode,
            List<DataNode> nodeList, DataNode commitNode) {
        IElementParameter param = dataNode.getElementParameter(EParameterName.USE_EXISTING_CONNECTION.getName());
        if (param != null) {
            param.setValue(Boolean.TRUE);
        }
        param = dataNode.getElementParameter(EParameterName.CONNECTION.getName());
        if (param != null) {
            param.setValue(connectionUID);
        }
        if (connectionNode == null) {
            IComponent component = null;
            String[] javaDbComponents = StatsAndLogsConstants.DB_OUTPUT_COMPONENTS;
            for (String dbComponent : javaDbComponents) {
                String connectionComponentName = null;
                if (OracleComponentHelper.filterOracleConnectionType(
                        (String) process.getElementParameter(EParameterName.DB_TYPE.getName()).getValue()).equals(dbComponent)) {//$NON-NLS-1$
                    if (dbComponent.endsWith("Output")) { //$NON-NLS-1$
                        String substring = dbComponent.substring(0, dbComponent.lastIndexOf("Output")); //$NON-NLS-1$
                        connectionComponentName = substring + "Connection"; //$NON-NLS-1$
                    } else {
                        connectionComponentName = "tOracleConnection"; //$NON-NLS-1$
                    }
                    component = ComponentsFactoryProvider.getInstance().get(connectionComponentName);

                    if (component != null) {
                        connectionNode = new DataNode(component, connectionUID);
                        connectionNode.setSubProcessStart(true);
                        connectionNode.setActivate(true);
                        // check if shared parameter exist, if yes, use it ONLY when use the project settings.
                        // name for shared connection can be always the same, as we use only when project settings is
                        // activated.
                        IElementParameter elementParameter = connectionNode
                                .getElementParameter(EParameterName.USE_SHARED_CONNECTION.getName());

                        if (elementParameter != null && elementParameter.getName() != null) {//$NON-NLS-1$
                            elementParameter.setValue(Boolean.TRUE);//$NON-NLS-1$
                            final String sharedConnName = "StatsAndLog_Shared_Connection"; //$NON-NLS-1$
                            if ((Boolean) process.getElementParameter(EParameterName.STATANDLOG_USE_PROJECT_SETTINGS.getName())
                                    .getValue()) {
                                connectionNode.getElementParameter(EParameterName.SHARED_CONNECTION_NAME.getName()).setValue(
                                        TalendTextUtils.addQuotes(sharedConnName));
                            } else {
                                String url = getUrl(process);
                                connectionNode.getElementParameter(EParameterName.SHARED_CONNECTION_NAME.getName()).setValue(
                                        url + "+" + TalendTextUtils.addQuotes("_" + sharedConnName)); //$NON-NLS-1$ //$NON-NLS-2$
                            }
                        }
                        setConnectionParameter(connectionNode, process, connectionUID, dataNode, nodeList);
                        if (connectionComponentName.contains("Oracle")) {//$NON-NLS-1$
                            if (connectionNode.getElementParameter(EParameterName.CONNECTION_TYPE.getName()) != null) {
                                connectionNode.getElementParameter(EParameterName.CONNECTION_TYPE.getName())
                                        .setValue(dbComponent);
                            }
                        }
                        connectionNode.setProcess(process);
                        nodeList.add(connectionNode);

                        IComponent prejobComponent = ComponentsFactoryProvider.getInstance().get(TPREJOB);
                        DataNode preNode = new DataNode(prejobComponent, PRE_STA_LOG_CON);
                        preNode.setStart(true);
                        preNode.setSubProcessStart(true);
                        preNode.setActivate(true);
                        preNode.setProcess(process);
                        nodeList.add(preNode);
                        DataConnection dataConnec = createDataConnectionForComponentOK(preNode, connectionNode);
                        ((List<IConnection>) preNode.getOutgoingConnections()).add(dataConnec);
                        ((List<IConnection>) connectionNode.getIncomingConnections()).add(dataConnec);

                    }
                }
            }
        }
        DataConnection dataConnec = createDataConnectionForSubJobOK(dataNode, commitNode);
        ((List<IConnection>) dataNode.getOutgoingConnections()).add(dataConnec);
        ((List<IConnection>) commitNode.getIncomingConnections()).add(dataConnec);
        return connectionNode;
    }

    /**
     * DOC zli Comment method "getUrl".
     * 
     * @param process
     */
    private static String getUrl(Process process) {
        String processDBType = (String) process.getElementParameter(EParameterName.DB_TYPE.getName()).getValue();
        int indexOfItemFromList = process.getElementParameter(EParameterName.DB_TYPE.getName()).getIndexOfItemFromList(
                processDBType);
        String[] listItemsDisplayName = process.getElementParameter(EParameterName.DB_TYPE.getName()).getListItemsDisplayName();
        processDBType = listItemsDisplayName[indexOfItemFromList];

        IElementParameter param = process.getElementParameter(EParameterName.FILENAME.getName());
        final String empty = ""; //$NON-NLS-1$
        String processDBFileName = empty;
        if (param != null) {
            processDBFileName = (String) param.getValue();
        }
        param = process.getElementParameter(EParameterName.DATASOURCE.getName());
        String processDBDatasouce = empty;
        if (param != null) {
            processDBDatasouce = (String) param.getValue();
        }

        param = process.getElementParameter(EParameterName.DBNAME.getName());
        String processDBName = empty;
        if (param != null) {
            processDBName = (String) param.getValue();
        }
        param = process.getElementParameter(EParameterName.HOST.getName());
        String processHost = empty;
        if (param != null) {
            processHost = (String) param.getValue();
        }
        param = process.getElementParameter(EParameterName.PORT.getName());
        String processDBPort = empty;
        if (param != null) {
            processDBPort = (String) param.getValue();
        }
        param = process.getElementParameter(EParameterName.PASS.getName());
        String processDBPass = empty;
        if (param != null) {
            processDBPass = (String) param.getValue();
        }
        param = process.getElementParameter(EParameterName.SCHEMA_DB.getName());
        String processDBSID = empty;
        if (param != null) {
            processDBSID = (String) param.getValue();
        }
        param = process.getElementParameter(EParameterName.USER.getName());
        String processDBUser = empty;
        if (param != null) {
            processDBUser = (String) param.getValue();
        }
        param = process.getElementParameter(EParameterName.PROPERTIES.getName());
        String processDBAdditionParameters = empty;
        if (param != null) {
            processDBAdditionParameters = (String) param.getValue();
        }
        param = process.getElementParameter(EParameterName.DB_VERSION.getName());
        String processDBVersion = empty;
        if (param != null) {
            processDBVersion = (String) param.getValue();
        }
        String dbURL = DatabaseConnStrUtil.getURLString(true, processDBType, processDBVersion, processHost, processDBUser,
                processDBPass, processDBPort, processDBSID, processDBFileName, processDBDatasouce, processDBFileName,
                processDBAdditionParameters);
        return dbURL;
    }

    private static DataConnection createDataConnectionForComponentOK(DataNode dataNode, DataNode commitNode) {
        DataConnection dataConnec = new DataConnection();
        dataConnec.setActivate(true);
        dataConnec.setLineStyle(EConnectionType.ON_COMPONENT_OK);
        dataConnec.setTraceConnection(false);
        dataConnec.setName("after_" + dataNode.getUniqueName() + "_" + commitNode.getUniqueName()); //$NON-NLS-1$ //$NON-NLS-2$
        // dataConnec.setName(refSource.getUniqueName() + "_to_hash_" + connection.getName());
        dataConnec.setSource(dataNode);
        dataConnec.setTarget(commitNode);
        dataConnec.setConnectorName(EConnectionType.ON_COMPONENT_OK.getName());
        return dataConnec;
    }

    private static DataConnection createDataConnectionForSubJobOK(DataNode dataNode, DataNode commitNode) {
        DataConnection dataConnec = new DataConnection();
        dataConnec.setActivate(true);
        dataConnec.setLineStyle(EConnectionType.ON_SUBJOB_OK);
        dataConnec.setTraceConnection(false);
        dataConnec.setName("sub_ok_" + dataNode.getUniqueName() + "_" + commitNode.getUniqueName()); //$NON-NLS-1$ //$NON-NLS-2$
        // dataConnec.setName(refSource.getUniqueName() + "_to_hash_" + connection.getName());
        dataConnec.setSource(dataNode);
        dataConnec.setTarget(commitNode);
        dataConnec.setConnectorName(EConnectionType.ON_SUBJOB_OK.getName());
        return dataConnec;
    }

    private static void setConnectionParameter(DataNode connectionNode, Process process, String connectionUID, DataNode dataNode,
            List<DataNode> nodeList) {
        if (connectionNode.getElementParameter(EParameterName.HOST.getName()) != null) {
            connectionNode.getElementParameter(EParameterName.HOST.getName()).setValue(
                    process.getElementParameter(EParameterName.HOST.getName()).getValue());
        }
        if (connectionNode.getElementParameter(EParameterName.PORT.getName()) != null) {
            connectionNode.getElementParameter(EParameterName.PORT.getName()).setValue(
                    process.getElementParameter(EParameterName.PORT.getName()).getValue());
        }
        if (connectionNode.getElementParameter(EParameterName.SCHEMA_DB.getName()) != null) {
            connectionNode.getElementParameter(EParameterName.SCHEMA_DB.getName()).setValue(
                    process.getElementParameter(EParameterName.SCHEMA_DB.getName()).getValue());
        }
        if (connectionNode.getElementParameter(EParameterName.DBNAME.getName()) != null) {
            connectionNode.getElementParameter(EParameterName.DBNAME.getName()).setValue(
                    process.getElementParameter(EParameterName.DBNAME.getName()).getValue());
        }
        // 7253
        IElementParameter elementParameter = connectionNode.getElementParameter("PROPERTY");
        if (elementParameter != null) {
            String repositoryValue = elementParameter.getRepositoryValue();
            if ("DATABASE:SQLITE".equals(repositoryValue)) {
                if (connectionNode.getElementParameter(EParameterName.DBNAME.getName()) != null) {
                    connectionNode.getElementParameter(EParameterName.DBNAME.getName()).setValue(
                            process.getElementParameter(EParameterName.DBFILE.getName()).getValue());
                }
            }
        }

        if (connectionNode.getElementParameter(EParameterName.DB_VERSION.getName()) != null) {
            connectionNode.getElementParameter(EParameterName.DB_VERSION.getName()).setValue(
                    process.getElementParameter(EParameterName.DB_VERSION.getName()).getValue());
        }
        if (connectionNode.getElementParameter(EParameterName.PROPERTIES.getName()) != null) {
            connectionNode.getElementParameter(EParameterName.PROPERTIES.getName()).setValue(
                    process.getElementParameter(EParameterName.PROPERTIES.getName()).getValue());
        }
        if (connectionNode.getElementParameter(EParameterName.USER.getName()) != null) {
            connectionNode.getElementParameter(EParameterName.USER.getName()).setValue(
                    process.getElementParameter(EParameterName.USER.getName()).getValue());
        }

        if (connectionNode.getElementParameter(EParameterName.PASS.getName()) != null) {
            connectionNode.getElementParameter(EParameterName.PASS.getName()).setValue(
                    process.getElementParameter(EParameterName.PASS.getName()).getValue());
        }

    }

    private static DataNode createLogsNode(boolean useFile, boolean console, String dbOutput) {
        JobLogsComponent logsComponent = new JobLogsComponent(useFile, console, dbOutput);
        DataNode logsNode = new DataNode(logsComponent, LOG_UNIQUE_NAME);
        logsNode.setStart(true);
        logsNode.setSubProcessStart(true);
        logsNode.setActivate(true);
        logsNode.getMetadataList().clear();

        // load the tLogCatcher to get the schema.
        IComponent tmpComponent = ComponentsFactoryProvider.getInstance().get("tLogCatcher"); //$NON-NLS-1$
        DataNode tmpNode = new DataNode(tmpComponent, "tmp"); //$NON-NLS-1$
        boolean found = false;
        for (int k = 0; k < tmpNode.getElementParameters().size() && !found; k++) {
            IElementParameter currentParam = tmpNode.getElementParameters().get(k);
            if (currentParam.getField().equals(EParameterFieldType.SCHEMA_TYPE)) {
                Object value = currentParam.getValue();
                if (value instanceof IMetadataTable) {
                    IMetadataTable table = ((IMetadataTable) value).clone();
                    table.setTableName(LOG_UNIQUE_NAME);
                    table.setAttachedConnector(currentParam.getContext());
                    logsNode.getMetadataList().add(table);
                }
                found = true;
            }
        }
        return logsNode;
    }

    private static DataNode createStatsNode(boolean useFile, boolean console, String dbOutput) {
        JobStatsComponent statsComponent = new JobStatsComponent(useFile, console, dbOutput);
        DataNode statsNode = new DataNode(statsComponent, STAT_UNIQUE_NAME);
        statsNode.setStart(true);
        statsNode.setSubProcessStart(true);
        statsNode.setActivate(true);

        statsNode.getMetadataList().clear();

        // load the tStatCatcher to get the schema.
        IComponent tmpComponent = ComponentsFactoryProvider.getInstance().get("tStatCatcher"); //$NON-NLS-1$
        DataNode tmpNode = new DataNode(tmpComponent, "tmp"); //$NON-NLS-1$
        boolean found = false;
        for (int k = 0; k < tmpNode.getElementParameters().size() && !found; k++) {
            IElementParameter currentParam = tmpNode.getElementParameters().get(k);
            if (currentParam.getField().equals(EParameterFieldType.SCHEMA_TYPE)) {
                Object value = currentParam.getValue();
                if (value instanceof IMetadataTable) {
                    IMetadataTable table = ((IMetadataTable) value).clone();
                    table.setTableName(STAT_UNIQUE_NAME);
                    table.setAttachedConnector(currentParam.getContext());
                    statsNode.getMetadataList().add(table);
                }
                found = true;
            }
        }
        return statsNode;
    }

    private static DataNode createMetterNode(boolean useFile, boolean console, String dbOutput) {
        JobMetterComponent statsComponent = new JobMetterComponent(useFile, console, dbOutput);
        DataNode statsNode = new DataNode(statsComponent, METER_UNIQUE_NAME);
        statsNode.setStart(true);
        statsNode.setSubProcessStart(true);
        statsNode.setActivate(true);

        statsNode.getMetadataList().clear();

        // load the tFlowMeterCatcher to get the schema.
        IComponent tmpComponent = ComponentsFactoryProvider.getInstance().get("tFlowMeterCatcher"); //$NON-NLS-1$
        DataNode tmpNode = new DataNode(tmpComponent, "tmp"); //$NON-NLS-1$
        boolean found = false;
        for (int k = 0; k < tmpNode.getElementParameters().size() && !found; k++) {
            IElementParameter currentParam = tmpNode.getElementParameters().get(k);
            if (currentParam.getField().equals(EParameterFieldType.SCHEMA_TYPE)) {
                Object value = currentParam.getValue();
                if (value instanceof IMetadataTable) {
                    IMetadataTable table = ((IMetadataTable) value).clone();
                    table.setTableName(METER_UNIQUE_NAME);
                    table.setAttachedConnector(currentParam.getContext());
                    statsNode.getMetadataList().add(table);
                }
                found = true;
            }
        }
        return statsNode;
    }

    public static void createStatsAndLogsParameters(IProcess process) {
        statsAndLogsParametersTitlePart(process);
        statsAndLogsParametersFilePart(process);
        statsAndLogsParametersDBPart(process);
        statsAndLogsParametersFinalPart(process);

    }

    private static void statsAndLogsParametersTitlePart(IProcess process) {
        ElementParameter param;
        IPreferenceStore preferenceStore = DesignerPlugin.getDefault().getPreferenceStore();

        List<IElementParameter> paramList = (List<IElementParameter>) process.getElementParameters();

        String languagePrefix = LanguageManager.getCurrentLanguage().toString() + "_"; //$NON-NLS-1$

        param = new ElementParameter(process);
        param.setName(EParameterName.UPDATE_COMPONENTS.getName());
        param.setValue(Boolean.FALSE);
        param.setDisplayName(EParameterName.UPDATE_COMPONENTS.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(1);
        param.setReadOnly(true);
        param.setRequired(false);
        param.setShow(false);
        paramList.add(param);

        param = new ElementParameter(process);
        param.setName(EParameterName.ON_STATCATCHER_FLAG.getName());
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.ON_STATCATCHER_FLAG.getName()));
        param.setDisplayName(EParameterName.ON_STATCATCHER_FLAG.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(1);
        paramList.add(param);

        param = new ElementParameter(process);
        param.setName(EParameterName.ON_LOGCATCHER_FLAG.getName());
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.ON_LOGCATCHER_FLAG.getName()));
        param.setDisplayName(EParameterName.ON_LOGCATCHER_FLAG.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(1);
        paramList.add(param);

        param = new ElementParameter(process);
        param.setName(EParameterName.ON_METERCATCHER_FLAG.getName());
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.ON_METERCATCHER_FLAG.getName()));
        param.setDisplayName(EParameterName.ON_METERCATCHER_FLAG.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(1);
        paramList.add(param);

        // on console
        param = new ElementParameter(process);
        param.setName(EParameterName.ON_CONSOLE_FLAG.getName());
        param.setValue(Boolean.FALSE);
        param.setDisplayName(EParameterName.ON_CONSOLE_FLAG.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(2);
        param
                .setShowIf("((ON_CONSOLE_FLAG == 'true' or ON_CONSOLE_FLAG == 'false') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true'))"); //$NON-NLS-1$
        paramList.add(param);
    }

    private static void statsAndLogsParametersFilePart(IProcess process) {
        ElementParameter param;
        IPreferenceStore preferenceStore = DesignerPlugin.getDefault().getPreferenceStore();

        List<IElementParameter> paramList = (List<IElementParameter>) process.getElementParameters();

        String languagePrefix = LanguageManager.getCurrentLanguage().toString() + "_"; //$NON-NLS-1$
        // on files
        param = new ElementParameter(process);
        param.setName(EParameterName.ON_FILES_FLAG.getName());
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.ON_FILES_FLAG.getName()));
        param.setDisplayName(EParameterName.ON_FILES_FLAG.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(10);
        param.setShowIf("(ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')"); //$NON-NLS-1$
        paramList.add(param);

        // file path
        param = new ElementParameter(process);
        param.setName(EParameterName.FILE_PATH.getName());
        param.setValue(addQuotes(replaceSlash(preferenceStore.getString(languagePrefix + EParameterName.FILE_PATH.getName()))));
        param.setDisplayName(EParameterName.FILE_PATH.getDisplayName());
        param.setField(EParameterFieldType.DIRECTORY);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param
                .setShowIf("(ON_FILES_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')"); //$NON-NLS-1$
        param.setNumRow(11);
        paramList.add(param);

        // stats file name
        param = new ElementParameter(process);
        param.setName(EParameterName.FILENAME_STATS.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.FILENAME_STATS.getName())));
        param.setDisplayName(EParameterName.FILENAME_STATS.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setShowIf("(ON_FILES_FLAG == 'true' and ON_STATCATCHER_FLAG == 'true')"); //$NON-NLS-1$
        param.setRequired(true);
        param.setNumRow(12);

        paramList.add(param);

        param = new ElementParameter(process);
        param.setName(EParameterName.FILENAME_LOGS.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.FILENAME_LOGS.getName())));
        param.setDisplayName(EParameterName.FILENAME_LOGS.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setShowIf("(ON_FILES_FLAG == 'true' and ON_LOGCATCHER_FLAG == 'true')"); //$NON-NLS-1$
        param.setNumRow(13);
        param.setRequired(true);
        paramList.add(param);

        param = new ElementParameter(process);
        param.setName(EParameterName.FILENAME_METTER.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.FILENAME_METTER.getName())));
        param.setDisplayName(EParameterName.FILENAME_METTER.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setShowIf("(ON_FILES_FLAG == 'true' and ON_METERCATCHER_FLAG == 'true')"); //$NON-NLS-1$
        param.setRequired(true);
        param.setNumRow(14);
        paramList.add(param);
    }

    private static void statsAndLogsParametersDBPart(IProcess process) {
        ElementParameter param;
        IPreferenceStore preferenceStore = DesignerPlugin.getDefault().getPreferenceStore();

        List<IElementParameter> paramList = (List<IElementParameter>) process.getElementParameters();

        // checks current language, if it is perl, set languageType to 0(default value), otherwise to 1.
        int languageType = 0;
        if (LanguageManager.getCurrentLanguage().equals(ECodeLanguage.JAVA)) {
            languageType = 1;
        }

        String languagePrefix = LanguageManager.getCurrentLanguage().toString() + "_"; //$NON-NLS-1$

        // on database
        param = new ElementParameter(process);
        param.setName(EParameterName.ON_DATABASE_FLAG.getName());
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.ON_DATABASE_FLAG.getName()));
        param.setDisplayName(EParameterName.ON_DATABASE_FLAG.getDisplayName()); // On Database
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(50);
        param.setShowIf("(ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')"); //$NON-NLS-1$
        paramList.add(param);

        ElementParameter parentPropertyType = new ElementParameter(process);
        parentPropertyType.setName(EParameterName.PROPERTY_TYPE.getName());
        parentPropertyType.setDisplayName(EParameterName.PROPERTY_TYPE.getDisplayName());
        parentPropertyType.setValue(""); //$NON-NLS-1$
        parentPropertyType.setCategory(EComponentCategory.STATSANDLOGS);
        parentPropertyType.setField(EParameterFieldType.PROPERTY_TYPE);
        parentPropertyType.setRepositoryValue(ERepositoryCategoryType.DATABASE.getName());
        parentPropertyType.setNumRow(51);
        parentPropertyType
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')"); //$NON-NLS-1$
        paramList.add(parentPropertyType);

        param = new ElementParameter(process);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setName(EParameterName.PROPERTY_TYPE.getName());
        param.setDisplayName(EParameterName.PROPERTY_TYPE.getDisplayName());
        param.setListItemsDisplayName(new String[] { EmfComponent.TEXT_BUILTIN, EmfComponent.TEXT_REPOSITORY });
        param.setListItemsDisplayCodeName(new String[] { EmfComponent.BUILTIN, EmfComponent.REPOSITORY });
        param.setListItemsValue(new String[] { EmfComponent.BUILTIN, EmfComponent.REPOSITORY });
        param.setValue(preferenceStore.getString(languagePrefix + EParameterName.PROPERTY_TYPE.getName()));
        param.setNumRow(51);
        param.setField(EParameterFieldType.TECHNICAL);
        param.setRepositoryValue(ERepositoryCategoryType.DATABASE.getName());
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')"); //$NON-NLS-1$

        param.setParentParameter(parentPropertyType);
        // paramList.add(param);

        param = new ElementParameter(process);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setName(EParameterName.REPOSITORY_PROPERTY_TYPE.getName());
        param.setDisplayName(EParameterName.REPOSITORY_PROPERTY_TYPE.getDisplayName());
        param.setListItemsDisplayName(new String[] {});
        param.setListItemsValue(new String[] {});
        param.setNumRow(51);
        param.setField(EParameterFieldType.TECHNICAL);
        param.setValue(preferenceStore.getString(languagePrefix + EParameterName.REPOSITORY_PROPERTY_TYPE.getName())); //$NON-NLS-1$
        param.setShow(false);
        param.setRequired(true);
        // paramList.add(param);
        param.setParentParameter(parentPropertyType);

        // dbType
        param = new ElementParameter(process);
        param.setName(EParameterName.DB_TYPE.getName());
        String type = preferenceStore.getString(languagePrefix + EParameterName.DB_TYPE.getName());
        if (type == null || "".equals(type.trim())) { //$NON-NLS-1$
            type = StatsAndLogsConstants.DB_COMPONENTS[languageType][0];
        }
        param.setValue(type);
        param.setDisplayName(EParameterName.DB_TYPE.getDisplayName());
        param.setField(EParameterFieldType.CLOSED_LIST);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setListItemsDisplayName(StatsAndLogsConstants.DISPLAY_DBNAMES[languageType]);
        param.setListItemsValue(StatsAndLogsConstants.DB_COMPONENTS[languageType]);
        param.setListRepositoryItems(StatsAndLogsConstants.REPOSITORY_ITEMS[languageType]);
        param.setListItemsDisplayCodeName(StatsAndLogsConstants.CODE_LIST[languageType]);
        param.setNumRow(52);
        param.setRepositoryValue("TYPE"); //$NON-NLS-1$
        param.setRequired(true);
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')"); //$NON-NLS-1$
        paramList.add(param);

        // dbVersion
        param = new ElementParameter(process);
        param.setName(EParameterName.DB_VERSION.getName());
        param.setDisplayName(EParameterName.DB_VERSION.getDisplayName());
        param.setField(EParameterFieldType.CLOSED_LIST);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setValue(StatsAndLogsConstants.ORACLE_VERSION_DRIVER[1]);
        param.setListItemsDisplayName(StatsAndLogsConstants.ORACLE_VERSION_DISPLAY);
        param.setListItemsValue(StatsAndLogsConstants.ORACLE_VERSION_DRIVER);
        // param.setListRepositoryItems(StatsAndLogsConstants.REPOSITORY_ITEMS[languageType]);
        param.setListItemsDisplayCodeName(StatsAndLogsConstants.ORACLE_VERSION_CODE);
        param.setNumRow(52);
        param.setRepositoryValue("DB_VERSION"); //$NON-NLS-1$
        param.setRequired(true);
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (DB_TYPE == 'OCLE' or DB_TYPE == 'ACCESS') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')"); //$NON-NLS-1$
        paramList.add(param);

        // host
        param = new ElementParameter(process);
        param.setName(EParameterName.HOST.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.HOST.getName())));
        param.setDisplayName(EParameterName.HOST.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(53);
        param.setRepositoryValue("SERVER_NAME"); //$NON-NLS-1$
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true') and (DB_TYPE!='SQLITE' and DB_TYPE!='ACCESS')"); //$NON-NLS-1$
        paramList.add(param);

        // port
        param = new ElementParameter(process);
        param.setName(EParameterName.PORT.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.PORT.getName())));
        param.setDisplayName(EParameterName.PORT.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(53);
        param.setRepositoryValue("PORT"); //$NON-NLS-1$
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true') and (DB_TYPE!='SQLITE' and DB_TYPE!='ACCESS' and DB_TYPE!='FIREBIRD')"); //$NON-NLS-1$
        paramList.add(param);

        // databaseSource
        // gcui:see bug 7456.
        param = new ElementParameter(process);
        param.setName(EParameterName.DATASOURCE.getName());
        param.setDisplayName(EParameterName.DATASOURCE.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.DATASOURCE.getName())));
        param.setNumRow(53);
        param.setRepositoryValue("DATASOURCE"); //$NON-NLS-1$
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')and (DB_TYPE =='INFORMIX' or DB_TYPE =='GODBC' or DB_TYPE =='MSODBC') "); //$NON-NLS-1$
        paramList.add(param);

        // dbName
        param = new ElementParameter(process);
        param.setName(EParameterName.DBNAME.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.DBNAME.getName())));
        param.setDisplayName(EParameterName.DBNAME.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(54);
        param.setRepositoryValue("SID"); //$NON-NLS-1$
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true') and (DB_TYPE!='SQLITE' and DB_TYPE!='ACCESS' and DB_TYPE!='FIREBIRD')"); //$NON-NLS-1$
        paramList.add(param);

        // additional parameters
        param = new ElementParameter(process);
        param.setName(EParameterName.PROPERTIES.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.PROPERTIES.getName())));
        param.setDisplayName(EParameterName.PROPERTIES.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(54);
        param.setRepositoryValue("PROPERTIES_STRING"); //$NON-NLS-1$
        param
                .setShowIf("(DB_TYPE=='SQL_SERVER' or DB_TYPE=='MYSQL' or DB_TYPE=='INFORMIX') and (ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')"); //$NON-NLS-1$
        paramList.add(param);
        // schema
        param = new ElementParameter(process);
        param.setName(EParameterName.SCHEMA_DB.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.SCHEMA_DB.getName())));
        param.setDisplayName(EParameterName.SCHEMA_DB.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(54);
        param.setRepositoryValue("SCHEMA"); //$NON-NLS-1$
        param
                .setShowIf("(DB_TYPE=='OCLE' or DB_TYPE=='POSTGRESQL') and (ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')"); //$NON-NLS-1$
        paramList.add(param);

        // username
        param = new ElementParameter(process);
        param.setName(EParameterName.USER.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.USER.getName())));
        param.setDisplayName(EParameterName.USER.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(55);
        param.setRequired(true);
        param.setRepositoryValue("USERNAME"); //$NON-NLS-1$
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')and (DB_TYPE!='SQLITE')"); //$NON-NLS-1$
        paramList.add(param);

        // password
        param = new ElementParameter(process);
        param.setName(EParameterName.PASS.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.PASS.getName())));
        param.setDisplayName(EParameterName.PASS.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(55);
        param.setRequired(true);
        param.setRepositoryValue("PASSWORD"); //$NON-NLS-1$
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true') and (DB_TYPE!='SQLITE')"); //$NON-NLS-1$
        paramList.add(param);
        // databse file path
        param = new ElementParameter(process);
        param.setName(EParameterName.DBFILE.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.DBFILE.getName())));
        param.setDisplayName(EParameterName.DBFILE.getDisplayName());
        param.setField(EParameterFieldType.FILE);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(56);
        param.setRepositoryValue("FILE"); //$NON-NLS-1$
        param
                .setShowIf("(DB_TYPE=='SQLITE' or DB_TYPE=='ACCESS' or DB_TYPE=='FIREBIRD') and (ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')"); //$NON-NLS-1$
        paramList.add(param);
        // Stats table
        param = new ElementParameter(process);
        param.setName(EParameterName.TABLE_STATS.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.TABLE_STATS.getName())));
        param.setDisplayName(EParameterName.TABLE_STATS.getDisplayName());
        param.setField(EParameterFieldType.DBTABLE);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(57);
        param.setShowIf("(ON_DATABASE_FLAG == 'true' and ON_STATCATCHER_FLAG == 'true')"); //$NON-NLS-1$
        paramList.add(param);

        // Log table
        param = new ElementParameter(process);
        param.setName(EParameterName.TABLE_LOGS.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.TABLE_LOGS.getName())));
        param.setDisplayName(EParameterName.TABLE_LOGS.getDisplayName());
        param.setField(EParameterFieldType.DBTABLE);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(58);
        param.setShowIf("(ON_DATABASE_FLAG == 'true' and ON_LOGCATCHER_FLAG == 'true')"); //$NON-NLS-1$
        paramList.add(param);

        // Metter table
        param = new ElementParameter(process);
        param.setName(EParameterName.TABLE_METER.getName()); //$NON-NLS-1$
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.TABLE_METER.getName())));
        param.setDisplayName(EParameterName.TABLE_METER.getDisplayName());
        param.setField(EParameterFieldType.DBTABLE);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(59);
        param.setShowIf("(ON_DATABASE_FLAG == 'true' and ON_METERCATCHER_FLAG == 'true')"); //$NON-NLS-1$
        paramList.add(param);
    }

    private static void statsAndLogsParametersFinalPart(IProcess process) {
        ElementParameter param;
        IPreferenceStore preferenceStore = DesignerPlugin.getDefault().getPreferenceStore();

        List<IElementParameter> paramList = (List<IElementParameter>) process.getElementParameters();

        String languagePrefix = LanguageManager.getCurrentLanguage().toString() + "_"; //$NON-NLS-1$

        // Catch runtime errors
        param = new ElementParameter(process);
        param.setName("CATCH_RUNTIME_ERRORS"); //$NON-NLS-1$
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.CATCH_RUNTIME_ERRORS.getName()));
        param.setDisplayName(EParameterName.CATCH_RUNTIME_ERRORS.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(90);
        param
                .setShowIf("((CATCH_RUNTIME_ERRORS == 'true' or CATCH_RUNTIME_ERRORS == 'false') and (ON_LOGCATCHER_FLAG == 'true'))"); //$NON-NLS-1$
        paramList.add(param);

        // Catch user errors
        param = new ElementParameter(process);
        param.setName("CATCH_USER_ERRORS"); //$NON-NLS-1$
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.CATCH_USER_ERRORS.getName()));
        param.setDisplayName(EParameterName.CATCH_USER_ERRORS.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(90);
        param.setShowIf("((CATCH_USER_ERRORS == 'true' or CATCH_USER_ERRORS == 'false') and (ON_LOGCATCHER_FLAG == 'true'))"); //$NON-NLS-1$
        paramList.add(param);

        // Catch user warning
        param = new ElementParameter(process);
        param.setName("CATCH_USER_WARNING"); //$NON-NLS-1$
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.CATCH_USER_WARNING.getName()));
        param.setDisplayName(EParameterName.CATCH_USER_WARNING.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(90);
        param.setShowIf("((CATCH_USER_WARNING == 'true' or CATCH_USER_WARNING == 'false') and (ON_LOGCATCHER_FLAG == 'true'))"); //$NON-NLS-1$
        paramList.add(param);

        // Catch realtime statistics
        param = new ElementParameter(process);
        param.setName("CATCH_REALTIME_STATS"); //$NON-NLS-1$
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.CATCH_REALTIME_STATS.getName()));
        param.setDisplayName(EParameterName.CATCH_REALTIME_STATS.getDisplayName() + " (" //$NON-NLS-1$
                + EParameterName.TSTATCATCHER_STATS.getDisplayName() + ")"); //$NON-NLS-1$
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(91);
        param
                .setShowIf("((CATCH_REALTIME_STATS == 'true' or CATCH_REALTIME_STATS == 'false') and (ON_STATCATCHER_FLAG == 'true'))"); //$NON-NLS-1$
        paramList.add(param);
    }

    /**
     * This function will add quotes only if necessary for the stats & logs.
     * 
     * @param str
     * @return
     */
    public static String addQuotes(String str) {
        // function?
        if (str.contains("(") && str.contains(")")) { //$NON-NLS-1$ //$NON-NLS-2$
            return str;
        }

        switch (LanguageManager.getCurrentLanguage()) {
        case JAVA:
            // if the user already added quotes (anywhere) then don't add.
            if (str.contains("\"")) { //$NON-NLS-1$
                return str;
            }
            break;
        default: // PERL
            // if the user already added quotes (anywhere) then don't add.
            if (str.contains("'")) { //$NON-NLS-1$
                return str;
            }
        }
        if (ContextParameterUtils.containContextVariables(str)) {
            return str;
        }
        return TalendTextUtils.addQuotes(str);
    }

    private static String replaceSlash(String str) {
        String tempStr = str.replaceAll("\\\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
        return tempStr;
    }
}
