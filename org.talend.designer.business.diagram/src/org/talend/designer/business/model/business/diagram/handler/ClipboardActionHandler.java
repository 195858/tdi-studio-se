// ============================================================================
//
// Copyright (C) 2006-2008 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.business.model.business.diagram.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.dnd.TemplateTransfer;
import org.eclipse.gmf.runtime.common.core.command.ICommand;
import org.eclipse.gmf.runtime.common.ui.action.global.GlobalActionId;
import org.eclipse.gmf.runtime.common.ui.services.action.global.IGlobalActionContext;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramEditPart;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramCommandStack;
import org.eclipse.gmf.runtime.diagram.ui.parts.IDiagramWorkbenchPart;
import org.eclipse.gmf.runtime.diagram.ui.providers.DiagramGlobalActionHandler;
import org.eclipse.gmf.runtime.diagram.ui.requests.PasteViewRequest;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.runtime.notation.Node;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.designer.business.diagram.custom.commands.GmfPastCommand;
import org.talend.designer.business.diagram.custom.edit.parts.BusinessItemShapeEditPart;
import org.talend.designer.business.model.business.BusinessAssignment;
import org.talend.designer.business.model.business.BusinessItem;
import org.talend.designer.business.model.business.BusinessProcess;
import org.talend.designer.business.model.business.TalendItem;

/**
 * wchen class global comment. Detailled comment
 */
public class ClipboardActionHandler extends DiagramGlobalActionHandler {

    private static boolean isCut = false;

    private static IDiagramWorkbenchPart older;

    private static Map cutItemIds;

    @Override
    public ICommand getCommand(IGlobalActionContext cntxt) {

        IWorkbenchPart part = cntxt.getActivePart();
        if (!(part instanceof IDiagramWorkbenchPart)) {
            return null;
        }

        IDiagramWorkbenchPart diagramPart = (IDiagramWorkbenchPart) part;
        ICommand command = null;

        String actionId = cntxt.getActionId();
        if (actionId.equals(GlobalActionId.COPY)) {
            command = getCopyCommand(cntxt, diagramPart, false);
            transfer(cntxt.getSelection());
            isCut = false;
            older = diagramPart;

        } else if (actionId.equals(GlobalActionId.CUT) && cntxt.getSelection() != null) {
            saveCut(cntxt.getSelection());
            command = getCutCommand(cntxt, diagramPart);
            transfer(cntxt.getSelection());
            isCut = true;
            older = diagramPart;

        }
        if (actionId.equals(GlobalActionId.PASTE)) {

            // diagramPart.getDiagramGraphicalViewer().setSelection(new
            // StructuredSelection(diagramPart.getDiagramEditPart()));

            PasteViewRequest pasteReq = createPasteViewRequest();
            CommandStack cs = diagramPart.getDiagramEditDomain().getDiagramCommandStack();

            Object[] objects = ((IStructuredSelection) cntxt.getSelection()).toArray();

            if (objects.length == 1) {

                Command paste = ((EditPart) objects[0]).getCommand(pasteReq);
                if (paste != null) {

                    cs.execute(paste);
                    diagramPart.getDiagramEditPart().getFigure().invalidate();
                    diagramPart.getDiagramEditPart().getFigure().validate();
                    selectAddedObject(diagramPart.getDiagramGraphicalViewer(), DiagramCommandStack.getReturnValues(paste));

                }
            }

            Object elements = TemplateTransfer.getInstance().getObject();

            if (elements instanceof List) {
                List<BusinessItem> list = (List<BusinessItem>) elements;
                DiagramEditPart dp = diagramPart.getDiagramEditPart();
                boolean inEditors = false;
                if (older != diagramPart) {
                    inEditors = true;
                    older = diagramPart;
                }

                GmfPastCommand pastBusiness = new GmfPastCommand((BusinessProcess) ((Diagram) dp.getModel()).getElement(), list,
                        dp, this.cutItemIds, this.isCut | inEditors);
                try {
                    pastBusiness.execute(null, null);
                } catch (ExecutionException e) {
                    ExceptionHandler.process(e);
                }

            }
            return null;
        }

        return command;
    }

    private void transfer(ISelection object) {
        if (object instanceof IStructuredSelection) {
            List<BusinessItem> selections = new ArrayList();
            for (Object obj : ((IStructuredSelection) object).toList()) {

                if (obj instanceof BusinessItemShapeEditPart) {
                    BusinessItemShapeEditPart editPart = (BusinessItemShapeEditPart) obj;
                    EObject element = ((Node) editPart.getModel()).getElement();
                    if (element instanceof BusinessItem) {
                        selections.add((BusinessItem) element);
                    }
                }
            }
            TemplateTransfer.getInstance().setObject(selections);
        }
    }

    private void saveCut(ISelection object) {
        cutItemIds = new HashMap();
        if (object instanceof IStructuredSelection) {

            for (Object obj : ((IStructuredSelection) object).toList()) {
                if (obj instanceof BusinessItemShapeEditPart) {
                    BusinessItemShapeEditPart editPart = (BusinessItemShapeEditPart) obj;
                    EObject element = ((Node) editPart.getModel()).getElement();
                    if (element instanceof BusinessItem) {
                        BusinessItem businessItem = (BusinessItem) element;
                        List assignments = new ArrayList();
                        for (Object assignment : businessItem.getAssignments()) {
                            BusinessAssignment ba = (BusinessAssignment) assignment;
                            TalendItem item = ba.getTalendItem();
                            if (item != null) {
                                assignments.add(item.getId());
                            }
                        }
                        cutItemIds.put(businessItem, assignments);

                    }
                }
            }
        }
    }

    @Override
    protected boolean canCut(IGlobalActionContext cntxt) {
        String actionId = cntxt.getActionId();
        if (actionId.equals(GlobalActionId.CUT)) {
            return true;
        }
        return false;
    }
}
