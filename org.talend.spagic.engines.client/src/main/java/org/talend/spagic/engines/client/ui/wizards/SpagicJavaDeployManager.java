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
package org.talend.spagic.engines.client.ui.wizards;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.BooleanUtils;
import org.eclipse.core.runtime.IPath;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.CorePlugin;
import org.talend.core.model.genhtml.HTMLDocUtils;
import org.talend.core.model.process.IContextParameter;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.utils.JavaResourcesHelper;
import org.talend.designer.core.IDesignerCoreService;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.JobInfo;
import org.talend.designer.runprocess.ProcessorUtilities;
import org.talend.repository.documentation.ExportFileResource;

/**
 * DOC qwei class global comment. Detailled comment <br/>
 * 
 */
public class SpagicJavaDeployManager extends org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobJavaScriptsManager {

    public List<ExportFileResource> getExportResources(ExportFileResource[] process, Map<ExportChoice, Boolean> exportChoice,
            String contextName, String launcher, int statisticPort, int tracePort, String... codeOptions) {

        for (int i = 0; i < process.length; i++) {
            ProcessItem processItem = (ProcessItem) process[i].getItem();

            String libPath = calculateLibraryPathFromDirectory(process[i].getDirectoryName());
            // use character @ as temporary classpath separator, this one will be replaced during the export.
            String standardJars = libPath + PATH_SEPARATOR + SYSTEMROUTINE_JAR + ProcessorUtilities.TEMP_JAVA_CLASSPATH_SEPARATOR
                    + libPath + PATH_SEPARATOR + USERROUTINE_JAR + ProcessorUtilities.TEMP_JAVA_CLASSPATH_SEPARATOR + ".";
            ProcessorUtilities.setExportConfig("java", standardJars, libPath);

            if (!BooleanUtils.isTrue(exportChoice.get(ExportChoice.doNotCompileCode))) {
                generateJobFiles(processItem, contextName, statisticPort != IProcessor.NO_STATISTICS,
                        tracePort != IProcessor.NO_TRACES, BooleanUtils.isTrue(exportChoice.get(ExportChoice.applyToChildren)));
            }
            List<URL> resources = new ArrayList<URL>();
            resources.addAll(getLauncher(BooleanUtils.isTrue(exportChoice.get(ExportChoice.needLauncher)), processItem,
                    escapeSpace(contextName), escapeSpace(launcher), statisticPort, tracePort, codeOptions));

            addSource(processItem, BooleanUtils.isTrue(exportChoice.get(ExportChoice.needSource)), process[i],
                    JOB_SOURCE_FOLDER_NAME);

            resources.addAll(getJobScripts(processItem, BooleanUtils.isTrue(exportChoice.get(ExportChoice.needJob))));
            // resources.addAll(getProperties(processItem, srcList));
            resources.addAll(getProperties(processItem, contextName));
            addContextScripts(process[i], BooleanUtils.isTrue(exportChoice.get(ExportChoice.needContext)));

            // add children jobs
            boolean needChildren = BooleanUtils.isTrue(exportChoice.get(ExportChoice.needJob))
                    && BooleanUtils.isTrue(exportChoice.get(ExportChoice.needContext));
            List<URL> childrenList = addChildrenResources(processItem, needChildren, process[i], exportChoice);
            resources.addAll(childrenList);
            process[i].addResources(resources);

            // Gets job designer resouce
            // List<URL> srcList = getSource(processItem, exportChoice.get(ExportChoice.needSource));
            // process[i].addResources(JOB_SOURCE_FOLDER_NAME, srcList);
        }

        // Exports the system libs
        List<ExportFileResource> list = new ArrayList<ExportFileResource>(Arrays.asList(process));
        // Add the java system libraries
        ExportFileResource rootResource = new ExportFileResource(null, LIBRARY_FOLDER_NAME);
        ExportFileResource spagicResource = new ExportFileResource(null, "");
        list.add(rootResource);

        list.add(spagicResource);
        // Gets system routines
        List<URL> systemRoutineList = getSystemRoutine(BooleanUtils.isTrue(exportChoice.get(ExportChoice.needSystemRoutine)));
        rootResource.addResources(systemRoutineList);
        // Gets user routines
        List<URL> userRoutineList = getUserRoutine(BooleanUtils.isTrue(exportChoice.get(ExportChoice.needUserRoutine)));
        rootResource.addResources(userRoutineList);

        // Gets talend libraries
        List<URL> talendLibraries = getExternalLibraries(BooleanUtils.isTrue(exportChoice.get(ExportChoice.needTalendLibraries)),
                process);
        rootResource.addResources(talendLibraries);

        return list;
    }

