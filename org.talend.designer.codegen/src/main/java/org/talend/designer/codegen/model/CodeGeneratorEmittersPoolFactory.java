// ============================================================================
//
// Copyright (C) 2006-2010 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.codegen.model;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.codegen.jet.JETEmitter;
import org.eclipse.emf.codegen.jet.JETException;
import org.eclipse.emf.common.CommonPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.swt.widgets.Display;
import org.talend.commons.CommonsPlugin;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.utils.StringUtils;
import org.talend.commons.utils.io.IOUtils;
import org.talend.core.CorePlugin;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.components.ComponentCompilations;
import org.talend.core.model.components.ComponentUtilities;
import org.talend.core.model.components.IComponent;
import org.talend.core.model.components.IComponentFileNaming;
import org.talend.core.model.components.IComponentsFactory;
import org.talend.core.model.temp.ECodePart;
import org.talend.core.utils.AccessingEmfJob;
import org.talend.designer.codegen.CodeGeneratorActivator;
import org.talend.designer.codegen.config.CodeGeneratorProgressMonitor;
import org.talend.designer.codegen.config.EInternalTemplate;
import org.talend.designer.codegen.config.JetBean;
import org.talend.designer.codegen.config.LightJetBean;
import org.talend.designer.codegen.config.TalendJetEmitter;
import org.talend.designer.codegen.config.TemplateUtil;
import org.talend.designer.codegen.i18n.Messages;
import org.talend.repository.model.ComponentsFactoryProvider;
import org.talend.repository.model.ExternalNodesFactory;

/**
 * Pool of initialized Jet Emitters. There are as many Emitters in this pool as Templzte available. Used for generation
 * performance constraint.
 * 
 * $Id$
 * 
 */
public final class CodeGeneratorEmittersPoolFactory {

    private static HashMap<JetBean, JETEmitter> emitterPool = null;

    private static boolean initialized = false;

    private static boolean initializeStart = false;

    private static Logger log = Logger.getLogger(CodeGeneratorEmittersPoolFactory.class);

    private static List<JetBean> jetFilesCompileFail = new ArrayList<JetBean>();

    private static String defaultTemplate = null;

    /**
     * Default Constructor. Must not be used.
     */
    private CodeGeneratorEmittersPoolFactory() {
    }

    private static JobRunnable jobRunnable = null;

    private static IStatus status = null;

    private static DelegateProgressMonitor delegateMonitor = new DelegateProgressMonitor();

    /***/
    private static class JobRunnable extends Thread {

        public JobRunnable(String name) {
            super(name);
            initializeStart = true;
        }

        public void run() {
            status = doRun();
        }

