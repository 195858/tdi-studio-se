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
package org.talend.designer.core.ui.action;

import java.util.List;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.ui.IWorkbenchPart;
import org.talend.core.model.process.IElementParameter;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.ui.editor.cmd.PropertyChangeCommand;
import org.talend.designer.core.ui.editor.connections.Connection;
import org.talend.designer.core.ui.editor.connections.ConnectionPart;
import org.talend.designer.core.ui.editor.connections.ConnectionTraceEditPart;

/**
 * hwang class global comment. Detailled comment
 */
public class TraceDisableAction extends SelectionAction {

    private static final String SET_TRACE_DISABLE = Messages.getString("TraceDisableAction.TraceDisableDesc"); //$NON-NLS-1$

    private static final String TRACE_DISABLE = Messages.getString("TraceDisableAction.TraceDesableTitle"); //$NON-NLS-1$

    public TraceDisableAction(IWorkbenchPart part) {
        super(part);
        setText(TRACE_DISABLE);
        setToolTipText(SET_TRACE_DISABLE);
        setDescription(SET_TRACE_DISABLE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {
        List parts = getSelectedObjects();
        if (parts.size() != 1) {
            return false;
        }
        Object input = parts.get(0);

        if (input instanceof ConnectionTraceEditPart) {
            ConnectionTraceEditPart tracePart = (ConnectionTraceEditPart) input;

            ConnectionPart connPart = (ConnectionPart) tracePart.getParent();
            Connection conn = (Connection) connPart.getModel();
            if (conn.enableTraces()) {
                IElementParameter element = conn.getElementParameter(EParameterName.TRACES_CONNECTION_ENABLE.getName());
                Boolean flag = (Boolean) element.getValue();
                if (flag == true) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void run() {
        List selection = getSelectedObjects();

        Object input = selection.get(0);

        if (input instanceof ConnectionTraceEditPart) {
            ConnectionTraceEditPart tracePart = (ConnectionTraceEditPart) input;

            ConnectionPart connPart = (ConnectionPart) tracePart.getParent();
            Connection conn = (Connection) connPart.getModel();

            execute(new PropertyChangeCommand(conn, EParameterName.TRACES_CONNECTION_ENABLE.getName(), false));

            tracePart.refresh();
        }

    }

}
