// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.dbmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.talend.commons.exception.SystemException;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.core.model.genhtml.HTMLDocUtils;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.process.IComponentDocumentation;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.IExternalData;
import org.talend.core.model.process.Problem;
import org.talend.core.model.temp.ECodePart;
import org.talend.designer.abstractmap.AbstractMapComponent;
import org.talend.designer.codegen.ICodeGeneratorService;
import org.talend.designer.core.model.utils.emf.talendfile.AbstractExternalData;
import org.talend.designer.dbmap.external.data.ExternalDbMapData;
import org.talend.designer.dbmap.external.data.ExternalDbMapEntry;
import org.talend.designer.dbmap.external.data.ExternalDbMapTable;
import org.talend.designer.dbmap.i18n.Messages;
import org.talend.designer.dbmap.language.generation.DbGenerationManager;
import org.talend.designer.dbmap.language.generation.GenericDbGenerationManager;
import org.talend.designer.dbmap.language.mysql.MysqlGenerationManager;
import org.talend.designer.dbmap.language.oracle.OracleGenerationManager;
import org.talend.designer.dbmap.language.teradata.TeradataGenerationManager;
import org.talend.designer.dbmap.model.emf.dbmap.DBMapData;
import org.talend.designer.dbmap.model.emf.dbmap.DBMapperTableEntry;
import org.talend.designer.dbmap.model.emf.dbmap.DbmapFactory;
import org.talend.designer.dbmap.model.emf.dbmap.FilterEntry;
import org.talend.designer.dbmap.model.emf.dbmap.InputTable;
import org.talend.designer.dbmap.model.emf.dbmap.OutputTable;
import org.talend.designer.dbmap.model.tableentry.TableEntryLocation;
import org.talend.designer.dbmap.utils.DBMapHelper;
import org.talend.designer.dbmap.utils.DataMapExpressionParser;
import org.talend.designer.dbmap.utils.problems.ProblemsAnalyser;

/**
 * DOC amaumont class global comment. Detailled comment <br/>
 * 
 * $Id: MapperComponent.java 1782 2007-02-03 07:57:38Z bqian $
 * 
 */
public class DbMapComponent extends AbstractMapComponent {

    private MapperMain mapperMain;

    private List<IMetadataTable> metadataListOut;

    private ExternalDbMapData externalData;

    private DbGenerationManager generationManager;

