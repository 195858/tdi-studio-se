// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.codegen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.talend.commons.exception.SystemException;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.commons.utils.VersionUtils;
import org.talend.commons.utils.generation.JavaUtils;
import org.talend.commons.utils.io.FilesUtils;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.IService;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.general.ILibrariesService;
import org.talend.core.model.general.Project;
import org.talend.core.model.process.JobInfo;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.RoutineItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.model.utils.JavaResourcesHelper;
import org.talend.core.ui.branding.AbstractBrandingService;
import org.talend.core.ui.branding.IBrandingService;
import org.talend.designer.runprocess.IRunProcessService;
import org.talend.repository.ProjectManager;
import org.talend.repository.model.RepositoryNodeUtilities;

/**
 * Routine synchronizer of java project.
 * 
 * yzhang class global comment. Detailled comment <br/>
 * 
 * $Id: JavaRoutineSynchronizer.java JavaRoutineSynchronizer 2007-2-2 下午03:29:12 +0000 (下午03:29:12, 2007-2-2 2007)
 * yzhang $
 * 
 */
public class JavaRoutineSynchronizer extends AbstractRoutineSynchronizer {

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.codegen.IRoutineSynchronizer#syncAllRoutines()
     */
    public void syncAllRoutines() throws SystemException {
        for (IRepositoryViewObject routine : getRoutines()) {
            RoutineItem routineItem = (RoutineItem) routine.getProperty().getItem();
            syncRoutine(routineItem, true);
        }

        try {
            ILibrariesService jms = CorePlugin.getDefault().getLibrariesService();
            List<URL> urls = jms.getTalendRoutinesFolder();

            for (URL systemModuleURL : urls) {
                if (systemModuleURL != null) {
                    String fileName = systemModuleURL.getPath();
                    if (fileName.startsWith("/")) { //$NON-NLS-1$
                        fileName = fileName.substring(1);
                    }
                    File f = new File(systemModuleURL.getPath());
                    if (f.isDirectory()) {
                        syncModule(f.listFiles());
                    }
                }
            }
        } catch (IOException e) {
            // e.printStackTrace();
            ExceptionHandler.process(e);
        }
    }
    
    @Override
    public void syncAllRoutinesForLogOn() throws SystemException {
        syncRoutineItemsForLogOn(getRoutines());
    }
    
    private void syncRoutineItemsForLogOn(List<IRepositoryViewObject> routineObjects) throws SystemException {
        for (IRepositoryViewObject routine : routineObjects) {
            RoutineItem routineItem = (RoutineItem) routine.getProperty().getItem();
            syncRoutine(routineItem);
        }

        try {
            ILibrariesService jms = CorePlugin.getDefault().getLibrariesService();
            List<URL> urls = jms.getTalendRoutinesFolder();

            for (URL systemModuleURL : urls) {
                if (systemModuleURL != null) {
                    String fileName = systemModuleURL.getPath();
                    if (fileName.startsWith("/")) { //$NON-NLS-1$
                        fileName = fileName.substring(1);
                    }
                    File f = new File(systemModuleURL.getPath());
                    if (f.isDirectory()) {
                        syncModule(f.listFiles());
                    }
                }
            }
        } catch (IOException e) {
            // e.printStackTrace();
            ExceptionHandler.process(e);
        }

    }

