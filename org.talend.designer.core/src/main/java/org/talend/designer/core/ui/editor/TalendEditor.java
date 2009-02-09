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
package org.talend.designer.core.ui.editor;

import org.talend.designer.core.ui.MultiPageTalendEditor;
import org.talend.designer.core.ui.editor.process.Process;

/**
 * Main class of the Gef Editor. <br/>
 * 
 * $Id$
 * 
 */
public class TalendEditor extends AbstractTalendEditor implements ITalendJobEditor {

    public TalendEditor() {
        this(false);

    }

    public TalendEditor(boolean readOnly) {
        super(readOnly);
    }

    public Process getProcess() {
        return (Process) this.process;
    }

    @Override
    public Object getAdapter(final Class type) {
        return super.getAdapter(type);
    }

    public void setParent(MultiPageTalendEditor multiPageTalendEditor) {
        super.setParent(multiPageTalendEditor);
    }

    public MultiPageTalendEditor getParent() {
        return (MultiPageTalendEditor) super.getParent();
    }

}
