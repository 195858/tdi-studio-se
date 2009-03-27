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
package org.talend.designer.fileoutputxml.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.IConnectionCategory;
import org.talend.core.model.utils.NodeUtil;
import org.talend.designer.fileoutputxml.FileOutputXMLComponent;
import org.talend.designer.fileoutputxml.data.Attribute;
import org.talend.designer.fileoutputxml.data.Element;
import org.talend.designer.fileoutputxml.data.FOXTreeNode;
import org.talend.designer.fileoutputxml.data.NameSpaceNode;
import org.talend.designer.fileoutputxml.util.TreeUtil;

/**
 * wzhang class global comment. Detailled comment
 */
public class MultiFOXManager extends FOXManager {

    /**
     * wzhang FOXMultiManager constructor comment.
     * 
     * @param foxComponent
     */
    public MultiFOXManager(FileOutputXMLComponent foxComponent) {
        super(foxComponent);
        uiManager = new MultiUIManager(this);
    }

    public List<IMetadataTable> getMultiSchemaData() {
        List<IMetadataTable> schemas = new ArrayList<IMetadataTable>();

        List<? extends IConnection> incomingConnections = NodeUtil.getIncomingConnections(foxComponent, IConnectionCategory.FLOW);
        for (IConnection connection : incomingConnections) {
            connection.getMetadataTable().setLabel(connection.getUniqueName());
            schemas.add(connection.getMetadataTable());
        }
        return schemas;
    }

