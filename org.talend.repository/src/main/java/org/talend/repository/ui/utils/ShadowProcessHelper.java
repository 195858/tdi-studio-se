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
package org.talend.repository.ui.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.talend.commons.ui.utils.PathUtils;
import org.talend.core.model.metadata.IMetadataColumn;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.MetadataColumn;
import org.talend.core.model.metadata.MetadataTable;
import org.talend.core.model.metadata.builder.connection.FileConnection;
import org.talend.core.model.metadata.builder.connection.LDAPSchemaConnection;
import org.talend.core.model.metadata.builder.connection.LdifFileConnection;
import org.talend.core.model.metadata.builder.connection.SchemaTarget;
import org.talend.core.model.metadata.builder.connection.WSDLSchemaConnection;
import org.talend.core.model.metadata.builder.connection.XmlFileConnection;
import org.talend.core.model.metadata.builder.connection.XmlXPathLoopDescriptor;
import org.talend.core.model.utils.TalendTextUtils;
import org.talend.core.utils.CsvArray;
import org.talend.repository.i18n.Messages;
import org.talend.repository.preview.AsynchronousPreviewHandler;
import org.talend.repository.preview.IPreview;
import org.talend.repository.preview.LDAPSchemaBean;
import org.talend.repository.preview.ProcessDescription;
import org.talend.repository.preview.WSDLSchemaBean;

/**
 * Create a ProcessDescription to use in the step2 & step3 of CSV File Wizard on Shadow mode.
 * 
 * $Id$
 * 
 */
public class ShadowProcessHelper {

    private static Logger log = Logger.getLogger(ShadowProcessHelper.class);

    /**
     * Create a ProcessDescription and set it width the value of FileConnection. Particularity : field FieldSeparator,
     * RowSeparator, EscapeChar and TextEnclosure are surround by double quote.
     * 
     * This method is usefull to adapt a processDescription before run the shadow process.
     * 
     * @param FileConnection
     * 
     * @return ProcessDescription
     */
    public static ProcessDescription getProcessDescription(final FileConnection connection) {
        ProcessDescription processDescription = new ProcessDescription();

        processDescription.setFilepath(TalendTextUtils.addQuotes(PathUtils.getPortablePath(connection.getFilePath())));

        processDescription.setServer(TalendTextUtils.addQuotes(connection.getServer()));

        processDescription.setRowSeparator(connection.getRowSeparatorValue());

        processDescription.setFieldSeparator(connection.getFieldSeparatorValue());

        processDescription.setEncoding(connection.getEncoding());

        // we make differences between Pattern in DELIMITED, CSV and REGEX FileConnection
        if (connection.getEscapeChar() != null || connection.getTextEnclosure() != null) {
            processDescription.setPattern(connection.getFieldSeparatorValue());
        } else {
            processDescription.setPattern(connection.getFieldSeparatorValue());
        }

        processDescription.setHeaderRow(connection.getHeaderValue());
        processDescription.setFooterRow(connection.getFooterValue());
        processDescription.setLimitRows(connection.getLimitValue());
        if (connection.getEscapeChar() != null
                && !connection.getEscapeChar().equals("") && !connection.getEscapeChar().equals("Empty")) { //$NON-NLS-1$
            processDescription.setEscapeCharacter(connection.getEscapeChar());
        } else {
            processDescription.setEscapeCharacter(TalendTextUtils.addQuotes("")); //$NON-NLS-1$
        }
        if (connection.getTextEnclosure() != null
                && !connection.getTextEnclosure().equals("") && !connection.getTextEnclosure().equals("Empty")) { //$NON-NLS-1$
            processDescription.setTextEnclosure(connection.getTextEnclosure());
        } else {
            processDescription.setTextEnclosure(TalendTextUtils.addQuotes("")); //$NON-NLS-1$
        }

        processDescription.setRemoveEmptyRow(connection.isRemoveEmptyRow());
        processDescription.setEncoding(TalendTextUtils.addQuotes(connection.getEncoding()));

        return processDescription;
    }

