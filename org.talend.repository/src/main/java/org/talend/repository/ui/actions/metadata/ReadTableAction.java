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
package org.talend.repository.ui.actions.metadata;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.talend.commons.ui.image.EImage;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.core.model.metadata.builder.connection.CDCConnection;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.DatabaseConnectionItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.ERepositoryStatus;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.MetadataTableRepositoryObject;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryConstants;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNode.ENodeType;
import org.talend.repository.model.RepositoryNode.EProperties;

/**
 * DOC smallet class global comment. Detailed comment <br/>
 * 
 * $Id$
 * 
 */
public class ReadTableAction extends AbstractCreateTableAction {

    protected static final String LABEL = Messages.getString("CreateTableAction.action.readTitle"); //$NON-NLS-1$

    public ReadTableAction() {
        super();
        this.setText(LABEL);
        this.setToolTipText(LABEL);
        this.setImageDescriptor(ImageProvider.getImageDesc(EImage.READ_ICON));
    }

    protected void init(RepositoryNode node) {
        setEnabled(false);
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        if (ENodeType.REPOSITORY_ELEMENT.equals(node.getType())) {
            if (factory.getStatus(node.getObject()) == ERepositoryStatus.DELETED) {
                return;
            }
            ERepositoryObjectType nodeType = (ERepositoryObjectType) node.getProperties(EProperties.CONTENT_TYPE);
            if (ERepositoryObjectType.METADATA_CON_TABLE.equals(nodeType)) {
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
                                setEnabled(RepositoryConstants.TABLE.equals(tableType));
                                return;
                            }
                        }
                    }
                }

                setEnabled(true);
                return;
            }

        }
    }

    public void run() {
        // RepositoryNode metadataNode = getViewPart().getRoot().getChildren().get(6);
        RepositoryNode metadataNode = getMetadataNode(getCurrentRepositoryNode());
        // Force focus to the repositoryView and open Metadata and DbConnection nodes
        getViewPart().setFocus();
        getViewPart().expand(metadataNode, true);

        IStructuredSelection selection = (IStructuredSelection) getSelection();
        RepositoryNode node = (RepositoryNode) selection.getFirstElement();

        // Init the content of the Wizard
        // init(node);

        ERepositoryObjectType nodeType = (ERepositoryObjectType) node.getProperties(EProperties.CONTENT_TYPE);

        if (ERepositoryObjectType.METADATA_CON_TABLE.equals(nodeType)) {
            ConnectionItem connectionItem = (ConnectionItem) node.getObject().getProperty().getItem();
            nodeType = ERepositoryObjectType.getItemType(connectionItem);
        }

        if (ERepositoryObjectType.METADATA_FILE_POSITIONAL.equals(nodeType)) {
            getViewPart().expand(metadataNode.getChildren().get(2), true);
            createFilePositionalTableWizard(node, true);

        } else if (ERepositoryObjectType.METADATA_FILE_DELIMITED.equals(nodeType)) {
            getViewPart().expand(metadataNode.getChildren().get(1), true);
            createFileDelimitedTableWizard(node, true);

        } else if (ERepositoryObjectType.METADATA_CONNECTIONS.equals(nodeType)) {
            getViewPart().expand(metadataNode.getChildren().get(0), true);
            createDatabaseTableWizard(node, true);

        } else if (ERepositoryObjectType.METADATA_FILE_REGEXP.equals(nodeType)) {
            getViewPart().expand(metadataNode.getChildren().get(0), true);
            createFileRegexpTableWizard(node, true);

        } else if (ERepositoryObjectType.METADATA_FILE_XML.equals(nodeType)) {
            getViewPart().expand(metadataNode.getChildren().get(0), true);
            createFileXmlTableWizard(node, true);

        } else if (ERepositoryObjectType.METADATA_FILE_EXCEL.equals(nodeType)) {
            getViewPart().expand(metadataNode.getChildren().get(0), true);
            createFileExcelTableWizard(node, true);

        } else if (ERepositoryObjectType.METADATA_FILE_LDIF.equals(nodeType)) {
            getViewPart().expand(metadataNode.getChildren().get(0), true);
            createFileLdifTableWizard(node, true);

        } else if (ERepositoryObjectType.METADATA_GENERIC_SCHEMA.equals(nodeType)) {
            getViewPart().expand(metadataNode.getChildren().get(0), true);
            createGenericSchemaWizard(node, true);

        } else if (ERepositoryObjectType.METADATA_LDAP_SCHEMA.equals(nodeType)) {
            getViewPart().expand(metadataNode.getChildren().get(0), true);
            createLDAPSchemaWizard(node, true);

        } else if (ERepositoryObjectType.METADATA_SALESFORCE_SCHEMA.equals(nodeType)) {
            getViewPart().expand(metadataNode.getChildren().get(0), true);
            createSalesforceSchemaWizard(node, true);
        }
    }

    /**
     * DOC qzhang Comment method "getMetadataNode".
     * 
     * @return
     */
    private RepositoryNode getMetadataNode(RepositoryNode node) {
        RepositoryNode parent = node.getParent();
        if (parent != null && parent.getParent() == null) {
            return parent;
        }
        return getMetadataNode(parent);
    }
}
