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
package org.talend.repository.ui.actions.routines;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.talend.commons.exception.SystemException;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.properties.RoutineItem;
import org.talend.designer.codegen.ICodeGeneratorService;
import org.talend.designer.codegen.IRoutineSynchronizer;
import org.talend.repository.editor.RepositoryEditorInput;
import org.talend.repository.ui.actions.AContextualAction;

/**
 * DOC smallet class global comment. Detailled comment <br/>
 * 
 * $Id: talend.epf 1 2006-09-29 17:06:40 +0000 (ven., 29 sept. 2006) nrousseau $
 * 
 */
public abstract class AbstractRoutineAction extends AContextualAction {

    /**
     * DOC smallet Comment method "openRoutineEditor".
     * 
     * @param routineItem
     * @throws SystemException
     * @throws PartInitException
     */
    protected IEditorPart openRoutineEditor(RoutineItem routineItem, boolean readOnly) throws SystemException, PartInitException {
        if (routineItem == null) {
            return null;
        }
        ICodeGeneratorService service = (ICodeGeneratorService) GlobalServiceRegister.getDefault().getService(
                ICodeGeneratorService.class);

        ECodeLanguage lang = ((RepositoryContext) CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY))
                .getProject().getLanguage();
        IRoutineSynchronizer routineSynchronizer;
        switch (lang) {
        case JAVA:
            routineSynchronizer = service.createJavaRoutineSynchronizer();
            break;
        case PERL:
            routineSynchronizer = service.createPerlRoutineSynchronizer();
            break;
        default:
            throw new UnsupportedOperationException("Unknow language " + lang);
        }

        // check if the related editor is open.
        IWorkbenchPage page = getActivePage();

        IEditorReference[] editorParts = page.getEditorReferences();
        String talendEditorID = "org.talend.designer.core.ui.editor.StandAloneTalend" + lang.getCaseName() + "Editor";
        boolean found = false;
        IEditorPart talendEditor = null;
        for (IEditorReference reference : editorParts) {
            IEditorPart editor = reference.getEditor(false);
            if (talendEditorID.equals(editor.getSite().getId())) {
                // TextEditor talendEditor = (TextEditor) editor;
                RepositoryEditorInput editorInput = (RepositoryEditorInput) editor.getEditorInput();
                if (editorInput.getItem().equals(routineItem)) {
                    page.bringToTop(editor);
                    found = true;
                    talendEditor = editor;
                    break;
                }
            }
        }

        if (!found) {
            routineSynchronizer.syncRoutine(routineItem, true);
            IFile file = routineSynchronizer.getRoutineFile(routineItem);

            RepositoryEditorInput input = new RepositoryEditorInput(file, routineItem);
            input.setReadOnly(readOnly);
            talendEditor = page.openEditor(input, talendEditorID); //$NON-NLS-1$            
        }

        return talendEditor;

    }
}