        public IStatus doRun() {
            try {

                ComponentsFactoryProvider.saveComponentVisibilityStatus();

                jetFilesCompileFail.clear();

                IProgressMonitor monitorWrap = null;
                if (!CommonsPlugin.isHeadless()) {
                    monitorWrap = new CodeGeneratorProgressMonitor(delegateMonitor);
                } else {
                    monitorWrap = new NullProgressMonitor();
                }

                CodeGeneratorInternalTemplatesFactory templatesFactory = CodeGeneratorInternalTemplatesFactoryProvider
                        .getInstance();
                templatesFactory.init();

                IComponentsFactory componentsFactory = ComponentsFactoryProvider.getInstance();
                // do not call init because it may be already loaded by
                // ComponentsFactoryProvider.saveComponentVisibilityStatus
                componentsFactory.getComponents();

                long startTime = System.currentTimeMillis();
                RepositoryContext repositoryContext = (RepositoryContext) CorePlugin.getContext().getProperty(
                        Context.REPOSITORY_CONTEXT_KEY);
                ECodeLanguage codeLanguage = repositoryContext.getProject().getLanguage();

                defaultTemplate = TemplateUtil.RESOURCES_DIRECTORY + TemplateUtil.DIR_SEP + EInternalTemplate.DEFAULT_TEMPLATE
                        + TemplateUtil.EXT_SEP + codeLanguage.getExtension() + TemplateUtil.TEMPLATE_EXT;

                List<JetBean> jetBeans = new ArrayList<JetBean>();

                List<TemplateUtil> templates = templatesFactory.getTemplates();
                List<IComponent> components = componentsFactory.getComponents();

                monitorWrap.beginTask(Messages.getString("CodeGeneratorEmittersPoolFactory.initMessage"), //$NON-NLS-1$
                        (2 * templates.size() + 5 * components.size()));

                int monitorBuffer = 0;
                for (TemplateUtil template : templates) {
                    JetBean jetBean = initializeUtilTemplate(template, codeLanguage);
                    jetBeans.add(jetBean);
                    monitorBuffer++;
                    if (monitorBuffer % 100 == 0) {
                        monitorWrap.worked(100);
                        monitorBuffer = 0;
                    }
                }

                if (components != null) {
                    ECodePart codePart = ECodePart.MAIN;
                    for (int i = 0; i < components.size(); i++) {
                        IComponent component = components.get(i);
                        // if (component.isTechnical() || component.isVisible()) {
                        if (component.getAvailableCodeParts().size() > 0) {
                            initComponent(codeLanguage, jetBeans, codePart, component);
                        }
                        // }
                        monitorBuffer++;
                        if (monitorBuffer % 100 == 0) {
                            monitorWrap.worked(100);
                            monitorBuffer = 0;
                        }
                    }
                }
                monitorWrap.worked(monitorBuffer);

                initializeEmittersPool(jetBeans, codeLanguage, monitorWrap);
                monitorWrap.done();

                if (!CommonsPlugin.isHeadless()) {
                    Display.getDefault().asyncExec(new Runnable() {

                        public void run() {
                            CorePlugin.getDefault().getDesignerCoreService().synchronizeDesignerUI(
                                    new PropertyChangeEvent(this, ComponentUtilities.NORMAL, null, null));
                        }

                    });
                }
                log.debug(Messages.getString(
                        "CodeGeneratorEmittersPoolFactory.componentCompiled", (System.currentTimeMillis() - startTime))); //$NON-NLS-1$
                initialized = true;

                // remove compilations markers
                ComponentCompilations.deleteMarkers();
                initializeStart = false;

            } catch (Exception e) {
                log.error(Messages.getString("CodeGeneratorEmittersPoolFactory.initialException"), e); //$NON-NLS-1$
                return new Status(IStatus.ERROR, CodeGeneratorActivator.PLUGIN_ID, Messages
                        .getString("CodeGeneratorEmittersPoolFactory.initialException"), e); //$NON-NLS-1$
            } finally {
                try {
                    IWorkspace workspace = ResourcesPlugin.getWorkspace();
                    IProject project = workspace.getRoot().getProject(".JETEmitters"); //$NON-NLS-1$
                    project.build(IncrementalProjectBuilder.AUTO_BUILD, null);
                } catch (CoreException e) {
                    ExceptionHandler.process(e);
                } finally {
                    try {
                        ResourcesPlugin.getWorkspace().save(true, new NullProgressMonitor());
                    } catch (CoreException e) {
                        ExceptionHandler.process(e);
                    }
                }
            }
            if (jetFilesCompileFail.size() > 0) {
                StringBuilder message = new StringBuilder();
                for (JetBean tmpJetBean : jetFilesCompileFail) {
                    if (message.length() > 0) {
                        message.append(",\r\n").append(tmpJetBean.getTemplateRelativeUri()); //$NON-NLS-1$
                    } else {
                        message.append(tmpJetBean.getTemplateRelativeUri());
                    }
                }
                return new Status(IStatus.ERROR, CodeGeneratorActivator.PLUGIN_ID, Messages
                        .getString("CodeGeneratorEmittersPoolFactory.failCompail") //$NON-NLS-1$
                        + message.toString());
            }
            CorePlugin.getDefault().getRcpService().activeSwitchProjectAction();
            return Status.OK_STATUS;
        }

    };

