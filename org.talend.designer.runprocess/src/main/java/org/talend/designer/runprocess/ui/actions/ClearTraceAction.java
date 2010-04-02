// ============================================================================
//
// Copyright (C) 2006-2010 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.runprocess.ui.actions;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.IProcess;
import org.talend.designer.core.ui.editor.nodes.Node;

/**
 * Clean trace data on a process. <br/>
 * 
 * $Id$
 * 
 */
public class ClearTraceAction extends Action {

    /** The process to be cleared. */
    private IProcess process;

    /**
     * Constructs a new ClearTraceAction.
     */
    public ClearTraceAction() {
        super("Clean trace tracking"); //$NON-NLS-1$

        setEnabled(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        //        
        IConnection connection = null;
        for (Iterator<? extends INode> i = process.getGraphicalNodes().iterator(); connection == null && i.hasNext();) {
            INode psNode = i.next();
            if (psNode instanceof Node) {
                Node node = (Node) psNode;
                node.getNodeProgressBar().updateState("UPDATE_STATUS", new Double(0)); //$NON-NLS-1$

                node.setErrorFlag(false);
                node.setCompareFlag(false);
                node.setErrorInfo(null);
                node.getNodeError().updateState("UPDATE_STATUS", false); //$NON-NLS-1$
                node.setErrorInfoChange("ERRORINFO", false); //$NON-NLS-1$
            }

            for (IConnection connec : psNode.getOutgoingConnections()) {
                connec.setTraceData(null);
            }
        }
    }

    /**
     * Getter for process.
     * 
     * @return the process
     */
    public IProcess getProcess() {
        return this.process;
    }

    /**
     * Sets the process.
     * 
     * @param process the process to set
     */
    public void setProcess(IProcess process) {
        this.process = process;
        setEnabled(process != null);
    }
}
