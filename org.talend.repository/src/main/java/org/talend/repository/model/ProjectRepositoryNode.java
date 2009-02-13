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
package org.talend.repository.model;

import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.preference.IPreferenceStore;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.exception.RuntimeExceptionHandler;
import org.talend.commons.utils.data.container.Container;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.PluginChecker;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.language.LanguageManager;
import org.talend.core.model.genhtml.IHTMLDocConstants;
import org.talend.core.model.metadata.MetadataTable;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.metadata.builder.connection.DelimitedFileConnection;
import org.talend.core.model.metadata.builder.connection.EbcdicConnection;
import org.talend.core.model.metadata.builder.connection.FileExcelConnection;
import org.talend.core.model.metadata.builder.connection.GenericSchemaConnection;
import org.talend.core.model.metadata.builder.connection.LDAPSchemaConnection;
import org.talend.core.model.metadata.builder.connection.LdifFileConnection;
import org.talend.core.model.metadata.builder.connection.PositionalFileConnection;
import org.talend.core.model.metadata.builder.connection.QueriesConnection;
import org.talend.core.model.metadata.builder.connection.Query;
import org.talend.core.model.metadata.builder.connection.RegexpFileConnection;
import org.talend.core.model.metadata.builder.connection.SAPConnection;
import org.talend.core.model.metadata.builder.connection.SAPFunctionUnit;
import org.talend.core.model.metadata.builder.connection.SalesforceSchemaConnection;
import org.talend.core.model.metadata.builder.connection.SubItemHelper;
import org.talend.core.model.metadata.builder.connection.TableHelper;
import org.talend.core.model.metadata.builder.connection.WSDLSchemaConnection;
import org.talend.core.model.metadata.builder.connection.XmlFileConnection;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.DatabaseConnectionItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.JobDocumentationItem;
import org.talend.core.model.properties.JobletDocumentationItem;
import org.talend.core.model.properties.Project;
import org.talend.core.model.properties.ProjectReference;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.Folder;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.model.repository.IRepositoryPrefConstants;
import org.talend.core.model.repository.RepositoryManager;
import org.talend.core.ui.ICDCProviderService;
import org.talend.core.ui.branding.IBrandingService;
import org.talend.core.ui.images.ECoreImage;
import org.talend.repository.ProjectManager;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.nodes.IProjectRepositoryNode;

/**
 * DOC nrousseau class global comment. Detailled comment
 */
public class ProjectRepositoryNode extends RepositoryNode implements IProjectRepositoryNode {

    private RepositoryNode businessProcessNode, recBinNode, codeNode, routineNode, snippetsNode, processNode, contextNode,
            docNode, metadataConNode, sqlPatternNode, metadataFileNode, metadataFilePositionalNode, metadataFileRegexpNode,
            metadataFileXmlNode, metadataFileLdifNode, metadataGenericSchemaNode, metadataLDAPSchemaNode, metadataWSDLSchemaNode,
            metadataFileExcelNode, metadataSalesforceSchemaNode, metadataSAPConnectionNode, metadataEbcdicConnectionNode;

    private RepositoryNode jobletNode;

    private RepositoryNode metadataNode;

    private RepositoryNode refProject;

    private boolean mergeRefProject;

    private final IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();

    private org.talend.core.model.general.Project project;

    /**
     * DOC nrousseau ProjectRepositoryNode constructor comment.
     * 
     * @param object
     * @param parent
     * @param type
     */
    public ProjectRepositoryNode(org.talend.core.model.general.Project project, IRepositoryObject object, RepositoryNode parent,
            RepositoryNode root, ENodeType type) {
        super(object, parent, type);
        // for referenced project
        this.project = project;
        setRoot(this);
    }

    public ProjectRepositoryNode(ProjectRepositoryNode projectNode) {
        this(projectNode.getProject(), projectNode.getObject(), projectNode.getParent(), (RepositoryNode) projectNode.getRoot(),
                projectNode.getType());

        this.setProperties(EProperties.LABEL, projectNode.getProperties(EProperties.LABEL));
        this.setProperties(EProperties.CONTENT_TYPE, projectNode.getProperties(EProperties.CONTENT_TYPE));
    }

    public ProjectRepositoryNode(IRepositoryObject object, RepositoryNode parent, ENodeType type) {
        super(object, parent, type);
        // base project
        this.project = ProjectManager.getInstance().getCurrentProject();
        setRoot(this);
    }

    private void hideHiddenNodes() {
        IBrandingService service = (IBrandingService) GlobalServiceRegister.getDefault().getService(IBrandingService.class);
        List<RepositoryNode> hiddens = service.getBrandingConfiguration().getHiddenRepositoryCategory(this);
        // this.getChildren().removeAll(hiddens);
        for (RepositoryNode node : hiddens) {
            removeNode(this, node);
        }
    }

    private void removeNode(RepositoryNode container, RepositoryNode node) {
        List<RepositoryNode> nodes = container.getChildren();

        if (nodes.contains(node)) {
            nodes.remove(node);
        } else {
            for (RepositoryNode n : nodes) {
                removeNode(n, node);
            }
        }
    }

    public void initialize() {
        List<RepositoryNode> nodes = getChildren();

        // 0. Recycle bin
        recBinNode = new BinRepositoryNode(this);
        nodes.add(recBinNode);

        // 1. Business process
        businessProcessNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        businessProcessNode.setProperties(EProperties.LABEL, ERepositoryObjectType.BUSINESS_PROCESS);
        businessProcessNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.BUSINESS_PROCESS);
        nodes.add(businessProcessNode);

