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
package org.talend.repository.ui.views;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.PluginChecker;
import org.talend.core.model.general.Project;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.properties.DatabaseConnectionItem;
import org.talend.core.model.properties.DocumentationItem;
import org.talend.core.model.properties.InformationLevel;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.JobletProcessItem;
import org.talend.core.model.properties.LinkDocumentationItem;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.ui.ICDCProviderService;
import org.talend.core.ui.IReferencedProjectService;
import org.talend.core.ui.images.CoreImageProvider;
import org.talend.core.ui.images.ECoreImage;
import org.talend.core.ui.images.OverlayImageProvider;
import org.talend.repository.ProjectManager;
import org.talend.repository.RepositoryPlugin;
import org.talend.repository.model.ECDCStatus;
import org.talend.repository.model.ERepositoryStatus;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.MetadataTableRepositoryObject;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNode.ENodeType;

/**
 * Label provider for the repository view. <code>DEBUG</code> boolean field specify if details (such as objects ids)
 * must appears on display or not.<br/>
 * 
 * $Id$
 * 
 */
public class RepositoryLabelProvider extends LabelProvider implements IColorProvider, IFontProvider {

    private static final Color STABLE_SECONDARY_ENTRY_COLOR = new Color(null, 100, 100, 100);

    private static final Color STABLE_PRIMARY_ENTRY_COLOR = new Color(null, 0, 0, 0);

    private static final Color INACTIVE_ENTRY_COLOR = new Color(null, 200, 200, 200);

    private static final Color LOCKED_ENTRY = new Color(null, 200, 0, 0);

    private static final Color MERGED_REFERENCED_ITEMS_COLOR = new Color(null, 120, 120, 120);

    private IRepositoryView view;

    public RepositoryLabelProvider(IRepositoryView view) {
        super();
        this.view = view;
    }

