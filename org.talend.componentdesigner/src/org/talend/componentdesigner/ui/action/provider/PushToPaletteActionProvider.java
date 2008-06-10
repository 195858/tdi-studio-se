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
package org.talend.componentdesigner.ui.action.provider;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.talend.componentdesigner.i18n.internal.Messages;
import org.talend.componentdesigner.util.file.FileCopy;
import org.talend.core.CorePlugin;

/**
 * DOC slanglois class global comment. Detailled comment
 */
public class PushToPaletteActionProvider extends CommonActionProvider {

    private IAction copyProjectAction;

    private List<IFolder> selectedFolderList;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.navigator.CommonActionProvider#init(org.eclipse.ui.navigator.ICommonActionExtensionSite)
     */
    public void init(ICommonActionExtensionSite anExtensionSite) {

        if (anExtensionSite.getViewSite() instanceof ICommonViewerWorkbenchSite) {
            copyProjectAction = new PushToPaletteAction();
        }
    }

    /**
     * Adds a submenu to the given menu with the name "New Component".
     */
    public void fillContextMenu(IMenuManager menu) {
        menu.insertBefore("group.edit", copyProjectAction); //$NON-NLS-1$
        // Object obj = ((TreeSelection) this.getContext().getSelection()).getFirstElement();// need to get all
        // selected.
        Iterator ite = ((TreeSelection) this.getContext().getSelection()).iterator();
        selectedFolderList = new ArrayList<IFolder>();
        while (ite.hasNext()) {
            Object obj = ite.next();
            if (obj instanceof IFolder) {
                selectedFolderList.add((IFolder) obj);
            }
        }
    }

    /**
     * DOC slanglois PushToPaletteActionProvider class global comment. Detailled comment
     */
    class PushToPaletteAction extends Action {

        public PushToPaletteAction() {
            super(Messages.getString("PushToPaletteActionProvider.PushComponentsToPalette")); //$NON-NLS-1$
            // setImageDescriptor(ImageLib.getImageDescriptor(ImageLib.COPYCOMPONENT_ACTION));
        }

        /*
         * (non-Javadoc) Method declared on IAction.
         */
        public void run() {
            String path = CorePlugin.getDefault().getComponentsLocalProviderService().getPreferenceStore().getString(
                    "USER_COMPONENTS_FOLDER"); //$NON-NLS-1$
            if (path == null || path.length() == 0) {
                new MessageDialog(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                        Messages.getString("PushToPaletteActionProvider.Error"), null, //$NON-NLS-1$
                        Messages.getString("PushToPaletteActionProvider.ErrorMSG"), MessageDialog.ERROR, new String[] { Messages.getString("PushToPaletteActionProvider.OK") }, 0).open(); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            File targetFile = new File(path);
            if (!targetFile.exists()) {
                new MessageDialog(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                        Messages.getString("PushToPaletteActionProvider.Error2"), null, //$NON-NLS-1$
                        Messages.getString("PushToPaletteActionProvider.ErrorMSG2"), MessageDialog.ERROR, new String[] { Messages.getString("PushToPaletteActionProvider.OK2") }, 0).open(); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            for (IFolder selectedFolder : selectedFolderList) {
                File sourceFile = selectedFolder.getRawLocation().toFile();
                String sourceComponentFolder = sourceFile.getAbsolutePath();
                String targetComponentFolder = targetFile.getAbsolutePath() + File.separator + sourceFile.getName();

                FileCopy.copyComponentFolder(sourceComponentFolder, targetComponentFolder, true);
            }

            CorePlugin.getDefault().getCodeGeneratorService().generationInit();

            MessageDialog warningMessageDialog = new MessageDialog(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                    Messages.getString("PushToPaletteActionProvider.Information"), null, Messages.getString("PushToPaletteActionProvider.InformationMSG"), MessageDialog.INFORMATION, //$NON-NLS-1$ //$NON-NLS-2$
                    new String[] { Messages.getString("PushToPaletteActionProvider.OK3") }, 0); //$NON-NLS-1$
            warningMessageDialog.open();

        }
    }
}
