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
package org.talend.repository.ui.wizards.exportjob.scriptsmanager;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.utils.generation.JavaUtils;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.PluginChecker;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.general.ILibrariesService;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.Project;
import org.talend.core.model.properties.RulesItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.utils.JavaResourcesHelper;
import org.talend.core.ui.IRulesProviderService;
import org.talend.designer.core.IDesignerCoreService;
import org.talend.designer.core.model.utils.emf.talendfile.ContextType;
import org.talend.designer.core.model.utils.emf.talendfile.ElementParameterType;
import org.talend.designer.core.model.utils.emf.talendfile.NodeType;
import org.talend.designer.core.model.utils.emf.talendfile.impl.ProcessTypeImpl;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.IRunProcessService;
import org.talend.designer.runprocess.ItemCacheManager;
import org.talend.designer.runprocess.JobInfo;
import org.talend.designer.runprocess.ProcessorUtilities;
import org.talend.librariesmanager.model.ModulesNeededProvider;
import org.talend.repository.ProjectManager;
import org.talend.repository.RepositoryPlugin;
import org.talend.repository.constants.FileConstants;
import org.talend.repository.documentation.ExportFileResource;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.IProxyRepositoryFactory;

/**
 * Manages the job scripts to be exported. <br/>
 * 
 * $Id: JobScriptsManager.java 1 2006-12-14 下�?�05:06:49 bqian
 * 
 */
public class JobJavaScriptsManager extends JobScriptsManager {

    private static final String USER_ROUTINES_PATH = "routines"; //$NON-NLS-1$

    private static final String SYSTEM_ROUTINES_PATH = "routines/system"; //$NON-NLS-1$

    protected static final String SYSTEMROUTINE_JAR = "systemRoutines.jar"; //$NON-NLS-1$

