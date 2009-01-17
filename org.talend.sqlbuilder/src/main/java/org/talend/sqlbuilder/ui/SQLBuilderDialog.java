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
package org.talend.sqlbuilder.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitorWithBlocking;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.talend.core.CorePlugin;
import org.talend.core.model.metadata.builder.connection.Query;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.model.update.RepositoryUpdateManager;
import org.talend.repository.IRepositoryChangedListener;
import org.talend.repository.RepositoryChangedEvent;
import org.talend.repository.RepositoryElementDelta;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNode.EProperties;
import org.talend.sqlbuilder.Messages;
import org.talend.sqlbuilder.SqlBuilderPlugin;
import org.talend.sqlbuilder.dbstructure.RepositoryNodeType;
import org.talend.sqlbuilder.dbstructure.SessionTreeNodeManager;
import org.talend.sqlbuilder.dbstructure.SessionTreeNodeUtils;
import org.talend.sqlbuilder.dbstructure.DBTreeProvider.MetadataTableRepositoryObject;
import org.talend.sqlbuilder.dbstructure.nodes.INode;
import org.talend.sqlbuilder.editors.MultiPageSqlBuilderEditor;
import org.talend.sqlbuilder.repository.utility.SQLBuilderRepositoryNodeManager;
import org.talend.sqlbuilder.util.ConnectionParameters;
import org.talend.sqlbuilder.util.ImageUtil;
import org.talend.sqlbuilder.util.TextUtil;
import org.talend.sqlbuilder.util.UIUtils;

/**
 * This Dialog is used for building sql.
 * 
 * $Id: SQLBuilderDialog.java,v 1.44 2006/11/09 08:44:09 tangfn Exp $
 * 
 */
public class SQLBuilderDialog extends Dialog implements ISQLBuilderDialog, IRepositoryChangedListener {

    // ends
    private boolean isFromRepositoryView = false;

    private DBDetailsComposite dbDetailsComposite;

    private DBStructureComposite structureComposite;

    private SQLBuilderTabComposite editorComposite;

    // Added by Tang Fengneng
    private ConnectionParameters connParameters;

    // Ends

    /**
     * The progress indicator control.
     */
    protected ProgressIndicator progressIndicator;

    /**
     * The progress monitor.
     */
    private ProgressMonitor progressMonitor = new ProgressMonitor();

    /**
     * SessionTreeNode Manager.
     */
    SessionTreeNodeManager nodeManager = new SessionTreeNodeManager();

    SQLBuilderRepositoryNodeManager manager = new SQLBuilderRepositoryNodeManager();

    /**
     * Internal progress monitor implementation.
     */
    private class ProgressMonitor implements IProgressMonitorWithBlocking {

        private boolean fIsCanceled;

        protected boolean forked = false;

        protected boolean locked = false;

        public void beginTask(String name, int totalWork) {
            if (progressIndicator == null) {
                return;
            }
            if (progressIndicator.isDisposed()) {
                return;
            }
            if (totalWork == UNKNOWN) {
                progressIndicator.beginAnimatedTask();
            } else {
                progressIndicator.beginTask(totalWork);
            }
        }

        public void done() {
            if (progressIndicator == null) {
                return;
            }
            if (!progressIndicator.isDisposed()) {
                progressIndicator.sendRemainingWork();
                progressIndicator.done();
            }
        }

        public void setTaskName(String name) {
        }

        public boolean isCanceled() {
            return fIsCanceled;
        }

        public void setCanceled(boolean b) {
            fIsCanceled = b;
            if (locked) {
                clearBlocked();
            }
        }

        public void subTask(String name) {
        }

        public void worked(int work) {
            internalWorked(work);
        }

