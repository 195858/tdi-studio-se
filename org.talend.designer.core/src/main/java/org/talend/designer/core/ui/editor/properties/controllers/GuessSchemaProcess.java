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
package org.talend.designer.core.ui.editor.properties.controllers;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.talend.commons.utils.VersionUtils;
import org.talend.core.CorePlugin;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.language.LanguageManager;
import org.talend.core.model.components.IComponent;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.Property;
import org.talend.core.prefs.ITalendCorePrefConstants;
import org.talend.core.utils.CsvArray;
import org.talend.designer.core.ui.editor.nodecontainer.NodeContainer;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.process.EDatabaseComponentName;
import org.talend.designer.core.ui.editor.process.Process;
import org.talend.designer.core.utils.JavaProcessUtil;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.ProcessorException;
import org.talend.designer.runprocess.ProcessorUtilities;
import org.talend.repository.model.ComponentsFactoryProvider;

/**
 * DOC hyWang class global comment. Detailled comment
 */
public class GuessSchemaProcess {

    protected static final int maximumRowsToPreview = CorePlugin.getDefault().getPreferenceStore().getInt(
            ITalendCorePrefConstants.PREVIEW_LIMIT);

    private static final String DEFAULT_JOB_NAME = "Mock_job_for_Guess_schema"; //$NON-NLS-1$

    private String memoSQL;

    private Process process;

    private Property property;

    private INode node;

    private IComponent outputComponent;

    private static final String CSV_EXT = "csv"; //$NON-NLS-1$

    private IPath outpath;

    private IPath temppath;

    private String currentProcessEncoding = "ISO-8859-15"; //$NON-NLS-1$

    private IContext selectContext;

    private Connection conn;

    private DbInfo info;

    private static String LIB_NODE = "tLibraryLoad"; //$NON-NLS-1$

    private static String TEMPFILE_APPEND_NAME = "GuessSchemaDelimitedFile"; //$NON-NLS-1$

    public GuessSchemaProcess(Property property, INode node, IContext selectContext, String memoSQL, DbInfo info) {
        this.property = property;
        this.node = node;
        this.selectContext = selectContext;
        this.memoSQL = memoSQL.replace("\r\n", " "); //$NON-NLS-1$ //$NON-NLS-2$
        this.info = info;
        this.conn = info.getConn();
        initOutpath();
    }

    private void initOutpath() {
        outpath = new Path(System.getProperty("user.dir")).append("Temp"); //$NON-NLS-1$ //$NON-NLS-2$ 
    }

