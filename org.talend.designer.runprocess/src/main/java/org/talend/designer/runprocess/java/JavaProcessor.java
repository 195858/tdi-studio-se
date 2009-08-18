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
package org.talend.designer.runprocess.java;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaBreakpointListener;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.JavaFormattingStrategy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.formatter.FormattingContext;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.formatter.MultiPassContentFormatter;
import org.eclipse.swt.widgets.Display;
import org.talend.commons.CommonsPlugin;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.RuntimeExceptionHandler;
import org.talend.commons.exception.SystemException;
import org.talend.commons.utils.generation.JavaUtils;
import org.talend.commons.utils.io.FilesUtils;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.PluginChecker;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.general.Project;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.utils.JavaResourcesHelper;
import org.talend.core.prefs.ITalendCorePrefConstants;
import org.talend.core.ui.IRulesProviderService;
import org.talend.designer.codegen.ICodeGenerator;
import org.talend.designer.codegen.ICodeGeneratorService;
import org.talend.designer.core.ISyntaxCheckableEditor;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.process.Process;
import org.talend.designer.runprocess.IJavaProcessorStates;
import org.talend.designer.runprocess.JobInfo;
import org.talend.designer.runprocess.Processor;
import org.talend.designer.runprocess.ProcessorException;
import org.talend.designer.runprocess.ProcessorUtilities;
import org.talend.designer.runprocess.RunProcessPlugin;
import org.talend.designer.runprocess.i18n.Messages;
import org.talend.designer.runprocess.prefs.RunProcessPrefsConstants;
import org.talend.librariesmanager.model.ModulesNeededProvider;
import org.talend.repository.ProjectManager;

/**
 * Creat the package folder for the java file, and put the generated file to the correct folder.
 * 
 * The creation for the java package should follow the pattern below:
 * 
 * 1)The name for the first grade folder should keep same with the T.O.S project name. 2)The folder name within the
 * project should be the job name.
 * 
 * <br/>
 * 
 * $Id: JavaProcessor.java 2007-1-22 上�?�10:53:24 yzhang $
 * 
 */
public class JavaProcessor extends Processor implements IJavaBreakpointListener {

    /** The java project within the project. */
    private static IJavaProject javaProject;

    /** The compiled code path. */
    private IPath compiledCodePath;

    /** The compiled context file path. */
    private IPath compiledContextPath;

    /** Tells if filename is based on id or label of the process. */
    private final boolean filenameFromLabel;

    private String typeName;

    private IJavaProcessorStates states;

    private ISyntaxCheckableEditor checkableEditor;

    private static IProject rootProject;

    /**
     * Matchs placeholder in subprocess_header.javajet, it will be replaced by the size of method code.
     */
    private static final String SIZE_COMMENT = "?SIZE?"; //$NON-NLS-1$

    private static final String METHOD_END_COMMENT = "End of Function:"; //$NON-NLS-1$

    private static final String METHOD_START_COMMENT = "Start of Function:"; //$NON-NLS-1$

    /**
     * Set current status.
     * 
     * DOC yzhang Comment method "setStatus".
     * 
     * @param states
     */
    public void setStatus(IJavaProcessorStates states) {
        this.states = states;
    }

    /**
     * Constructs a new JavaProcessor.
     * 
     * @param process Process to be turned in Java code.
     * @param filenameFromLabel Tells if filename is based on id or label of the process.
     */
    public JavaProcessor(IProcess process, boolean filenameFromLabel) {
        super(process);
        this.process = process;
        this.filenameFromLabel = filenameFromLabel;
        setProcessorStates(STATES_RUNTIME);
    }

    /*
     * Initialization of the variable codePath and contextPath.
     * 
     * @see org.talend.designer.runprocess.IProcessor#initPaths(org.talend.core.model .process.IContext)
     */
    @Override
    public void initPaths(IContext context) throws ProcessorException {
        if (context.equals(this.context)) {
            return;
        }

        try {
            this.project = getProcessorProject();
            createInternalPackage();
        } catch (CoreException e1) {
            throw new ProcessorException(Messages.getString("JavaProcessor.notFoundedProjectException")); //$NON-NLS-1$
        }

        initCodePath(context);
        this.context = context;
    }

    public void initPath() throws ProcessorException {
        initCodePath(context);
    }

    public void initCodePath(IContext context) throws ProcessorException {
        // RepositoryContext repositoryContext = (RepositoryContext)
        // CorePlugin.getContext().getProperty(
        // Context.REPOSITORY_CONTEXT_KEY);
        // Project project = repositoryContext.getProject();

        String projectFolderName = JavaResourcesHelper.getProjectFolderName(getProcess().getProperty().getItem());

        String jobFolderName = JavaResourcesHelper.getJobFolderName(process.getLabel(), process.getVersion());
        String fileName = filenameFromLabel ? escapeFilename(process.getLabel()) : process.getId();

        try {
            IPackageFragment projectPackage = getProjectPackage(projectFolderName);
            IPackageFragment jobPackage = getProjectPackage(projectPackage, jobFolderName);
            IPackageFragment contextPackage = getProjectPackage(jobPackage, "contexts"); //$NON-NLS-1$

            this.codePath = jobPackage.getPath().append(fileName + JavaUtils.JAVA_EXTENSION);
            this.codePath = this.codePath.removeFirstSegments(1);
            this.compiledCodePath = this.codePath.removeLastSegments(1).append(fileName);
            this.compiledCodePath = new Path(JavaUtils.JAVA_CLASSES_DIRECTORY).append(this.compiledCodePath
                    .removeFirstSegments(1));

            this.typeName = jobPackage.getPath().append(fileName).removeFirstSegments(2).toString().replace('/', '.');

            this.contextPath = contextPackage.getPath().append(
                    escapeFilename(context.getName()) + JavaUtils.JAVA_CONTEXT_EXTENSION);
            this.contextPath = this.contextPath.removeFirstSegments(1);
            this.compiledContextPath = this.contextPath.removeLastSegments(1).append(fileName);
            this.compiledContextPath = new Path(JavaUtils.JAVA_CLASSES_DIRECTORY).append(this.compiledContextPath
                    .removeFirstSegments(1));

        } catch (CoreException e) {
            throw new ProcessorException(Messages.getString("JavaProcessor.notFoundedFolderException")); //$NON-NLS-1$
        }
    }

