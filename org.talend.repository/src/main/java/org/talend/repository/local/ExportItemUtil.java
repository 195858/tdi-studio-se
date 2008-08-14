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
package org.talend.repository.local;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.ExternalCrossReferencer;
import org.talend.commons.emf.EmfHelper;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.Project;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.PropertiesPackage;
import org.talend.core.model.properties.User;
import org.talend.core.model.properties.helper.ByteArrayResource;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.repository.ProjectManager;
import org.talend.repository.constants.FileConstants;
import org.talend.repository.documentation.IFileExporterFullPath;
import org.talend.repository.documentation.TarFileExporterFullPath;
import org.talend.repository.documentation.ZipFileExporterFullPath;
import org.talend.repository.model.ProxyRepositoryFactory;

/***/
public class ExportItemUtil {

    private static final String EXPORTUSER_TALEND_COM = "exportuser@talend.com";

    private ResourceSet resourceSet;

    private Resource projectResource;

    private Resource propertyResource;

    private Resource itemResource;

    private File projectFile;

    private File propertyFile;

    private File itemFile;

    private IPath projectPath;

    private IPath propertyPath;

    private IPath itemPath;

    private Project project;

    private Map<String, User> login2user = new HashMap<String, User>();

    private ProjectManager pManager = ProjectManager.getInstance();

    public ExportItemUtil() {
        project = pManager.getCurrentProject().getEmfProject();
    }

    public ExportItemUtil(Project project) {
        this.project = project;
    }

