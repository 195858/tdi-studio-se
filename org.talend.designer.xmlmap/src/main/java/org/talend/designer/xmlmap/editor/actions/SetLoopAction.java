package org.talend.designer.xmlmap.editor.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.talend.designer.xmlmap.i18n.Messages;
import org.talend.designer.xmlmap.model.emf.xmlmap.AbstractInOutTree;
import org.talend.designer.xmlmap.model.emf.xmlmap.IConnection;
import org.talend.designer.xmlmap.model.emf.xmlmap.NodeType;
import org.talend.designer.xmlmap.model.emf.xmlmap.OutputTreeNode;
import org.talend.designer.xmlmap.model.emf.xmlmap.TreeNode;
import org.talend.designer.xmlmap.parts.TreeNodeEditPart;
import org.talend.designer.xmlmap.ui.tabs.MapperManager;
import org.talend.designer.xmlmap.util.XmlMapUtil;

public class SetLoopAction extends SelectionAction {

    public static String ID = "xml map set as loop action";

    private MapperManager mapperManager;

    private boolean isLoop = false;

    List<TreeNode> loopNodeList = new ArrayList<TreeNode>();

    // private List<TreeNode> nodesNeedToChangeMainStatus = new ArrayList<TreeNode>();

    public SetLoopAction(IWorkbenchPart part) {
        super(part);
        setId(ID);
        setText("As loop element");
    }

    @Override
    protected boolean calculateEnabled() {
        // nodesNeedToChangeMainStatus.clear();
        if (getSelectedObjects().isEmpty()) {
            return false;
        }
        if (getSelectedObjects().get(0) instanceof TreeNodeEditPart) {
            TreeNodeEditPart nodePart = (TreeNodeEditPart) getSelectedObjects().get(0);
            TreeNode model = (TreeNode) nodePart.getModel();

            if (NodeType.ATTRIBUT.equals(model.getNodeType()) || NodeType.NAME_SPACE.equals(model.getNodeType())
                    || !(model.eContainer() instanceof TreeNode)) {
                return false;
            }

            if (model.isLoop()) {
                return false;
            }

        } else {
            return false;
        }

        return true;
    }

    public void update(Object selection) {
        setSelection(new StructuredSelection(selection));
    }

    @Override
    public void run() {
        TreeNodeEditPart nodePart = (TreeNodeEditPart) getSelectedObjects().get(0);
        TreeNode model = (TreeNode) nodePart.getModel();

        AbstractInOutTree abstractTree = null;
        TreeNode docRoot = null;

        // remove old loop
        if (model instanceof OutputTreeNode) {
            OutputTreeNode outputNode = (OutputTreeNode) model;
            docRoot = (OutputTreeNode) XmlMapUtil.getTreeNodeRoot(outputNode);
            if (docRoot != null) {
                XmlMapUtil.cleanSubGroup(outputNode);
                List<TreeNode> newLoopUpGroups = new ArrayList<TreeNode>();
                findUpGroupNode(newLoopUpGroups, outputNode);
                // clean all groups except the ancestor of new loop
                XmlMapUtil.cleanSubGroup(docRoot, newLoopUpGroups);

                // reset the group in case some element ancestor of loop element are not group but under the group
                if (!newLoopUpGroups.isEmpty()) {
                    TreeNode rootGroup = newLoopUpGroups.get(newLoopUpGroups.size() - 1);
                    upsetGroup(outputNode, rootGroup);
                }
                if (docRoot.eContainer() instanceof AbstractInOutTree) {
                    abstractTree = (AbstractInOutTree) docRoot.eContainer();
                }
            }
        } else if (model instanceof TreeNode) {
            docRoot = XmlMapUtil.getTreeNodeRoot(model);
            if (docRoot.eContainer() instanceof AbstractInOutTree) {
                abstractTree = (AbstractInOutTree) docRoot.eContainer();
            }
        }
        // TDI-20147
        isLoop = false;
        checkSubElementIsLoop(model);
        if (isLoop) {
            if (MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
                    Messages.getString("SetLoopAction.cleanSubLoopTitle"),
                    Messages.getString("SetLoopAction.cleanSubLoopMessages"))) {
                cleanSubLoop(model);
            } else {
                return;
            }
        }
        checkParentElementIsLoop(nodePart);
        model.setLoop(true);
        XmlMapUtil.clearMainNode(model);
        XmlMapUtil.upsetMainNode(model);

        if (XmlMapUtil.hasDocument(abstractTree)) {
            loopNodeList.clear();
            getLoopNode(docRoot);
            if (loopNodeList != null && loopNodeList.size() > 1) {
                abstractTree.setMultiLoops(true);
            } else {
                abstractTree.setMultiLoops(false);
            }
        }
        if (abstractTree != null) {
            mapperManager.getProblemsAnalyser().checkLoopProblems(abstractTree);
            mapperManager.getMapperUI().updateStatusBar();
        }
    }

    private void cleanSubLoop(TreeNode docRoot) {
        for (TreeNode treeNode : docRoot.getChildren()) {
            if (treeNode.isLoop()) {
                treeNode.setLoop(false);
            }
            cleanSubLoop(treeNode);
        }
    }

    private boolean checkParentElementIsLoop(TreeNodeEditPart nodePart) {
        if (nodePart != null) {
            if (nodePart.getParent() instanceof TreeNodeEditPart) {
                TreeNodeEditPart nodePartTemp = (TreeNodeEditPart) nodePart.getParent();
                TreeNode model = (TreeNode) nodePartTemp.getModel();
                if (model.isLoop()) {
                    model.setLoop(false);
                    return true;
                } else {
                    checkParentElementIsLoop(nodePartTemp);
                }
            }
        }
        return false;
    }

    private void checkSubElementIsLoop(TreeNode subTreeNode) {
        if (subTreeNode == null) {
            return;
        }
        TreeNode e = subTreeNode;
        if (e.isLoop()) {
            isLoop = true;
        }
        for (TreeNode treeNode : subTreeNode.getChildren()) {
            checkSubElementIsLoop(treeNode);
        }
    }

    private void getLoopNode(TreeNode pNode) {
        if (pNode == null) {
            return;
        }
        TreeNode e = pNode;
        if (e.isLoop()) {
            loopNodeList.add(e);
        }
        for (TreeNode treeNode : pNode.getChildren()) {
            getLoopNode(treeNode);
        }
    }

    private void findUpGroupNode(List<TreeNode> newLoopUpGroups, OutputTreeNode node) {
        if (node.eContainer() instanceof OutputTreeNode) {
            OutputTreeNode parent = (OutputTreeNode) node.eContainer();
            if (parent.isGroup()) {
                newLoopUpGroups.add(parent);
            }
            findUpGroupNode(newLoopUpGroups, parent);
        }
    }

    private void upsetGroup(TreeNode node, TreeNode rootGroup) {
        if (node.eContainer() instanceof TreeNode && rootGroup != node.eContainer()) {
            TreeNode parent = (TreeNode) node.eContainer();
            if (!parent.isGroup()) {
                parent.setGroup(true);
            }
        }
    }

    public void setMapperManager(MapperManager mapperManager) {
        this.mapperManager = mapperManager;
    }

}