    /**
     * DOC chuang Comment method "computeMethodSizeIfNeeded".
     * 
     * @param processCode
     * @return
     */
    private String computeMethodSizeIfNeeded(String processCode) {
        // must match TalendDesignerPrefConstants.DISPLAY_METHOD_SIZE
        boolean displayMethodSize = Boolean.parseBoolean(CorePlugin.getDefault().getDesignerCoreService().getPreferenceStore(
                "displayMethodSize")); //$NON-NLS-1$
        if (displayMethodSize) {
            StringBuffer code = new StringBuffer(processCode);
            int fromIndex = 0;
            while (fromIndex != -1 && fromIndex < code.length()) {
                int methodStartPos = code.indexOf(METHOD_START_COMMENT, fromIndex);
                if (methodStartPos < 0) {
                    break;
                }
                int sizeCommentPos = code.indexOf(SIZE_COMMENT, fromIndex);

                // move ahead to the start position of source code
                methodStartPos = code.indexOf("*/", sizeCommentPos) + 2; //$NON-NLS-1$

                int methodEndPos = code.indexOf(METHOD_END_COMMENT, fromIndex);
                if (methodEndPos < 0) {
                    break;
                }
                // start position for next search
                fromIndex = methodEndPos + METHOD_END_COMMENT.length();
                // go back to the end position of source code
                methodEndPos = code.lastIndexOf("/*", methodEndPos); //$NON-NLS-1$
                int size = methodEndPos - methodStartPos;
                code.replace(sizeCommentPos, sizeCommentPos + SIZE_COMMENT.length(), String.valueOf(size));

            }
            return code.toString();
        } else {
            return processCode;
        }
    }

    /*
     * Append the generated java code form context into java file wihtin the project. If the file not existed new one
     * will be created.
     * 
     * @see org.talend.designer.runprocess.IProcessor#generateCode(org.talend.core .model.process.IContext, boolean,
     * boolean, boolean)
     */
    @SuppressWarnings("restriction")
    @Override
    public void generateCode(boolean statistics, boolean trace, boolean javaProperties) throws ProcessorException {
        super.generateCode(statistics, trace, javaProperties);
        try {
            String currentJavaProject = null; // hywang modified for 6484
            IRulesProviderService rulesService = (IRulesProviderService) GlobalServiceRegister.getDefault().getService(
                    IRulesProviderService.class);
            ICodeGenerator codeGen;
            ICodeGeneratorService service = RunProcessPlugin.getDefault().getCodeGeneratorService();
            if (javaProperties) {
                String javaInterpreter = ""; //$NON-NLS-1$
                String javaLib = ""; //$NON-NLS-1$
                currentJavaProject = ProjectManager.getInstance().getProject(getProcess().getProperty().getItem())
                        .getTechnicalLabel();
                String javaContext = getContextPath().toOSString();

                codeGen = service.createCodeGenerator(process, statistics, trace, javaInterpreter, javaLib, javaContext,
                        currentJavaProject);
            } else {
                codeGen = service.createCodeGenerator(process, statistics, trace);
            }
            String processCode = ""; //$NON-NLS-1$
            try {
                processCode = codeGen.generateProcessCode();
                // hywang add for 6484
                boolean useGenerateRuleFiles = false;
                List<? extends INode> allNodes = this.process.getGeneratingNodes();
                for (int i = 0; i < allNodes.size(); i++) {
                    if (allNodes.get(i) instanceof INode) {
                        INode node = (INode) allNodes.get(i);
                        if (node.getComponent().getName().equals("tRules")) {
                            useGenerateRuleFiles = true;
                            break;
                        }
                    }
                }
                if (useGenerateRuleFiles) {
                    rulesService.generateFinalRuleFiles(currentJavaProject, this.process);
                }

                // replace drl template tages
                if (PluginChecker.isSnippetsPluginLoaded()) {
                    processCode = replaceSnippet(processCode);
                }
                if (PluginChecker.isSnippetsPluginLoaded()) {
                    processCode = replaceSnippet(processCode);
                }

            } catch (SystemException e) {
                throw new ProcessorException(Messages.getString("Processor.generationFailed"), e); //$NON-NLS-1$
            } catch (IOException e) {
                ExceptionHandler.process(e);
            }
            // Generating files
            IFile codeFile = this.project.getFile(this.codePath);

            // format the code before save the file.
            processCode = formatCode(processCode);
            // see feature 4610:option to see byte length of each code method
            processCode = computeMethodSizeIfNeeded(processCode);
            InputStream codeStream = new ByteArrayInputStream(processCode.getBytes());

            if (!codeFile.exists()) {
                // see bug 0003592, detele file with different case in windows
                deleteFileIfExisted(codeFile);

                codeFile.create(codeStream, true, null);
            } else {
                codeFile.setContents(codeStream, true, false, null);
            }

            processCode = null;

            // updateContextCode(codeGen);
            syntaxCheck();

            codeFile.getProject().deleteMarkers("org.eclipse.jdt.debug.javaLineBreakpointMarker", true, IResource.DEPTH_INFINITE); //$NON-NLS-1$

            List<INode> breakpointNodes = CorePlugin.getContext().getBreakpointNodes(process);
            if (!breakpointNodes.isEmpty()) {
                String[] nodeNames = new String[breakpointNodes.size()];
                int pos = 0;
                String nodeName;
                for (INode node : breakpointNodes) {
                    nodeName = node.getUniqueName();
                    if (node.getComponent().getMultipleComponentManagers().size() > 0) {
                        nodeName += "_" + node.getComponent().getMultipleComponentManagers().get(0).getInput().getName(); //$NON-NLS-1$
                    }
                    nodeNames[pos++] = "[" + nodeName + " main ] start"; //$NON-NLS-1$ //$NON-NLS-2$
                }
                int[] lineNumbers = getLineNumbers(codeFile, nodeNames);
                setBreakpoints(codeFile, getTypeName(), lineNumbers);
            }
        } catch (CoreException e1) {
            if (e1.getStatus() != null && e1.getStatus().getException() != null) {
                ExceptionHandler.process(e1.getStatus().getException());
            }
            throw new ProcessorException(Messages.getString("Processor.tempFailed"), e1); //$NON-NLS-1$
        }
    }

