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
package org.talend.repository;

import java.beans.PropertyChangeEvent;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.SystemException;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.PluginChecker;
import org.talend.core.context.Context;
import org.talend.core.model.components.ComponentUtilities;
import org.talend.core.model.components.IComponentsFactory;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.metadata.designerproperties.ComponentToRepositoryProperty;
import org.talend.core.model.migration.IMigrationToolService;
import org.talend.core.model.process.IContextManager;
import org.talend.core.model.process.IContextParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.Property;
import org.talend.core.model.properties.SQLPatternItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.ui.DisableLanguageActions;
import org.talend.core.ui.IEBCDICProviderService;
import org.talend.core.ui.ISAPProviderService;
import org.talend.designer.runprocess.IRunProcessService;
import org.talend.repository.model.ComponentsFactoryProvider;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.IRepositoryService;
import org.talend.repository.model.ProjectRepositoryNode;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNodeUtilities;
import org.talend.repository.plugin.integration.BindingActions;
import org.talend.repository.plugin.integration.SwitchProjectAction;
import org.talend.repository.ui.actions.metadata.AbstractCreateTableAction;
import org.talend.repository.ui.actions.metadata.CreateTableAction;
import org.talend.repository.ui.actions.sqlpattern.CreateSqlpatternAction;
import org.talend.repository.ui.actions.sqlpattern.EditSqlpatternAction;
import org.talend.repository.ui.dialog.ContextRepositoryReviewDialog;
import org.talend.repository.ui.login.LoginDialog;
import org.talend.repository.ui.utils.ColumnNameValidator;
import org.talend.repository.ui.utils.DBConnectionContextUtils;
import org.talend.repository.ui.utils.DataStringConnection;
import org.talend.repository.ui.views.IRepositoryView;
import org.talend.repository.ui.views.RepositoryView;
import org.talend.repository.ui.wizards.RepositoryWizard;
import org.talend.repository.ui.wizards.metadata.connection.database.DatabaseWizard;
import org.talend.repository.ui.wizards.metadata.connection.files.delimited.DelimitedFileWizard;
import org.talend.repository.ui.wizards.metadata.connection.files.excel.ExcelFileWizard;
import org.talend.repository.ui.wizards.metadata.connection.files.ldif.LdifFileWizard;
import org.talend.repository.ui.wizards.metadata.connection.files.positional.FilePositionalWizard;
import org.talend.repository.ui.wizards.metadata.connection.files.regexp.RegexpFileWizard;
import org.talend.repository.ui.wizards.metadata.connection.files.salesforce.SalesforceSchemaWizard;
import org.talend.repository.ui.wizards.metadata.connection.files.xml.XmlFileWizard;
import org.talend.repository.ui.wizards.metadata.connection.genericshema.GenericSchemaWizard;
import org.talend.repository.ui.wizards.metadata.connection.ldap.LDAPSchemaWizard;
import org.talend.repository.ui.wizards.metadata.connection.wsdl.WSDLSchemaWizard;
import org.talend.repository.utils.RepositoryPathProvider;

;

/**
 * DOC qian class global comment. Detailled comment <br/>
 * 
 * $Id: talend-code-templates.xml 1 2006-09-29 17:06:40 +0000 (星期五, 29 九月 2006) nrousseau $
 * 
 */

public class RepositoryService implements IRepositoryService {

