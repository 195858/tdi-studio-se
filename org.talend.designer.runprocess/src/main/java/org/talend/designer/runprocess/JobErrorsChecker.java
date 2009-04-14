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
package org.talend.designer.runprocess;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.epic.perleditor.editors.util.TalendPerlValidator;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.CorePlugin;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.language.LanguageManager;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.process.IProcess2;
import org.talend.core.model.process.Problem;
import org.talend.core.model.process.Problem.ProblemStatus;
import org.talend.core.model.properties.ProcessItem;
import org.talend.designer.codegen.ITalendSynchronizer;
import org.talend.designer.core.IDesignerCoreService;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.views.problems.Problems;
import org.talend.designer.core.utils.DesignerUtilities;
import org.talend.designer.runprocess.ErrorDetailTreeBuilder.JobErrorEntry;

/**
 * Check if there is error in jobs before running.
 */
public class JobErrorsChecker {

    public static List<Node> nodeList = new ArrayList<Node>();

    public static boolean hasErrors(Shell shell) {

        try {
            // CorePlugin.getDefault().getRunProcessService().getProject(LanguageManager.getCurrentLanguage()).build(
            // IncrementalProjectBuilder.AUTO_BUILD, null);

            List<ProcessItem> items = ProcessorUtilities.getAllProcessItems();
            IDesignerCoreService service = CorePlugin.getDefault().getDesignerCoreService();

            boolean isPerl = false;
            if (LanguageManager.getCurrentLanguage().equals(ECodeLanguage.PERL)) {
                isPerl = true;
            }

            ITalendSynchronizer synchronizer = CorePlugin.getDefault().getCodeGeneratorService().createRoutineSynchronizer();

            Set<String> jobNames = new HashSet<String>();
            for (ProcessItem item : items) {
                // get source file
                IFile sourceFile = synchronizer.getFile(item);

                // See Bug 5421
                // Get job from editor if it is opened.
                IProcess process = DesignerUtilities.findProcessFromEditors(item.getProperty().getId(), item.getProperty()
                        .getVersion());
                if (process == null) {
                    // Get job from file if it is not opened.
                    process = service.getProcessFromProcessItem(item);
                }//

                if (isPerl) {
                    // check syntax error in perl. java use auto build to check syntax
                    validatePerlScript(sourceFile, process);
                }

                jobNames.add(process.getLabel());

                if (process instanceof IProcess2) {
                    IProcess2 process2 = (IProcess2) process;
                    process2.setActivate(true);
                    process2.checkProcess();

                }
                // Property property = process.getProperty();
                Problems.addRoutineFile(sourceFile, item.getProperty());
            }
            Problems.refreshProblemTreeView();

            // collect error
            List<Problem> errors = Problems.getProblemList().getProblemsBySeverity(ProblemStatus.ERROR);
            ErrorDetailTreeBuilder builder = new ErrorDetailTreeBuilder();
            List<JobErrorEntry> input = builder.createTreeInput(errors, jobNames);
            if (input.size() > 0) {
                ErrorDetailDialog dialog = new ErrorDetailDialog(shell, input);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    // stop running
                    return true;
                }
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }

        return false;
    }

    public static void validatePerlScript(IFile file, IProcess process) {
        nodeList.clear();
        try {
            String sourceCode = getSourceCode(file.getContents());
            Set<String> set = TalendPerlValidator.instance().validate(file, sourceCode);
            Iterator<String> ite = set.iterator();
            if (set.isEmpty()) {
                for (INode inode : process.getGraphicalNodes()) {
                    if (inode instanceof Node) {
                        Node node = (Node) inode;
                        node.setErrorFlag(false);
                        node.setErrorInfo(null);
                        node.getNodeError().updateState("UPDATE_STATUS", false);//$NON-NLS-1$
                        node.setErrorInfoChange("ERRORINFO", false);//$NON-NLS-1$
                    }
                }
            } else {

                while (ite.hasNext()) {
                    String uniName = (String) ite.next();
                    for (INode inode : process.getGraphicalNodes()) {
                        if (inode instanceof Node) {
                            Node node = (Node) inode;
                            if (node.getUniqueName().equals(uniName)) {
                                nodeList.add(node);
                            } else {
                                node.setErrorFlag(false);
                                node.setErrorInfo(null);
                                node.getNodeError().updateState("UPDATE_STATUS", false);//$NON-NLS-1$
                                node.setErrorInfoChange("ERRORINFO", false);//$NON-NLS-1$
                            }
                        }
                    }
                }

                for (Node node : nodeList) {
                    node.setErrorFlag(true);
                    node.setErrorInfo(null);
                    node.getNodeError().updateState("UPDATE_STATUS", false);//$NON-NLS-1$
                    node.setErrorInfoChange("ERRORINFO", true);//$NON-NLS-1$
                }
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }

    }

    private static void adderrorMark() {

    }

    /**
     * DOC chuang Comment method "getSourceCode".
     * 
     * @param contents
     * @return
     */
    private static String getSourceCode(InputStream contents) {
        String sourceCode = ""; //$NON-NLS-1$
        InputStreamReader in = null;
        try {
            in = new InputStreamReader(new BufferedInputStream(contents));
            StringBuffer buffer = new StringBuffer();
            char[] readBuffer = new char[2048];
            int n = in.read(readBuffer);
            while (n > 0) {
                buffer.append(readBuffer, 0, n);
                n = in.read(readBuffer);
            }
            sourceCode = buffer.toString();
        } catch (Exception e) {
            ExceptionHandler.process(e);
        } finally {
            if (contents != null) {
                try {
                    contents.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }

        return sourceCode;
    }

}
