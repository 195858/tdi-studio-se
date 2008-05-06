// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core.ui.editor.cmd;

import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.MetadataTable;
import org.talend.core.model.process.EConnectionType;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INodeConnector;
import org.talend.core.model.utils.TalendTextUtils;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.process.ConnectionManager;
import org.talend.designer.core.ui.editor.connections.Connection;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.process.Process;

/**
 * Command that creates a new connection. <br/>
 * 
 * $Id$
 * 
 */
public class ConnectionCreateCommand extends Command {

    protected Connection connection;

    private String connectorName;

    private EConnectionType newLineStyle;

    protected Node source;

    protected Node target = null;

    private String metaName;

    private String connectionName;

    private IMetadataTable newMetadata;

    private static boolean creatingConnection = false;

    /**
     * Initialisation of the creation of the connection with a source and style of connection.
     * 
     * @param nodeSource source of the connection
     * @param lineStyle line style
     * @param meta
     */
    public ConnectionCreateCommand(Node nodeSource, String connectorName, List<Object> listArgs) {
        setLabel(Messages.getString("ConnectionCreateCommand.Label")); //$NON-NLS-1$
        this.source = nodeSource;
        this.connectorName = connectorName;
        this.metaName = (String) listArgs.get(0);
        this.connectionName = (String) listArgs.get(1);
        newMetadata = (IMetadataTable) listArgs.get(2);
    }

    /**
     * Set the target of the connection, the source has already been set before.
     * 
     * @param target
     */
    public void setTarget(Node targetNode) {
        this.target = targetNode;
    }

    private String askForConnectionName(String nodeLabel, String oldName) {

        // check if the source got the ELT Table parameter, if yes, take the name by default
        IElementParameter elementParam = source.getElementParameter("ELT_TABLE_NAME");
        if (source.isELTComponent() && elementParam != null && elementParam.getField().equals(EParameterFieldType.TEXT)) {
            String name2 = elementParam.getValue().toString();
            if (name2 != null) {
                name2 = TalendTextUtils.removeQuotes(name2);
                if (!name2.equals("")) {
                    return name2;
                }
            }
        }

        // check if the target got the ELT Table parameter, if yes, take the name by default
        elementParam = target.getElementParameter("ELT_TABLE_NAME");
        if (target.isELTComponent() && elementParam != null && elementParam.getField().equals(EParameterFieldType.TEXT)) {
            String name2 = elementParam.getValue().toString();
            if (name2 != null) {
                name2 = TalendTextUtils.removeQuotes(name2);
                if (!name2.equals("")) {
                    return name2;
                }
            }
        }

        InputDialog id = new InputDialog(null, nodeLabel + Messages.getString("ConnectionCreateAction.dialogTitle"), //$NON-NLS-1$
                Messages.getString("ConnectionCreateAction.dialogMessage"), oldName, null); //$NON-NLS-1$ //$NON-NLS-2$
        id.open();
        if (id.getReturnCode() == InputDialog.CANCEL) {
            return ""; //$NON-NLS-1$
        }
        return id.getValue();
    }

    public boolean canExecute() {

        if (target != null) {
            if (!ConnectionManager.canConnectToTarget(source, null, target, source.getConnectorFromName(connectorName)
                    .getDefaultConnectionType(), connectorName, connectionName)) {
                creatingConnection = false;
                return false;
            }
            newLineStyle = ConnectionManager.getNewConnectionType();
        }
        creatingConnection = true;
        return true;
    }

