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
package org.talend.repository.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.map.MultiKeyMap;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.MessageBoxExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.image.EImage;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.core.CorePlugin;
import org.talend.core.model.components.ComponentUtilities;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.metadata.builder.connection.SubscriberTable;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.JobletProcessItem;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.model.repository.RepositoryManager;
import org.talend.designer.core.model.utils.emf.talendfile.NodeType;
import org.talend.expressionbuilder.ExpressionPersistance;
import org.talend.repository.ProjectManager;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.ERepositoryStatus;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.JobletReferenceBean;
import org.talend.repository.model.MetadataTableRepositoryObject;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryConstants;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNodeUtilities;
import org.talend.repository.model.RepositoryNode.ENodeType;
import org.talend.repository.model.RepositoryNode.EProperties;
import org.talend.repository.ui.actions.metadata.DeleteTableAction;
import org.talend.repository.ui.dialog.JobletReferenceDialog;

/**
 * Action used to delete object from repository. This action manages logical and physical deletions.<br/>
 * 
 * $Id$
 * 
 */
public class DeleteAction extends AContextualAction {

    private static DeleteAction singleton;

    private static final String DELETE_LOGICAL_TITLE = Messages.getString("DeleteAction.action.logicalTitle"); //$NON-NLS-1$

    private static final String DELETE_FOREVER_TITLE = Messages.getString("DeleteAction.action.foreverTitle"); //$NON-NLS-1$

    private static final String DELETE_LOGICAL_TOOLTIP = Messages.getString("DeleteAction.action.logicalToolTipText"); //$NON-NLS-1$

    private static final String DELETE_FOREVER_TOOLTIP = Messages.getString("DeleteAction.action.logicalToolTipText"); //$NON-NLS-1$

    public DeleteAction() {
        super();
        setId(ActionFactory.DELETE.getId());
        this.setImageDescriptor(ImageProvider.getImageDesc(EImage.DELETE_ICON));
        //        this.setActionDefinitionId("deleteItem"); //$NON-NLS-1$
        singleton = this;
    }

    public static DeleteAction getInstance() {
        return singleton;
    }

    @Override
    public void run() {
        ISelection selection = getSelection();
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();

        boolean needToUpdataPalette = false;
        Set<ERepositoryObjectType> types = new HashSet<ERepositoryObjectType>();
        for (Object obj : ((IStructuredSelection) selection).toArray()) {
            if (obj instanceof RepositoryNode) {
                RepositoryNode node = (RepositoryNode) obj;
                try {

                    if (isForbidNode(node)) {
                        continue;
                    }

                    if (node.getType() == ENodeType.REPOSITORY_ELEMENT) {
                        boolean needReturn = deleteElements(factory, node);
                        if (node.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.JOBLET) {
                            needToUpdataPalette = true;
                        }
                        if (needReturn) {
                            return;
                        }
                        types.add(node.getObjectType());
                    } else if (node.getType() == ENodeType.SIMPLE_FOLDER) {

                        types.add(node.getContentType());
                        // fixed for the documentation deleted
                        if (node.getContentType() == ERepositoryObjectType.PROCESS
                                || node.getContentType() == ERepositoryObjectType.JOBLET) {
                            types.add(ERepositoryObjectType.DOCUMENTATION);
                        }

                        deleteFolder(node, factory);
                    }
                } catch (PersistenceException e) {
                    MessageBoxExceptionHandler.process(e);
                } catch (BusinessException e) {
                    MessageBoxExceptionHandler.process(e);
                }
            }
        }
        if (needToUpdataPalette) {
            ComponentUtilities.updatePalette();
        }
        RepositoryManager.refreshDeletedNode(types);
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        for (IEditorReference editors : page.getEditorReferences()) {
           CorePlugin.getDefault().getDiagramModelService().refreshBusinessModel(editors);
        }
    }