        // 2. Process
        processNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        processNode.setProperties(EProperties.LABEL, ERepositoryObjectType.PROCESS);
        processNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.PROCESS);
        nodes.add(processNode);

        if (PluginChecker.isJobLetPluginLoaded()) {
            // 2.1 Joblet
            jobletNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
            jobletNode.setProperties(EProperties.LABEL, ERepositoryObjectType.JOBLET);
            jobletNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.JOBLET);
            nodes.add(jobletNode);
        }

        // 3. Context
        contextNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        contextNode.setProperties(EProperties.LABEL, ERepositoryObjectType.CONTEXT);
        contextNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.CONTEXT);
        nodes.add(contextNode);

        // 4. Code
        codeNode = new StableRepositoryNode(this,
                Messages.getString("RepositoryContentProvider.repositoryLabel.code"), ECoreImage.CODE_ICON); //$NON-NLS-1$
        codeNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.ROUTINES);
        nodes.add(codeNode);

        // 4.1. Routines
        routineNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        routineNode.setProperties(EProperties.LABEL, ERepositoryObjectType.ROUTINES);
        routineNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.ROUTINES);
        codeNode.getChildren().add(routineNode);

        // 4.2. Snippets
        // if (PluginChecker.isSnippetsPluginLoaded()) {
        // snippetsNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        // snippetsNode.setProperties(EProperties.LABEL, ERepositoryObjectType.SNIPPETS);
        // snippetsNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.SNIPPETS);
        // codeNode.getChildren().add(snippetsNode);
        // }

        // 5. Sql patterns
        sqlPatternNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        sqlPatternNode.setProperties(EProperties.LABEL, ERepositoryObjectType.SQLPATTERNS);
        sqlPatternNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.SQLPATTERNS);
        nodes.add(sqlPatternNode);

        // 6. Documentation
        docNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        docNode.setProperties(EProperties.LABEL, ERepositoryObjectType.DOCUMENTATION);
        docNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.DOCUMENTATION);
        nodes.add(docNode);

        // 7. Metadata
        metadataNode = new RepositoryNode(null, this, ENodeType.STABLE_SYSTEM_FOLDER);
        metadataNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA);
        metadataNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA);
        nodes.add(metadataNode);

        // 7.1. Metadata connections
        metadataConNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        metadataConNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_CONNECTIONS);
        metadataConNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_CONNECTIONS);
        metadataNode.getChildren().add(metadataConNode);

        // 7.2. Metadata file delimited
        metadataFileNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        metadataFileNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_FILE_DELIMITED);
        metadataFileNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_FILE_DELIMITED);
        metadataNode.getChildren().add(metadataFileNode);

        // 7.3. Metadata file positional
        metadataFilePositionalNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        metadataFilePositionalNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_FILE_POSITIONAL);
        metadataFilePositionalNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_FILE_POSITIONAL);
        metadataNode.getChildren().add(metadataFilePositionalNode);

        // 7.4. Metadata file regexp
        metadataFileRegexpNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        metadataFileRegexpNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_FILE_REGEXP);
        metadataFileRegexpNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_FILE_REGEXP);
        metadataNode.getChildren().add(metadataFileRegexpNode);

        // 7.5. Metadata file xml
        metadataFileXmlNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        metadataFileXmlNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_FILE_XML);
        metadataFileXmlNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_FILE_XML);
        metadataNode.getChildren().add(metadataFileXmlNode);

        // 7.6. Metadata file ldif
        metadataFileLdifNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        metadataFileLdifNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_FILE_LDIF);
        metadataFileLdifNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_FILE_LDIF);
        metadataNode.getChildren().add(metadataFileLdifNode);

        // 7.7. Metadata file Excel
        metadataFileExcelNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        metadataFileExcelNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_FILE_EXCEL);
        metadataFileExcelNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_FILE_EXCEL);
        metadataNode.getChildren().add(metadataFileExcelNode);

        // 7.8. LDAP schemas
        metadataLDAPSchemaNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        metadataLDAPSchemaNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_LDAP_SCHEMA);
        metadataLDAPSchemaNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_LDAP_SCHEMA);
        metadataNode.getChildren().add(metadataLDAPSchemaNode);

        // 7.9. Generic schemas
        metadataGenericSchemaNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        metadataGenericSchemaNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_GENERIC_SCHEMA);
        metadataGenericSchemaNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_GENERIC_SCHEMA);
        metadataNode.getChildren().add(metadataGenericSchemaNode);
        // 7.10 WSDL
        metadataWSDLSchemaNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
        metadataWSDLSchemaNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_WSDL_SCHEMA);
        metadataWSDLSchemaNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_WSDL_SCHEMA);
        metadataNode.getChildren().add(metadataWSDLSchemaNode);

        // 7.11 Salesforce

        ECodeLanguage codeLanguage = LanguageManager.getCurrentLanguage();
        if (codeLanguage != ECodeLanguage.PERL) {
            metadataSalesforceSchemaNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
            metadataSalesforceSchemaNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_SALESFORCE_SCHEMA);
            metadataSalesforceSchemaNode
                    .setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_SALESFORCE_SCHEMA);
            metadataNode.getChildren().add(metadataSalesforceSchemaNode);
        }

        // 7.12 SAP
        if (PluginChecker.isTIS() && LanguageManager.getCurrentLanguage() == ECodeLanguage.JAVA) {
            metadataSAPConnectionNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
            metadataSAPConnectionNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_SAPCONNECTIONS);
            metadataSAPConnectionNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_SAPCONNECTIONS);
            metadataNode.getChildren().add(metadataSAPConnectionNode);
        }
        // 7.13 EBCDIC
        if (PluginChecker.isEBCDICPluginLoaded()) {
            metadataEbcdicConnectionNode = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
            metadataEbcdicConnectionNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_FILE_EBCDIC);
            metadataEbcdicConnectionNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_FILE_EBCDIC);
            metadataNode.getChildren().add(metadataEbcdicConnectionNode);
        }

        // Reference Projects
        if (PluginChecker.isTIS() && getParent() == null && !getMergeRefProject()
                && project.getEmfProject().getReferencedProjects().size() > 0) {
            refProject = new RepositoryNode(null, this, ENodeType.SYSTEM_FOLDER);
            refProject.setProperties(EProperties.LABEL, ERepositoryObjectType.REFERENCED_PROJECTS);
            refProject.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.REFERENCED_PROJECTS);
            nodes.add(refProject);
        }
        // hide hidden nodes;
        hideHiddenNodes();
    }

    /**
     * DOC nrousseau Comment method "initializeChildren".
     * 
     * @param parent
     */
    public void initializeChildren(Object parent) {
        initializeChildren(project, parent);
        if (PluginChecker.isTIS() && getMergeRefProject()) {

            for (Object o : project.getEmfProject().getReferencedProjects()) {
                if (o instanceof ProjectReference) {
                    org.talend.core.model.general.Project p = new org.talend.core.model.general.Project(((ProjectReference) o)
                            .getReferencedProject());
                    initializeChildren(p, parent);
                }
            }
        }
    }

    public void initializeChildren(org.talend.core.model.general.Project newProject, Object parent) {
        try {
            if (parent == businessProcessNode) {
                convert(factory.getBusinessProcess(newProject), businessProcessNode, ERepositoryObjectType.BUSINESS_PROCESS,
                        recBinNode);
            } else if (parent == processNode) {
                convert(factory.getProcess(newProject), processNode, ERepositoryObjectType.PROCESS, recBinNode);
            } else if (parent == jobletNode) {
                convert(factory.getJoblets(newProject), jobletNode, ERepositoryObjectType.JOBLET, recBinNode);
            } else if (parent == routineNode) {
                convert(factory.getRoutine(newProject), routineNode, ERepositoryObjectType.ROUTINES, recBinNode);
            } else if (parent == snippetsNode) {
                convert(factory.getSnippets(newProject), snippetsNode, ERepositoryObjectType.SNIPPETS, recBinNode);
            } else if (parent == contextNode) {
                convert(factory.getContext(newProject), contextNode, ERepositoryObjectType.CONTEXT, recBinNode);
            } else if (parent == docNode) {
                // convertDocumentation(factory.getDocumentation(), docNode, ERepositoryObjectType.DOCUMENTATION,
                // recBinNode);

                convert(factory.getDocumentation(newProject), docNode, ERepositoryObjectType.DOCUMENTATION, recBinNode);
            } else if (parent == metadataConNode) {
                convert(factory.getMetadataConnection(newProject), metadataConNode, ERepositoryObjectType.METADATA_CONNECTIONS,
                        recBinNode);
            } else if (parent == metadataSAPConnectionNode) {
                convert(factory.getMetadataSAPConnection(newProject), metadataSAPConnectionNode,
                        ERepositoryObjectType.METADATA_SAPCONNECTIONS, recBinNode);
            } else if (parent == metadataEbcdicConnectionNode) {
                convert(factory.getMetadataEBCDIC(newProject), metadataEbcdicConnectionNode,
                        ERepositoryObjectType.METADATA_FILE_EBCDIC, recBinNode);
            } else if (parent == sqlPatternNode) {
                convert(factory.getMetadataSQLPattern(newProject), sqlPatternNode, ERepositoryObjectType.SQLPATTERNS, recBinNode);
            } else if (parent == metadataFileNode) {
                convert(factory.getMetadataFileDelimited(newProject), metadataFileNode,
                        ERepositoryObjectType.METADATA_FILE_DELIMITED, recBinNode);
            } else if (parent == metadataFilePositionalNode) {
                convert(factory.getMetadataFilePositional(newProject), metadataFilePositionalNode,
                        ERepositoryObjectType.METADATA_FILE_POSITIONAL, recBinNode);
            } else if (parent == metadataFileRegexpNode) {
                convert(factory.getMetadataFileRegexp(newProject), metadataFileRegexpNode,
                        ERepositoryObjectType.METADATA_FILE_REGEXP, recBinNode);
            } else if (parent == metadataFileXmlNode) {
                convert(factory.getMetadataFileXml(newProject), metadataFileXmlNode, ERepositoryObjectType.METADATA_FILE_XML,
                        recBinNode);
            } else if (parent == metadataFileLdifNode) {
                convert(factory.getMetadataFileLdif(newProject), metadataFileLdifNode, ERepositoryObjectType.METADATA_FILE_LDIF,
                        recBinNode);
            } else if (parent == metadataFileExcelNode) {
                convert(factory.getMetadataFileExcel(newProject), metadataFileExcelNode,
                        ERepositoryObjectType.METADATA_FILE_EXCEL, recBinNode);
            } else if (parent == metadataSalesforceSchemaNode) {
                convert(factory.getMetadataSalesforceSchema(newProject), metadataSalesforceSchemaNode,
                        ERepositoryObjectType.METADATA_SALESFORCE_SCHEMA, recBinNode);
            } else if (parent == metadataLDAPSchemaNode) {
                convert(factory.getMetadataLDAPSchema(newProject), metadataLDAPSchemaNode,
                        ERepositoryObjectType.METADATA_LDAP_SCHEMA, recBinNode);
            } else if (parent == metadataGenericSchemaNode) {
                convert(factory.getMetadataGenericSchema(newProject), metadataGenericSchemaNode,
                        ERepositoryObjectType.METADATA_GENERIC_SCHEMA, recBinNode);
            } else if (parent == metadataWSDLSchemaNode) {
                convert(factory.getMetadataWSDLSchema(newProject), metadataWSDLSchemaNode,
                        ERepositoryObjectType.METADATA_WSDL_SCHEMA, recBinNode);
            } else if (parent == refProject) {
                if (!getMergeRefProject()) {
                    handleReferenced(refProject);
                }
            } else if (parent == recBinNode) {
                List<IRepositoryObject> objects = factory.getRecycleBinItems(newProject);
                for (IRepositoryObject object : objects) {
                    if (!isGeneratedJobItem(object.getProperty().getItem())) {
                        RepositoryNode node = new RepositoryNode(object, recBinNode, ENodeType.REPOSITORY_ELEMENT);
                        node.setProperties(EProperties.CONTENT_TYPE, object.getType());
                        node.setProperties(EProperties.LABEL, object.getLabel());
                        recBinNode.getChildren().add(node);
                        node.setParent(recBinNode);
                    }
                }
            }
        } catch (PersistenceException e) {
            RuntimeExceptionHandler.process(e);
        }
    }

    /**
     * 
     * ggu Comment method "isGeneratedJobItem".
     * 
     * feature 4393
     */
    private boolean isGeneratedJobItem(Item item) {
        if (item != null) {
            if (item instanceof JobDocumentationItem || item instanceof JobletDocumentationItem) {
                return true;
            }
        }
        return false;
    }

    private RepositoryNode getDocumentationNode(ERepositoryObjectType type) {
        if (getMergeRefProject()) {
            RepositoryNode docNode = getRootRepositoryNode(ERepositoryObjectType.DOCUMENTATION);
            for (RepositoryNode child : docNode.getChildren()) {

                if (type == child.getContentType()) {
                    return child;
                }

                for (RepositoryNode c : child.getChildren()) {
                    if (type == c.getContentType()) {
                        return c;
                    }
                }
            }
        }
        return null;
    }

    /**
     * ftang Comment method "convertDocumentation".
     * 
     * @param fromModel
     * @param parent
     * @param type
     * @param recBinNode
     */
    private void convertDocumentation(Container fromModel, RepositoryNode parent, ERepositoryObjectType type,
            RepositoryNode recBinNode) {
        RepositoryNode generatedFolder = getDocumentationNode(ERepositoryObjectType.GENERATED);
        if (generatedFolder == null) {
            generatedFolder = new StableRepositoryNode(parent, ERepositoryObjectType.GENERATED.toString(),
                    ECoreImage.FOLDER_CLOSE_ICON);
            parent.getChildren().add(generatedFolder);
        }
        RepositoryNode jobsFolder = getDocumentationNode(ERepositoryObjectType.JOBS);
        if (jobsFolder == null) {
            jobsFolder = new StableRepositoryNode(generatedFolder, ERepositoryObjectType.JOBS.toString(),
                    ECoreImage.FOLDER_CLOSE_ICON);
            generatedFolder.getChildren().add(jobsFolder);
        }
        RepositoryNode jobletsFolder = getDocumentationNode(ERepositoryObjectType.JOBLETS);

        if (jobletsFolder == null) {
            jobletsFolder = new StableRepositoryNode(generatedFolder, ERepositoryObjectType.JOBLETS.toString(),
                    ECoreImage.FOLDER_CLOSE_ICON);
            generatedFolder.getChildren().add(jobletsFolder);
        }

        jobsFolder.setProperties(EProperties.LABEL, ERepositoryObjectType.JOBS.toString());
        jobsFolder.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.JOBS);

        jobletsFolder.setProperties(EProperties.LABEL, ERepositoryObjectType.JOBLETS.toString());
        jobletsFolder.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.JOBLETS);

        Container generatedContainer = null;
        for (Object object : fromModel.getSubContainer()) {
            if (((Container) object).getLabel().equalsIgnoreCase(ERepositoryObjectType.GENERATED.toString())) {
                generatedContainer = (Container) object;
                break;
            }
        }

        Container jobsNode = null;
        Container jobletsNode = null;
        for (Object object : generatedContainer.getSubContainer()) {
            if (((Container) object).getLabel().equalsIgnoreCase(ERepositoryObjectType.JOBS.toString())) {
                jobsNode = (Container) object;
                break;
            }
        }

        for (Object object : generatedContainer.getSubContainer()) {
            if (((Container) object).getLabel().equalsIgnoreCase(ERepositoryObjectType.JOBLETS.toString())) {
                jobletsNode = (Container) object;
                break;
            }
        }

        // get the files under generated/nodes.
        if (jobsNode != null) {
            convert(jobsNode, jobsFolder, ERepositoryObjectType.JOB_DOC, recBinNode);
        }

        if (jobletsNode != null) {
            convert(jobletsNode, jobletsFolder, ERepositoryObjectType.JOBLET_DOC, recBinNode);
        }

        generatedFolder.setProperties(EProperties.LABEL, ERepositoryObjectType.GENERATED.toString());
        generatedFolder.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.GENERATED); // ERepositoryObjectType
        // .FOLDER);

    }

    // /**
    // * ftang Comment method "createSubFolder".
    // *
    // * @param folder
    // * @param fromModel
    // */
    // private void createSubFolder(RepositoryNode folder, Container fromModel) {
    //
    // for (Object object : fromModel.getSubContainer()) {
    // Container container = (Container) object;
    // // Folder oFolder = new Folder((Property) container.getProperty(), ERepositoryObjectType.JOBS);
    // RepositoryNode subFolder = new StableRepositoryNode(folder, container.getLabel(), ECoreImage.FOLDER_CLOSE_ICON);
    // subFolder.setProperties(EProperties.LABEL, container.getLabel());
    // subFolder.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.JOBS); // ERepositoryObjectType.FOLDER);
    // folder.getChildren().add(subFolder);
    // if (container.getSubContainer() != null && container.getSubContainer().size() > 0) {
    // createSubFolder(subFolder, container);
    // }
    // }
    //
    // }
    private RepositoryNode getSQLPatternNode(String parentLabel, String label) {
        if (getMergeRefProject()) {
            List<RepositoryNode> sqlChildren = getRootRepositoryNode(ERepositoryObjectType.SQLPATTERNS).getChildren();
            // List<RepositoryNode> sqlChildren = parent.getChildren();

            for (RepositoryNode sqlChild : sqlChildren) {
                if (label.equalsIgnoreCase(sqlChild.toString())) {
                    return sqlChild;
                }
                for (RepositoryNode userDefined : sqlChild.getChildren()) {
                    if (label.equalsIgnoreCase(userDefined.getProperties(EProperties.LABEL).toString())) {
                        if (sqlChild.toString().equalsIgnoreCase(parentLabel))
                            return userDefined;
                    }

                }
            }
        }
        return null;
    }

    private void convert(Container fromModel, RepositoryNode parent, ERepositoryObjectType type, RepositoryNode recBinNode) {

        if (parent == null) {
            return;
        }

        String label = null;

        if (getMergeRefProject()) {
            for (Object obj : fromModel.getSubContainer()) {
                Container container = (Container) obj;
                Folder oFolder = new Folder((Property) container.getProperty(), type);
                if (oFolder.getProperty() == null) {
                    continue;
                }

                RepositoryNode folder = null;

                label = container.getLabel();

                boolean isJobDocRootFolder = ((label.indexOf("_") != -1) && (label.indexOf(".") != -1)); //$NON-NLS-1$ //$NON-NLS-2$
                boolean isPicFolderName = label.equals(IHTMLDocConstants.PIC_FOLDER_NAME);

                // Do not show job documentation root folder and Foder "pictures" on the repository view.
                if (isJobDocRootFolder || isPicFolderName) {
                    continue;
                }
                // system
                if (label.equals(RepositoryConstants.SYSTEM_DIRECTORY)) {
                    List list = parent.getChildren();
                    boolean existSystemFolder = false;
                    for (RepositoryNode node : parent.getChildren()) {
                        if ("system".equalsIgnoreCase(node.getLabel())) { //$NON-NLS-1$
                            existSystemFolder = true;
                            break;
                        }
                    }
                    if (!existSystemFolder) {
                        folder = new StableRepositoryNode(parent, Messages
                                .getString("RepositoryContentProvider.repositoryLabel.system"), ECoreImage.FOLDER_CLOSE_ICON); //$NON-NLS-1$
                        parent.getChildren().add(folder);
                    } else {
                        continue;
                    }

                } else if (label.equalsIgnoreCase(ERepositoryObjectType.GENERATED.toString())) {
                    convertDocumentation(fromModel, parent, type, recBinNode);
                    continue;
                } else {
                    if (label.equalsIgnoreCase("userDefined")) { //$NON-NLS-1$
                        label.toCharArray();
                    }
                    String a = parent.getProperties(EProperties.LABEL).toString();
                    folder = getSQLPatternNode(a, label);
                    if (folder == null) {
                        folder = new RepositoryNode(oFolder, parent, ENodeType.SIMPLE_FOLDER);
                        parent.getChildren().add(folder);
                    }

                }
                folder.setProperties(EProperties.LABEL, label);
                folder.setProperties(EProperties.CONTENT_TYPE, type); // ERepositoryObjectType.FOLDER);
                convert(container, folder, type, recBinNode);

            }

        } else {
            for (Object obj : fromModel.getSubContainer()) {
                Container container = (Container) obj;
                Folder oFolder = new Folder((Property) container.getProperty(), type);
                if (oFolder.getProperty() == null) {
                    continue;
                }

                RepositoryNode folder = null;

                label = container.getLabel();

                boolean isJobDocRootFolder = ((label.indexOf("_") != -1) && (label.indexOf(".") != -1)); //$NON-NLS-1$ //$NON-NLS-2$
                boolean isPicFolderName = label.equals(IHTMLDocConstants.PIC_FOLDER_NAME);

                // Do not show job documentation root folder and Foder "pictures" on the repository view.
                if (isJobDocRootFolder || isPicFolderName) {
                    continue;
                }

                if (label.equals(RepositoryConstants.SYSTEM_DIRECTORY)) {
                    // system
                    folder = new StableRepositoryNode(parent, Messages
                            .getString("RepositoryContentProvider.repositoryLabel.system"), ECoreImage.FOLDER_CLOSE_ICON); //$NON-NLS-1$

                } else if (label.equalsIgnoreCase(ERepositoryObjectType.GENERATED.toString())) {
                    convertDocumentation(fromModel, parent, type, recBinNode);
                    continue;
                } else {
                    folder = new RepositoryNode(oFolder, parent, ENodeType.SIMPLE_FOLDER);
                }
                folder.setProperties(EProperties.LABEL, label);
                folder.setProperties(EProperties.CONTENT_TYPE, type); // ERepositoryObjectType.FOLDER);
                parent.getChildren().add(folder);
                convert(container, folder, type, recBinNode);

            }

        }
        // not folder
        for (Object obj : fromModel.getMembers()) {
            IRepositoryObject repositoryObject = (IRepositoryObject) obj;
            addNode(parent, type, recBinNode, repositoryObject);
        }
    }

    private void handleReferenced(RepositoryNode parent) {
        if (parent.getType().equals(ENodeType.SYSTEM_FOLDER)) {
            for (Iterator iter = factory.getReferencedProjects().iterator(); iter.hasNext();) {
                Project emfProject = (Project) iter.next();

                ProjectRepositoryNode referencedProjectNode = new ProjectRepositoryNode(
                        new org.talend.core.model.general.Project(emfProject), null, parent, this, ENodeType.REFERENCED_PROJECT);
                referencedProjectNode.setProperties(EProperties.LABEL, emfProject.getLabel()); // //$NON-NLS-1$
                referencedProjectNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.REFERENCED_PROJECTS);
                parent.getChildren().add(referencedProjectNode);
                referencedProjectNode.initialize();
            }
        }
    }

    private void addNode(RepositoryNode parent, ERepositoryObjectType type, RepositoryNode recBinNode,
            IRepositoryObject repositoryObject) {

        RepositoryNode node = new RepositoryNode(repositoryObject, parent, ENodeType.REPOSITORY_ELEMENT);

        node.setProperties(EProperties.CONTENT_TYPE, type);
        node.setProperties(EProperties.LABEL, repositoryObject.getLabel());
        if (factory.getStatus(repositoryObject) == ERepositoryStatus.DELETED) {
            // recBinNode.getChildren().add(node);
            // node.setParent(recBinNode);
        } else {
            parent.getChildren().add(node);
        }

        if (type == ERepositoryObjectType.METADATA_CONNECTIONS) {
            DatabaseConnection metadataConnection = (DatabaseConnection) ((ConnectionItem) repositoryObject.getProperty()
                    .getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, metadataConnection);
        }
        if (type == ERepositoryObjectType.METADATA_SAPCONNECTIONS) {
            SAPConnection metadataConnection = (SAPConnection) ((ConnectionItem) repositoryObject.getProperty().getItem())
                    .getConnection();
            createTables(recBinNode, node, repositoryObject, metadataConnection);
        }
        // PTODO tgu implementation a revoir
        if (type == ERepositoryObjectType.METADATA_FILE_DELIMITED) {
            DelimitedFileConnection metadataConnection = (DelimitedFileConnection) ((ConnectionItem) repositoryObject
                    .getProperty().getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, metadataConnection);
        }
        if (type == ERepositoryObjectType.METADATA_FILE_POSITIONAL) {
            PositionalFileConnection metadataConnection = (PositionalFileConnection) ((ConnectionItem) repositoryObject
                    .getProperty().getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, metadataConnection);
        }
        if (type == ERepositoryObjectType.METADATA_FILE_REGEXP) {
            RegexpFileConnection metadataConnection = (RegexpFileConnection) ((ConnectionItem) repositoryObject.getProperty()
                    .getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, metadataConnection);
        }
        if (type == ERepositoryObjectType.METADATA_FILE_XML) {
            XmlFileConnection metadataConnection = (XmlFileConnection) ((ConnectionItem) repositoryObject.getProperty().getItem())
                    .getConnection();
            createTables(recBinNode, node, repositoryObject, metadataConnection);
        }

        if (type == ERepositoryObjectType.METADATA_FILE_EXCEL) {
            FileExcelConnection metadataConnection = (FileExcelConnection) ((ConnectionItem) repositoryObject.getProperty()
                    .getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, metadataConnection);
        }

        if (type == ERepositoryObjectType.METADATA_FILE_LDIF) {
            LdifFileConnection metadataConnection = (LdifFileConnection) ((ConnectionItem) repositoryObject.getProperty()
                    .getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, metadataConnection);
        }

        if (type == ERepositoryObjectType.METADATA_LDAP_SCHEMA) {
            LDAPSchemaConnection metadataConnection = (LDAPSchemaConnection) ((ConnectionItem) repositoryObject.getProperty()
                    .getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, metadataConnection);
        }

        if (type == ERepositoryObjectType.METADATA_GENERIC_SCHEMA) {
            GenericSchemaConnection genericSchemaConnection = (GenericSchemaConnection) ((ConnectionItem) repositoryObject
                    .getProperty().getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, genericSchemaConnection);
        }
        if (type == ERepositoryObjectType.METADATA_WSDL_SCHEMA) {
            WSDLSchemaConnection genericSchemaConnection = (WSDLSchemaConnection) ((ConnectionItem) repositoryObject
                    .getProperty().getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, genericSchemaConnection);
        }
        if (type == ERepositoryObjectType.METADATA_SALESFORCE_SCHEMA) {
            SalesforceSchemaConnection genericSchemaConnection = (SalesforceSchemaConnection) ((ConnectionItem) repositoryObject
                    .getProperty().getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, genericSchemaConnection);
        }
        if (type == ERepositoryObjectType.METADATA_FILE_EBCDIC) {
            EbcdicConnection ebcdicConnection = (EbcdicConnection) ((ConnectionItem) repositoryObject.getProperty().getItem())
                    .getConnection();
            createTables(recBinNode, node, repositoryObject, ebcdicConnection);
        }
    }

    /**
     * DOC tguiu Comment method "createTables".
     * 
     * @param node
     * @param repositoryObjectType TODO
     * @param iMetadataConnection
     * @param metadataConnection
     */
    private void createTables(RepositoryNode recBinNode, RepositoryNode node, final IRepositoryObject repObj, EList list,
            ERepositoryObjectType repositoryObjectType) {
        for (Object currentTable : list) {
            if (currentTable instanceof org.talend.core.model.metadata.builder.connection.MetadataTable) {
                org.talend.core.model.metadata.builder.connection.MetadataTable metadataTable = (org.talend.core.model.metadata.builder.connection.MetadataTable) currentTable;
                RepositoryNode tableNode = createMetatableNode(node, repObj, metadataTable, repositoryObjectType);
                if (SubItemHelper.isDeleted(metadataTable)) {
                    recBinNode.getChildren().add(tableNode);
                } else {
                    node.getChildren().add(tableNode);
                }
            } else if (currentTable instanceof Query) {
                Query query = (Query) currentTable;
                RepositoryNode queryNode = createQueryNode(node, repObj, query);
                if (SubItemHelper.isDeleted(query)) {
                    recBinNode.getChildren().add(queryNode);
                } else {
                    node.getChildren().add(queryNode);
                }

            }
        }
    }

    /**
     * DOC cantoine Comment method "createTable".
     * 
     * @param node
     * @param metadataTable
     * @param repositoryObjectType TODO
     * @param iMetadataConnection
     */
    private void createTable(RepositoryNode recBinNode, RepositoryNode node, final IRepositoryObject repObj,
            org.talend.core.model.metadata.builder.connection.MetadataTable metadataTable,
            ERepositoryObjectType repositoryObjectType) {
        RepositoryNode tableNode = createMetatableNode(node, repObj, metadataTable, repositoryObjectType);
        if (TableHelper.isDeleted(metadataTable)) {
            recBinNode.getChildren().add(tableNode);
        } else {
            node.getChildren().add(tableNode);
        }
    }

    private void createTables(RepositoryNode recBinNode, RepositoryNode node, final IRepositoryObject repObj,
            Connection metadataConnection) {

        // // 5.GENERIC SCHEMAS
        // RepositoryNode genericSchemaNode = new StableRepositoryNode(node, Messages
        // .getString("RepositoryContentProvider.repositoryLabel.GenericSchema"), ECoreImage.FOLDER_CLOSE_ICON);
        // node.getChildren().add(genericSchemaNode);

        if (metadataConnection instanceof DatabaseConnection) {

            // 1.Tables:
            RepositoryNode tablesNode = new StableRepositoryNode(node, Messages
                    .getString("RepositoryContentProvider.repositoryLabel.TableSchemas"), ECoreImage.FOLDER_CLOSE_ICON); //$NON-NLS-1$
            node.getChildren().add(tablesNode);

            // 2.VIEWS:
            RepositoryNode viewsNode = new StableRepositoryNode(node, Messages
                    .getString("RepositoryContentProvider.repositoryLabel.ViewSchemas"), ECoreImage.FOLDER_CLOSE_ICON); //$NON-NLS-1$
            node.getChildren().add(viewsNode);

            // 3.SYNONYMS:
            RepositoryNode synonymsNode = new StableRepositoryNode(node, Messages
                    .getString("RepositoryContentProvider.repositoryLabel.SynonymSchemas"), ECoreImage.FOLDER_CLOSE_ICON); //$NON-NLS-1$
            node.getChildren().add(synonymsNode);

            Iterator metadataTables = metadataConnection.getTables().iterator();
            while (metadataTables.hasNext()) {
                org.talend.core.model.metadata.builder.connection.MetadataTable metadataTable = (org.talend.core.model.metadata.builder.connection.MetadataTable) metadataTables
                        .next();

                String typeTable = null;
                if (metadataTable.getTableType() != null) {
                    typeTable = metadataTable.getTableType();
                    if (typeTable.equals("TABLE")) { //$NON-NLS-1$
                        createTable(recBinNode, tablesNode, repObj, metadataTable, ERepositoryObjectType.METADATA_CON_TABLE);

                    } else if (typeTable.equals("VIEW")) { //$NON-NLS-1$
                        createTable(recBinNode, viewsNode, repObj, metadataTable, ERepositoryObjectType.METADATA_CON_TABLE);

                    } else if (typeTable.equals("SYNONYM")) { //$NON-NLS-1$
                        createTable(recBinNode, synonymsNode, repObj, metadataTable, ERepositoryObjectType.METADATA_CON_TABLE);
                    }

                    // else if (typeTable.equals("GENERIC_SCHEMA")) {
                    // //TODO not finished.
                    // createTable(recBinNode, tablesNode, repObj, metadataTable,
                    // ERepositoryObjectType.METADATA_CON_TABLE);
                    // }
                } else {
                    createTable(recBinNode, tablesNode, repObj, metadataTable, ERepositoryObjectType.METADATA_CON_TABLE);
                }
            }

            // if (!node.getChildren().contains(tablesNode)) {
            // node.getChildren().add(tablesNode);
            // }

            // createTables(recBinNode, node, repObj, metadataConnection.getTables());

            // 4.Queries:
            RepositoryNode queriesNode = new StableRepositoryNode(node, Messages
                    .getString("RepositoryContentProvider.repositoryLabel.Queries"), ECoreImage.FOLDER_CLOSE_ICON); //$NON-NLS-1$
            node.getChildren().add(queriesNode);
            QueriesConnection queriesConnection = (metadataConnection).getQueries();
            if (queriesConnection != null) {
                createTables(recBinNode, queriesNode, repObj, queriesConnection.getQuery(),
                        ERepositoryObjectType.METADATA_CON_TABLE);
            }

            // 5. Change Data Capture
            Item item = node.getObject().getProperty().getItem();
            if (item instanceof DatabaseConnectionItem) {
                DatabaseConnectionItem connectionItem = (DatabaseConnectionItem) item;
                DatabaseConnection connection = (DatabaseConnection) connectionItem.getConnection();
                if (PluginChecker.isCDCPluginLoaded()) {
                    ICDCProviderService service = (ICDCProviderService) GlobalServiceRegister.getDefault().getService(
                            ICDCProviderService.class);
                    if (service != null && service.canCreateCDCConnection(connection)) {
                        RepositoryNode cdcNode = new StableRepositoryNode(node, Messages
                                .getString("RepositoryContentProvider.repositoryLabel.CDCFoundation"), //$NON-NLS-1$
                                ECoreImage.FOLDER_CLOSE_ICON);
                        node.getChildren().add(cdcNode);
                        service.createCDCTypes(recBinNode, cdcNode, connection.getCdcConns());
                    }
                }
            }
        } else if (metadataConnection instanceof SAPConnection) {
            // The sap wizard plugin is loaded
            // 1.Tables:
            RepositoryNode functionNode = new StableRepositoryNode(node, Messages
                    .getString("RepositoryContentProvider.repositoryLabel.sapFunction"), ECoreImage.FOLDER_CLOSE_ICON); //$NON-NLS-1$
            node.getChildren().add(functionNode);
            // add functions
            createSAPFunctionNodes(recBinNode, repObj, metadataConnection, functionNode);

        } else {
            createTables(recBinNode, node, repObj, metadataConnection.getTables(), ERepositoryObjectType.METADATA_CON_TABLE);
        }
    }

    /**
     * DOC YeXiaowei Comment method "createSAPFunctionNodes".
     * 
     * @param metadataConnection
     * @param functionNode
     */
    private void createSAPFunctionNodes(final RepositoryNode recBin, IRepositoryObject rebObj, Connection metadataConnection,
            RepositoryNode functionNode) {
        EList functions = ((SAPConnection) metadataConnection).getFuntions();
        if (functions == null || functions.isEmpty()) {
            return;
        }
        for (int i = 0; i < functions.size(); i++) {
            SAPFunctionUnit unit = (SAPFunctionUnit) functions.get(i);
            RepositoryNode tableNode = createSAPNode(rebObj, functionNode, unit);
            if (SubItemHelper.isDeleted(unit)) {
                recBin.getChildren().add(tableNode);
            } else {
                functionNode.getChildren().add(tableNode);
            }

        }
    }

    /**
     * DOC YeXiaowei Comment method "createSAPNode".
     * 
     * @param rebObj
     * @param functionNode
     * @param unit
     * @return
     */
    private RepositoryNode createSAPNode(IRepositoryObject rebObj, RepositoryNode functionNode, SAPFunctionUnit unit) {
        SAPFunctionRepositoryObject modelObj = new SAPFunctionRepositoryObject(rebObj, functionNode, unit);
        modelObj.setLabel(unit.getName());
        RepositoryNode tableNode = new RepositoryNode(modelObj, functionNode, ENodeType.REPOSITORY_ELEMENT);
        tableNode.setProperties(EProperties.LABEL, modelObj.getLabel());
        tableNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_SAP_FUNCTION);
        return tableNode;
    }

    /**
     * DOC tguiu Comment method "createMetatable".
     * 
     * @param node
     * @param table
     * @param repositoryObjectType TODO
     * @param iMetadataFileDelimited
     * @return
     */
    private RepositoryNode createMetatableNode(RepositoryNode node, IRepositoryObject repObj,
            final org.talend.core.model.metadata.builder.connection.MetadataTable table,
            ERepositoryObjectType repositoryObjectType) {
        MetadataTable modelObj = new MetadataTableRepositoryObject(repObj, table);
        modelObj.setLabel(table.getLabel());
        RepositoryNode tableNode = new RepositoryNode(modelObj, node, ENodeType.REPOSITORY_ELEMENT);
        tableNode.setProperties(EProperties.LABEL, table.getLabel());
        tableNode.setProperties(EProperties.CONTENT_TYPE, repositoryObjectType);
        return tableNode;

    }

    /**
     * DOC cantoine Comment method "createQueryNode".
     * 
     * @param node
     * @param repObj
     * @param query
     * @return
     */
    private RepositoryNode createQueryNode(RepositoryNode node, IRepositoryObject repObj, Query query) {
        QueryRepositoryObject modelObj = new QueryRepositoryObject(repObj, query);
        modelObj.setLabel(query.getLabel());
        RepositoryNode tableNode = new RepositoryNode(modelObj, node, ENodeType.REPOSITORY_ELEMENT);
        tableNode.setProperties(EProperties.LABEL, query.getLabel());
        tableNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_CON_QUERY);
        return tableNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getCodeNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getCodeNode()
     */
    public RepositoryNode getCodeNode() {
        return this.codeNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getProcessNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getProcessNode()
     */
    public RepositoryNode getProcessNode() {
        return this.processNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataConNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataConNode()
     */
    public RepositoryNode getMetadataConNode() {
        return this.metadataConNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataConNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataConNode()
     */
    public RepositoryNode getMetadataSAPConnectionNode() {
        return this.metadataSAPConnectionNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataFileNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataFileNode()
     */
    public RepositoryNode getMetadataFileNode() {
        return this.metadataFileNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataFilePositionalNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataFilePositionalNode()
     */
    public RepositoryNode getMetadataFilePositionalNode() {
        return this.metadataFilePositionalNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataFileRegexpNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataFileRegexpNode()
     */
    public RepositoryNode getMetadataFileRegexpNode() {
        return this.metadataFileRegexpNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataFileXmlNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataFileXmlNode()
     */
    public RepositoryNode getMetadataFileXmlNode() {
        return this.metadataFileXmlNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataFileLdifNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataFileLdifNode()
     */
    public RepositoryNode getMetadataFileLdifNode() {
        return this.metadataFileLdifNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataLDAPSchemaNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataLDAPSchemaNode()
     */
    public RepositoryNode getMetadataLDAPSchemaNode() {
        return this.metadataLDAPSchemaNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataWSDLSchemaNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataWSDLSchemaNode()
     */
    public RepositoryNode getMetadataWSDLSchemaNode() {
        return this.metadataWSDLSchemaNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataFileExcelNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataFileExcelNode()
     */
    public RepositoryNode getMetadataFileExcelNode() {
        return this.metadataFileExcelNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataSalesforceSchemaNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataSalesforceSchemaNode()
     */
    public RepositoryNode getMetadataSalesforceSchemaNode() {
        return this.metadataSalesforceSchemaNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getJobletNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getJobletNode()
     */
    public RepositoryNode getJobletNode() {
        return this.jobletNode;
    }

    public RepositoryNode getReferenceProjectNode() {
        return this.refProject;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataNode()
     */
    public RepositoryNode getMetadataNode() {
        return this.metadataNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataGenericSchemaNode()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getMetadataGenericSchemaNode()
     */
    public RepositoryNode getMetadataGenericSchemaNode() {
        return this.metadataGenericSchemaNode;
    }

    public RepositoryNode getMetadataEbcdicConnectionNode() {
        return this.metadataEbcdicConnectionNode;
    }

    public RepositoryNode getContextNode() {
        return this.getContextNode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getProject()
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.IProjectRepositoryNode#getProject()
     */
    public org.talend.core.model.general.Project getProject() {
        return this.project;
    }

    public RepositoryNode getRootRepositoryNode(ERepositoryObjectType type) {
        if (type == null) {
            return null;
        }

        switch (type) {
        case BUSINESS_PROCESS:
            return this.businessProcessNode;
        case PROCESS:
            return this.processNode;
        case CONTEXT:
            return this.contextNode;
        case ROUTINES:
            return this.routineNode;
        case SNIPPETS:
            return this.snippetsNode;
        case GENERATED:
        case JOBS:
        case JOB_DOC:
        case JOBLETS:
        case JOBLET_DOC:
        case DOCUMENTATION:
            return this.docNode;
            // case METADATA_CON_TABLE:
        case METADATA:
            return this.metadataNode; // maybe, there are some problems to process some fuctions.
        case METADATA_CON_VIEW:
        case METADATA_CON_SYNONYM:
        case METADATA_CON_QUERY:
        case METADATA_CON_CDC:
        case METADATA_CONNECTIONS:
            return this.metadataConNode;
        case METADATA_SAPCONNECTIONS:
            return this.metadataSAPConnectionNode;
        case SQLPATTERNS:
            return this.sqlPatternNode;
        case METADATA_FILE_DELIMITED:
            return this.metadataFileNode;
        case METADATA_FILE_POSITIONAL:
            return this.metadataFilePositionalNode;
        case METADATA_FILE_REGEXP:
            return this.metadataFileRegexpNode;
        case METADATA_FILE_XML:
            return this.metadataFileXmlNode;
        case METADATA_FILE_LDIF:
            return this.metadataFileLdifNode;
        case METADATA_FILE_EXCEL:
            return this.metadataFileExcelNode;
        case METADATA_FILE_EBCDIC:
            return this.metadataEbcdicConnectionNode;
        case METADATA_SALESFORCE_SCHEMA:
            return this.metadataSalesforceSchemaNode;
        case METADATA_GENERIC_SCHEMA:
            return this.metadataGenericSchemaNode;
        case METADATA_LDAP_SCHEMA:
            return this.metadataLDAPSchemaNode;
        case METADATA_WSDL_SCHEMA:
            return this.metadataWSDLSchemaNode;
        case REFERENCED_PROJECTS:
            return this.refProject;
        case JOBLET:
            return this.jobletNode;
        default:
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.nodes.IProjectRepositoryNode#isMainProject()
     */
    public boolean isMainProject() {
        return getParent() == null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.model.nodes.IProjectRepositoryNode#getRecBinNode()
     */
    public RepositoryNode getRecBinNode() {
        return this.recBinNode;
    }

    public boolean getMergeRefProject() {
        IPreferenceStore preferenceStore = RepositoryManager.getPreferenceStore();
        this.mergeRefProject = preferenceStore.getBoolean(IRepositoryPrefConstants.MERGE_REFERENCE_PROJECT);
        return this.mergeRefProject;
    }

}
