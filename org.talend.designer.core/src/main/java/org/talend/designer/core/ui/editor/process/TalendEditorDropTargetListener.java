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
package org.talend.designer.core.ui.editor.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.geometry.Translatable;
import org.eclipse.emf.common.util.EList;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.tools.CreationTool;
import org.eclipse.gef.ui.palette.editparts.PaletteEditPart;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.utils.image.ImageUtils.ICON_SIZE;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.PluginChecker;
import org.talend.core.model.components.IComponent;
import org.talend.core.model.metadata.IEbcdicConstant;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.ISAPConstant;
import org.talend.core.model.metadata.builder.ConvertionHelper;
import org.talend.core.model.metadata.builder.connection.CDCConnection;
import org.talend.core.model.metadata.builder.connection.CDCType;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.metadata.builder.connection.Query;
import org.talend.core.model.metadata.builder.connection.SAPFunctionUnit;
import org.talend.core.model.metadata.designerproperties.PropertyConstants.CDCTypeMode;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.DatabaseConnectionItem;
import org.talend.core.model.properties.EbcdicConnectionItem;
import org.talend.core.model.properties.FileItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.JobletProcessItem;
import org.talend.core.model.properties.LinkRulesItem;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.RulesItem;
import org.talend.core.model.properties.SAPConnectionItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.model.repository.RepositoryObject;
import org.talend.core.ui.ICDCProviderService;
import org.talend.core.ui.images.CoreImageProvider;
import org.talend.core.ui.metadata.command.RepositoryChangeMetadataForEBCDICCommand;
import org.talend.core.ui.metadata.command.RepositoryChangeMetadataForSAPCommand;
import org.talend.designer.core.DesignerPlugin;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.EmfComponent;
import org.talend.designer.core.ui.editor.AbstractTalendEditor;
import org.talend.designer.core.ui.editor.TalendEditor;
import org.talend.designer.core.ui.editor.cmd.ChangeValuesFromRepository;
import org.talend.designer.core.ui.editor.cmd.CreateNodeContainerCommand;
import org.talend.designer.core.ui.editor.cmd.PropertyChangeCommand;
import org.talend.designer.core.ui.editor.cmd.QueryGuessCommand;
import org.talend.designer.core.ui.editor.cmd.RepositoryChangeMetadataCommand;
import org.talend.designer.core.ui.editor.cmd.RepositoryChangeQueryCommand;
import org.talend.designer.core.ui.editor.nodecontainer.NodeContainer;
import org.talend.designer.core.ui.editor.nodecontainer.NodeContainerPart;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.preferences.TalendDesignerPrefConstants;
import org.talend.designer.core.utils.DesignerUtilities;
import org.talend.repository.model.ComponentsFactoryProvider;
import org.talend.repository.model.ERepositoryStatus;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.MetadataTableRepositoryObject;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.SAPFunctionRepositoryObject;
import org.talend.repository.model.RepositoryNode.EProperties;

/**
 * Performs a native Drop for the talendEditor. see feature
 * 
 * $Id: TalendEditorDropTargetListener.java 1 2006-09-29 17:06:40 +0000 (ææäº, 29 ä¹æ 2006) nrousseau $
 * 
 */
public class TalendEditorDropTargetListener extends TemplateTransferDropTargetListener {

    private AbstractTalendEditor editor;

    private boolean fromPalette; // only for palette dnd, feature 6457

    private List<Node> dragedNodes = new ArrayList<Node>();

    /**
     * TalendEditorDropTargetListener constructor comment.
     * 
     * @param editor
     */
    public TalendEditorDropTargetListener(AbstractTalendEditor editor) {
        super(editor.getViewer());
        this.editor = editor;
        setTransfer(LocalSelectionTransfer.getTransfer());
    }

    public boolean isEnabled(DropTargetEvent e) {
        if (PluginChecker.isCDCPluginLoaded()) {
            ICDCProviderService service = (ICDCProviderService) GlobalServiceRegister.getDefault().getService(
                    ICDCProviderService.class);
            Object obj = getSelection().getFirstElement();
            if (obj instanceof RepositoryNode) {
                RepositoryNode sourceNode = (RepositoryNode) obj;
                if (service != null && (service.isSubscriberTableNode(sourceNode) || service.isSystemSubscriberTable(sourceNode))) {
                    return false;
                }
            }

        }
        return !this.editor.getProcess().isReadOnly();
    }

    public void dragEnter(DropTargetEvent event) {

    }

    public void dragLeave(DropTargetEvent event) {

    }

    public void dragOperationChanged(DropTargetEvent event) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.gef.dnd.TemplateTransferDropTargetListener#handleDragOver()
     */
    @Override
    protected void handleDragOver() {
        super.handleDragOver();
        // when the job that selected is the same one in the current editor, the drag event should be disabled.
        IStructuredSelection selection = getSelection();
        if (selection.size() != 1) {
            getCurrentEvent().detail = DND.DROP_NONE;
            return;
        }

        if (selection.getFirstElement() instanceof RepositoryNode) {
            RepositoryNode sourceNode = (RepositoryNode) selection.getFirstElement();
            if (equalsJobInCurrentEditor(sourceNode)) {
                getCurrentEvent().detail = DND.DROP_NONE;
            }
        }

    }

    public void dragOver(DropTargetEvent event) {
        // multi-drag for job only
        IStructuredSelection selection = getSelection();
        boolean allowed = true;
        Iterator iter = selection.iterator();
        selection.size();
        while (iter.hasNext()) {
            Object next = iter.next();
            if (next instanceof RepositoryNode) {
                RepositoryNode sourceNode = (RepositoryNode) next;
                IRepositoryObject object = sourceNode.getObject();
                if (object != null) {
                    Item item = object.getProperty().getItem();
                    if (!(item instanceof ProcessItem)) {
                        allowed = false;
                    }
                }
            }
        }
        if (selection.size() > 1 && !allowed) {
            event.detail = DND.DROP_NONE;
        }

    }