    public void initModel() {
        int i = 0;
        List<? extends IConnection> incomingConnections = NodeUtil.getIncomingConnections(foxComponent, IConnectionCategory.FLOW);
        for (IConnection connection : incomingConnections) {
            IMetadataTable metadataTable = connection.getMetadataTable();
            metadataTable.setLabel(connection.getUniqueName());
            treeData = new ArrayList<FOXTreeNode>();
            if (i == 0)// the first schema as current
                currentSchema = metadataTable.getLabel();
            FOXTreeNode rootNode = null;
            FOXTreeNode current = null;
            FOXTreeNode temp = null;
            FOXTreeNode mainNode = null;
            String mainPath = null;
            String currentPath = null;

            String schemaId = metadataTable.getLabel() + ":"; //$NON-NLS-1$

            // build root tree
            List<Map<String, String>> rootTable = (List<Map<String, String>>) foxComponent
                    .getTableList(FileOutputXMLComponent.ROOT);
            for (Map<String, String> rootMap : rootTable) {
                String newPath = rootMap.get(FileOutputXMLComponent.PATH);
                String columnName = rootMap.get(FileOutputXMLComponent.COLUMN);
                if (columnName != null && columnName.length() > 0 && !columnName.startsWith(metadataTable.getLabel())) {
                    continue;
                }
                if (rootMap.get(FileOutputXMLComponent.ATTRIBUTE).equals("attri")) { //$NON-NLS-1$
                    temp = new Attribute(newPath);
                    current.addChild(temp);
                } else if (rootMap.get(FileOutputXMLComponent.ATTRIBUTE).equals("ns")) { //$NON-NLS-1$
                    temp = new NameSpaceNode(newPath);
                    current.addChild(temp);
                } else {
                    temp = addElement(current, currentPath, newPath);
                    if (rootNode == null) {
                        rootNode = temp;
                    }
                    if (rootMap.get(FileOutputXMLComponent.ATTRIBUTE).equals("main")) { //$NON-NLS-1$
                        temp.setMain(true);
                        mainNode = temp;
                        mainPath = newPath;
                    }
                    current = temp;
                    currentPath = newPath;
                }
                temp.setRow(metadataTable.getLabel());
                if (columnName != null && columnName.length() > 0 && columnName.startsWith(schemaId)) {
                    columnName = columnName.replace(schemaId, ""); //$NON-NLS-1$
                    temp.setColumn(metadataTable.getColumn(columnName));
                    temp.setTable(metadataTable);
                }
            }

            // build group tree
            current = mainNode;
            currentPath = mainPath;
            boolean isFirst = true;
            List<Map<String, String>> groupTable = (List<Map<String, String>>) foxComponent
                    .getTableList(FileOutputXMLComponent.GROUP);
            for (Map<String, String> groupMap : groupTable) {
                String newPath = groupMap.get(FileOutputXMLComponent.PATH);
                String columnName = groupMap.get(FileOutputXMLComponent.COLUMN);
                if (columnName != null && columnName.length() > 0 && !columnName.startsWith(metadataTable.getLabel())) {
                    continue;
                }
                if (groupMap.get(FileOutputXMLComponent.ATTRIBUTE).equals("attri")) { //$NON-NLS-1$
                    temp = new Attribute(newPath);
                    current.addChild(temp);
                } else if (groupMap.get(FileOutputXMLComponent.ATTRIBUTE).equals("ns")) { //$NON-NLS-1$
                    temp = new NameSpaceNode(newPath);
                    current.addChild(temp);
                } else {
                    temp = this.addElement(current, currentPath, newPath);
                    if (groupMap.get(FileOutputXMLComponent.ATTRIBUTE).equals("main")) { //$NON-NLS-1$
                        temp.setMain(true);
                        mainNode = temp;
                        mainPath = newPath;
                    }
                    if (isFirst) {
                        temp.setGroup(true);
                        isFirst = false;
                    }
                    current = temp;
                    currentPath = newPath;
                }
                temp.setRow(metadataTable.getLabel());
                if (columnName != null && columnName.length() > 0 && columnName.startsWith(schemaId)) {
                    columnName = columnName.replace(schemaId, ""); //$NON-NLS-1$
                    temp.setColumn(metadataTable.getColumn(columnName));
                    temp.setTable(metadataTable);
                }
            }

            // build loop tree
            current = mainNode;
            currentPath = mainPath;
            isFirst = true;
            List<Map<String, String>> loopTable = (List<Map<String, String>>) foxComponent
                    .getTableList(FileOutputXMLComponent.LOOP);
            for (Map<String, String> loopMap : loopTable) {
                String newPath = loopMap.get(FileOutputXMLComponent.PATH);
                String columnName = loopMap.get(FileOutputXMLComponent.COLUMN);
                if (columnName != null && columnName.length() > 0 && !columnName.startsWith(metadataTable.getLabel())) {
                    continue;
                }
                if (loopMap.get(FileOutputXMLComponent.ATTRIBUTE).equals("attri")) { //$NON-NLS-1$
                    temp = new Attribute(newPath);
                    current.addChild(temp);
                } else if (loopMap.get(FileOutputXMLComponent.ATTRIBUTE).equals("ns")) { //$NON-NLS-1$
                    temp = new NameSpaceNode(newPath);
                    current.addChild(temp);
                } else {
                    temp = this.addElement(current, currentPath, newPath);
                    if (loopMap.get(FileOutputXMLComponent.ATTRIBUTE).equals("main")) { //$NON-NLS-1$
                        temp.setMain(true);
                    }
                    if (isFirst) {
                        temp.setLoop(true);
                        isFirst = false;
                    }
                    current = temp;
                    currentPath = newPath;
                }
                temp.setRow(metadataTable.getLabel());
                if (columnName != null && columnName.length() > 0 && columnName.startsWith(schemaId)) {
                    columnName = columnName.replace(schemaId, ""); //$NON-NLS-1$
                    temp.setColumn(metadataTable.getColumn(columnName));
                    temp.setTable(metadataTable);
                }
            }

            if (rootNode == null) {
                rootNode = new Element("rootTag"); //$NON-NLS-1$
            }

            rootNode.setParent(null);
            treeData.add(rootNode);
            rootNode.setRow(metadataTable.getLabel());
            contents.put(metadataTable.getLabel(), treeData);
            i++;
        }
    }

