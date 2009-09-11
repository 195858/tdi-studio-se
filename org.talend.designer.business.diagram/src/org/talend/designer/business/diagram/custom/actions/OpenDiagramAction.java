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
package org.talend.designer.business.diagram.custom.actions;

import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.core.model.properties.BusinessProcessItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.model.repository.RepositoryManager;
import org.talend.core.model.repository.RepositoryObject;
import org.talend.core.ui.images.ECoreImage;
import org.talend.designer.business.diagram.i18n.Messages;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNodeUtilities;
import org.talend.repository.model.RepositoryNode.EProperties;
import org.talend.repository.ui.actions.AContextualAction;

/**
 * DOC mhelleboid class global comment. Detailled comment <br/>
 * 
 * $Id$
 * 
 */
public class OpenDiagramAction extends AContextualAction implements IIntroAction {

    private Properties params;

    public OpenDiagramAction() {
        super();
        setImageDescriptor(ImageProvider.getImageDesc(ECoreImage.BUSINESS_PROCESS_ICON));
        setText(Messages.getString("OpenDiagramAction.EditBusinessModel")); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    protected void doRun() {
        ISelection selection = getSelectedObject();
        if (selection == null) {
            return;
        }
        Object obj = ((IStructuredSelection) selection).getFirstElement();
        if (obj instanceof RepositoryNode) {
            RepositoryNode repositoryNode = (RepositoryNode) obj;
            IRepositoryObject repositoryObject = repositoryNode.getObject();

            if (repositoryObject instanceof RepositoryObject) {
                RepositoryObject abstractRepositoryObject = (RepositoryObject) repositoryObject;

                BusinessProcessItem businessProcessItem = (BusinessProcessItem) abstractRepositoryObject.getProperty().getItem();
                DiagramResourceManager diagramResourceManager = new DiagramResourceManager(getActivePage(),
                        new NullProgressMonitor());
                IFile file = diagramResourceManager.createDiagramFile();
                diagramResourceManager.updateResource(businessProcessItem, file);
                diagramResourceManager.openEditor(businessProcessItem, file, false);
            }
            RepositoryManager.getRepositoryView().refresh(repositoryNode);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.actions.ITreeContextualAction#init(org.eclipse.jface.viewers.TreeViewer,
     * org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init(TreeViewer viewer, IStructuredSelection selection) {
        boolean enabled = false;

        if (!selection.isEmpty() && selection.size() == 1) {
            Object object = selection.getFirstElement();
            if (object instanceof RepositoryNode) {
                RepositoryNode repositoryNode = (RepositoryNode) object;
                ERepositoryObjectType nodeType = (ERepositoryObjectType) repositoryNode.getProperties(EProperties.CONTENT_TYPE);
                if (repositoryNode.getType() == RepositoryNode.ENodeType.REPOSITORY_ELEMENT) {
                    if (nodeType == ERepositoryObjectType.BUSINESS_PROCESS) {
                        enabled = true;
                    }
                }
            }
        }

        if (ProxyRepositoryFactory.getInstance().isUserReadOnlyOnCurrentProject()) {
            enabled = false;
        }

        setEnabled(enabled);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.actions.AContextualView#getClassForDoubleClick()
     */
    @Override
    public Class getClassForDoubleClick() {
        return BusinessProcessItem.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.intro.config.IIntroAction#run(org.eclipse.ui.intro.IIntroSite, java.util.Properties)
     */
    public void run(IIntroSite site, Properties params) {
        this.params = params;
        PlatformUI.getWorkbench().getIntroManager().closeIntro(PlatformUI.getWorkbench().getIntroManager().getIntro());
        doRun();

    }

    private ISelection getSelectedObject() {
        if (params == null) {
            return getSelection();
        } else {
            RepositoryNode repositoryNode = RepositoryNodeUtilities.getRepositoryNode(params.getProperty("nodeId"), false);
            if (repositoryNode != null) {
                RepositoryNodeUtilities.expandParentNode(getViewPart(), repositoryNode);
                return new StructuredSelection(repositoryNode);
            }
            return null;
        }
    }
}
