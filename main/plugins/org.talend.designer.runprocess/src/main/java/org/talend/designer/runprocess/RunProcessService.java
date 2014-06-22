// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
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

import java.util.Set;

import org.apache.log4j.Level;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.language.ICodeProblemsChecker;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.process.IProcess2;
import org.talend.core.model.properties.Property;
import org.talend.designer.core.ui.action.SaveJobBeforeRunAction;
import org.talend.designer.runprocess.i18n.Messages;
import org.talend.designer.runprocess.ui.actions.RunProcessAction;

/**
 * DOC qian class global comment. An implementation of the IRunProcessService. <br/>
 * 
 * $Id: talend-code-templates.xml 1 2006-09-29 17:06:40 +0000 (星期五, 29 九月 2006) nrousseau $
 * 
 */

public class RunProcessService implements IRunProcessService {

    private IRunProcessService delegateService;

    /**
     * DOC amaumont RunProcessService constructor comment.
     */
    public RunProcessService() {
        super();
        setDelegateService(new DefaultRunProcessService());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.designer.runprocess.IRunProcessService#createCodeProcessor(org.talend.core.model.process.IProcess,
     * org.talend.core.language.ECodeLanguage, boolean)
     */
    public IProcessor createCodeProcessor(IProcess process, Property property, ECodeLanguage language, boolean filenameFromLabel) {
        return delegateService.createCodeProcessor(process, property, language, filenameFromLabel);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IRunProcessService#createPerformanceData(java.lang.String)
     */
    public IPerformanceData createPerformanceData(String data) {
        return delegateService.createPerformanceData(data);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IRunProcessService#getProject(org.talend.core.language.ECodeLanguage)
     */
    public IProject getProject(ECodeLanguage language) throws CoreException {
        return delegateService.getProject(language);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IRunProcessService#getRoutineFilenameExt()
     */
    public String getRoutineFilenameExt() {
        return delegateService.getRoutineFilenameExt();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IRunProcessService#getSyntaxChecker(org.talend.core.language.ECodeLanguage)
     */
    public ICodeProblemsChecker getSyntaxChecker(ECodeLanguage codeLanguage) {
        return delegateService.getSyntaxChecker(codeLanguage);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IRunProcessService#getSelectedContext()
     */
    public IContext getSelectedContext() {
        RunProcessContext activeContext = RunProcessPlugin.getDefault().getRunProcessContextManager().getActiveContext();
        if (activeContext != null) {
            return activeContext.getSelectedContext();
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IRunProcessService#perlExec(java.lang.StringBuffer, java.lang.StringBuffer,
     * org.eclipse.core.runtime.IPath, java.lang.String, org.apache.log4j.Level, java.lang.String, java.lang.String,
     * int, int, java.lang.String[])
     */
    public int perlExec(StringBuffer out, StringBuffer err, IPath absCodePath, String contextName, Level level,
            String perlInterpreterLibOption, String perlModuleDirectoryOption, int statOption, int traceOption,
            String... codeOptions) throws ProcessorException {
        return delegateService.perlExec(out, err, absCodePath, contextName, level, perlInterpreterLibOption,
                perlModuleDirectoryOption, statOption, traceOption, codeOptions);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IRunProcessService#removeProcess(org.talend.core.model.process.IProcess)
     */
    public void removeProcess(IProcess activeProcess) {
        delegateService.removeProcess(activeProcess);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IRunProcessService#setActiveProcess(org.talend.core.model.process.IProcess)
     */
    public void setActiveProcess(IProcess2 activeProcess) {
        delegateService.setActiveProcess(activeProcess);
    }

    public void setActiveProcess(IProcess2 activeProcess, boolean refreshUI) {
        delegateService.setActiveProcess(activeProcess, refreshUI);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.talend.designer.runprocess.IRunProcessService#setDelegateService(org.talend.designer.runprocess.
     * IRunProcessService)
     */
    public void setDelegateService(IRunProcessService delegateService) {
        boolean isValidDelegate = delegateService != null && !(delegateService instanceof RunProcessService);
        if (isValidDelegate) {
            this.delegateService = delegateService;
        } else {
            throw new IllegalArgumentException(Messages.getString("RunProcessService.delegateServiceError")); //$NON-NLS-1$
        }
    }

    public void updateLibraries(Set<String> jobModuleList, IProcess process) {
        delegateService.updateLibraries(jobModuleList, process);
    }

    public void refreshView() {
        delegateService.refreshView();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IRunProcessService#getJavaProject()
     */
    public IJavaProject getJavaProject() throws CoreException {
        return delegateService.getJavaProject();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IRunProcessService#getPauseTime()
     */
    public int getPauseTime() {
        return delegateService.getPauseTime();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IRunProcessService#deleteAllJobs(boolean)
     */
    public void deleteAllJobs(boolean fromPluginModel) {
        new DeleteAllJobWhenStartUp().startup(fromPluginModel);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IRunProcessService#needDeleteAllJobs()
     */
    public boolean needDeleteAllJobs() {
        return !DeleteAllJobWhenStartUp.executed;
    }

    public IAction getRunProcessAction() {
        return new RunProcessAction();
    }

    public boolean enableTraceForActiveRunProcess() {
        RunProcessContext activeContext = RunProcessPlugin.getDefault().getRunProcessContextManager().getActiveContext();
        if (activeContext != null) {
            return activeContext.isMonitorTrace();
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IRunProcessService#saveJobBeforeRun(org.talend.core.model.process.IProcess)
     */
    public void saveJobBeforeRun(IProcess activeProcess) {
        new SaveJobBeforeRunAction(activeProcess).run();
    }

    public IPreferenceStore getPreferenceStore() {
        return RunProcessPlugin.getDefault().getPreferenceStore();
    }

    public IProcess getActiveProcess() {
        if (RunProcessPlugin.getDefault().getRunProcessContextManager().getActiveContext() == null) {
            return null;
        }
        return RunProcessPlugin.getDefault().getRunProcessContextManager().getActiveContext().getProcess();
    }

    public boolean checkExportProcess(IStructuredSelection selection, boolean isJob) {
        return delegateService.checkExportProcess(selection, isJob);
    }
}