    protected void tableLoaderX(Element element, String parentPath, List<Map<String, String>> table) {
        if (element.getTable() != null) {
            String schemaId = ""; //$NON-NLS-1$
            // set parent node
            if (foxComponent.istFileOutputXMLMultiSchema()) {
                schemaId = element.getTable().getLabel() + ":"; //$NON-NLS-1$
            }

            Map<String, String> newMap = new HashMap<String, String>();
            String currentPath = parentPath + "/" + element.getLabel(); //$NON-NLS-1$
            newMap.put(FileOutputXMLComponent.PATH, currentPath);
            newMap.put(FileOutputXMLComponent.COLUMN, element.getColumnLabel());
            newMap.put(FileOutputXMLComponent.ATTRIBUTE, element.isMain() ? "main" : "branch"); //$NON-NLS-1$ //$NON-NLS-2$
            newMap.put(FileOutputXMLComponent.VALUE, ""); //$NON-NLS-1$
            table.add(newMap);
            for (FOXTreeNode att : element.getAttributeChildren()) {
                newMap = new HashMap<String, String>();
                newMap.put(FileOutputXMLComponent.PATH, att.getLabel());
                newMap.put(FileOutputXMLComponent.COLUMN, att.getColumnLabel());
                newMap.put(FileOutputXMLComponent.ATTRIBUTE, "attri"); //$NON-NLS-1$
                newMap.put(FileOutputXMLComponent.VALUE, ""); //$NON-NLS-1$
                table.add(newMap);
            }
            for (FOXTreeNode att : element.getNameSpaceChildren()) {
                newMap = new HashMap<String, String>();
                newMap.put(FileOutputXMLComponent.PATH, att.getLabel());
                newMap.put(FileOutputXMLComponent.COLUMN, att.getColumnLabel());
                newMap.put(FileOutputXMLComponent.ATTRIBUTE, "ns"); //$NON-NLS-1$
                newMap.put(FileOutputXMLComponent.VALUE, ""); //$NON-NLS-1$
                table.add(newMap);
            }
        }
        List<FOXTreeNode> children = element.getElementChildren();
        String currentPath = parentPath + "/" + element.getLabel(); //$NON-NLS-1$
        for (FOXTreeNode child : children) {
            if (!child.isGroup() && !child.isLoop()) {
                tableLoader((Element) child, currentPath, table);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.fileoutputxml.managers.FOXManager#saveDataToComponent()
     */
    @Override
    public boolean saveDataToComponent() {
        boolean result = false;
        List<Map<String, String>> root = new ArrayList<Map<String, String>>();
        List<Map<String, String>> loop = new ArrayList<Map<String, String>>();
        List<Map<String, String>> group = new ArrayList<Map<String, String>>();

        root.addAll(getRootTable());
        loop.addAll(getLoopTable());
        group.addAll(getGroupTable());

        if (foxComponent.setTableElementParameter(root, FileOutputXMLComponent.ROOT)) {
            result = true;
        }
        if (foxComponent.setTableElementParameter(loop, FileOutputXMLComponent.LOOP)) {
            result = true;
        }
        if (foxComponent.setTableElementParameter(group, FileOutputXMLComponent.GROUP)) {
            result = true;
        }
        return result;
    }

    @Override
    public List<Map<String, String>> getGroupTable() {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        for (FOXTreeNode rootNode : this.getOriginalNodes()) {
            Element groupNode = (Element) TreeUtil.getGroupNode(rootNode);
            if (groupNode != null) {
                String path = TreeUtil.getPath(groupNode);
                tableLoader(groupNode, path.substring(0, path.lastIndexOf("/")), result); //$NON-NLS-1$
            }
        }
        return result;

    }

    @Override
    public List<Map<String, String>> getLoopTable() {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        for (FOXTreeNode rootNode : this.getOriginalNodes()) {
            Element loopNode = (Element) TreeUtil.getLoopNode(rootNode);
            if (loopNode != null) {
                String path = TreeUtil.getPath(loopNode);
                tableLoader(loopNode, path.substring(0, path.lastIndexOf("/")), result); //$NON-NLS-1$
            }
        }
        return result;
    }

    @Override
    public List<Map<String, String>> getRootTable() {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        for (FOXTreeNode rootNode : this.getOriginalNodes()) {
            tableLoader((Element) rootNode, "", result); //$NON-NLS-1$
        }
        return result;
    }

}