    private GenericSchemaWizard genericSchemaWizard = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryService#getComponentsFactory()
     */
    public IComponentsFactory getComponentsFactory() {
        return ComponentsFactoryProvider.getInstance();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryService#getPathFileName(java.lang.String, java.lang.String)
     */
    public IPath getPathFileName(String folderName, String fileName) {
        return RepositoryPathProvider.getPathFileName(folderName, fileName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryService#getProxyRepositoryFactory()
     */
    public IProxyRepositoryFactory getProxyRepositoryFactory() {
        return ProxyRepositoryFactory.getInstance();
    }

    public IPath getRepositoryPath(RepositoryNode node) {
        return RepositoryNodeUtilities.getPath(node);
    }

    ChangeProcessor changeProcessor = new ChangeProcessor();

    /*
     * (non-Javadoc)
     * 
     * @seeorg.talend.repository.model.IRepositoryService#registerRepositoryChangedListener(org.talend.repository.
     * IRepositoryChangedListener)
     */
    public void registerRepositoryChangedListener(IRepositoryChangedListener listener) {
        changeProcessor.addRepositoryChangedListener(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.repository.model.IRepositoryService#registerRepositoryChangedListenerAsFirst(org.talend.repository
     * .IRepositoryChangedListener)
     */
    public void registerRepositoryChangedListenerAsFirst(IRepositoryChangedListener listener) {
        changeProcessor.registerRepositoryChangedListenerAsFirst(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.talend.repository.model.IRepositoryService#removeRepositoryChangedListener(org.talend.repository.
     * IRepositoryChangedListener)
     */
    public void removeRepositoryChangedListener(IRepositoryChangedListener listener) {
        changeProcessor.removeRepositoryChangedListener(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.repository.model.IRepositoryService#repositoryChanged(org.talend.repository.RepositoryElementDelta)
     */
    public void repositoryChanged(RepositoryElementDelta delta) {
        changeProcessor.repositoryChanged(delta);
    }

    // This method is used for the Action in RepositoryView to synchronize the sqlBuilder.
    // see DataBaseWizard, DatabaseTableWizard, AContextualAction
    public void notifySQLBuilder(List<IRepositoryObject> list) {
        IRepositoryChangedListener listener = (IRepositoryChangedListener) RepositoryView.show();
        removeRepositoryChangedListener(listener);
        for (Iterator<IRepositoryObject> iter = list.iterator(); iter.hasNext();) {
            IRepositoryObject element = iter.next();
            repositoryChanged(new RepositoryElementDelta(element));
        }
        registerRepositoryChangedListenerAsFirst(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryService#validateColumnName(java.lang.String, int)
     */
    public String validateColumnName(String columnName, int index) {
        return ColumnNameValidator.validateColumnNameFormat(columnName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryService#getGenericSchemaWizardDialog(org.eclipse.swt.widgets.Shell,
     * org.eclipse.ui.IWorkbench, boolean, org.eclipse.jface.viewers.ISelection, java.lang.String[], boolean)
     */
    public WizardDialog getGenericSchemaWizardDialog(Shell shell, IWorkbench workbench, boolean creation, ISelection selection,
            String[] existingNames, boolean isSinglePageOnly) {

        genericSchemaWizard = new GenericSchemaWizard(workbench, creation, selection, existingNames, isSinglePageOnly);
        return new WizardDialog(shell, genericSchemaWizard);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryService#getPropertyFromWizardDialog()
     */
    public Property getPropertyFromWizardDialog() {
        if (this.genericSchemaWizard != null) {
            return this.genericSchemaWizard.getConnectionProperty();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryService#getPathForSaveAsGenericSchema()
     */
    public IPath getPathForSaveAsGenericSchema() {
        if (this.genericSchemaWizard != null) {
            return this.genericSchemaWizard.getPathForSaveAsGenericSchema();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryService#openLoginDialog()
     */
    public void openLoginDialog() {

        if (CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY) != null) {
            return;
        }

        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        boolean logged = false;
        LoginDialog loginDialog = new LoginDialog(shell);
        // PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(true);
        logged = loginDialog.open() == LoginDialog.OK;
        if (logged) {

            // addCommand();
            new DisableLanguageActions().earlyStartup();

            new BindingActions().bind();

            IMigrationToolService toolService = CorePlugin.getDefault().getMigrationToolService();
            toolService.executeMigration(SwitchProjectAction.PLUGIN_MODEL);

            IRunProcessService runService = CorePlugin.getDefault().getRunProcessService();
            runService.deleteAllJobs(SwitchProjectAction.PLUGIN_MODEL);

            CorePlugin.getDefault().getCodeGeneratorService().initializeTemplates();
            CorePlugin.getDefault().getDesignerCoreService().synchronizeDesignerUI(
                    new PropertyChangeEvent(this, ComponentUtilities.NORMAL, null, null));

        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryService#initializeForTalendStartupJob()
     */
    public void initializeForTalendStartupJob() {
        // do nothing now.

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryService#initializeTalend()
     */
    public void initializePluginMode() {

        if (CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY) != null) {
            return;
        }
        openLoginDialog();
    }

    boolean rcpMode = false;

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryService#isRCPMode()
     */
    public boolean isRCPMode() {
        return rcpMode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryService#setRCPMode()
     */
    public void setRCPMode() {
        rcpMode = true;
    }

    public void openMetadataConnection(IRepositoryObject o) {
        final RepositoryNode realNode = RepositoryNodeUtilities.getRepositoryNode(o);
        openMetadataConnection(false, realNode, null);
    }

    public ConnectionItem openMetadataConnection(boolean creation, RepositoryNode realNode, INode node) {

        if (realNode != null) {
            IWizard relatedWizard = null;
            ERepositoryObjectType objectType = null;
            if (creation) {
                objectType = realNode.getContentType();
            } else {
                objectType = realNode.getObjectType();
            }
            if (objectType.equals(ERepositoryObjectType.METADATA_CONNECTIONS)) {
                relatedWizard = new DatabaseWizard(PlatformUI.getWorkbench(), creation, realNode, null);
            } else if (objectType.equals(ERepositoryObjectType.METADATA_FILE_DELIMITED)) {
                relatedWizard = new DelimitedFileWizard(PlatformUI.getWorkbench(), creation, realNode, null);
            } else if (objectType.equals(ERepositoryObjectType.METADATA_FILE_LDIF)) {
                relatedWizard = new LdifFileWizard(PlatformUI.getWorkbench(), creation, realNode, null);
            } else if (objectType.equals(ERepositoryObjectType.METADATA_FILE_POSITIONAL)) {
                relatedWizard = new FilePositionalWizard(PlatformUI.getWorkbench(), creation, realNode, null);
            } else if (objectType.equals(ERepositoryObjectType.METADATA_FILE_REGEXP)) {
                relatedWizard = new RegexpFileWizard(PlatformUI.getWorkbench(), creation, realNode, null);
            } else if (objectType.equals(ERepositoryObjectType.METADATA_FILE_XML)) {
                relatedWizard = new XmlFileWizard(PlatformUI.getWorkbench(), creation, realNode, null);
            } else if (objectType.equals(ERepositoryObjectType.METADATA_GENERIC_SCHEMA)) {
                relatedWizard = new GenericSchemaWizard(PlatformUI.getWorkbench(), creation, realNode, null, true);
            } else if (objectType.equals(ERepositoryObjectType.METADATA_WSDL_SCHEMA)) {
                relatedWizard = new WSDLSchemaWizard(PlatformUI.getWorkbench(), creation, realNode, null, false);
            } else if (objectType.equals(ERepositoryObjectType.METADATA_LDAP_SCHEMA)) {
                relatedWizard = new LDAPSchemaWizard(PlatformUI.getWorkbench(), creation, realNode, null, false);
            } else if (objectType.equals(ERepositoryObjectType.METADATA_FILE_EXCEL)) {
                relatedWizard = new ExcelFileWizard(PlatformUI.getWorkbench(), creation, realNode, null);
            } else if (objectType.equals(ERepositoryObjectType.METADATA_SALESFORCE_SCHEMA)) {
                relatedWizard = new SalesforceSchemaWizard(PlatformUI.getWorkbench(), creation, realNode, null, false);
            } else if (objectType.equals(ERepositoryObjectType.METADATA_FILE_EBCDIC)) {
                if (PluginChecker.isEBCDICPluginLoaded()) {
                    IEBCDICProviderService service = (IEBCDICProviderService) GlobalServiceRegister.getDefault().getService(
                            IEBCDICProviderService.class);
                    if (service != null) {
                        relatedWizard = service.newEbcdicWizard(PlatformUI.getWorkbench(), creation, realNode, null);
                    }
                }
            } else if (objectType.equals(ERepositoryObjectType.METADATA_SAPCONNECTIONS)) {
                if (PluginChecker.isSAPWizardPluginLoaded()) {
                    ISAPProviderService service = (ISAPProviderService) GlobalServiceRegister.getDefault().getService(
                            ISAPProviderService.class);
                    if (service != null) {
                        relatedWizard = service.newSAPWizard(PlatformUI.getWorkbench(), creation, realNode, null);
                    }
                }
            }
            boolean changed = false;
            if (relatedWizard != null) {
                ConnectionItem connItem = null;
                if (creation && node != null && relatedWizard instanceof RepositoryWizard) {
                    connItem = ((RepositoryWizard) relatedWizard).getConnectionItem();
                    if (connItem != null) {
                        changed = ComponentToRepositoryProperty.setValue(connItem.getConnection(), node);
                    }
                }
                if (connItem != null && changed) {
                    // Open the Wizard
                    WizardDialog wizardDialog = new WizardDialog(Display.getCurrent().getActiveShell(), relatedWizard);

                    wizardDialog.setPageSize(600, 500);
                    wizardDialog.create();
                    if (wizardDialog.open() == wizardDialog.OK) {
                        return connItem;
                    }
                }
            }
        }
        return null;
    }

    public void openEditSchemaWizard(String value) {
        final RepositoryNode realNode = RepositoryNodeUtilities.getMetadataTableFromConnection(value);
        if (realNode != null) {
            AbstractCreateTableAction action = new CreateTableAction() {

                /*
                 * (non-Javadoc)
                 * 
                 * @see org.talend.repository.ui.actions.AContextualAction#getSelection()
                 */
                @Override
                public ISelection getSelection() {
                    return new StructuredSelection(realNode);
                }
            };
            action.run();
        }
    }

    public DatabaseConnection cloneOriginalValueConnection(DatabaseConnection dbConn) {
        return DBConnectionContextUtils.cloneOriginalValueConnection(dbConn);
    }

    public IEditorPart openSQLPatternEditor(SQLPatternItem item, boolean readOnly) {
        IEditorPart openSQLPatternEditor = null;
        try {
            openSQLPatternEditor = new EditSqlpatternAction().openSQLPatternEditor(item, readOnly);
        } catch (PartInitException e) {
            ExceptionHandler.process(e);
        } catch (SystemException e) {
            ExceptionHandler.process(e);
        }
        return openSQLPatternEditor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryService#createSqlpattern()
     */
    public void createSqlpattern(String path, boolean isFromSqlPatternComposite) {
        new CreateSqlpatternAction(path, isFromSqlPatternComposite).run();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryService#addRepositoryViewListener(org.eclipse.ui.ISelectionListener)
     */
    public void addRepositoryTreeViewListener(ISelectionChangedListener listener) {
        TreeViewer treeViewer = getRepositoryTreeView();
        if (treeViewer != null) {
            treeViewer.addSelectionChangedListener(listener);
        } else {
            RepositoryView.addPreparedListeners(listener);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.talend.repository.model.IRepositoryService#removeRepositoryTreeViewListener(org.eclipse.jface.viewers.
     * ISelectionChangedListener)
     */
    public void removeRepositoryTreeViewListener(ISelectionChangedListener listener) {
        TreeViewer treeViewer = getRepositoryTreeView();
        if (treeViewer != null) {
            treeViewer.removeSelectionChangedListener(listener);
        }
    }

    /**
     * yzhang Comment method "getRepositoryView".
     * 
     * @return
     */
    public TreeViewer getRepositoryTreeView() {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page != null) {
            IViewPart view = page.findView(RepositoryView.ID);
            if (view == null) {
                try {
                    view = page.showView(RepositoryView.ID);
                } catch (Exception e) {
                    ExceptionHandler.process(e);
                }
            }
            return ((RepositoryView) view).getViewer();
        } else {
            return null;
        }
    }

    public IPreferenceStore getRepositoryPreferenceStore() {
        return RepositoryPlugin.getDefault().getPreferenceStore();
    }

    public RepositoryNode getRepositoryNode(String id, boolean expanded) {
        return RepositoryNodeUtilities.getRepositoryNode(id, expanded);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.talend.repository.model.IRepositoryService#openRepositoryReviewDialog(org.talend.core.model.repository.
     * ERepositoryObjectType, java.lang.String)
     */
    public void openRepositoryReviewDialog(ERepositoryObjectType type, String repositoryType, List<IContextParameter> params,
            IContextManager contextManager) {
        ContextRepositoryReviewDialog dialog = new ContextRepositoryReviewDialog(new Shell(), type, params, contextManager);
        dialog.open();
    }

    /**
     * wzhang Comment method "getRootRepositoryNode".
     * 
     * @param type
     * @return
     */
    public RepositoryNode getRootRepositoryNode(ERepositoryObjectType type) {
        IRepositoryView view = RepositoryView.show();
        if (view != null) {
            ProjectRepositoryNode root = (ProjectRepositoryNode) view.getRoot();
            return root.getRootRepositoryNode(type);
        }
        return null;
    }

    /**
     * wzhang Comment method "getDatabaseStringURL".
     * 
     * @param conn
     * @return
     */
    public String getDatabaseStringURL(DatabaseConnection conn) {
        DataStringConnection dataStrConn = new DataStringConnection();
        return dataStrConn.getUrlConnectionStr(conn);
    }

    public Action getRepositoryViewDoubleClickAction() {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page != null) {
            IViewPart view = page.findView(RepositoryView.ID);
            if (view == null) {
                try {
                    view = page.showView(RepositoryView.ID);
                } catch (Exception e) {
                    ExceptionHandler.process(e);
                }
            }
            RepositoryView repositoryView = (RepositoryView) view;

            return repositoryView.getDoubleClickAction();
        }
        return null;
    }
}
