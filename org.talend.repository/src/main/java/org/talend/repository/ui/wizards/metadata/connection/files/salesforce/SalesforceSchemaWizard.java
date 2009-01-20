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
package org.talend.repository.ui.wizards.metadata.connection.files.salesforce;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.commons.ui.swt.dialogs.ErrorDialogWidthDetailArea;
import org.talend.commons.utils.VersionUtils;
import org.talend.core.CorePlugin;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.metadata.IMetadataContextModeManager;
import org.talend.core.model.metadata.builder.connection.ConnectionFactory;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.metadata.builder.connection.SalesforceSchemaConnection;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.ui.images.ECoreImage;
import org.talend.designer.core.model.utils.emf.talendfile.ContextType;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNodeUtilities;
import org.talend.repository.ui.utils.ConnectionContextHelper;
import org.talend.repository.ui.wizards.PropertiesWizardPage;
import org.talend.repository.ui.wizards.RepositoryWizard;
import org.talend.repository.ui.wizards.metadata.MetadataContextModeManager;
import org.talend.repository.ui.wizards.metadata.connection.Step0WizardPage;

/**
 * DOC YeXiaowei class global comment. Detailled comment <br/>
 * 
 */
public class SalesforceSchemaWizard extends RepositoryWizard implements INewWizard {

    private static Logger log = Logger.getLogger(SalesforceSchemaWizard.class);

    private PropertiesWizardPage salesforceSchemaWizardPage0 = null;

    private SalesforceWizardPage page1 = null;

    private SalesforceWizardPage page2 = null;

    private SalesforceWizardPage page3 = null;

    private SalesforceSchemaConnection connection = null;

    private Property connectionProperty = null;

    private ConnectionItem connectionItem = null;

    private SalesforceModuleParseAPI salesforceAPI = new SalesforceModuleParseAPI();

    private boolean isSinglePageOnly = false;

    private static final String ALL_STEPS = " 4"; //$NON-NLS-1$

    private IMetadataContextModeManager contextModeManager;

    private boolean isToolbar;

    /**
     * Sets the isToolbar.
     * 
     * @param isToolbar the isToolbar to set
     */
    public void setToolbar(boolean isToolbar) {
        this.isToolbar = isToolbar;
    }

    /**
     * DOC YeXiaowei SalesforceSchemaWizard constructor comment.
     * 
     * @param workbench
     * @param creation
     * @param forceReadOnly
     */
    public SalesforceSchemaWizard(IWorkbench workbench, boolean creation, ISelection selection, String[] existingNames,
            boolean isSinglePageOnly) {
        super(workbench, creation);
        this.selection = selection;
        this.existingNames = existingNames;
        this.isSinglePageOnly = isSinglePageOnly;
        setNeedsProgressMonitor(true);

        // TODO: should to changed icon.
        setDefaultPageImageDescriptor(ImageProvider.getImageDesc(ECoreImage.DEFAULT_WIZ));

        if (selection == null || existingNames == null) {
            connectionProperty = PropertiesFactory.eINSTANCE.createProperty();
            connectionProperty
                    .setAuthor(((RepositoryContext) CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY))
                            .getUser());
            connectionProperty.setVersion(VersionUtils.DEFAULT_VERSION);
            connectionProperty.setStatusCode(""); //$NON-NLS-1$

            connectionItem = PropertiesFactory.eINSTANCE.createSalesforceSchemaConnectionItem();
            connectionItem.setProperty(connectionProperty);
            return;
        }

        Object obj = ((IStructuredSelection) selection).getFirstElement();
        RepositoryNode node = (RepositoryNode) obj;
        switch (node.getType()) {
        case SIMPLE_FOLDER:
        case REPOSITORY_ELEMENT:
            pathToSave = RepositoryNodeUtilities.getPath(node);
            break;
        case SYSTEM_FOLDER:
            pathToSave = new Path(""); //$NON-NLS-1$
            break;
        }

