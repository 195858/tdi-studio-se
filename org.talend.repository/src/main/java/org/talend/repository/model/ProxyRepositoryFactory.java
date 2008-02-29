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
package org.talend.repository.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.EList;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.LoginException;
import org.talend.commons.exception.MessageBoxExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.utils.data.container.RootContainer;
import org.talend.commons.utils.generation.JavaUtils;
import org.talend.commons.utils.workbench.resources.ResourceUtils;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.language.LanguageManager;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.general.Project;
import org.talend.core.model.metadata.builder.connection.AbstractMetadataObject;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.metadata.builder.connection.SubItemHelper;
import org.talend.core.model.metadata.builder.connection.TableHelper;
import org.talend.core.model.migration.IMigrationToolService;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.ContextItem;
import org.talend.core.model.properties.FolderItem;
import org.talend.core.model.properties.FolderType;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.JobDocumentationItem;
import org.talend.core.model.properties.JobletDocumentationItem;
import org.talend.core.model.properties.JobletProcessItem;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.Property;
import org.talend.core.model.properties.SpagoBiServer;
import org.talend.core.model.properties.Status;
import org.talend.core.model.properties.User;
import org.talend.core.model.properties.UserProjectAuthorization;
import org.talend.core.model.properties.UserProjectAuthorizationType;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.Folder;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.repository.documentation.ERepositoryActionName;
import org.talend.repository.i18n.Messages;
import org.talend.repository.ui.utils.JavaResourcesHelper;
import org.talend.repository.ui.utils.PerlResourcesHelper;
import org.talend.repository.ui.views.RepositoryContentProvider.ISubRepositoryObject;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobJavaScriptsManager;
import org.talend.repository.utils.RepositoryPathProvider;

/**
 * Repository factory use by client. Based on implementation provide by extension point system. This class contains all
 * commons treatments done by repository whatever implementation.<br/>
 * 
 * $Id$
 * 
 */
/**
 * DOC Administrator class global comment. Detailled comment
 */
public final class ProxyRepositoryFactory implements IProxyRepositoryFactory {

    private static Logger log = Logger.getLogger(ProxyRepositoryFactory.class);

    private IRepositoryFactory repositoryFactoryFromProvider;