    private void buildProcess() {
        process = new Process(property);
        process.getContextManager().getListContext().addAll(node.getProcess().getContextManager().getListContext());
        process.getContextManager().setDefaultContext(this.selectContext);
        outputComponent = ComponentsFactoryProvider.getInstance().get(
                EDatabaseComponentName.FILEDELIMITED.getOutPutComponentName());

        // create the tLibraryLoad for the input node
        for (ModuleNeeded module : node.getComponent().getModulesNeeded()) {
            if (module.isRequired()) {
                Node libNode1 = new Node(ComponentsFactoryProvider.getInstance().get(LIB_NODE), process);
                libNode1.setPropertyValue("LIBRARY", "\"" + module.getModuleName() + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                process.addNodeContainer(new NodeContainer(libNode1));
            }
        }
        for (IElementParameter param : node.getElementParameters()) {
            Set<String> neededLibraries = new HashSet<String>();
            JavaProcessUtil.findMoreLibraries(neededLibraries, param, true);
            for (String lib : neededLibraries) {
                Node libNode1 = new Node(ComponentsFactoryProvider.getInstance().get(LIB_NODE), process);
                libNode1.setPropertyValue("LIBRARY", "\"" + lib + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                process.addNodeContainer(new NodeContainer(libNode1));
            }
        }

        // create the tLibraryLoad for the output component which is "tFileOutputDelimited"
        for (ModuleNeeded module : outputComponent.getModulesNeeded()) {
            Node libNode2 = new Node(ComponentsFactoryProvider.getInstance().get(LIB_NODE), process);
            libNode2.setPropertyValue("LIBRARY", "\"" + module.getModuleName() + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            process.addNodeContainer(new NodeContainer(libNode2));
        }
        int fetchSize = 1000; // for sql statement, feature 6622.
        String codeStart, codeMain, codeEnd;
        temppath = (Path) buildTempCSVFilename();
        codeStart = "java.lang.Class.forName(\"" + info.getDriverClassName() + "\");\r\n" + "String url = \"" + info.getUrl() //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + "\";\r\n" + "java.sql.Connection conn = java.sql.DriverManager.getConnection(url, \"" + info.getUsername() //$NON-NLS-1$ //$NON-NLS-2$
                + "\", \"" + info.getPwd() + "\");\r\n" + "java.sql.Statement stm = conn.createStatement();\r\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + "stm.setFetchSize(" + fetchSize + ");\r\n" //$NON-NLS-1$ //$NON-NLS-2$
                + "java.sql.ResultSet rs = stm.executeQuery(" + memoSQL + ");\r\n" //$NON-NLS-1$ //$NON-NLS-2$
                + "java.sql.ResultSetMetaData rsmd = rs.getMetaData();\r\n" + "int numbOfColumn = rsmd.getColumnCount();\r\n" //$NON-NLS-1$ //$NON-NLS-2$
                + "\r\n" + "String fileName = (new java.io.File(\r\n" + "                    \"" + temppath //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + "\")).getAbsolutePath().replace(\r\n" + "                    \"\\\\\", \"/\");\r\n" //$NON-NLS-1$ //$NON-NLS-2$
                + "com.csvreader.CsvWriter csvWriter = new com.csvreader.CsvWriter(\r\n" //$NON-NLS-1$
                + "                    new java.io.BufferedWriter(new java.io.OutputStreamWriter(\r\n" //$NON-NLS-1$
                + "                            new java.io.FileOutputStream(\r\n" //$NON-NLS-1$
                + "                                    fileName, false),\r\n" //$NON-NLS-1$
                + "                            \"ISO-8859-15\")), ';');\r\n" + "                            \r\n" //$NON-NLS-1$ //$NON-NLS-2$
                + "csvWriter.setEscapeMode(com.csvreader.CsvWriter.ESCAPE_MODE_DOUBLED);\r\n" //$NON-NLS-1$
                + "csvWriter.setTextQualifier('\"');\r\n" + "csvWriter.setForceQualifier(true);\r\n" + "int nbRows = 0;\r\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + "String[] columnNames = new String[numbOfColumn];\r\n" + "String[] nullables = new String[numbOfColumn];" //$NON-NLS-1$ //$NON-NLS-2$
                + "for(int i = 1;i<=numbOfColumn;i++){\r\n" + "columnNames[i-1] = rsmd.getColumnName(i);\r\n" //$NON-NLS-1$ //$NON-NLS-2$
                + "nullables[i-1] = rsmd.isNullable(i) == 0? \"false\" : \"true\";" + "}\r\n" //$NON-NLS-1$ //$NON-NLS-2$
                + "csvWriter.writeRecord(columnNames);" + "csvWriter.writeRecord(nullables);" + "while (rs.next()) {"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        codeMain = "String[] dataOneRow = new String[numbOfColumn];\r\n" + "for (int i = 1; i <= numbOfColumn; i++) {\r\n" //$NON-NLS-1$ //$NON-NLS-2$
                + "    \r\n" + "    String tempStr = rs.getString(i);\r\n" + "    dataOneRow[i-1] = tempStr;\r\n" + "}\r\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                + "csvWriter.writeRecord(dataOneRow);"; //$NON-NLS-1$

        codeEnd = "nbRows++;\r\n" + "    if (nbRows > " + maximumRowsToPreview + ") break;\r\n" + "}\r\n" + "stm.close();\r\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                + "conn.close();\r\n" + "csvWriter.close();\r\n"; //$NON-NLS-1$ //$NON-NLS-2$

        IComponent component = null;
        switch (LanguageManager.getCurrentLanguage()) {
        case JAVA:
            component = ComponentsFactoryProvider.getInstance().get("tJavaFlex"); //$NON-NLS-1$
            break;
        case PERL:
        default:
            component = ComponentsFactoryProvider.getInstance().get("tPerlFlex"); //$NON-NLS-1$
            break;

        }

        Node flexNode = new Node(component, process); //$NON-NLS-1$
        flexNode.setPropertyValue("CODE_START", codeStart); //$NON-NLS-1$
        flexNode.setPropertyValue("CODE_MAIN", codeMain); //$NON-NLS-1$
        flexNode.setPropertyValue("CODE_END", codeEnd); //$NON-NLS-1$

        process.addNodeContainer(new NodeContainer(flexNode));
    }

    // write content to a temp .csv file
    private IPath buildTempCSVFilename() {
        String filename = outpath.lastSegment();

        if (outpath.getFileExtension() != null) {
            filename = filename.substring(0, filename.length() - outpath.getFileExtension().length());
        } else // Check if file has no suffix.
        {
            int length = filename.length();
            filename = filename.substring(0, length) + TEMPFILE_APPEND_NAME + "."; //$NON-NLS-1$
        }

        filename += CSV_EXT;
        IPath tempPath;
        tempPath = Path.fromOSString(CorePlugin.getDefault().getPreferenceStore().getString(
                ITalendCorePrefConstants.FILE_PATH_TEMP));
        tempPath = tempPath.append(filename);

        return tempPath;
    }

    public CsvArray run() throws ProcessorException {
        buildProcess();
        IProcessor processor = ProcessorUtilities.getProcessor(process);
        processor.setContext(selectContext);
        File previousFile = temppath.toFile();
        if (previousFile.exists()) {
            previousFile.delete();
        }
        java.lang.Process executeprocess = processor.run(IProcessor.NO_STATISTICS, IProcessor.NO_TRACES, null);
        ProcessStreamTrashReader.readAndForget(executeprocess);

        try {
            CsvArray array = new CsvArray();
            array = array.createFrom(previousFile, currentProcessEncoding);
            return array;
        } catch (IOException ioe) {
            throw new ProcessorException(ioe);
        }

    }

    public static Property getNewmockProperty() {

        Property mockProperty = PropertiesFactory.eINSTANCE.createProperty();
        mockProperty.setAuthor(((RepositoryContext) CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY))
                .getUser());
        mockProperty.setVersion(VersionUtils.DEFAULT_VERSION);
        mockProperty.setStatusCode(""); //$NON-NLS-1$
        mockProperty.setId("ID"); //$NON-NLS-1$
        mockProperty.setLabel(DEFAULT_JOB_NAME);

        return mockProperty;
    }
}
