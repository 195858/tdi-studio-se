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
package org.talend.repository.ui.actions.metadata;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.talend.commons.ui.image.EImage;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.core.model.metadata.builder.connection.AbstractMetadataObject;
import org.talend.core.model.metadata.builder.connection.CDCConnection;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.metadata.builder.connection.Query;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.ContextItem;
import org.talend.core.model.properties.DatabaseConnectionItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.JobletProcessItem;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.model.update.RepositoryUpdateManager;
import org.talend.repository.ProjectManager;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.ISubRepositoryObject;
import org.talend.repository.model.MetadataTableRepositoryObject;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryConstants;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.ui.actions.AContextualAction;

/**
 * ggu class global comment. Detailled comment
 */
public class DetecteViewImpactAction extends AContextualAction {

    private static final String LABEL = Messages.getString("DetecteViewImpactAction.Label"); //$NON-NLS-1$

    /**
     * ggu DetectedModificationAction constructor comment.
     */
    public DetecteViewImpactAction() {
        setText(LABEL);
        setToolTipText(LABEL);
        setImageDescriptor(ImageProvider.getImageDesc(EImage.REFRESH_ICON));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.commons.ui.swt.actions.ITreeContextualAction#init(org.eclipse.jface.viewers.TreeViewer,
     * org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init(TreeViewer viewer, IStructuredSelection selection) {
        boolean canWork = !selection.isEmpty() && selection.size() == 1;
        if (canWork) {
            IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
            if (factory.isUserReadOnlyOnCurrentProject()) {
                canWork = false;
            } else {
                Object o = selection.getFirstElement();
                RepositoryNode node = (RepositoryNode) o;
                switch (node.getType()) {
                case REPOSITORY_ELEMENT:
                    switch (node.getObjectType()) {
                    case METADATA_CON_TABLE:
                        IRepositoryObject repositoryObject = node.getObject();
                        if (repositoryObject != null) {
                            Item item2 = repositoryObject.getProperty().getItem();
                            if (item2 instanceof DatabaseConnectionItem) {
                                DatabaseConnectionItem item = (DatabaseConnectionItem) repositoryObject.getProperty().getItem();
                                DatabaseConnection connection = (DatabaseConnection) item.getConnection();
                                CDCConnection cdcConns = connection.getCdcConns();
                                if (cdcConns != null) {
                                    if (repositoryObject instanceof MetadataTableRepositoryObject) {
                                        MetadataTable table = ((MetadataTableRepositoryObject) repositoryObject).getTable();
                                        String tableType = table.getTableType();
                                        canWork = RepositoryConstants.TABLE.equals(tableType);
                                        break;
                                    }
                                }
                            }
                        }
                        canWork = true;
                        break;
                    case METADATA_CON_QUERY:

                    case METADATA_CONNECTIONS:
                    case METADATA_FILE_DELIMITED:
                    case METADATA_FILE_POSITIONAL:
                    case METADATA_FILE_REGEXP:
                    case METADATA_FILE_XML:
                    case METADATA_FILE_LDIF:
                    case METADATA_FILE_EXCEL:
                    case METADATA_SAPCONNECTIONS:
                    case METADATA_FILE_EBCDIC:
                        // case METADATA_SALESFORCE_SCHEMA:
                        // case METADATA_LDAP_SCHEMA:
                        // case METADATA_WSDL_SCHEMA:
                    case METADATA_FILE_RULES:
                    case CONTEXT:
                    case JOBLET:
                        canWork = true;
                        break;
                    default:
                        canWork = false;
                    }
                    break;
                default:
                    canWork = false;
                }
                if (canWork && !ProjectManager.getInstance().isInCurrentMainProject(node)) {
                    canWork = false;
                }
            }
        }
        setEnabled(canWork);

    }

    @Override
    public void run() {
        RepositoryNode node = getCurrentRepositoryNode();
        if (node == null) {
            return;
        }
        if (node.getObject() instanceof ISubRepositoryObject) {
            ISubRepositoryObject subObject = (ISubRepositoryObject) node.getObject();
            if (subObject != null) {
                // schema
                AbstractMetadataObject metadataObject = subObject.getAbstractMetadataObject();
                if (metadataObject instanceof MetadataTable) {
                    RepositoryUpdateManager.updateSchema((MetadataTable) metadataObject, false);
                } else
                // query
                if (metadataObject instanceof Query) {
                    RepositoryUpdateManager.updateQuery((Query) metadataObject, false);
                }
            }
        } else if (node.getObject() instanceof IRepositoryObject) {
            IRepositoryObject object = (IRepositoryObject) node.getObject();
            if (object != null) {
                Item item = object.getProperty().getItem();
                if (item != null) {
                    // context
                    if (item instanceof ContextItem) {
                        RepositoryUpdateManager.updateContext((ContextItem) item, false);
                    } else
                    // connection
                    if (item instanceof ConnectionItem) {
                        Connection connection = ((ConnectionItem) item).getConnection();
                        if (connection instanceof DatabaseConnection) {
                            RepositoryUpdateManager.updateDBConnection((DatabaseConnection) connection, false);
                        } else {
                            RepositoryUpdateManager.updateFileConnection(connection, false);
                        }
                    } else
                    // joblet
                    if (item instanceof JobletProcessItem) {
                        RepositoryUpdateManager.updateJoblet((JobletProcessItem) item, false);
                    }

                }
            }
        }

    }
}