    private static ProxyRepositoryFactory singleton = null;

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
        if (l == null) {
            throw new IllegalArgumentException();
        }
        support.addPropertyChangeListener(l);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
        if (l != null) {
            support.removePropertyChangeListener(l);
        }
    }

    protected void fireRepositoryPropertyChange(String property, Object oldValue, Object newValue) {
        if (support.hasListeners(property)) {
            support.firePropertyChange(property, oldValue, newValue);
        }
    }

    /**
     * DOC smallet ProxyRepositoryFactory constructor comment.
     */
    private ProxyRepositoryFactory() {
        // TODO Auto-generated constructor stub
    }

    public static synchronized ProxyRepositoryFactory getInstance() {
        if (singleton == null) {
            singleton = new ProxyRepositoryFactory();
        }
        return singleton;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#refreshJobPictureFolder()
     */
    public void refreshJobPictureFolder(String picFolder) {
        IFolder folder = RepositoryPathProvider.getFolder(picFolder);
        try {
            folder.refreshLocal(1, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#refreshJobPictureFolder()
     */
    public void refreshDocumentationFolder(String docFolder) {
        try {
            IProject project = ResourceModelUtils.getProject(getRepositoryContext().getProject());
            project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getRepositoryContext()
     */
    public RepositoryContext getRepositoryContext() {
        Context ctx = CorePlugin.getContext();
        return (RepositoryContext) ctx.getProperty(Context.REPOSITORY_CONTEXT_KEY);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getRepositoryFactoryFromProvider()
     */
    public IRepositoryFactory getRepositoryFactoryFromProvider() {
        return this.repositoryFactoryFromProvider;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#setRepositoryFactoryFromProvider(org.talend.repository.model.IRepositoryFactory)
     */
    public void setRepositoryFactoryFromProvider(IRepositoryFactory repositoryFactoryFromProvider) {
        this.repositoryFactoryFromProvider = repositoryFactoryFromProvider;
    }

    private void checkFileName(String fileName, String pattern) {
        if (!Pattern.matches(pattern, fileName)) {
            // i18n
            // throw new IllegalArgumentException("Label " + fileName + " does not match pattern " + pattern);
            throw new IllegalArgumentException(Messages.getString(
                    "ProxyRepositoryFactory.illegalArgumentException.labelNotMatchPattern", new String[] { fileName, pattern })); //$NON-NLS-1$
        }
    }

    private void checkFileNameAndPath(Item item, String pattern, IPath path, boolean folder) throws PersistenceException {
        String fileName = item.getProperty().getLabel();
        checkFileName(fileName, pattern);
        if (!this.repositoryFactoryFromProvider.isNameAvailable(item, null)) {
            // i18n
            // throw new IllegalArgumentException("Label " + fileName + " is already in use");
            throw new IllegalArgumentException(Messages.getString(
                    "ProxyRepositoryFactory.illegalArgumentException.labeAlreadyInUse", new String[] { fileName })); //$NON-NLS-1$
        }
    }

    private void checkFileNameAndPath(String label, String pattern, ERepositoryObjectType type, IPath path, boolean folder)
            throws PersistenceException {
        String fileName = label;
        checkFileName(fileName, pattern);
        if (!this.repositoryFactoryFromProvider.isPathValid(type, path, label)) {
            // i18n
            // throw new IllegalArgumentException("Label " + fileName + " is already in use");
            throw new IllegalArgumentException(Messages.getString(
                    "ProxyRepositoryFactory.illegalArgumentException.labeAlreadyInUse", new String[] { fileName })); //$NON-NLS-1$
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getMetadataConnectionsItem()
     */
    public List<ConnectionItem> getMetadataConnectionsItem() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getMetadataConnectionsItem();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getMetadataConnectionsItem()
     */
    public List<ContextItem> getContextItem() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getContextItem();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#isValid(org.talend.core.model.general.Project,
     * org.talend.core.model.repository.ERepositoryObjectType, org.eclipse.core.runtime.IPath, java.lang.String)
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#isNameAvailable(org.talend.core.model.properties.Item,
     * java.lang.String)
     */
    public boolean isNameAvailable(Item item, String name) throws PersistenceException {
        return this.repositoryFactoryFromProvider.isNameAvailable(item, name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#isPathValid(org.talend.core.model.repository.ERepositoryObjectType,
     * org.eclipse.core.runtime.IPath, java.lang.String)
     */
    public boolean isPathValid(ERepositoryObjectType type, IPath path, String label) throws PersistenceException {
        return this.repositoryFactoryFromProvider.isPathValid(type, path, label);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#createProject(java.lang.String, java.lang.String,
     * org.talend.core.model.temp.ECodeLanguage, org.talend.core.model.properties.User)
     */
    public Project createProject(String label, String description, ECodeLanguage language, User author)
            throws PersistenceException {
        checkFileName(label, RepositoryConstants.PROJECT_PATTERN);
        Project toReturn = this.repositoryFactoryFromProvider.createProject(label, description, language, author);

        IMigrationToolService service = (IMigrationToolService) GlobalServiceRegister.getDefault().getService(
                IMigrationToolService.class);
        service.initNewProjectTasks(toReturn);

        return toReturn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#createFolder(org.talend.core.model.repository.ERepositoryObjectType,
     * org.eclipse.core.runtime.IPath, java.lang.String)
     */
    public Folder createFolder(ERepositoryObjectType type, IPath path, String label) throws PersistenceException {
        checkFileNameAndPath(label, RepositoryConstants.FOLDER_PATTERN, type, path, true);
        Folder createFolder = this.repositoryFactoryFromProvider.createFolder(type, path, label);
        if (type == ERepositoryObjectType.PROCESS || type == ERepositoryObjectType.JOBLET) {
            fireRepositoryPropertyChange(ERepositoryActionName.FOLDER_CREATE.getName(), path, new Object[] { createFolder, type });
        }
        return createFolder;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#deleteFolder(org.talend.core.model.repository.ERepositoryObjectType,
     * org.eclipse.core.runtime.IPath)
     */
    public synchronized void deleteFolder(ERepositoryObjectType type, IPath path) throws PersistenceException {
        this.repositoryFactoryFromProvider.deleteFolder(type, path);
        if (type == ERepositoryObjectType.PROCESS) {
            fireRepositoryPropertyChange(ERepositoryActionName.FOLDER_DELETE.getName(), path, type);
        }
        if (type == ERepositoryObjectType.JOBLET) {
            fireRepositoryPropertyChange(ERepositoryActionName.JOBLET_FOLDER_DELETE.getName(), path, type);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#moveFolder(org.talend.core.model.repository.ERepositoryObjectType,
     * org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath)
     */
    public void moveFolder(ERepositoryObjectType type, IPath sourcePath, IPath targetPath) throws PersistenceException {
        this.repositoryFactoryFromProvider.moveFolder(type, sourcePath, targetPath);
        if (type == ERepositoryObjectType.PROCESS) {
            fireRepositoryPropertyChange(ERepositoryActionName.FOLDER_MOVE.getName(), sourcePath, targetPath);
        }
        if (type == ERepositoryObjectType.JOBLET) {
            fireRepositoryPropertyChange(ERepositoryActionName.JOBLET_FOLDER_MOVE.getName(), sourcePath, targetPath);
        }
        this.repositoryFactoryFromProvider.updateItemsPath(type, targetPath.append(sourcePath.lastSegment()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getBusinessProcess()
     */
    public RootContainer<String, IRepositoryObject> getBusinessProcess() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getBusinessProcess();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getDocumentation()
     */
    public RootContainer<String, IRepositoryObject> getDocumentation() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getDocumentation();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getMetadataConnection()
     */
    public RootContainer<String, IRepositoryObject> getMetadataConnection() throws PersistenceException {
        RootContainer<String, IRepositoryObject> metadataConnection = this.repositoryFactoryFromProvider.getMetadataConnection();

        return metadataConnection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getMetadataFileDelimited()
     */
    public RootContainer<String, IRepositoryObject> getMetadataFileDelimited() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getMetadataFileDelimited();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getNextId()
     */
    public String getNextId() {
        String nextId = this.repositoryFactoryFromProvider.getNextId();

        // i18n
        // log.trace("New ID generated on project [" + getRepositoryContext().getProject() + "] = " + nextId);
        String str[] = new String[] { getRepositoryContext().getProject() + "", nextId + "" };//$NON-NLS-1$ //$NON-NLS-2$
        log.trace(Messages.getString("ProxyRepositoryFactory.log.newIdGenerated", str)); //$NON-NLS-1$
        return nextId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getProcess()
     */
    public RootContainer<String, IRepositoryObject> getProcess() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getProcess();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getProcess()
     */
    public RootContainer<String, IRepositoryObject> getContext() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getContext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getRoutine()
     */
    public RootContainer<String, IRepositoryObject> getRoutine() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getRoutine();
    }

    public RootContainer<String, IRepositoryObject> getRoutineFromProject(Project project) throws PersistenceException {
        return this.repositoryFactoryFromProvider.getRoutineFromProject(project);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getSnippets()
     */
    public RootContainer<String, IRepositoryObject> getSnippets() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getSnippets();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getRecycleBinItems()
     */
    public List<IRepositoryObject> getRecycleBinItems() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getRecycleBinItems();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#readProject()
     */
    public Project[] readProject() throws PersistenceException, BusinessException {
        return this.repositoryFactoryFromProvider.readProject();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#renameFolder(org.talend.core.model.repository.ERepositoryObjectType,
     * org.eclipse.core.runtime.IPath, java.lang.String)
     */
    public void renameFolder(ERepositoryObjectType type, IPath path, String label) throws PersistenceException {
        this.repositoryFactoryFromProvider.renameFolder(type, path, label);
        if (type == ERepositoryObjectType.PROCESS) {
            fireRepositoryPropertyChange(ERepositoryActionName.FOLDER_RENAME.getName(), path, label);
        }
        if (type == ERepositoryObjectType.JOBLET) {
            fireRepositoryPropertyChange(ERepositoryActionName.JOBLET_FOLDER_RENAME.getName(), path, label);
        }
        this.repositoryFactoryFromProvider.updateItemsPath(type, path.removeLastSegments(1).append(label));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#deleteObject(org.talend.core.model.general.Project,
     * org.talend.core.model.repository.IRepositoryObject)
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#deleteObjectLogical(org.talend.core.model.repository.IRepositoryObject)
     */
    public void deleteObjectLogical(IRepositoryObject objToDelete) throws PersistenceException, BusinessException {
        checkAvailability(objToDelete);
        this.repositoryFactoryFromProvider.deleteObjectLogical(objToDelete);
        unlock(objToDelete);
        // i18n
        // log.debug("Logical deletion [" + objToDelete + "] by " + getRepositoryContext().getUser() + ".");
        String str[] = new String[] { objToDelete + "", getRepositoryContext().getUser() + "" };//$NON-NLS-1$ //$NON-NLS-2$
        log.debug(Messages.getString("ProxyRepositoryFactory.log.logicalDeletion", str)); //$NON-NLS-1$

        // TODO this need to be refactered after M2.
        if (objToDelete.getType() == ERepositoryObjectType.PROCESS || objToDelete.getType() == ERepositoryObjectType.JOBLET
                || objToDelete.getType() == ERepositoryObjectType.ROUTINES) {
            fireRepositoryPropertyChange(ERepositoryActionName.JOB_DELETE_TO_RECYCLE_BIN.getName(), null, objToDelete);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#deleteObjectPhysical(org.talend.core.model.repository.IRepositoryObject)
     */
    public void forceDeleteObjectPhysical(IRepositoryObject objToDelete) throws PersistenceException {
        this.repositoryFactoryFromProvider.deleteObjectPhysical(objToDelete);
        // i18n
        // log.info("Physical deletion [" + objToDelete + "] by " + getRepositoryContext().getUser() + ".");
        String str[] = new String[] { objToDelete.toString(), getRepositoryContext().getUser().toString() };
        log.info(Messages.getString("ProxyRepositoryFactory.log.physicalDeletion", str)); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#deleteObjectPhysical(org.talend.core.model.repository.IRepositoryObject)
     */
    public void deleteObjectPhysical(IRepositoryObject objToDelete) throws PersistenceException {
        this.repositoryFactoryFromProvider.deleteObjectPhysical(objToDelete);
        // i18n
        // log.info("Physical deletion [" + objToDelete + "] by " + getRepositoryContext().getUser() + ".");
        String str[] = new String[] { objToDelete.toString(), getRepositoryContext().getUser().toString() };
        log.info(Messages.getString("ProxyRepositoryFactory.log.physicalDeletion", str)); //$NON-NLS-1$

        if (objToDelete.getType() == ERepositoryObjectType.PROCESS || objToDelete.getType() == ERepositoryObjectType.JOBLET
                || objToDelete.getType() == ERepositoryObjectType.ROUTINES) {
            fireRepositoryPropertyChange(ERepositoryActionName.JOB_DELETE_FOREVER.getName(), null, objToDelete);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#restoreObject(org.talend.core.model.repository.IRepositoryObject,
     * org.eclipse.core.runtime.IPath)
     */
    public void restoreObject(IRepositoryObject objToRestore, IPath path) throws PersistenceException, BusinessException {
        if (ProxyRepositoryFactory.getInstance().isUserReadOnlyOnCurrentProject()) {
            throw new BusinessException(Messages.getString("ProxyRepositoryFactory.bussinessException.itemNonModifiable")); //$NON-NLS-1$
        }
        this.repositoryFactoryFromProvider.restoreObject(objToRestore, path);
        unlock(objToRestore);
        // i18n
        // log.debug("Restoration [" + objToRestore + "] by " + getRepositoryContext().getUser() + " to \"/" + path +
        // "\".");
        String str[] = new String[] { objToRestore + "", getRepositoryContext().getUser() + "", path + "" };//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        log.debug(Messages.getString("ProxyRepositoryFactory.log.Restoration", str)); //$NON-NLS-1$
        if (objToRestore.getType() == ERepositoryObjectType.PROCESS || objToRestore.getType() == ERepositoryObjectType.JOBLET
                || objToRestore.getType() == ERepositoryObjectType.ROUTINES) {
            fireRepositoryPropertyChange(ERepositoryActionName.JOB_RESTORE.getName(), null, objToRestore);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#moveObject(org.talend.core.model.general.Project,
     * org.talend.core.model.repository.IRepositoryObject)
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#moveObject(org.talend.core.model.repository.IRepositoryObject,
     * org.eclipse.core.runtime.IPath)
     */
    public void moveObject(IRepositoryObject objToMove, IPath targetPath, IPath... sourcePath) throws PersistenceException,
            BusinessException {
        checkAvailability(objToMove);
        checkFileNameAndPath(objToMove.getProperty().getItem(), RepositoryConstants.getPattern(objToMove.getType()), targetPath,
                false);
        this.repositoryFactoryFromProvider.moveObject(objToMove, targetPath);
        // i18n
        // log.debug("Move [" + objToMove + "] to \"" + path + "\".");
        String str[] = new String[] { objToMove + "", targetPath + "" }; //$NON-NLS-1$ //$NON-NLS-2$
        log.debug(Messages.getString("ProxyRepositoryFactory.log.move", str)); //$NON-NLS-1$
        unlock(getItem(objToMove));
        if (objToMove.getType() == ERepositoryObjectType.PROCESS) {
            if (sourcePath != null && sourcePath.length == 1) {
                fireRepositoryPropertyChange(ERepositoryActionName.JOB_MOVE.getName(), objToMove, new IPath[] { sourcePath[0],
                        targetPath });
            }
        }
        if (objToMove.getType() == ERepositoryObjectType.JOBLET) {
            if (sourcePath != null && sourcePath.length == 1) {
                fireRepositoryPropertyChange(ERepositoryActionName.JOBLET_MOVE.getName(), objToMove, new IPath[] { sourcePath[0],
                        targetPath });
            }
        }

    }

    // TODO SML Renommer et finir la m�thode et la plugger dans toutes les m�thodes
    private void checkAvailability(IRepositoryObject objToMove) throws BusinessException {
        if (!isEditableAndLockIfPossible(objToMove) || ProxyRepositoryFactory.getInstance().isUserReadOnlyOnCurrentProject()) {
            throw new BusinessException(Messages.getString("ProxyRepositoryFactory.bussinessException.itemNonModifiable")); //$NON-NLS-1$
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getMetadataFilePositional()
     */
    public RootContainer<String, IRepositoryObject> getMetadataFilePositional() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getMetadataFilePositional();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getMetadataFileRegexp()
     */
    public RootContainer<String, IRepositoryObject> getMetadataFileRegexp() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getMetadataFileRegexp();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getMetadataFileXml()
     */
    public RootContainer<String, IRepositoryObject> getMetadataFileXml() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getMetadataFileXml();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getMetadataFileLdif()
     */
    public RootContainer<String, IRepositoryObject> getMetadataFileLdif() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getMetadataFileLdif();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#lock(org.talend.core.model.repository.IRepositoryObject)
     */
    public void lock(IRepositoryObject obj) throws PersistenceException, BusinessException {
        lock(getItem(obj));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#lock(org.talend.core.model.properties.Item)
     */
    public void lock(Item item) throws PersistenceException, BusinessException {
        if (getStatus(item).isPotentiallyEditable()) {
            this.repositoryFactoryFromProvider.lock(item);
            // i18n
            // log.debug("Lock [" + item + "] by \"" + getRepositoryContext().getUser() + "\".");
            String str[] = new String[] { item.toString(), getRepositoryContext().getUser().toString() };
            log.debug(Messages.getString("ProxyRepositoryFactory.log.lock", str)); //$NON-NLS-1$
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#getAllVersion(org.talend.core.model.general.Project, int)
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getAllVersion(java.lang.String)
     */
    public List<IRepositoryObject> getAllVersion(String id) throws PersistenceException {
        return this.repositoryFactoryFromProvider.getAllVersion(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#getLastVersion(org.talend.core.model.general.Project, int)
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getLastVersion(java.lang.String)
     */
    public IRepositoryObject getLastVersion(String id) throws PersistenceException {
        return this.repositoryFactoryFromProvider.getLastVersion(id);
    }

    public List<IRepositoryObject> getAll(ERepositoryObjectType type) throws PersistenceException {
        return getAll(type, false);
    }

    public List<IRepositoryObject> getAll(ERepositoryObjectType type, boolean withDeleted) throws PersistenceException {
        return this.repositoryFactoryFromProvider.getAll(type, withDeleted);
    }

    public List<String> getFolders(ERepositoryObjectType type) throws PersistenceException {
        List<String> toReturn = new ArrayList<String>();
        Project project = getRepositoryContext().getProject();
        EList list = project.getEmfProject().getFolders();

        String[] split = ERepositoryObjectType.getFolderName(type).split("/"); //$NON-NLS-1$
        String labelType = split[split.length - 1];

        for (Object current : list) {
            FolderItem folderItem = (FolderItem) current;
            addChildren(toReturn, folderItem, labelType, ""); //$NON-NLS-1$
        }
        return toReturn;
    }

    private void addChildren(List<String> target, FolderItem source, String type, String path) {
        if (source.getType() == FolderType.FOLDER_LITERAL) {
            // FIXME mhelleboid Related to bug 364
            if (source.getProperty().getLabel().equals(".settings")) { //$NON-NLS-1$
                return;
            }
            target.add(path + source.getProperty().getLabel());

            for (Object current : source.getChildren()) {
                if (current instanceof FolderItem) {
                    addChildren(target, (FolderItem) current, type, path + source.getProperty().getLabel() + "/"); //$NON-NLS-1$
                }
            }
        }

        if (source.getType() == FolderType.SYSTEM_FOLDER_LITERAL || source.getType() == FolderType.STABLE_SYSTEM_FOLDER_LITERAL) {
            boolean match = source.getProperty().getLabel().equals(type);

            for (Object current : source.getChildren()) {
                if (current instanceof FolderItem) {
                    FolderItem currentChild = (FolderItem) current;
                    if (currentChild.getType() == FolderType.FOLDER_LITERAL && match) {
                        addChildren(target, currentChild, type, path);
                    } else if (currentChild.getType() == FolderType.SYSTEM_FOLDER_LITERAL
                            || currentChild.getType() == FolderType.STABLE_SYSTEM_FOLDER_LITERAL) {
                        addChildren(target, currentChild, type, path);
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getDocumentationStatus()
     */
    public List<Status> getDocumentationStatus() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getDocumentationStatus();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getTechnicalStatus()
     */
    public List<Status> getTechnicalStatus() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getTechnicalStatus();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getTechnicalStatus()
     */
    // public List<SpagoBiServer> getSpagoBiServer() throws PersistenceException {
    // return this.repositoryFactoryFromProvider.getSpagoBiServer();
    // }
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#setDocumentationStatus(java.util.List)
     */
    public void setDocumentationStatus(List<Status> list) throws PersistenceException {
        this.repositoryFactoryFromProvider.setDocumentationStatus(list);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#forceCreate(org.talend.core.model.properties.Item,
     * org.eclipse.core.runtime.IPath)
     */
    public void forceCreate(Item item, IPath path) throws PersistenceException {
        this.repositoryFactoryFromProvider.create(item, path);
        // if (item instanceof ProcessItem) {
        // fireRepositoryPropertyChange(ERepositoryActionName.JOB_CREATE.getName(), null, item);
        // }
    }

    public void createParentFoldersRecursively(ERepositoryObjectType itemType, IPath path) throws PersistenceException {
        List<String> folders = getFolders(itemType);

        for (int i = 0; i < path.segmentCount(); i++) {
            IPath parentPath = path.removeLastSegments(path.segmentCount() - i);
            String folderLabel = path.segment(i);

            String folderName = parentPath.append(folderLabel).toString();
            if (!folders.contains(folderName)) {
                createFolder(itemType, parentPath, folderLabel);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#setTechnicalStatus(java.util.List)
     */
    public void setTechnicalStatus(List<Status> list) throws PersistenceException {
        this.repositoryFactoryFromProvider.setTechnicalStatus(list);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#setSpagoBiServer(java.util.List)
     */
    public void setSpagoBiServer(List<SpagoBiServer> list) throws PersistenceException {
        this.repositoryFactoryFromProvider.setSpagoBiServer(list);
    }

    public void setMigrationTasksDone(Project project, List<String> list) throws PersistenceException {
        this.repositoryFactoryFromProvider.setMigrationTasksDone(project, list);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#isServerValid()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#isServerValid()
     */
    // public String isServerValid() {
    // return this.repositoryFactoryFromProvider.isServerValid();
    // }
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#create(org.talend.core.model.properties.Item,
     * org.eclipse.core.runtime.IPath)
     */
    public void create(Item item, IPath path, boolean... isImportItem) throws PersistenceException {
        checkFileNameAndPath(item, RepositoryConstants.getPattern(ERepositoryObjectType.getItemType(item)), path, false);
        this.repositoryFactoryFromProvider.create(item, path);
        if ((item instanceof ProcessItem || item instanceof JobletProcessItem)
                && (isImportItem == null || isImportItem.length == 0)) {
            fireRepositoryPropertyChange(ERepositoryActionName.JOB_CREATE.getName(), null, item);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#save(org.talend.core.model.properties.Item)
     */
    public void save(Item item, boolean... isMigrationTask) throws PersistenceException {
        this.repositoryFactoryFromProvider.save(item);
        if ((item instanceof ProcessItem || item instanceof JobletProcessItem)
                && (isMigrationTask == null || isMigrationTask.length == 0)) {
            fireRepositoryPropertyChange(ERepositoryActionName.JOB_SAVE.getName(), null, item);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#save(org.talend.core.model.properties.Property)
     */
    public void save(Property property, String... originalNameAndVersion) throws PersistenceException {
        this.repositoryFactoryFromProvider.save(property);
        if (property.getItem() instanceof ProcessItem || property.getItem() instanceof JobletProcessItem) {
            fireRepositoryPropertyChange(ERepositoryActionName.JOB_PROPERTIES_CHANGE.getName(), originalNameAndVersion, property);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#copy(org.talend.core.model.properties.Item,
     * org.eclipse.core.runtime.IPath)
     */
    public Item copy(Item sourceItem, IPath targetPath) throws PersistenceException, BusinessException {
        Item targetItem = this.repositoryFactoryFromProvider.copy(sourceItem, targetPath);

        if (sourceItem instanceof ProcessItem || sourceItem instanceof JobletProcessItem) {
            fireRepositoryPropertyChange(ERepositoryActionName.JOB_COPY.getName(), sourceItem, targetItem);
        }
        return targetItem;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#copy(org.talend.core.model.properties.Item,
     * org.eclipse.core.runtime.IPath)
     */
    public Item copy(Item sourceItem, IPath targetPath, String newName) throws PersistenceException, BusinessException {
        Item targetItem = this.repositoryFactoryFromProvider.copy(sourceItem, targetPath, newName);

        if (sourceItem instanceof ProcessItem || sourceItem instanceof JobletProcessItem) {
            fireRepositoryPropertyChange(ERepositoryActionName.JOB_COPY.getName(), sourceItem, targetItem);
        }
        return targetItem;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#reload(org.talend.core.model.properties.Property)
     */
    public Property reload(Property property) throws PersistenceException {
        return this.repositoryFactoryFromProvider.reload(property);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#unlock(org.talend.core.model.general.Project,
     * org.talend.core.model.repository.IRepositoryObject)
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#unlock(org.talend.core.model.repository.IRepositoryObject)
     */
    public void unlock(IRepositoryObject obj) throws PersistenceException {
        unlock(getItem(obj));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#unlock(org.talend.core.model.properties.Item)
     */
    public void unlock(Item obj) throws PersistenceException {
        if (getStatus(obj) == ERepositoryStatus.LOCK_BY_USER || obj instanceof JobletDocumentationItem
                || obj instanceof JobDocumentationItem) {
            Date commitDate = obj.getState().getCommitDate();
            Date modificationDate = obj.getProperty().getModificationDate();
            if (modificationDate == null || commitDate == null || modificationDate.before(commitDate)) {
                this.repositoryFactoryFromProvider.unlock(obj);

                // i18n
                // log.debug("Unlock [" + obj + "] by \"" + getRepositoryContext().getUser() + "\".");
                String str[] = new String[] { obj.toString(), getRepositoryContext().getUser().toString() };
                log.debug(Messages.getString("ProxyRepositoryFactory.log.unlock", str)); //$NON-NLS-1$
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#findUser(org.talend.core.model.general.Project)
     */
    // public boolean doesLoggedUserExist() throws PersistenceException {
    // return this.repositoryFactoryFromProvider.doesLoggedUserExist();
    // }
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#createUser(org.talend.core.model.general.Project)
     */
    // public void createUser() throws PersistenceException {
    // this.repositoryFactoryFromProvider.createUser();
    // }
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#initialize()
     */
    public void initialize() throws PersistenceException {
        this.repositoryFactoryFromProvider.initialize();
    }

    /**
     * @param project
     * @throws PersistenceException
     * @throws LoginException
     * @see org.talend.repository.model.IRepositoryFactory#logOnProject(org.talend.core.model.general.Project)
     */
    public void logOnProject(Project project) throws PersistenceException, LoginException {
        IMigrationToolService service = (IMigrationToolService) GlobalServiceRegister.getDefault().getService(
                IMigrationToolService.class);
        service.executeProjectTasks(project, true);

        getRepositoryContext().setProject(project);
        LanguageManager.reset();
        this.repositoryFactoryFromProvider.logOnProject(project);

        emptyTempFolder(project);

        // i18n
        // log.info(getRepositoryContext().getUser() + " logged on " + getRepositoryContext().getProject());
        String str[] = new String[] { getRepositoryContext().getUser() + "", getRepositoryContext().getProject() + "" }; //$NON-NLS-1$ //$NON-NLS-2$        
        log.info(Messages.getString("ProxyRepositoryFactory.log.loggedOn", str)); //$NON-NLS-1$

        try {
            CorePlugin.getDefault().getLibrariesService().syncLibraries();
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }

        service.executeProjectTasks(project, false);
    }

    /**
     * DOC smallet Comment method "emptyTempFolder".
     * 
     * @param project
     * @throws PersistenceException
     */
    private void emptyTempFolder(Project project) throws PersistenceException {
        long start = System.currentTimeMillis();
        IProject fsProject = ResourceModelUtils.getProject(project);
        IFolder folder = ResourceUtils.getFolder(fsProject, RepositoryConstants.TEMP_DIRECTORY, true);
        int nbResourcesDeleted = ResourceUtils.emptyFolder(folder);
        long elapsedTime = System.currentTimeMillis() - start;
        log.trace(Messages.getString("ProxyRepositoryFactory.log.tempFolderEmptied", nbResourcesDeleted, elapsedTime)); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#getStatus(org.talend.core.model.properties.Item)
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getStatus(org.talend.core.model.repository.IRepositoryObject)
     */
    public ERepositoryStatus getStatus(IRepositoryObject obj) {
        if (obj instanceof ISubRepositoryObject) {
            ISubRepositoryObject subRepositoryObject = (ISubRepositoryObject) obj;
            if (SubItemHelper.isDeleted(subRepositoryObject.getAbstractMetadataObject())) {
                return ERepositoryStatus.DELETED;
            }
        }
        return getStatus(getItem(obj));
    }

    @Deprecated
    public boolean isDeleted(MetadataTable table) {
        // TODO SML/MHE Remove when table are items
        if (TableHelper.isDeleted(table)) {
            return true;
        }
        return false;
    }

    public boolean isUserReadOnlyOnCurrentProject() {
        RepositoryContext repositoryContext = getRepositoryContext();
        Project project = repositoryContext.getProject();

        EList userAuthorizations = project.getEmfProject().getUserAuthorization();
        for (Object o : userAuthorizations.toArray()) {
            UserProjectAuthorization userProjectAuthorization = (UserProjectAuthorization) o;
            if (userProjectAuthorization.getUser() != null) {
                if (userProjectAuthorization.getUser().getLogin().equals(repositoryContext.getUser().getLogin())) {
                    UserProjectAuthorizationType type = userProjectAuthorization.getType();
                    return type.getValue() == UserProjectAuthorizationType.READ_ONLY;
                }
            }
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getStatus(org.talend.core.model.properties.Item)
     */
    public ERepositoryStatus getStatus(Item item) {
        // PTODO SML [FOLDERS] temp code
        ERepositoryStatus toReturn;
        if (item instanceof FolderItem) {
            toReturn = ERepositoryStatus.EDITABLE;
        } else {
            toReturn = this.repositoryFactoryFromProvider.getStatus(item);
        }

        if (toReturn != ERepositoryStatus.DELETED && isUserReadOnlyOnCurrentProject()) {
            return ERepositoryStatus.READ_ONLY;
        }

        return toReturn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#getStatusAndLockIfPossible(org.talend.core.model.properties.Item)
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#isEditableAndLockIfPossible(org.talend.core.model.properties.Item)
     */
    public boolean isEditableAndLockIfPossible(Item item) {
        ERepositoryStatus status = getStatus(item);
        if (status.isPotentiallyEditable()) {
            try {
                lock(item);
            } catch (PersistenceException e) {
                MessageBoxExceptionHandler.process(e);
            } catch (BusinessException e) {
                // Nothing to do
            }
            status = getStatus(item);
        }

        return status.isEditable();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#isEditable(org.talend.core.model.repository.IRepositoryObject)
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#isEditableAndLockIfPossible(org.talend.core.model.repository.IRepositoryObject)
     */
    public boolean isEditableAndLockIfPossible(IRepositoryObject obj) {
        if (obj instanceof ISubRepositoryObject) {
            AbstractMetadataObject abstractMetadataObject = ((ISubRepositoryObject) obj).getAbstractMetadataObject();
            if (SubItemHelper.isDeleted(abstractMetadataObject)) {
                return false;
            } else {
                return isEditableAndLockIfPossible(getItem(obj));
            }
        } else {
            return isEditableAndLockIfPossible(getItem(obj));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#isPotentiallyEditable(org.talend.core.model.properties.Item)
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#isPotentiallyEditable(org.talend.core.model.properties.Item)
     */
    private boolean isPotentiallyEditable(Item item) {
        ERepositoryStatus status = getStatus(item);
        return status.isPotentiallyEditable();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IRepositoryFactory#isPotentiallyEditable(org.talend.core.model.repository.IRepositoryObject)
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#isPotentiallyEditable(org.talend.core.model.repository.IRepositoryObject)
     */
    public boolean isPotentiallyEditable(IRepositoryObject obj) {
        if (obj instanceof ISubRepositoryObject) {
            AbstractMetadataObject abstractMetadataObject = ((ISubRepositoryObject) obj).getAbstractMetadataObject();
            if (SubItemHelper.isDeleted(abstractMetadataObject)) {
                return false;
            } else {
                return isPotentiallyEditable(getItem(obj));
            }
        } else {
            return isPotentiallyEditable(getItem(obj));
        }
    }

    private Item getItem(IRepositoryObject obj) {
        return obj.getProperty().getItem();
    }

    public List<org.talend.core.model.properties.Project> getReferencedProjects() {
        return this.repositoryFactoryFromProvider.getReferencedProjects();
    }

    public void removeContextFiles(IProcess process, IContext context) throws Exception {
        IResource resource = getContextResource(process, context);
        if (resource != null) {
            resource.delete(true, null);
        }
    }

    /**
     * Gets the context file resource according to the project type.
     * 
     * @param process
     * @param context
     * @return
     */
    private IResource getContextResource(IProcess process, IContext context) throws Exception {
        switch (((RepositoryContext) CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY)).getProject()
                .getLanguage()) {
        case JAVA:
            IPath path = new Path(JavaUtils.JAVA_SRC_DIRECTORY).append(JavaResourcesHelper.getCurrentProjectName()).append(
                    JavaResourcesHelper.getJobFolderName(process.getName())).append(JobJavaScriptsManager.JOB_CONTEXT_FOLDER)
                    .append(context.getName() + JavaUtils.JAVA_CONTEXT_EXTENSION);
            return JavaResourcesHelper.getSpecificResourceInJavaProject(path);
        case PERL:
            String contextFullName = PerlResourcesHelper.getCurrentProjectName()
                    + ".job_" + PerlResourcesHelper.escapeSpace(process.getName()) + "_" //$NON-NLS-1$ //$NON-NLS-2$
                    + PerlResourcesHelper.escapeSpace(context.getName()) + PerlResourcesHelper.CONTEXT_FILE_SUFFIX;
            return PerlResourcesHelper.getSpecificResourceInPerlProject(new Path(contextFullName));
        }
        return null;
    }

    public Boolean hasChildren(Object parent) {
        return repositoryFactoryFromProvider.hasChildren(parent);
    }

    public RootContainer<String, IRepositoryObject> getMetadataGenericSchema() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getMetadataGenericSchema();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getMetadataLDAPSchema()
     */
    public RootContainer<String, IRepositoryObject> getMetadataLDAPSchema() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getMetadataLDAPSchema();
    }

    public List<ModuleNeeded> getModulesNeededForJobs() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getModulesNeededForJobs();
    }

    public RootContainer<String, IRepositoryObject> getJoblets() throws PersistenceException {
        return this.repositoryFactoryFromProvider.getJoblets();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProxyRepositoryFactory#getMetadataWSDLSchema()
     */
    public RootContainer<String, IRepositoryObject> getMetadataWSDLSchema() throws PersistenceException {
        // TODO Auto-generated method stub
        return this.repositoryFactoryFromProvider.getMetadataWSDLSchema();
    }

    /**
     * DOC tang Comment method "logOnProject".
     * 
     * @param project
     * @param monitorWrap
     * @throws PersistenceException
     * @throws LoginException
     */
    public void logOnProject(Project project, IProgressMonitor monitorWrap) throws LoginException, PersistenceException {
        IMigrationToolService service = (IMigrationToolService) GlobalServiceRegister.getDefault().getService(
                IMigrationToolService.class);
        service.executeProjectTasks(project, true, monitorWrap);

        monitorWrap.setTaskName(Messages.getString("ProxyRepositoryFactory.logonInProgress")); //$NON-NLS-1$
        monitorWrap.worked(1);
        getRepositoryContext().setProject(project);
        LanguageManager.reset();
        this.repositoryFactoryFromProvider.logOnProject(project);

        emptyTempFolder(project);

        // i18n
        // log.info(getRepositoryContext().getUser() + " logged on " + getRepositoryContext().getProject());
        String str[] = new String[] { getRepositoryContext().getUser() + "", getRepositoryContext().getProject() + "" }; //$NON-NLS-1$ //$NON-NLS-2$        
        log.info(Messages.getString("ProxyRepositoryFactory.log.loggedOn", str)); //$NON-NLS-1$

        monitorWrap.setTaskName(Messages.getString("ProxyRepositoryFactory.synchronizeLibraries")); //$NON-NLS-1$
        monitorWrap.worked(1);

        try {
            CorePlugin.getDefault().getLibrariesService().syncLibraries();
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }

        service.executeProjectTasks(project, false, monitorWrap);

    }
}