    protected static final String USERROUTINE_JAR = "userRoutines.jar"; //$NON-NLS-1$

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.repository.ui.wizards.exportjob.JobScriptsManager#getExportResources(org.talend.core.model.properties
     * .ProcessItem[], boolean, boolean, boolean, boolean, boolean, boolean, boolean, java.lang.String)
     */
    @Override
    public List<ExportFileResource> getExportResources(ExportFileResource[] process, Map<ExportChoice, Object> exportChoice,
            String contextName, String launcher, int statisticPort, int tracePort, String... codeOptions) {

        for (int i = 0; i < process.length; i++) {
            ProcessItem processItem = (ProcessItem) process[i].getItem();
            String selectedJobVersion = processItem.getProperty().getVersion();
            if (!isMultiNodes() && this.getSelectedJobVersion() != null) {
                selectedJobVersion = this.getSelectedJobVersion();
            }
            if (progressMonitor != null) {
                progressMonitor
                        .subTask(Messages.getString("JobJavaScriptsManager.exportJob") + process[i].getNode().getObject().getLabel() + "_" + selectedJobVersion); //$NON-NLS-1$ //$NON-NLS-2$
            }
            String libPath = calculateLibraryPathFromDirectory(process[i].getDirectoryName());
            // use character @ as temporary classpath separator, this one will be replaced during the export.
            String standardJars = libPath + PATH_SEPARATOR + SYSTEMROUTINE_JAR + ProcessorUtilities.TEMP_JAVA_CLASSPATH_SEPARATOR
                    + libPath + PATH_SEPARATOR + USERROUTINE_JAR + ProcessorUtilities.TEMP_JAVA_CLASSPATH_SEPARATOR + "."; //$NON-NLS-1$
            ProcessorUtilities.setExportConfig("java", standardJars, libPath); //$NON-NLS-1$

            if (!isOptionChoosed(exportChoice, ExportChoice.doNotCompileCode)) {
                generateJobFiles(processItem, contextName, selectedJobVersion, statisticPort != IProcessor.NO_STATISTICS,
                        tracePort != IProcessor.NO_TRACES, isOptionChoosed(exportChoice, ExportChoice.applyToChildren),
                        progressMonitor);
            }
            List<URL> resources = new ArrayList<URL>();
            resources.addAll(getLauncher(isOptionChoosed(exportChoice, ExportChoice.needLauncher), processItem,
                    escapeSpace(contextName), escapeSpace(launcher), statisticPort, tracePort, codeOptions));

            addSourceCode(process, processItem, isOptionChoosed(exportChoice, ExportChoice.needSourceCode), process[i],
                    selectedJobVersion);

            addJobItem(process, processItem, isOptionChoosed(exportChoice, ExportChoice.needJobItem), process[i],
                    selectedJobVersion);

            addDependencies(process, processItem, isOptionChoosed(exportChoice, ExportChoice.needDependencies), process[i]);
            resources.addAll(getJobScripts(processItem, selectedJobVersion, (Boolean) exportChoice
                    .get(ExportChoice.needJobScript))); // always need job generation

            addContextScripts(process[i], selectedJobVersion, isOptionChoosed(exportChoice, ExportChoice.needContext));

            // add children jobs
            boolean needChildren = true;
            List<URL> childrenList = addChildrenResources(process, processItem, needChildren, process[i], exportChoice,
                    selectedJobVersion);
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
        list.add(rootResource);
        // Gets system routines
        List<URL> systemRoutineList = getSystemRoutine(isOptionChoosed(exportChoice, ExportChoice.needSystemRoutine));
        rootResource.addResources(systemRoutineList);
        // Gets user routines
        List<URL> userRoutineList = getUserRoutine(isOptionChoosed(exportChoice, ExportChoice.needUserRoutine));
        rootResource.addResources(userRoutineList);

        // Gets talend libraries
        List<URL> talendLibraries = getExternalLibraries(isOptionChoosed(exportChoice, ExportChoice.needTalendLibraries), process);
        rootResource.addResources(talendLibraries);

        if (PluginChecker.isRulesPluginLoaded()) {
            // hywang add for 6484,add final drl files or xls files to exported job script
            ExportFileResource ruleFileResource = new ExportFileResource(null, "Rules/rules/final"); //$NON-NLS-N$
            list.add(ruleFileResource);
            try {
                Map<String, List<URL>> map = initUrlForRulesFiles(process);
                Object[] keys = map.keySet().toArray();
                for (int i = 0; i < keys.length; i++) {
                    List<URL> talendDrlFiles = map.get(keys[i].toString());
                    ruleFileResource.addResources(keys[i].toString(), talendDrlFiles);
                }
            } catch (CoreException e) {
                ExceptionHandler.process(e);
            } catch (MalformedURLException e) {
                ExceptionHandler.process(e);
            } catch (PersistenceException e) {
                ExceptionHandler.process(e);
            }
        }
        return list;
    }

    // bug 8720
    public boolean isOptionChoosed(Map<ExportChoice, Object> exportChoice, Object key) {
        if (key != null) {
            return BooleanUtils.isTrue((Boolean) exportChoice.get(key));
        }
        return false;
    }

    /**
     * DOC acer Comment method "addContextScripts".
     * 
     * @param resource
     * @param boolean1
     */
    protected void addContextScripts(ExportFileResource resource, Boolean needContext) {
        addContextScripts((ProcessItem) resource.getItem(), escapeFileNameSpace((ProcessItem) resource.getItem()), resource
                .getItem().getProperty().getVersion(), resource, needContext);
    }

    /**
     * ftang Comment method "addContextScripts".
     * 
     * @param resource
     * @param boolean1
     */
    protected void addContextScripts(ExportFileResource resource, String jobVersion, Boolean needContext) {
        addContextScripts((ProcessItem) resource.getItem(), escapeFileNameSpace((ProcessItem) resource.getItem()), jobVersion,
                resource, needContext);
    }

    /**
     * DOC acer Comment method "addContextScripts".
     * 
     * @param resource
     * @param boolean1
     */
    protected void addContextScripts(ProcessItem processItem, String jobName, String jobVersion, ExportFileResource resource,
            Boolean needContext) {
        if (!needContext) {
            return;
        }
        List<URL> list = new ArrayList<URL>(1);
        String projectName = getCorrespondingProjectName(processItem);
        String folderName = JavaResourcesHelper.getJobFolderName(jobName, jobVersion);
        try {
            IPath classRoot = getClassRootPath();
            classRoot = classRoot.append(projectName).append(folderName).append(JOB_CONTEXT_FOLDER);
            File contextDir = classRoot.toFile();
            if (contextDir.isDirectory()) {
                // See bug 0003568: Three contexts file exported, while only two contexts in the job.
                list.addAll(getActiveContextFiles(classRoot.toFile().listFiles(), processItem));
            }

            // list.add(classRoot.toFile().toURL());

            String jobPackagePath = projectName + PATH_SEPARATOR + folderName + PATH_SEPARATOR + JOB_CONTEXT_FOLDER;
            resource.addResources(jobPackagePath, list);
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
    }

    /**
     * User may delete some contexts after generating the context files. So we will only export those files that match
     * any existing context name. See bug 0003568: Three contexts file exported, while only two contexts in the job.
     * 
     * @param listFiles The generated context files.
     * @param processItem The current process item that will be exported.
     * @return An url list of context files.
     * @throws MalformedURLException
     */
    @SuppressWarnings("deprecation")
    private List<URL> getActiveContextFiles(File[] listFiles, ProcessItem processItem) throws MalformedURLException {
        List<URL> contextFileUrls = new ArrayList<URL>();
        try {
            // get all context name from process
            Set<String> contextNames = new HashSet<String>();
            for (Object o : processItem.getProcess().getContext()) {
                if (o instanceof ContextType) {
                    ContextType context = (ContextType) o;
                    contextNames.add(context.getName().replace(" ", "")); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
            for (File file : listFiles) {
                String fileName = file.getName();
                // remove file extension
                fileName = fileName.substring(0, fileName.lastIndexOf('.'));
                if (contextNames.contains(fileName)) {
                    // if the file match any existing context, add this file to list
                    contextFileUrls.add(file.toURL());
                }
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
        return contextFileUrls;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.talend.repository.ui.wizards.exportjob.scriptsmanager.JobScriptsManager#getSource(org.talend.core.model.
     * properties.ProcessItem, boolean)
     */
    @Override
    protected void addJobItem(ExportFileResource[] allResources, ProcessItem processItem, boolean needSource,
            ExportFileResource resource, String... selectedJobVersion) {
        // getItemResource(processItem, resource, basePath, selectedJobVersion);
        // super.addSource(processItem, needSource, resource, basePath, selectedJobVersion);
        // Get java src
        if (!needSource) {
            return;
        }
        try {
            String projectName = getCorrespondingProjectName(processItem);
            String jobName = processItem.getProperty().getLabel();
            String jobVersion = processItem.getProperty().getVersion();
            if (!isMultiNodes() && selectedJobVersion != null && selectedJobVersion.length == 1) {
                jobVersion = selectedJobVersion[0];
            }

            IPath projectFilePath = getCorrespondingProjectRootPath(processItem).append(FileConstants.LOCAL_PROJECT_FILENAME);

            String processPath = processItem.getState().getPath();
            processPath = processPath == null || processPath.equals("") ? "" : processPath; //$NON-NLS-1$ //$NON-NLS-2$
            IPath emfFileRootPath = getEmfFileRootPath(processItem);
            ERepositoryObjectType itemType = ERepositoryObjectType.getItemType(processItem);
            IPath typeFolderPath = new Path(ERepositoryObjectType.getFolderName(itemType));
            IPath itemFilePath = emfFileRootPath.append(processPath).append(
                    jobName + "_" + jobVersion + "." + FileConstants.ITEM_EXTENSION); //$NON-NLS-1$ //$NON-NLS-2$
            IPath propertiesFilePath = emfFileRootPath.append(processPath).append(
                    jobName + "_" + jobVersion + "." + FileConstants.PROPERTIES_EXTENSION); //$NON-NLS-1$ //$NON-NLS-2$
            // project file
            checkAndAddProjectResource(allResources, resource, JOB_ITEMS_FOLDER_NAME + PATH_SEPARATOR + projectName, FileLocator
                    .toFileURL(projectFilePath.toFile().toURL()));

            List<URL> emfFileUrls = new ArrayList<URL>();
            emfFileUrls.add(FileLocator.toFileURL(itemFilePath.toFile().toURL()));
            emfFileUrls.add(FileLocator.toFileURL(propertiesFilePath.toFile().toURL()));
            String relativePath = JOB_ITEMS_FOLDER_NAME + PATH_SEPARATOR + projectName + PATH_SEPARATOR
                    + typeFolderPath.toOSString();
            if (processPath != null && !"".equals(processPath)) {
                relativePath = relativePath + PATH_SEPARATOR + processPath;
            }
            resource.addResources(relativePath, emfFileUrls);
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
    }

    protected void addSourceCode(ExportFileResource[] allResources, ProcessItem processItem, boolean needSource,
            ExportFileResource resource, String... selectedJobVersion) {
        // getItemResource(processItem, resource, basePath, selectedJobVersion);
        // super.addSource(processItem, needSource, resource, basePath, selectedJobVersion);
        // Get java src
        if (!needSource) {
            return;
        }
        try {
            String projectName = getCorrespondingProjectName(processItem);
            String jobName = processItem.getProperty().getLabel();
            String jobVersion = processItem.getProperty().getVersion();
            if (!isMultiNodes() && selectedJobVersion != null && selectedJobVersion.length == 1) {
                jobVersion = selectedJobVersion[0];
            }

            String jobFolderName = JavaResourcesHelper.getJobFolderName(jobName, jobVersion);

            IPath path = getSrcRootLocation();
            path = path.append(projectName).append(jobFolderName); //$NON-NLS-1$

            FilenameFilter filter = new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".java"); //$NON-NLS-1$
                }
            };
            List<URL> javaFileUrls = new ArrayList<URL>();
            for (File curFile : path.toFile().listFiles(filter)) {
                javaFileUrls.add(FileLocator.toFileURL(curFile.toURL()));
            }

            resource.addResources(JOB_SOURCE_FOLDER_NAME + PATH_SEPARATOR + projectName + PATH_SEPARATOR + jobFolderName,
                    javaFileUrls);

            // get different routines.

            IRunProcessService service = CorePlugin.getDefault().getRunProcessService();
            IProject javaProject;

            javaProject = service.getProject(ECodeLanguage.JAVA);

            IFolder rep = javaProject.getFolder(JavaUtils.JAVA_SRC_DIRECTORY + PATH_SEPARATOR + JavaUtils.JAVA_ROUTINES_DIRECTORY
                    + PATH_SEPARATOR + JavaUtils.JAVA_SYSTEM_ROUTINES_DIRECTORY);
            List<URL> systemRoutinesFileUrls = new ArrayList<URL>();
            if (rep.exists()) {
                for (IResource fileResource : rep.members()) {
                    if (fileResource instanceof IFile
                            && ((IFile) fileResource).getFileExtension().equals(ECodeLanguage.JAVA.getExtension())) {
                        systemRoutinesFileUrls.add(fileResource.getLocationURI().toURL());
                    }
                }

                resource.addResources(JOB_SOURCE_FOLDER_NAME + PATH_SEPARATOR + JavaUtils.JAVA_ROUTINES_DIRECTORY
                        + PATH_SEPARATOR + JavaUtils.JAVA_SYSTEM_ROUTINES_DIRECTORY, systemRoutinesFileUrls);
            }

            rep = javaProject.getFolder(JavaUtils.JAVA_SRC_DIRECTORY + PATH_SEPARATOR + JavaUtils.JAVA_ROUTINES_DIRECTORY);
            List<URL> userRoutinesFileUrls = new ArrayList<URL>();
            if (rep.exists()) {
                for (IResource fileResource : rep.members()) {
                    if (fileResource instanceof IFile
                            && ((IFile) fileResource).getFileExtension().equals(ECodeLanguage.JAVA.getExtension())) {
                        userRoutinesFileUrls.add(fileResource.getLocationURI().toURL());
                    }
                }

                resource.addResources(JOB_SOURCE_FOLDER_NAME + PATH_SEPARATOR + JavaUtils.JAVA_ROUTINES_DIRECTORY,
                        userRoutinesFileUrls);
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
    }

    protected String calculateLibraryPathFromDirectory(String directory) {
        int nb = directory.split(PATH_SEPARATOR).length - 1;
        String path = "../"; //$NON-NLS-1$
        for (int i = 0; i < nb; i++) {
            path = path.concat("../"); //$NON-NLS-1$
        }
        return path + LIBRARY_FOLDER_NAME;
    }

    private List<URL> addChildrenResources(ExportFileResource[] allResources, ProcessItem process, boolean needChildren,
            ExportFileResource resource, Map<ExportChoice, Object> exportChoice, String... selectedJobVersion) {
        List<JobInfo> list = new ArrayList<JobInfo>();
        String projectName = getCorrespondingProjectName(process);
        try {
            List<ProcessItem> processedJob = new ArrayList<ProcessItem>();
            getChildrenJobAndContextName(allResources, process.getProperty().getLabel(), list, process, projectName,
                    processedJob, resource, exportChoice, selectedJobVersion);
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }

        List<URL> allJobScripts = new ArrayList<URL>();
        if (needChildren) {
            ProjectManager projectManager = ProjectManager.getInstance();
            for (Iterator<JobInfo> iter = list.iterator(); iter.hasNext();) {
                JobInfo jobInfo = iter.next();
                Project project = projectManager.getProject(jobInfo.getProcessItem());
                String childProjectName = projectName;
                if (project != null) {
                    childProjectName = project.getLabel().toLowerCase();
                }
                allJobScripts.addAll(getJobScripts(childProjectName, jobInfo.getJobName(), jobInfo.getJobVersion(),
                        isOptionChoosed(exportChoice, ExportChoice.needJobScript)));
                addContextScripts(jobInfo.getProcessItem(), jobInfo.getJobName(), jobInfo.getJobVersion(), resource,
                        isOptionChoosed(exportChoice, ExportChoice.needContext));
                addDependencies(allResources, jobInfo.getProcessItem(), isOptionChoosed(exportChoice,
                        ExportChoice.needDependencies), resource);
            }
        }

        return allJobScripts;
    }

    protected void getChildrenJobAndContextName(ExportFileResource[] allResources, String rootName, List<JobInfo> list,
            ProcessItem process, String projectName, List<ProcessItem> processedJob, ExportFileResource resource,
            Map<ExportChoice, Object> exportChoice, String... selectedJobVersion) {
        if (processedJob.contains(process)) {
            // prevent circle
            return;
        }
        processedJob.add(process);
        addJobItem(allResources, process, isOptionChoosed(exportChoice, ExportChoice.needJobItem), resource);
        addDependencies(allResources, process, isOptionChoosed(exportChoice, ExportChoice.needDependencies), resource);
        addSourceCode(allResources, process, isOptionChoosed(exportChoice, ExportChoice.needSourceCode), resource);

        Set<JobInfo> subjobInfos = ProcessorUtilities.getChildrenJobInfo(process);
        for (JobInfo subjobInfo : subjobInfos) {
            String processLabel = subjobInfo.getJobName();
            if (processLabel.equals(rootName)) {
                continue;
            }

            list.add(subjobInfo);

            getChildrenJobAndContextName(allResources, rootName, list, subjobInfo.getProcessItem(), projectName, processedJob,
                    resource, exportChoice);
        }
    }

    /**
     * Gets required java jars.
     * 
     * @param process
     * 
     * @param boolean1
     * @return
     */
    protected List<URL> getExternalLibraries(boolean needLibraries, ExportFileResource[] process) {
        List<URL> list = new ArrayList<URL>();
        if (!needLibraries) {
            return list;
        }
        ILibrariesService librariesService = CorePlugin.getDefault().getLibrariesService();
        String path = librariesService.getLibrariesPath();
        // Gets all the jar files
        File file = new File(path);
        File[] files = file.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".properties") //$NON-NLS-1$ //$NON-NLS-2$
                        || name.toLowerCase().endsWith(".zip") ? true : false; //$NON-NLS-1$
            }
        });
        // Lists all the needed jar files
        Set<String> listModulesReallyNeeded = new HashSet<String>();
        IDesignerCoreService designerService = RepositoryPlugin.getDefault().getDesignerCoreService();
        for (int i = 0; i < process.length; i++) {
            ExportFileResource resource = process[i];
            ProcessItem item = (ProcessItem) resource.getItem();

            String version = item.getProperty().getVersion();
            if (!isMultiNodes() && this.getSelectedJobVersion() != null) {
                version = this.getSelectedJobVersion();
            }
            ProcessItem selectedProcessItem;
            if (resource.getNode() != null) {
                selectedProcessItem = ItemCacheManager.getProcessItem(resource.getNode().getRoot().getProject(), item
                        .getProperty().getId(), version);
            } else {
                // if no node given, take in the current project only
                selectedProcessItem = ItemCacheManager.getProcessItem(item.getProperty().getId(), version);
            }
            IProcess iProcess = designerService.getProcessFromProcessItem(selectedProcessItem);
            listModulesReallyNeeded.addAll(iProcess.getNeededLibraries(true));
        }

        for (ModuleNeeded moduleNeeded : ModulesNeededProvider.getModulesNeededForRoutines()) {
            listModulesReallyNeeded.add(moduleNeeded.getModuleName());
        }

        for (int i = 0; i < files.length; i++) {
            File tempFile = files[i];
            try {
                if (listModulesReallyNeeded.contains(tempFile.getName())) {
                    list.add(tempFile.toURL());
                }
            } catch (MalformedURLException e) {
                ExceptionHandler.process(e);
            }
        }

        return list;
        // List<URL> libraries = new ArrayList<URL>();
        // if (needLibraries) {
        // try {
        // ILibrariesService service = CorePlugin.getDefault().getLibrariesService();
        // libraries = service.getTalendRoutines();
        // } catch (Exception e) {
        // ExceptionHandler.process(e);
        // }
        // }
        // return libraries;
    }

    /**
     * Gets Job Scripts.
     * 
     * @param process
     * @param needJob
     * @param needContext
     * @return
     */
    protected List<URL> getJobScripts(ProcessItem process, boolean needJob) {

        String projectName = getCorrespondingProjectName(process);
        return this.getJobScripts(projectName, escapeFileNameSpace(process), process.getProperty().getVersion(), needJob);
    }

    /**
     * Gets Job Scripts.
     * 
     * @param process
     * @param version
     * @param needJob
     * @return
     */
    protected List<URL> getJobScripts(ProcessItem process, String version, boolean needJob) {
        String projectName = getCorrespondingProjectName(process);
        return this.getJobScripts(projectName, escapeFileNameSpace(process), version, needJob);
    }

    /**
     * Gets Job Scripts.
     * 
     * @param projectName TODO
     * @param needJob
     * @param process
     * @param needContext
     * 
     * @return
     */
    protected List<URL> getJobScripts(String projectName, String jobName, String jobVersion, boolean needJob) {
        List<URL> list = new ArrayList<URL>(1);
        if (!needJob) {
            return list;
        }
        String jobFolderName = JavaResourcesHelper.getJobFolderName(jobName, jobVersion);

        try {
            String classRoot = getClassRootLocation();
            String jarPath = getTmpFolder() + PATH_SEPARATOR + jobFolderName + ".jar"; //$NON-NLS-1$
            // Exports the jar file
            JarBuilder jarbuilder = new JarBuilder(classRoot, jarPath);

            // builds the jar file of the job classes,needContext specifies whether inclucdes the context.
            // add the job
            String jobPath = projectName + PATH_SEPARATOR + jobFolderName;
            List<String> include = new ArrayList<String>();
            include.add(jobPath);
            jarbuilder.setIncludeDir(include);
            // filter the context
            String contextPaht = jobPath + PATH_SEPARATOR + JOB_CONTEXT_FOLDER;
            List<String> excludes = new ArrayList<String>(1);
            excludes.add(contextPaht);
            jarbuilder.setExcludeDir(excludes);

            jarbuilder.buildJar();

            File jarFile = new File(jarPath);
            URL url = jarFile.toURL();
            list.add(url);
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
        return list;
    }

    /**
     * Gets all the perl files in the project .Perl.
     * 
     * @param name
     * @param projectName
     * 
     * @return
     */
    protected String getClassRootLocation() throws Exception {
        IPath binPath = getClassRootPath();
        URL url = binPath.toFile().toURL();
        return url.getPath();
    }

    private IPath getClassRootPath() throws Exception {
        IProject project = RepositoryPlugin.getDefault().getRunProcessService().getProject(ECodeLanguage.JAVA);

        IJavaProject javaProject = JavaCore.create(project);
        IPath binPath = javaProject.getOutputLocation();

        IPath root = project.getParent().getLocation();
        binPath = root.append(binPath);
        return binPath;
    }

    /**
     * Get the path of .JAVA/src
     * 
     * @throws Exception
     */
    protected IPath getSrcRootLocation() throws Exception {
        IProject project = RepositoryPlugin.getDefault().getRunProcessService().getProject(ECodeLanguage.JAVA);

        IJavaProject javaProject = JavaCore.create(project);
        IPackageFragmentRoot[] pp = javaProject.getAllPackageFragmentRoots();
        IPackageFragmentRoot src = null;
        for (IPackageFragmentRoot root : pp) {
            if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
                src = root;
                break;
            }
        }

        IPath root = project.getParent().getLocation();
        root = root.append(src.getPath());
        return root;
    }

    /**
     * Gets system routine.
     * 
     * @param needSystemRoutine
     * @return
     */
    protected List<URL> getSystemRoutine(boolean needSystemRoutine) {
        List<URL> list = new ArrayList<URL>();
        if (!needSystemRoutine) {
            return list;
        }
        try {
            String classRoot = getClassRootLocation();
            List<String> include = new ArrayList<String>();
            include.add(SYSTEM_ROUTINES_PATH);

            String jarPath = getTmpFolder() + PATH_SEPARATOR + SYSTEMROUTINE_JAR;

            // make a jar file of system routine classes
            JarBuilder jarbuilder = new JarBuilder(classRoot, jarPath);
            jarbuilder.setIncludeDir(include);

            jarbuilder.buildJar();

            File jarFile = new File(jarPath);
            URL url = jarFile.toURL();
            list.add(url);
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
        return list;
    }

    /**
     * Gets system routine.
     * 
     * @param needSystemRoutine
     * @return
     */
    protected List<URL> getUserRoutine(boolean needUserRoutine) {
        List<URL> list = new ArrayList<URL>();
        if (!needUserRoutine) {
            return list;
        }
        try {
            String classRoot = getClassRootLocation();
            List<String> include = new ArrayList<String>();
            include.add(USER_ROUTINES_PATH);

            List<String> excludes = new ArrayList<String>();
            excludes.add(SYSTEM_ROUTINES_PATH);

            String jarPath = getTmpFolder() + PATH_SEPARATOR + USERROUTINE_JAR;

            // make a jar file of system routine classes
            JarBuilder jarbuilder = new JarBuilder(classRoot, jarPath);
            jarbuilder.setIncludeDir(include);
            jarbuilder.setExcludeDir(excludes);

            jarbuilder.buildJar();

            File jarFile = new File(jarPath);
            URL url = jarFile.toURL();
            list.add(url);
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
        return list;
    }

    /**
     * 
     * Gets the set of current job's context.
     * 
     * @return a List of context names.
     * 
     */
    @Override
    public List<String> getJobContexts(ProcessItem processItem) {
        List<String> contextNameList = new ArrayList<String>();
        for (Object o : ((ProcessTypeImpl) processItem.getProcess()).getContext()) {
            if (o instanceof ContextType) {
                ContextType context = (ContextType) o;
                if (contextNameList.contains(context.getName())) {
                    continue;
                }
                contextNameList.add(context.getName());
            }
        }
        return contextNameList;
    }

    public List<String> getJobContextsComboValue(ProcessItem processItem) {
        List<String> contextNameList = new ArrayList<String>();
        for (Object o : ((ProcessTypeImpl) processItem.getProcess()).getContext()) {
            if (o instanceof ContextType) {
                ContextType context = (ContextType) o;
                if (contextNameList.contains(context.getName())) {
                    continue;
                }
                contextNameList.add(context.getName());
            }
        }
        return contextNameList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobScriptsManager#getCurrentProjectName()
     */
    @Override
    protected String getCorrespondingProjectName(Item item) {
        return JavaResourcesHelper.getProjectFolderName(item);
    }

    protected List<URL> getLib(List<String> libs, Boolean needLib) {
        List<URL> list = new ArrayList<URL>();
        if (!needLib) {
            return list;
        }

        try {
            ILibrariesService librariesService = CorePlugin.getDefault().getLibrariesService();
            String path = librariesService.getLibrariesPath();
            // Gets all the jar files
            File file = new File(path);
            File[] files = file.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".properties") //$NON-NLS-1$ //$NON-NLS-2$
                            || name.toLowerCase().endsWith(".zip") ? true : false; //$NON-NLS-1$
                }
            });

            for (int i = 0; i < files.length; i++) {
                File tempFile = files[i];
                try {
                    if (libs.contains(tempFile.getName())) {
                        list.add(tempFile.toURL());
                    }
                } catch (MalformedURLException e) {
                    ExceptionHandler.process(e);
                }
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
        return list;
    }

    /**
     * DOC hywang Comment method "initUrlForDrlFiles".
     * 
     * @param process
     * @param talendDrlFiles
     * @throws PersistenceException
     * @throws CoreException
     * @throws MalformedURLException
     */
    private Map<String, List<URL>> initUrlForRulesFiles(ExportFileResource[] process) throws PersistenceException, CoreException,
            MalformedURLException {

        Map<String, List<URL>> map = new HashMap<String, List<URL>>();
        List<URL> urlList = new ArrayList<URL>();

        String processLabelAndVersion = null;
        IFile file;
        Item item = null;
        ProcessItem pi = null;
        if (PluginChecker.isRulesPluginLoaded()) {
            IProxyRepositoryFactory factory = CorePlugin.getDefault().getProxyRepositoryFactory();
            IRulesProviderService rulesService = (IRulesProviderService) GlobalServiceRegister.getDefault().getService(
                    IRulesProviderService.class);

            for (int i = 0; i < process.length; i++) { // loop every exported job
                if (!urlList.isEmpty()) {
                    urlList = new ArrayList<URL>();
                }
                item = ((ExportFileResource) process[i]).getItem();

                if (item instanceof ProcessItem) {
                    pi = (ProcessItem) item;
                    processLabelAndVersion = JavaResourcesHelper.getJobFolderName(pi.getProperty().getLabel(), pi.getProperty()
                            .getVersion());
                }
                for (int j = 0; j < pi.getProcess().getNode().size(); j++) { // loop every node in every exported job
                    if (pi.getProcess().getNode().get(j) instanceof NodeType) {
                        NodeType node = (NodeType) pi.getProcess().getNode().get(j);
                        if (rulesService.isRuleComponent(node)) {
                            for (Object obj : node.getElementParameter()) {
                                if (obj instanceof ElementParameterType) {
                                    ElementParameterType elementParameter = (ElementParameterType) obj;
                                    if (elementParameter.getName().equals("PROPERTY:REPOSITORY_PROPERTY_TYPE")) { //$NON-NLS-N$
                                        String id = elementParameter.getValue();
                                        if (factory.getLastVersion(id).getProperty().getItem() != null) {
                                            if (factory.getLastVersion(id).getProperty().getItem() instanceof RulesItem) {
                                                RulesItem rulesItem = (RulesItem) factory.getLastVersion(id).getProperty()
                                                        .getItem();
                                                file = rulesService.getFinalRuleFile(rulesItem, processLabelAndVersion);
                                                URL url = file.getLocationURI().toURL();
                                                urlList.add(url);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                map.put(processLabelAndVersion, urlList);
            }
        }
        return map;
    }

}
