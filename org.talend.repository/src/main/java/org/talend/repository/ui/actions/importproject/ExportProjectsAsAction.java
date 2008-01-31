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
package org.talend.repository.ui.actions.importproject;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.undo.CreateFileOperation;
import org.eclipse.ui.ide.undo.DeleteResourcesOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.utils.data.container.RootContainer;
import org.talend.core.CorePlugin;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.general.ILibrariesService;
import org.talend.core.model.general.Project;
import org.talend.core.model.properties.RoutineItem;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.designer.core.model.utils.emf.component.IMPORTType;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.ResourceModelUtils;
import org.talend.repository.ui.wizards.newproject.copyfromeclipse.TalendZipFileExportWizard;

/**
 * Action used to refresh a repository view.<br/>
 * 
 * $Id: RefreshAction.java 824 2006-12-01 15:49:55 +0000 (ven., 01 déc. 2006) smallet $
 * 
 */
public class ExportProjectsAsAction extends Action implements IWorkbenchWindowActionDelegate {

    private static final String LIB = "lib";

    private static final String CODE = "code";

    private static final String TYPE = "FOLDER";

    private IWorkbenchWindow window;

    public ExportProjectsAsAction() {
        super();
    }

    public void run() {
        initializeExternalLibraries();
        Shell activeShell = Display.getCurrent().getActiveShell();
        TalendZipFileExportWizard docWizard = new TalendZipFileExportWizard();
        WizardDialog dialog = new WizardDialog(activeShell, docWizard);
        docWizard.setWindowTitle(Messages.getString("ExportProjectsAsAction.actionTitle"));
        dialog.create();
        dialog.open();

        clearExternalLibraries();

    }