    public String getText(Property property) {
        StringBuffer string = new StringBuffer();
        string.append(property.getLabel());
        // PTODO SML [FOLDERS++] temp code
        if (ERepositoryObjectType.getItemType(property.getItem()) != ERepositoryObjectType.FOLDER) {
            string.append(" " + property.getVersion()); //$NON-NLS-1$
        }
        // nodes in the recycle bin
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        if (factory.getStatus(property.getItem()) == ERepositoryStatus.DELETED) {
            String oldPath = property.getItem().getState().getPath();
            string.append(" (" + oldPath + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // PTODO SML [FOLDERS++] temp code
        // if (object.getType() != ERepositoryObjectType.FOLDER) {
        // string.append(" [" + factory.getStatus(object.getProperty().getItem()) + "]");
        // }
        return string.toString();
    }

    public String getText(Object obj) {
        if (obj instanceof Property) {
            return getText((Property) obj);
        }

        RepositoryNode node = (RepositoryNode) obj;

        if (node.getType() == ENodeType.REPOSITORY_ELEMENT || node.getType() == ENodeType.SIMPLE_FOLDER) {
            IRepositoryObject object = node.getObject();
            if (object == null) {
                return node.getLabel();
            }
            org.talend.core.model.properties.Project mainProject = ProjectManager.getInstance().getCurrentProject()
                    .getEmfProject();
            org.talend.core.model.properties.Project emfproject = ProjectManager.getInstance().getProject(
                    object.getProperty().getItem());

            // TODO SML remove this table rustine
            switch (object.getType()) {
            case METADATA_CON_QUERY:
            case SNIPPETS:
            case METADATA_CON_SYNONYM:
            case METADATA_CON_TABLE:
            case METADATA_CON_VIEW:
            case METADATA_CON_CDC:
            case METADATA_SAP_FUNCTION:
                String label = object.getLabel();
                if (!mainProject.equals(emfproject) && PluginChecker.isRefProjectLoaded()) {

                    IReferencedProjectService service = (IReferencedProjectService) GlobalServiceRegister.getDefault()
                            .getService(IReferencedProjectService.class);
                    if (service != null && service.isMergeRefProject()) {
                        Project project = new Project(emfproject);
                        label = label + " (@" + project.getLabel() + ")";
                    }

                }
                return label;
            default:
                break;
            }
            String label = getText(object.getProperty());
            if (!mainProject.equals(emfproject) && PluginChecker.isRefProjectLoaded()) {
                IReferencedProjectService service = (IReferencedProjectService) GlobalServiceRegister.getDefault().getService(
                        IReferencedProjectService.class);
                if (service != null && service.isMergeRefProject()) {
                    Project project = new Project(emfproject);
                    label = label + " (@" + project.getLabel() + ")";
                }

            }

            return label;
        } else {
            String label = node.getLabel();
            return label;
        }
    }

    public Image getImage(Property property) {
        Item item = property.getItem();
        ERepositoryObjectType itemType = ERepositoryObjectType.getItemType(item);
        Image img = null;
        if (itemType == ERepositoryObjectType.JOBLET) {
            img = getJobletCustomIcon(view.getSite().getShell().getDisplay(), property);
        } else {
            img = CoreImageProvider.getImage(itemType);
        }

        // Manage doc extensions:
        if (itemType == ERepositoryObjectType.DOCUMENTATION) {
            if (item instanceof DocumentationItem) {
                img = OverlayImageProvider.getImageWithDocExt(((DocumentationItem) item).getExtension());
            } else if (item instanceof LinkDocumentationItem) {
                img = OverlayImageProvider.getImageWithSpecial(img).createImage();
                /*
                 * It's slowly, so disable it.
                 */
                // LinkType link = ((LinkDocumentationItem) item).getLink();
                // if (!LinkUtils.validateLink(link)) {
                // img = OverlayImageProvider.getImageWithError(img).createImage();
                // }
            }
        }

        // add the error info in the icon only for routine (only for java, because the perl auto build problem.)
        // if (itemType == ERepositoryObjectType.ROUTINES) {
        // ECodeLanguage lang = ((RepositoryContext)
        // CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY))
        // .getProject().getLanguage();
        // if (lang == ECodeLanguage.JAVA) {
        // IDesignerCoreService designerCoreService = RepositoryPlugin.getDefault().getDesignerCoreService();
        // Boolean isCompilePass = designerCoreService.isRoutineCompilePass(property.getLabel());
        // if (isCompilePass != null && !isCompilePass) {
        // img = OverlayImageProvider.getImageWithError(img).createImage();
        // }
        // }
        // }

        // Manage master job case:
        // PTODO SML
        // if (node.getObject().getType() == ERepositoryObjectType.PROCESS &&
        // node.getObject().getLabel().equals("Tagada")) {
        // img = OverlayImageProvider.getImageWithSpecial(img).createImage();
        // }

        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        ERepositoryStatus repositoryStatus = factory.getStatus(item);

        Image image = OverlayImageProvider.getImageWithStatus(img, repositoryStatus);

        InformationLevel informationLevel = property.getMaxInformationLevel();
        ERepositoryStatus informationStatus = factory.getStatus(informationLevel);

        return OverlayImageProvider.getImageWithStatus(image, informationStatus);
    }

    public static File getDefaultJobletImage() {
        Bundle b = CorePlugin.getDefault().getBundle();
        URL url = null;
        try {
            url = FileLocator.toFileURL(FileLocator.find(b, new Path(ECoreImage.JOBLET_ICON.getPath()), null));
            return new File(url.getPath());
        } catch (IOException e) {
            throw new RuntimeException(ECoreImage.JOBLET_ICON.getPath() + " doesn't exist.");
        }
    }

    /**
     * DOC bqian Comment method "getJobletCustomIcon".
     * 
     * @param property
     * @return
     */
    public static Image getJobletCustomIcon(Display display, Property property) {
        JobletProcessItem item = (JobletProcessItem) property.getItem();
        Image image = null;
        if (item.getIcon() == null) {
            // File image = RepositoryLabelProvider.getDefaultJobletImage();
            // try {
            // item.getIcon().setInnerContentFromFile(image);
            // } catch (Exception e) {
            // ExceptionHandler.process(e);
            // }

            image = ImageProvider.getImage(ECoreImage.JOBLET_ICON);
        } else {
            ByteArrayInputStream bis = new ByteArrayInputStream(item.getIcon().getInnerContent());
            ImageData imageData = new ImageData(bis);
            image = new Image(display, imageData);
        }
        return image;
    }

    public Image getImage(Object obj) {
        if (obj instanceof Property) {
            return getImage((Property) obj);
        }

        RepositoryNode node = (RepositoryNode) obj;

        switch (node.getType()) {
        case STABLE_SYSTEM_FOLDER:
        case SYSTEM_FOLDER:
            return ImageProvider.getImage(node.getIcon());
        case SIMPLE_FOLDER:
            // FIXME SML Move in repository node
            ECoreImage image = (view.getExpandedState(obj) ? ECoreImage.FOLDER_OPEN_ICON : ECoreImage.FOLDER_CLOSE_ICON);
            return ImageProvider.getImage(image);
        default:
            if (node.getObject() == null) {
                return ImageProvider.getImage(node.getIcon());
            }

            switch (node.getObject().getType()) {
            case METADATA_CON_QUERY:
            case SNIPPETS:
            case METADATA_CON_SYNONYM:
            case METADATA_CON_VIEW:
            case JOB_DOC:
            case JOBLET_DOC:
                return ImageProvider.getImage(node.getIcon());
            case METADATA_CON_TABLE:
                Image tableImage = ImageProvider.getImage(node.getIcon());
                Item item = node.getObject().getProperty().getItem();
                if (item != null && item instanceof DatabaseConnectionItem) {
                    if (PluginChecker.isCDCPluginLoaded()) {
                        ICDCProviderService service = (ICDCProviderService) GlobalServiceRegister.getDefault().getService(
                                ICDCProviderService.class);
                        if (service != null) {
                            String cdcLinkId = service.getCDCConnectionLinkId((DatabaseConnectionItem) item);
                            if (cdcLinkId != null) { // cdc connection exist.
                                if (node.getObject() instanceof MetadataTableRepositoryObject) {
                                    MetadataTable table = ((MetadataTableRepositoryObject) node.getObject()).getTable();
                                    String tableType = table.getTableType();
                                    if (tableType != null && "TABLE".equals(tableType)) {
                                        ECDCStatus status = ECDCStatus.NONE;
                                        if (table.isActivatedCDC()) {
                                            status = ECDCStatus.ACTIVATED;
                                        } else if (table.isAttachedCDC()) {
                                            status = ECDCStatus.ADDED;
                                        }
                                        return OverlayImageProvider.getImageWithCDCStatus(tableImage, status).createImage();
                                    }
                                }
                            }
                        }
                    }
                }
                return tableImage;
            case METADATA_CON_CDC:
                ImageDescriptor idf = RepositoryPlugin.imageDescriptorFromPlugin(RepositoryPlugin.PLUGIN_ID,
                        "icons/subscriber.jpg");
                return idf.createImage();
            default:
                break;
            }

            return getImage(node.getObject().getProperty());
        }
    }

    public Color getBackground(Object element) {
        return null;
    }

    public Color getForeground(Object element) {
        RepositoryNode node = (RepositoryNode) element;
        switch (node.getType()) {
        case REFERENCED_PROJECT:
            return STABLE_PRIMARY_ENTRY_COLOR;
        case STABLE_SYSTEM_FOLDER:
            if (node.getLabel().equals(ERepositoryObjectType.SNIPPETS.toString())) {
                return INACTIVE_ENTRY_COLOR;
            }
            if (node.getContentType() == ERepositoryObjectType.METADATA) {
                return STABLE_PRIMARY_ENTRY_COLOR;
            }
        case SYSTEM_FOLDER:
            if (node.getContentType() == ERepositoryObjectType.BUSINESS_PROCESS) {
                return STABLE_PRIMARY_ENTRY_COLOR;
            }
            if (node.getContentType() == ERepositoryObjectType.PROCESS) {
                return STABLE_PRIMARY_ENTRY_COLOR;
            }
            return STABLE_SECONDARY_ENTRY_COLOR;
        default:
            IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
            ERepositoryStatus repositoryStatus = factory.getStatus(node.getObject());
            if (repositoryStatus == ERepositoryStatus.LOCK_BY_OTHER) {
                return LOCKED_ENTRY;
            } else {
                if (PluginChecker.isRefProjectLoaded()) {
                    IReferencedProjectService service = (IReferencedProjectService) GlobalServiceRegister.getDefault()
                            .getService(IReferencedProjectService.class);
                    if (service != null && service.isMergeRefProject()) {
                        IRepositoryObject object = node.getObject();
                        if (object != null) {
                            org.talend.core.model.properties.Project project = ProjectManager.getInstance().getProject(
                                    object.getProperty().getItem());
                            org.talend.core.model.properties.Project curProject = ProjectManager.getInstance()
                                    .getCurrentProject().getEmfProject();
                            if (!curProject.equals(project)) {
                                return MERGED_REFERENCED_ITEMS_COLOR;
                            }
                        }
                    }
                }
                return null;
            }
        }
    }

    public Font getFont(Object element) {
        RepositoryNode node = (RepositoryNode) element;
        switch (node.getType()) {
        case STABLE_SYSTEM_FOLDER:
            if (node.getLabel().equals(ERepositoryObjectType.SNIPPETS.toString())) {
                return JFaceResources.getFontRegistry().defaultFont();
            }
        case SYSTEM_FOLDER:
            return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
        default:
            return JFaceResources.getFontRegistry().defaultFont();
        }
    }

}
