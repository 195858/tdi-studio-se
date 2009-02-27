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
package org.talend.designer.components.model;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.talend.commons.CommonsPlugin;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.utils.io.FilesUtils;
import org.talend.commons.utils.time.TimeMeasure;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.language.LanguageManager;
import org.talend.core.model.components.AbstractComponentsProvider;
import org.talend.core.model.components.IComponent;
import org.talend.core.model.components.IComponentsFactory;
import org.talend.core.ui.branding.IBrandingService;
import org.talend.designer.components.i18n.Messages;
import org.talend.designer.core.model.components.EmfComponent;
import org.talend.designer.core.model.process.AbstractProcessProvider;

/**
 * Component factory that look for each component and load their information. <br/>
 * 
 * $Id$
 */
public class ComponentsFactory implements IComponentsFactory {

    private static final String OLD_COMPONENTS_USER_INNER_FOLDER = "user"; //$NON-NLS-1$

    private static Logger log = Logger.getLogger(ComponentsFactory.class);

    private static List<IComponent> componentList = null;

    private static Map<String, IComponent> componentsCache = new HashMap<String, IComponent>();

    // 1. only the in the directory /components ,not including /resource
    // 2. include the skeleton files and external include files
    private static List<String> skeletonList = null;

    private static final String SKELETON_SUFFIX = ".skeleton"; //$NON-NLS-1$

    private static final String INCLUDEFILEINJET_SUFFIX = ".inc.javajet"; //$NON-NLS-1$

    public ComponentsFactory() {
        if (!INCLUDEFILEINJET_SUFFIX.equals(".inc.javajet")) { //$NON-NLS-1$
            ExceptionHandler.process(new IllegalStateException(Messages.getString("ComponentsFactory.parentNotRecompiled")), //$NON-NLS-1$
                    Priority.WARN);
        }
    }

