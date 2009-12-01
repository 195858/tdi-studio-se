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
package org.talend.sqlbuilder.actions;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Shell;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.PluginChecker;
import org.talend.core.model.properties.DatabaseConnectionItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.RepositoryManager;
import org.talend.core.model.utils.TalendTextUtils;
import org.talend.core.ui.ICDCProviderService;
import org.talend.core.ui.images.ECoreImage;
import org.talend.repository.ProjectManager;
import org.talend.repository.model.ERepositoryStatus;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.IRepositoryService;
import org.talend.repository.model.MetadataTableRepositoryObject;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.QueryEMFRepositoryNode;
import org.talend.repository.model.QueryRepositoryObject;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.ui.actions.AContextualAction;
import org.talend.sqlbuilder.Messages;
import org.talend.sqlbuilder.ui.SQLBuilderDialog;
import org.talend.sqlbuilder.util.ConnectionParameters;
import org.talend.sqlbuilder.util.TextUtil;

/**
 * DOC smallet class global comment. Detailled comment <br/>
 * 
 * $Id: talend.epf 1 2006-09-29 17:06:40 +0000 (ven., 29 sept. 2006) nrousseau $
 * 
 */
public class EditQueriesAction extends AContextualAction {

    public EditQueriesAction() {
        super();
        setImageDescriptor(ImageProvider.getImageDesc(ECoreImage.METADATA_QUERY_ICON));
    }

    protected void doRun() {
        IStructuredSelection selection = (IStructuredSelection) getSelection();
        RepositoryNode node = (RepositoryNode) selection.getFirstElement();

        ConnectionParameters connParameters = new ConnectionParameters();
        if (node.getObjectType() == ERepositoryObjectType.METADATA_CONNECTIONS) {
            connParameters.setRepositoryName(node.getObject().getLabel());
            connParameters.setRepositoryId(node.getObject().getId());
            connParameters.setQuery(""); //$NON-NLS-1$
        } else if (node.getObjectType() == ERepositoryObjectType.METADATA_CON_QUERY) {
            QueryRepositoryObject queryRepositoryObject = (QueryRepositoryObject) node.getObject();
            DatabaseConnectionItem parent = (DatabaseConnectionItem) queryRepositoryObject.getProperty().getItem();
            connParameters.setRepositoryName(parent.getProperty().getLabel());
            connParameters.setRepositoryId(parent.getProperty().getId());
            connParameters.setQueryObject(queryRepositoryObject.getQuery());
            connParameters.setQuery(queryRepositoryObject.getQuery().getValue());
        } else if (node.getObjectType() == ERepositoryObjectType.METADATA_CON_TABLE) {
            DatabaseConnectionItem connectionItem = (DatabaseConnectionItem) node.getObject().getProperty().getItem();
            connParameters.setRepositoryName(connectionItem.getProperty().getLabel());
            connParameters.setRepositoryId(connectionItem.getProperty().getId());
            connParameters.setMetadataTable((MetadataTableRepositoryObject) node.getObject());
            connParameters.setQuery(""); //$NON-NLS-1$
        }

        Shell parentShell = new Shell(getViewPart().getViewer().getControl().getDisplay());
        TextUtil.setDialogTitle(TalendTextUtils.SQL_BUILDER_TITLE_REP);
        SQLBuilderDialog dial = new SQLBuilderDialog(parentShell, node);

        connParameters.setNodeReadOnly(false);
        connParameters.setFromRepository(true);
        dial.setConnParameters(connParameters);
        dial.open();
        RepositoryManager.refreshCreatedNode(ERepositoryObjectType.METADATA_CONNECTIONS);
    }

    public void init(TreeViewer viewer, IStructuredSelection selection) {
        boolean canWork = !selection.isEmpty() && selection.size() == 1;
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        if (factory.isUserReadOnlyOnCurrentProject()) {
            canWork = false;
        }
        if (canWork) {

            Object o = selection.getFirstElement();
            RepositoryNode node = (RepositoryNode) o;
            switch (node.getType()) {
            case REPOSITORY_ELEMENT:
                if (factory.getStatus(node.getObject()) == ERepositoryStatus.DELETED
                        || factory.getStatus(node.getObject()) == ERepositoryStatus.LOCK_BY_OTHER) {
                    canWork = false;
                }
                if (!isUnderDBConnection(node)) {
                    canWork = false;
                }
                if (node.getObjectType() != ERepositoryObjectType.METADATA_CONNECTIONS
                        && node.getObjectType() != ERepositoryObjectType.METADATA_CON_QUERY
                        && node.getObjectType() != ERepositoryObjectType.METADATA_CON_TABLE) {
                    canWork = false;
                } else {
                    // for cdc
                    if (PluginChecker.isCDCPluginLoaded()) {
                        ICDCProviderService cdcService = (ICDCProviderService) GlobalServiceRegister.getDefault().getService(
                                ICDCProviderService.class);
                        if (cdcService != null && cdcService.isSubscriberTableNode(node)) {
                            canWork = false;
                            break;
                        }
                    }
                    IRepositoryService service = (IRepositoryService) GlobalServiceRegister.getDefault().getService(
                            IRepositoryService.class);
                    IProxyRepositoryFactory repFactory = service.getProxyRepositoryFactory();
                    if (repFactory.isPotentiallyEditable(node.getObject())) {
                        this.setText(Messages.getString("EditQueriesAction.textEditQueries")); //$NON-NLS-1$
                    } else {
                        this.setText(Messages.getString("EditQueriesAction.textOpenQueries")); //$NON-NLS-1$
                    }
                }
                break;
            default:
                canWork = false;
            }
            if (canWork && !ProjectManager.getInstance().isInCurrentMainProject(node)) {
                canWork = false;
            }
        }
        setEnabled(canWork);
    }

    @Override
    public Class getClassForDoubleClick() {
        return QueryEMFRepositoryNode.class;
    }
}