    /**
     * Create a ProcessDescription and set it width the value of XmlFileConnection.
     * 
     * This method is usefull to adapt a processDescription before run the shadow process.
     * 
     * @param XmlFileConnection
     * 
     * @return ProcessDescription
     */
    public static ProcessDescription getProcessDescription(final XmlFileConnection connection) {
        ProcessDescription processDescription = new ProcessDescription();
        processDescription.setFilepath(TalendTextUtils.addQuotes(PathUtils.getPortablePath(connection.getXmlFilePath())));
        processDescription.setLoopQuery(TalendTextUtils.addQuotes(((XmlXPathLoopDescriptor) connection.getSchema().get(0))
                .getAbsoluteXPathQuery()));
        if (((XmlXPathLoopDescriptor) connection.getSchema().get(0)).getLimitBoucle() != null
                && !("").equals(((XmlXPathLoopDescriptor) connection.getSchema().get(0)).getLimitBoucle()) //$NON-NLS-1$
                && (((XmlXPathLoopDescriptor) connection.getSchema().get(0)).getLimitBoucle().intValue()) != 0) {
            processDescription.setLoopLimit(((XmlXPathLoopDescriptor) connection.getSchema().get(0)).getLimitBoucle());
        }

        List<Map<String, String>> mapping = new ArrayList<Map<String, String>>();

        List<SchemaTarget> schemaTargets = ((XmlXPathLoopDescriptor) connection.getSchema().get(0)).getSchemaTargets();

        if (schemaTargets != null && !schemaTargets.isEmpty()) {
            Iterator<SchemaTarget> iterate = schemaTargets.iterator();
            while (iterate.hasNext()) {
                SchemaTarget schemaTarget = iterate.next();
                Map<String, String> lineMapping = new HashMap<String, String>();
                lineMapping.put("QUERY", TalendTextUtils.addQuotes(schemaTarget.getRelativeXPathQuery())); //$NON-NLS-1$ 
                mapping.add(lineMapping);
            }
        }
        processDescription.setMapping(mapping);
        if (connection.getEncoding() != null && !("").equals(connection.getEncoding())) { //$NON-NLS-1$
            processDescription.setEncoding(TalendTextUtils.addQuotes(connection.getEncoding()));
        } else {
            processDescription.setEncoding(TalendTextUtils.addQuotes("UTF-8")); //$NON-NLS-1$
        }

        return processDescription;
    }

    /**
     * Create a ProcessDescription and set it width the value of LdifFileConnection.
     * 
     * This method is usefull to adapt a processDescription before run the shadow process.
     * 
     * @param LdifFileConnection
     * 
     * @return ProcessDescription
     */
    public static ProcessDescription getProcessDescription(final LdifFileConnection connection) {
        ProcessDescription processDescription = new ProcessDescription();
        processDescription.setFilepath(TalendTextUtils.addQuotes(PathUtils.getPortablePath(connection.getFilePath())));
        List<IMetadataTable> tableSchema = new ArrayList<IMetadataTable>();

        IMetadataTable table = new MetadataTable();

        List<IMetadataColumn> schema = new ArrayList<IMetadataColumn>();

        if (connection.getValue() != null && !connection.getValue().isEmpty()) {
            Iterator<String> iterate = connection.getValue().iterator();

            while (iterate.hasNext()) {

                IMetadataColumn iMetadataColumn = new MetadataColumn();
                iMetadataColumn.setLabel(iterate.next());
                iMetadataColumn.setKey(false);
                iMetadataColumn.setLength(0);
                iMetadataColumn.setNullable(false);
                iMetadataColumn.setType("String"); //$NON-NLS-1$
                iMetadataColumn.setTalendType("id_String"); //$NON-NLS-1$

                schema.add(iMetadataColumn);
            }

        } else {

            IMetadataColumn iMetadataDn = new MetadataColumn();
            iMetadataDn.setLabel("dn"); //$NON-NLS-1$
            iMetadataDn.setKey(false);
            iMetadataDn.setLength(0);
            iMetadataDn.setNullable(false);
            iMetadataDn.setType("String"); //$NON-NLS-1$
            iMetadataDn.setTalendType("id_String"); //$NON-NLS-1$

            schema.add(iMetadataDn);
        }

        table.setTableName("tFileInputLDIF"); //$NON-NLS-1$
        table.setListColumns(schema);
        tableSchema.add(table);
        processDescription.setSchema(tableSchema);

        // PTODO cantoine : create encoding field for LDIF fileConnection
        processDescription.setEncoding(TalendTextUtils.addQuotes("UTF-8")); //$NON-NLS-1$

        return processDescription;
    }

