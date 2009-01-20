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
package org.talend.designer.mapper;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.language.LanguageManager;
import org.talend.core.model.genhtml.HTMLDocUtils;
import org.talend.core.model.genhtml.HTMLHandler;
import org.talend.core.model.genhtml.IHTMLDocConstants;
import org.talend.core.model.genhtml.XMLHandler;
import org.talend.core.model.metadata.types.JavaTypesManager;
import org.talend.core.model.process.IComponentDocumentation;
import org.talend.designer.mapper.external.data.ExternalMapperData;
import org.talend.designer.mapper.external.data.ExternalMapperTable;
import org.talend.designer.mapper.external.data.ExternalMapperTableEntry;

/**
 * This class is used for generating HTML file for Component 'tMap'. <br/>
 * 
 */
public class MapperComponentDocumentation implements IComponentDocumentation {

    private String componentName;

    private String tempFolderPath;

    private Document document;

    private ExternalMapperData externalData;

    private String previewPicPath;

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IComponentDocumentation#getHTMLFile()
     */
    public URL getHTMLFile() {

        String xmlFilepath = this.tempFolderPath + File.separatorChar + this.componentName
                + IHTMLDocConstants.XML_FILE_SUFFIX;

        String htmlFilePath = this.tempFolderPath + File.separatorChar + this.componentName
                + IHTMLDocConstants.HTML_FILE_SUFFIX;

        final Bundle b = Platform.getBundle(Activator.PLUGIN_ID);

        URL xslFileUrl = null;
        try {
            xslFileUrl = FileLocator.toFileURL(FileLocator
                    .find(b, new Path(IHTMLDocConstants.TMAP_XSL_FILE_PATH), null));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String xslFilePath = xslFileUrl.getPath();

        generateXMLInfo();

        XMLHandler.generateXMLFile(tempFolderPath, xmlFilepath, document);
        HTMLHandler.generateHTMLFile(this.tempFolderPath, xslFilePath, xmlFilepath, htmlFilePath);

        File htmlFile = new File(htmlFilePath);
        if (htmlFile.exists()) {
            try {
                return htmlFile.toURL();
            } catch (MalformedURLException e) {
                ExceptionHandler.process(e);
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IComponentDocumentation#setComponentLabel(java.lang.String)
     */
    public void setComponentName(String componentLabel) {
        this.componentName = componentLabel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IComponentDocumentation#setPath(java.lang.String)
     */
    public void setTempFolderPath(String tempFolderPath) {
        this.tempFolderPath = tempFolderPath;
    }

    /**
     * Sets the <code>externalData</code>.
     * 
     * @param externalData
     */
    public void setExternalData(ExternalMapperData externalData) {
        this.externalData = externalData;
    }

    /**
     * Generates all information which for XML file.
     */
    private void generateXMLInfo() {
        document = DocumentHelper.createDocument();
        Element externalNodeElement = document.addElement("externalNode"); //$NON-NLS-1$
        externalNodeElement.addAttribute("name", HTMLDocUtils.checkString(this.componentName)); //$NON-NLS-1$

        externalNodeElement.addAttribute("preview", HTMLDocUtils.checkString(this.previewPicPath)); //$NON-NLS-1$

        List<ExternalMapperTable> inputTables = externalData.getInputTables();
        List<ExternalMapperTable> outputTables = externalData.getOutputTables();
        List<ExternalMapperTable> varTables = externalData.getVarsTables();

        handleMapperTablesInfo(inputTables, externalNodeElement, IHTMLDocConstants.MAPPER_TABLE_INPUT);
        handleMapperTablesInfo(outputTables, externalNodeElement, IHTMLDocConstants.MAPPER_TABLE_OUPUT);
        handleMapperTablesInfo(varTables, externalNodeElement, IHTMLDocConstants.MAPPER_TABLE_VAR);
    }

    /**
     * Generates input tables information.
     * 
     * @param mapperTableType
     */
    private void handleMapperTablesInfo(List<ExternalMapperTable> inputTables, Element externalNodeElement,
            String mapperTableType) {
        List<ExternalMapperTable> tables = inputTables;
        if (!HTMLDocUtils.checkList(tables)) {
            return;
        }
        generateMapperTablesInfo(externalNodeElement, tables, mapperTableType);
    }

    /**
     * This method used for generating all mapper tables information into xml file.
     * 
     * @param externalNodeElement
     * @param tables
     * @param mapperTableType
     */
    private void generateMapperTablesInfo(Element externalNodeElement, List<ExternalMapperTable> tables,
            String mapperTableType) {
        Element mapperTableElement = externalNodeElement.addElement("mapperTable"); //$NON-NLS-1$
        mapperTableElement.addAttribute("type", HTMLDocUtils.checkString(mapperTableType)); //$NON-NLS-1$
        Element tableElement = null;
        for (ExternalMapperTable table : tables) {
            tableElement = mapperTableElement.addElement("table"); //$NON-NLS-1$
            generateTableSummaryInfo(mapperTableElement, tableElement, table);

            List<ExternalMapperTableEntry> metadataTableEntries = table.getMetadataTableEntries();
            if (!HTMLDocUtils.checkList(metadataTableEntries)) {
                continue;
            }

            Element metadataTableEntriesElement = tableElement.addElement("metadataTableEntries"); //$NON-NLS-1$
            for (ExternalMapperTableEntry entry : metadataTableEntries) {
                generateTablesEntriesInfo(metadataTableEntriesElement, entry);
            }

            List<ExternalMapperTableEntry> constraintTableEntries = table.getConstraintTableEntries();
            if (!HTMLDocUtils.checkList(constraintTableEntries)) {
                continue;
            }
            Element constraintTableEntriesElement = tableElement.addElement("constraintTableEntries"); //$NON-NLS-1$
            for (ExternalMapperTableEntry entry : constraintTableEntries) {
                generateTablesEntriesInfo(constraintTableEntriesElement, entry);
            }
        }
    }

    /**
     * Generates metadata tables entries information.
     * 
     * @param metadataTableEntriesElement
     * @param entry
     */
    private void generateTablesEntriesInfo(Element metadataTableEntriesElement, ExternalMapperTableEntry entry) {
        Element entryElement = metadataTableEntriesElement.addElement("entry"); //$NON-NLS-1$
        entryElement.addAttribute("name", HTMLDocUtils.checkString(entry.getName())); //$NON-NLS-1$
        String type = HTMLDocUtils.checkString(entry.getType());
        if (LanguageManager.getCurrentLanguage().equals(ECodeLanguage.JAVA) && type != "") { //$NON-NLS-1$
            type = JavaTypesManager.getTypeToGenerate(type, entry.isNullable());
        }

        entryElement.addAttribute("type", type); //$NON-NLS-1$
        entryElement.addAttribute("expression", HTMLDocUtils.checkString(entry.getExpression())); //$NON-NLS-1$
        entryElement.addAttribute("isNullable", String.valueOf(entry.isNullable())); //$NON-NLS-1$
    }

    /**
     * Generates the summary information for table.
     * 
     * @param mapperTableElement
     * @param tableElement
     * @param table
     */
    private void generateTableSummaryInfo(Element mapperTableElement, Element tableElement, ExternalMapperTable table) {

        tableElement.addAttribute("name", table.getName()); //$NON-NLS-1$
        tableElement.addAttribute("matching-mode", table.getMatchingMode()); //$NON-NLS-1$
        tableElement.addAttribute("isMinimized", String.valueOf(table.isMinimized())); //$NON-NLS-1$
        tableElement.addAttribute("isReject", String.valueOf(table.isReject())); //$NON-NLS-1$
        tableElement.addAttribute("isRejectInnerJoin", String.valueOf(table.isRejectInnerJoin())); //$NON-NLS-1$
        tableElement.addAttribute("isInnerJoin", String.valueOf(table.isInnerJoin())); //$NON-NLS-1$
        tableElement.addAttribute("isPersistent", String.valueOf(table.isPersistent())); //$NON-NLS-1$
        tableElement.addAttribute("expressionFilter", String.valueOf(table.getExpressionFilter())); //$NON-NLS-1$
        tableElement.addAttribute("activateExpressionFilter", String.valueOf(table.isActivateExpressionFilter())); //$NON-NLS-1$
    }

    /**
     * Sets the preview picture of component.
     * 
     * @param previewPicPath
     */
    public void setPreviewPicPath(String previewPicPath) {
        this.previewPicPath = previewPicPath;

    }
}
