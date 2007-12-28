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
package org.talend.designer.core.ui.editor.process;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.talend.designer.core.ui.editor.cmd.CreateNodeContainerCommand;
import org.talend.designer.core.ui.editor.cmd.CreateNoteCommand;
import org.talend.designer.core.ui.editor.cmd.MoveNodeCommand;
import org.talend.designer.core.ui.editor.cmd.MoveNoteCommand;
import org.talend.designer.core.ui.editor.nodecontainer.NodeContainer;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.nodes.NodePart;
import org.talend.designer.core.ui.editor.notes.Note;
import org.talend.designer.core.ui.editor.notes.NoteEditPart;

/**
 * Edit policy of the Diagram that will allow to move the objects on it and create nodes. <br/>
 * 
 * $Id$
 * 
 */
public class ProcessLayoutEditPolicy extends XYLayoutEditPolicy {

    // ------------------------------------------------------------------------
    // Overridden from ConstrainedLayoutEditPolicy

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.gef.editpolicies.ConstrainedLayoutEditPolicy#createChildEditPolicy(org.eclipse.gef.EditPart)
     */
    protected EditPolicy createChildEditPolicy(final EditPart child) {
        ProcessResizableEditPolicy policy = new ProcessResizableEditPolicy();
        policy.setResizeDirections(0);
        return policy;
    }

    // ------------------------------------------------------------------------
    // Abstract methods from ConstrainedLayoutEditPolicy

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.gef.editpolicies.ConstrainedLayoutEditPolicy#createAddCommand(org.eclipse.gef.EditPart,
     * java.lang.Object)
     */
    protected Command createAddCommand(final EditPart child, final Object constraint) {
        return null; // no support for adding
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.gef.editpolicies.ConstrainedLayoutEditPolicy#createChangeConstraintCommand(org.eclipse.gef.EditPart,
     * java.lang.Object)
     */
    public Command createChangeConstraintCommand(final EditPart child, final Object constraint) {
        // return a command to move the part to the location given by the constraint
        if (child instanceof NodePart) {
            if (((Node) child.getModel()).isReadOnly()) {
                return null;
            }
            MoveNodeCommand locationCommand = new MoveNodeCommand((Node) child.getModel(), ((Rectangle) constraint).getLocation());
            return locationCommand;
        }
        if (child instanceof NoteEditPart) {
            if (((Note) child.getModel()).isReadOnly()) {
                return null;
            }
            MoveNoteCommand locationCommand = new MoveNoteCommand((Note) child.getModel(), ((Rectangle) constraint).getLocation());
            return locationCommand;
        }
        // if (child.getModel() instanceof IJobletNode) {
        // if (((IJobletNode) child.getModel()).isReadOnly()) {
        // return null;
        // }
        // JobletNodeMoveCommand command = new JobletNodeMoveCommand((IJobletNode) child.getModel(), ((Rectangle)
        // constraint)
        // .getLocation());
        // return command;
        // }
        return null;
    }

    // ------------------------------------------------------------------------
    // Abstract methods from LayoutEditPolicy

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.gef.editpolicies.LayoutEditPolicy#getCreateCommand(org.eclipse.gef.requests.CreateRequest)
     */
    protected Command getCreateCommand(final CreateRequest request) {
        if (((Process) getHost().getModel()).isReadOnly()) {
            return null;
        }
        Rectangle constraint = (Rectangle) getConstraintFor(request);

        Command command = null;
        if (Note.class.equals(request.getNewObjectType())) {
            command = new CreateNoteCommand((Process) getHost().getModel(), (Note) request.getNewObject(), constraint
                    .getLocation());
        } else if (request.getNewObject() instanceof Node) {
            command = new CreateNodeContainerCommand((Process) getHost().getModel(), new NodeContainer((Node) request
                    .getNewObject()), constraint.getLocation());
        }
        // else if (IJobletNode.class.equals(request.getNewObjectType())) {
        // command = new JobletNodeCreateCommand((Process) getHost().getModel(), (IJobletNode) request.getNewObject(),
        // constraint);
        // }

        return command;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.gef.editpolicies.LayoutEditPolicy#getDeleteDependantCommand(org.eclipse.gef.Request)
     */
    protected Command getDeleteDependantCommand(final Request request) {
        return null; // no support for deleting a dependant
    }
}
