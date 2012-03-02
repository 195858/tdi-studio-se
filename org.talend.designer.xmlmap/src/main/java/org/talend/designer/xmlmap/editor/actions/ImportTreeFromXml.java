// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.xmlmap.editor.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.talend.designer.xmlmap.model.emf.xmlmap.AbstractInOutTree;
import org.talend.designer.xmlmap.model.emf.xmlmap.InputXmlTree;
import org.talend.designer.xmlmap.model.emf.xmlmap.NodeType;
import org.talend.designer.xmlmap.model.emf.xmlmap.OutputXmlTree;
import org.talend.designer.xmlmap.model.emf.xmlmap.TreeNode;
import org.talend.designer.xmlmap.model.emf.xmlmap.XmlmapFactory;
import org.talend.designer.xmlmap.parts.TreeNodeEditPart;
import org.talend.designer.xmlmap.ui.tabs.MapperManager;
import org.talend.designer.xmlmap.util.XmlMapUtil;
import org.talend.repository.ui.wizards.metadata.connection.files.xml.treeNode.Attribute;
import org.talend.repository.ui.wizards.metadata.connection.files.xml.treeNode.Element;
import org.talend.repository.ui.wizards.metadata.connection.files.xml.treeNode.FOXTreeNode;
import org.talend.repository.ui.wizards.metadata.connection.files.xml.treeNode.NameSpaceNode;
import org.talend.repository.ui.wizards.metadata.connection.files.xml.util.TreeUtil;

/**
 * wchen class global comment. Detailled comment
 */
public class ImportTreeFromXml extends SelectionAction {

    private TreeNode parentNode;

    private MapperManager mapperManager;

    private Shell shell;

    private boolean input;

    public static final String ID = "org.talend.designer.xmlmap.editor.actions.ImportTreeFromXml";

    public ImportTreeFromXml(IWorkbenchPart part, Shell shell) {
        super(part);
        this.shell = shell;
        setId(ID);
        setText("Import From File");
    }

    @Override
    public void run() {
        List<FOXTreeNode> list = new ArrayList<FOXTreeNode>();
        FileDialog f = new FileDialog(shell);
        String file = f.open();
        if (file != null) {
            boolean clickOk = TreeUtil.getFoxTreeNodesForXmlMap(file, shell, list);
            if (clickOk) {
                TreeNode treeNodeRoot = XmlMapUtil.getTreeNodeRoot(parentNode);
                XmlMapUtil.detachNodeConnections(treeNodeRoot, mapperManager.getCopyOfMapData(), true);
                parentNode.getChildren().clear();
                prepareEmfTreeNode(list, parentNode);

                if (parentNode.getChildren().isEmpty()) {
                    TreeNode rootNode = null;
                    if (input) {
                        rootNode = XmlmapFactory.eINSTANCE.createTreeNode();
                    } else {
                        rootNode = XmlmapFactory.eINSTANCE.createOutputTreeNode();
                    }
                    rootNode.setName("root");
                    rootNode.setNodeType(NodeType.ELEMENT);
                    rootNode.setType(XmlMapUtil.DEFAULT_DATA_TYPE);
                    rootNode.setXpath(XmlMapUtil.getXPath(parentNode.getXpath(), "root", NodeType.ELEMENT));
                    parentNode.getChildren().add(rootNode);
                    showError();
                }
                // loop / main
                parentNode.getChildren().get(0).setLoop(true);
                parentNode.getChildren().get(0).setMain(true);

                if (parentNode.eContainer() instanceof InputXmlTree) {
                    mapperManager.refreshInputTreeSchemaEditor((InputXmlTree) parentNode.eContainer());
                } else if (parentNode.eContainer() instanceof OutputXmlTree) {
                    mapperManager.refreshOutputTreeSchemaEditor((OutputXmlTree) parentNode.eContainer());
                }
                if (treeNodeRoot.eContainer() instanceof AbstractInOutTree) {
                    mapperManager.getProblemsAnalyser().checkLoopProblems((AbstractInOutTree) treeNodeRoot.eContainer());
                    mapperManager.getMapperUI().updateStatusBar();
                }
            }
        }

    }

    private void prepareEmfTreeNode(List<FOXTreeNode> list, TreeNode parent) {
        if (list == null || list.isEmpty()) {
            return;
        }
        String xPath = parent.getXpath();
        TreeNode createTreeNode = null;
        for (FOXTreeNode foxNode : list) {
            if (input) {
                createTreeNode = XmlmapFactory.eINSTANCE.createTreeNode();
            } else {
                createTreeNode = XmlmapFactory.eINSTANCE.createOutputTreeNode();
            }
            createTreeNode.setName(foxNode.getLabel());
            if (foxNode instanceof Element) {
                createTreeNode.setNodeType(NodeType.ELEMENT);
            } else if (foxNode instanceof Attribute) {
                createTreeNode.setNodeType(NodeType.ATTRIBUT);
            } else if (foxNode instanceof NameSpaceNode) {
                createTreeNode.setNodeType(NodeType.NAME_SPACE);
                createTreeNode.setDefaultValue(foxNode.getDefaultValue());
                if (createTreeNode.getName() == null || createTreeNode.getName().equals("")) {
                    createTreeNode.setName(XmlMapUtil.DEFAULT_NAME_SPACE_PREFIX);
                }
            }
            createTreeNode.setXpath(XmlMapUtil.getXPath(xPath, createTreeNode.getName(), createTreeNode.getNodeType()));
            if (foxNode.getDataType() != null && "".equals(foxNode.getDataType())) {
                createTreeNode.setType(foxNode.getDataType());
            } else {
                createTreeNode.setType(XmlMapUtil.DEFAULT_DATA_TYPE);
            }
            parent.getChildren().add(createTreeNode);
            if (foxNode.getChildren() != null && !foxNode.getChildren().isEmpty()) {
                prepareEmfTreeNode(foxNode.getChildren(), createTreeNode);
            }
        }

    }

    public void setMapperManager(MapperManager mapperManager) {
        this.mapperManager = mapperManager;
    }

    @Override
    protected boolean calculateEnabled() {
        if (getSelectedObjects().isEmpty()) {
            return false;
        } else {
            Object object = getSelectedObjects().get(0);
            if (object instanceof TreeNodeEditPart) {
                TreeNodeEditPart parentPart = (TreeNodeEditPart) object;
                parentNode = (TreeNode) parentPart.getModel();
                if (parentNode.eContainer() instanceof AbstractInOutTree && XmlMapUtil.DOCUMENT.equals(parentNode.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void update(Object selection) {
        setSelection(new StructuredSelection(selection));
    }

    protected void showError() {
        MessageDialog.openError(null, "Error", "Import fail, please check your xml file!");
    }

    public boolean isInput() {
        return input;
    }

    public void setInput(boolean input) {
        this.input = input;
    }

}
