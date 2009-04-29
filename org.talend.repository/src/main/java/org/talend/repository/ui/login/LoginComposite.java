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
package org.talend.repository.ui.login;

import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.exception.WarningException;
import org.talend.commons.ui.image.EImage;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.commons.utils.PasswordHelper;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.general.ConnectionBean;
import org.talend.core.model.general.Project;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.User;
import org.talend.core.prefs.PreferenceManipulator;
import org.talend.core.ui.branding.IBrandingService;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.IRepositoryFactory;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryConstants;
import org.talend.repository.model.RepositoryFactoryProvider;
import org.talend.repository.ui.ERepositoryImages;
import org.talend.repository.ui.actions.importproject.DeleteProjectsAsAction;
import org.talend.repository.ui.actions.importproject.ImportDemoProjectAction;
import org.talend.repository.ui.actions.importproject.ImportProjectAsAction;
import org.talend.repository.ui.login.connections.ConnectionsDialog;
import org.talend.repository.ui.wizards.newproject.NewProjectWizard;

/**
 * labe Composite login.<br/>
 * 
 * $Id: /talend/tos/trunk/org.talend.repository/src/main/java/org/talend/repository/ui/login/LoginComposite.java 24167
 * 2009-04-28T09:55:53.574018Z wchen $
 * 
 */
public class LoginComposite extends Composite {

    private static int horizontalMerge = 145;

    private static final int HORIZONTAL_SPACE_BUTTONS = 0;

    private static final int VERTICAL_SPACE = 0;

    private static final int HORIZONTAL_SPACE = 5;

    /**
     * Colors used for Remote Object background when enabled.
     */
    public static final Color WHITE_COLOR = new Color(null, 255, 255, 255);

    public static final Color GREY_COLOR = new Color(null, 200, 200, 200);

    private FormToolkit toolkit;

    private ComboViewer connectionsViewer;

    private LoginDialog dialog;

    private Text user;

    private Text passwordText;

    private ComboViewer projectViewer;

    public Button fillProjectsBtn;

    public Button openProjectBtn;

    private Button manageConnectionsButton;

    private Project[] projects;

    private Label newProjectLabel;

    private Button manageProjectsButton;

    private Label statusLabel;

    private Composite messageImageStatus;

    private ComboViewer manageViewer;

    private Label manageProjectLabel1;

    private String lastWarnings;

    private Label existingLabel;

    /**
     * Constructs a new LoginComposite.
     * 
     * @param parent Parent component.
     * @param style Style bits.
     */
    public LoginComposite(Composite parent, int style, LoginDialog dialog) {
        super(parent, style);

        this.dialog = dialog;

        toolkit = new FormToolkit(this.getDisplay());
        Form form = toolkit.createForm(this);
        Composite formBody = form.getBody();

        formBody.setBackgroundMode(SWT.INHERIT_DEFAULT);

        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);
        form.setLayoutData(new GridData(GridData.FILL_BOTH));

        layout = new GridLayout(3, false);
        formBody.setLayout(layout);

        messageImageStatus = toolkit.createComposite(formBody);
        messageImageStatus.setLayout(new FormLayout());

