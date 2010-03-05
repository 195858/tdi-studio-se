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
package org.talend.designer.mapper.ui.dnd;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Control;
import org.talend.designer.mapper.managers.MapperManager;

/**
 * DOC amaumont class global comment. Detailled comment <br/>
 * 
 * $Id$
 * 
 */
public class DropTargetOperationListener {

    int authorizedOperations = DND.DROP_DEFAULT | DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;

    Transfer[] authorizedTransfers = new Transfer[] { TableEntriesTransfer.getInstance() };

    private DropTargetListener dropTargetListener;

    public DropTargetOperationListener(final MapperManager mapperManager) {
        super();
        dropTargetListener = new DefaultDropTargetListener(mapperManager);
    }

    /**
     * DOC amaumont Comment method "addControl".
     * 
     * @param outputTablesZoneView
     */
    public void addControl(Control control) {
        DropTarget dropTarget = new DropTarget(control, authorizedOperations);
        dropTarget.setTransfer(authorizedTransfers);
        dropTarget.addDropListener(dropTargetListener);
    }

}
