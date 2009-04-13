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
package org.talend.repository.ui.dialog;

import java.util.Iterator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.talend.repository.i18n.Messages;
import org.talend.repository.ui.actions.ExportProjectSettings;
import org.talend.repository.ui.actions.ImportProjectSettings;

/**
 * wchen class global comment. Detailled comment
 */
public class ProjectSettingsPreferenceDialog extends PreferenceDialog {

    private Button importButton;

    private Button exportButton;

    public static final int IMPORT = 97;

    public static final int EXPORT = 98;

    /**
     * wchen ProjectSettingsPreferenceDialog constructor comment.
     * 
     * @param parentShell
     * @param manager
     */
    public ProjectSettingsPreferenceDialog(Shell parentShell, PreferenceManager manager) {
        super(parentShell, manager);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        importButton = createButton(parent, IMPORT, "Import", false);
        exportButton = createButton(parent, EXPORT, "Export", false);
        super.createButtonsForButtonBar(parent);

    }

    @Override
    protected void buttonPressed(int buttonId) {
        switch (buttonId) {
        case IDialogConstants.OK_ID: {
            okPressed();
            return;
        }
        case IDialogConstants.CANCEL_ID: {
            cancelPressed();
            return;
        }
        case IDialogConstants.HELP_ID: {
            helpPressed();
            return;
        }
        case IMPORT: {
            importPressed();
            return;
        }
        case EXPORT: {
            exportPressed();
            return;
        }
        }
    }

    private void importPressed() {

        FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
        String[] files = new String[] { "*.xml" };
        fileDialog.setFilterExtensions(files);

        String path = fileDialog.open();
        ImportProjectSettings settings = new ImportProjectSettings(path);

        boolean error = false;
        try {
            settings.updateProjectSettings();
        } catch (Exception e) {
            error = true;
            showErrorMessage();
        }

        // IPreferenceNode[] rootSubNodes = this.getPreferenceManager().getRootSubNodes();
        // for (IPreferenceNode node : rootSubNodes) {
        // refresh(node);
        // }

        // close the projec settings and open it again to get new settings
        if (!error) {
            close();
            ProjectSettingDialog dialog = new ProjectSettingDialog();
            dialog.open();
        }
    }

    // private void refresh(IPreferenceNode rootSubNodes) {
    // if (rootSubNodes != null) {
    // IPreferencePage page = rootSubNodes.getPage();
    // if (page instanceof ProjectSettingPage) {
    // ((ProjectSettingPage) page).refresh();
    // }
    // for (IPreferenceNode child : rootSubNodes.getSubNodes()) {
    // refresh(child);
    // }
    // }
    // }

    private void exportPressed() {
        saveCurrentSettings();
        FileDialog fileDialog = new FileDialog(getShell(), SWT.SAVE);
        fileDialog.setFileName("ProjectSettings.xml");
        String[] files = new String[] { "*.xml" };
        fileDialog.setFilterExtensions(files);

        String path = fileDialog.open();
        ExportProjectSettings settings = new ExportProjectSettings(path);
        settings.saveProjectSettings();

    }

    private void showErrorMessage() {
        MessageBox message = new MessageBox(new Shell(getShell()), SWT.ICON_ERROR | SWT.OK);
        message.setMessage(Messages.getString("ImportProjectSettings.Error"));
        message.open();
    }

    protected void saveCurrentSettings() {
        SafeRunnable.run(new SafeRunnable() {

            private boolean errorOccurred;

            public void run() {
                errorOccurred = false;
                boolean hasFailedOK = false;
                try {
                    Iterator nodes = getPreferenceManager().getElements(PreferenceManager.PRE_ORDER).iterator();
                    while (nodes.hasNext()) {
                        IPreferenceNode node = (IPreferenceNode) nodes.next();
                        IPreferencePage page = node.getPage();
                        if (page != null) {
                            if (!page.performOk()) {
                                hasFailedOK = true;
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    handleException(e);
                } finally {

                    if (hasFailedOK) {
                        setReturnCode(FAILED);
                        return;
                    }

                    if (!errorOccurred) {

                        handleSave();
                    }
                    setReturnCode(OK);
                }
            }

            public void handleException(Throwable e) {
                errorOccurred = true;

                Policy.getLog().log(new Status(IStatus.ERROR, Policy.JFACE, 0, e.toString(), e));

                clearSelectedNode();
                String message = JFaceResources.getString("SafeRunnable.errorMessage"); //$NON-NLS-1$

                Policy.getStatusHandler().show(new Status(IStatus.ERROR, Policy.JFACE, message, e),
                        JFaceResources.getString("Error")); //$NON-NLS-1$                                                             

            }
        });
    }

    void clearSelectedNode() {
        setSelectedNodePreference(null);
    }

}