    public static Job initialize() {
        Job job = new AccessingEmfJob(Messages.getString("CodeGeneratorEmittersPoolFactory.initMessage")) { //$NON-NLS-1$

            @Override
            protected IStatus doRun(IProgressMonitor monitor) {
                synchronized (delegateMonitor) {
                    if (jobRunnable == null) {
                        jobRunnable = new JobRunnable(Messages.getString("CodeGeneratorEmittersPoolFactory.codeThread")); //$NON-NLS-1$
                        jobRunnable.start();
                    }
                }

                delegateMonitor.addDelegate(monitor);

                while (jobRunnable != null && jobRunnable.isAlive()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                }

                delegateMonitor.clearDelegate();

                synchronized (delegateMonitor) {
                    jobRunnable = null;
                }

                return status;
            }

        };
        job.setUser(true);
        job.setPriority(Job.INTERACTIVE);
        job.schedule();
        job.wakeUp(); // start as soon as possible

        return job;
    }

    /**
     * initialization of available templates.
     * 
     * @param template
     * @param codeLanguage
     * @return
     */
    private static JetBean initializeUtilTemplate(TemplateUtil template, ECodeLanguage codeLanguage) {
        JetBean jetBean = new JetBean(CodeGeneratorActivator.PLUGIN_ID, TemplateUtil.RESOURCES_DIRECTORY + TemplateUtil.DIR_SEP
                + template.getResourceName() + TemplateUtil.EXT_SEP + codeLanguage.getExtension() + TemplateUtil.TEMPLATE_EXT,
                template.getResourceName(), template.getVersion(), codeLanguage.getName(), ""); //$NON-NLS-1$
        jetBean.addClassPath("CORE_LIBRARIES", CorePlugin.PLUGIN_ID); //$NON-NLS-1$
        jetBean.addClassPath("CODEGEN_LIBRARIES", CodeGeneratorActivator.PLUGIN_ID); //$NON-NLS-1$
        jetBean.addClassPath("COMMON_LIBRARIES", CommonsPlugin.PLUGIN_ID); //$NON-NLS-1$
        jetBean.setClassLoader(new CodeGeneratorEmittersPoolFactory().getClass().getClassLoader());
        return jetBean;
    }

