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

import java.util.List;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.requests.DirectEditRequest;
import org.talend.designer.xmlmap.figures.OutputXmlTreeFigure;
import org.talend.designer.xmlmap.figures.cells.IWidgetCell;
import org.talend.designer.xmlmap.model.emf.xmlmap.InputXmlTree;
import org.talend.designer.xmlmap.model.emf.xmlmap.OutputXmlTree;
import org.talend.designer.xmlmap.model.emf.xmlmap.XmlmapPackage;
import org.talend.designer.xmlmap.parts.directedit.XmlMapNodeCellEditorLocator;
import org.talend.designer.xmlmap.parts.directedit.XmlMapNodeDirectEditManager;

/**
 * wchen class global comment. Detailled comment
 */
public class OutputXmlTreeEditPart extends AbstractInOutTreeEditPart {

    private OutputXmlTreeFigure figure;

    private XmlMapNodeDirectEditManager directEditManager;

    @Override
    protected IFigure createFigure() {
        figure = new OutputXmlTreeFigure(this);
        return figure;
    }

    @Override
    public IFigure getContentPane() {
        return ((OutputXmlTreeFigure) getFigure()).getColumnContainer();
    }

    @Override
    protected List getModelChildren() {
        return ((OutputXmlTree) getModel()).getNodes();
    }

    @Override
    public void notifyChanged(Notification notification) {
        int type = notification.getEventType();
        int featureId = notification.getFeatureID(XmlmapPackage.class);
        switch (type) {
        case Notification.ADD:
        case Notification.REMOVE:
        case Notification.REMOVE_MANY:
            switch (featureId) {
            case XmlmapPackage.OUTPUT_XML_TREE__NODES:
                refreshChildren();
                break;
            case XmlmapPackage.ABSTRACT_IN_OUT_TREE__FILTER_INCOMING_CONNECTIONS:
                refreshTargetConnections();
                break;
            }
            break;
        case Notification.SET:
            switch (featureId) {
            case XmlmapPackage.OUTPUT_XML_TREE__NODES:
                refreshChildren();
            case XmlmapPackage.OUTPUT_XML_TREE__REJECT:
            case XmlmapPackage.OUTPUT_XML_TREE__REJECT_INNER_JOIN:
            case XmlmapPackage.OUTPUT_XML_TREE__EXPRESSION_FILTER:
            case XmlmapPackage.OUTPUT_XML_TREE__MINIMIZED:
            case XmlmapPackage.OUTPUT_XML_TREE__ALL_IN_ONE:
            case XmlmapPackage.OUTPUT_XML_TREE__ENABLE_EMPTY_ELEMENT:
                ((OutputXmlTreeFigure) getFigure()).update(featureId);
                break;
            case XmlmapPackage.TREE_NODE__EXPRESSION:
                ((OutputXmlTreeFigure) getFigure()).update(XmlmapPackage.TREE_NODE__TYPE);
                break;
            case XmlmapPackage.ABSTRACT_IN_OUT_TREE__MULTI_LOOPS:
                // fix for TDI-20808 , clean aggreage and groups if it's multiloops
                OutputXmlTree model = (OutputXmlTree) getModel();
                boolean changed = false;
                if (model.isMultiLoops()) {
                    changed = cleanGroup(model.getNodes());

                }
                if (model.isMultiLoops() && getParent() instanceof XmlMapDataEditPart) {
                    List childPart = ((XmlMapDataEditPart) getParent()).getChildren();
                    for (Object o : childPart) {
                        if (o instanceof InputXmlTreeEditPart) {
                            InputXmlTree inputTree = (InputXmlTree) ((InputXmlTreeEditPart) o).getModel();
                            if (inputTree.isMultiLoops() && !inputTree.isLookup()) {
                                changed = cleanAggregate(model.getNodes()) || changed;
                                break;
                            }
                        }
                    }
                }
                if (changed) {
                    refreshChildren();
                }
            }
        }
    }

    @Override
    public void performRequest(Request req) {
        if (RequestConstants.REQ_DIRECT_EDIT.equals(req.getType())) {
            DirectEditRequest drequest = (DirectEditRequest) req;
            Point figureLocation = drequest.getLocation();

            IFigure findFigureAt = getFigure().findFigureAt(figureLocation.x, figureLocation.y);

            if (findFigureAt instanceof IWidgetCell) {
                directEditManager = new XmlMapNodeDirectEditManager(this, new XmlMapNodeCellEditorLocator((Figure) findFigureAt));
                directEditManager.show();
            }
        }

    }

}
