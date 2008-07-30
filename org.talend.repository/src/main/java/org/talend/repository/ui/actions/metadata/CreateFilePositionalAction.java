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
package org.talend.repository.ui.actions.metadata;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.core.model.properties.PositionalFileConnectionItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.ui.images.ECoreImage;
import org.talend.core.ui.images.OverlayImageProvider;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNode.EProperties;
import org.talend.repository.ui.wizards.metadata.connection.files.positional.FilePositionalWizard;

/**
 * DOC cantoine class global comment. Detailled comment <br/>
 * 
 * $Id$
 * 
 */
public class CreateFilePositionalAction extends AbstractCreateAction {

    private static final String EDIT_LABEL = Messages.getString("CreateFilePositionalAction.action.editTitle"); //$NON-NLS-1$

    private static final String OPEN_LABEL = Messages.getString("CreateFilePositionalAction.action.openTitle"); //$NON-NLS-1$

    private static final String CREATE_LABEL = Messages.getString("CreateFilePositionalAction.action.createTitle"); //$NON-NLS-1$

    protected static final int WIZARD_WIDTH = 920;

    protected static final int WIZARD_HEIGHT = 540;

    private boolean creation = false;

    ImageDescriptor defaultImage = ImageProvider.getImageDesc(ECoreImage.METADATA_FILE_POSITIONAL_ICON);

    ImageDescriptor createImage = OverlayImageProvider.getImageWithNew(ImageProvider
            .getImage(ECoreImage.METADATA_FILE_POSITIONAL_ICON));

    /**
     * DOC cantoine CreateFilePositionalAction constructor comment.
     * 
     * @param viewer
     */
    public CreateFilePositionalAction() {
        super();

        this.setText(CREATE_LABEL);
        this.setToolTipText(CREATE_LABEL);
        this.setImageDescriptor(defaultImage);
    }

    public CreateFilePositionalAction(boolean isToolbar) {
        super();
        setToolbar(isToolbar);
        this.setText(CREATE_LABEL);
        this.setToolTipText(CREATE_LABEL);
        this.setImageDescriptor(defaultImage);
    }

    public void run() {
        // RepositoryNode metadataNode = getViewPart().getRoot().getChildren().get(6);
        // RepositoryNode filePositionalNode = metadataNode.getChildren().get(2);
        RepositoryNode filePositionalNode = getCurrentRepositoryNode();

        if (isToolbar()) {
            if (filePositionalNode != null
                    && filePositionalNode.getContentType() != ERepositoryObjectType.METADATA_FILE_POSITIONAL) {
                filePositionalNode = null;
            }
            if (filePositionalNode == null) {
                filePositionalNode = getRepositoryNodeForDefault(ERepositoryObjectType.METADATA_FILE_POSITIONAL);
            }
        }
        ISelection selection = null;
        WizardDialog wizardDialog;
        if (isToolbar()) {
            init(filePositionalNode);
            wizardDialog = new WizardDialog(new Shell(), new FilePositionalWizard(PlatformUI.getWorkbench(), creation,
                    filePositionalNode, getExistingNames()));
        } else {
            selection = getSelection();
            wizardDialog = new WizardDialog(new Shell(), new FilePositionalWizard(PlatformUI.getWorkbench(), creation, selection,
                    getExistingNames()));
        }
        wizardDialog.setPageSize(WIZARD_WIDTH, WIZARD_HEIGHT);
        wizardDialog.create();
        wizardDialog.open();
        if (isToolbar()) {
            refresh(filePositionalNode);
        } else {
            refresh(((IStructuredSelection) selection).getFirstElement());
        }
    }

    public Class getClassForDoubleClick() {
        return PositionalFileConnectionItem.class;
    }

    protected void init(RepositoryNode node) {
        ERepositoryObjectType nodeType = (ERepositoryObjectType) node.getProperties(EProperties.CONTENT_TYPE);
        if (!ERepositoryObjectType.METADATA_FILE_POSITIONAL.equals(nodeType)) {
            return;
        }

        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        switch (node.getType()) {
        case SIMPLE_FOLDER:
        case SYSTEM_FOLDER:
            if (factory.isUserReadOnlyOnCurrentProject()
                    || !node.getRoot().getProject().equals(factory.getRepositoryContext().getProject())) {
                setEnabled(false);
                return;
            }
            this.setText(CREATE_LABEL);
            collectChildNames(node);
            this.setImageDescriptor(createImage);
            creation = true;
            break;
        case REPOSITORY_ELEMENT:
            if (factory.isPotentiallyEditable(node.getObject())) {
                this.setText(EDIT_LABEL);
                this.setImageDescriptor(defaultImage);
                collectSiblingNames(node);
            } else {
                this.setText(OPEN_LABEL);
                this.setImageDescriptor(defaultImage);
            }
            collectSiblingNames(node);
            creation = false;
            break;
        }
        setEnabled(true);
    }
}