    /**
     * initialization of the available components.
     * 
     * @param codeLanguage
     * @param jetBeans
     * @param codePart
     * @param component
     */
    private static void initComponent(ECodeLanguage codeLanguage, List<JetBean> jetBeans, ECodePart codePart, IComponent component) {

        if (component.getAvailableCodeParts().contains(codePart)) {
            IComponentFileNaming fileNamingInstance = ComponentsFactoryProvider.getFileNamingInstance();
            String templateURI = component.getPathSource() + TemplateUtil.DIR_SEP + component.getName() + TemplateUtil.DIR_SEP
                    + fileNamingInstance.getJetFileName(component, codeLanguage.getExtension(), codePart);

            JetBean jetBean = new JetBean(IComponentsFactory.COMPONENTS_LOCATION, templateURI, component.getName(), component
                    .getVersion(), codeLanguage.getName(), codePart.getName());
            jetBean.addClassPath("CORE_LIBRARIES", CorePlugin.PLUGIN_ID); //$NON-NLS-1$
            jetBean.addClassPath("CODEGEN_LIBRARIES", CodeGeneratorActivator.PLUGIN_ID); //$NON-NLS-1$
            jetBean.addClassPath("COMMON_LIBRARIES", CommonsPlugin.PLUGIN_ID); //$NON-NLS-1$

            for (String pluginDependency : component.getPluginDependencies()) {
                jetBean.addClassPath(pluginDependency.toUpperCase().replaceAll("\\.", "_") + "_LIBRARIES", pluginDependency); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

            String familyName = component.getOriginalFamilyName();
            if (familyName.contains("|")) { //$NON-NLS-1$
                familyName = component.getOriginalFamilyName().substring(0, component.getOriginalFamilyName().indexOf("|")); //$NON-NLS-1$
            }
            jetBean.setFamily(StringUtils.removeSpecialCharsForPackage(familyName.toLowerCase()));

            if (component.getPluginFullName().compareTo(IComponentsFactory.COMPONENTS_LOCATION) != 0) {
                jetBean.addClassPath("EXTERNAL_COMPONENT_" + component.getPluginFullName().toUpperCase().replaceAll("\\.", "_"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        component.getPluginFullName());
                jetBean.setClassLoader(ExternalNodesFactory.getInstance(component.getPluginFullName()).getClass()
                        .getClassLoader());
            } else {
                jetBean.setClassLoader(new CodeGeneratorEmittersPoolFactory().getClass().getClassLoader());
            }
            jetBeans.add(jetBean);
        }
        if (codePart.compareTo(ECodePart.MAIN) == 0) {
            if (component.getAvailableCodeParts().contains(ECodePart.BEGIN)) {
                initComponent(codeLanguage, jetBeans, ECodePart.BEGIN, component);
            }
            if (component.getAvailableCodeParts().contains(ECodePart.END)) {
                initComponent(codeLanguage, jetBeans, ECodePart.END, component);
            }
        }

    }

    /**
     * real pool initialization.
     * 
     * @param monitorWrap
     * 
     * @return
     * @throws JETException
     */
    private static void initializeEmittersPool(List<JetBean> components, ECodeLanguage codeLanguage, IProgressMonitor monitorWrap) {
        IProgressMonitor monitor = new NullProgressMonitor();
        IProgressMonitor sub = new SubProgressMonitor(monitor, 1);
        int monitorBuffer = 0;

        HashMap<String, String> globalClasspath = new HashMap<String, String>();
        for (JetBean jetBean : components) {
            globalClasspath.putAll(jetBean.getClassPath());
            // compute the CRC
            jetBean.setCrc(extractTemplateHashCode(jetBean));
        }

        emitterPool = new HashMap<JetBean, JETEmitter>();
        List<JetBean> alreadyCompiledEmitters = new ArrayList<JetBean>();

        // try {
        TalendJetEmitter dummyEmitter = null;
        try {
            dummyEmitter = new TalendJetEmitter(null, null, sub, globalClasspath, !ComponentCompilations.getMarkers());
        } catch (JETException e) {
            log.error(Messages.getString("CodeGeneratorEmittersPoolFactory.jetEmitterInitialException") + e.getMessage(), e); //$NON-NLS-1$
        }

        boolean isSkeletonChanged = JetSkeletonManager.updateSkeletonPersistenceData();
        // if there is one skeleton changed, there need generate all jet--->java again. so, it won't load the
        // JetPersistenceJAVA
        if (!isSkeletonChanged) {
            try {
                alreadyCompiledEmitters = loadEmfPersistentData(EmfEmittersPersistenceFactory.getInstance(codeLanguage)
                        .loadEmittersPool(), components, monitorWrap);
                for (JetBean jetBean : alreadyCompiledEmitters) {
                    TalendJetEmitter emitter = new TalendJetEmitter(getFullTemplatePath(jetBean), jetBean.getClassLoader(),
                            jetBean.getFamily(), jetBean.getClassName(), jetBean.getLanguage(), jetBean.getCodePart(),
                            dummyEmitter.getTalendEclipseHelper());
                    emitter.setMethod(jetBean.getMethod());
                    emitterPool.put(jetBean, emitter);
                    monitorBuffer++;
                    if (monitorBuffer % 100 == 0) {
                        monitorWrap.worked(100);
                        monitorBuffer = 0;
                    }
                }
            } catch (BusinessException e) {
                // error already loggued
                emitterPool = new HashMap<JetBean, JETEmitter>();
            }
        }

        // for (JetBean jetBean : components) {
        // if (!emitterPool.containsKey(jetBean)) {
        // // System.out.println("The new file is not in JetPersistence* cache:" +
        // // jetBean.getTemplateFullUri());
        // TalendJetEmitter emitter = new TalendJetEmitter(jetBean.getTemplateFullUri(), jetBean.getClassLoader(),
        // jetBean
        // .getFamily(), jetBean.getClassName(), jetBean.getLanguage(), jetBean.getCodePart(), dummyEmitter
        // .getTalendEclipseHelper());
        // emitter.initialize(sub);
        //
        // if (emitter.getMethod() != null) {
        // jetBean.setMethod(emitter.getMethod());
        // jetBean.setClassName(emitter.getMethod().getDeclaringClass().getName());
        // alreadyCompiledEmitters.add(jetBean);
        // } else {
        // jetFilesCompileFail.add(jetBean);
        // }
        // emitterPool.put(jetBean, emitter);
        // monitorBuffer++;
        // if (monitorBuffer % 100 == 0) {
        // monitorWrap.worked(100);
        // monitorBuffer = 0;
        // }
        // }
        // }
        synchronizedComponent(components, sub, alreadyCompiledEmitters, dummyEmitter, monitorBuffer, monitorWrap);

        monitorWrap.worked(monitorBuffer);
        // } catch (JETException e) {
        //            log.error(Messages.getString("CodeGeneratorEmittersPoolFactory.jetEmitterInitialException") + e.getMessage(), e); //$NON-NLS-1$
        // }
        try {
            EmfEmittersPersistenceFactory.getInstance(codeLanguage).saveEmittersPool(
                    extractEmfPersistenData(alreadyCompiledEmitters));
        } catch (BusinessException e) {
            log.error(Messages.getString("CodeGeneratorEmittersPoolFactory.PersitentData.Error") + e.getMessage(), e); //$NON-NLS-1$
        }
    }

    private static String getFullTemplatePath(JetBean jetBean) {
        return Platform.getPlugin(jetBean.getJetPluginRepository()).getDescriptor().getInstallURL().toString()
                + jetBean.getTemplateRelativeUri();
    }

    private static void synchronizedComponent(List<JetBean> components, IProgressMonitor sub,
            List<JetBean> alreadyCompiledEmitters, TalendJetEmitter dummyEmitter, int monitorBuffer, IProgressMonitor monitorWrap) {
        for (JetBean jetBean : components) {
            if (!emitterPool.containsKey(jetBean)) {
                // System.out.println("The new file is not in JetPersistence* cache:" + getFullTemplatePath(jetBean));
                TalendJetEmitter emitter = new TalendJetEmitter(getFullTemplatePath(jetBean), jetBean.getClassLoader(), jetBean
                        .getFamily(), jetBean.getClassName(), jetBean.getLanguage(), jetBean.getCodePart(), dummyEmitter
                        .getTalendEclipseHelper());
                // 10901: Component synchronization fails
                try {
                    emitter.initialize(sub);
                } catch (JETException e) {
                    log
                            .error(
                                    Messages.getString("CodeGeneratorEmittersPoolFactory.jetEmitterInitialException") + e.getMessage(), e); //$NON-NLS-1$
                    continue;
                }

                if (emitter.getMethod() != null) {
                    jetBean.setMethod(emitter.getMethod());
                    jetBean.setClassName(emitter.getMethod().getDeclaringClass().getName());
                    alreadyCompiledEmitters.add(jetBean);
                } else {
                    jetFilesCompileFail.add(jetBean);
                }
                emitterPool.put(jetBean, emitter);
                monitorBuffer++;
                if (monitorBuffer % 100 == 0) {
                    monitorWrap.worked(100);
                    monitorBuffer = 0;
                }
            }
        }
    }

    /**
     * DOC mhirt Comment method "extractEmfPersistenData".
     * 
     * @param alreadyCompiledEmitters
     * @return
     */
    private static List<LightJetBean> extractEmfPersistenData(List<JetBean> alreadyCompiledEmitters) {
        List<LightJetBean> toReturn = new ArrayList<LightJetBean>();
        for (JetBean unit : alreadyCompiledEmitters) {
            // long unitCRC = extractTemplateHashCode(unit);
            long unitCRC = unit.getCrc();
            toReturn.add(new LightJetBean(unit.getTemplateRelativeUri(), unit.getClassName(), unit.getMethod().getName(), unit
                    .getVersion(), unit.getLanguage(), unitCRC));
        }
        return toReturn;
    }

    /**
     * DOC mhirt Comment method "extractTemplateHashCode".
     * 
     * @param unit
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    private static long extractTemplateHashCode(JetBean unit) {
        long unitCRC = 0;

        URI uri = URI.createURI(unit.getTemplateFullUri());
        uri = CommonPlugin.resolve(uri);
        URL url;
        try {
            url = new URL(uri.toString());
            unitCRC = IOUtils.computeCRC(url.openStream());
        } catch (Exception e) {
            // ignore me even if i'm null
        }

        return unitCRC;
    }

    private static List<JetBean> loadEmfPersistentData(List<LightJetBean> datas, List<JetBean> completeJetBeanList,
            IProgressMonitor monitorWrap) throws BusinessException {
        List<JetBean> toReturn = new ArrayList<JetBean>();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject project = workspace.getRoot().getProject(".JETEmitters"); //$NON-NLS-1$
        URL url;
        try {
            url = new File(project.getLocation() + "/runtime").toURL(); //$NON-NLS-1$
            int lightBeanIndex = 0;
            LightJetBean lightBean = null;
            LightJetBean myLightJetBean = null;
            String unitTemplateFullURI = ""; //$NON-NLS-1$
            long unitTemplateHashCode = 0;

            HashMap<String, LightJetBean> mapOnName = new HashMap<String, LightJetBean>();
            boolean forceMethodLoad = ComponentCompilations.getMarkers();
            if (forceMethodLoad) {
                // init specific map based on component name : mapOnName
                for (LightJetBean ljb : datas) {
                    mapOnName.put(ljb.getTemplateRelativeUri().substring(ljb.getTemplateRelativeUri().lastIndexOf("/")), ljb); //$NON-NLS-1$
                }
            }
            int monitorBuffer = 0;
            for (JetBean unit : completeJetBeanList) {
                monitorBuffer++;
                if (monitorBuffer % 200 == 0) {
                    monitorWrap.worked(200);
                    monitorBuffer = 0;
                }
                unitTemplateFullURI = unit.getTemplateRelativeUri();
                unitTemplateHashCode = unit.getCrc();

                myLightJetBean = new LightJetBean(unitTemplateFullURI, unit.getVersion(), unitTemplateHashCode);
                if (((lightBeanIndex = datas.indexOf(myLightJetBean)) > -1) || forceMethodLoad) {
                    if (!forceMethodLoad) {
                        lightBean = datas.get(lightBeanIndex);
                    } else {
                        lightBean = mapOnName.get(myLightJetBean.getTemplateRelativeUri().substring(
                                myLightJetBean.getTemplateRelativeUri().lastIndexOf("/"))); //$NON-NLS-1$
                    }
                    if (lightBean != null) {
                        unit.setClassName(lightBean.getClassName());
                        try {
                            Method method = loadMethod(url, lightBean.getMethodName(), unit);
                            if (method != null) {
                                unit.setMethod(method);
                                toReturn.add(unit);
                            }
                        } catch (ClassNotFoundException e) {
                            log.info(Messages.getString("CodeGeneratorEmittersPoolFactory.Class.NotFound", unit.getClassName())); //$NON-NLS-1$
                        }
                    }
                }
            }
            monitorWrap.worked(monitorBuffer);
        } catch (MalformedURLException e) {
            log.error(Messages.getString("CodeGeneratorEmittersPoolFactory.JETEmitters.NoPresent")); //$NON-NLS-1$
            throw new BusinessException(e);
        }
        return toReturn;
    }

    /**
     * DOC mhirt Comment method "loadMethod".
     * 
     * @param methodName
     * @return
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     */
    private static Method loadMethod(URL url, String methodName, JetBean unit) throws ClassNotFoundException {
        if (currentClassLoader != unit.getClassLoader()) {
            currentClassLoader = unit.getClassLoader();
            theClassLoader = new URLClassLoader(new URL[] { url }, unit.getClassLoader());
        }
        Class theClass;
        try {
            theClass = theClassLoader.loadClass(unit.getClassName());
        } catch (Error e) {
            throw new ClassNotFoundException(e.getMessage(), e);
        }
        Method[] methods = theClass.getDeclaredMethods();
        for (int i = 0; i < methods.length; ++i) {
            if (methods[i].getName().equals(methodName)) {
                return methods[i];
            }
        }
        return null;
    }

    private static ClassLoader currentClassLoader = null;

    private static URLClassLoader theClassLoader = null;

    /**
     * Getter for emitterPool.
     * 
     * @return the emitterPool
     */
    public static HashMap<JetBean, JETEmitter> getEmitterPool() {
        if (!isInitialized() && !isInitializeStart()) {
            initialize();
        }
        return emitterPool;
    }

    /**
     * DOC xtan Comment method "getJETEmitter".
     * 
     * @param jetBean
     * @return
     */
    public static JETEmitter getJETEmitter(JetBean jetBean) {
        if (!isInitialized() && !isInitializeStart()) {
            initialize();
        }

        // only for components, not for /resources jet file, if it compile error, it will get the
        // default_template.javajet
        if (jetBean.getTemplateRelativeUri() != null && !jetBean.getTemplateRelativeUri().startsWith("resources")) { //$NON-NLS-1$
            if (jetFilesCompileFail.contains(jetBean)) {
                JetBean defaultJetBean = new JetBean();
                defaultJetBean.setTemplateRelativeUri(defaultTemplate);
                return emitterPool.get(defaultJetBean);
            }
        }
        return emitterPool.get(jetBean);
    }

    /**
     * Getter for initialized.
     * 
     * @return the initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Sets the initialized.
     * 
     * @param initialized the initialized to set
     */
    public static void setInitialized(boolean initialized) {
        CodeGeneratorEmittersPoolFactory.initialized = initialized;
    }

    /***/
    private static class DelegateProgressMonitor implements IProgressMonitor {

        private List<IProgressMonitor> delegates = new ArrayList<IProgressMonitor>();

        private boolean cancelled;

        public void addDelegate(IProgressMonitor progressMonitor) {
            delegates.add(progressMonitor);
        }

        public void clearDelegate() {
            delegates.clear();
        }

        public void beginTask(String name, int totalWork) {
            for (IProgressMonitor delegate : delegates) {
                delegate.beginTask(name, totalWork);
            }
        }

        public void done() {
            for (IProgressMonitor delegate : delegates) {
                delegate.done();
            }
        }

        public void internalWorked(double work) {
            for (IProgressMonitor delegate : delegates) {
                delegate.internalWorked(work);
            }
        }

        public boolean isCanceled() {
            return cancelled;
        }

        public void setCanceled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        public void setTaskName(String name) {
            for (IProgressMonitor delegate : delegates) {
                delegate.setTaskName(name);
            }
        }

        public void subTask(String name) {
            for (IProgressMonitor delegate : delegates) {
                delegate.subTask(name);
            }
        }

        public void worked(int work) {
            for (IProgressMonitor delegate : delegates) {
                delegate.worked(work);
            }
        }

    }

    public static boolean isInitializeStart() {
        return initializeStart;
    }
}