    public void execute() {
        if (connectionName == null) {
            // ask for the name if there is no

            final INodeConnector mainConnector;

            EConnectionType connecType;
            if (source.isELTComponent()) {
                connecType = EConnectionType.TABLE;
            } else {
                connecType = EConnectionType.FLOW_MAIN;
            }
            mainConnector = source.getConnectorFromType(connecType);

            if (source.getConnectorFromName(connectorName).isBuiltIn()) {
                boolean connectionOk = false;
                while (!connectionOk) {
                    connectionName = askForConnectionName(source.getLabel(), connectionName);
                    if (connectionName.equals("")) {
                        creatingConnection = false;
                        dispose();
                        return;
                    }
                    metaName = connectionName;
                    newMetadata = new MetadataTable();
                    newMetadata.setTableName(connectionName);
                    newMetadata.setLabel(connectionName);
                    if ((connecType.equals(EConnectionType.TABLE) || source.getProcess().checkValidConnectionName(connectionName))
                            && ConnectionManager.canConnectToTarget(source, null, target, source.getConnectorFromName(
                                    connectorName).getDefaultConnectionType(), connectorName, connectionName)) {
                        connectionOk = true;
                    } else {
                        String message = Messages.getString("ConnectionCreateAction.errorCreateConnectionName", //$NON-NLS-1$
                                connectionName);
                        MessageDialog.openError(null, Messages.getString("ConnectionCreateAction.error"), message); //$NON-NLS-1$
                    }
                }

            } else {
                newMetadata = null;

                if (source.isELTComponent()) {
                    connectionName = askForConnectionName(source.getLabel(), connectionName);
                } else {
                    metaName = source.getMetadataFromConnector(mainConnector.getName()).getTableName();
                    String baseName = source.getConnectionName();
                    if (source.getProcess().checkValidConnectionName(baseName)) {
                        connectionName = source.getProcess().generateUniqueConnectionName(baseName);
                    }
                }
            }

        }

        if (newMetadata != null) {
            source.getMetadataList().add(newMetadata);
            this.connection = new Connection(source, target, newLineStyle, connectorName, metaName, connectionName);
        } else {
            this.connection = new Connection(source, target, newLineStyle, connectorName, metaName, connectionName, metaName);
        }
        INodeConnector nodeConnectorSource, nodeConnectorTarget;
        nodeConnectorSource = connection.getSourceNodeConnector();
        nodeConnectorSource.setCurLinkNbOutput(nodeConnectorSource.getCurLinkNbOutput() + 1);
        nodeConnectorTarget = connection.getTargetNodeConnector();
        nodeConnectorTarget.setCurLinkNbInput(nodeConnectorTarget.getCurLinkNbInput() + 1);

        if ((newLineStyle == EConnectionType.SYNCHRONIZE) || (newLineStyle == EConnectionType.PARALLELIZE)) {
            ((Process) source.getProcess()).setPropertyValue(EParameterName.MULTI_THREAD_EXECATION.getName(), Boolean.TRUE);
        }

        creatingConnection = false;
        ((Process) source.getProcess()).checkStartNodes();
        source.checkAndRefreshNode();
        target.checkAndRefreshNode();
    }

    public void undo() {
        connection.disconnect();
        INodeConnector nodeConnectorSource, nodeConnectorTarget;
        nodeConnectorSource = connection.getSourceNodeConnector();
        nodeConnectorSource.setCurLinkNbOutput(nodeConnectorSource.getCurLinkNbOutput() - 1);
        nodeConnectorTarget = connection.getTargetNodeConnector();
        nodeConnectorTarget.setCurLinkNbInput(nodeConnectorTarget.getCurLinkNbInput() - 1);
        if (newMetadata != null) {
            source.getMetadataList().remove(newMetadata);
        }
        ((Process) source.getProcess()).checkStartNodes();
        source.checkAndRefreshNode();
        target.checkAndRefreshNode();
        // ((Process) source.getProcess()).checkProcess();
    }

    public void redo() {
        execute();
    }

    /**
     * Getter for creatingConnection.
     * 
     * @return the creatingConnection
     */
    public static boolean isCreatingConnection() {
        return creatingConnection;
    }

    /**
     * Sets the creatingConnection.
     * 
     * @param creatingConnection the creatingConnection to set
     */
    public static void setCreatingConnection(boolean creatingConnection) {
        ConnectionCreateCommand.creatingConnection = creatingConnection;
    }
}