        public void internalWorked(double work) {
            if (!progressIndicator.isDisposed()) {
                progressIndicator.worked(work);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.core.runtime.IProgressMonitorWithBlocking#clearBlocked()
         */
        public void clearBlocked() {
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.core.runtime.IProgressMonitorWithBlocking#setBlocked(org.eclipse.core.runtime.IStatus)
         */
        public void setBlocked(IStatus reason) {
            locked = true;
        }
    }

    private String dialogTitle;

    /**
     * Create the dialog.
     * 
     * @param parentShell
     */
    public SQLBuilderDialog(Shell parentShell, String title) {
        this(parentShell);
    }

    public SQLBuilderDialog(Shell parentShell) {
        super(parentShell);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.RESIZE | SWT.MIN | SWT.MAX | SWT.APPLICATION_MODAL);
        parentShell.setImage(ImageUtil.getImage("Images.title")); //$NON-NLS-1$
        SqlBuilderPlugin.getDefault().getRepositoryService().registerRepositoryChangedListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    public void configureShell(Shell shell) {
        super.configureShell(shell);
        // Set the title bar text
        shell.setText(TextUtil.getDialogTitle());
    }

    /**
     * Create contents of the dialog.
     * 
     * @param parent
     */
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        // container.setLayout(new GridLayout());

        final SashForm mainSashForm = new SashForm(container, SWT.NONE | SWT.VERTICAL);
        mainSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final SashForm sashFormStructureAndEditor = new SashForm(mainSashForm, SWT.NONE);

        final SashForm sashFormResultAndDetail = new SashForm(mainSashForm, SWT.NONE);
        mainSashForm.setWeights(new int[] { 3, 1 });

        createResult(sashFormResultAndDetail);
        createDetail(sashFormResultAndDetail);
        sashFormResultAndDetail.setWeights(new int[] { 4, 3 });

        createDatabaseStructure(sashFormStructureAndEditor);
        createSQLEditor(sashFormStructureAndEditor);
        sashFormStructureAndEditor.setWeights(new int[] { 4, 6 });

        if (connParameters.isFromRepository() && connParameters.getQueryObject() != null) {
            structureComposite.openNewQueryEditor();
        } else if (connParameters.isFromRepository() && connParameters.getMetadataTable() != null) {
            structureComposite.openNewTableEditor();
        } else {
            structureComposite.openNewEditor();
        }

        // RefreshDetailCompositeAction refreshAction =
        new RefreshDetailCompositeAction(structureComposite.getTreeViewer());

        return container;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.sqlbuilder.ui.ISQLBuilderDialog#openEditor(org.talend.repository.model.RepositoryNode,
     * java.util.List, org.talend.sqlbuilder.util.ConnectionParameters, boolean)
     */
    public void openEditor(RepositoryNode node, List<String> repositoryName, ConnectionParameters connParam,
            boolean isDefaultEditor) {
        editorComposite.openNewEditor(node, repositoryName, connParam, isDefaultEditor);
    }

    /**
     * Creates the sql detail composite.
     * 
     * @param sashFormResultAndDetail
     */
    private void createDetail(SashForm sashFormResultAndDetail) {
        dbDetailsComposite = new DBDetailsComposite(sashFormResultAndDetail, SWT.BORDER);
    }

    /**
     * Creates the composite to display sql execution result.
     * 
     * @param sashFormResultAndDetail
     */
    private void createResult(SashForm sashFormResultAndDetail) {
        // SQLResultComposite resultView =
        new SQLResultComposite(sashFormResultAndDetail, SWT.BORDER);

    }

    /**
     * Creates the sql editor composite.
     * 
     * @param sashFormStructureAndEditor
     */
    private void createSQLEditor(SashForm sashFormStructureAndEditor) {

        editorComposite = new SQLBuilderTabComposite(sashFormStructureAndEditor, SWT.BORDER, this);
    }

    /**
     * Creates composite to display database structure.
     * 
     * @param sashFormStructureAndEditor
     */
    private void createDatabaseStructure(SashForm sashFormStructureAndEditor) {
        // if (connParameters.getMetadataTable() == null || (connParameters.isRepository() &&
        // connParameters.isSchemaRepository())) {
        // structureComposite = new DBStructureComposite(sashFormStructureAndEditor, SWT.BORDER, this);
        // } else {
        BuildInDBStructure buildInDBStructure = new BuildInDBStructure(sashFormStructureAndEditor, SWT.NONE | SWT.VERTICAL, this,
                connParameters);
        structureComposite = buildInDBStructure.getDbstructureCom();
        // }
        structureComposite.setProgressMonitor(this.getProgressMonitor());
    }

    /**
     * Create contents of the button bar.
     * 
     * @param parent
     */
    protected void createButtonsForButtonBar(Composite parent) {
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        parent.setLayoutData(data);

        // increment the number of columns in the button bar
        GridLayout layout = (GridLayout) parent.getLayout();
        layout.makeColumnsEqualWidth = false;
        layout.numColumns = 4;

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        progressIndicator = new ProgressIndicator(parent);
        progressIndicator.setLayoutData(gd);

        gd = new GridData();
        gd.widthHint = 200;
        Label l = new Label(parent, SWT.NONE);
        l.setLayoutData(gd);

        // OK and Cancel buttons
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);

        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(label);
        button.setFont(JFaceResources.getDialogFont());
        button.setData(new Integer(id));
        button.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                buttonPressed(((Integer) event.widget.getData()).intValue());
            }
        });
        if (defaultButton) {
            Shell shell = parent.getShell();
            if (shell != null) {
                shell.setDefaultButton(button);
            }
        }
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END);
        int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
        Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
        data.widthHint = Math.max(widthHint, minSize.x);
        button.setLayoutData(data);
        return button;
    }

    /**
     * Return the initial size of the dialog.
     */
    protected Point getInitialSize() {
        return new Point(800, 600);
    }

    public SQLBuilderTabComposite getEditorComposite() {
        return editorComposite;
    }

    @Override
    public boolean close() {
        SqlBuilderPlugin.getDefault().getRepositoryService().removeRepositoryChangedListener(this);

        clean();
        SQLBuilderRepositoryNodeManager.removeAllRepositoryNodes();
        return super.close();
    }

    private void clean() {
        SessionTreeNodeUtils.dispose();
        nodeManager.clear();
    }

    /**
     * Returns the progress monitor to use for operations run in this progress dialog.
     * 
     * @return the progress monitor
     */
    public IProgressMonitor getProgressMonitor() {
        return progressMonitor;
    }

    /**
     * Added by Tang Fengneng Sets the connParameters.
     * 
     * @param connParameters the connParameters to set
     */
    public void setConnParameters(ConnectionParameters connParameters) {
        this.connParameters = connParameters;
    }

    public ConnectionParameters getConnParameters() {
        return connParameters;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    public void okPressed() {
        // gain the contextmode from sqlbuilder,and set it in connParameters,add by hyWang
        MultiPageSqlBuilderEditor editor = null;
        CTabFolder folder = getEditorComposite().getTabFolder();
        CTabItem[] a = folder.getItems();
        for (int i = 0; i < a.length; i++) {
            CTabItem itm = a[i];
            Object obj = itm.getData("KEY"); //$NON-NLS-1$
            if (obj instanceof MultiPageSqlBuilderEditor) {
                editor = (MultiPageSqlBuilderEditor) obj;
            }
            if (editor != null) {
                if (itm.getData() instanceof Query) {
                    Query q = (Query) itm.getData();
                    connParameters.setIfContextButtonCheckedFromBuiltIn(q.isContextMode());
                }

            }
        }

        if (EParameterFieldType.DBTABLE.equals(connParameters.getFieldType())) {
            final IStructuredSelection selection = (IStructuredSelection) structureComposite.getTreeViewer().getSelection();
            final Object firstElement = selection.getFirstElement();
            if (firstElement instanceof RepositoryNode) {
                RepositoryNode node = (RepositoryNode) firstElement;
                boolean is = node.getProperties(EProperties.CONTENT_TYPE).equals(RepositoryNodeType.TABLE);
                if (is) {
                    MetadataTableRepositoryObject object = (MetadataTableRepositoryObject) node.getObject();
                    connParameters.setSelectDBTable(object.getSourceName());
                }
            }
        } else {
            String sql = ""; //$NON-NLS-1$
            // sql = editorComposite.getDefaultTabSql();
            sql = editorComposite.getCurrentTabSql();
            // if (ConnectionParameters.isJavaProject()) {
            // sql = sql.replace("\"", "\\" + "\"");
            // } else {
            // sql = sql.replace("'", "\\'");
            // }

            // sql = QueryUtil.checkAndAddQuotes(sql);

            connParameters.setQuery(sql);

            if (connParameters.isFromRepository() && !connParameters.isNodeReadOnly()) {
                List<Query> qs = new ArrayList<Query>();
                boolean isInfo = false;
                final CTabFolder tabFolder = getEditorComposite().getTabFolder();
                final CTabItem[] items = tabFolder.getItems();

                for (int i = 0; i < items.length; i++) {
                    CTabItem item = items[i];
                    final String text = item.getText();
                    boolean isInfo2 = text.length() > 1 && text.substring(0, 1).equals("*"); //$NON-NLS-1$
                    if (isInfo2) {
                        isInfo = true;
                    }
                }
                if (isInfo) {
                    String title = Messages.getString("SQLBuilderDialog.SaveAllQueries.Title"); //$NON-NLS-1$
                    String info = Messages.getString("SQLBuilderDialog.SaveAllQueries.Info"); //$NON-NLS-1$
                    boolean openQuestion = MessageDialog.openQuestion(getShell(), title, info);
                    if (openQuestion) {
                        for (CTabItem item : items) {
                            final String text = item.getText();
                            boolean isInfo2 = text.length() > 1 && text.substring(0, 1).equals("*"); //$NON-NLS-1$
                            if (isInfo2) {
                                MultiPageSqlBuilderEditor meditor = null;
                                Object control = item.getData("KEY"); //$NON-NLS-1$
                                if (control instanceof MultiPageSqlBuilderEditor) {
                                    meditor = (MultiPageSqlBuilderEditor) control;
                                }
                                if (meditor != null) {
                                    RepositoryNode node = null;
                                    node = meditor.getActivePageRepositoryNode();
                                    if (text.substring(1).startsWith(AbstractSQLEditorComposite.QUERY_PREFIX)) {
                                        if (item.getData() instanceof Query) {
                                            Query q = (Query) item.getData();
                                            q.setValue(meditor.getActivePageSqlString());
                                            // add by hyWang
                                            q.setContextMode(meditor.getActiveEditors().getContextmode().getContextmodeaction()
                                                    .isChecked());
                                            qs.add(q);
                                            if (node != null && q != null) {
                                                manager.saveQuery(node, q, null);
                                            }
                                        }
                                    } else {
                                        meditor.getActivePageSaveAsSQLAction().run();
                                    }
                                }
                            }
                        }
                    }
                }
                if (connParameters.getQueryObject() != null) {
                    RepositoryUpdateManager.updateQuery(connParameters.getQueryObject().getQueries());
                }
            }
        }

        //

        super.okPressed();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.sqlbuilder.ui.ISQLBuilderDialog#refreshNode(org.talend.repository.model.RepositoryNode)
     */
    public void refreshNode(RepositoryNode node) {
        structureComposite.doRefresh(node);
    }

    /**
     * qianbing class global comment. Refreshes Detail Composite according to selection changing of the database
     * structure viewer. <br/>
     * 
     * $Id: talend-code-templates.xml,v 1.3 2006/11/01 05:38:28 nicolas Exp $
     * 
     */
    public class RefreshDetailCompositeAction extends SelectionProviderAction {

        /**
         * qianbing RefreshDetailCompositeAction constructor comment.
         * 
         * @param provider
         */
        public RefreshDetailCompositeAction(ISelectionProvider provider) {
            super(provider, "Refresh DetailComposite"); //$NON-NLS-1$
        }

        /*
         * (non-Java)
         * 
         * @see
         * org.eclipse.ui.actions.SelectionProviderAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection
         * )
         */
        public void selectionChanged(final IStructuredSelection selection) {
            IRunnableWithProgress progress = new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("", IProgressMonitor.UNKNOWN); //$NON-NLS-1$

                    try {
                        INode node = null;
                        String msg = null;
                        if (!selection.isEmpty()) {
                            try {
                                final RepositoryNode repositoryNode = (RepositoryNode) selection.getFirstElement();
                                if (SQLBuilderRepositoryNodeManager.getRepositoryType(repositoryNode) == RepositoryNodeType.FOLDER) {
                                    return;
                                }
                                node = nodeManager.convert2INode(repositoryNode);
                            } catch (Exception e) {
                                msg = e.getMessage();
                                SqlBuilderPlugin.log(msg, e);
                            }
                            final INode argNode = node;
                            final String argMsg = msg;
                            Display.getDefault().asyncExec(new Runnable() {

                                public void run() {
                                    dbDetailsComposite.setSelectedNode(argNode, argMsg);
                                }
                            });
                        }
                    } finally {
                        monitor.done();
                    }
                }
            };

            UIUtils.runWithProgress(progress, true, getProgressMonitor(), getShell());

        }
    }

    public boolean isFromRepositoryView() {
        return this.isFromRepositoryView;
    }

    public void setFromRepositoryView(boolean isFromRepositoryView) {
        this.isFromRepositoryView = isFromRepositoryView;
    }

    public void repositoryChanged(RepositoryChangedEvent event) {
        clean();
        if (structureComposite != null) {
            structureComposite.updateStructureView(event);
        }
        manager.synchronizeAllSqlEditors(this);
    }

    public void notifySQLBuilder(IRepositoryObject o) {
        CorePlugin.getDefault().getRepositoryService().removeRepositoryChangedListener(this);
        CorePlugin.getDefault().getRepositoryService().repositoryChanged(new RepositoryElementDelta(o));
        CorePlugin.getDefault().getRepositoryService().registerRepositoryChangedListener(this);
    }

    /*
     * (non-Java)
     * 
     * @see org.talend.sqlbuilder.ui.ISQLBuilderDialog#openEditor(org.talend.repository.model.RepositoryNode,
     * java.util.List, org.talend.sqlbuilder.util.ConnectionParameters, boolean, java.util.List)
     */
    public void openEditor(RepositoryNode node, List<String> repositoryName, ConnectionParameters connParam,
            boolean isDefaultEditor, List<RepositoryNode> nodeSel) {
        editorComposite.setNodesSel(nodeSel);
        editorComposite.openNewEditor(node, repositoryName, connParam, isDefaultEditor);

    }

    public DBStructureComposite getStructureComposite() {
        return this.structureComposite;
    }

}