    @Override
    protected Request createTargetRequest() {
        fromPalette = false;
        CreateRequest request = new CreateRequest();
        CreationFactory factory = getFactory(LocalSelectionTransfer.getTransfer().getSelection());
        if (factory != null) {
            fromPalette = true;
            request.setFactory(factory);
            return request;
        }
        return super.createTargetRequest();
    }

    @Override
    protected CreationFactory getFactory(Object template) {
        CreationFactory factory = super.getFactory(template);
        if (factory == null) { // for palette dnd, feature 6457
            if (template != null && template instanceof IStructuredSelection) {
                Object element = ((IStructuredSelection) template).getFirstElement();
                if (element != null && element instanceof PaletteEditPart) {
                    Object model = ((PaletteEditPart) element).getModel();
                    if (model != null && model instanceof ToolEntry) {
                        return (CreationFactory) ((ToolEntry) model).getToolProperty(CreationTool.PROPERTY_CREATION_FACTORY);
                    }

                }
            }
        }
        return factory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.gef.dnd.TemplateTransferDropTargetListener#handleDrop()
     */
    @Override
    protected void handleDrop() {
        updateTargetRequest();
        updateTargetEditPart();

        // if drop a node on the job, create new component,
        // else just update the schema or something of the target component.

        // if (getTargetEditPart() instanceof NodeContainerPart) {

        // IEditorPart iep = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        // IEditorInput iei = iep.getEditorInput();
        // iei
        // EditPart ep = getTargetEditPart();

        if (fromPalette && getTargetRequest() instanceof CreateRequest && getTargetEditPart() instanceof ProcessPart) {
            // for palette dnd, feature 6457
            Object newObject = ((CreateRequest) getTargetRequest()).getNewObject();
            if (newObject != null) {
                Command command = getCommand();
                if (command != null) {
                    execCommandStack(command);
                }
            }
            return;
        }
        //
        if (!(getTargetEditPart() instanceof NodeContainerPart)) {

            try {
                createNewComponent(getCurrentEvent());
            } catch (OperationCanceledException e) {
                return;
            }

        } else {
            createSchema(getSelection().getFirstElement(), getTargetEditPart());
            createQuery(getSelection().getFirstElement(), getTargetEditPart());
            createProperty(getSelection().getFirstElement(), getTargetEditPart());
            createChildJob(getSelection().getFirstElement(), getTargetEditPart());
        }
        this.eraseTargetFeedback();
    }

    /**
     * DOC qwei Comment method "createChildJob".
     */
    private void createChildJob(Object dragModel, EditPart targetEditPart) {
        if (!(dragModel instanceof RepositoryNode && targetEditPart instanceof NodeContainerPart)) {
            return;
        }
        RepositoryNode dragNode = (RepositoryNode) dragModel;
        NodeContainerPart nodePart = (NodeContainerPart) targetEditPart;

        if (dragNode.getObject().getProperty().getItem() instanceof ProcessItem) {
            ProcessItem processItem = (ProcessItem) dragNode.getObject().getProperty().getItem();
            Command command = getChangeChildProcessCommand((Node) nodePart.getNodePart().getModel(), processItem);
            if (command != null) {
                execCommandStack(command);
            }
        }
    }

    /**
     * DOC bqian Comment method "createSchema".
     * 
     * @param firstElement
     * @param targetEditPart
     */
    private void createSchema(Object dragModel, EditPart targetEditPart) {
        if (!(dragModel instanceof RepositoryNode && targetEditPart instanceof NodeContainerPart)) {
            return;
        }
        RepositoryNode dragNode = (RepositoryNode) dragModel;
        NodeContainerPart nodePart = (NodeContainerPart) targetEditPart;

        if (dragNode.getObject().getProperty().getItem() instanceof ConnectionItem) {
            ConnectionItem connectionItem = (ConnectionItem) dragNode.getObject().getProperty().getItem();
            Command command = getChangeMetadataCommand(dragNode, (Node) nodePart.getNodePart().getModel(), connectionItem);
            if (command != null) {
                execCommandStack(command);
            }
        }
    }

    private void createQuery(Object dragModel, EditPart targetEditPart) {
        if (!(dragModel instanceof RepositoryNode && targetEditPart instanceof NodeContainerPart)) {
            return;
        }
        RepositoryNode dragNode = (RepositoryNode) dragModel;
        NodeContainerPart nodePart = (NodeContainerPart) targetEditPart;
        if (dragNode.getObject().getProperty().getItem() instanceof ConnectionItem) {
            ConnectionItem connectionItem = (ConnectionItem) dragNode.getObject().getProperty().getItem();
            Command command = getChangeQueryCommand(dragNode, (Node) nodePart.getNodePart().getModel(), connectionItem);
            if (command != null) {
                execCommandStack(command);
            }
        }
    }

    private void createProperty(Object dragModel, EditPart targetEditPart) {
        if (!(dragModel instanceof RepositoryNode && targetEditPart instanceof NodeContainerPart)) {
            return;
        }
        RepositoryNode dragNode = (RepositoryNode) dragModel;
        NodeContainerPart nodePart = (NodeContainerPart) targetEditPart;
        if (dragNode.getObject().getProperty().getItem() instanceof ConnectionItem) {
            ConnectionItem connectionItem = (ConnectionItem) dragNode.getObject().getProperty().getItem();
            Command command = getChangePropertyCommand(dragNode, (Node) nodePart.getNodePart().getModel(), connectionItem);
            if (command != null) {
                execCommandStack(command);
            }
        }
    }

    private boolean equalsJobInCurrentEditor(RepositoryNode sourceNode) {
        Item item = sourceNode.getObject().getProperty().getItem();
        if (item instanceof ProcessItem) {
            return editor.getProcess().getProperty().getItem().equals(item);
        }
        return false;
    }

    private IStructuredSelection getSelection() {
        LocalSelectionTransfer transfer = (LocalSelectionTransfer) getTransfer();
        IStructuredSelection selection = (IStructuredSelection) transfer.getSelection();
        return selection;
    }

    /**
     * Used to store data temporarily. <br/>
     * 
     * $Id: talend.epf 1 2006-09-29 17:06:40 +0000 (ææäº, 29 ä¹æ 2006) nrousseau $
     * 
     */
    class TempStore {

        // This is the element that user select in the repositoryView.
        RepositoryNode seletetedNode = null;

        EDatabaseComponentName componentName = null;

        IComponent component;

    }

    public void createNewComponent(DropTargetEvent event1) {
        boolean quickCreateInput = event1.detail == DND.DROP_LINK;
        boolean quickCreateOutput = event1.detail == DND.DROP_COPY;
        Iterator iterator = getSelection().iterator();
        List<TempStore> list = new ArrayList<TempStore>();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj instanceof RepositoryNode) {
                RepositoryNode sourceNode = (RepositoryNode) obj;
                if (equalsJobInCurrentEditor(sourceNode)) {
                    continue;
                }

                Item item = sourceNode.getObject().getProperty().getItem();
                ERepositoryObjectType type = sourceNode.getObjectType();
                if (!(item instanceof ConnectionItem) && !(item instanceof ProcessItem) && !(item instanceof JobletProcessItem)
                        && !(item instanceof RulesItem) && !(item instanceof LinkRulesItem)) { // hywang modified for
                    // feature 6484,for
                    // RulesItem
                    return;
                }
                TempStore store = new TempStore();
                store.seletetedNode = sourceNode;
                getAppropriateComponent(item, quickCreateInput, quickCreateOutput, store, type);
                if (store.component != null) {
                    list.add(store);
                } else {
                    MessageDialog.openInformation(editor.getEditorSite().getShell(), Messages
                            .getString("TalendEditorDropTargetListener.dngsupportdialog.title"), //$NON-NLS-1$
                            Messages.getString("TalendEditorDropTargetListener.dngsupportdialog.content")); //$NON-NLS-1$
                }
            }

            org.eclipse.swt.graphics.Point swtLocation = new org.eclipse.swt.graphics.Point(event1.x, event1.y);
            Canvas canvas = (Canvas) editor.getViewer().getControl();

            /*
             * translate to Canvas coordinate
             */
            swtLocation = canvas.toControl(swtLocation);
            org.eclipse.swt.graphics.Point size = canvas.getSize();
            /*
             * translate to Viewport coordinate with zoom
             */
            org.eclipse.draw2d.geometry.Point draw2dPosition = new org.eclipse.draw2d.geometry.Point(swtLocation.x, swtLocation.y);

            /*
             * calcule the view port position. Take into acounte the scroll position
             */
            ProcessPart part = (ProcessPart) editor.getViewer().getRootEditPart().getRoot().getChildren().get(0);

            IFigure targetFigure = part.getFigure();
            translateAbsolateToRelative(targetFigure, draw2dPosition);

            // creates every node
            for (Iterator<TempStore> iter = list.iterator(); iter.hasNext();) {
                TempStore store = iter.next();

                RepositoryNode selectedNode = store.seletetedNode;
                IComponent element = store.component;
                Node node = new Node(element);
                // for bug4564(metadata label format)
                // IPreferenceStore preferenceStore = DesignerPlugin.getDefault().getPreferenceStore();
                // if (preferenceStore.getBoolean(TalendDesignerPrefConstants.USE_REPOSITORY_NAME)) {
                // node.setPropertyValue(EParameterName.LABEL.getName(), selectedNode.getObject().getLabel());
                // }
                IPreferenceStore preferenceStore = DesignerPlugin.getDefault().getPreferenceStore();
                if (preferenceStore.getBoolean(TalendDesignerPrefConstants.USE_REPOSITORY_NAME)) {
                    String LabelValue = null;
                    RepositoryNode repositoryNode = null;
                    repositoryNode = (RepositoryNode) getSelection().getFirstElement();
                    // dnd a table
                    IElementParameter dbTableParam = node.getElementParameterFromField(EParameterFieldType.DBTABLE);
                    boolean hasDbTableField = dbTableParam != null;

                    if (repositoryNode.getObjectType() == ERepositoryObjectType.METADATA_CON_TABLE
                            && repositoryNode.getObject() != null
                            && repositoryNode.getObject().getProperty().getItem() instanceof DatabaseConnectionItem
                            && hasDbTableField) {
                        LabelValue = DesignerUtilities.getParameterVar(dbTableParam.getName());
                    } else if (repositoryNode.getObjectType() == ERepositoryObjectType.PROCESS) { // dnd a job
                        LabelValue = DesignerUtilities.getParameterVar(EParameterName.PROCESS);
                    } else if (CorePlugin.getDefault().getDesignerCoreService().getPreferenceStore(
                            TalendDesignerPrefConstants.DEFAULT_LABEL).equals( //$NON-NLS-1$
                            node.getPropertyValue(EParameterName.LABEL.getName()))) {// dnd a default
                        LabelValue = selectedNode.getObject().getLabel();
                    }
                    if (LabelValue != null) {
                        node.setPropertyValue(EParameterName.LABEL.getName(), LabelValue);
                    }
                }
                processSpecificDBTypeIfSameProduct(store.componentName, node);

                NodeContainer nc = new NodeContainer(node);
                // create the node on the design sheet
                execCommandStack(new CreateNodeContainerCommand((Process) editor.getProcess(), nc, draw2dPosition));
                // initialize the propertiesView

                List<Command> commands = createRefreshingPropertiesCommand(selectedNode, node);
                for (Command command : commands) {
                    execCommandStack(command);
                }
                draw2dPosition = draw2dPosition.getCopy();
                draw2dPosition.x += TalendEditor.GRID_SIZE;
                draw2dPosition.y += TalendEditor.GRID_SIZE;

                node.checkNode();

            }

        }

    }

    /**
     * DOC bqian Comment method "createRefreshingPropertiesCommand".
     * 
     * @param selectedNode
     * @param node
     */
    private List<Command> createRefreshingPropertiesCommand(RepositoryNode selectedNode, Node node) {
        List<Command> list = new ArrayList<Command>();
        if (selectedNode.getObject().getProperty().getItem() instanceof ConnectionItem) {
            String propertyId = selectedNode.getObject().getProperty().getId();
            ConnectionItem originalConnectionItem = (ConnectionItem) selectedNode.getObject().getProperty().getItem();
            ConnectionItem connectionItem = originalConnectionItem;
            Connection originalConnection = connectionItem.getConnection();
            Connection connection = connectionItem.getConnection();
            // if component is CDC, replace by the CDC connection.
            if (node.getComponent().getName().contains("CDC")) { // to replace by a flag CDC in component? //$NON-NLS-1$
                if (selectedNode.getObject().getProperty().getItem() instanceof DatabaseConnectionItem) {
                    CDCConnection cdcConn = ((DatabaseConnection) connection).getCdcConns();
                    if (cdcConn != null) {
                        EList cdcTypes = cdcConn.getCdcTypes();
                        if (cdcTypes != null && !cdcTypes.isEmpty()) {
                            CDCType cdcType = (CDCType) cdcTypes.get(0);
                            // replace property by CDC property.
                            propertyId = cdcType.getLinkDB();
                            try {
                                IRepositoryObject object = ProxyRepositoryFactory.getInstance().getLastVersion(propertyId);
                                if (object != null) {
                                    if (object.getProperty().getItem() instanceof DatabaseConnectionItem) {
                                        DatabaseConnectionItem dbConnItem = (DatabaseConnectionItem) object.getProperty()
                                                .getItem();
                                        // replace connection by CDC connection
                                        connectionItem = dbConnItem;
                                        connection = dbConnItem.getConnection();
                                    }
                                }
                            } catch (PersistenceException e) {
                                ExceptionHandler.process(e);
                            }
                            // set cdc type mode.
                            IElementParameter logModeParam = node.getElementParameter(EParameterName.CDC_TYPE_MODE.getName());
                            if (logModeParam != null) {
                                String cdcTypeMode = ((DatabaseConnection) originalConnection).getCdcTypeMode();
                                Command logModeCmd = new PropertyChangeCommand(node, EParameterName.CDC_TYPE_MODE.getName(),
                                        CDCTypeMode.LOG_MODE.getName().equals(cdcTypeMode));
                                list.add(logModeCmd);
                            }
                        }
                    }
                }

            }

            // for SAP
            if (selectedNode.getObjectType() == ERepositoryObjectType.METADATA_SAP_FUNCTION
                    && PluginChecker.isSAPWizardPluginLoaded()) {
                SAPFunctionUnit functionUnit = (SAPFunctionUnit) ((SAPFunctionRepositoryObject) selectedNode.getObject())
                        .getAbstractMetadataObject();
                for (MetadataTable table : (List<MetadataTable>) functionUnit.getTables()) {
                    Command sapCmd = new RepositoryChangeMetadataForSAPCommand(node, ISAPConstant.TABLE_SCHEMAS,
                            table.getLabel(), ConvertionHelper.convert(table));
                    list.add(sapCmd);
                }
            }

            // fore EBCDIC, by cli
            if (selectedNode.getObjectType() == ERepositoryObjectType.METADATA_FILE_EBCDIC
                    && PluginChecker.isEBCDICPluginLoaded()) {
                for (MetadataTable table : (List<MetadataTable>) originalConnection.getTables()) {
                    Command ebcdicCmd = new RepositoryChangeMetadataForEBCDICCommand(node, IEbcdicConstant.TABLE_SCHEMAS, table
                            .getLabel(), ConvertionHelper.convert(table));
                    list.add(ebcdicCmd);
                }
            }

            IElementParameter propertyParam = node.getElementParameterFromField(EParameterFieldType.PROPERTY_TYPE);
            if (propertyParam != null) {
                propertyParam.getChildParameters().get(EParameterName.PROPERTY_TYPE.getName()).setValue(EmfComponent.REPOSITORY);
                propertyParam.getChildParameters().get(EParameterName.REPOSITORY_PROPERTY_TYPE.getName()).setValue(propertyId);
            }
            IProxyRepositoryFactory factory = DesignerPlugin.getDefault().getProxyRepositoryFactory();

            Map<String, IMetadataTable> repositoryTableMap = new HashMap<String, IMetadataTable>();

            if (!originalConnection.isReadOnly()) {
                for (Object tableObj : originalConnection.getTables()) {
                    org.talend.core.model.metadata.builder.connection.MetadataTable table;

                    table = (org.talend.core.model.metadata.builder.connection.MetadataTable) tableObj;

                    if (factory.getStatus(originalConnectionItem) != ERepositoryStatus.DELETED) {
                        if (!factory.isDeleted(table)) {
                            String value = table.getId();
                            IMetadataTable newTable = ConvertionHelper.convert(table);
                            repositoryTableMap.put(value, newTable);
                        }
                    }
                }
            }
            // DesignerPlugin.getDefault().getProxyRepositoryFactory().getLastVersion("")
            if (propertyParam != null) {
                // command used to set property type
                ChangeValuesFromRepository command1 = new ChangeValuesFromRepository(node, connection, propertyParam.getName()
                        + ":" + EParameterName.REPOSITORY_PROPERTY_TYPE.getName(), propertyId, true); //$NON-NLS-1$
                command1.setMaps(repositoryTableMap);
                if (selectedNode.getProperties(EProperties.CONTENT_TYPE) != ERepositoryObjectType.METADATA_CON_QUERY) {
                    command1.setGuessQuery(true);
                }
                if (selectedNode.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.METADATA_SAP_FUNCTION) {
                    command1.setSapFunctionName((String) selectedNode.getProperties(EProperties.LABEL));
                }
                list.add(command1);
            }

            // command used to set metadata
            Command command = getChangeMetadataCommand(selectedNode, node, originalConnectionItem);
            if (command != null) {
                list.add(command);
            }

            // command used to set query
            if (selectedNode.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.METADATA_CON_QUERY) {
                IElementParameter queryParam = node.getElementParameterFromField(EParameterFieldType.QUERYSTORE_TYPE);

                RepositoryObject object = (RepositoryObject) selectedNode.getObject();
                Query query = (Query) object.getAdapter(Query.class);
                String value = originalConnectionItem.getProperty().getId() + " - " + query.getLabel(); //$NON-NLS-1$
                if (queryParam != null) {
                    RepositoryChangeQueryCommand command3 = new RepositoryChangeQueryCommand(node, query, queryParam.getName()
                            + ":" + EParameterName.REPOSITORY_QUERYSTORE_TYPE.getName(), value); //$NON-NLS-1$
                    list.add(command3);
                }
            } else {
                if (connection instanceof DatabaseConnection) {
                    DatabaseConnection connection2 = (DatabaseConnection) connection;
                    String schema = connection2.getSchema();
                    String dbType = connection2.getDatabaseType();
                    QueryGuessCommand queryGuessCommand = null;
                    if (node.getMetadataList().size() == 0) {
                        queryGuessCommand = new QueryGuessCommand(node, null, schema, dbType);
                    } else {
                        // modified by hyWang for bug 7190
                        queryGuessCommand = new QueryGuessCommand(node, node.getMetadataList().get(0), schema, dbType);
                    }
                    if (queryGuessCommand != null) {
                        list.add(queryGuessCommand);
                    }
                }
            }
            // context, moved to ChangeValuesFromRepository(bug 5198)
            // ConnectionContextHelper.addContextForNodeParameter(node, connectionItem);
        } else if (selectedNode.getObject().getProperty().getItem() instanceof ProcessItem) {
            ProcessItem processItem = (ProcessItem) selectedNode.getObject().getProperty().getItem();
            // command used to set job
            String value = processItem.getProperty().getId();
            PropertyChangeCommand command4 = new PropertyChangeCommand(node, EParameterName.PROCESS_TYPE_PROCESS.getName(), value);
            list.add(command4);
            PropertyChangeCommand command5 = new PropertyChangeCommand(node, EParameterName.PROCESS_TYPE_CONTEXT.getName(), node
                    .getProcess().getContextManager().getDefaultContext().getName());
            list.add(command5);
        } else if (selectedNode.getObject().getProperty().getItem() instanceof FileItem) { // hywang add for 6484
            if (selectedNode.getObject().getProperty().getItem() instanceof RulesItem) {
                RulesItem rulesItem = (RulesItem) selectedNode.getObject().getProperty().getItem();
                //                String displayName = "Rules:" + rulesItem.getProperty().getLabel(); //$NON-NLS-N$
                IElementParameter propertyParam = node.getElementParameterFromField(EParameterFieldType.PROPERTY_TYPE);
                if (propertyParam != null) {
                    propertyParam.getChildParameters().get(EParameterName.PROPERTY_TYPE.getName()).setValue(
                            EmfComponent.REPOSITORY);
                    // propertyParam.getChildParameters().get(EParameterName.REPOSITORY_PROPERTY_TYPE.getName())
                    // .setListItemsDisplayName(new String[] { displayName });
                    final String showId = rulesItem.getProperty().getId();
                    PropertyChangeCommand command6 = new PropertyChangeCommand(node, EParameterName.REPOSITORY_PROPERTY_TYPE
                            .getName(), showId);
                    list.add(command6);
                }
            }
        } else if (selectedNode.getObject().getProperty().getItem() instanceof LinkRulesItem) {
            LinkRulesItem linkItem = (LinkRulesItem) selectedNode.getObject().getProperty().getItem();
            IElementParameter propertyParam = node.getElementParameterFromField(EParameterFieldType.PROPERTY_TYPE);
            if (propertyParam != null) {
                propertyParam.getChildParameters().get(EParameterName.PROPERTY_TYPE.getName()).setValue(EmfComponent.REPOSITORY);
                // propertyParam.getChildParameters().get(EParameterName.REPOSITORY_PROPERTY_TYPE.getName())
                // .setListItemsDisplayName(new String[] { displayName });
                final String showId = linkItem.getProperty().getId();
                PropertyChangeCommand command7 = new PropertyChangeCommand(node, EParameterName.REPOSITORY_PROPERTY_TYPE
                        .getName(), showId);
                list.add(command7);
            }
        }
        return list;
    }

    /**
     * DOC bqian Comment method "getChangeMetadataCommand".
     * 
     * @param selectedNode
     * @param node
     * @param list
     * @param connectionItem
     */
    private Command getChangeMetadataCommand(RepositoryNode selectedNode, Node node, ConnectionItem connectionItem) {
        if (selectedNode.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.METADATA_CON_TABLE
                || selectedNode.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.METADATA_SAP_FUNCTION) {
            String etlSchema = null;
            if (connectionItem.getConnection() instanceof DatabaseConnection) {
                DatabaseConnection connection = (DatabaseConnection) connectionItem.getConnection();
                if (connection instanceof DatabaseConnection) {
                    etlSchema = connection.getSchema();
                }
                if (!"".equals(etlSchema)) {
                    IElementParameter e = node.getElementParameter("ELT_SCHEMA_NAME");
                    if (e != null) {
                        e.setValue("\"" + etlSchema + "\"");
                    }
                    // node.getElementParameter("ELT_SCHEMA_NAME").setValue("\"" + etlSchema + "\"");
                }
            }

            RepositoryObject object = (RepositoryObject) selectedNode.getObject();
            MetadataTable table = (MetadataTable) object.getAdapter(MetadataTable.class);
            String value = connectionItem.getProperty().getId() + " - " + table.getLabel(); //$NON-NLS-1$
            IElementParameter schemaParam = node.getElementParameterFromField(EParameterFieldType.SCHEMA_TYPE);
            IElementParameter queryParam = node.getElementParameterFromField(EParameterFieldType.QUERYSTORE_TYPE);
            if (queryParam != null) {
                queryParam = queryParam.getChildParameters().get(EParameterName.QUERYSTORE_TYPE.getName());
                if (queryParam != null) {
                    queryParam.setValue(EmfComponent.BUILTIN);
                }
            }
            // for SAP
            if (PluginChecker.isSAPWizardPluginLoaded() && connectionItem instanceof SAPConnectionItem
                    && object instanceof MetadataTableRepositoryObject) {
                Command sapCmd = new RepositoryChangeMetadataForSAPCommand(node, ISAPConstant.TABLE_SCHEMAS, table.getLabel(),
                        ConvertionHelper.convert(table));
                return sapCmd;
            }

            // for EBCDIC (bug 5860)
            if (PluginChecker.isEBCDICPluginLoaded() && connectionItem instanceof EbcdicConnectionItem) {
                Command ebcdicCmd = new RepositoryChangeMetadataForEBCDICCommand(node, IEbcdicConstant.TABLE_SCHEMAS, table
                        .getLabel(), ConvertionHelper.convert(table));
                return ebcdicCmd;
            }
            if (schemaParam == null) {
                return null;
            }
            if (node.isELTComponent()) {
                node.setPropertyValue(EParameterName.LABEL.getName(), "__ELT_TABLE_NAME__");
            }
            schemaParam.getChildParameters().get(EParameterName.SCHEMA_TYPE.getName()).setValue(EmfComponent.REPOSITORY);
            RepositoryChangeMetadataCommand command2 = new RepositoryChangeMetadataCommand(node, schemaParam.getName() + ":" //$NON-NLS-1$
                    + EParameterName.REPOSITORY_SCHEMA_TYPE.getName(), value, ConvertionHelper.convert(table), null);
            return command2;
        }
        return null;
    }

    private Command getChangeQueryCommand(RepositoryNode selectedNode, Node node, ConnectionItem connectionItem) {
        if (selectedNode.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.METADATA_CON_QUERY) {
            RepositoryObject object = (RepositoryObject) selectedNode.getObject();
            Query query = (Query) object.getAdapter(Query.class);
            String value = connectionItem.getProperty().getId() + " - " + query.getLabel(); //$NON-NLS-1$
            IElementParameter queryParam = node.getElementParameterFromField(EParameterFieldType.QUERYSTORE_TYPE);
            if (queryParam != null) {
                queryParam.getChildParameters().get(EParameterName.QUERYSTORE_TYPE.getName()).setValue(EmfComponent.REPOSITORY);
                RepositoryChangeQueryCommand command2 = new RepositoryChangeQueryCommand(node, query, queryParam.getName() + ":" //$NON-NLS-1$
                        + EParameterName.REPOSITORY_QUERYSTORE_TYPE.getName(), value);
                return command2;
            }

        }
        return null;
    }

    private Command getChangePropertyCommand(RepositoryNode selectedNode, Node node, ConnectionItem connectionItem) {
        ERepositoryObjectType selectedNodetype = selectedNode.getObjectType();
        EDatabaseComponentName name = EDatabaseComponentName.getCorrespondingComponentName(connectionItem, selectedNodetype);
        if (name != null) {
            List<String> componentNameList = new ArrayList<String>();
            componentNameList.add(name.getInputComponentName());
            componentNameList.add(name.getOutPutComponentName());
            String nodeComponentName = node.getComponent().getName();
            if (componentNameList.contains(nodeComponentName)) {
                IElementParameter param = node.getElementParameterFromField(EParameterFieldType.PROPERTY_TYPE);
                if (param != null) {
                    return getPropertyPublicPart(selectedNode, param, node, connectionItem);
                }
            }
        }

        return null;
    }

    private Command getPropertyPublicPart(RepositoryNode selectedNode, IElementParameter param, Node node,
            ConnectionItem connectionItem) {
        param.getChildParameters().get(EParameterName.PROPERTY_TYPE.getName()).setValue(EmfComponent.REPOSITORY);
        ChangeValuesFromRepository command2 = new ChangeValuesFromRepository(node, connectionItem.getConnection(), param
                .getName()
                + ":" + EParameterName.REPOSITORY_PROPERTY_TYPE.getName(), selectedNode.getObject().getProperty().getId()); //$NON-NLS-1$
        return command2;

    }

    /**
     * DOC qwei Comment method "getChangeChildProcessCommand".
     */
    private Command getChangeChildProcessCommand(Node node, ProcessItem processItem) {
        // command used to set job
        String value = processItem.getProperty().getId();
        IElementParameter processParam = node.getElementParameterFromField(EParameterFieldType.PROCESS_TYPE);
        if (processParam != null) {
            PropertyChangeCommand command2 = new PropertyChangeCommand(node, EParameterName.PROCESS_TYPE_PROCESS.getName(), value);
            return command2;
        }
        return null;
    }

    public void dropAccept(DropTargetEvent event) {
    }

    boolean value1 = true;

    boolean value2 = true;

    boolean value3 = true;

    private void getAppropriateComponent(Item item, boolean quickCreateInput, boolean quickCreateOutput, TempStore store,
            ERepositoryObjectType type) {
        EDatabaseComponentName name = EDatabaseComponentName.getCorrespondingComponentName(item, type);
        String componentName = null;
        if (item instanceof JobletProcessItem) { // joblet
            componentName = item.getProperty().getLabel();
        } else if (name == null) {
            return;
        } else { // tRunjob
            componentName = name.getDefaultComponentName();
        }

        List<IComponent> components = ComponentsFactoryProvider.getInstance().getComponents();
        // tRunJob is special from our rules
        if (name == EDatabaseComponentName.RunJob || item instanceof JobletProcessItem) {
            store.component = ComponentsFactoryProvider.getInstance().get(componentName);
        } else {
            // for database, file, webservices, saleforce ...

            String productNameWanted = name.getProductName();
            String needValue1 = "tELT" + name.getInputComponentName().substring(1, name.getInputComponentName().length());

            String needValue2 = "tELT" + name.getOutPutComponentName().substring(1, name.getOutPutComponentName().length());

            String needVlue3 = "tELT"
                    + productNameWanted.substring(productNameWanted.indexOf(":") + 1, productNameWanted.length()) + "Map";
            List<IComponent> neededComponents = new ArrayList<IComponent>();
            for (IComponent component : components) {
                if (component instanceof EmfComponent) {
                    EmfComponent emfComponent = (EmfComponent) component;
                    String componentProductname = emfComponent.getRepositoryType();
                    if (componentProductname == null) {
                        continue;
                    }
                    if (productNameWanted.endsWith(componentProductname)) {
                        neededComponents.add(emfComponent);
                    }
                }
            }
            if (type.toString().equalsIgnoreCase("Metadata schema")) {

                for (IComponent component1 : neededComponents) {
                    if (component1.getName().equalsIgnoreCase(needValue1)) {
                        value1 = false;
                    }
                    if (component1.getName().equalsIgnoreCase(needValue2)) {
                        value2 = false;
                    }

                }
                for (IComponent component : components) {
                    needValue1 = "tELT" + name.getInputComponentName().substring(1, name.getInputComponentName().length());

                    needValue2 = "tELT" + name.getOutPutComponentName().substring(1, name.getOutPutComponentName().length());

                    needVlue3 = "tELT"
                            + productNameWanted.substring(productNameWanted.indexOf(":") + 1, productNameWanted.length()) + "Map";

                    if ((component.getName().equals(needValue1) && value1)
                            || (component.getName().equalsIgnoreCase(needValue2) && value2)) {
                        neededComponents.add(component);
                    }
                    if (component.getName().equalsIgnoreCase(needVlue3)) {
                        neededComponents.remove(component);
                    }
                    if (productNameWanted.equalsIgnoreCase("DATABASE:TERADATA")) {
                        needValue1 = "t" + name.getInputComponentName().substring(1, name.getInputComponentName().length());

                        needValue2 = "t" + name.getOutPutComponentName().substring(1, name.getOutPutComponentName().length());

                        needVlue3 = "tELT"
                                + productNameWanted.substring(productNameWanted.indexOf(":") + 1, productNameWanted.length())
                                + "Map";

                        if (component.getName().equals(needValue1) || component.getName().equalsIgnoreCase(needValue2)) {
                            neededComponents.add(component);
                        }
                        if (component.getName().equalsIgnoreCase(needVlue3) && value3) {
                            neededComponents.remove(component);
                        }

                    }
                }
            }
            if (type.toString().equalsIgnoreCase("Db Connections")) {
                for (IComponent component1 : neededComponents) {

                    if (component1.getName().equalsIgnoreCase(needVlue3)) {
                        value3 = false;
                    }
                }
                for (IComponent component : components) {
                    needValue1 = "tELT" + name.getInputComponentName().substring(1, name.getInputComponentName().length());

                    needValue2 = "tELT" + name.getOutPutComponentName().substring(1, name.getOutPutComponentName().length());

                    needVlue3 = "tELT"
                            + productNameWanted.substring(productNameWanted.indexOf(":") + 1, productNameWanted.length()) + "Map";

                    if (component.getName().equals(needValue1) || component.getName().equalsIgnoreCase(needValue2)) {
                        neededComponents.remove(component);
                    }
                    if (component.getName().equalsIgnoreCase(needVlue3) && value3) {
                        neededComponents.add(component);
                    }

                }

            }

            IComponent component = chooseOneComponent(neededComponents, name, quickCreateInput, quickCreateOutput);
            store.component = component;
        }
        store.componentName = name;
    }

    /**
     * Let the user choose which component he would like to create.
     * 
     * @param neededComponents
     * @param name
     * @param quickCreateInput
     * @param quickCreateOutput
     */
    private IComponent chooseOneComponent(List<IComponent> neededComponents, EDatabaseComponentName name,
            boolean quickCreateInput, boolean quickCreateOutput) {
        if (neededComponents.isEmpty()) {
            return null;
        }
        if (neededComponents.size() == 1) {
            return neededComponents.get(0);
        }

        IComponent inputComponent = getComponentByName(name.getInputComponentName(), quickCreateInput, neededComponents);
        if (inputComponent != null) {
            return inputComponent;
        }
        IComponent outputComponent = getComponentByName(name.getOutPutComponentName(), quickCreateOutput, neededComponents);
        if (outputComponent != null) {
            return outputComponent;
        }

        ComponentChooseDialog dialog = new ComponentChooseDialog(editor.getSite().getShell(), neededComponents);
        IComponent defaultComponent = getComponentByName(name.getDefaultComponentName(), true, neededComponents);
        if (defaultComponent != null) {
            dialog.setInitialSelections(new Object[] { defaultComponent });
        }
        if (dialog.open() == IDialogConstants.OK_ID) {
            return dialog.getResultComponent();
        }

        throw new OperationCanceledException(Messages.getString("TalendEditorDropTargetListener.cancelOperation")); //$NON-NLS-1$
    }

    private IComponent getComponentByName(String name, boolean loop, List<IComponent> neededComponents) {
        if (loop) {
            for (IComponent component : neededComponents) {
                if (component.getName().equals(name)) {
                    return component;
                }
            }
        }
        return null;
    }

    private void execCommandStack(Command command) {
        CommandStack cs = editor.getCommandStack();
        if (cs != null) {
            cs.execute(command);
        } else {
            command.execute();
        }
    }

    /**
     * see issue 0002439.<br>
     * There are two types of Oracle.
     * 
     * @param name
     * @param node
     */
    private void processSpecificDBTypeIfSameProduct(EDatabaseComponentName name, Node node) {
        // process "Oracle with service name"
        if (name == EDatabaseComponentName.DBORACLESN) {
            IElementParameter p = node.getElementParameter("CONNECTION_TYPE"); //$NON-NLS-1$
            // set value to "ORACLE_SERVICE_NAME"
            p.setValue(p.getListItemsValue()[1]);
        }
    }

    public void translateAbsolateToRelative(IFigure owner, Translatable t) {
        owner.translateToRelative(t);

        Rectangle bounds = owner.getBounds();
        t.performTranslate(-bounds.x, -bounds.y);

    }

    /**
     * Sets the editor.
     * 
     * @param editor the editor to set
     */
    public void setEditor(AbstractTalendEditor editor) {
        this.editor = editor;
    }
}