    private List<URL> addChildrenResources(ProcessItem process, boolean needChildren, ExportFileResource resource,
            Map<ExportChoice, Boolean> exportChoice) {
        List<JobInfo> list = new ArrayList<JobInfo>();
        if (needChildren) {
            String projectName = getCurrentProjectName();
            try {
                List<ProcessItem> processedJob = new ArrayList<ProcessItem>();
                getChildrenJobAndContextName(process.getProperty().getLabel(), list, process, projectName, processedJob,
                        resource, exportChoice);
            } catch (Exception e) {
                ExceptionHandler.process(e);
            }

        }

        List<URL> allJobScripts = new ArrayList<URL>();
        for (Iterator<JobInfo> iter = list.iterator(); iter.hasNext();) {
            JobInfo jobInfo = iter.next();
            allJobScripts.addAll(getJobScripts(jobInfo.getJobName(), jobInfo.getJobVersion(), exportChoice
                    .get(ExportChoice.needJob)));
            addContextScripts(jobInfo.getJobName(), jobInfo.getJobVersion(), resource, exportChoice.get(ExportChoice.needContext));
        }

        return allJobScripts;
    }

    public List<URL> getProperties(ProcessItem processItem, String contextName) {
        List<URL> list = new ArrayList<URL>();
        Properties p = new Properties();
        FileOutputStream out = null;
        String projectName = getCurrentProjectName();
        String jobName = processItem.getProperty().getLabel();
        String jobFolderName = JavaResourcesHelper.getJobFolderName(escapeFileNameSpace(processItem), processItem.getProperty()
                .getVersion());
        try {
            // List<SpagoBiServer> listServerSapgo = null;
            // listServerSapgo = SpagicServerHelper.parse(new SpagicPreferencePage().getPreferenceStore().getString(
            // SpagoBiServer.SPAGOBI_SERVER));
            // if (listServerSapgo != null && !listServerSapgo.isEmpty()) {
            // Iterator<SpagoBiServer> iterator = listServerSapgo.iterator();
            // while (iterator.hasNext()) {
            // SpagoBiServer spagoBiServer = iterator.next();
            // }
            // }
            IPath path = getSrcRootLocation();
            File file = new File(getTmpFolder() + PATH_SEPARATOR + "spagic.properties");
            path = path.append(projectName).append(jobFolderName).append(jobName + ".java");
            BufferedReader buff = new BufferedReader(new FileReader(path.toPortableString()));
            int nbLine = 0;
            while (buff.readLine() != null) {
                nbLine++;
            }
            out = new FileOutputStream(file);
            PrintStream ps = new PrintStream(out);
            IDesignerCoreService service = CorePlugin.getDefault().getDesignerCoreService();
            IProcess process = service.getProcessFromProcessItem(processItem);
            List<IContextParameter> ctxParams = process.getContextManager().getContext(contextName).getContextParameterList();
            for (IContextParameter ctxParam : ctxParams) {
                p.put(ctxParam.getName(), ctxParam.getValue());
            }
            p.put("JobClassName", getCurrentProjectName()
                    + "."
                    + JavaResourcesHelper.getJobFolderName(processItem.getProperty().getLabel(), processItem.getProperty()
                            .getVersion()) + "." + processItem.getProperty().getLabel());
            p.put("talendJobClassDescription", HTMLDocUtils.checkString(processItem.getProperty().getDescription()));
            p.put("rowNumber", Integer.toString(nbLine));
            p.put("host", "localhost");
            p.list(ps);
            ps.flush();
            list.add(file.toURI().toURL());
        } catch (Exception e) {
            ExceptionHandler.process(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
        }
        return list;
    }
}