    private void init() {
        removeOldComponentsUserFolder(); // not used anymore

        TimeMeasure.measureActive = false;
        TimeMeasure.begin("ComponentsFactory.init"); //$NON-NLS-1$
        long startTime = System.currentTimeMillis();
        componentList = new ArrayList<IComponent>();
        skeletonList = new ArrayList<String>();

        XsdValidationCacheManager.getInstance().load();

        // 1. Load system components:
        loadComponentsFromFolder(IComponentsFactory.COMPONENTS_INNER_FOLDER);
        TimeMeasure.step("ComponentsFactory.init", Messages.getString("ComponentsFactory.afterSystemComponent")); //$NON-NLS-1$ //$NON-NLS-2$

        // 3.Load Component from extension point: components_provider
        loadComponentsFromComponentsProviderExtension();

        // 3.Load Component from extension point: component_definition
        loadComponentsFromExtensions();

        XsdValidationCacheManager.getInstance().save();

        TimeMeasure.end("ComponentsFactory.init"); //$NON-NLS-1$
        log.debug(componentList.size() + " components loaded in " + (System.currentTimeMillis() - startTime) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$

        try {
            CorePlugin.getDefault().getRunProcessService().updateLibraries();
        } catch (CoreException e) {
            ExceptionHandler.process(e);
        }
        TimeMeasure.measureActive = false;
    }

    private void loadComponentsFromComponentsProviderExtension() {
        ComponentsProviderManager componentsProviderManager = ComponentsProviderManager.getInstance();
        for (AbstractComponentsProvider componentsProvider : componentsProviderManager.getProviders()) {
            try {
                componentsProvider.preComponentsLoad();
                if (componentsProvider.getInstallationFolder().exists()) {
                    loadComponentsFromFolder(componentsProvider.getComponentsLocation());
                }
            } catch (IOException e) {
                ExceptionHandler.process(e);
            }
        }
    }

    private void removeOldComponentsUserFolder() {
        String userPath = IComponentsFactory.COMPONENTS_INNER_FOLDER + File.separatorChar + OLD_COMPONENTS_USER_INNER_FOLDER;
        File componentsLocation = getComponentsLocation(userPath);
        if (componentsLocation != null && componentsLocation.exists()) {
            FilesUtils.removeFolder(componentsLocation, true);
        }
    }

    /**
     * DOC qzhang Comment method "loadComponentsFromExtensions".
     */
    private void loadComponentsFromExtensions() {
        AbstractProcessProvider.loadComponentsFromProviders();
    }

    private void loadComponentsFromFolder(String pathSource) {
        TimeMeasure.begin("ComponentsFactory.loadComponentsFromFolder"); //$NON-NLS-1$

        TimeMeasure.begin("ComponentsFactory.loadComponentsFromFolder.checkFiles"); //$NON-NLS-1$
        TimeMeasure.pause("ComponentsFactory.loadComponentsFromFolder.checkFiles"); //$NON-NLS-1$

        TimeMeasure.begin("ComponentsFactory.loadComponentsFromFolder.emf1"); //$NON-NLS-1$
        TimeMeasure.pause("ComponentsFactory.loadComponentsFromFolder.emf1"); //$NON-NLS-1$

        TimeMeasure.begin("ComponentsFactory.loadComponentsFromFolder.emf2"); //$NON-NLS-1$
        TimeMeasure.pause("ComponentsFactory.loadComponentsFromFolder.emf2"); //$NON-NLS-1$

        TimeMeasure.begin("ComponentsFactory.loadComponentsFromFolder.loadIcons"); //$NON-NLS-1$
        TimeMeasure.pause("ComponentsFactory.loadComponentsFromFolder.loadIcons"); //$NON-NLS-1$

        // TimeMeasure.display=false;

        File source = getComponentsLocation(pathSource);
        File[] childDirectories;

        FileFilter fileFilter = new FileFilter() {

            public boolean accept(final File file) {
                return file.isDirectory() && file.getName().charAt(0) != '.'
                        && !file.getName().equals(IComponentsFactory.EXTERNAL_COMPONENTS_INNER_FOLDER);
            }

        };
        if (source == null) {
            ExceptionHandler.process(new Exception(Messages.getString("ComponentsFactory.componentNotFound") + pathSource)); //$NON-NLS-1$
            return;
        }

        childDirectories = source.listFiles(fileFilter);

        IBrandingService service = (IBrandingService) GlobalServiceRegister.getDefault().getService(IBrandingService.class);

        String[] availableComponents = service.getBrandingConfiguration().getAvailableComponents();

        FileFilter skeletonFilter = new FileFilter() {

            public boolean accept(final File file) {
                String fileName = file.getName();
                return file.isFile() && fileName.charAt(0) != '.'
                        && (fileName.endsWith(SKELETON_SUFFIX) || fileName.endsWith(INCLUDEFILEINJET_SUFFIX));
            }

        };

        if (childDirectories != null) {
            for (File currentFolder : childDirectories) {

                // get the skeleton files first, then XML config files later.
                File[] skeletonFiles = currentFolder.listFiles(skeletonFilter);
                if (skeletonFiles != null) {
                    for (File file : skeletonFiles) {
                        skeletonList.add(file.getAbsolutePath()); // path
                    }
                }

                try {
                    TimeMeasure.resume("ComponentsFactory.loadComponentsFromFolder.checkFiles"); //$NON-NLS-1$
                    ComponentFileChecker.checkComponentFolder(currentFolder, getCodeLanguageSuffix());
                    TimeMeasure.pause("ComponentsFactory.loadComponentsFromFolder.checkFiles"); //$NON-NLS-1$
                    TimeMeasure.resume("ComponentsFactory.loadComponentsFromFolder.emf1"); //$NON-NLS-1$
                    File xmlMainFile = new File(currentFolder, ComponentFilesNaming.getInstance().getMainXMLFileName(
                            currentFolder.getName(), getCodeLanguageSuffix()));
                    TimeMeasure.pause("ComponentsFactory.loadComponentsFromFolder.emf1"); //$NON-NLS-1$
                    TimeMeasure.resume("ComponentsFactory.loadComponentsFromFolder.emf2"); //$NON-NLS-1$

                    if (CommonsPlugin.isHeadless() && componentsCache.containsKey(xmlMainFile.getAbsolutePath())) {
                        // In headless mode, we assume the components won't change and we will use a cache
                        componentList.add(componentsCache.get(xmlMainFile.getAbsolutePath()));
                        continue;
                    }

                    EmfComponent currentComp = new EmfComponent(xmlMainFile, pathSource);

                    if (availableComponents != null && !ArrayUtils.contains(availableComponents, currentComp.getName())) {
                        continue;
                    }

                    TimeMeasure.pause("ComponentsFactory.loadComponentsFromFolder.emf2"); //$NON-NLS-1$

                    if (componentList.contains(currentComp)) {
                        log.warn("Component " + currentComp.getName() + " already exists. Cannot load user version."); //$NON-NLS-1$ //$NON-NLS-2$
                    } else {
                        currentComp.setResourceBundle(getComponentResourceBundle(currentComp, pathSource));
                        TimeMeasure.resume("ComponentsFactory.loadComponentsFromFolder.loadIcons"); //$NON-NLS-1$
                        loadIcons(currentFolder, currentComp);
                        TimeMeasure.pause("ComponentsFactory.loadComponentsFromFolder.loadIcons"); //$NON-NLS-1$
                        componentList.add(currentComp);
                    }

                    if (CommonsPlugin.isHeadless()) {
                        componentsCache.put(xmlMainFile.getAbsolutePath(), currentComp);
                    }
                } catch (MissingMainXMLComponentFileException e) {
                    log.trace(currentFolder.getName() + " is not a " + getCodeLanguageSuffix() + " component", e); //$NON-NLS-1$ //$NON-NLS-2$
                } catch (BusinessException e) {
                    BusinessException ex = new BusinessException("Cannot load component \"" + currentFolder.getName() + "\": " //$NON-NLS-1$ //$NON-NLS-2$
                            + e.getMessage(), e);
                    ExceptionHandler.process(ex, Level.WARN);
                }
            }
        }
        // TimeMeasure.display=true;
        TimeMeasure.end("ComponentsFactory.loadComponentsFromFolder.checkFiles"); //$NON-NLS-1$
        TimeMeasure.end("ComponentsFactory.loadComponentsFromFolder.emf1"); //$NON-NLS-1$
        TimeMeasure.end("ComponentsFactory.loadComponentsFromFolder.emf2"); //$NON-NLS-1$
        TimeMeasure.end("ComponentsFactory.loadComponentsFromFolder.loadIcons"); //$NON-NLS-1$
        TimeMeasure.end("ComponentsFactory.loadComponentsFromFolder"); //$NON-NLS-1$
    }

    /**
     * DOC smallet Comment method "checkComponentFolder".
     * 
     * @param currentFolder
     * @return
     * @throws BusinessException
     */

    private File getComponentsLocation(String folder) {
        Bundle b = Platform.getBundle(IComponentsFactory.COMPONENTS_LOCATION);

        File file = null;
        try {
            URL url = FileLocator.find(b, new Path(folder), null);
            if (url == null) {
                return null;
            }
            URL fileUrl = FileLocator.toFileURL(url);
            file = new File(fileUrl.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return file;
    }

    /**
     * 
     * 
     * Needs to create our own class loader in order to clear the cache for a ResourceBundle. Without using a new class
     * loader each time the values would not be reread from the .properties file
     * 
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4212439
     * 
     * yzhang ComponentsFactory class global comment. Detailled comment <br/>
     * 
     * $Id$
     * 
     */
    private static class ResClassLoader extends ClassLoader {

        ResClassLoader(ClassLoader parent) {
            super(parent);
        }
    }

    private ResourceBundle getComponentResourceBundle(IComponent currentComp, String source) {
        String label = ComponentFilesNaming.getInstance().getBundleName(currentComp.getName(), source);
        // String pluginFullName = currentComp.getPluginFullName();
        // System.out.println(pluginFullName);
        // Bundle bundle = Platform.getBundle(pluginFullName);
        // ClassLoader classLoader = bundle.getClass().getClassLoader();
        // return ResourceBundle.getBundle(label, Locale.getDefault(), classLoader);

        ResourceBundle bundle = ResourceBundle.getBundle(label, Locale.getDefault(), new ResClassLoader(getClass()
                .getClassLoader()));

        return bundle;
    }

    private String getCodeLanguageSuffix() {
        return LanguageManager.getCurrentLanguage().getName();
    }

    private void loadIcons(File folder, IComponent component) {
        ComponentIconLoading cil = new ComponentIconLoading(folder);

        component.setIcon32(cil.getImage32());
        component.setIcon24(cil.getImage24());
        component.setIcon16(cil.getImage16());
    }

    public int size() {
        if (componentList == null) {
            init();
        }
        return componentList.size();
    }

    public IComponent get(final String name) {
        IComponent comp = null;
        if (componentList == null) {
            init();
        }

        for (int i = 0; i < componentList.size(); i++) {
            comp = componentList.get(i);
            if (comp != null) {
                if (comp.getName().equals(name)) {
                    return comp;
                }
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.components.IComponentsFactory#getComponents()
     */
    public List<IComponent> getComponents() {
        if (componentList == null) {
            init();
        }
        return componentList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.components.IComponentsFactory#getComponentPath()
     */
    public URL getComponentPath() throws IOException {
        Bundle b = Platform.getBundle(IComponentsFactory.COMPONENTS_LOCATION);
        URL url = FileLocator.toFileURL(FileLocator.find(b, new Path(IComponentsFactory.COMPONENTS_INNER_FOLDER), null));
        return url;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.components.IComponentsFactory#getSkeletons()
     */
    public List<String> getSkeletons() {
        if (skeletonList == null) {
            init();
        }
        return skeletonList;
    }

    public void reset() {
        componentList = null;
        skeletonList = null;
    }
}