    /**
     * DOC zwang Comment method "clearExternalLibraries".
     */
    private void clearExternalLibraries() {
        List<IResource> resourcesToDelete = new ArrayList<IResource>();
        IResource[] resourceArray = null;

        try {
            IProxyRepositoryFactory repositoryFactory = ProxyRepositoryFactory.getInstance();
            Project[] projects = repositoryFactory.readProject();
            for (Project project : projects) {
                IProject fsProject = ResourceModelUtils.getProject(project);
                IFolder libJavaFolder = fsProject.getFolder(ExportProjectsAsAction.LIB);
                if (!libJavaFolder.exists()) {
                    continue;
                }

                final DeleteResourcesOperation operation = new DeleteResourcesOperation(new IResource[] { libJavaFolder },
                        IDEWorkbenchMessages.DeleteResourceAction_operationLabel, true);
                PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(operation, null,
                        WorkspaceUndoUtil.getUIInfoAdapter(window.getShell()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * DOC bqian Comment method "test".
     */
    private void initializeExternalLibraries() {
        initializeLibPath();
        // final InputStream initialContents = null;
        final Map<Project, List<LinkTargetStore>> map = getProjectAndRelatedLinks();

        IRunnableWithProgress op = new IRunnableWithProgress() {

            public void run(IProgressMonitor monitor) {
                Set<Project> projects = map.keySet();
                monitor.beginTask("Create external libraries' links", projects.size());

                for (Project project : projects) {
                    monitor.setTaskName("Process project" + project.getLabel());
                    List<LinkTargetStore> links = map.get(project);

                    for (LinkTargetStore store : links) {
                        CreateFileOperation op = new CreateFileOperation(store.file, store.uri, null,
                                IDEWorkbenchMessages.WizardNewFileCreationPage_title);
                        try {
                            PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(op, monitor,
                                    WorkspaceUndoUtil.getUIInfoAdapter(window.getShell()));
                        } catch (final ExecutionException e) {
                            ExceptionHandler.process(e);
                        }
                    }
                    monitor.worked(1);
                }
                monitor.done();
            }
        };
        try {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, true, op);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * DOC bqian Comment method "getProjectAndRelatedLinks".
     * 
     * @return
     */
    private Map<Project, List<LinkTargetStore>> getProjectAndRelatedLinks() {
        Map<Project, List<LinkTargetStore>> map = new HashMap<Project, List<LinkTargetStore>>();
        IProxyRepositoryFactory repositoryFactory = ProxyRepositoryFactory.getInstance();
        try {
            Project[] projects = repositoryFactory.readProject();
            for (Project project : projects) {
                IProject fsProject = ResourceModelUtils.getProject(project);
                IFolder libJavaFolder = fsProject.getFolder(ExportProjectsAsAction.CODE);
                if (libJavaFolder.exists()) {
                    List<LinkTargetStore> links = getLinksFromProject(project);
                    map.put(project, links);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    /**
     * DOC zwang Comment method "getLinksFromProject".
     * 
     * @param project
     * @return
     */
    private List<LinkTargetStore> getLinksFromProject(Project project) {
        List<IMPORTType> imports = null;
        String linkTarget = null;

        RootContainer<String, IRepositoryObject> routines = getRouineFromProject(project);
        List<LinkTargetStore> paths = new ArrayList<LinkTargetStore>();
        String language = project.getLanguage().getName().trim();

        for (IRepositoryObject obj : routines.getMembers()) {
            RoutineItem routine = (RoutineItem) obj.getProperty().getItem();
            imports = routine.getImports();

            if (language != null) {
                if (project != null && ECodeLanguage.JAVA.getName().equals(language)) {

                    IPath containerPath = getContainerFullPath(project.getTechnicalLabel());
                    for (IMPORTType importType : imports) {
                        try {
                            LinkTargetStore store = new LinkTargetStore();
                            IPath newFilePath = containerPath.append(ExportProjectsAsAction.LIB + java.io.File.separatorChar
                                    + ECodeLanguage.JAVA.getName() + java.io.File.separatorChar + importType.getMODULE());

                            linkTarget = EXTERNAL_LIB_JAVA_PATH + java.io.File.separatorChar + importType.getMODULE();

                            URI path = new URI(linkTarget.replace(java.io.File.separatorChar, '/'));
                            store.file = ResourcesPlugin.getWorkspace().getRoot().getFile(newFilePath);
                            store.uri = path;
                            paths.add(store);
                        } catch (Exception e) {
                            ExceptionHandler.process(e);
                        }
                    }
                }
            }
        }
        return paths;
    }

    /**
     * DOC bqian Comment method "getRouineFromProject".
     * 
     * @param project
     */
    private RootContainer<String, IRepositoryObject> getRouineFromProject(Project project) {
        ProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();

        RootContainer<String, IRepositoryObject> routines = null;
        try {
            routines = factory.getRoutineFromProject(project);
        } catch (PersistenceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return routines;

    }

    public final static String EXTERNAL_LIB_JAVA_PATH = "external_lib_java_path";

    public final static String EXTERNAL_LIB_PERL_PATH = "external_lib_perl_path";

    /**
     * DOC bqian Comment method "initializeLibPath".
     */
    private void initializeLibPath() {
        IPathVariableManager pathVariableManager = ResourcesPlugin.getWorkspace().getPathVariableManager();
        ILibrariesService libService = CorePlugin.getDefault().getLibrariesService();
        String libPath = libService.getLibrariesPath();
        try {
            Pattern javaPattern = Pattern.compile("(.*)java$");
            Pattern perlPattern = Pattern.compile("(.*)perl$");
            Matcher javaMatcher = javaPattern.matcher(libPath);
            Matcher perlMatcher = perlPattern.matcher(libPath);
            if (javaMatcher.find()) {
                pathVariableManager.setValue(EXTERNAL_LIB_JAVA_PATH, new Path(libPath));
                pathVariableManager.setValue(EXTERNAL_LIB_PERL_PATH,
                        new Path(javaMatcher.group(1) + ECodeLanguage.PERL.getName()));
            } else if (perlMatcher.find()) {
                pathVariableManager.setValue(EXTERNAL_LIB_PERL_PATH, new Path(libPath));
                pathVariableManager.setValue(EXTERNAL_LIB_JAVA_PATH,
                        new Path(perlMatcher.group(1) + ECodeLanguage.JAVA.getName()));
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
    }

    /**
     * Returns the currently entered container name. Null if the field is empty. Note that the container may not exist
     * yet if the user entered a new container name in the field.
     * 
     * @return IPath
     */
    public IPath getContainerFullPath(String pathName) {
        // TODO the name of current project
        if (pathName == null || pathName.length() < 1) {
            return null;
        }
        // The user may not have made this absolute so do it for them
        return (new Path(pathName)).makeAbsolute();
    }

    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
        this.window = window;
    }

    public void run(IAction action) {
        run();
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }

    /**
     * use to store the file and the file's corresponding link target temporarily. <br/>
     * 
     * $Id: talend.epf 1 2006-09-29 17:06:40 +0000 (ææäº, 29 ä¹æ 2006) nrousseau $
     * 
     */
    class LinkTargetStore {

        IFile file;

        URI uri;
    }

}