/**
 * A dialog used to choose one component.
 */
class ComponentChooseDialog extends ListDialog {

    /**
     * bqian ComponentChooseDialog constructor comment.
     * 
     * @param parentShell
     */
    public ComponentChooseDialog(Shell parentShell, List<IComponent> input) {
        super(parentShell);
        setTitle(Messages.getString("TalendEditorDropTargetListener.title")); //$NON-NLS-1$
        setMessage(Messages.getString("TalendEditorDropTargetListener.chooseComponent")); //$NON-NLS-1$
        setInput(input);
        setContentProvider(new ArrayContentProvider());
        setLabelProvider(new LabelProvider() {

            @Override
            public Image getImage(Object element) {
                IComponent component = (IComponent) element;
                return CoreImageProvider.getComponentIcon(component, ICON_SIZE.ICON_16);
            }

            @Override
            public String getText(Object element) {
                IComponent component = (IComponent) element;
                return component.getName();
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
             */
            @Override
            public void dispose() {
                super.dispose();
            }
        });

    }

    // /*
    // * (non-Javadoc)
    // *
    // * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
    // */
    // @Override
    // protected Control createDialogArea(Composite parent) {
    // Composite content = (Composite) super.createDialogArea(parent);
    // GridData data = (GridData) content.getLayoutData();
    // data.minimumHeight = 400;
    // data.heightHint = 400;
    // data.minimumWidth = 500;
    // data.widthHint = 500;
    // content.setLayoutData(data);
    //
    // TableViewer viewer = new TableViewer(content, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    // viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
    // viewer.setContentProvider(new ArrayContentProvider());
    // viewer.setLabelProvider(new LabelProvider() {
    //
    // @Override
    // public Image getImage(Object element) {
    // IComponent component = (IComponent) element;
    // return component.getIcon32().createImage();
    // }
    //
    // @Override
    // public String getText(Object element) {
    // IComponent component = (IComponent) element;
    // return component.getName();
    // }
    //
    // /*
    // * (non-Javadoc)
    // *
    // * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
    // */
    // @Override
    // public void dispose() {
    // super.dispose();
    // }
    // });
    //
    // // viewer.addSelectionChangedListener(new ISelectionChangedListener() {
    // //
    // // public void selectionChanged(SelectionChangedEvent event) {
    // //
    // // getButton(IDialogConstants.OK_ID).setEnabled(highlightOKButton);
    // // }
    // // });
    // viewer.addDoubleClickListener(new IDoubleClickListener() {
    //
    // public void doubleClick(DoubleClickEvent event) {
    // if (getButton(IDialogConstants.OK_ID).isEnabled()) {
    // okPressed();
    // }
    // }
    // });
    // viewer.setInput(input);
    // return content;
    // }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    // @Override
    // protected void okPressed() {
    // IStructuredSelection selection = (IStructuredSelection) repositoryView.getViewer().getSelection();
    // result = (RepositoryNode) selection.getFirstElement();
    // super.okPressed();
    // }
    public IComponent getResultComponent() {
        return (IComponent) getResult()[0];
    }
}
