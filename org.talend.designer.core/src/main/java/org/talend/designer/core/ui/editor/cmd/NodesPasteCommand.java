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
package org.talend.designer.core.ui.editor.cmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.model.components.IComponent;
import org.talend.core.model.metadata.IMetadataColumn;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.process.EConnectionType;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.ElementParameterParser;
import org.talend.core.model.process.IConnectionCategory;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.IExternalNode;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.INodeConnector;
import org.talend.core.model.process.INodeReturn;
import org.talend.core.model.process.IProcess;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.designer.core.ui.AbstractMultiPageTalendEditor;
import org.talend.designer.core.ui.editor.TalendEditor;
import org.talend.designer.core.ui.editor.connections.Connection;
import org.talend.designer.core.ui.editor.nodecontainer.NodeContainer;
import org.talend.designer.core.ui.editor.nodecontainer.NodeContainerPart;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.nodes.NodePart;
import org.talend.designer.core.ui.editor.process.Process;
import org.talend.designer.core.ui.editor.process.ProcessPart;
import org.talend.designer.core.ui.editor.subjobcontainer.SubjobContainer;
import org.talend.designer.core.ui.editor.subjobcontainer.SubjobContainerPart;
import org.talend.designer.core.utils.UpgradeElementHelper;
import org.talend.repository.model.ComponentsFactoryProvider;

/**
 * Command used to paste all the components.
 * 
 * $Id$
 * 
 */
public class NodesPasteCommand extends Command {

    private Process process;

    private List<NodeContainer> nodeContainerList;

    private List<EditPart> oldSelection;

    private NodePart nodePart;

    private List<NodePart> nodeParts;

    private List<Connection> connections;

    private List<String> createdNames;

    private boolean multipleCommand;

    Point cursorLocation = null;

    private List<SubjobContainerPart> subjobParts;

    /*
     * if true, all of properties will keep originally. feature 6131
     */
    private boolean isJobletRefactor = false;

    /**
     * Getter for cursorLocation.
     * 
     * @return the cursorLocation
     */
    public Point getCursorLocation() {
        return this.cursorLocation;
    }

    /**
     * Sets the cursorLocation.
     * 
     * @param cursorLocation the cursorLocation to set
     */
    public void setCursorLocation(Point cursorLocation) {
        this.cursorLocation = cursorLocation;
    }

    /**
     * 
     * cLi Comment method "setJobletRefactor".
     * 
     * feature 6131, refactor nodes to joblet.
     */
    public void setJobletRefactor(boolean isJobletRefactor) {
        this.isJobletRefactor = isJobletRefactor;
    }

    public boolean isJobletRefactor() {
        return this.isJobletRefactor;
    }

    public NodesPasteCommand(List<NodePart> nodeParts, Process process, Point cursorLocation) {
        this.process = process;
        nodePart = nodeParts.get(0);
        setCursorLocation(cursorLocation);
        orderNodeParts(nodeParts);
        setLabel(Messages.getString("NodesPasteCommand.label")); //$NON-NLS-1$

    }