        switch (node.getType()) {
        case SIMPLE_FOLDER:
        case SYSTEM_FOLDER:
            connection = ConnectionFactory.eINSTANCE.createSalesforceSchemaConnection();
            MetadataTable metadataTable = ConnectionFactory.eINSTANCE.createMetadataTable();
            IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
            metadataTable.setId(factory.getNextId());
            connection.getTables().add(metadataTable);
            connectionProperty = PropertiesFactory.eINSTANCE.createProperty();
            connectionProperty
                    .setAuthor(((RepositoryContext) CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY))
                            .getUser());
            connectionProperty.setVersion(VersionUtils.DEFAULT_VERSION);
            connectionProperty.setStatusCode(""); //$NON-NLS-1$

            connectionItem = PropertiesFactory.eINSTANCE.createSalesforceSchemaConnectionItem();
            connectionItem.setProperty(connectionProperty);
            connectionItem.setConnection(connection);
            break;

        case REPOSITORY_ELEMENT:
            connection = (SalesforceSchemaConnection) ((ConnectionItem) node.getObject().getProperty().getItem()).getConnection();
            connectionProperty = node.getObject().getProperty();
            connectionItem = (ConnectionItem) node.getObject().getProperty().getItem();
            setRepositoryObject(node.getObject());
            isRepositoryObjectEditable();
            initLockStrategy();
            break;
        }
        initConnection();
    }

    public SalesforceSchemaWizard(IWorkbench workbench, boolean creation, RepositoryNode node, String[] existingNames,
            boolean isSinglePageOnly) {
        super(workbench, creation);
        this.existingNames = existingNames;
        this.isSinglePageOnly = isSinglePageOnly;
        setNeedsProgressMonitor(true);

        // TODO: should to changed icon.
        setDefaultPageImageDescriptor(ImageProvider.getImageDesc(ECoreImage.DEFAULT_WIZ));
        switch (node.getType()) {
        case SIMPLE_FOLDER:
        case REPOSITORY_ELEMENT:
            pathToSave = RepositoryNodeUtilities.getPath(node);
            break;
        case SYSTEM_FOLDER:
            pathToSave = new Path(""); //$NON-NLS-1$
            break;
        }

        switch (node.getType()) {
        case SIMPLE_FOLDER:
        case SYSTEM_FOLDER:
            connection = ConnectionFactory.eINSTANCE.createSalesforceSchemaConnection();
            MetadataTable metadataTable = ConnectionFactory.eINSTANCE.createMetadataTable();
            IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
            metadataTable.setId(factory.getNextId());
            connection.getTables().add(metadataTable);
            connectionProperty = PropertiesFactory.eINSTANCE.createProperty();
            connectionProperty
                    .setAuthor(((RepositoryContext) CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY))
                            .getUser());
            connectionProperty.setVersion(VersionUtils.DEFAULT_VERSION);
            connectionProperty.setStatusCode(""); //$NON-NLS-1$

            connectionItem = PropertiesFactory.eINSTANCE.createSalesforceSchemaConnectionItem();
            connectionItem.setProperty(connectionProperty);
            connectionItem.setConnection(connection);
            break;

        case REPOSITORY_ELEMENT:
            connection = (SalesforceSchemaConnection) ((ConnectionItem) node.getObject().getProperty().getItem()).getConnection();
            connectionProperty = node.getObject().getProperty();
            connectionItem = (ConnectionItem) node.getObject().getProperty().getItem();
            // set the repositoryObject, lock and set isRepositoryObjectEditable
            setRepositoryObject(node.getObject());
            isRepositoryObjectEditable();
            initLockStrategy();
            break;
        }
        initConnection();
    }

    private void initConnection() {
        ConnectionContextHelper.checkContextMode(connectionItem);
        contextModeManager = new MetadataContextModeManager();
        if (connectionItem.getConnection().isContextMode()) {
            ContextType contextTypeForContextMode = ConnectionContextHelper.getContextTypeForContextMode(connectionItem
                    .getConnection());
            contextModeManager.setSelectedContextType(contextTypeForContextMode);
        }
    }

    @Override
    public void addPages() {
        if (isToolbar) {
            pathToSave = null;
        }
        salesforceSchemaWizardPage0 = new Step0WizardPage(connectionProperty, pathToSave,
                ERepositoryObjectType.METADATA_SALESFORCE_SCHEMA, !isRepositoryObjectEditable(), creation);

        salesforceSchemaWizardPage0.setTitle(Messages.getString("FileWizardPage.titleCreate") + " 1 " //$NON-NLS-1$ //$NON-NLS-2$
                + Messages.getString("FileWizardPage.of") + ALL_STEPS); //$NON-NLS-1$ //$NON-NLS-2$
        salesforceSchemaWizardPage0.setDescription(Messages.getString("FileWizardPage.descriptionCreateStep0")); //$NON-NLS-1$

        addPage(salesforceSchemaWizardPage0);

        if (creation) {
            setWindowTitle(Messages.getString("SalesforceSchemaWizard.windowTitleCreate")); //$NON-NLS-1$
        } else {
            setWindowTitle(Messages.getString("SalesforceSchemaWizard.windowTitleUpdate")); //$NON-NLS-1$
        }

        page1 = new SalesforceWizardPage(1, connectionItem, isRepositoryObjectEditable(), existingNames, salesforceAPI,
                contextModeManager);

        page1.setTitle(Messages.getString("FileWizardPage.titleCreate") + " 2 " //$NON-NLS-1$ //$NON-NLS-2$
                + Messages.getString("FileWizardPage.of") + ALL_STEPS); //$NON-NLS-1$ //$NON-NLS-2$
        page1.setDescription(Messages.getString("FileWizardPage.descriptionCreateStep1")); //$NON-NLS-1$

        addPage(page1);

        page2 = new SalesforceWizardPage(2, connectionItem, isRepositoryObjectEditable(), existingNames, salesforceAPI,
                contextModeManager);

        page2.setTitle(Messages.getString("FileWizardPage.titleCreate") + " 3 " //$NON-NLS-1$ //$NON-NLS-2$
                + Messages.getString("FileWizardPage.of") + ALL_STEPS); //$NON-NLS-1$ //$NON-NLS-2$
        page2.setDescription(Messages.getString("FileWizardPage.descriptionCreateStep2")); //$NON-NLS-1$

        addPage(page2);

        if (creation) {
            page3 = new SalesforceWizardPage(3, connectionItem, isRepositoryObjectEditable(), existingNames, salesforceAPI,
                    contextModeManager);

            page3.setTitle(Messages.getString("FileWizardPage.titleCreate") + " 4 " //$NON-NLS-1$ //$NON-NLS-2$
                    + Messages.getString("FileWizardPage.of") + ALL_STEPS); //$NON-NLS-1$ //$NON-NLS-2$
            page3.setDescription(Messages.getString("FileWizardPage.descriptionCreateStep3")); //$NON-NLS-1$

            addPage(page3);
            page3.setPageComplete(false);
        }
        page1.setPageComplete(false);
        page2.setPageComplete(false);
    }

    @Override
    public boolean performFinish() {

        boolean formIsPerformed = false;

        if (page3 == null) {
            formIsPerformed = page1.isPageComplete();
        } else {
            formIsPerformed = page3.isPageComplete();
        }

        if (formIsPerformed) {
            IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
            try {
                if (creation) {
                    String nextId = factory.getNextId();
                    connectionProperty.setId(nextId);
                    factory.create(connectionItem, salesforceSchemaWizardPage0.getDestinationPath());
                } else {
                    factory.save(connectionItem);
                    closeLockStrategy();
                }
            } catch (PersistenceException e) {
                String detailError = e.toString();
                new ErrorDialogWidthDetailArea(getShell(), PID, Messages.getString("CommonWizard.persistenceException"), //$NON-NLS-1$
                        detailError);
                log.error(Messages.getString("CommonWizard.persistenceException") + "\n" + detailError); //$NON-NLS-1$ //$NON-NLS-2$
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.selection = selection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.wizards.RepositoryWizard#getConnectionItem()
     */
    public ConnectionItem getConnectionItem() {
        return this.connectionItem;
    }

}