    public void exportItems(File destination, Collection<Item> items) throws Exception {
        IFileExporterFullPath exporter = null;
        File tmpDirectory = null;
        Map<File, IPath> toExport;

        if (destination.getName().endsWith(".tar")) {
            createFolder(destination.getParentFile());
            exporter = new TarFileExporterFullPath(destination.getAbsolutePath(), false);
        } else if (destination.getName().endsWith(".tar.gz")) {
            createFolder(destination.getParentFile());
            exporter = new TarFileExporterFullPath(destination.getAbsolutePath(), true);
        } else if (destination.getName().endsWith(".zip")) {
            createFolder(destination.getParentFile());
            exporter = new ZipFileExporterFullPath(destination.getAbsolutePath(), true);
        } else {
            createFolder(destination);
        }

        try {
            if (exporter != null) {
                tmpDirectory = createTmpDirectory();
            }

            try {
                if (exporter != null) {
                    toExport = exportItems(items, tmpDirectory, true);

                    // in case of .tar.gz we remove extension twice
                    IPath rootPath = new Path(destination.getName()).removeFileExtension().removeFileExtension();
                    for (File file : toExport.keySet()) {
                        IPath path = toExport.get(file);
                        exporter.write(file.getAbsolutePath(), rootPath.append(path).toString());
                    }
                } else {
                    toExport = exportItems(items, destination, true);
                }
            } catch (Exception e) {
                throw e;
            } finally {
                if (exporter != null) {
                    deleteTmpDirectory(tmpDirectory);
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (exporter != null) {
                try {
                    exporter.finished();
                } catch (Exception e) {
                    // ignore me
                }
            }
        }
    }

    public Collection<Item> getAllVersions(Collection<Item> items) throws PersistenceException {
        Collection<Item> itemsVersions = new ArrayList<Item>();
        for (Item item : items) {
            org.talend.core.model.general.Project itemProject = new org.talend.core.model.general.Project(pManager
                    .getProject(item));
            List<IRepositoryObject> allVersion = ProxyRepositoryFactory.getInstance().getAllVersion(itemProject,
                    item.getProperty().getId());
            for (IRepositoryObject repositoryObject : allVersion) {
                itemsVersions.add(repositoryObject.getProperty().getItem());
            }
        }
        return itemsVersions;
    }

    public Set<File> createLocalResources(File destinationDirectory, Item item) throws Exception {
        List<Item> items = new ArrayList<Item>();
        items.add(item);

        Map<File, IPath> exportItems = exportItems(items, destinationDirectory, false);

        return exportItems.keySet();
    }

    private Map<File, IPath> exportItems(Collection<Item> items, File destinationDirectory, boolean projectFolderStructure)
            throws Exception {
        Map<File, IPath> toExport = new HashMap<File, IPath>();

        try {
            init();
            for (Item item : items) {
                project = pManager.getProject(item);

                computeProjectFileAndPath(destinationDirectory);
                if (!toExport.containsKey(projectFile)) {
                    createProjectResource(items);
                    toExport.put(projectFile, projectPath);
                }
                if (ERepositoryObjectType.getItemType(item).isResourceItem()) {
                    Collection<EObject> copiedObjects = copyObjects(item);

                    Item copiedItem = (Item) EcoreUtil.getObjectByType(copiedObjects, PropertiesPackage.eINSTANCE.getItem());
                    fixItem(copiedItem);
                    computeItemFilesAndPaths(destinationDirectory, copiedItem, projectFolderStructure);
                    createItemResources(copiedItem, copiedObjects);
                    fixItemUserReferences(copiedItem);
                    fixItemLockState();
                    toExport.put(propertyFile, propertyPath);
                    toExport.put(itemFile, itemPath);
                }
            }

            dereferenceNotContainedObjects();
            saveResources();
        } catch (Exception e) {
            throw e;
        } finally {
            cleanResources();
        }

        return toExport;
    }

    private File createTmpDirectory() throws IOException {
        File tmpDirectory = null;
        int suffix = 0;
        while (tmpDirectory == null || tmpDirectory.exists()) {
            tmpDirectory = new File(SystemUtils.getJavaIoTmpDir(), "talendExportItems" + suffix);
            suffix++;
        }

        if (!tmpDirectory.mkdir()) {
            throw new IOException("cannot create " + tmpDirectory);
        }

        return tmpDirectory;
    }

    private void deleteTmpDirectory(File tmpDirectory) {
        if (tmpDirectory.isFile()) {
            tmpDirectory.delete();
        } else {
            for (File file : tmpDirectory.listFiles()) {
                deleteTmpDirectory(file);
            }
            tmpDirectory.delete();
        }
    }

    private void computeProjectFileAndPath(File destinationFile) {
        projectPath = getProjectPath();
        projectPath = projectPath.append(FileConstants.LOCAL_PROJECT_FILENAME);
        projectFile = new File(destinationFile, projectPath.toOSString());
    }

    private IPath getProjectPath() {
        return new Path(project.getLabel());
    }

    private void computeItemFilesAndPaths(File destinationFile, Item item, boolean projectFolderStructure) {
        IPath fileNamePath = getProjectPath();

        if (projectFolderStructure) {
            ERepositoryObjectType itemType = ERepositoryObjectType.getItemType(item);
            IPath typeFolderPath = new Path(ERepositoryObjectType.getFolderName(itemType));
            IPath itemDestinationPath = typeFolderPath.append(item.getProperty().getItem().getState().getPath());
            fileNamePath = fileNamePath.append(itemDestinationPath);
        }
        fileNamePath = fileNamePath.append(ResourceFilenameHelper.getExpectedFileName(item.getProperty().getLabel(), item
                .getProperty().getVersion()));
        propertyPath = fileNamePath.addFileExtension(FileConstants.PROPERTIES_EXTENSION);
        propertyFile = new File(destinationFile, propertyPath.toOSString());

        itemPath = fileNamePath.addFileExtension(FileConstants.ITEM_EXTENSION);
        itemFile = new File(destinationFile, itemPath.toOSString());
    }

    private void init() {
        resourceSet = new ResourceSetImpl();
    }

    private void createProjectResource(Collection<Item> items) {
        projectResource = createResource(projectFile, false);

        EObject projectCopy = EcoreUtil.copy(project);
        projectResource.getContents().add(projectCopy);

        Set<String> logins = new HashSet<String>();
        logins.add(EXPORTUSER_TALEND_COM);
        for (Item item : items) {
            User author = item.getProperty().getAuthor();
            if (author != null) {
                logins.add(author.getLogin());
            }
        }

        for (String login : logins) {
            User user = PropertiesFactory.eINSTANCE.createUser();
            user.setLogin(login);
            projectResource.getContents().add(user);
            login2user.put(login, user);
        }
    }

    private void createItemResources(Item item, Collection<EObject> copiedObjects) {
        propertyResource = createResource(propertyFile, false);
        moveObjectsToResource(propertyResource, copiedObjects, PropertiesPackage.eINSTANCE.getProperty());
        moveObjectsToResource(propertyResource, copiedObjects, PropertiesPackage.eINSTANCE.getItemState());
        moveObjectsToResource(propertyResource, copiedObjects, PropertiesPackage.eINSTANCE.getItem());

        boolean isFileItem = PropertiesPackage.eINSTANCE.getFileItem().isSuperTypeOf(item.eClass());
        itemResource = createResource(itemFile, isFileItem);
        moveObjectsToResource(itemResource, copiedObjects, null);
    }

    private void fixItem(Item item) {
        item.getProperty().setLabel(item.getProperty().getLabel().replace(' ', '_'));
    }

    private Resource createResource(File file, boolean byteArrayResource) {
        URI uri = URI.createFileURI(file.getAbsolutePath());
        if (byteArrayResource) {
            Resource resource = new ByteArrayResource(uri);
            resourceSet.getResources().add(resource);
            return resource;
        } else {
            return resourceSet.createResource(uri);
        }
    }

    private void saveResources() throws IOException, PersistenceException {
        for (Resource resource : resourceSet.getResources()) {
            EmfHelper.saveResource(resource);
        }
    }

    private void cleanResources() {
        for (Resource resource : resourceSet.getResources()) {
            resource.unload();
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<EObject> copyObjects(Item item) {
        List<EObject> objects = new ArrayList<EObject>();

        objects.add(item);
        EList references = item.eClass().getEAllReferences();
        for (Iterator iter = references.iterator(); iter.hasNext();) {
            EReference reference = (EReference) iter.next();
            if (!reference.isTransient()) {
                if (reference.isMany()) {
                    EList referencedEList = (EList) item.eGet(reference);
                    for (Iterator iterator = referencedEList.iterator(); iterator.hasNext();) {
                        EObject referenceEObject = (EObject) iterator.next();
                        if (referenceEObject != null) {
                            objects.add(referenceEObject);
                        }
                    }
                } else {
                    EObject referenceEObject = (EObject) item.eGet(reference);
                    if (referenceEObject != null) {
                        objects.add(referenceEObject);
                    }
                }
            }
        }

        return EcoreUtil.copyAll(objects);
    }

    private void moveObjectsToResource(Resource resource, Collection<EObject> objects, EClass type) {
        Collection<EObject> objectsToTransfer;
        if (type != null) {
            objectsToTransfer = EcoreUtil.getObjectsByType(objects, type);
        } else {
            objectsToTransfer = objects;
        }
        resource.getContents().addAll(objectsToTransfer);
        objects.removeAll(objectsToTransfer);
    }

    private void fixItemUserReferences(Item item) {
        Item newItem = (Item) EcoreUtil.getObjectByType(propertyResource.getContents(), PropertiesPackage.eINSTANCE.getItem());
        User author = item.getProperty().getAuthor();
        String login = EXPORTUSER_TALEND_COM;
        if (author != null) {
            login = author.getLogin();
        }
        newItem.getProperty().setAuthor(login2user.get(login));
    }

    private void fixItemLockState() {
        Item item = (Item) EcoreUtil.getObjectByType(propertyResource.getContents(), PropertiesPackage.eINSTANCE.getItem());
        item.getState().setLocker(null);
        item.getState().setLockDate(null);
        item.getState().setLocked(false);
    }

    @SuppressWarnings("unchecked")
    private void dereferenceNotContainedObjects() {
        Map<EObject, Collection<Setting>> externalObjects = ExternalCrossReferencer.find(resourceSet);

        for (EObject object : externalObjects.keySet()) {
            Collection<Setting> collection = externalObjects.get(object);
            for (Setting setting : collection) {
                if (setting.getEStructuralFeature().isMany()) {
                    EList referencedEList = (EList) setting.getEObject().eGet(setting.getEStructuralFeature());
                    referencedEList.clear();
                } else {
                    setting.getEObject().eSet(setting.getEStructuralFeature(), null);
                }
            }
        }
    }

    private void createFolder(File folder) throws IOException {
        folder.mkdirs();
        if (!folder.exists()) {
            throw new IOException("unable to create directory '" + folder + "'");
        }
    }
}