    /**
     * parse a file describe by a fileConnection in XmlArray. Simple method to run the shadow process from the
     * fileConnection.
     * 
     * @param fileConnection
     * @return xmlArray
     * @throws CoreException
     */
    public static CsvArray getCsvArray(final FileConnection fileConnection, String type) throws CoreException {
        return getCsvArray(getProcessDescription(fileConnection), type);
    }

    /**
     * parse a file describe by a processDescription in XmlArray.
     * 
     * @param processDescription
     * @return xmlArray
     */
    public static CsvArray getCsvArray(final ProcessDescription processDescription, String type) throws CoreException {

        CsvArray csvArray = null;

        IPreview preview = createPreview();

        if (preview != null) {
            csvArray = preview.preview(processDescription, type);
        }
        return csvArray;
    }

    /**
     * DOC amaumont Comment method "createPreview".
     * 
     * @param configurationElements
     * @return
     * @throws CoreException
     */
    private static IPreview createPreview() throws CoreException {
        IExtensionRegistry registry = Platform.getExtensionRegistry();

        // use the org.talend.repository.filepreview_provider
        IConfigurationElement[] configurationElements = registry
                .getConfigurationElementsFor("org.talend.core.filepreview_provider"); //$NON-NLS-1$

        IPreview preview = null;
        if (configurationElements.length > 0) {
            preview = (IPreview) configurationElements[0].createExecutableExtension("class"); //$NON-NLS-1$
        }
        if (preview == null) {
            log.error(Messages.getString("ShadowProcessHelper.logError.previewIsNull01") //$NON-NLS-1$
                    + Messages.getString("ShadowProcessHelper.logError.previewIsNull02")); //$NON-NLS-1$
        }

        return preview;
    }

    public static AsynchronousPreviewHandler<CsvArray> createPreviewHandler() throws CoreException {
        IPreview preview = createPreview();
        return new AsynchronousPreviewHandler<CsvArray>(preview);
    }

    /**
     * Administrator Comment method "getProcessDescription".
     * 
     * @param connection
     * @return
     */
    public static ProcessDescription getProcessDescription(LDAPSchemaConnection connection) {
        ProcessDescription processDescription = new ProcessDescription();
        List<IMetadataTable> tableSchema = new ArrayList<IMetadataTable>();

        IMetadataTable table = new MetadataTable();

        List<IMetadataColumn> schema = new ArrayList<IMetadataColumn>();

        if (connection.getValue() != null && !connection.getValue().isEmpty()) {
            Iterator<String> iterate = connection.getValue().iterator();

            while (iterate.hasNext()) {
                IMetadataColumn iMetadataColumn = new MetadataColumn();
                iMetadataColumn.setLabel(iterate.next());
                iMetadataColumn.setKey(false);
                iMetadataColumn.setLength(0);
                iMetadataColumn.setNullable(false);
                iMetadataColumn.setType("String"); //$NON-NLS-1$
                iMetadataColumn.setTalendType("id_String"); //$NON-NLS-1$

                schema.add(iMetadataColumn);
            }
        } else {

            IMetadataColumn iMetadataDn = new MetadataColumn();
            iMetadataDn.setLabel("dn"); //$NON-NLS-1$
            iMetadataDn.setKey(false);
            iMetadataDn.setLength(0);
            iMetadataDn.setNullable(false);
            iMetadataDn.setType("String"); //$NON-NLS-1$
            iMetadataDn.setTalendType("id_String"); //$NON-NLS-1$

            schema.add(iMetadataDn);
        }

        table.setTableName("tLDAPInput"); //$NON-NLS-1$
        table.setListColumns(schema);
        tableSchema.add(table);
        processDescription.setSchema(tableSchema);

        LDAPSchemaBean bean = new LDAPSchemaBean();
        // TODO: added properties here...
        bean.setHost(TalendTextUtils.addQuotes(connection.getHost()));
        bean.setPort(connection.getPort());// int
        bean.setEncryMethod(TalendTextUtils.addQuotes(connection.getEncryptionMethodName()));
        bean.setAuthen(connection.isUseAuthen());
        bean.setAuthMethod(TalendTextUtils.addQuotes(connection.getProtocol()));
        bean.setUser(TalendTextUtils.addQuotes(connection.getBindPrincipal()));
        bean.setPasswd(TalendTextUtils.addQuotes(connection.getBindPassword()));

        bean.setBaseDN(TalendTextUtils.addQuotes(connection.getSelectedDN()));

        bean.setReferrals(connection.getReferrals());
        bean.setAliasDereferenring(connection.getAliases());

        bean.setMultiValueSeparator(TalendTextUtils.addQuotes(","));

        bean.setFilter(TalendTextUtils.addQuotes(connection.getFilter()));

        bean.setCountLimit(connection.getCountLimit()); // int
        bean.setTimeOutLimit(connection.getTimeOutLimit());// int

        processDescription.setLdapSchemaBean(bean);

        processDescription.setEncoding(TalendTextUtils.addQuotes("UTF-8")); //$NON-NLS-1$

        return processDescription;
    }