        Label createLabel = toolkit.createLabel(messageImageStatus, null);
        createLabel.setBackgroundImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_ERROR));
        FormData formData2 = new FormData();
        formData2.left = new FormAttachment(0);
        formData2.right = new FormAttachment(0, 16);
        formData2.top = new FormAttachment(0);
        formData2.bottom = new FormAttachment(0, 16);
        createLabel.setLayoutData(formData2);

        statusLabel = toolkit.createLabel(messageImageStatus, null);
        formData2 = new FormData();
        formData2.left = new FormAttachment(createLabel, HORIZONTAL_SPACE);
        formData2.right = new FormAttachment(100, -HORIZONTAL_SPACE);
        statusLabel.setLayoutData(formData2);

        GridData projectGroupData2 = new GridData(GridData.FILL_HORIZONTAL);
        projectGroupData2.horizontalSpan = 3;
        messageImageStatus.setLayoutData(projectGroupData2);

        Group groupConnection = new Group(formBody, SWT.NONE);
        groupConnection.setText(Messages.getString("LoginComposite.connection")); //$NON-NLS-1$

        projectGroupData2 = new GridData(GridData.FILL_HORIZONTAL);
        projectGroupData2.horizontalSpan = 3;
        groupConnection.setLayoutData(projectGroupData2);

        groupConnection.setLayout(layout);

        // Connections listbox:
        Label connectionLabel = toolkit.createLabel(groupConnection, Messages.getString("LoginComposite.connections")); //$NON-NLS-1$
        Point size = connectionLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        horizontalMerge = size.x;
        // TODO SML this method assume that this label is the longest and must be optimize
        GridData conGrid2 = new GridData();
        conGrid2.widthHint = horizontalMerge;
        connectionLabel.setLayoutData(conGrid2);

        connectionsViewer = new ComboViewer(groupConnection, SWT.BORDER | SWT.READ_ONLY);
        GridData conGrid = new GridData(GridData.FILL_HORIZONTAL);
        conGrid.horizontalSpan = 0;
        connectionsViewer.getControl().setLayoutData(conGrid);
        connectionsViewer.setContentProvider(new ArrayContentProvider());
        connectionsViewer.setLabelProvider(new ConnectionLabelProvider());

        manageConnectionsButton = toolkit.createButton(groupConnection, null, SWT.PUSH);
        manageConnectionsButton.setToolTipText(Messages.getString("LoginComposite.manageConnectionsToolTipHint")); //$NON-NLS-1$
        manageConnectionsButton.setImage(ImageProvider.getImage(EImage.THREE_DOTS_ICON));
        GridData checkGrid = new GridData(SWT.BORDER | SWT.RIGHT);
        checkGrid.verticalSpan = 1;
        manageConnectionsButton.setLayoutData(checkGrid);

        IBrandingService brandingService = (IBrandingService) GlobalServiceRegister.getDefault().getService(
                IBrandingService.class);
        boolean usesMailCheck = brandingService.getBrandingConfiguration().isUseMailLoginCheck();
        Label userLabel;
        if (usesMailCheck) {
            toolkit.createLabel(groupConnection, Messages.getString("connections.form.field.username")); //$NON-NLS-1$
        } else {
            toolkit.createLabel(groupConnection, Messages.getString("connections.form.field.usernameNoMail")); //$NON-NLS-1$
        }
        // Username:

        user = toolkit.createText(groupConnection, "", SWT.BORDER); //$NON-NLS-1$
        GridData userGrid2 = new GridData(GridData.FILL_HORIZONTAL);
        userGrid2.horizontalSpan = 2;
        user.setLayoutData(userGrid2);
        user.setEnabled(false);

        // Password:
        toolkit.createLabel(groupConnection, Messages.getString("connections.form.field.password")); //$NON-NLS-1$
        passwordText = toolkit.createText(groupConnection, "", SWT.PASSWORD | SWT.BORDER); //$NON-NLS-1$
        GridData passwordGrid = new GridData(GridData.FILL_HORIZONTAL);
        passwordGrid.horizontalSpan = 2;
        passwordText.setLayoutData(passwordGrid);

        Label emptyLabel = toolkit.createLabel(formBody, ""); //$NON-NLS-1$
        GridData emptyLabelData = new GridData(GridData.FILL_HORIZONTAL);
        emptyLabelData.horizontalSpan = 3;
        emptyLabel.setLayoutData(emptyLabelData);

        // Project
        Group group = new Group(formBody, SWT.NONE);
        group.setText(Messages.getString("LoginComposite.project")); //$NON-NLS-1$
        GridData projectGroupData = new GridData(GridData.FILL_HORIZONTAL);
        projectGroupData.horizontalSpan = 3;
        group.setLayoutData(projectGroupData);

        FormData data;

        FormLayout groupLayout = new FormLayout();
        group.setLayout(groupLayout);

        // go
        manageProjectsButton = toolkit.createButton(group, null, SWT.PUSH);
        manageProjectsButton.setText(Messages.getString("LoginComposite.manageProjectsButton")); //$NON-NLS-1$

        manageViewer = new ComboViewer(group, SWT.BORDER | SWT.READ_ONLY);
        manageViewer.setContentProvider(new ArrayContentProvider());
        manageViewer.setInput(getManageElements());

        data = new FormData();
        data.right = new FormAttachment(100, -HORIZONTAL_SPACE);
        data.left = new FormAttachment(manageViewer.getControl(), HORIZONTAL_SPACE);
        data.top = new FormAttachment(0, HORIZONTAL_SPACE);
        manageProjectsButton.setLayoutData(data);

        // TODO select
        manageProjectLabel1 = toolkit.createLabel(group, Messages.getString("LoginComposite.existing")); //$NON-NLS-1$
        data = new FormData();
        data.left = new FormAttachment(0, HORIZONTAL_SPACE);
        data.bottom = new FormAttachment(manageProjectsButton, VERTICAL_SPACE, SWT.CENTER);

        manageProjectLabel1.setLayoutData(data);
        // data for managerViewer
        data = new FormData();
        data.left = new FormAttachment(manageProjectLabel1, HORIZONTAL_SPACE);
        data.bottom = new FormAttachment(manageProjectLabel1, VERTICAL_SPACE, SWT.CENTER);
        manageViewer.getControl().setLayoutData(data);

        existingLabel = toolkit.createLabel(group, Messages.getString("LoginComposite.manageProjectPre"));
        data = new FormData();
        data.left = new FormAttachment(manageProjectLabel1, 0, SWT.CENTER);
        data.top = new FormAttachment(manageProjectLabel1, 40);
        existingLabel.setLayoutData(data);

        // open
        openProjectBtn = toolkit.createButton(group, null, SWT.PUSH);
        openProjectBtn.setText(Messages.getString("LoginComposite.buttons.open")); //$NON-NLS-1$
        openProjectBtn.setToolTipText(Messages.getString("LoginComposite.buttons.open.desc")); //$NON-NLS-1$
        Image image = ImageProvider.getImage(ERepositoryImages.OPEN_PROJECT_ICON);
        openProjectBtn.setImage(image);
        data = new FormData();

        data.right = new FormAttachment(100, -HORIZONTAL_SPACE);
        data.bottom = new FormAttachment(existingLabel, VERTICAL_SPACE, SWT.CENTER);
        openProjectBtn.setLayoutData(data);
        // refresh
        fillProjectsBtn = toolkit.createButton(group, null, SWT.PUSH);
        // fillProjectsBtn.setText(Messages.getString("LoginComposite.buttons.open")); //$NON-NLS-1$
        fillProjectsBtn.setToolTipText(Messages.getString("LoginComposite.buttons.fill.desc")); //$NON-NLS-1$
        fillProjectsBtn.setImage(ImageProvider.getImage(EImage.REFRESH_ICON));
        data = new FormData();
        data.right = new FormAttachment(openProjectBtn, -HORIZONTAL_SPACE);
        data.bottom = new FormAttachment(openProjectBtn, VERTICAL_SPACE, SWT.CENTER);
        fillProjectsBtn.setLayoutData(data);

        projectViewer = new ComboViewer(group, SWT.BORDER | SWT.READ_ONLY);

        projectViewer.setContentProvider(new ArrayContentProvider());
        projectViewer.setLabelProvider(new ProjectLabelProvider());

        data = new FormData();
        data.left = new FormAttachment(manageViewer.getControl(), 0, SWT.LEFT);
        data.right = new FormAttachment(fillProjectsBtn, -HORIZONTAL_SPACE);
        data.bottom = new FormAttachment(fillProjectsBtn, 0, SWT.CENTER);
        projectViewer.getControl().setLayoutData(data);

        PreferenceManipulator prefManipulator = new PreferenceManipulator(CorePlugin.getDefault().getPreferenceStore());
        if (prefManipulator.getBoolean(ImportDemoProjectAction.DEMO_ALREADY_IMPORTED)) {
            manageViewer.setSelection(new StructuredSelection(new Object[] { manageViewer.getElementAt(1) }));
        } else {
            manageViewer.setSelection(new StructuredSelection(new Object[] { manageViewer.getElementAt(0) }));
        }

        fillContents();
        addListeners();
    }

    private ManageItem[] getManageElements() {
        List<ManageItem> toReturn = new ArrayList<ManageItem>();

        IBrandingService brandingService = (IBrandingService) GlobalServiceRegister.getDefault().getService(
                IBrandingService.class);
        boolean usesDemoProjects = brandingService.getBrandingConfiguration().isUseDemoProjects();

        if (usesDemoProjects) {
            toReturn.add(new ManageItem(ImportDemoProjectAction.getInstance().getToolTipText()) {

                @Override
                public void run() {
                    importDemoProject();
                }
            });
        }

        toReturn.add(new ManageItem(Messages.getString("LoginComposite.buttons.newProject.desc")) { //$NON-NLS-1$

                    @Override
                    public void run() {
                        createNewProject();
                    }

                });
        toReturn.add(new ManageItem(Messages.getString("LoginComposite.buttons.importProject.desc")) { //$NON-NLS-1$

                    @Override
                    public void run() {
                        importProjects();
                    }

                });
        toReturn.add(new ManageItem(Messages.getString("LoginComposite.buttons.deleteProject.desc")) { //$NON-NLS-1$

                    @Override
                    public void run() {
                        deleteProject();
                    }

                });

        return toReturn.toArray(new ManageItem[] {});
    }

    /**
     * Class use to fill manage projects dialog box.
     */
    private abstract class ManageItem {

        private String label;

        public ManageItem(String label) {
            super();
            this.label = label;
        }

        public String getLabel() {
            return this.label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return getLabel();
        }

        public abstract void run();
    }

    /**
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        toolkit.dispose();
        super.dispose();
    }

    public void setErrorMessage(String string) {
        if (string == null) {
            messageImageStatus.setVisible(false);
        } else {
            statusLabel.setText(string);
            messageImageStatus.setVisible(true);
        }
    }

    private void fillContents() {
        PreferenceManipulator prefManipulator = new PreferenceManipulator(CorePlugin.getDefault().getPreferenceStore());

        List<ConnectionBean> storedConnections = prefManipulator.readConnections();
        connectionsViewer.setInput(storedConnections);

        // Check number of connection available.
        if (storedConnections.size() == 0) {

        } else if (storedConnections.size() == 1) {
            connectionsViewer.setSelection(new StructuredSelection(new Object[] { storedConnections.get(0) }));
        } else {
            // select last connection used
            String lastConnection = prefManipulator.getLastConnection();
            boolean selected = false;
            for (ConnectionBean curent : storedConnections) {
                String stringValue = ((LabelProvider) connectionsViewer.getLabelProvider()).getText(curent);
                if (stringValue.equals(lastConnection)) {
                    selectLast(stringValue, connectionsViewer.getCombo());
                    selected = true;
                }
            }
            if (!selected) {
                connectionsViewer.setSelection(new StructuredSelection(new Object[] { storedConnections.get(0) }));
            }
        }

        projectViewer.getControl().setEnabled(false);

        if (getConnection() != null) {
            user.setText(getConnection().getUser());
            passwordText.setText(getConnection().getPassword());

            if (!isAuthenticationNeeded()) {
                unpopulateRemoteLoginElements();
            }
            setRepositoryContextInContext();
        }
        updateButtons();

        // Validate data
        if (validateFields()) {
            populateProjectList();
            validateProject();
        }
    }

    /**
     * If setted, Select last ? used in PreferenceStore.
     * 
     * @param prefManipulator
     */
    private void selectLast(String lastObjectSelected, Combo comboToSelect) {
        if (lastObjectSelected != null) {
            int userIndex = -1;
            String[] items = comboToSelect.getItems();
            for (int i = 0; userIndex == -1 && i < items.length; i++) {
                if (lastObjectSelected.equals(items[i])) {
                    userIndex = i;
                }
            }
            if (userIndex != -1) {
                comboToSelect.select(userIndex);
            } else {
                comboToSelect.select(0);
            }
        }
    }

    /**
     * 
     * @return
     */
    protected boolean isAuthenticationNeeded() {
        if (getConnection() == null || !getConnection().isComplete()) {
            return false;
        }
        IRepositoryFactory repository = RepositoryFactoryProvider.getRepositoriyById(getConnection().getRepositoryId());
        if (repository == null) {
            return false;
        }
        return repository.isAuthenticationNeeded();
    }

    private void addListeners() {
        connectionsViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                user.setText(getConnection().getUser());
                passwordText.setText(getConnection().getPassword());
                updateServerFields();
                updateButtons();

                // Validate data
                if (validateFields()) {
                    populateProjectList();
                    validateProject();
                }
            }
        });

        ModifyListener modifyListener = new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                unpopulateProjectList();
                dialog.updateButtons();
                setRepositoryContextInContext();
            }
        };

        passwordText.addModifyListener(modifyListener);

        fillProjectsBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                // Validate data
                if (validateFields()) {
                    populateProjectList();
                    validateProject();
                }
            }
        });

        openProjectBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                dialog.okPressed();
            }
        });

        projectViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                PreferenceManipulator prefManipulator = new PreferenceManipulator(CorePlugin.getDefault().getPreferenceStore());
                prefManipulator.setLastProject(getProject().getLabel());
                dialog.updateButtons();
                setRepositoryContextInContext();
            }
        });

        manageConnectionsButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ConnectionsDialog connectionsDialog = new ConnectionsDialog(getShell());
                int open = connectionsDialog.open();
                if (open == Window.OK) {
                    PreferenceManipulator prefManipulator = new PreferenceManipulator(CorePlugin.getDefault()
                            .getPreferenceStore());
                    prefManipulator.saveConnections(connectionsDialog.getConnections());
                    fillContents();
                }
            }
        });

        manageProjectsButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ManageItem item = null;
                if (!manageViewer.getSelection().isEmpty()) {
                    IStructuredSelection sel = (IStructuredSelection) manageViewer.getSelection();
                    item = (ManageItem) sel.getFirstElement();

                }
                item.run();
            }
        });
    }

    public void createNewProject() {
        Project project = null;
        NewProjectWizard newPrjWiz = new NewProjectWizard();
        WizardDialog newProjectDialog = new WizardDialog(getShell(), newPrjWiz);
        newProjectDialog.setTitle(Messages.getString("LoginDialog.newProjectTitle")); //$NON-NLS-1$
        if (newProjectDialog.open() == Window.OK) {
            project = newPrjWiz.getProject();
            populateProjectList();
            selectProject(project);
        }
        validateProject();
    }

    public void importProjects() {
        ImportProjectAsAction.getInstance().run();
        populateProjectList();
        String newProject = ImportProjectAsAction.getInstance().getProjectName();
        if (newProject != null) {
            selectProject(newProject);
        }
        validateProject();
    }

    public void importDemoProject() {
        // dialog.setMessage("Importing demo project ...");
        ImportDemoProjectAction.getInstance().setShell(getShell());
        ImportDemoProjectAction.getInstance().run();
        populateProjectList();
        String newProject = ImportDemoProjectAction.getInstance().getProjectName();
        if (newProject != null) {
            selectProject(newProject);
        }
        validateProject();
    }

    public void deleteProject() {
        DeleteProjectsAsAction deleteProjectAction = new DeleteProjectsAsAction(true);
        deleteProjectAction.run();
        populateProjectList();
        dialog.updateButtons();
        validateProject();
    }

    /**
     * DOC smallet Comment method "updateServerFields".
     */
    private void updateServerFields() {
        setRepositoryContextInContext();
        validateFields();

        if (isAuthenticationNeeded()) {
            unpopulateProjectList();
            populateRemoteLoginElements();
        } else {
            unpopulateRemoteLoginElements();
        }
        dialog.updateButtons();
    }

    private void updateButtons() {
        if (isAuthenticationNeeded() || !validateFields()) {
            manageViewer.getControl().setVisible(false);
            manageProjectLabel1.setVisible(false);
            manageProjectsButton.setVisible(false);
            existingLabel.setVisible(false);
        } else {
            manageViewer.getControl().setVisible(true);
            manageProjectLabel1.setVisible(true);
            manageProjectsButton.setVisible(true);
            existingLabel.setVisible(true);
        }
    }

    private void unpopulateProjectList() {
        projectViewer.setInput(null);
        projects = null;
        projectViewer.getControl().setEnabled(false);
    }

    public RepositoryContext getRepositoryContext() {
        RepositoryContext repositoryContext = new RepositoryContext();
        repositoryContext.setUser(getUser());
        repositoryContext.setClearPassword(passwordText.getText());
        repositoryContext.setProject(getProject());
        repositoryContext.setFields(getConnection().getDynamicFields());
        return repositoryContext;
    }

    private void setRepositoryContextInContext() {
        Context ctx = CorePlugin.getContext();
        ctx.putProperty(Context.REPOSITORY_CONTEXT_KEY, getRepositoryContext());
    }

    protected void populateProjectList() {

        if (getConnection() == null || !getConnection().isComplete()) {
            return;
        }
        ProxyRepositoryFactory repositoryFactory = ProxyRepositoryFactory.getInstance();
        repositoryFactory.setRepositoryFactoryFromProvider(RepositoryFactoryProvider.getRepositoriyById(getConnection()
                .getRepositoryId()));

        boolean initialized = false;

        try {
            try {
                repositoryFactory.checkAvailability();
            } catch (WarningException e) {
                String warnings = e.getMessage();
                if (warnings != null && !warnings.equals(lastWarnings)) {
                    lastWarnings = warnings;
                    MessageDialog.openWarning(getShell(), "Warning", warnings); //$NON-NLS-1$
                }
            }

            try {
                IRunnableWithProgress op = new IRunnableWithProgress() {

                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        try {
                            ProxyRepositoryFactory.getInstance().initialize();
                        } catch (PersistenceException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                };
                new ProgressMonitorDialog(getShell()).run(true, false, op);
            } catch (InvocationTargetException e) {
                throw (Exception) e.getTargetException();
            } catch (InterruptedException e) {
            }

            initialized = true;
        } catch (Exception e) {
            projects = new Project[0];

            MessageDialog.openError(getShell(), "Unable to retrieve projects", "Unable to retrieve projects:\n" + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (initialized) {
            try {
                projects = repositoryFactory.readProject();
            } catch (PersistenceException e) {
                projects = new Project[0];

                setErrorMessage(Messages.getString("LoginComposite.refreshFailure1") + e.getMessage() //$NON-NLS-1$
                        + Messages.getString("LoginComposite.refreshFailure2")); //$NON-NLS-1$
                MessageDialog.openError(getShell(),
                        "Enable to retrieve projects", "Enable to retrieve projects:\n" + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
            } catch (BusinessException e) {
                projects = new Project[0];

                MessageDialog.openError(getShell(),
                        "Enable to retrieve projects", "Enable to retrieve projects:\n" + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        projectViewer.setInput(projects);

        // importDemoProjectAction.setExistingProjects(projects);

        if (projects.length > 0) {
            // Try to select the last recently used project
            selectLastUsedProject();

            projectViewer.getControl().setEnabled(true);
        } else {
            projectViewer.getControl().setEnabled(false);
        }
    }

    /**
     * DOC smallet Comment method "selectLastUsedProject".
     * 
     * @param projects
     */
    private void selectLastUsedProject() {
        Project[] projects = (Project[]) projectViewer.getInput();
        PreferenceManipulator prefManipulator = new PreferenceManipulator(CorePlugin.getDefault().getPreferenceStore());
        String lastProject = prefManipulator.getLastProject();
        if (lastProject != null) {
            Project goodProject = null;
            for (int i = 0; goodProject == null && i < projects.length; i++) {
                if (lastProject.equals(projects[i].getLabel())) {
                    goodProject = projects[i];
                }
            }

            if (goodProject == null && projects.length > 0) {
                goodProject = projects[0];
            }

            if (goodProject != null) {
                selectProject(goodProject);
            }
        }
    }

    /**
     * DOC smallet Comment method "selectProject".
     * 
     * @param goodProject
     */
    private void selectProject(Project goodProject) {
        projectViewer.setSelection(new StructuredSelection(new Object[] { goodProject }));
        setRepositoryContextInContext();
    }

    private void selectProject(String projectName) {
        for (Project current : projects) {
            if (current.getLabel().equals(projectName)) {
                selectProject(current);
                return;
            }
        }
    }

    public ConnectionBean getConnection() {
        IStructuredSelection sel = (IStructuredSelection) connectionsViewer.getSelection();
        ConnectionBean firstElement = (ConnectionBean) sel.getFirstElement();
        return firstElement;
    }

    public User getUser() {
        User toReturn = PropertiesFactory.eINSTANCE.createUser();
        toReturn.setLogin(user.getText());
        try {
            toReturn.setPassword(PasswordHelper.encryptPasswd(passwordText.getText()));
        } catch (NoSuchAlgorithmException e) {
            // e.printStackTrace();
            ExceptionHandler.process(e);
        }
        return toReturn;
    }

    public Project getProject() {
        Project project = null;
        if (!projectViewer.getSelection().isEmpty()) {
            IStructuredSelection sel = (IStructuredSelection) projectViewer.getSelection();
            project = (Project) sel.getFirstElement();
        }
        return project;
    }

    public boolean canFinish() {
        return validateFields() && validateProject();
    }

    /**
     * Label provider for Projects. <br/>
     * 
     * $Id$
     * 
     */
    private class ProjectLabelProvider extends LabelProvider {

        /**
         * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
         */
        @Override
        public String getText(Object element) {
            Project prj = (Project) element;
            String toReturn = prj.getLabel() + " - " + prj.getLanguage().getName(); //$NON-NLS-1$
            if (!prj.isLocal() && !isAuthenticationNeeded()) {
                toReturn += " (remote project in offline mode)"; //$NON-NLS-1$
            }
            return toReturn;
        }

    }

    /**
     * DOC smallet LoginComposite class global comment. Detailled comment <br/>
     * 
     * $Id$
     * 
     */
    private class ConnectionLabelProvider extends LabelProvider {

        /**
         * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
         */
        @Override
        public String getText(Object element) {
            ConnectionBean prj = (ConnectionBean) element;
            if (prj.isComplete()) {
                return prj.getName();
            } else {
                return prj.getName() + " (" + Messages.getString("connections.form.field.imcomplete") + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }
    }

    /**
     * clear login values.
     */
    private void unpopulateRemoteLoginElements() {
        passwordText.setText(""); //$NON-NLS-1$
        passwordText.setEnabled(false);
        passwordText.setEditable(false);
        passwordText.setBackground(GREY_COLOR);
        // checkBtn.setEnabled(false);
        populateProjectList();
    }

    /**
     * fill login valueswith default elements.
     */
    private void populateRemoteLoginElements() {
        passwordText.setEnabled(true);
        passwordText.setEditable(true);
        passwordText.setBackground(WHITE_COLOR);
        // checkBtn.setEnabled(true);

        // if (userCombo.getText().length() == 0) {
        // PreferenceManipulator prefManipulator = new
        // PreferenceManipulator(CorePlugin.getDefault().getPreferenceStore());
        // selectLast(prefManipulator.getLastUser(), userCombo);
        // }
        unpopulateProjectList();
    }

    private boolean validateFields() {
        String errorMsg = null;
        boolean valid = true;
        IBrandingService brandingService = (IBrandingService) GlobalServiceRegister.getDefault().getService(
                IBrandingService.class);
        boolean usesMailCheck = brandingService.getBrandingConfiguration().isUseMailLoginCheck();
        boolean serverIsLocal = !isAuthenticationNeeded();
        if (valid && getConnection() == null) {
            valid = false;
            errorMsg = Messages.getString("LoginComposite.connectionEmpty"); //$NON-NLS-1$
        } else if (valid && !getConnection().isComplete()) {
            valid = false;
            errorMsg = Messages.getString("LoginComposite.connectionIncomplete"); //$NON-NLS-1$
        } else if (valid && !serverIsLocal && user.getText().length() == 0) {
            valid = false;
            errorMsg = Messages.getString("LoginComposite.usernameEmpty"); //$NON-NLS-1$
        } else if (valid && usesMailCheck && !Pattern.matches(RepositoryConstants.MAIL_PATTERN, getUser().getLogin())) {
            valid = false;
            errorMsg = Messages.getString("LoginComposite.usernameMail"); //$NON-NLS-1$
        }
        if (valid && !serverIsLocal && passwordText.getText().length() == 0) {
            valid = false;
            errorMsg = Messages.getString("LoginComposite.passwordEmpty"); //$NON-NLS-1$
        }

        if (!valid) {
            setErrorMessage(errorMsg);
            // checkBtn.setEnabled(false);
        } else {
            setErrorMessage(null);
            if (isAuthenticationNeeded()) {
                // checkBtn.setEnabled(true);
            }
        }
        return valid;
    }

    private boolean validateProject() {
        String errorMsg = null;
        boolean valid = true;
        if (projectViewer.getCombo().getText().length() == 0) {
            valid = false;
            errorMsg = Messages.getString("LoginComposite.projectEmpty"); //$NON-NLS-1$
        }

        if (!valid) {
            setErrorMessage(errorMsg);
        } else {
            setErrorMessage(null);
        }
        return valid;
    }
}
