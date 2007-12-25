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
package org.talend.designer.core.ui;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackEvent;
import org.eclipse.gef.commands.CommandStackEventListener;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.epic.perleditor.PerlEditorPlugin;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.MessageBoxExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.core.CorePlugin;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.context.UpdateRunJobComponentContextHelper;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.context.JobContextManager;
import org.talend.core.model.process.IContextManager;
import org.talend.core.model.process.IContextParameter;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.properties.Property;
import org.talend.core.ui.IUIRefresher;
import org.talend.core.ui.images.ECoreImage;
import org.talend.designer.core.DesignerPlugin;
import org.talend.designer.core.ISyntaxCheckableEditor;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.ui.editor.CodeEditorFactory;
import org.talend.designer.core.ui.editor.ProcessEditorInput;
import org.talend.designer.core.ui.editor.TalendEditor;
import org.talend.designer.core.ui.editor.TalendJavaEditor;
import org.talend.designer.core.ui.editor.TalendPerlEditor;
import org.talend.designer.core.ui.editor.TalendTabbedPropertySheetPage;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.nodes.NodeLabel;
import org.talend.designer.core.ui.editor.nodes.NodeLabelEditPart;
import org.talend.designer.core.ui.editor.nodes.NodePart;
import org.talend.designer.core.ui.editor.outline.NodeTreeEditPart;
import org.talend.designer.core.ui.editor.process.Process;
import org.talend.designer.core.ui.editor.process.ProcessPart;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.ProcessorException;
import org.talend.designer.runprocess.ProcessorUtilities;
import org.talend.repository.RepositoryPlugin;
import org.talend.repository.job.deletion.JobResourceManager;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.IRepositoryService;
import org.talend.repository.ui.views.IRepositoryView;

/**
 * This class is the main editor, the differents pages in it are: <br/><b>1)</b> {@link TalendEditor} <br/><b>2)</b>
 * {@link Text Editor on the generated code} <br/><br/> This class uses the interface ISelectionListener, it allows to
 * propage the Delete evenement to the designer. <br/>
 * 
 * $Id$
 * 
 */