    public static ProcessDescription getProcessDescription(WSDLSchemaConnection connection) {
        ProcessDescription processDescription = new ProcessDescription();
        List<IMetadataTable> tableSchema = new ArrayList<IMetadataTable>();

        IMetadataTable table = new MetadataTable();

        List<IMetadataColumn> schema = new ArrayList<IMetadataColumn>();

        if (connection.getValue() != null && !connection.getValue().isEmpty()) {
            Iterator<String> iterate = connection.getValue().iterator();

            while (iterate.hasNext()) {
                IMetadataColumn iMetadataColumn = new MetadataColumn();
                iMetadataColumn.setLabel(iterate.next());
                iMetadataColumn.setKey(false);
                iMetadataColumn.setLength(0);
                iMetadataColumn.setNullable(false);
                iMetadataColumn.setType("String"); //$NON-NLS-1$
                iMetadataColumn.setTalendType("id_String"); //$NON-NLS-1$

                schema.add(iMetadataColumn);
            }
        } else {

            IMetadataColumn iMetadataDn = new MetadataColumn();
            iMetadataDn.setLabel(connection.getMethodName()); //$NON-NLS-1$
            iMetadataDn.setKey(false);
            iMetadataDn.setLength(0);
            iMetadataDn.setNullable(false);
            iMetadataDn.setType("String"); //$NON-NLS-1$
            iMetadataDn.setTalendType("id_String"); //$NON-NLS-1$

            schema.add(iMetadataDn);
        }

        table.setTableName("tWebServiceInput"); //$NON-NLS-1$
        table.setListColumns(schema);
        tableSchema.add(table);
        processDescription.setSchema(tableSchema);
        WSDLSchemaBean bean = new WSDLSchemaBean();
        // TODO: added properties here...
        bean.setWslUrl(TalendTextUtils.addQuotes(connection.getWSDL()));
        bean.setEndpointURI(TalendTextUtils.addQuotes(connection.getEndpointURI()));
        bean.setMethod(TalendTextUtils.addQuotes(connection.getMethodName()));
        bean.setNeedAuth(connection.isNeedAuth());
        bean.setUserName(TalendTextUtils.addQuotes(connection.getUserName()));
        bean.setPassword(TalendTextUtils.addQuotes(connection.getPassword()));
        bean.setParameters(connection.getParameters());
        bean.setUseProxy(connection.isUseProxy());
        bean.setProxyHost(TalendTextUtils.addQuotes(connection.getProxyHost()));
        bean.setProxyPort(TalendTextUtils.addQuotes(connection.getProxyPort()));
        bean.setProxyUser(TalendTextUtils.addQuotes(connection.getProxyUser()));
        bean.setProxyPassword(TalendTextUtils.addQuotes(connection.getProxyPassword()));
        processDescription.setWsdlSchemaBean(bean);
        if (connection.getEncoding() != null && !connection.getEncoding().equals("")) {
            processDescription.setEncoding(connection.getEncoding()); //$NON-NLS-1$
        } else {
            processDescription.setEncoding(TalendTextUtils.addQuotes("UTF-8")); //$NON-NLS-1$
        }

        return processDescription;
    }
}
