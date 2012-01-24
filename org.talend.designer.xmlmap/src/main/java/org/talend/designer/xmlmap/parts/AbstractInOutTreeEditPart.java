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
package org.talend.designer.xmlmap.parts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.editpolicies.NonResizableEditPolicy;
import org.talend.designer.xmlmap.figures.anchors.FilterTreeAnchor;
import org.talend.designer.xmlmap.model.emf.xmlmap.AbstractInOutTree;
import org.talend.designer.xmlmap.policy.DragAndDropEditPolicy;
import org.talend.designer.xmlmap.policy.XmlDirectEditPolicy;

/**
 * DOC talend class global comment. Detailled comment
 */
public class AbstractInOutTreeEditPart extends BaseEditPart implements NodeEditPart {

    private boolean treeOrChildSelected = false;

    public boolean isTreeOrChildSelected() {
        return treeOrChildSelected;
    }

    public void setTreeOrChildSelected(boolean treeOrChildSelected) {
        this.treeOrChildSelected = treeOrChildSelected;
    }

    public void updateChildrenConnections(List treeNodeParts, boolean selected) {
        for (Object obj : treeNodeParts) {
            if (obj instanceof TreeNodeEditPart) {
                List connections = new ArrayList();
                TreeNodeEditPart treeNodePart = (TreeNodeEditPart) obj;
                connections.addAll(treeNodePart.getSourceConnections());
                connections.addAll(treeNodePart.getTargetConnections());
                for (Object connObj : connections) {
                    if (connObj instanceof BaseConnectionEditPart) {
                        ((BaseConnectionEditPart) connObj).updateForegroundColor(selected);
                    }
                }
                if (!treeNodePart.getChildren().isEmpty()) {
                    updateChildrenConnections(treeNodePart.getChildren(), selected);
                }
            }
        }
    }

    public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection) {
        // TODO Auto-generated method stub
        return null;
    }

    public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection) {
        if (connection instanceof FilterConnectionEditPart) {
            return new FilterTreeAnchor(getFigure(), connection, this);
        }
        return null;
    }

    public ConnectionAnchor getSourceConnectionAnchor(Request request) {
        // TODO Auto-generated method stub
        return null;
    }

    public ConnectionAnchor getTargetConnectionAnchor(Request request) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new NonResizableEditPolicy());
        installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new XmlDirectEditPolicy());
        installEditPolicy("Drag and Drop", new DragAndDropEditPolicy());
    }

    @Override
    protected List getModelTargetConnections() {
        return ((AbstractInOutTree) getModel()).getFilterIncomingConnections();
    }

}