public class MultiPageTalendEditor extends MultiPageEditorPart implements IResourceChangeListener, ISelectionListener,
        IUIRefresher {

    private final AdapterImpl dirtyListener = new AdapterImpl() {

        @Override
        public void notifyChanged(Notification notification) {
            if (notification.getEventType() != Notification.REMOVING_ADAPTER) {
                propertyIsDirty = true;
                getTalendEditor().getProperty().eAdapters().remove(dirtyListener);
                process.updateProperties();
                getTalendEditor().getProperty().eAdapters().add(dirtyListener);
                firePropertyChange(IEditorPart.PROP_DIRTY);
            }
        }
    };

    private boolean propertyIsDirty = false;

    public static final String ID = "org.talend.designer.core.ui.MultiPageTalendEditor"; //$NON-NLS-1$

    private final TalendEditor designerEditor = new TalendEditor();;

    private AbstractDecoratedTextEditor codeEditor;

    private Process process;

    private IProcessor processor;

    private String oldJobName;

    private boolean keepPropertyLocked; // used only if the user try to open more than one editor at a time.

    private boolean codeSync = false;

    public MultiPageTalendEditor() {
        super();

        if (CorePlugin.getDefault().getRepositoryService().needSetPartListener()) {

            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(
                    CorePlugin.getDefault().getDesignerCoreService().getActiveProcessTracker());
            CorePlugin.getDefault().getRepositoryService().setPartListener(false);

            Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                    try {
                        CorePlugin.getDefault().getCodeGeneratorService().createRoutineSynchronizer().syncAllRoutines();
                    } catch (Exception e) {
                        ExceptionHandler.process(e);
                    }
                }
            });

        }
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    public void setReadOnly(boolean readonly) {
        designerEditor.setReadOnly(readonly);
    }

    /**
     * Creates page 0 of the multi-page editor, which contains a text editor.
     */
    void createPage0() {
        try {
            int index = addPage(designerEditor, getEditorInput());
            setPageText(index, Messages.getString("MultiPageTalendEditor.Designer")); //$NON-NLS-1$
            designerEditor.setParent(this);
        } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(), Messages.getString("MultiPageTalendEditor.Designer.Error"), //$NON-NLS-1$
                    null, e.getStatus());
        }
    }

    public TalendEditor getTalendEditor() {
        return designerEditor;
    }

    /**
     * Creates page 1 of the multi-page editor, which allows you to change the font used in page 2.
     */
    void createPage1() {
        codeEditor = CodeEditorFactory.getInstance().getCodeEditor(getCurrentLang());
        process = designerEditor.getProcess();
        process.setEditor(this);
        processor = ProcessorUtilities.getProcessor(process, process.getContextManager().getDefaultContext());

        process.setProcessor(processor);
        if (processor.getProcessorType().equals("javaProcessor")) { //$NON-NLS-1$
            processor.setProcessorStates(IProcessor.STATES_EDIT);
            if (codeEditor instanceof ISyntaxCheckableEditor) {
                processor.setSyntaxCheckableEditor((ISyntaxCheckableEditor) codeEditor);
            }
        }
        if (codeEditor instanceof TalendJavaEditor) {
            ((TalendJavaEditor) codeEditor).addEditorPart(this);
        }

        try {
            int index = addPage(codeEditor, createFileEditorInput());

            // init Syntax Validation.
            if (getCurrentLang() == ECodeLanguage.PERL) {
                PerlEditorPlugin.getDefault().setSyntaxValidationPreference(true);
            }

            setPageText(index, Messages.getString("MultiPageTalendEditor.Code")); //$NON-NLS-1$
        } catch (PartInitException pie) {
            ErrorDialog.openError(getSite().getShell(), Messages.getString("MultiPageTalendEditor.Designer.Error"), //$NON-NLS-1$
                    null, pie.getStatus());
        }
        if (process.getGeneratingNodes().size() != 0) {
            Job job = new Job("Generating code") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    ProcessorUtilities.generateCode(process, process.getContextManager().getDefaultContext(), false, false, true,
                            ProcessorUtilities.GENERATE_WITH_FIRST_CHILD);

                    return Status.OK_STATUS;
                }
            };
            job.setUser(true);
            job.setPriority(Job.BUILD);
            job.schedule(); // start as soon as possible
            codeSync = true;
        }

        CommandStackEventListener commandStackEventListener = new CommandStackEventListener() {

            public void stackChanged(CommandStackEvent event) {
                codeSync = false;
            }
        };
        CommandStack commandStack = (CommandStack) designerEditor.getAdapter(CommandStack.class);
        commandStack.addCommandStackEventListener(commandStackEventListener);

    }

    /**
     * get the current project generating code language.
     * 
     * @return the current generating code language
     */
    private ECodeLanguage getCurrentLang() {
        return ((RepositoryContext) CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY)).getProject()
                .getLanguage();
    }

    /**
     * Creates the pages of the multi-page editor.
     */
    @Override
    protected void createPages() {
        setTitleImage(ImageProvider.getImage(ECoreImage.PROCESS_ICON));
        createPage0();
        createPage1();
    }

    /**
     * The <code>MultiPageEditorPart</code> implementation of this <code>IWorkbenchPart</code> method disposes all
     * nested editors. Subclasses may extend.
     */
    @Override
    public void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        if (this.codeEditor instanceof TalendJavaEditor) {
            ((TalendJavaEditor) codeEditor).removeEditorPart(this);
        }

        // MultieditPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(
        // (org.eclipse.jface.util.IPropertyChangeListener) this);
        super.dispose();

        if (isKeepPropertyLocked()) {
            return;
        }

        // Unlock the process :
        IRepositoryService service = DesignerPlugin.getDefault().getRepositoryService();
        IProxyRepositoryFactory repFactory = service.getProxyRepositoryFactory();
        try {
            getTalendEditor().getProperty().eAdapters().remove(dirtyListener);
            Property property = repFactory.reload(getTalendEditor().getProperty());
            getTalendEditor().setProperty(property);
            repFactory.unlock(property.getItem());
        } catch (PersistenceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        IRepositoryView viewPart = (IRepositoryView) getSite().getPage().findView(IRepositoryView.VIEW_ID);
        if (viewPart != null) {
            viewPart.refresh();
        }
        if (process != null) {
            process.setEditor(null);
        }
    }

    /**
     * Saves the multi-page editor's document.
     */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        if (!isDirty()) {
            return;
        }
        updateRunJobContext();
        getTalendEditor().getProperty().eAdapters().remove(dirtyListener);
        getEditor(0).doSave(monitor);
        getTalendEditor().getProperty().eAdapters().add(dirtyListener);
        codeSync();

        propertyIsDirty = false;
        firePropertyChange(IEditorPart.PROP_DIRTY);

    }

    private void updateRunJobContext() {
        JobContextManager manager = (JobContextManager) getProcess().getContextManager();
        if (!manager.isModified()) {
            return;
        }
        Map<String, String> nameMap = manager.getNameMap();
        try {
            IProxyRepositoryFactory factory = DesignerPlugin.getDefault().getProxyRepositoryFactory();

            Set<String> curContextVars = getCurrentContextVariables(manager);
            String jobName = getProcess().getLabel();

            UpdateRunJobComponentContextHelper.updateItemRunJobComponentReference(factory, nameMap, jobName, curContextVars);

            IEditorReference[] reference = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                    .getEditorReferences();
            List<IProcess> processes = RepositoryPlugin.getDefault().getDesignerCoreService().getOpenedProcess(reference);

            UpdateRunJobComponentContextHelper.updateOpenedJobRunJobComponentReference(processes, nameMap, jobName,
                    curContextVars);
        } catch (PersistenceException e) {
            e.printStackTrace();
        }
        // clear the flags
        nameMap.clear();
        manager.setModified(false);
    }

    private Set<String> getCurrentContextVariables(IContextManager manager) {
        Set<String> varNameSet = new HashSet<String>();
        if (manager != null) {
            for (IContextParameter param : manager.getDefaultContext().getContextParameterList()) {
                varNameSet.add(param.getName());
            }
        }
        return varNameSet;
    }

    public void codeSync() {
        if (!codeSync && process.getGeneratingNodes().size() != 0) {
            try {
                processor.generateCode(false, false, true);

            } catch (ProcessorException pe) {
                MessageBoxExceptionHandler.process(pe);
            }
            codeSync = true;
        }
    }

    /**
     * Saves the multi-page editor's document as another file. Also updates the text for page 0's tab, and updates this
     * multi-page editor's input to correspond to the nested editor's.
     */
    @Override
    public void doSaveAs() {
        IEditorPart editor = getEditor(0);
        editor.doSaveAs();
        setPageText(0, editor.getTitle());
        setInput(editor.getEditorInput());
    }

    /*
     * (non-Javadoc) Method declared on IEditorPart
     */
    public void gotoMarker(final IMarker marker) {
        setActivePage(0);
    }

    /**
     * The <code>MultiPageEditorExample</code> implementation of this method checks that the input is an instance of
     * <code>IFileEditorInput</code>.
     */
    @Override
    public void init(final IEditorSite site, final IEditorInput editorInput) throws PartInitException {
        if (!(editorInput instanceof IFileEditorInput) && !(editorInput instanceof ProcessEditorInput)) {
            throw new PartInitException(Messages.getString("MultiPageTalendEditor.InvalidInput")); //$NON-NLS-1$
        }
        setSite(site);
        setInput(editorInput);
        site.setSelectionProvider(new MultiPageTalendSelectionProvider(this));
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);

        // Lock the process :
        IRepositoryService service = DesignerPlugin.getDefault().getRepositoryService();
        IProxyRepositoryFactory repFactory = service.getProxyRepositoryFactory();
        ProcessEditorInput processEditorInput = (ProcessEditorInput) editorInput;
        Process currentProcess = (processEditorInput).getLoadedProcess();
        if (!currentProcess.isReadOnly()) {
            try {
                processEditorInput.getItem().getProperty().eAdapters().add(dirtyListener);
                repFactory.lock(currentProcess);
            } catch (PersistenceException e) {
                e.printStackTrace();
            } catch (BusinessException e) {
                // Nothing to do
            }
        } else {
            setReadOnly(true);
        }
    }

    @Override
    public boolean isDirty() {
        return propertyIsDirty || super.isDirty();
    }

    /*
     * (non-Javadoc) Method declared on IEditorPart.
     */
    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
     * Calculates the contents of page 2 when the it is activated.
     */
    @Override
    protected void pageChange(final int newPageIndex) {
        super.pageChange(newPageIndex);
        setName();
        if (newPageIndex == 1) {
            if (codeEditor instanceof ISyntaxCheckableEditor) {
                moveCursorToSelectedComponent();

                /*
                 * Belowing method had been called at line 331 within the generateCode method, as soon as code
                 * generated.
                 */
                // ((ISyntaxCheckableEditor) codeEditor).validateSyntax();
            }
            codeSync();
        }
    }

    public void showDesignerPage() {
        setActivePage(0);
    }

    public void showCodePage() {
        setActivePage(1);
    }

    /**
     * DOC smallet Comment method "setName".
     * 
     * @param label
     */
    public void setName() {
        String label = getEditorInput().getName();
        oldJobName = label;
        // if (getActivePage() == 1) {
        setPartName(Messages.getString("MultiPageTalendEditor.Job", label)); //$NON-NLS-1$
        // } else {
        // setPartName(Messages.getString("other Label??", label));
        // //$NON-NLS-1$
        // }
    }

    /**
     * Move Cursor to Selected Node.
     * 
     * @param processor
     */
    private void moveCursorToSelectedComponent() {
        String nodeName = getSelectedNodeName();
        if (nodeName != null) {
            if (codeEditor instanceof TalendJavaEditor) {
                ((TalendJavaEditor) codeEditor).placeCursorTo(nodeName); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                ((TalendPerlEditor) codeEditor).placeCursorTo(nodeName); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    /**
     * Get the selected Node if any.
     * 
     * @return the component selected name or null if component is not found or is not activated
     */
    public String getSelectedNodeName() {
        String nodeName = null;
        Node node = getSelectedGraphicNode();
        if (node != null) {
            if (node.isActivate() || node.isDummy()) {
                nodeName = node.getUniqueName();
            } else {
                nodeName = null;

            }
            if (node.getComponent().getMultipleComponentManager() != null) {
                nodeName += "_" + node.getComponent().getMultipleComponentManager().getInput().getName(); //$NON-NLS-1$
            }
        }
        return nodeName;
    }

    /**
     * DOC amaumont Comment method "getSelectedNode".
     * 
     * @return
     */
    public Node getSelectedGraphicNode() {
        Node node = null;
        List selections = designerEditor.getViewer().getSelectedEditParts();
        if (selections.size() == 1) {
            Object selection = selections.get(0);

            if (selection instanceof NodeTreeEditPart) {
                NodeTreeEditPart nTreePart = (NodeTreeEditPart) selection;
                node = (Node) nTreePart.getModel();
            } else {
                if (selection instanceof NodePart) {
                    NodePart editPart = (NodePart) selection;
                    node = (Node) editPart.getModel();
                } else if (selection instanceof NodeLabelEditPart) {
                    NodeLabelEditPart editPart = (NodeLabelEditPart) selection;
                    node = ((NodeLabel) editPart.getModel()).getNode();
                }
            }
        }
        return node;
    }

    public EditPart getOldSelection() {
        IPropertySheetPage propertyPage = (IPropertySheetPage) designerEditor.getAdapter(IPropertySheetPage.class);
        if (propertyPage instanceof TalendTabbedPropertySheetPage) {
            StructuredSelection selections = ((TalendTabbedPropertySheetPage) propertyPage).getOldSelection();
            if (selections != null) {
                Object selection = selections.getFirstElement();
                if (selection instanceof EditPart) {
                    return (EditPart) selection;
                }
            }
        }
        return null;
    }

    /**
     * Closes all project files on project close.
     */

    public void resourceChanged(final IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
            Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                    IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
                    for (int i = 0; i < pages.length; i++) {
                        if (((FileEditorInput) designerEditor.getEditorInput()).getFile().getProject()
                                .equals(event.getResource())) {
                            IEditorPart editorPart = pages[i].findEditor(designerEditor.getEditorInput());
                            pages[i].closeEditor(editorPart, true);
                        }
                    }
                }
            });
        }
    }

    @Override
    public Object getAdapter(final Class adapter) {
        if (designerEditor.equals(getActiveEditor())) {
            return this.getActiveEditor().getAdapter(adapter);
        }
        /*
         * if (textEditor.equals(getActiveEditor())) { if (adapter == IPropertySheetPage.class) { return null; } return
         * this.getActiveEditor().getAdapter(adapter); }
         */
        return super.getAdapter(adapter);
    }

    /**
     * Will allow to propagate the Delete evenement in the designer.
     */
    public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
        if (this.equals(getSite().getPage().getActiveEditor())) {
            if (selection instanceof StructuredSelection) {
                StructuredSelection structSel = (StructuredSelection) selection;
                if (structSel.getFirstElement() instanceof EditPart) {
                    if (designerEditor.equals(getActiveEditor())) {
                        designerEditor.selectionChanged(getActiveEditor(), selection);

                    }
                }
            }
        }
    }

    /**
     * Getter for codeEditor.
     * 
     * @return the codeEditor
     */
    public TalendJavaEditor getCodeEditor() {
        return (TalendJavaEditor) this.codeEditor;
    }

    private FileEditorInput createFileEditorInput() {

        IPath codePath = processor.getCodePath();

        if (codePath.isEmpty()) {
            // reinitialize the processor if there was any problem during the initialization.
            // (should not happen)
            try {
                processor.initPath();
            } catch (ProcessorException e) {
                MessageBoxExceptionHandler.process(e);
            }
            codePath = processor.getCodePath();
        }

        IFile codeFile = ResourcesPlugin.getWorkspace().getRoot().getFile(
                processor.getCodeProject().getFullPath().append(codePath));
        if (!codeFile.exists()) {
            // Create empty one
            try {
                codeFile.create(new ByteArrayInputStream("".getBytes()), true, null); //$NON-NLS-1$
            } catch (CoreException e) {
                // Do nothing.
            }
        }
        return new FileEditorInput(codeFile);
    }

    /**
     * Getter for process.
     * 
     * @return the process
     */
    public IProcess getProcess() {
        return this.process;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.editor.INameRefresher#refreshName()
     */
    public void refreshName() {
        try {
            JobResourceManager jobResourceManager = JobResourceManager.getInstance();
            jobResourceManager.removeProtection(designerEditor);
            for (String id : designerEditor.getProtectedIds()) {
                if (designerEditor.getJobResource(id).getJobName().equalsIgnoreCase(oldJobName)) {
                    // delete only the job renamed
                    jobResourceManager.deleteResource(designerEditor.getJobResource(id));
                }
            }
            designerEditor.resetJobResources();

            setName();
            designerEditor.getCurrentJobResource().setJobName(getEditorInput().getName());
            jobResourceManager.addProtection(designerEditor);

            processor.initPath();
            processor.setProcessorStates(IProcessor.STATES_EDIT);

            FileEditorInput input = createFileEditorInput();
            codeEditor.setInput(input);

            IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (activeWorkbenchWindow != null) {
                if (activeWorkbenchWindow.getActivePage().isPartVisible(this)) {
                    new ActiveProcessTracker().partBroughtToTop(this);
                    DesignerPlugin.getDefault().getRunProcessService().refreshView();
                }
            }

        } catch (Exception e) {
            MessageBoxExceptionHandler.process(e);
        }
    }

    public void updateChildrens() {
        // just call the method add protection will update new childrens and
        // keep old ones (keep to delete automatically
        // when closing job)
        JobResourceManager jobResourceManager = JobResourceManager.getInstance();
        jobResourceManager.addProtection(designerEditor);
    }

    /**
     * DOC bqian Comment method "selectNode".
     * 
     * @param node
     */
    public void selectNode(Node node) {
        GraphicalViewer viewer = getTalendEditor().getViewer();
        Object object = viewer.getRootEditPart().getChildren().get(0);
        if (object instanceof ProcessPart) {
            for (EditPart editPart : (List<EditPart>) ((ProcessPart) object).getChildren()) {
                if (editPart instanceof NodePart) {
                    if (((NodePart) editPart).getModel().equals(node)) {
                        viewer.select(editPart);
                    }
                }
            }
        }
    }

    public boolean isJobAlreadyOpened() {
        return foundExistEditor(this.getEditorInput());
    }

    private boolean foundExistEditor(final IEditorInput editorInput) {
        IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow != null) {

            WorkbenchPage page = (WorkbenchPage) activeWorkbenchWindow.getActivePage();
            if (page != null) {
                int i = 0;
                if (editorInput instanceof ProcessEditorInput) {
                    ProcessEditorInput curEditorInput = (ProcessEditorInput) editorInput;

                    IEditorReference[] ref = page.findEditors(curEditorInput, ID, IWorkbenchPage.MATCH_INPUT);
                    boolean exist = ref.length > 1;
                    if (exist) {
                        // MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                        // "New Editor Error!", " It's not possible to open another editor of current job. ");
                        IEditorPart activePart = page.getActiveEditor();
                        // activePart.removePropertyListener(listener);
                        // page.getEditorPresentation().closeEditor(activePart);

                    }
                    return exist;
                }
            }

        }
        return false;
    }

    /**
     * Getter for keepPropertyLocked.
     * 
     * @return the keepPropertyLocked
     */
    public boolean isKeepPropertyLocked() {
        return this.keepPropertyLocked;
    }

    /**
     * Sets the keepPropertyLocked.
     * 
     * @param keepPropertyLocked the keepPropertyLocked to set
     */
    public void setKeepPropertyLocked(boolean keepPropertyLocked) {
        this.keepPropertyLocked = keepPropertyLocked;
    }

}