    public void syncAllBeans() throws SystemException {
        // for (IRepositoryViewObject routine : getBeans()) {
        // BeanItem beanItem = (BeanItem) routine.getProperty().getItem();
        // // syncRoutine(routineItem, true);
        // syncBean(beanItem, true);
        // }

        // try {
        // ILibrariesService jms = CorePlugin.getDefault().getLibrariesService();
        // List<URL> urls = jms.getTalendBeansFolder();
        //
        // for (URL systemModuleURL : urls) {
        // if (systemModuleURL != null) {
        // String fileName = systemModuleURL.getPath();
        //                    if (fileName.startsWith("/")) { //$NON-NLS-1$
        // fileName = fileName.substring(1);
        // }
        // File f = new File(systemModuleURL.getPath());
        // if (f.isDirectory()) {
        // syncModule(f.listFiles());
        // }
        // }
        // }
        // } catch (IOException e) {
        // // e.printStackTrace();
        // ExceptionHandler.process(e);
        // }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.codegen.IRoutineSynchronizer#syncRoutine(org.talend .core.model.properties.RoutineItem)
     */
    @Override
    protected void doSyncRoutine(RoutineItem routineItem, boolean copyToTemp) throws SystemException {
        FileOutputStream fos = null;
        try {
            IFile file = getRoutineFile(routineItem);
            if (routineItem.getProperty().getModificationDate() != null) {
                long modificationItemDate = routineItem.getProperty().getModificationDate().getTime();
                long modificationFileDate = file.getModificationStamp();
                if (modificationItemDate <= modificationFileDate) {
                    return;
                }
            } else {
                routineItem.getProperty().setModificationDate(new Date());
            }

            if (copyToTemp) {
                String routineContent = new String(routineItem.getContent().getInnerContent());
                // see 14713
                String version = VersionUtils.getVersion();
                if (routineContent.contains("%GENERATED_LICENSE%")) { //$NON-NLS-1$
                    IService service = GlobalServiceRegister.getDefault().getService(IBrandingService.class);
                    if (service instanceof AbstractBrandingService) {
                        String routineHeader = ((AbstractBrandingService) service).getRoutineLicenseHeader(version);
                        routineContent = routineContent.replace("%GENERATED_LICENSE%", routineHeader); //$NON-NLS-1$
                    }
                }// end
                String label = routineItem.getProperty().getLabel();
                if (!label.equals(ITalendSynchronizer.TEMPLATE) && routineContent != null) {
                    routineContent = routineContent.replaceAll(ITalendSynchronizer.TEMPLATE, label);
                    // routineContent = renameRoutinePackage(routineItem,
                    // routineContent);
                    File f = file.getLocation().toFile();
                    fos = new FileOutputStream(f);
                    fos.write(routineContent.getBytes());
                    fos.close();
                }
            }
            file.refreshLocal(1, null);
        } catch (CoreException e) {
            throw new SystemException(e);
        } catch (IOException e) {
            throw new SystemException(e);
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                // ignore me even if i'm null
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.codegen.IRoutineSynchronizer#syncRoutine(org.talend .core.model.properties.RoutineItem)
     */
    @Override
    protected void doSyncBean(Item beanItem, boolean copyToTemp) throws SystemException {

    }

    /**
     * add project name in package declaration.
     * 
     * @param routineItem
     * @param routineContent
     * @return
     */
    private String renameRoutinePackage(RoutineItem routineItem, String routineContent) {
        if (!routineItem.isBuiltIn()) { // only for user created routines
            ProjectManager pManager = ProjectManager.getInstance();
            org.talend.core.model.properties.Project project = pManager.getProject(routineItem);
            // for create new routine
            String oldPackage = "package(\\s)+" //$NON-NLS-1$
                    + JavaUtils.JAVA_ROUTINES_DIRECTORY + "(\\s)*;"; //$NON-NLS-1$
            String newPackage = "package " + JavaUtils.JAVA_ROUTINES_DIRECTORY //$NON-NLS-1$
                    + "." + project.getTechnicalLabel().toLowerCase() + ";"; //$NON-NLS-1$ //$NON-NLS-2$

            routineContent = routineContent.replaceAll(oldPackage, newPackage);
        }
        return routineContent;
    }

    private IFile getRoutineFile(RoutineItem routineItem) throws SystemException {
        try {
            IRunProcessService service = CodeGeneratorActivator.getDefault().getRunProcessService();
            IProject javaProject = service.getProject(ECodeLanguage.JAVA);
            // ProjectManager projectManager = ProjectManager.getInstance();
            // org.talend.core.model.properties.Project project = projectManager.getProject(routineItem);
            initRoutineFolder(javaProject, routineItem);
            String routinesFolder = getRoutinesFolder(routineItem);
            if (!routineItem.isBuiltIn()) {
                routinesFolder = getRoutinesFolder(routineItem);
            }
            IFile file = javaProject.getFile(routinesFolder + "/" //$NON-NLS-1$
                    + routineItem.getProperty().getLabel() + JavaUtils.JAVA_EXTENSION);
            return file;
        } catch (CoreException e) {
            throw new SystemException(e);
        }
    }

    private IFile getBeanFile(Item beanItem) throws SystemException {
        try {
            IRunProcessService service = CodeGeneratorActivator.getDefault().getRunProcessService();
            IProject javaProject = service.getProject(ECodeLanguage.JAVA);
            ProjectManager projectManager = ProjectManager.getInstance();
            org.talend.core.model.properties.Project project = projectManager.getProject(beanItem);
            initBeanFolder(javaProject, project);
            String beansFolder = getBeansFolder(null);
            // if (!beanItem.isBuiltIn()) {
            beansFolder = getBeansFolder(project);
            // }
            IFile file = javaProject.getFile(beansFolder + "/" //$NON-NLS-1$
                    + beanItem.getProperty().getLabel() + JavaUtils.JAVA_EXTENSION);
            return file;
        } catch (CoreException e) {
            throw new SystemException(e);
        }
    }

    public IFile getProcessFile(JobInfo jobInfo) throws SystemException {
        IRunProcessService service = CodeGeneratorActivator.getDefault().getRunProcessService();
        try {
            IProject javaProject = service.getProject(ECodeLanguage.JAVA);

            String projectFolderName = jobInfo.getProjectFolderName();

            String folderName = JavaResourcesHelper.getJobFolderName(jobInfo.getJobName(), jobInfo.getJobVersion());
            IFile file = javaProject.getFile(JavaUtils.JAVA_SRC_DIRECTORY + "/" //$NON-NLS-1$
                    + projectFolderName + "/" + folderName + "/" //$NON-NLS-1$ //$NON-NLS-2$
                    + jobInfo.getJobName() + JavaUtils.JAVA_EXTENSION);
            return file;
        } catch (CoreException e) {
            throw new SystemException(e);
        }
    }

    private IFile getProcessFile(ProcessItem item) throws SystemException {
        IRunProcessService service = CodeGeneratorActivator.getDefault().getRunProcessService();
        try {
            IProject javaProject = service.getProject(ECodeLanguage.JAVA);

            String projectFolderName = JavaResourcesHelper.getProjectFolderName(item);

            String folderName = JavaResourcesHelper.getJobFolderName(item.getProperty().getLabel(), item.getProperty()
                    .getVersion());
            IFile file = javaProject.getFile(JavaUtils.JAVA_SRC_DIRECTORY + "/" //$NON-NLS-1$
                    + projectFolderName + "/" + folderName + "/" //$NON-NLS-1$ //$NON-NLS-2$
                    + item.getProperty().getLabel() + JavaUtils.JAVA_EXTENSION);
            return file;
        } catch (CoreException e) {
            throw new SystemException(e);
        }
    }

    /**
     * DOC mhirt Comment method "initRoutineFolder".
     * 
     * @param javaProject
     * @param project
     * @throws CoreException
     */
    private void initRoutineFolder(IProject javaProject, RoutineItem item) throws CoreException {
        IFolder rep = javaProject.getFolder(getRoutinesFolder(item));
        if (!rep.exists()) {
            rep.create(true, true, null);
        }
        // if (project != null) {
        // rep = javaProject.getFolder(getRoutinesFolder(project));
        // if (!rep.exists()) {
        // rep.create(true, true, null);
        // }
        // }
    }

    private void initBeanFolder(IProject javaProject, org.talend.core.model.properties.Project project) throws CoreException {
        IFolder rep = javaProject.getFolder(getBeansFolder(null));
        if (!rep.exists()) {
            rep.create(true, true, null);
        }
    }

    private String getRoutinesFolder(RoutineItem routineItem) {
        String routinesPath = JavaUtils.JAVA_SRC_DIRECTORY + "/" //$NON-NLS-1$
                + routineItem.getPackageType();
        // if (project != null) {
        // // add project name in package path
        // routinesPath += "/" + project.getTechnicalLabel().toLowerCase();
        // }
        return routinesPath;
    }

    private String getBeansFolder(org.talend.core.model.properties.Project project) {
        String routinesPath = JavaUtils.JAVA_SRC_DIRECTORY + "/" //$NON-NLS-1$
                + JavaUtils.JAVA_BEANS_DIRECTORY;
        return routinesPath;
    }

    private void initModuleFolder(IProject javaProject, Project project, String directory) throws CoreException {
        IFolder rep = javaProject.getFolder(JavaUtils.JAVA_SRC_DIRECTORY + "/" //$NON-NLS-1$
                + JavaUtils.JAVA_ROUTINES_DIRECTORY + "/" //$NON-NLS-1$
                + JavaUtils.JAVA_SYSTEM_ROUTINES_DIRECTORY + "/" + directory);
        if (!rep.exists()) {
            rep.create(true, true, null);
        }
    }

    public void copyFile(File in, IFile out) throws Exception {
        if (out.exists()) {
            out.delete(true, null);
        }
        FileInputStream fis = new FileInputStream(in);
        if (!out.exists()) {
            out.create(fis, true, null);
        }
        fis.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.codegen.IRoutineSynchronizer#syncRoutine(org.talend .core.model.properties.RoutineItem)
     */
    public IFile syncModule(File[] modules) throws SystemException {
        return syncModules(modules, "");
    }

    private IFile syncModules(File[] modules, String directory) throws SystemException {
        try {
            IRunProcessService service = CodeGeneratorActivator.getDefault().getRunProcessService();
            IProject javaProject = service.getProject(ECodeLanguage.JAVA);
            Project project = ((RepositoryContext) CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY))
                    .getProject();
            initModuleFolder(javaProject, project, directory);

            for (File module : modules) {
                if (!module.isDirectory()) {
                    IFile file = javaProject.getFile(JavaUtils.JAVA_SRC_DIRECTORY + "/" //$NON-NLS-1$
                            + JavaUtils.JAVA_ROUTINES_DIRECTORY + "/" //$NON-NLS-1$
                            + JavaUtils.JAVA_SYSTEM_ROUTINES_DIRECTORY + "/" + directory + module.getName()); //$NON-NLS-1$

                    copyFile(module, file);
                } else if (!module.getName().startsWith(".") && !FilesUtils.isSVNFolder(module.getName())) {
                    syncModules(module.listFiles(), directory + module.getName() + "/");
                }
            }
        } catch (CoreException e) {
            throw new SystemException(e);
        } catch (FileNotFoundException e) {
            throw new SystemException(e);
        } catch (IOException e) {
            throw new SystemException(e);
        } catch (Exception e) {
            throw new SystemException(e);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.codegen.ITalendSynchronizer#getFile(org.talend.core .model.properties.Item)
     */
    public IFile getFile(Item item) throws SystemException {
        if (item instanceof RoutineItem) {
            return getRoutineFile((RoutineItem) item);
        } else if (item instanceof ProcessItem) {
            return getProcessFile((ProcessItem) item);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * qli modified to fix the bug 5400 and 6185.
     * 
     * @seeorg.talend.designer.codegen.AbstractRoutineSynchronizer#renameRoutineClass(org.talend.core.model.properties.
     * RoutineItem, java.lang.String)
     */
    @Override
    public void renameRoutineClass(RoutineItem routineItem) {
        if (routineItem == null) {
            return;
        }
        String routineContent = new String(routineItem.getContent().getInnerContent());
        String label = routineItem.getProperty().getLabel();
        //
        String regexp = "public(\\s)+class(\\s)+\\w+(\\s)+\\{";//$NON-NLS-1$
        routineContent = routineContent.replaceFirst(regexp, "public class " + label + " {");//$NON-NLS-1$//$NON-NLS-2$
        routineItem.getContent().setInnerContent(routineContent.getBytes());
    }

    /*
     * (non-Javadoc)
     * 
     * qli modified to fix the bug 5400 and 6185.
     * 
     * @seeorg.talend.designer.codegen.AbstractRoutineSynchronizer#renameRoutineClass(org.talend.core.model.properties.
     * RoutineItem, java.lang.String)
     */
    @Override
    public void renameBeanClass(Item beanItem) {

    }

    public void deleteRoutinefile(IRepositoryViewObject objToDelete) {
        try {
            IRunProcessService service = CodeGeneratorActivator.getDefault().getRunProcessService();
            IProject javaProject = service.getProject(ECodeLanguage.JAVA);
            IFile file = javaProject.getFile(JavaUtils.JAVA_SRC_DIRECTORY + "/" + JavaUtils.JAVA_ROUTINES_DIRECTORY + "/" //$NON-NLS-1$ //$NON-NLS-2$
                    + objToDelete.getLabel() + JavaUtils.JAVA_EXTENSION);
            /*
             * File f = file.getLocation().toFile(); f.delete();
             */
            file.delete(true, null);
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void deleteBeanfile(IRepositoryViewObject objToDelete) {
        try {
            IRunProcessService service = CodeGeneratorActivator.getDefault().getRunProcessService();
            IProject javaProject = service.getProject(ECodeLanguage.JAVA);
            IFile file = javaProject.getFile(JavaUtils.JAVA_SRC_DIRECTORY + "/" + JavaUtils.JAVA_BEANS_DIRECTORY + "/" //$NON-NLS-1$ //$NON-NLS-2$
                    + objToDelete.getLabel() + JavaUtils.JAVA_EXTENSION);
            /*
             * File f = file.getLocation().toFile(); f.delete();
             */
            file.delete(true, null);
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public IFile getRoutinesFile(Item item) throws SystemException {
        try {
            if (item instanceof RoutineItem) {
                RoutineItem routineItem = (RoutineItem) item;
                ProjectManager projectManager = ProjectManager.getInstance();
                org.talend.core.model.properties.Project project = projectManager.getProject(routineItem);
                IProject iProject = ResourcesPlugin.getWorkspace().getRoot().getProject(project.getTechnicalLabel());
                String repositoryPath = ERepositoryObjectType.getFolderName(ERepositoryObjectType.ROUTINES);
                String folderPath = RepositoryNodeUtilities.getPath(routineItem.getProperty().getId()).toString();
                String fileName = routineItem.getProperty().getLabel() + "_" + routineItem.getProperty().getVersion()
                        + JavaUtils.ITEM_EXTENSION;
                String path = null;
                if (folderPath != null && !folderPath.trim().equals("")) {
                    path = repositoryPath + "/" + folderPath + "/" + fileName;
                } else {
                    path = repositoryPath + "/" + fileName;
                }

                IFile file = iProject.getFile(path);
                return file;
            }
        } catch (Exception e) {
            throw new SystemException(e);
        }
        return null;
    }
}