    /**
     * DOC qwei Comment method "deleteFolder".
     */
    private void deleteFolder(final RepositoryNode node, final IProxyRepositoryFactory factory) {
        try {
            IRunnableWithProgress op = new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        monitor.beginTask("Delete Running", 100); //$NON-NLS-1$
                        IPath path = RepositoryNodeUtilities.getPath(node);
                        ERepositoryObjectType objectType = (ERepositoryObjectType) node.getProperties(EProperties.CONTENT_TYPE);
                        List<RepositoryNode> repositoryList = node.getChildren();
                        monitor.worked(10);
                        int taskTotal = repositoryList.size();
                        for (RepositoryNode repositoryNode : repositoryList) {
                            deleteRepositoryNode(repositoryNode, factory);
                            monitor.worked(1 * 100 / taskTotal);
                        }
                        factory.deleteFolder(objectType, path);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    monitor.done();
                }

            };
            PlatformUI.getWorkbench().getProgressService().run(true, true, op);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteRepositoryNode(RepositoryNode repositoryNode, IProxyRepositoryFactory factory)
            throws PersistenceException, BusinessException {
        // TODO Auto-generated method stub
        if (repositoryNode.getType() == ENodeType.SIMPLE_FOLDER) {
            IPath path = RepositoryNodeUtilities.getPath(repositoryNode);
            ERepositoryObjectType objectType = (ERepositoryObjectType) repositoryNode.getProperties(EProperties.CONTENT_TYPE);
            List<RepositoryNode> repositoryList = repositoryNode.getChildren();
            for (RepositoryNode repositoryNode2 : repositoryList) {
                deleteRepositoryNode(repositoryNode2, factory);
            }
            factory.deleteFolder(objectType, path);

        } else {
            IRepositoryObject objToDelete = repositoryNode.getObject();
            factory.deleteObjectLogical(objToDelete);
        }
    }

    /**
     * DOC qzhang Comment method "checkRepository".
     * 
     * @param factory
     * @param currentJobNode
     * @return
     */

    public static IEditorReference[] getEditors() {
        final List<IEditorReference> list = new ArrayList<IEditorReference>();
        Display.getDefault().syncExec(new Runnable() {

            public void run() {
                IEditorReference[] reference = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                        .getEditorReferences();
                list.addAll(Arrays.asList(reference));
            }
        });
        return list.toArray(new IEditorReference[0]);
    }

    private static boolean isOpenedItem(Item openedItem, MultiKeyMap openProcessMap) {
        if (openedItem == null) {
            return false;
        }
        Property property = openedItem.getProperty();
        return (openProcessMap.get(property.getId(), property.getLabel(), property.getVersion()) != null);
    }

    private static MultiKeyMap createOpenProcessMap(List<IProcess> openedProcessList) {
        MultiKeyMap map = new MultiKeyMap();
        if (openedProcessList != null) {
            for (IProcess process : openedProcessList) {
                map.put(process.getId(), process.getLabel(), process.getVersion(), process);
            }
        }
        return map;
    }