    /**
     * DOC amaumont MapperComponent constructor comment.
     */
    public DbMapComponent() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.AbstractExternalNode#initialize()
     */
    public void initialize() {
        super.initialize();
        initMapperMain(false);
        mapperMain.loadInitialParamters();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IExternalComponent#getPersistentData()
     */
    public IExternalData getExternalData() {
        if (this.externalData == null) {
            this.externalData = new ExternalDbMapData();
        }
        return this.externalData;
    }

    private void initMapperMain(boolean forceNew) {
        if (forceNew || !forceNew && mapperMain == null) {
            mapperMain = new MapperMain(this);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IExternalComponent#open(org.eclipse.swt.widgets.Display)
     */
    public int open(final Display display) {
        // TimeMeasure.start("Total open");
        // TimeMeasure.display = false;
        initMapperMain(true);
        mapperMain.createModelFromExternalData(getIODataComponents(), getMetadataList(), externalData, true);
        Shell shell = mapperMain.createUI(display);
        // TimeMeasure.display = true;
        // TimeMeasure.end("Total open");
        while (!shell.isDisposed()) {
            try {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            } catch (Throwable e) {
                if (MapperMain.isStandAloneMode()) {
                    // e.printStackTrace();
                    ExceptionHandler.process(e);
                } else {
                    ExceptionHandler.process(e);
                }
            }
        }
        if (MapperMain.isStandAloneMode()) {
            display.dispose();
        }
        restoreMapperModelFromInternalData();
        return mapperMain.getMapperDialogResponse();
    }

    /**
     * DOC amaumont Comment method "refreshMapperConnectorData".
     */
    public void restoreMapperModelFromInternalData() {
        mapperMain.loadModelFromInternalData();
        metadataListOut = mapperMain.getMetadataListOut();
        externalData = mapperMain.buildExternalData();
        sortOutputsConnectionsLikeVisualOrder();
    }

    /**
     * Sort outgoingConnections for code generation as visible output zone of tMap.
     */
    @SuppressWarnings("unchecked")//$NON-NLS-1$
    private void sortOutputsConnectionsLikeVisualOrder() {

        if (!MapperMain.isStandAloneMode()) {

            List<IConnection> outgoingConnections = (List<IConnection>) getOutgoingConnections();
            Map<String, IConnection> connectionNameToOutgoingConnection = new HashMap<String, IConnection>();
            for (IConnection connection : outgoingConnections) {
                connectionNameToOutgoingConnection.put(connection.getUniqueName(), connection);
            }

            List<ExternalDbMapTable> outputTables = externalData.getOutputTables();
            List<IConnection> tmpList = new ArrayList<IConnection>(outgoingConnections);
            outgoingConnections.clear();

            int lstSize = outputTables.size();
            for (int i = 0; i < lstSize; i++) {
                ExternalDbMapTable table = outputTables.get(i);
                String tableName = table.getName();

                IConnection connection = connectionNameToOutgoingConnection.get(tableName);
                if (connection != null) {
                    outgoingConnections.add(connection);
                }
            }
            // add connections without metadata
            for (IConnection connection : tmpList) {
                if (!outgoingConnections.contains(connection)) {
                    outgoingConnections.add(connection);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IExternalComponent#open()
     */
    public int open(final Composite parent) {
        // initMapperMain();
        // mapperMain.createModelFromExternalData(getIODataComponents(), getMetadataList(), externalData, true);
        // mapperMain.createUI(parent);
        // return mapperMain.getMapperDialogResponse();
        return open(parent.getDisplay());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IExternalComponent#setPersistentData(java.lang.Object)
     */
    public void setExternalData(IExternalData externalData) {
        this.externalData = (ExternalDbMapData) externalData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.INode#getGeneratedCode()
     */
    public String getGeneratedCode() {
        try {
            ICodeGeneratorService service = PluginUtils.getCodeGeneratorService();

            return service.createCodeGenerator().generateComponentCode(this, ECodePart.MAIN);
        } catch (SystemException e) {
            ExceptionHandler.process(e);
        }
        return ""; //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.INode#getMetadataList()
     */
    public List<IMetadataTable> getMetadataList() {
        return this.metadataListOut;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.INode#setMetadataList(java.util.List)
     */
    public void setMetadataList(List<IMetadataTable> metadataTablesOut) {
        this.metadataListOut = metadataTablesOut;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.AbstractExternalNode#setExternalXmlData(java.io.InputStream)
     */
    // public void loadDataIn(InputStream in, Reader stringReader) throws IOException, ClassNotFoundException {
    // StringBuilder input = null;
    // if (stringReader != null) {
    // Unmarshaller unmarshaller = new Unmarshaller(ExternalDbMapData.class);
    // unmarshaller.setWhitespacePreserve(true);
    // try {
    // BufferedReader r = new BufferedReader(stringReader);
    // input = new StringBuilder();
    // String line;
    // while ((line = r.readLine()) != null) {
    // input.append(line);
    //                    input.append("\r\n"); //$NON-NLS-0$
    // }
    // // see 13019 ,remove all table-entries node when load job which contains db mapper components from old
    // // version's tos
    //                String buf = input.toString().replaceAll("<table-entries .*?>.*?</table-entries>", ""); //$NON-NLS-0$ //$NON-NLS-1$
    // stringReader.close();
    // stringReader = new StringReader(buf);
    // externalData = (ExternalDbMapData) unmarshaller.unmarshal(stringReader);
    // } catch (MarshalException e) {
    // ExceptionHandler.process(e);
    // } catch (ValidationException e) {
    // ExceptionHandler.process(e);
    // } finally {
    // if (stringReader != null) {
    // stringReader.close();
    // }
    // }
    // }
    // }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IExternalNode#loadDataOut(java.io.OutputStream, java.io.Writer)
     */
    // public void loadDataOut(final OutputStream out, Writer writer) throws IOException {
    //
    // initMapperMain();
    // mapperMain.createModelFromExternalData(getIncomingConnections(), getOutgoingConnections(), externalData,
    // getMetadataList(), false);
    // ExternalDbMapData data = mapperMain.buildExternalData();
    // if (mapperMain != null && data != null) {
    //
    // try {
    // Marshaller marshaller = new Marshaller(writer);
    // marshaller.marshal(externalData);
    // } catch (MarshalException e) {
    // ExceptionHandler.process(e);
    // } catch (ValidationException e) {
    // ExceptionHandler.process(e);
    // } catch (IOException e) {
    // ExceptionHandler.process(e);
    // } finally {
    // if (writer != null) {
    // writer.close();
    // }
    // }
    // }
    // }

    @Override
    public void buildExternalData(AbstractExternalData abstractData) {
        externalData = new ExternalDbMapData();
        if (abstractData instanceof DBMapData) {
            DBMapData mapperData = (DBMapData) abstractData;
            List<ExternalDbMapTable> externalTables = new ArrayList<ExternalDbMapTable>();
            // input
            for (InputTable pTable : mapperData.getInputTables()) {
                ExternalDbMapTable externalTable = new ExternalDbMapTable();
                externalTable.setName(pTable.getName());
                externalTable.setMinimized(pTable.isMinimized());
                externalTable.setAlias(pTable.getAlias());
                externalTable.setJoinType(pTable.getJoinType());
                externalTable.setTableName(pTable.getTableName());
                List<ExternalDbMapEntry> entities = new ArrayList<ExternalDbMapEntry>();
                for (DBMapperTableEntry pEntity : pTable.getDBMapperTableEntries()) {
                    ExternalDbMapEntry entity = new ExternalDbMapEntry();
                    entity.setExpression(pEntity.getExpression());
                    entity.setJoin(pEntity.isJoin());
                    entity.setName(pEntity.getName());
                    entity.setOperator(pEntity.getOperator());
                    entities.add(entity);
                }
                externalTable.setMetadataTableEntries(entities);
                externalTables.add(externalTable);
            }
            externalData.setInputTables(externalTables);

            // output
            externalTables = new ArrayList<ExternalDbMapTable>();
            for (OutputTable pTable : mapperData.getOutputTables()) {
                ExternalDbMapTable externalTable = new ExternalDbMapTable();
                externalTable.setName(pTable.getName());
                externalTable.setMinimized(pTable.isMinimized());
                externalTable.setTableName(pTable.getTableName());
                List<ExternalDbMapEntry> entities = new ArrayList<ExternalDbMapEntry>();
                for (DBMapperTableEntry pEntity : pTable.getDBMapperTableEntries()) {
                    ExternalDbMapEntry entity = new ExternalDbMapEntry();
                    entity.setExpression(pEntity.getExpression());
                    entity.setName(pEntity.getName());
                    entities.add(entity);
                }
                externalTable.setMetadataTableEntries(entities);
                externalTables.add(externalTable);
                // filters
                entities = new ArrayList<ExternalDbMapEntry>();
                for (FilterEntry pFilter : pTable.getFilterEntries()) {
                    ExternalDbMapEntry entity = new ExternalDbMapEntry();
                    entity.setExpression(pFilter.getExpression());
                    entity.setName(pFilter.getName());
                    entities.add(entity);
                }
                externalTable.setCustomConditionsEntries(entities);

            }
            externalData.setOutputTables(externalTables);

        }
        this.setExternalData(externalData);

    }

    @Override
    public AbstractExternalData getExternalEmfData() {
        final DBMapData emfMapperData = DbmapFactory.eINSTANCE.createDBMapData();
        if (mapperMain == null) {
            initMapperMain(false);
        }
        mapperMain.createModelFromExternalData(getIncomingConnections(), getOutgoingConnections(), externalData,
                getMetadataList(), false);
        ExternalDbMapData data = mapperMain.buildExternalData();
        if (mapperMain != null && data != null) {
            if (externalData != null) {
                DBMapHelper.saveDataToEmf(emfMapperData, externalData);
            }
        }
        return emfMapperData;
    }

    public void renameInputConnection(String oldConnectionName, String newConnectionName) {
        if (oldConnectionName == null || newConnectionName == null) {
            throw new NullPointerException();
        }
        if (externalData != null) {
            List<ExternalDbMapTable> inputTables = externalData.getInputTables();
            for (ExternalDbMapTable table : inputTables) {
                if (table.getTableName() != null
                        && (table.getTableName().equals(oldConnectionName) || table.getName().equals(oldConnectionName))) {
                    table.setTableName(newConnectionName);
                    table.setName(newConnectionName);
                    TableEntryLocation oldLocation = new TableEntryLocation(oldConnectionName, null);
                    TableEntryLocation newLocation = new TableEntryLocation(newConnectionName, null);
                    replaceLocationsInAllExpressions(oldLocation, newLocation, true);
                }
            }
        }
    }

    public void renameOutputConnection(String oldName, String newName) {
        if (oldName == null || newName == null) {
            throw new NullPointerException();
        }
        if (externalData != null) {
            List<ExternalDbMapTable> outputTables = externalData.getOutputTables();
            for (ExternalDbMapTable table : outputTables) {
                if (table.getTableName() != null && table.getTableName().equals(oldName)) {
                    table.setTableName(newName);
                }
            }
        }
    }

    protected void renameMetadataColumnName(String conectionName, String oldColumnName, String newColumnName) {
        if (conectionName == null || oldColumnName == null || newColumnName == null) {
            throw new NullPointerException();
        }
        if (externalData != null) {
            // rename metadata column name
            List<ExternalDbMapTable> tables = new ArrayList<ExternalDbMapTable>(externalData.getInputTables());
            tables.addAll(externalData.getOutputTables());
            ExternalDbMapTable tableFound = null;
            for (ExternalDbMapTable table : tables) {
                if (table.getName().equals(conectionName)) {
                    List<ExternalDbMapEntry> metadataTableEntries = table.getMetadataTableEntries();
                    for (ExternalDbMapEntry entry : metadataTableEntries) {
                        if (entry.getName().equals(oldColumnName)) {
                            entry.setName(newColumnName);
                            tableFound = table;
                            break;
                        }
                    }
                    break;
                }
            }

            // it is necessary to update expressions only if renamed column come from input table
            if (tableFound != null && externalData.getInputTables().indexOf(tableFound) != -1) {
                TableEntryLocation oldLocation = new TableEntryLocation(conectionName, oldColumnName);
                TableEntryLocation newLocation = new TableEntryLocation(conectionName, newColumnName);
                replaceLocationsInAllExpressions(oldLocation, newLocation, false);
            }

        }
    }

    /**
     * DOC amaumont Comment method "replaceLocations".
     * 
     * @param oldLocation
     * @param newLocation
     * @param tableRenamed TODO
     * @param newTableName
     * @param newColumnName
     */
    private void replaceLocationsInAllExpressions(TableEntryLocation oldLocation, TableEntryLocation newLocation,
            boolean tableRenamed) {
        // replace old location by new location for all expressions in mapper
        List<ExternalDbMapTable> tables = new ArrayList<ExternalDbMapTable>(externalData.getInputTables());
        tables.addAll(new ArrayList<ExternalDbMapTable>(externalData.getVarsTables()));
        tables.addAll(new ArrayList<ExternalDbMapTable>(externalData.getOutputTables()));
        DataMapExpressionParser dataMapExpressionParser = new DataMapExpressionParser(getGenerationManager().getLanguage());
        // loop on all tables
        for (ExternalDbMapTable table : tables) {
            List<ExternalDbMapEntry> metadataTableEntries = table.getMetadataTableEntries();
            if (metadataTableEntries != null) {
                // loop on all entries of current table
                for (ExternalDbMapEntry entry : metadataTableEntries) {
                    replaceLocation(oldLocation, newLocation, entry, dataMapExpressionParser, tableRenamed);
                } // for (ExternalMapperTableEntry entry : metadataTableEntries) {
            }
            if (table.getCustomConditionsEntries() != null) {
                for (ExternalDbMapEntry entry : table.getCustomConditionsEntries()) {
                    replaceLocation(oldLocation, newLocation, entry, dataMapExpressionParser, tableRenamed);
                }
            }
        } // for (ExternalMapperTable table : tables) {
    }

    public void replaceLocation(TableEntryLocation oldLocation, TableEntryLocation newLocation, ExternalDbMapEntry entry,
            DataMapExpressionParser dataMapExpressionParser, boolean tableRenamed) {
        String currentExpression = entry.getExpression();
        if (currentExpression == null || currentExpression.length() == 0) {
            return;
        }
        TableEntryLocation[] tableEntryLocations = dataMapExpressionParser.parseTableEntryLocations(currentExpression);
        // loop on all locations of current expression
        for (int i = 0; i < tableEntryLocations.length; i++) {
            TableEntryLocation currentLocation = tableEntryLocations[i];
            if (tableRenamed && oldLocation.tableName.equals(currentLocation.tableName)) {
                oldLocation.columnName = currentLocation.columnName;
                newLocation.columnName = currentLocation.columnName;
            }
            if (currentLocation.equals(oldLocation)) {
                currentExpression = dataMapExpressionParser.replaceLocation(currentExpression, currentLocation, newLocation);
            }
        } // for (int i = 0; i < tableEntryLocations.length; i++) {
        entry.setExpression(currentExpression);

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.AbstractExternalNode#getProblems()
     */
    @Override
    public List<Problem> getProblems() {
        if (mapperMain == null) {
            initMapperMain(false);
        }
        ProblemsAnalyser problemsAnalyser = new ProblemsAnalyser(mapperMain.getMapperManager());
        return problemsAnalyser.checkProblems(externalData);
    }

    /**
     * Getter for mapperMain.
     * 
     * @return the mapperMain
     */
    public MapperMain getMapperMain() {
        return this.mapperMain;
    }

    public DbGenerationManager getGenerationManager() {
        if (generationManager == null) {
            IElementParameter elementParameter = getElementParameter("COMPONENT_NAME"); //$NON-NLS-1$
            String value = (String) elementParameter.getValue();
            if (value.contains("tELTTeradataMap")) { //$NON-NLS-1$
                generationManager = new TeradataGenerationManager();
            } else if (value.contains("tELTOracleMap")) { //$NON-NLS-1$
                generationManager = new OracleGenerationManager();
            } else if (value.contains("tELTMysqlMap")) { //$NON-NLS-1$
                generationManager = new MysqlGenerationManager();
            } else if (value.contains("tELTJDBCMap") || value.contains("tELTMSSqlMap") || value.contains("tELTSybaseMap") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    || value.contains("tELTPostgresqlMap")) { //$NON-NLS-1$
                generationManager = new GenericDbGenerationManager();
            } else {
                throw new IllegalArgumentException(Messages.getString("DbMapComponent.unknowValue") + value); //$NON-NLS-1$
            }
        }

        return generationManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IExternalNode#getComponentDocumentation(java.lang.String, java.lang.String)
     */
    public IComponentDocumentation getComponentDocumentation(String componentName, String tempFolderPath) {
        DbMapComponentDocumentation componentDocumentation = new DbMapComponentDocumentation();
        componentDocumentation.setComponentName(componentName);
        componentDocumentation.setTempFolderPath(tempFolderPath);
        componentDocumentation.setExternalData(this.getExternalData());

        componentDocumentation.setPreviewPicPath(HTMLDocUtils.getPreviewPicPath(this));

        return componentDocumentation;
    }

    /**
     * 
     * DOC amaumont Comment method "hasOrRenameData".
     * 
     * @param oldName
     * @param newName can be null if <code>renameAction</code> is false
     * @param renameAction true to rename in all expressions, false to get boolean if present in one of the expressions
     * @return
     */
    protected boolean hasOrRenameData(String oldName, String newName, boolean renameAction) {
        if (oldName == null || newName == null && renameAction) {
            throw new NullPointerException();
        }

        if (externalData != null) {
            List<ExternalDbMapTable> tables = new ArrayList<ExternalDbMapTable>(externalData.getInputTables());
            tables.addAll(externalData.getOutputTables());
            if (externalData.getVarsTables() != null) {
                tables.addAll(externalData.getVarsTables());
            }

            for (ExternalDbMapTable table : tables) {

                List<ExternalDbMapEntry> metadataTableEntries = table.getMetadataTableEntries();

                // if (table.getExpressionFilter() != null) {
                // if (renameAction) {
                // String expression = renameDataIntoExpression(pattern, matcher, substitution,
                // table.getExpressionFilter());
                // table.setExpressionFilter(expression);
                // } else {
                // if (hasDataIntoExpression(pattern, matcher, table.getExpressionFilter())) {
                // return true;
                // }
                // }
                // }

                if (metadataTableEntries != null) {
                    // loop on all entries of current table
                    for (ExternalDbMapEntry entry : metadataTableEntries) {
                        if (hasOrRenameEntry(entry, oldName, newName, renameAction)) {
                            return true;
                        }
                    } // for (ExternalMapperTableEntry entry : metadataTableEntries) {
                }
                if (table.getCustomConditionsEntries() != null) {
                    for (ExternalDbMapEntry entry : table.getCustomConditionsEntries()) {
                        if (hasOrRenameEntry(entry, oldName, newName, renameAction)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IExternalNode#getTMapExternalData()
     */
    public IExternalData getTMapExternalData() {
        // TODO Auto-generated method stub
        return null;
    }
}