    /**
     * DOC nrousseau Comment method "formatCode".
     * 
     * @param processCode
     * @return
     */
    private String formatCode(String processCode) {
        IDocument document = new Document(processCode);

        // we cannot make calls to Ui in headless mode
        if (CommonsPlugin.isHeadless()) {
            return document.get();
        }

        JavaTextTools tools = JavaPlugin.getDefault().getJavaTextTools();
        tools.setupJavaDocumentPartitioner(document, IJavaPartitions.JAVA_PARTITIONING);

        IFormattingContext context = null;
        DocumentRewriteSession rewriteSession = null;

        IDocumentExtension4 extension = (IDocumentExtension4) document;
        DocumentRewriteSessionType type = DocumentRewriteSessionType.SEQUENTIAL;
        rewriteSession = extension.startRewriteSession(type);

        try {

            final String rememberedContents = document.get();

            try {
                final MultiPassContentFormatter formatter = new MultiPassContentFormatter(IJavaPartitions.JAVA_PARTITIONING,
                        IDocument.DEFAULT_CONTENT_TYPE);

                formatter.setMasterStrategy(new JavaFormattingStrategy());
                // formatter.setSlaveStrategy(new CommentFormattingStrategy(),
                // IJavaPartitions.JAVA_DOC);
                // formatter.setSlaveStrategy(new CommentFormattingStrategy(),
                // IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
                // formatter.setSlaveStrategy(new CommentFormattingStrategy(),
                // IJavaPartitions.JAVA_MULTI_LINE_COMMENT);

                context = new FormattingContext();
                context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.TRUE);
                Map map = new HashMap(JavaCore.getOptions());
                context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, map);
                formatter.format(document, context);
            } catch (RuntimeException x) {
                // fire wall for
                // https://bugs.eclipse.org/bugs/show_bug.cgi?id=47472
                // if something went wrong we undo the changes we just did
                // TODO to be removed after 3.0 M8
                document.set(rememberedContents);
                throw x;
            }

        } finally {
            extension.stopRewriteSession(rewriteSession);
            if (context != null) {
                context.dispose();
            }
        }
        return document.get();
    }

    @Override
    public void setSyntaxCheckableEditor(ISyntaxCheckableEditor checkableEditor) {
        this.checkableEditor = checkableEditor;
    }

    private void syntaxCheck() {
        if (checkableEditor != null) {
            checkableEditor.validateSyntax();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IProcessor#getCodeContext()
     */
    @Override
    public String getCodeContext() {
        return getCodeProject().getLocation().append(getContextPath()).removeLastSegments(1).toOSString();
    }

    private String escapeFilename(final String filename) {
        return filename != null ? filename.replace(" ", "") : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IProcessor#getCodePath()
     */
    @Override
    public IPath getCodePath() {
        return this.states.getCodePath();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IProcessor#getContextPath()
     */
    @Override
    public IPath getContextPath() {
        return this.states.getContextPath();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IProcessor#getCodeProject()
     */
    @Override
    public IProject getCodeProject() {
        return this.project.getProject();
    }

    /**
     * Find line numbers of the beginning of the code of process nodes.
     * 
     * @param file Code file where we are searching node's code.
     * @param nodes List of nodes searched.
     * @return Line numbers where code of nodes appears.
     * @throws CoreException Search failed.
     */
    private static int[] getLineNumbers(IFile file, String[] nodes) throws CoreException {
        List<Integer> lineNumbers = new ArrayList<Integer>();

        // List of code's lines searched in the file
        List<String> searchedLines = new ArrayList<String>();
        for (String node : nodes) {
            searchedLines.add(node);
        }

        LineNumberReader lineReader = new LineNumberReader(new InputStreamReader(file.getContents()));
        try {
            String line = lineReader.readLine();
            while (!searchedLines.isEmpty() && line != null) {
                boolean nodeFound = false;
                for (Iterator<String> i = searchedLines.iterator(); !nodeFound && i.hasNext();) {
                    String nodeMain = i.next();
                    if (line.indexOf(nodeMain) != -1) {
                        nodeFound = true;
                        i.remove();

                        // Search the first valid code line
                        boolean lineCodeFound = false;
                        line = lineReader.readLine();
                        while (line != null && !lineCodeFound) {
                            if (isCodeLine(line)) {
                                lineCodeFound = true;
                                lineNumbers.add(new Integer(lineReader.getLineNumber() + 1));
                            }
                            line = lineReader.readLine();
                        }
                    }
                }
                line = lineReader.readLine();
            }
        } catch (IOException ioe) {
            IStatus status = new Status(IStatus.ERROR, "", IStatus.OK, "Source code read failure.", ioe); //$NON-NLS-1$ //$NON-NLS-2$
            throw new CoreException(status);
        }

        int[] res = new int[lineNumbers.size()];
        int pos = 0;
        for (Integer i : lineNumbers) {
            res[pos++] = i.intValue();
        }
        return res;
    }

    /**
     * Return line number where stands specific node in code generated.
     * 
     * @param nodeName
     */
    @Override
    public int getLineNumber(String nodeName) {
        IFile codeFile = this.project.getProject().getFile(this.codePath);
        int[] lineNumbers = new int[] { 0 };
        try {
            lineNumbers = JavaProcessor.getLineNumbers(codeFile, new String[] { nodeName });
        } catch (CoreException e) {
            lineNumbers = new int[] { 0 };
            // e.printStackTrace();
            ExceptionHandler.process(e);
        }
        if (lineNumbers.length > 0) {
            return lineNumbers[0];
        } else {
            return 0;
        }
    }

    /**
     * Tells if a line is a line of perl code, not an empty or comment line.
     * 
     * @param line The tested line of code.
     * @return true if the line is a line of code.
     */
    private static boolean isCodeLine(String line) {
        String trimed = line.trim();
        return trimed.length() > 0 && trimed.charAt(0) != '#';
    }

    /**
     * Set java breakpoints in a java file.
     * 
     * @param srcFile Java file in wich breakpoints are added.
     * @param lineNumbers Line numbers in the source file where breakpoints are installed.
     * @throws CoreException Breakpoint addition failed.
     */
    private static void setBreakpoints(IFile codeFile, String typeName, int[] lines) throws CoreException {
        final String javaLineBrekPointMarker = "org.eclipse.jdt.debug.javaLineBreakpointMarker"; //$NON-NLS-1$
        codeFile.deleteMarkers(javaLineBrekPointMarker, true, IResource.DEPTH_ZERO);

        for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
            JDIDebugModel.createLineBreakpoint(codeFile, typeName, lines[lineNumber] + 1, -1, -1, 0, true, null);
        }
    }

    /**
     * A java project under folder .Java will be created if there is no existed.
     * 
     * DOC yzhang Comment method "getProject".
     * 
     * @return
     * @throws CoreException
     */
    public static IProject getProcessorProject() throws CoreException {
        if (rootProject != null) {
            return rootProject;
        }
        return initializeProject();

    }

    private static IProject initializeProject() throws CoreException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        rootProject = root.getProject(JavaUtils.JAVA_PROJECT_NAME);

        initJavaProject(rootProject);
        javaProject = JavaCore.create(rootProject);
        return rootProject;
    }

    public static void updateClasspath() throws CoreException {
        if (rootProject == null || javaProject == null) {
            initializeProject();
        }
        IClasspathEntry jreClasspathEntry = JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER")); //$NON-NLS-1$
        IClasspathEntry classpathEntry = JavaCore.newSourceEntry(javaProject.getPath().append(JavaUtils.JAVA_SRC_DIRECTORY));

        List<IClasspathEntry> classpath = new ArrayList<IClasspathEntry>();
        classpath.add(jreClasspathEntry);
        classpath.add(classpathEntry);

        Set<String> listModulesReallyNeeded = new HashSet<String>();
        for (ModuleNeeded moduleNeeded : ModulesNeededProvider.getModulesNeeded()) {
            listModulesReallyNeeded.add(moduleNeeded.getModuleName());
        }

        // see bug 0005559: Import cannot be resolved in routine after opening
        // Job Designer
        for (ModuleNeeded moduleNeeded : ModulesNeededProvider.getModulesNeededForRoutines()) {
            listModulesReallyNeeded.add(moduleNeeded.getModuleName());
        }

        File externalLibDirectory = new File(CorePlugin.getDefault().getLibrariesService().getLibrariesPath());
        if ((externalLibDirectory != null) && (externalLibDirectory.isDirectory())) {
            for (File externalLib : externalLibDirectory.listFiles(FilesUtils.getAcceptJARFilesFilter())) {
                if (externalLib.isFile() && listModulesReallyNeeded.contains(externalLib.getName())) {
                    classpath.add(JavaCore.newLibraryEntry(new Path(externalLib.getAbsolutePath()), null, null));
                }
            }
        }

        IClasspathEntry[] classpathEntryArray = classpath.toArray(new IClasspathEntry[classpath.size()]);

        javaProject.setRawClasspath(classpathEntryArray, null);

        javaProject.setOutputLocation(javaProject.getPath().append(JavaUtils.JAVA_CLASSES_DIRECTORY), null);
    }

    /**
     * DOC mhirt Comment method "initJavaProject".
     * 
     * @param prj
     * @throws CoreException
     */
    private static void initJavaProject(IProject prj) throws CoreException {
        // Does the java nature exists in the environment
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtension nature = registry.getExtension("org.eclipse.core.resources.natures", JavaCore.NATURE_ID); //$NON-NLS-1$

        if (!prj.exists()) {
            final IWorkspace workspace = ResourcesPlugin.getWorkspace();
            final IProjectDescription desc = workspace.newProjectDescription(prj.getName());
            if (nature != null) {
                desc.setNatureIds(new String[] { JavaCore.NATURE_ID });
            }
            prj.create(null);
            prj.open(IResource.BACKGROUND_REFRESH, null);
            prj.setDescription(desc, null);

            IFolder runtimeFolder = prj.getFolder(new Path(JavaUtils.JAVA_CLASSES_DIRECTORY));
            if (!runtimeFolder.exists()) {
                runtimeFolder.create(false, true, null);
            }

            IFolder sourceFolder = prj.getFolder(new Path(JavaUtils.JAVA_SRC_DIRECTORY));
            if (!sourceFolder.exists()) {
                sourceFolder.create(false, true, null);
            }
        } else {
            if (!prj.isOpen()) {
                prj.open(null);
            }
            if (prj.getNature(JavaCore.NATURE_ID) == null && nature != null) {
                IProjectDescription description = prj.getDescription();
                String[] natures = description.getNatureIds();
                String[] newNatures = new String[natures.length + 1];
                System.arraycopy(natures, 0, newNatures, 0, natures.length);
                newNatures[natures.length] = JavaCore.NATURE_ID;
                description.setNatureIds(newNatures);
                prj.open(IResource.BACKGROUND_REFRESH, null);
                prj.setDescription(description, null);
            }
        }
    }

    /**
     * Get the required project package under java project, if not existed new one will be created.
     * 
     * DOC yzhang Comment method "getProjectPackage".
     * 
     * @param packageName The required package name, should keep same with the T.O.S project name.
     * @return The required packaged.
     * @throws JavaModelException
     */
    private IPackageFragment getProjectPackage(String packageName) throws JavaModelException {

        IPackageFragmentRoot root = this.javaProject.getPackageFragmentRoot(this.javaProject.getProject().getFolder(
                JavaUtils.JAVA_SRC_DIRECTORY));
        IPackageFragment leave = root.getPackageFragment(packageName);
        if (!leave.exists()) {
            root.createPackageFragment(packageName, true, null);
        }

        return root.getPackageFragment(packageName);
    }

    public static void createInternalPackage() {

        IPackageFragmentRoot root = getJavaProject().getPackageFragmentRoot(
                getJavaProject().getProject().getFolder(JavaUtils.JAVA_SRC_DIRECTORY));
        IPackageFragment leave = root.getPackageFragment("internal"); //$NON-NLS-1$
        if (!leave.exists()) {
            try {
                root.createPackageFragment("internal", true, null); //$NON-NLS-1$
            } catch (JavaModelException e) {
                throw new RuntimeException(Messages.getString("JavaProcessor.notFoundedFolderException")); //$NON-NLS-1$
            }
        }
    }

    /**
     * Get the required job package under the project package within the tranfered project, if not existed new one will
     * be created.
     * 
     * DOC yzhang Comment method "getJobPackage".
     * 
     * @param projectPackage The project package within which the job package you need to get, can be getted by method
     * getProjectPackage().
     * @param jobName The required job package name.
     * @return The required job package.
     * @throws JavaModelException
     */
    private IPackageFragment getProjectPackage(IPackageFragment projectPackage, String jobName) throws JavaModelException {

        IPackageFragmentRoot root = this.javaProject.getPackageFragmentRoot(projectPackage.getResource());
        IPackageFragment leave = root.getPackageFragment(jobName);
        if (!leave.exists()) {
            root.createPackageFragment(jobName, true, null);
        }

        return root.getPackageFragment(jobName);

    }

    /*
     * Get the interpreter of Java.
     * 
     * @see org.talend.designer.runprocess.IProcessor#getInterpreter()
     */
    @Override
    public String getInterpreter() throws ProcessorException {
        // if the interpreter has been set to a specific one (not standard),
        // then this value won't be null
        String interpreter = super.getInterpreter();
        if (interpreter != null) {
            return interpreter;
        }
        return getDefaultInterpreter();

    }

    public static String getDefaultInterpreter() throws ProcessorException {
        IPreferenceStore prefStore = CorePlugin.getDefault().getPreferenceStore();
        String javaInterpreter = prefStore.getString(ITalendCorePrefConstants.JAVA_INTERPRETER);
        if (javaInterpreter == null || javaInterpreter.length() == 0) {
            throw new ProcessorException(Messages.getString("Processor.configureJava")); //$NON-NLS-1$
        }
        return javaInterpreter;
    }

    @Override
    public String getLibraryPath() throws ProcessorException {
        // if the library path has been set to a specific one (not standard),
        // then this value won't be null
        String libraryPath = super.getLibraryPath();
        if (libraryPath != null) {
            return libraryPath;
        }
        return CorePlugin.getDefault().getLibrariesService().getLibrariesPath();
    }

    @Override
    public String getCodeLocation() throws ProcessorException {
        // if the routine path has been set to a specific one (not standard),
        // then this value won't be null
        String codeLocation = super.getCodeLocation();
        if (codeLocation != null) {
            return codeLocation;
        }
        return this.getCodeProject().getLocation().toOSString();
    }

    /**
     * Getter for compliedCodePath.
     * 
     * @return the compliedCodePath
     */
    public IPath getCompiledCodePath() {
        return this.compiledCodePath;
    }

    /**
     * Getter for compiledContextPath.
     * 
     * @return the compiledContextPath
     */
    public IPath getCompiledContextPath() {
        return this.compiledContextPath;
    }

    /**
     * Getter for codePath.
     * 
     * @return the codePath
     */
    public IPath getSrcCodePath() {
        return this.codePath;
    }

    /**
     * Getter for srcContextPath.
     * 
     * @return the srcContextPath
     */
    public IPath getSrcContextPath() {
        return this.contextPath;
    }

    @Override
    public String[] getCommandLine() throws ProcessorException {
        // java -cp libdirectory/*.jar;project_path classname;

        // init java interpreter
        String command;
        try {
            command = getInterpreter();
        } catch (ProcessorException e1) {
            command = "java"; //$NON-NLS-1$
        }
        // zli
        boolean win32 = false;
        String classPathSeparator;
        if (targetPlatform == null) {
            targetPlatform = Platform.getOS();
            win32 = Platform.OS_WIN32.equals(targetPlatform);
            classPathSeparator = JavaUtils.JAVA_CLASSPATH_SEPARATOR;
        } else {
            win32 = targetPlatform.equals(Platform.OS_WIN32);
            if (win32) {
                classPathSeparator = ";"; //$NON-NLS-1$
            } else {
                classPathSeparator = ":"; //$NON-NLS-1$
            }
        }

        Set<String> neededLibraries = process.getNeededLibraries(true);

        if (neededLibraries == null) {
            neededLibraries = new HashSet<String>();
            for (ModuleNeeded moduleNeeded : ModulesNeededProvider.getModulesNeeded()) {
                neededLibraries.add(moduleNeeded.getModuleName());
            }

        } else { // this will avoid to add all libraries, only the needed
            // libraries will be added
            for (ModuleNeeded moduleNeeded : ModulesNeededProvider.getModulesNeededForRoutines()) {
                neededLibraries.add(moduleNeeded.getModuleName());
            }
        }

        boolean exportingJob = ProcessorUtilities.isExportConfig();
        String unixRootPathVar = "$ROOT_PATH";
        String unixRootPath = unixRootPathVar + "/";

        StringBuffer libPath = new StringBuffer();
        File externalLibDirectory = new File(CorePlugin.getDefault().getLibrariesService().getLibrariesPath());
        if ((externalLibDirectory != null) && (externalLibDirectory.isDirectory())) {
            for (File externalLib : externalLibDirectory.listFiles(FilesUtils.getAcceptJARFilesFilter())) {
                if (externalLib.isFile() && neededLibraries.contains(externalLib.getName())) {
                    if (!win32 && exportingJob) {
                        libPath.append(unixRootPath);
                    }
                    if (exportingJob) {
                        libPath.append(new Path(this.getLibraryPath()).append(externalLib.getName()) + classPathSeparator);
                    } else {
                        libPath.append(new Path(externalLib.getAbsolutePath()).toPortableString() + classPathSeparator);
                    }
                }
            }
        }

        // init project_path
        String projectPath;
        if (exportingJob) {
            projectPath = getCodeLocation();
            if (projectPath != null) {
                projectPath = projectPath.replace(ProcessorUtilities.TEMP_JAVA_CLASSPATH_SEPARATOR, classPathSeparator);
            }
        } else {
            IFolder classesFolder = javaProject.getProject().getFolder(JavaUtils.JAVA_CLASSES_DIRECTORY);
            IPath projectFolderPath = classesFolder.getFullPath().removeFirstSegments(1);
            projectPath = Path.fromOSString(getCodeProject().getLocation().toOSString()).append(projectFolderPath).toOSString()
                    + classPathSeparator;
        }

        // init class name
        IPath classPath = getCodePath().removeFirstSegments(1);
        String className = classPath.toString().replace('/', '.');

        String exportJar = ""; //$NON-NLS-1$
        if (exportingJob) {
            String version = ""; //$NON-NLS-1$
            if (process.getVersion() != null) {
                version = "_" + process.getVersion(); //$NON-NLS-1$
                version = version.replace(".", "_"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            exportJar = classPathSeparator
                    + (!win32 && exportingJob ? unixRootPath : "") + process.getName().toLowerCase() + version + ".jar" + classPathSeparator; //$NON-NLS-1$
            Set<JobInfo> jobInfos = ProcessorUtilities.getChildrenJobInfo((ProcessItem) process.getProperty().getItem());
            for (JobInfo jobInfo : jobInfos) {
                if (jobInfo.getJobVersion() != null) {
                    version = "_" + jobInfo.getJobVersion(); //$NON-NLS-1$
                    version = version.replace(".", "_"); //$NON-NLS-1$ //$NON-NLS-2$
                }
                exportJar += (!win32 && exportingJob ? unixRootPath : "") + jobInfo.getJobName().toLowerCase() + version + ".jar" + classPathSeparator; //$NON-NLS-1$
            }
        }

        String libFolder = ""; //$NON-NLS-1$
        if (exportingJob) {
            libFolder = new Path(this.getLibraryPath()) + classPathSeparator;
        } else {
            libFolder = new Path(externalLibDirectory.getAbsolutePath()).toPortableString() + classPathSeparator;
        }
        String portableCommand = new Path(command).toPortableString();
        String portableProjectPath = new Path(projectPath).toPortableString();

        if (exportingJob) {
            portableProjectPath = unixRootPathVar + classPathSeparator + portableProjectPath;
        }

        if (!win32 && exportingJob) {
            String libraryPath = ProcessorUtilities.getLibraryPath();
            if (libraryPath != null) {
                portableProjectPath = portableProjectPath.replace(libraryPath, unixRootPath + libraryPath);
                libFolder = libFolder.replace(libraryPath, unixRootPath + libraryPath);
            }

        }
        String[] strings;

        List<String> tmpParams = new ArrayList<String>();
        tmpParams.add(portableCommand);

        String[] proxyParameters = getProxyParameters();
        if (proxyParameters != null && proxyParameters.length > 0) {
            for (String str : proxyParameters) {
                tmpParams.add(str);
            }
        }
        tmpParams.add("-cp"); //$NON-NLS-1$
        tmpParams.add(libPath.toString() + portableProjectPath + exportJar + libFolder);
        tmpParams.add(className);
        strings = tmpParams.toArray(new String[0]);

        String[] cmd2 = addVMArguments(strings);
        // achen modify to fix 0001268
        if (!exportingJob) {
            return cmd2;
        } else {
            List<String> list = new ArrayList<String>();
            if (":".equals(classPathSeparator)) { //$NON-NLS-1$
                list.add("cd `dirname $0`\n"); //$NON-NLS-1$
                list.add("ROOT_PATH=`pwd`\n"); //$NON-NLS-1$
            } else {
                list.add("cd %~dp0\r\n"); //$NON-NLS-1$
            }
            list.addAll(Arrays.asList(cmd2));
            return list.toArray(new String[0]);
        }
        // end
    }

    private String[] addVMArguments(String[] strings) {
        String string = RunProcessPlugin.getDefault().getPreferenceStore().getString(RunProcessPrefsConstants.VMARGUMENTS);
        String replaceAll = string.trim();
        String[] vmargs = replaceAll.split(" "); //$NON-NLS-1$
        String[] lines = new String[strings.length + vmargs.length];
        System.arraycopy(strings, 0, lines, 0, 1);
        System.arraycopy(vmargs, 0, lines, 1, vmargs.length);
        System.arraycopy(strings, 1, lines, vmargs.length + 1, strings.length - 1);
        return lines;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IProcessor#getProcessorType()
     */
    @Override
    public String getProcessorType() {
        return JavaUtils.PROCESSOR_TYPE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IProcessor#getProcessorStates()
     */
    @Override
    public void setProcessorStates(int states) {
        if (states == STATES_RUNTIME) {
            new JavaProcessorRuntimeStates(this);
        } else if (states == STATES_EDIT) {
            new JavaProcessorEditStates(this);
        }

    }

    /*
     * Get current class name, and it imported package structure.
     * 
     * @see org.talend.designer.runprocess.IProcessor#getTypeName()
     */
    @Override
    public String getTypeName() {
        return this.typeName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.IProcessor#saveLaunchConfiguration()
     */
    @Override
    public Object saveLaunchConfiguration() throws CoreException {

        /*
         * When launch debug progress, just share all libraries between farther job and child jobs
         */
        computeLibrariesPath(false);

        ILaunchConfiguration config = null;
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        String projectName = this.getCodeProject().getName();
        ILaunchConfigurationType type = launchManager
                .getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
        if (type != null) {
            ILaunchConfigurationWorkingCopy wc = type.newInstance(null, launchManager
                    .generateUniqueLaunchConfigurationNameFrom(this.getCodePath().lastSegment()));
            wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
            wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, this.getTypeName());
            wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_STOP_IN_MAIN, true);
            wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, CTX_ARG + context.getName());
            config = wc.doSave();
        }
        return config;
    }

    // generate the ILaunchConfiguration with the parameter string.
    public Object saveLaunchConfigurationWithParam(String parameterStr) throws CoreException {

        /*
         * When launch debug progress, just share all libraries between farther job and child jobs
         */
        computeLibrariesPath(false);

        ILaunchConfiguration config = null;
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        String projectName = this.getCodeProject().getName();
        ILaunchConfigurationType type = launchManager
                .getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
        if (type != null) {
            ILaunchConfigurationWorkingCopy wc = type.newInstance(null, launchManager
                    .generateUniqueLaunchConfigurationNameFrom(this.getCodePath().lastSegment()));
            wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
            wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, this.getTypeName());
            wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_STOP_IN_MAIN, true);
            wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, CTX_ARG + context.getName() + parameterStr);
            config = wc.doSave();
        }
        return config;
    }

    // // see bug 3914, make the order of the jar files consistent with the
    // command
    // // line in run mode
    private void sortClasspath() throws CoreException {
        IClasspathEntry[] entries = javaProject.getRawClasspath();

        Set<String> listModulesReallyNeeded = process.getNeededLibraries(true);
        if (listModulesReallyNeeded == null) {
            listModulesReallyNeeded = new HashSet<String>();
        } else {
            // see bug 0005559: Import cannot be resolved in routine after
            // opening Job Designer
            for (ModuleNeeded moduleNeeded : ModulesNeededProvider.getModulesNeededForRoutines()) {
                listModulesReallyNeeded.add(moduleNeeded.getModuleName());
            }
        }

        // sort
        int exchange = 2; // The first,second library is JVM and SRC.
        for (String jar : listModulesReallyNeeded) {
            int index = indexOfEntry(entries, jar);
            if (index >= 0 && index != exchange) {
                // exchange
                IClasspathEntry entry = entries[index];
                IClasspathEntry first = entries[exchange];
                entries[index] = first;
                entries[exchange] = entry;
                exchange++;
            }
        }

        javaProject.setRawClasspath(entries, null);
    }

    private static int indexOfEntry(final IClasspathEntry[] dest, final String jarName) {

        if (jarName == null) {
            return -1;
        }

        for (int i = 0; i < dest.length; i++) {
            IClasspathEntry entry = dest[i];

            if (entry == null) {
                continue;
            }

            if (entry.getPath() == null || entry.getPath().lastSegment() == null) {
                continue;
            }
            if (entry.getPath().lastSegment().equals(jarName)) {
                return i;
            }
        }
        return -1;
    }

    public static IJavaProject getJavaProject() {
        return javaProject;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.runprocess.Processor#generateContextCode()
     */
    @Override
    public void generateContextCode() throws ProcessorException {
        RepositoryContext repositoryContext = (RepositoryContext) CorePlugin.getContext().getProperty(
                Context.REPOSITORY_CONTEXT_KEY);
        Project project = repositoryContext.getProject();

        ICodeGenerator codeGen;
        ICodeGeneratorService service = RunProcessPlugin.getDefault().getCodeGeneratorService();
        String javaInterpreter = ""; //$NON-NLS-1$
        String javaLib = ""; //$NON-NLS-1$
        String currentJavaProject = project.getTechnicalLabel();
        String javaContext = getContextPath().toOSString();

        codeGen = service.createCodeGenerator(process, false, false, javaInterpreter, javaLib, javaContext, currentJavaProject);

        updateContextCode(codeGen);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.debug.core.IJavaBreakpointListener#addingBreakpoint(org
     * .eclipse.jdt.debug.core.IJavaDebugTarget, org.eclipse.jdt.debug.core.IJavaBreakpoint)
     */
    public void addingBreakpoint(IJavaDebugTarget target, IJavaBreakpoint breakpoint) {

    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.eclipse.jdt.debug.core.IJavaBreakpointListener#
     * breakpointHasCompilationErrors(org.eclipse.jdt.debug.core. IJavaLineBreakpoint,
     * org.eclipse.jdt.core.dom.Message[])
     */
    public void breakpointHasCompilationErrors(IJavaLineBreakpoint breakpoint, Message[] errors) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.eclipse.jdt.debug.core.IJavaBreakpointListener# breakpointHasRuntimeException(org.eclipse.jdt.debug.core.
     * IJavaLineBreakpoint, org.eclipse.debug.core.DebugException)
     */
    public void breakpointHasRuntimeException(IJavaLineBreakpoint breakpoint, DebugException exception) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.debug.core.IJavaBreakpointListener#breakpointHit(org. eclipse.jdt.debug.core.IJavaThread,
     * org.eclipse.jdt.debug.core.IJavaBreakpoint)
     */
    public int breakpointHit(IJavaThread thread, IJavaBreakpoint breakpoint) {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.debug.core.IJavaBreakpointListener#breakpointInstalled
     * (org.eclipse.jdt.debug.core.IJavaDebugTarget , org.eclipse.jdt.debug.core.IJavaBreakpoint)
     */
    public void breakpointInstalled(IJavaDebugTarget target, IJavaBreakpoint breakpoint) {
        updateGraphicalNodeBreaking(breakpoint, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.debug.core.IJavaBreakpointListener#breakpointRemoved(
     * org.eclipse.jdt.debug.core.IJavaDebugTarget, org.eclipse.jdt.debug.core.IJavaBreakpoint)
     */
    public void breakpointRemoved(IJavaDebugTarget target, IJavaBreakpoint breakpoint) {
        if (!target.isTerminated()) {
            updateGraphicalNodeBreaking(breakpoint, true);
        }

    }

    /**
     * yzhang Comment method "updateGraphicalNodeBreaking".
     * 
     * @param breakpoint
     */
    private void updateGraphicalNodeBreaking(IJavaBreakpoint breakpoint, boolean removed) {
        try {
            Integer breakLineNumber = (Integer) breakpoint.getMarker().getAttribute(IMarker.LINE_NUMBER);
            if (breakLineNumber == null || breakLineNumber == -1) {
                return;
            }
            IFile codeFile = this.project.getFile(this.codePath);
            if (!codeFile.exists()) {
                JDIDebugModel.removeJavaBreakpointListener(this);
                return;
            }
            LineNumberReader lineReader = new LineNumberReader(new InputStreamReader(codeFile.getContents()));
            String content = null;
            while (lineReader.getLineNumber() < breakLineNumber - 3) {
                content = lineReader.readLine();
            }
            int startIndex = content.indexOf("[") + 1; //$NON-NLS-1$
            int endIndex = content.indexOf(" main ] start"); //$NON-NLS-1$
            if (startIndex != -1 && endIndex != -1) {
                String nodeUniqueName = content.substring(startIndex, endIndex);
                List<? extends INode> breakpointNodes = CorePlugin.getContext().getBreakpointNodes(process);
                List<? extends INode> graphicalNodes = process.getGraphicalNodes();
                if (graphicalNodes == null) {
                    return;
                }
                for (INode node : graphicalNodes) {
                    if (node.getUniqueName().equals(nodeUniqueName) && removed && breakpointNodes.contains(node)) {
                        CorePlugin.getContext().removeBreakpoint(process, node);
                        if (node instanceof Node) {
                            final INode currentNode = node;
                            Display.getDefault().syncExec(new Runnable() {

                                public void run() {
                                    ((Node) currentNode).removeStatus(Process.BREAKPOINT_STATUS);

                                }

                            });
                        }
                    } else if (node.getUniqueName().equals(nodeUniqueName) && !removed && !breakpointNodes.contains(node)) {
                        CorePlugin.getContext().addBreakpoint(process, node);
                        if (node instanceof Node) {
                            final INode currentNode = node;
                            Display.getDefault().syncExec(new Runnable() {

                                public void run() {
                                    ((Node) currentNode).addStatus(Process.BREAKPOINT_STATUS);

                                }

                            });
                        }
                    }
                }

            }

        } catch (CoreException e) {
            RuntimeExceptionHandler.process(e);
        } catch (IOException e) {
            RuntimeExceptionHandler.process(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.debug.core.IJavaBreakpointListener#installingBreakpoint
     * (org.eclipse.jdt.debug.core.IJavaDebugTarget , org.eclipse.jdt.debug.core.IJavaBreakpoint,
     * org.eclipse.jdt.debug.core.IJavaType)
     */
    public int installingBreakpoint(IJavaDebugTarget target, IJavaBreakpoint breakpoint, IJavaType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Use this to replace updateClasspath and sortClasspath
     * <p>
     * DOC YeXiaowei Comment method "computeClasspath".
     * 
     * @deprecated
     * @throws CoreException
     */
    private void computeClasspath(boolean clearExist) throws CoreException {

        if (rootProject == null || javaProject == null) {
            initializeProject();
        }

        List<IClasspathEntry> classpath = new ArrayList<IClasspathEntry>();
        if (clearExist) {
            // add basic classpath
            IClasspathEntry jreClasspathEntry = JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER")); //$NON-NLS-1$
            IClasspathEntry classpathEntry = JavaCore.newSourceEntry(javaProject.getPath().append(JavaUtils.JAVA_SRC_DIRECTORY));

            classpath.add(jreClasspathEntry);
            classpath.add(classpathEntry);
        } else {
            // reserve original
            IClasspathEntry[] exists = javaProject.getRawClasspath();
            if (exists != null && exists.length > 0) {
                for (IClasspathEntry entry : exists) {
                    if (!classpath.contains(entry)) {
                        classpath.add(entry);
                    }
                }
            }
        }

        // Some shadow process does not implements
        // process.getNeededLibraries(boolean true). If this, just add all jars
        Set<String> listModulesReallyNeeded = process.getNeededLibraries(true);
        if (listModulesReallyNeeded == null) {
            updateClasspath();
        } else {
            // see bug 0005559: Import cannot be resolved in routine after
            // opening Job Designer
            for (ModuleNeeded moduleNeeded : ModulesNeededProvider.getModulesNeededForRoutines()) {
                listModulesReallyNeeded.add(moduleNeeded.getModuleName());
            }

            File externalLibDirectory = new File(CorePlugin.getDefault().getLibrariesService().getLibrariesPath());
            if ((externalLibDirectory != null) && (externalLibDirectory.isDirectory())) {
                for (File externalLib : externalLibDirectory.listFiles(FilesUtils.getAcceptJARFilesFilter())) {
                    if (externalLib.isFile() && listModulesReallyNeeded.contains(externalLib.getName())) {
                        IClasspathEntry entry = JavaCore.newLibraryEntry(new Path(externalLib.getAbsolutePath()), null, null);
                        if (!classpath.contains(entry)) {
                            classpath.add(entry);
                        }
                    }
                }
            }

            IClasspathEntry[] classpathEntryArray = classpath.toArray(new IClasspathEntry[classpath.size()]);

            javaProject.setRawClasspath(classpathEntryArray, null);

            javaProject.setOutputLocation(javaProject.getPath().append(JavaUtils.JAVA_CLASSES_DIRECTORY), null);
        }
    }

    /*
     * @see bug 0005633. Classpath error when current job inlcude some tRunJob-es.
     * 
     * @see org.talend.designer.runprocess.IProcessor#computeLibrariesPath(boolean)
     */
    public void computeLibrariesPath(boolean clear) {
        try {
            updateClasspath();
            // see bug 5633
            sortClasspath();
        } catch (CoreException e) {
            ExceptionHandler.process(e);
        }
    }
}