    public static List<JobletReferenceBean> checkRepositoryNodeFromProcess(IProxyRepositoryFactory factory,
            RepositoryNode currentJobNode) {
        IRepositoryObject object = currentJobNode.getObject();
        List<JobletReferenceBean> list = new ArrayList<JobletReferenceBean>();

        List<IProcess> openedProcessList = CorePlugin.getDefault().getDesignerCoreService().getOpenedProcess(getEditors());
        MultiKeyMap openProcessMap = createOpenProcessMap(openedProcessList);
        Item item = null;
        if (object != null) {
            Property property = object.getProperty();
            if (property != null) {
                item = property.getItem();

                String label = property.getLabel();
                String version = property.getVersion();

                // List<UpdateResult> resultList = new ArrayList<UpdateResult>();
                List<IRepositoryObject> processList = null;
                try {
                    processList = factory.getAll(ERepositoryObjectType.PROCESS, true);
                    if (processList == null) {
                        processList = new ArrayList<IRepositoryObject>();
                    }
                    List<IRepositoryObject> jobletList = factory.getAll(ERepositoryObjectType.JOBLET, true);
                    if (jobletList != null) {
                        processList.addAll(jobletList);
                    }
                } catch (PersistenceException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                EList nodesList = null;
                for (IRepositoryObject process : processList) {
                    // node = (EList) process.getGraphicalNodes();
                    Property property2 = process.getProperty();

                    boolean isDelete = factory.getStatus(process) == ERepositoryStatus.DELETED;
                    boolean isJob = true;

                    Item item2 = property2.getItem();
                    if (!isOpenedItem(item2, openProcessMap)) {
                        if (item2 instanceof ProcessItem) {
                            nodesList = ((ProcessItem) item2).getProcess().getNode();
                        } else if (item2 instanceof JobletProcessItem) {
                            nodesList = ((JobletProcessItem) item2).getJobletProcess().getNode();
                        }
                    }
                    if (nodesList != null) {
                        // isExtensionComponent(node);
                        for (Object object2 : nodesList) {
                            if (object2 instanceof NodeType) {
                                NodeType nodeType = (NodeType) object2;
                                nodeType.getElementParameter();
                                boolean equals = nodeType.getComponentName().equals(label)
                                        && nodeType.getComponentVersion().equals(version);
                                if (equals) {
                                    String path = item2.getState().getPath();

                                    boolean found = false;
                                    JobletReferenceBean bean = new JobletReferenceBean(property2.getLabel(), property2
                                            .getVersion(), path);
                                    bean.setJobFlag(isJob, isDelete);

                                    for (JobletReferenceBean b : list) {
                                        if (b.toString().equals(bean.toString())) {
                                            found = true;
                                            b.addNodeNum();
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        list.add(bean);
                                    }
                                }
                            }
                        }
                    }
                }
                for (IProcess openedProcess : openedProcessList) {
                    for (INode node : openedProcess.getGraphicalNodes()) {
                        boolean equals = node.getComponent().getName().equals(label)
                                && node.getComponent().getVersion().equals(version);

                        boolean isDelete = factory.getStatus(openedProcess) == ERepositoryStatus.DELETED;
                        boolean isJob = true;
                        Property property2 = openedProcess.getProperty();
                        Item item2 = property2.getItem();
                        String path = item2.getState().getPath();

                        if (equals) {

                            boolean found = false;
                            JobletReferenceBean bean = new JobletReferenceBean(property2.getLabel(), property2.getVersion(), path);
                            bean.setJobFlag(isJob, isDelete);

                            for (JobletReferenceBean b : list) {
                                if (b.toString().equals(bean.toString())) {
                                    found = true;
                                    b.addNodeNum();
                                    break;
                                }
                            }
                            if (!found) {
                                list.add(bean);
                            }
                        }

                    }
                }

            }

        }
        return list;
    }

    /**
     * ftang Comment method "isForbbidNode".
     * 
     * @param node
     * @return
     */
    private boolean isForbidNode(RepositoryNode node) {

        IRepositoryObject nodeObject = node.getObject();
        // Avoid to delete node which is locked.
        if (nodeObject != null && nodeObject.getProperty().getItem().getState().isLocked()
                && !(getText().equals(DELETE_FOREVER_TITLE))) {
            return true;
        }

        // Avoid to delete all related documentation node by click Key "Delete" from keyboard.
        if (node.getContentType() == ERepositoryObjectType.JOB_DOC) {
            return true;
        }

        if (node.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.JOB_DOC) {
            return true;
        }

        if (node.getContentType() == ERepositoryObjectType.JOBLET_DOC) {
            return true;
        }

        if (node.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.JOBLET_DOC) {
            return true;
        }

        if (node.getContentType() == ERepositoryObjectType.JOBS) {
            return true;
        }
        if (node.getContentType() == ERepositoryObjectType.GENERATED) {
            return true;
        }
        if (node.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.METADATA_CON_CDC) {
            return true;
        }
        if (node.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.METADATA_CON_TABLE) {
            final IRepositoryObject object = nodeObject;
            if (object != null && object instanceof MetadataTableRepositoryObject) {
                final MetadataTable table = ((MetadataTableRepositoryObject) object).getTable();
                if (table != null && table instanceof SubscriberTable) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * ftang Comment method "deleteElements".
     * 
     * @param factory
     * @param currentJobNode
     * @throws PersistenceException
     * @throws BusinessException
     */
    private boolean deleteElements(IProxyRepositoryFactory factory, RepositoryNode currentJobNode) throws PersistenceException,
            BusinessException {
        Boolean confirm = null;
        boolean needReturn = false;
        IRepositoryObject objToDelete = currentJobNode.getObject();

        List<JobletReferenceBean> checkRepository = checkRepositoryNodeFromProcess(factory, currentJobNode);
        if (checkRepository.size() > 0) {
            JobletReferenceDialog dialog = new JobletReferenceDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getShell(), objToDelete, checkRepository);
            dialog.open();
            return true;
        }
        // To manage case of we have a subitem. This is possible using 'DEL' shortcut:
        ERepositoryObjectType nodeType = (ERepositoryObjectType) currentJobNode.getProperties(EProperties.CONTENT_TYPE);
        if (nodeType.isSubItem()) {
            final DeleteTableAction deleteTableAction = new DeleteTableAction();
            deleteTableAction.setWorkbenchPart(getWorkbenchPart());
            deleteTableAction.run();
            needReturn = true;
        } else {
            if (factory.getStatus(objToDelete) == ERepositoryStatus.DELETED) {
                if (confirm == null) {
                    String title = Messages.getString("DeleteAction.dialog.title"); //$NON-NLS-1$
                    String message = currentJobNode.getProperties(EProperties.LABEL)
                            + " " + Messages.getString("DeleteAction.dialog.message0") + "\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            + Messages.getString("DeleteAction.dialog.message2"); //$NON-NLS-1$
                    confirm = (MessageDialog.openQuestion(new Shell(), title, message));
                }
                if (confirm) {

                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    for (IEditorReference editors : page.getEditorReferences()) {
                        String nameInEditor = editors.getName();
                        if (objToDelete.getLabel().equals(nameInEditor.substring(nameInEditor.indexOf(" ") + 1))) { //$NON-NLS-1$
                            page.closeEditor(editors.getEditor(false), false);
                        }
                    }

                    factory.deleteObjectPhysical(objToDelete);
                    ExpressionPersistance.getInstance().jobDeleted(objToDelete.getLabel());

                }
            } else {
                factory.deleteObjectLogical(objToDelete);
            }
        }

        return needReturn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.actions.ITreeContextualAction#init(org.eclipse.jface.viewers.TreeViewer,
     * org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init(TreeViewer viewer, IStructuredSelection selection) {
        visible = !selection.isEmpty();
        if (selection.isEmpty()) {
            setEnabled(false);
            return;
        }

        boolean enabled = true;
        this.setText(null);
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        if (factory.isUserReadOnlyOnCurrentProject()) {
            visible = false;
        }
        for (Object o : (selection).toArray()) {
            if (visible) {
                RepositoryNode node = (RepositoryNode) o;
                if (!ProjectManager.getInstance().isInCurrentMainProject(node)) {
                    visible = false;
                    break;
                }
                switch (node.getType()) {
                case STABLE_SYSTEM_FOLDER:
                    visible = false;
                case SYSTEM_FOLDER:
                    visible = false;
                    break;
                case SIMPLE_FOLDER:
                    Object obj = node.getProperties(EProperties.LABEL);
                    String label = null;
                    if (obj instanceof String) {
                        label = (String) obj;
                    }
                    if (node.getContentType() == ERepositoryObjectType.JOB_DOC
                            || node.getContentType() == ERepositoryObjectType.JOBLET_DOC
                            || RepositoryConstants.USER_DEFINED.equals(label)) {
                        visible = false;
                    } else {
                        this.setText(DELETE_LOGICAL_TITLE);
                        this.setToolTipText(DELETE_LOGICAL_TOOLTIP);
                        if (node.hasChildren()) {
                            visible = true;
                            enabled = true;
                        }
                    }
                    break;
                case REPOSITORY_ELEMENT:
                    if (node.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.JOB_DOC
                            || node.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.JOBLET_DOC) {
                        visible = false;
                        break;
                    }
                    if (node.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.METADATA_CON_CDC) {
                        enabled = false;
                        visible = false;
                        break;
                    }
                    IRepositoryObject repObj = node.getObject();
                    IProxyRepositoryFactory repFactory = ProxyRepositoryFactory.getInstance();

                    ERepositoryStatus status = repFactory.getStatus(repObj);
                    boolean isEditable = status.isPotentiallyEditable() || status.isEditable();
                    boolean isDeleted = repFactory.getStatus(repObj) == ERepositoryStatus.DELETED;

                    if (isDeleted) {
                        ERepositoryObjectType nodeType = (ERepositoryObjectType) node.getProperties(EProperties.CONTENT_TYPE);
                        if (ERepositoryObjectType.METADATA_CON_TABLE.equals(nodeType)) {
                            visible = false;
                            break;
                        }
                        if (ERepositoryObjectType.METADATA_CON_QUERY.equals(nodeType)) {
                            visible = false;
                            break;
                        }

                        if (getText() == null || DELETE_FOREVER_TITLE.equals(getText())) {
                            this.setText(DELETE_FOREVER_TITLE);
                            this.setToolTipText(DELETE_FOREVER_TOOLTIP);
                        } else {
                            visible = false;
                        }
                    } else {
                        switch (repObj.getType()) {
                        case METADATA_CON_TABLE:
                        case METADATA_CON_QUERY:
                            visible = false;
                            break;
                        default:
                            if (getText() == null || DELETE_LOGICAL_TITLE.equals(getText())) {
                                this.setText(DELETE_LOGICAL_TITLE);
                                this.setToolTipText(DELETE_LOGICAL_TOOLTIP);

                                if (!isEditable) {
                                    visible = true;
                                    enabled = false;
                                }
                            } else {
                                visible = false;
                            }
                            break;
                        }
                    }
                    break;
                default:
                    // Nothing to do
                    break;
                }
            }
        }
        setEnabled(enabled);
    }

    private boolean visible;

    /**
     * Getter for visible.
     * 
     * @return the visible
     */
    @Override
    public boolean isVisible() {
        return this.visible;
    }

    /**
     * Sets the visible.
     * 
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