    @SuppressWarnings("unchecked")
    private String createNewConnectionName(String oldName, String baseName) {
        String newName = null;
        if (baseName != null) {
            for (String uniqueConnectionName : createdNames) {
                if (process.checkValidConnectionName(uniqueConnectionName, true)) {
                    process.addUniqueConnectionName(uniqueConnectionName);
                }
            }
            newName = process.generateUniqueConnectionName(baseName);

            for (String uniqueConnectionName : createdNames) {
                if (!process.checkValidConnectionName(uniqueConnectionName, true)) {
                    process.removeUniqueConnectionName(uniqueConnectionName);
                }
            }
        } else {
            if (process.checkValidConnectionName(oldName, true)) {
                newName = oldName;
            } else {
                newName = checkExistingNames("copyOf" + oldName); //$NON-NLS-1$
            }
            newName = checkNewNames(newName, baseName);
        }
        createdNames.add(newName);
        return newName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.gef.commands.Command#canExecute()
     */
    @Override
    public boolean canExecute() {
        return !process.isReadOnly();
    }

    private void orderNodeParts(List<NodePart> nodeParts) {
        this.nodeParts = new ArrayList<NodePart>();

        Point curLocation;

        NodePart toAdd = null;

        List<NodePart> restToOrder = new ArrayList<NodePart>();
        restToOrder.addAll(nodeParts);

        for (NodePart copiedNodePart : nodeParts) {
            curLocation = null;
            for (NodePart partToOrder : restToOrder) {
                Node copiedNode = (Node) partToOrder.getModel();
                if (curLocation == null) {
                    curLocation = copiedNode.getLocation();
                    toAdd = partToOrder;
                } else {
                    if (curLocation.y >= copiedNode.getLocation().y) {
                        if (curLocation.x >= copiedNode.getLocation().x) {
                            curLocation = copiedNode.getLocation();
                            toAdd = partToOrder;
                        }
                    }
                }
            }
            if (toAdd != null) {
                this.nodeParts.add(toAdd);
                restToOrder.remove(toAdd);
            }
        }
    }

    private String checkExistingNames(final String oldName) {
        String tmpName = oldName + "_"; //$NON-NLS-1$
        String newName = oldName;

        int index = 0;
        while (!process.checkValidConnectionName(newName, true)) {
            newName = tmpName + (index++);
        }
        return newName;
    }

    private String checkNewNames(final String oldName, String baseName) {
        String tmpName = oldName + "_"; //$NON-NLS-1$
        if (baseName != null) {
            tmpName = baseName;
        }
        String newName = oldName;

        int index = 0;
        while (createdNames.contains(newName)) {
            newName = tmpName + index++;
        }
        // check the name again in process.
        while (!process.checkValidConnectionName(newName, true)) {
            newName = tmpName + (index++);
        }
        return newName;
    }

    /**
     * 
     * Will return a empty location for a component from a given point.
     * 
     * @param location
     * @return
     */
    private Point findLocationForNode(final Point location, final Dimension size, int index, int firstIndex,
            NodePart copiedNodePart) {
        Point newLocation = findLocationForNodeInProcess(location, size);
        newLocation = findLocationForNodeInContainerList(newLocation, size, index, firstIndex, copiedNodePart);
        return newLocation;
    }

    @SuppressWarnings("unchecked")
    private Point findLocationForNodeInProcess(final Point location, Dimension size) {
        Rectangle copiedRect = new Rectangle(location, size);
        Point newLocation = new Point(location);
        for (Node node : (List<Node>) process.getGraphicalNodes()) {
            Rectangle currentRect = new Rectangle(node.getLocation(), node.getSize());
            if (currentRect.intersects(copiedRect)) {
                newLocation.x += size.width;
                newLocation.y += size.height;
                return findLocationForNodeInProcess(newLocation, size);
            }
        }
        return newLocation;
    }

    private Point findLocationForNodeInContainerList(final Point location, Dimension size, int index, int firstIndex,
            NodePart copiedNodePart) {
        Rectangle copiedRect = new Rectangle(location, size);
        Point newLocation = new Point(location);
        if (getCursorLocation() == null) {
            for (NodeContainer nodeContainer : nodeContainerList) {
                Node node = nodeContainer.getNode();
                Rectangle currentRect = new Rectangle(node.getLocation(), node.getSize());
                if (currentRect.intersects(copiedRect)) {
                    newLocation.x += size.width;
                    newLocation.y += size.height;
                    // newLocation = computeTheDistance(index, firstIndex, newLocation);
                    Point tmpPoint = findLocationForNodeInProcess(newLocation, size);
                    return findLocationForNodeInContainerList(tmpPoint, size, index, firstIndex, copiedNodePart);
                }
            }
            return newLocation;
        }
        if (!nodePart.equals(copiedNodePart)) {
            newLocation = computeTheDistance(index, firstIndex, newLocation);
        }
        return newLocation;
    }

    private Point computeTheDistance(int index, int firstIndex, Point location) {
        Point firstNodeLocation = ((Node) nodePart.getModel()).getLocation();
        Point currentNodeLocation = ((Node) nodeParts.get(index).getModel()).getLocation();

        int distanceX = firstNodeLocation.x - currentNodeLocation.x;
        int distanceY = firstNodeLocation.y - currentNodeLocation.y;
        location.x = location.x - distanceX;
        location.y = location.y - distanceY;
        return location;
    }

    private boolean containNodeInProcess(Node copiedNode) {
        if (copiedNode == null) {
            return false;
        }
        IProcess curNodeProcess = copiedNode.getProcess();
        if (curNodeProcess != null) {
            List<? extends INode> graphicalNodes = curNodeProcess.getGraphicalNodes();
            if (graphicalNodes != null) {
                for (INode node : graphicalNodes) {
                    if (node == copiedNode) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void createNodeContainerList() {
        int firstIndex = 0;
        int index = 0;
        nodeContainerList = new ArrayList<NodeContainer>();
        connections = new ArrayList<Connection>();
        createdNames = new ArrayList<String>();
        Map<String, String> oldNameTonewNameMap = new HashMap<String, String>();
        Map<String, String> oldMetaToNewMeta = new HashMap<String, String>();

        // see bug 0004882: Subjob title is not copied when copying/pasting subjobs from one job to another
        Map<Node, SubjobContainer> mapping = new HashMap<Node, SubjobContainer>();
        // create the nodes
        for (NodePart copiedNodePart : nodeParts) {
            Node copiedNode = (Node) copiedNodePart.getModel();
            if (!containNodeInProcess(copiedNode)) {
                continue;
            }
            IComponent component = ComponentsFactoryProvider.getInstance().get(copiedNode.getComponent().getName());
            if (component == null) {
                component = copiedNode.getComponent();
            }
            Node pastedNode = new Node(component, process);
            if (isJobletRefactor()) { // keep original for joblet refactor.
                process.removeUniqueNodeName(pastedNode.getUniqueName());
                pastedNode.setPropertyValue(EParameterName.UNIQUE_NAME.getName(), copiedNode.getUniqueName());
                process.addUniqueNodeName(copiedNode.getUniqueName());
            }
            // for bug 0004882: Subjob title is not copied when copying/pasting subjobs from one job to another
            makeCopyNodeAndSubjobMapping(copiedNode, pastedNode, mapping);

            Point location = null;
            if (getCursorLocation() == null) {
                location = copiedNode.getLocation();
            } else {
                location = getCursorLocation();
                index = nodeParts.indexOf(copiedNodePart);
            }

            if (process.isGridEnabled()) {
                // replace the component to set it on the grid if it's enabled
                int tempVar = location.x / TalendEditor.GRID_SIZE;
                location.x = tempVar * TalendEditor.GRID_SIZE;
                tempVar = location.y / TalendEditor.GRID_SIZE;
                location.y = tempVar * TalendEditor.GRID_SIZE;
            }
            pastedNode.setLocation(findLocationForNode(location, copiedNode.getSize(), index, firstIndex, copiedNodePart));
            pastedNode.setSize(copiedNode.getSize());

            INodeConnector mainConnector;
            if (pastedNode.isELTComponent()) {
                mainConnector = pastedNode.getConnectorFromType(EConnectionType.TABLE);
            } else {
                mainConnector = pastedNode.getConnectorFromType(EConnectionType.FLOW_MAIN);
            }

            if (!mainConnector.isMultiSchema()) {
                if (copiedNode.getMetadataList().size() != 0) {
                    pastedNode.getMetadataList().clear();
                    for (IMetadataTable metaTable : copiedNode.getMetadataList()) {
                        IMetadataTable newMetaTable = metaTable.clone();
                        if (metaTable.getTableName().equals(copiedNode.getUniqueName())) {
                            newMetaTable.setTableName(pastedNode.getUniqueName());
                        }
                        for (IMetadataColumn column : metaTable.getListColumns()) {
                            if (column.isCustom()) {
                                IMetadataColumn newColumn = newMetaTable.getColumn(column.getLabel());
                                newColumn.setReadOnly(column.isReadOnly());
                                newColumn.setCustom(column.isCustom());
                            }
                        }
                        pastedNode.getMetadataList().add(newMetaTable);
                    }
                }
            } else {
                List<IMetadataTable> copyOfMetadataList = new ArrayList<IMetadataTable>();
                for (IMetadataTable metaTable : copiedNode.getMetadataList()) {
                    IMetadataTable newTable = metaTable.clone();
                    if (copiedNode.isELTComponent()) {
                        newTable.setTableName(createNewConnectionName(metaTable.getTableName(),
                                Process.DEFAULT_TABLE_CONNECTION_NAME));
                    } else {
                        newTable.setTableName(createNewConnectionName(metaTable.getTableName(), null));
                    }
                    oldMetaToNewMeta.put(pastedNode.getUniqueName() + ":" + metaTable.getTableName(), newTable.getTableName()); //$NON-NLS-1$

                    for (IMetadataColumn column : metaTable.getListColumns()) {
                        if (column.isCustom()) {
                            IMetadataColumn newColumn = newTable.getColumn(column.getLabel());
                            newColumn.setReadOnly(column.isReadOnly());
                            newColumn.setCustom(column.isCustom());
                        }
                    }
                    newTable.sortCustomColumns();
                    copyOfMetadataList.add(newTable);
                }
                pastedNode.setMetadataList(copyOfMetadataList);
                IExternalNode externalNode = pastedNode.getExternalNode();
                if (externalNode != null) {
                    if (copiedNode.getExternalData() != null) {
                        try {
                            externalNode.setExternalData(copiedNode.getExternalData().clone());
                        } catch (CloneNotSupportedException e) {
                            ExceptionHandler.process(e);
                        }
                        pastedNode.setExternalData(externalNode.getExternalData());
                    }
                    for (IMetadataTable metaTable : copiedNode.getMetadataList()) {
                        String oldName = metaTable.getTableName();
                        String newName = oldMetaToNewMeta.get(pastedNode.getUniqueName() + ":" + metaTable.getTableName()); //$NON-NLS-1$
                        externalNode.renameOutputConnection(oldName, newName);
                    }
                }
            }
            pastedNode.getNodeLabel().setOffset(new Point(copiedNode.getNodeLabel().getOffset()));
            oldNameTonewNameMap.put(copiedNode.getUniqueName(), pastedNode.getUniqueName());
            if (copiedNode.getElementParametersWithChildrens() != null) {
                for (ElementParameter param : (List<ElementParameter>) copiedNode.getElementParametersWithChildrens()) {
                    if (!EParameterName.UNIQUE_NAME.getName().equals(param.getName())) {
                        IElementParameter elementParameter = pastedNode.getElementParameter(param.getName());
                        if (param.getField() == EParameterFieldType.TABLE) {
                            List<Map<String, Object>> tableValues = (List<Map<String, Object>>) param.getValue();
                            ArrayList newValues = new ArrayList();
                            for (Map<String, Object> map : tableValues) {
                                Map<String, Object> newMap = new HashMap<String, Object>();
                                newMap.putAll(map);
                                newValues.add(newMap);
                            }
                            elementParameter.setValue(newValues);
                        } else {
                            if (param.getParentParameter() != null) {
                                String parentName = param.getParentParameter().getName();
                                pastedNode.setPropertyValue(parentName + ":" + param.getName(), param.getValue()); //$NON-NLS-1$
                            } else {
                                Object value = param.getValue();
                                if (value instanceof String) {
                                    String copiedParamValue = (String) value;
                                    // noly rename global variables that exist in copied nodes
                                    for (NodePart part : nodeParts) {
                                        Node node = (Node) part.getModel();
                                        for (INodeReturn returns : node.getReturns()) {
                                            String copiedVarName = ElementParameterParser.parse(node, returns.getVarName());
                                            if (copiedParamValue.indexOf(copiedVarName) != -1) {
                                                String newValue = copiedVarName.replace(node.getUniqueName(), pastedNode
                                                        .getUniqueName());
                                                copiedParamValue = copiedParamValue.replace(copiedVarName, newValue);
                                                value = copiedParamValue;
                                            }
                                        }
                                    }
                                }
                                pastedNode.setPropertyValue(param.getName(), value);
                                // See Bug 0005722: the pasted component don't keep the same read-only mode and didn;t
                                // hide
                                // the password.
                                elementParameter.setReadOnly(param.isReadOnly());
                                elementParameter.setRepositoryValueUsed(param.isRepositoryValueUsed());
                            }
                        }
                    }
                }
            }
            nodeContainerList.add(new NodeContainer(pastedNode));
        }
        process.setCopyPasteSubjobMappings(mapping);
        Map<String, String> oldToNewConnVarMap = new HashMap<String, String>();

        // add the connections
        for (NodePart copiedNodePart : nodeParts) {
            Node copiedNode = (Node) copiedNodePart.getModel();
            for (Connection connection : (List<Connection>) copiedNode.getOutgoingConnections()) {
                Node pastedTargetNode = null, pastedSourceNode = null;

                String nodeSource = oldNameTonewNameMap.get(copiedNode.getUniqueName());
                for (NodeContainer nodeContainer : nodeContainerList) {
                    Node node = nodeContainer.getNode();
                    if (node.getUniqueName().equals(nodeSource)) {
                        pastedSourceNode = node;
                    }
                }

                Node targetNode = connection.getTarget();
                // test if the target is in the nodes to paste to add the
                // connection
                // if the targeted node is not in the nodes to paste, then the
                // string will be null
                String nodeToConnect = oldNameTonewNameMap.get(targetNode.getUniqueName());
                if (nodeToConnect != null) {
                    for (NodeContainer nodeContainer : nodeContainerList) {
                        Node node = nodeContainer.getNode();
                        if (node.getUniqueName().equals(nodeToConnect)) {
                            pastedTargetNode = node;
                        }
                    }
                }
                if ((pastedSourceNode != null) && (pastedTargetNode != null)) {
                    String newConnectionName;
                    String metaTableName;

                    if (connection.getLineStyle().hasConnectionCategory(IConnectionCategory.UNIQUE_NAME)
                            && connection.getLineStyle().hasConnectionCategory(IConnectionCategory.FLOW)) {
                        String newNameBuiltIn = oldMetaToNewMeta.get(pastedSourceNode.getUniqueName() + ":" //$NON-NLS-1$
                                + connection.getMetaName());
                        if (newNameBuiltIn == null) {
                            IElementParameter formatParam = pastedSourceNode.getElementParameter(EParameterName.CONNECTION_FORMAT
                                    .getName());
                            String baseName = Process.DEFAULT_ROW_CONNECTION_NAME;
                            if (formatParam != null) {
                                String value = (String) formatParam.getValue();
                                if (value != null && !"".equals(value)) { //$NON-NLS-1$
                                    baseName = value;
                                }
                            }
                            if (process.checkValidConnectionName(connection.getName(), true)) {
                                baseName = null; // keep the name, bug 5086
                            }
                            newConnectionName = createNewConnectionName(connection.getName(), baseName);
                        } else {
                            newConnectionName = newNameBuiltIn;
                        }
                    } else {
                        newConnectionName = connection.getName();
                    }

                    String meta = oldMetaToNewMeta.get(pastedSourceNode.getUniqueName() + ":" + connection.getMetaName()); //$NON-NLS-1$
                    if (meta != null) {
                        if (pastedSourceNode.getConnectorFromType(connection.getLineStyle()).isMultiSchema()
                                && !connection.getLineStyle().equals(EConnectionType.TABLE)) {
                            newConnectionName = meta;
                        }
                        metaTableName = meta;
                    } else {
                        if (pastedSourceNode.getConnectorFromType(connection.getLineStyle()).isMultiSchema()) {
                            metaTableName = pastedSourceNode.getMetadataList().get(0).getTableName();
                        } else {
                            metaTableName = pastedSourceNode.getUniqueName(); // connection.getMetaName();
                        }
                    }
                    Connection pastedConnection;
                    if (!pastedTargetNode.isELTComponent()) {
                        pastedConnection = new Connection(pastedSourceNode, pastedTargetNode, connection.getLineStyle(),
                                connection.getConnectorName(), metaTableName, newConnectionName, connection.isMonitorConnection());
                    } else {
                        pastedConnection = new Connection(pastedSourceNode, pastedTargetNode, connection.getLineStyle(),
                                connection.getConnectorName(), metaTableName, newConnectionName, metaTableName, connection
                                        .isMonitorConnection());
                    }

                    connections.add(pastedConnection);

                    // pastedConnection.setActivate(pastedSourceNode.isActivate());
                    for (ElementParameter param : (List<ElementParameter>) connection.getElementParameters()) {
                        // pastedConnection.getElementParameter(param.getName())
                        // .setValue(param.getValue());
                        pastedConnection.setPropertyValue(param.getName(), param.getValue());
                    }

                    // // keep the label (bug 3778)
                    // if (pastedConnection != null) {
                    // if (pastedConnection.getSourceNodeConnector().isBuiltIn()
                    // && pastedConnection.getLineStyle().hasConnectionCategory(EConnectionType.FLOW)) {
                    // pastedConnection.setPropertyValue(EParameterName.LABEL.getName(), connection.getName());
                    // } else {
                    // pastedConnection.setPropertyValue(EParameterName.LABEL.getName(), newConnectionName);
                    // }
                    // }

                    pastedConnection.getConnectionLabel().setOffset(new Point(connection.getConnectionLabel().getOffset()));
                    INodeConnector connector = pastedConnection.getSourceNodeConnector();
                    connector.setCurLinkNbOutput(connector.getCurLinkNbOutput() + 1);
                    connector = pastedConnection.getTargetNodeConnector();
                    connector.setCurLinkNbInput(connector.getCurLinkNbInput() + 1);
                    IExternalNode externalNode = pastedTargetNode.getExternalNode();
                    if (externalNode != null) {
                        externalNode.renameInputConnection(connection.getName(), newConnectionName);
                    }

                    // (feature 2962)
                    if (pastedConnection.getMetadataTable() == null) {
                        continue;
                    }
                    for (IMetadataColumn column : pastedConnection.getMetadataTable().getListColumns()) {
                        String oldConnVar = connection.getName() + "." + column.getLabel(); //$NON-NLS-1$
                        String newConnVar = newConnectionName + "." + column.getLabel(); //$NON-NLS-1$
                        // String oldConnVar = connection.getName();
                        // String newConnVar = newConnectionName;
                        if (!oldToNewConnVarMap.containsKey(oldConnVar)) {
                            oldToNewConnVarMap.put(oldConnVar, newConnVar);
                        }
                    }
                }
            }
        }

        // rename the connection data for node parameters. (feature 2962)
        for (NodeContainer nodeContainer : nodeContainerList) {
            Node node = nodeContainer.getNode();
            for (String oldConnVar : oldToNewConnVarMap.keySet()) {
                String newConnVar = oldToNewConnVarMap.get(oldConnVar);
                if (newConnVar != null) {
                    node.renameData(oldConnVar, newConnVar);
                }
            }

        }

        // check if the new components use the old components name.
        Map<String, Set<String>> usedDataMap = new HashMap<String, Set<String>>();
        for (NodeContainer nodeContainer : nodeContainerList) {
            Node currentNode = nodeContainer.getNode();
            String uniqueName = currentNode.getUniqueName();
            for (String oldName : oldNameTonewNameMap.keySet()) {
                if (usedDataMap != null && usedDataMap.get(uniqueName) != null
                        && !oldName.equals(oldNameTonewNameMap.get(oldName)) && currentNode.useData(oldName)) {
                    Set<String> oldNameSet = usedDataMap.get(uniqueName);
                    if (oldNameSet == null) {
                        oldNameSet = new HashSet<String>();
                        usedDataMap.put(uniqueName, oldNameSet);
                    }
                    oldNameSet.add(oldName);
                }
            }
        }

        // check if the new connections use the old components name.
        Map<String, Set<String>> usedDataMapForConnections = new HashMap<String, Set<String>>();
        for (Connection connection : connections) {
            String uniqueName = connection.getUniqueName();
            for (String oldName : oldNameTonewNameMap.keySet()) {
                if (oldNameTonewNameMap != null && oldNameTonewNameMap.get(oldName) != null && oldName != null
                        && !oldName.equals(oldNameTonewNameMap.get(oldName))
                        && UpgradeElementHelper.isUseData(connection, oldName)) {
                    Set<String> oldNameSet = usedDataMapForConnections.get(uniqueName);
                    if (oldNameSet == null) {
                        oldNameSet = new HashSet<String>();
                        usedDataMapForConnections.put(uniqueName, oldNameSet);
                    }
                    oldNameSet.add(oldName);
                }
            }
        }

        if (!usedDataMap.isEmpty() || !usedDataMapForConnections.isEmpty()) {
            MessageBox msgBox = new MessageBox(PlatformUI.getWorkbench().getDisplay().getActiveShell(), SWT.YES | SWT.NO
                    | SWT.ICON_WARNING);
            msgBox.setMessage(Messages.getString("NodesPasteCommand.renameMessages")); //$NON-NLS-1$
            if (msgBox.open() == SWT.YES) {
                for (NodeContainer nodeContainer : nodeContainerList) {
                    Node currentNode = nodeContainer.getNode();
                    Set<String> oldNameSet = usedDataMap.get(currentNode.getUniqueName());
                    if (oldNameSet != null && !oldNameSet.isEmpty()) {
                        for (String oldName : oldNameSet) {
                            currentNode.renameData(oldName, oldNameTonewNameMap.get(oldName));
                        }
                    }
                }

                // Rename connections
                for (Connection connection : connections) {
                    Set<String> oldNameSet = usedDataMapForConnections.get(connection.getUniqueName());
                    if (oldNameSet != null && !oldNameSet.isEmpty()) {
                        for (String oldName : oldNameSet) {
                            UpgradeElementHelper.renameData(connection, oldName, oldNameTonewNameMap.get(oldName));
                        }
                    }
                }
            }
        }
    }

    /**
     * DOC bqian Comment method "makeCopyNodeAndSubjobMapping".<br>
     * see bug 0004882: Subjob title is not copied when copying/pasting subjobs from one job to another
     * 
     * @param copiedNode
     * @param pastedNode
     */
    private void makeCopyNodeAndSubjobMapping(Node copiedNode, Node pastedNode, Map<Node, SubjobContainer> mapping) {
        for (SubjobContainerPart subjobPart : subjobParts) {
            SubjobContainer subjob = (SubjobContainer) subjobPart.getModel();
            if (subjob != null && subjob.getSubjobStartNode() != null && subjob.getSubjobStartNode().equals(copiedNode)) {
                mapping.put(pastedNode, subjob);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute() {
        // create the node container list to paste
        createNodeContainerList();
        AbstractMultiPageTalendEditor multiPageTalendEditor = (AbstractMultiPageTalendEditor) PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        GraphicalViewer viewer = multiPageTalendEditor.getTalendEditor().getViewer();
        // save old selection
        if (!multipleCommand) {
            oldSelection = new ArrayList<EditPart>();
            for (EditPart editPart : (List<EditPart>) viewer.getSelectedEditParts()) {
                oldSelection.add(editPart);
            }
            // remove the old selection
            viewer.deselectAll();
        }
        // creates the different nodes
        for (NodeContainer nodeContainer : nodeContainerList) {
            process.addNodeContainer(nodeContainer);
        }
        // check that the created connections exists now, or create them if needed
        for (String newConnectionName : createdNames) {
            if (process.checkValidConnectionName(newConnectionName, true)) {
                process.addUniqueConnectionName(newConnectionName);
            }
        }
        process.checkStartNodes();
        process.checkProcess();

        // set the new node as the current selection
        if (!multipleCommand) {
            EditPart processPart = (EditPart) viewer.getRootEditPart().getChildren().get(0);
            if (processPart instanceof ProcessPart) { // can only be
                // ProcessPart but still
                // test
                List<EditPart> sel = new ArrayList<EditPart>();
                for (EditPart editPart : (List<EditPart>) processPart.getChildren()) {
                    if (editPart instanceof SubjobContainerPart) {
                        for (EditPart subjobChildsPart : (List<EditPart>) editPart.getChildren()) {
                            if (subjobChildsPart instanceof NodeContainerPart) {
                                if (nodeContainerList.contains(((NodeContainerPart) subjobChildsPart).getModel())) {
                                    NodePart nodePart = ((NodeContainerPart) subjobChildsPart).getNodePart();
                                    if (nodePart != null) {
                                        sel.add(nodePart);
                                    }
                                }
                            }
                        }
                    }
                    if (editPart instanceof NodePart) {
                        Node currentNode = (Node) editPart.getModel();
                        if (nodeContainerList.contains(currentNode.getNodeContainer())) {
                            sel.add(editPart);
                        }
                    }
                }
                StructuredSelection s = new StructuredSelection(sel);
                viewer.setSelection(s);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void undo() {
        // remove the current selection
        AbstractMultiPageTalendEditor multiPageTalendEditor = (AbstractMultiPageTalendEditor) PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        GraphicalViewer viewer = multiPageTalendEditor.getTalendEditor().getViewer();
        if (!multipleCommand) {
            viewer.deselectAll();
        }

        for (NodeContainer nodeContainer : nodeContainerList) {
            // remove the connections name from the list
            for (Connection connection : (List<Connection>) nodeContainer.getNode().getOutgoingConnections()) {
                process.removeUniqueConnectionName(connection.getName());
            }
            process.removeNodeContainer(nodeContainer);
        }

        // check that the created connections are removed, remove them if not
        for (String newConnectionName : createdNames) {
            if (!process.checkValidConnectionName(newConnectionName, true)) {
                process.removeUniqueConnectionName(newConnectionName);
            }
        }

        process.checkStartNodes();
        process.checkProcess();

        // set the old selection active
        if (!multipleCommand) {
            StructuredSelection s = new StructuredSelection(oldSelection);
            viewer.setSelection(s);
        }
    }

    /**
     * Getter for multipleCommand.
     * 
     * @return the multipleCommand
     */
    public boolean isMultipleCommand() {
        return multipleCommand;
    }

    /**
     * Sets the multipleCommand.
     * 
     * @param multipleCommand the multipleCommand to set
     */
    public void setMultipleCommand(boolean multipleCommand) {
        this.multipleCommand = multipleCommand;
    }

    /**
     * Getter for nodeContainerList.
     * 
     * @return the nodeContainerList
     */
    public List<NodeContainer> getNodeContainerList() {
        return nodeContainerList;
    }

    /**
     * bqian Comment method "setSelectedSubjobs". <br>
     * see bug 0004882: Subjob title is not copied when copying/pasting subjobs from one job to another
     * 
     * @param subjobParts
     */
    public void setSelectedSubjobs(List<SubjobContainerPart> subjobParts) {
        this.subjobParts = subjobParts;
    }
}
