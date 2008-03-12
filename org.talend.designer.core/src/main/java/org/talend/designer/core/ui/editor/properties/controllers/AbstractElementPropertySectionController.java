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
package org.talend.designer.core.ui.editor.properties.controllers;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.BidiMap;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.views.properties.PropertySheet;
import org.talend.commons.ui.swt.proposal.ContentProposalAdapterExtended;
import org.talend.commons.ui.utils.ControlUtils;
import org.talend.commons.ui.utils.TypedTextCommandExecutor;
import org.talend.commons.utils.generation.CodeGenerationUtils;
import org.talend.core.context.RepositoryContext;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.language.ICodeProblemsChecker;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.QueryUtil;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.Element;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IElement;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.process.Problem;
import org.talend.core.model.process.Problem.ProblemStatus;
import org.talend.core.model.utils.ContextParameterUtils;
import org.talend.core.model.utils.TalendTextUtils;
import org.talend.core.ui.proposal.ProcessProposalUtils;
import org.talend.designer.core.DesignerPlugin;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.EmfComponent;
import org.talend.designer.core.model.process.jobsettings.JobSettingsConstants;
import org.talend.designer.core.ui.AbstractMultiPageTalendEditor;
import org.talend.designer.core.ui.editor.cmd.PropertyChangeCommand;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.process.Process;
import org.talend.designer.core.ui.editor.properties.ContextParameterExtractor;
import org.talend.designer.core.ui.editor.properties.OpenSQLBuilderDialogJob;
import org.talend.designer.core.ui.editor.properties.controllers.generator.IDynamicProperty;
import org.talend.designer.core.ui.views.jobsettings.JobSettingsView;
import org.talend.designer.core.ui.views.properties.ComponentSettingsView;
import org.talend.designer.core.ui.views.properties.WidgetFactory;
import org.talend.designer.runprocess.IRunProcessService;
import org.talend.sqlbuilder.ui.SQLBuilderDialog;
import org.talend.sqlbuilder.util.ConnectionParameters;
import org.talend.sqlbuilder.util.EConnectionParameterName;
import org.talend.sqlbuilder.util.TextUtil;
import org.talend.sqlbuilder.util.UIUtils;

/**
 * DOC yzhang class global comment. Detailled comment <br/>
 * 
 * $Id: talend-code-templates.xml 1 2006-09-29 17:06:40 +0000 (鏄熸湡浜�, 29 涔濇湀 2006) yzhang $
 * 
 */

public abstract class AbstractElementPropertySectionController implements PropertyChangeListener {

    protected static final String SQLEDITOR = "SQLEDITOR"; //$NON-NLS-1$

    private Map<String, SQLBuilderDialog> sqlbuilers = new HashMap<String, SQLBuilderDialog>();

    protected IDynamicProperty dynamicProperty;

    protected Composite composite;

    protected BidiMap hashCurControls;

    protected Element elem;

    protected AbstractMultiPageTalendEditor part;

    protected EComponentCategory section;

    protected EditionControlHelper editionControlHelper;

    private int additionalHeightSize;

    protected static final String VARIABLE_TOOLTIP = Messages
            .getString("AbstractElementPropertySectionController.variableTooltip"); //$NON-NLS-1$

    protected static final String NAME = "NAME"; //$NON-NLS-1$

    protected static final String COLUMN = "COLUMN"; //$NON-NLS-1$

    // PTODO qzhang use PARAMETER_NAME it for bug 853.
    protected static final String PARAMETER_NAME = TypedTextCommandExecutor.PARAMETER_NAME; //$NON-NLS-1$

    protected static final int MAX_PERCENT = 100;

    protected static final int STANDARD_LABEL_WIDTH = 100;

    protected static final int STANDARD_HEIGHT = 20;

    protected static final int STANDARD_BUTTON_WIDTH = 25;

    protected static final String DOTS_BUTTON = "icons/dots_button.gif"; //$NON-NLS-1$s

    protected EParameterFieldType paramFieldType;

    // for job settings extra.(feature 2710)
    protected IElementParameter curParameter;

    public static Map<String, String> connKeyMap = new HashMap<String, String>(10);

    static {
        connKeyMap.put("SERVER_NAME", "HOST");
        connKeyMap.put("PORT", "PORT");
        connKeyMap.put("SID", "DBNAME");
        connKeyMap.put("SCHEMA", "SCHEMA_DB");
        connKeyMap.put("USERNAME", "USER");
        connKeyMap.put("PASSWORD", "PASS");
        connKeyMap.put("PROPERTIES_STRING", "PROPERTIES");
        connKeyMap.put("DIRECTORY", "DIRECTORY");
        connKeyMap.put("FILE", "FILE");
        connKeyMap.put("DATASOURCE", "DATASOURCE");
    }

    /**
     * DOC yzhang Comment method "createControl".
     * 
     * Create control within the tabbed property setcion.
     * 
     * @param subComposite. The composite selected in the editor or view, transfered from super class of tabbed
     * properties framwork.
     * @param param. The paramenter from EMF.
     * @param numInRow. The ID of the control in a row.
     * @param nbInRow. The total quantity of the control in a row.
     * @param top
     * @param rowSize height that can take the control (0 if default size)
     * @param lastControl. The latest control created beside current being created.
     * @return. The control created by this method will be the paramenter of next be called createControl method for
     * position calculate.
     */
    public abstract Control createControl(final Composite subComposite, final IElementParameter param, final int numInRow,
            final int nbInRow, final int top, final Control lastControl);

    public abstract int estimateRowSize(final Composite subComposite, final IElementParameter param);

    protected int getColorStyledTextRowSize(int nbLines) {

        return 0;
    }

    /**
     * Will return true of false depends if the control has dynamic size or not.
     * 
     * @return
     */
    public boolean hasDynamicRowSize() {
        return false;
    }

    /**
     * Used only to force the rowSize if the size is dynamic.
     * 
     * @param height
     */
    public void setAdditionalHeightSize(int height) {
        this.additionalHeightSize = height;
    }

    /**
     * Used only to force the rowSize if the size is dynamic.
     * 
     * @return the height
     */
    public int getAdditionalHeightSize() {
        return additionalHeightSize;
    }

    /**
     * DOC yzhang AbstractElementPropertySectionController constructor comment.
     */
    public AbstractElementPropertySectionController(IDynamicProperty dp) {
        init(dp);
    }

    protected String getRepositoryItemFromRepositoryName(IElementParameter param, String repositoryName) {
        String value = (String) param.getValue();
        Object[] valuesList = param.getListItemsValue();
        String[] originalList = param.getListItemsDisplayName();
        for (int i = 0; i < valuesList.length; i++) {
            if (valuesList[i].equals(value)) {
                return originalList[i];
            }
        }
        return "";
    }

    protected String getValueFromRepositoryName(String repositoryName) {
        for (IElementParameter param : (List<IElementParameter>) elem.getElementParameters()) {
            if (param.getRepositoryValue() != null) {
                if (param.getRepositoryValue().equals(repositoryName)) {
                    if (param.getField().equals(EParameterFieldType.CLOSED_LIST)) {
                        return getRepositoryItemFromRepositoryName(param, repositoryName);
                    }
                    return (String) param.getValue();
                }
            }
        }
        return "";
    }

    protected String getValueFromRepositoryName(Element elem2, String repositoryName) {

        for (IElementParameter param : (List<IElementParameter>) elem2.getElementParameters()) {
            // for job settings extra.(feature 2710)
            if (!sameExtraParameter(param)) {
                continue;
            }
            if (param.getRepositoryValue() != null) {
                if (param.getRepositoryValue().equals(repositoryName)) {
                    if (param.getField().equals(EParameterFieldType.CLOSED_LIST)) {
                        return getRepositoryItemFromRepositoryName(param, repositoryName);
                    }
                    return (String) param.getValue();
                }
            }
        }
        return "";
    }

    protected String getParaNameFromRepositoryName(String repositoryName) {
        for (IElementParameter param : (List<IElementParameter>) elem.getElementParameters()) {
            // for job settings extra.(feature 2710)
            if (!sameExtraParameter(param)) {
                continue;
            }
            if (param.getRepositoryValue() != null) {
                if (param.getRepositoryValue().equals(repositoryName)) {
                    return param.getName();
                }
            }
        }
        return null;
    }

    protected String getParaNameFromRepositoryName(Element elem2, String repositoryName) {
        for (IElementParameter param : (List<IElementParameter>) elem2.getElementParameters()) {
            // for job settings extra.(feature 2710)
            if (!sameExtraParameter(param)) {
                continue;
            }
            if (param.getRepositoryValue() != null) {
                if (param.getRepositoryValue().equals(repositoryName)) {
                    return param.getName();
                }
            }
        }
        return null;
    }

    /**
     * DOC yzhang Comment method "init".
     * 
     * Configuration for necessay parameters from class DynamicTabbedPropertiesSection.
     */
    public void init(IDynamicProperty dp) {
        this.dynamicProperty = dp;
        hashCurControls = dp.getHashCurControls();
        elem = dp.getElement();
        part = dp.getPart();
        section = dp.getSection();
        composite = dp.getComposite();

        editionControlHelper = new EditionControlHelper();
        // elem.addPropertyChangeListener(this);
    }

    /**
     * Getter for dynamicTabbedPropertySection.
     * 
     * @return the dynamicTabbedPropertySection
     */
    public IDynamicProperty getDynamicProperty() {
        return this.dynamicProperty;
    }

    WidgetFactory widgetFactory = null;

    /**
     * DOC yzhang Comment method "getWidgetFactory".
     * 
     * Get the TabbedPropertySheetWidgetFactory for control creating.
     * 
     * @return
     */
    protected WidgetFactory getWidgetFactory() {
        if (widgetFactory == null) {
            widgetFactory = new WidgetFactory();
        }
        return widgetFactory;
    }

    /**
     * 
     * DOC amaumont DynamicTabbedPropertySection class global comment. Detailled comment <br/>
     * 
     * @author amaumont $Id: DynamicTabbedPropertySection.java 344 2006-11-08 14:29:42 +0000 (mer., 08 nov. 2006)
     * smallet $
     * 
     */
    class EditionControlHelper {

        private final CheckErrorsHelper checkErrorsHelper;

        protected UndoRedoHelper undoRedoHelper;

        private ContentProposalAdapterExtended extendedProposal;

        /**
         * DOC amaumont EditionListenerManager constructor comment.
         */
        public EditionControlHelper() {
            super();
            this.checkErrorsHelper = new CheckErrorsHelper();
            this.undoRedoHelper = new UndoRedoHelper();
        }

        /**
         * DOC amaumont Comment method "checkErrors".
         * 
         * @param t
         * @param b
         */
        public void checkErrors(Control control) {
            this.checkErrorsHelper.checkErrors(control);
        }

        /**
         * DOC amaumont Comment method "register".
         * 
         * @param parameterName
         * @param control
         * @param checkSyntax
         */
        public void register(final String parameterName, final Control control, boolean checkSyntax) {
            if (parameterName == null || control == null) {
                throw new NullPointerException();
            }
            if (!elem.getElementParameter(parameterName).isReadOnly()) {
                IProcess process = part.getTalendEditor().getProcess();
                this.extendedProposal = ProcessProposalUtils.installOn(control, process);
                if (!elem.getElementParameter(parameterName).isNoCheck()) {
                    this.checkErrorsHelper.register(control, extendedProposal);
                }
                extendedProposal.addContentProposalListener(new IContentProposalListener() {

                    public void proposalAccepted(IContentProposal proposal) {
                        if (control instanceof Text) {
                            ContextParameterExtractor.saveContext(parameterName, elem, ((Text) control).getText());
                        } else if (control instanceof StyledText) {
                            ContextParameterExtractor.saveContext(parameterName, elem, ((StyledText) control).getText());
                        }
                    }
                });
                // this.checkErrorsHelper.checkErrors(control, false);
                ContextParameterExtractor.installOn(control, (Process) process, parameterName, elem);
            }

            this.undoRedoHelper.register(control);
        }

        public void unregisterUndo(Control control) {
            this.undoRedoHelper.unregister(control);
        }

        /**
         * DOC amaumont Comment method "register".
         * 
         * @param control
         */
        public void unregister(Control control) {
            this.checkErrorsHelper.unregister(control);
            this.undoRedoHelper.unregister(control);
        }

    }

    private static Map<Control, ControlProperties> controlToProp = new HashMap<Control, ControlProperties>();

    /**
     * 
     * DOC amaumont DynamicTabbedPropertySection class global comment. Detailled comment <br/>
     * 
     * @author amaumont $Id: DynamicTabbedPropertySection.java 344 2006-11-08 14:29:42 +0000 (mer., 08 nov. 2006)
     * smallet $
     * 
     */
    class CheckErrorsHelper {

        /**
         * DOC amaumont CheckSyntaxHelper constructor comment.
         */
        public CheckErrorsHelper() {
            super();
        }

        private final FocusListener focusListenerForCheckingError = new FocusListener() {

            public void focusGained(FocusEvent event) {
                focusGainedExecute((Control) event.widget);
            }

            public void focusLost(FocusEvent event) {
                if (!extendedProposal.isProposalOpened()) {
                    Control control = (Control) event.widget;
                    checkErrorsForPropertiesOnly(control);
                }
            }

        };

        private final KeyListener keyListenerForCheckingError = new KeyListener() {

            public void keyPressed(KeyEvent event) {
                Control control = (Control) event.widget;
                resetErrorState(control);
            }

            public void keyReleased(KeyEvent e) {
            }

        };

        private ContentProposalAdapterExtended extendedProposal;

        public void register(Control control, ContentProposalAdapterExtended extendedProposal) {
            control.addFocusListener(focusListenerForCheckingError);
            control.addKeyListener(keyListenerForCheckingError);
            this.extendedProposal = extendedProposal;
        }

        /**
         * DOC amaumont Comment method "unregister".
         * 
         * @param control
         */
        public void unregister(Control control) {
            control.removeFocusListener(focusListenerForCheckingError);
            control.removeKeyListener(keyListenerForCheckingError);
        }

        private void focusGainedExecute(Control control) {
            resetErrorState(control);
        }

        /**
         * DOC amaumont Comment method "checkSyntax".
         * 
         * @param control
         * @param modifying
         */
        public void checkErrors(final Control control) {

            IElementParameter elementParameter = elem.getElementParameter(getParameterName(control));

            if (elementParameter.isReadOnly() || elementParameter.isNoCheck()) {
                return;
            }

            final Color bgColorError = control.getDisplay().getSystemColor(SWT.COLOR_RED);
            final Color fgColorError = control.getDisplay().getSystemColor(SWT.COLOR_WHITE);

            final ECodeLanguage language = ((RepositoryContext) org.talend.core.CorePlugin.getContext().getProperty(
                    org.talend.core.context.Context.REPOSITORY_CONTEXT_KEY)).getProject().getLanguage();

            IRunProcessService service = DesignerPlugin.getDefault().getRunProcessService();
            final ICodeProblemsChecker syntaxChecker = service.getSyntaxChecker(language);

            final String valueFinal = ControlUtils.getText(control);

            ControlProperties existingControlProperties = controlToProp.get(control);

            List<Problem> problems = new ArrayList<Problem>();
            if (valueFinal != null) {
                if (language == ECodeLanguage.PERL) {
                    problems = syntaxChecker.checkProblemsForExpression(valueFinal);
                } else if (language == ECodeLanguage.JAVA) {
                    String key = CodeGenerationUtils.buildProblemKey(elem.getElementName(), elementParameter.getName());
                    problems = syntaxChecker.checkProblemsFromKey(key, null);
                }
            }

            boolean isRequired = elem.getElementParameter(getParameterName(control)).isRequired();
            if (problems != null) {
                if (isRequired && (valueFinal == null || valueFinal.trim().length() == 0)) {
                    problems.add(new Problem(null,
                            Messages.getString("AbstractElementPropertySectionController.fieldRequired"), ProblemStatus.ERROR)); //$NON-NLS-1$
                }
            }

            if (problems != null && problems.size() > 0) {
                if (existingControlProperties == null) {
                    ControlProperties properties = new ControlProperties();
                    controlToProp.put(control, properties);
                    // store original properties to restore them when error will be corrected
                    properties.originalBgColor = control.getBackground();
                    properties.originalFgColor = control.getForeground();
                    properties.originalToolTip = control.getToolTipText();
                }

                control.setBackground(bgColorError);
                control.setForeground(fgColorError);
                String tooltip = Messages.getString("AbstractElementPropertySectionController.syntaxError"); //$NON-NLS-1$

                for (Problem problem : problems) {
                    tooltip += "\n" + problem.getDescription(); //$NON-NLS-1$
                }
                control.setToolTipText(tooltip);
            } else {
                resetErrorState(control);
            }
        }

        /**
         * DOC amaumont Comment method "resetErrorState".
         * 
         * @param control
         * @param previousProblem
         */
        private void resetErrorState(final Control control) {
            ControlProperties existingControlProperties = controlToProp.get(control);
            if (existingControlProperties != null) {
                control.setToolTipText(existingControlProperties.originalToolTip);
                control.setBackground(existingControlProperties.originalBgColor);
                control.setForeground(existingControlProperties.originalFgColor);
                controlToProp.remove(control);
            }
        }
    }

    /**
     * 
     * Container of original properties of Control. <br/>
     * 
     * $Id: DynamicTabbedPropertySection.java 865 2006-12-06 06:14:57 +0000 (鏄熸湡涓�, 06 鍗佷簩鏈� 2006) bqian $
     * 
     */
    class ControlProperties {

        private Color originalBgColor;

        private Color originalFgColor;

        private String originalToolTip;

        /**
         * DOC amaumont ControlProperties constructor comment.
         */
        public ControlProperties() {
            super();
        }
    }

    protected Command getTextCommandForHelper(String paramName, String text) {
        return new PropertyChangeCommand(elem, paramName, text);
    }

    /**
     * 
     * DOC amaumont DynamicTabbedPropertySection class global comment. Detailled comment <br/>
     * 
     * @author amaumont
     * 
     * $Id: DynamicTabbedPropertySection.java 865 2006-12-06 06:14:57 +0000 (鏄熸湡涓�, 06 鍗佷簩鏈� 2006) bqian $
     * 
     */
    class UndoRedoHelper {

        protected TypedTextCommandExecutor typedTextCommandExecutor;

        /**
         * DOC amaumont Comment method "unregister".
         * 
         * @param control
         */
        public void unregister(Control control) {
            // ControlUtils.removeModifyListener(control, modifyListenerForUndoRedo);
            typedTextCommandExecutor.unregister(control);
        }

        public UndoRedoHelper() {
            this.typedTextCommandExecutor = new TypedTextCommandExecutor() {

                @Override
                public void addNewCommand(Control control) {
                    String name = getParameterName(control);
                    String text = ControlUtils.getText(control);
                    Command cmd = getTextCommandForHelper(name, text);
                    getCommandStack().execute(cmd);
                }

                @Override
                public void updateCommand(Control control) {
                    CommandStack commandStack = getCommandStack();
                    Object[] commands = commandStack.getCommands();

                    if (commands.length == 0 || commandStack.getRedoCommand() != null) {
                        addNewCommand(control);
                    } else {
                        Object lastCommandObject = commands[commands.length - 1];
                        String name = getParameterName(control);
                        if (lastCommandObject instanceof PropertyChangeCommand) {
                            PropertyChangeCommand lastCommand = (PropertyChangeCommand) lastCommandObject;
                            if (name.equals(lastCommand.getPropName()) && (lastCommand.getElement() == elem)) {
                                String text = ControlUtils.getText(control);
                                lastCommand.dispose();
                                commandStack.execute(new PropertyChangeCommand(elem, name, text));
                                // lastCommand.modifyValue(text);
                            }
                        }
                    }
                }

            };

        }

        /**
         * DOC amaumont Comment method "register".
         * 
         * @param control
         */
        public void register(Control control) {
            // ControlUtils.addModifyListener(control, modifyListenerForUndoRedo);
            typedTextCommandExecutor.register(control);
        }
    }

    /**
     * DOC amaumont Comment method "getParameterName".
     * 
     * @param control
     * @return
     */
    public String getParameterName(Control control) {

        String name = (String) control.getData(PARAMETER_NAME);
        if (name == null) { // if the control don't support this property, then take in the list.
            name = (String) hashCurControls.getKey(control);
        }
        if (name == null) {
            throw new IllegalStateException(
                    "parameterName shouldn't be null or you call this method too early ! (control value : '" //$NON-NLS-1$
                            + ControlUtils.getText(control) + "')"); //$NON-NLS-1$
        }
        return name;
    }

    /**
     * Get the command stack of the Gef editor.
     * 
     * @return
     */
    protected CommandStack getCommandStack() {
        Object adapter = part.getTalendEditor().getAdapter(CommandStack.class);
        return (CommandStack) adapter;
    }

    /**
     * Accept Text and StyledText control.
     * 
     * @param labelText
     */
    public void addDragAndDropTarget(final Control textControl) {
        DropTargetListener dropTargetListener = new DropTargetListener() {

            String propertyName = null;

            public void dragEnter(final DropTargetEvent event) {
            }

            public void dragLeave(final DropTargetEvent event) {
            }

            public void dragOperationChanged(final DropTargetEvent event) {
            }

            public void dragOver(final DropTargetEvent event) {
                if (TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
                    propertyName = getParameterName(textControl);
                    for (int i = 0; i < elem.getElementParameters().size(); i++) {
                        IElementParameter param = elem.getElementParameters().get(i);
                        if (param.getName().equals(propertyName)) {
                            if (param.isReadOnly()) {
                                event.detail = DND.ERROR_INVALID_DATA;
                            }
                        }
                    }
                }
            }

            public void drop(final DropTargetEvent event) {
                if (propertyName != null) {
                    String text;
                    if (textControl instanceof StyledText) {
                        text = ((StyledText) textControl).getText() + (String) event.data;
                    } else {
                        text = ((Text) textControl).getText() + (String) event.data;
                    }
                    Command cmd = new PropertyChangeCommand(elem, propertyName, text);
                    getCommandStack().execute(cmd);
                }
            }

            public void dropAccept(final DropTargetEvent event) {
            }
        };

        DropTarget target = new DropTarget(textControl, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
        Transfer[] transfers = new Transfer[] { TextTransfer.getInstance() };
        target.setTransfer(transfers);
        target.addDropListener(dropTargetListener);

    }

    /**
     * Sets the elem.
     * 
     * @param elem the elem to set
     */
    protected void setElem(Element elem) {
        this.elem = elem;
    }

    /**
     * Sets the hashCurControls.
     * 
     * @param hashCurControls the hashCurControls to set
     */
    protected void setHashCurControls(BidiMap hashCurControls) {
        this.hashCurControls = hashCurControls;
    }

    /**
     * Sets the part.
     * 
     * @param part the part to set
     */
    protected void setPart(AbstractMultiPageTalendEditor part) {
        this.part = part;
    }

    /**
     * Sets the section.
     * 
     * @param section the section to set
     */
    protected void setSection(EComponentCategory section) {
        this.section = section;
    }

    /**
     * DOC amaumont Comment method "checkErrors".
     * 
     * @param control must be or extends <code>Text</code> or <code>StyledText</code>
     */
    protected void checkErrorsForPropertiesOnly(Control control) {
        if (this.section == EComponentCategory.BASIC) {
            editionControlHelper.checkErrors(control);
        }
    }

    public abstract void refresh(IElementParameter param, boolean check);

    /**
     * qzhang Comment method "fixedCursorPosition".
     * 
     * @param param
     * @param labelText
     * @param value
     * @param valueChanged
     */
    protected void fixedCursorPosition(IElementParameter param, Control labelText, Object value, boolean valueChanged) {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IWorkbenchPart workbenchPart = page.getActivePart();

        if ((workbenchPart instanceof PropertySheet) || (workbenchPart instanceof JobSettingsView)
                || (workbenchPart instanceof ComponentSettingsView)) {
            Object control = editionControlHelper.undoRedoHelper.typedTextCommandExecutor.getActiveControl();
            if (param.getName().equals(control) && valueChanged && !param.isRepositoryValueUsed()) {
                String previousText = editionControlHelper.undoRedoHelper.typedTextCommandExecutor.getPreviousText2();
                String currentText = (String) value;
                labelText.setFocus();
                ControlUtils.setCursorPosition(labelText, getcursorPosition(previousText, currentText));
            }
        }
    }

    /**
     * qzhang Comment method "getcursorPosition".
     * 
     * @param previousText
     * @param currentText
     * @return
     */
    private int getcursorPosition(String previousText, String currentText) {
        if (previousText.length() == currentText.length() + 1) {
            return getLeftCharPosition(currentText, previousText, false);
        } else if (previousText.length() == currentText.length() - 1) {
            return getLeftCharPosition(previousText, currentText, true);
        }
        return 0;
    }

    /**
     * qzhang Comment method "getLeftCharPosition".
     * 
     * @param previousText
     * @param currentText
     * @return
     */
    private int getLeftCharPosition(String previousText, String currentText, boolean add) {
        int i = 0;
        for (; i < currentText.length() - 1; i++) {
            if (currentText.charAt(i) != previousText.charAt(i)) {
                break;
            }
        }
        if (add) {
            return i + 1;
        } else {
            return i;
        }
    }

    public void openSqlBuilderBuildIn(final ConnectionParameters connParameters, final String propertyName) {
        OpenSQLBuilderDialogJob openDialogJob = new OpenSQLBuilderDialogJob(connParameters, composite, elem, propertyName,
                getCommandStack(), this);
        IWorkbenchSiteProgressService siteps = (IWorkbenchSiteProgressService) part.getSite().getAdapter(
                IWorkbenchSiteProgressService.class);
        siteps.showInDialog(composite.getShell(), openDialogJob);
        openDialogJob.schedule();
    }

    protected ConnectionParameters connParameters;

    private void setConnectionParameters(Element element) {
        String type = getValueFromRepositoryName(element, "TYPE"); //$NON-NLS-1$
        connParameters.setDbType(type);
        String frameWorkKey = getValueFromRepositoryName(element, "FRAMEWORK_TYPE"); //$NON-NLS-1$
        connParameters.setFrameworkType(frameWorkKey);

        String schema = setConnectionParameter(element, connParameters, EConnectionParameterName.SCHEMA.getName());
        connParameters.setSchema(schema);

        String userName = setConnectionParameter(element, connParameters, EConnectionParameterName.USERNAME.getName());
        connParameters.setUserName(userName);

        String password = setConnectionParameter(element, connParameters, EConnectionParameterName.PASSWORD.getName());
        connParameters.setPassword(password);

        String host = setConnectionParameter(element, connParameters, EConnectionParameterName.SERVER_NAME.getName());
        connParameters.setHost(host);

        String port = setConnectionParameter(element, connParameters, EConnectionParameterName.PORT.getName());
        connParameters.setPort(port);
        String datasource = setConnectionParameter(element, connParameters, EConnectionParameterName.DATASOURCE.getName());
        connParameters.setDatasource(datasource);

        String dbName = setConnectionParameter(element, connParameters, EConnectionParameterName.SID.getName());
        connParameters.setDbName(dbName);
        String file = setConnectionParameter(element, connParameters, EConnectionParameterName.FILE.getName());
        connParameters.setFilename(file);
        String dir = setConnectionParameter(element, connParameters, EConnectionParameterName.DIRECTORY.getName());
        connParameters.setDirectory(dir);

        String jdbcProps = setConnectionParameter(element, connParameters, EConnectionParameterName.PROPERTIES_STRING.getName());
        connParameters.setJdbcProperties(jdbcProps);

        String realTableName = null;
        if (EmfComponent.REPOSITORY.equals(elem.getPropertyValue(EParameterName.SCHEMA_TYPE.getName()))) {
            final Object propertyValue = elem.getPropertyValue(EParameterName.REPOSITORY_SCHEMA_TYPE.getName());
            final IMetadataTable metadataTable = dynamicProperty.getRepositoryTableMap().get(propertyValue);
            if (metadataTable != null) {
                realTableName = metadataTable.getTableName();
            }
        }
        connParameters
                .setSchemaName(QueryUtil.getTableName(elem, connParameters.getMetadataTable(), schema, type, realTableName));

    }

    protected void initConnectionParametersWithContext(IElement element, IContext context) {
        connParameters.setDbName(getParameterValueWithContext(element, EConnectionParameterName.SID.getName(), context));
        connParameters.setPassword(getParameterValueWithContext(element, EConnectionParameterName.PASSWORD.getName(), context));
        connParameters.setPort(getParameterValueWithContext(element, EConnectionParameterName.PORT.getName(), context));
        connParameters.setSchema(getParameterValueWithContext(element, EConnectionParameterName.SCHEMA.getName(), context));
        connParameters.setHost(getParameterValueWithContext(element, EConnectionParameterName.SERVER_NAME.getName(), context));
        connParameters.setUserName(getParameterValueWithContext(element, EConnectionParameterName.USERNAME.getName(), context));
        connParameters.setDirectory(getParameterValueWithContext(element, EConnectionParameterName.DIRECTORY.getName(), context));
        connParameters.setFilename(getParameterValueWithContext(element, EConnectionParameterName.FILE.getName(), context));
        connParameters.setJdbcProperties(getParameterValueWithContext(element, EConnectionParameterName.PROPERTIES_STRING
                .getName(), context));
        connParameters
                .setDatasource(getParameterValueWithContext(element, EConnectionParameterName.DATASOURCE.getName(), context));
    }

    private String getParameterValueWithContext(IElement elem, String key, IContext context) {
        if (elem == null || key == null)
            return "";
        String actualKey = connKeyMap.get(key);
        if (actualKey != null) {
            return fetchElementParameterValue(elem, context, actualKey);
        } else {
            return fetchElementParameterValue(elem, context, key);
        }
    }

    /**
     * DOC yexiaowei Comment method "fetchElementParameterValude".
     * 
     * @param elem
     * @param context
     * @param actualKey
     * @return
     */
    private String fetchElementParameterValue(IElement elem, IContext context, String actualKey) {
        IElementParameter elemParam = elem.getElementParameter(actualKey);
        if (elemParam != null) {
            String value = (String) elemParam.getValue();
            if (value != null)
                return ContextParameterUtils.parseScriptContextCode(value, context);
            else
                return "";
        } else {
            return "";
        }
    }

    protected void initConnectionParameters() {

        connParameters = null;

        connParameters = new ConnectionParameters();
        String type = getValueFromRepositoryName(elem, "TYPE"); //$NON-NLS-1$
        connParameters.setDbType(type);

        connParameters.setNode(elem);
        String selectedComponentName = (String) elem.getPropertyValue(EParameterName.UNIQUE_NAME.getName());
        connParameters.setSelectedComponentName(selectedComponentName);
        connParameters.setFieldType(paramFieldType);
        if (elem instanceof Node) {
            connParameters.setMetadataTable(((Node) elem).getMetadataList().get(0));
        }

        connParameters.setSchemaRepository(EmfComponent.REPOSITORY.equals(elem.getPropertyValue(EParameterName.SCHEMA_TYPE
                .getName())));
        connParameters.setFromDBNode(true);

        connParameters.setQuery("");

        List<? extends IElementParameter> list = elem.getElementParameters();
        boolean end = false;
        for (int i = 0; i < list.size() && !end; i++) {
            IElementParameter param = list.get(i);
            if (param.getField() == EParameterFieldType.MEMO_SQL) {
                connParameters.setNodeReadOnly(param.isReadOnly());
                end = true;
            }

        }

        Object value = elem.getPropertyValue("USE_EXISTING_CONNECTION");
        IElementParameter compList = elem.getElementParameterFromField(EParameterFieldType.COMPONENT_LIST);
        if (value != null && (value instanceof Boolean) && ((Boolean) value) && compList != null) {
            Object compValue = compList.getValue();
            Node connectionNode = null;
            if (compValue != null && !compValue.equals("")) {
                List<? extends INode> nodes = part.getProcess().getGraphicalNodes();
                for (INode node : nodes) {
                    if (node.getUniqueName().equals(compValue) && (node instanceof Node)) {
                        connectionNode = (Node) node;
                        break;
                    }
                }
                if (connectionNode != null) {
                    setConnectionParameters(connectionNode);
                }
            }
        } else {
            setConnectionParameters(elem);
        }
        setConnectionParameterNames(elem, connParameters);
    }

    private String setConnectionParameter(Element element, ConnectionParameters connParameters, String repositoryName) {
        String userName = getValueFromRepositoryName(element, repositoryName); //$NON-NLS-1$
        return userName;
    }

    private void setConnectionParameterNames(Element element, ConnectionParameters connParameters) {

        setConnectionParameterName(element, connParameters, EConnectionParameterName.SCHEMA.getName());

        setConnectionParameterName(element, connParameters, EConnectionParameterName.USERNAME.getName());

        setConnectionParameterName(element, connParameters, EConnectionParameterName.PASSWORD.getName());

        setConnectionParameterName(element, connParameters, EConnectionParameterName.SERVER_NAME.getName());

        setConnectionParameterName(element, connParameters, EConnectionParameterName.PORT.getName());
        setConnectionParameterName(element, connParameters, EConnectionParameterName.DATASOURCE.getName());

        setConnectionParameterName(element, connParameters, EConnectionParameterName.SID.getName());
        setConnectionParameterName(element, connParameters, EConnectionParameterName.FILE.getName());
        setConnectionParameterName(element, connParameters, EConnectionParameterName.DIRECTORY.getName());
        setConnectionParameterName(element, connParameters, EConnectionParameterName.PROPERTIES_STRING.getName());

    }

    private void setConnectionParameterName(Element element, ConnectionParameters connParameters, String repositoryName) {
        final String paraNameFromRepositoryName = getParaNameFromRepositoryName(element, repositoryName);
        if (paraNameFromRepositoryName != null) {
            connParameters.getRepositoryNameParaName().put(repositoryName, paraNameFromRepositoryName);
        }
    }

    /**
     * DOC Administrator Comment method "setTextErrorInfo".
     * 
     * @param labelText
     * @param red
     */
    private void setTextErrorInfo(Text labelText, Color red) {
        labelText.setBackground(red);
        labelText.setToolTipText("Value is invalid");
    }

    /**
     * 
     * DOC ggu Comment method "isExtra".
     * 
     * for extra db setting.
     */
    private boolean sameExtraParameter(IElementParameter param) {
        // for job settings extra.(feature 2710)
        if (curParameter != null) {
            boolean extra = JobSettingsConstants.isExtraParameter(this.curParameter.getName());
            boolean paramFlag = JobSettingsConstants.isExtraParameter(param.getName());
            return extra == paramFlag;
        }
        return true;
    }

    /**
     * DOC qzhang Comment method "openSQLBuilder".
     * 
     * @param repositoryType
     * @param propertyName
     * @param query
     */
    protected String openSQLBuilder(String repositoryType, String propertyName, String query) {
        // boolean status = true;
        if (repositoryType.equals(EmfComponent.BUILTIN)) {
            connParameters.setQuery(query);
            if (connParameters.isShowConfigParamDialog()) {
                initConnectionParametersWithContext(elem, part.getTalendEditor().getProcess().getContextManager()
                        .getDefaultContext());
                openSqlBuilderBuildIn(connParameters, propertyName);
            }

        } else if (repositoryType.equals(EmfComponent.REPOSITORY)) {
            String repositoryName2 = ""; //$NON-NLS-1$
            IElementParameter memoParam = elem.getElementParameter(propertyName);

            IElementParameter repositoryParam = elem.getElementParameterFromField(EParameterFieldType.PROPERTY_TYPE, memoParam
                    .getCategory());
            if (repositoryParam != null) {
                IElementParameter itemFromRepository = repositoryParam.getChildParameters().get(
                        EParameterName.REPOSITORY_PROPERTY_TYPE.getName());
                String value = (String) itemFromRepository.getValue();
                for (String key : this.dynamicProperty.getRepositoryConnectionItemMap().keySet()) {

                    if (key.equals(value)) {
                        repositoryName2 = this.dynamicProperty.getRepositoryConnectionItemMap().get(key).getProperty().getLabel();

                    }
                }
            }

            // When no repository avaiable on "Repository" mode, open a MessageDialog.
            if (repositoryName2 == null || repositoryName2.length() == 0) {
                MessageDialog.openError(composite.getShell(), Messages.getString("NoRepositoryDialog.Title"), Messages //$NON-NLS-1$
                        .getString("NoRepositoryDialog.Text")); //$NON-NLS-1$
                return null;
            }
            String key = this.part.getTalendEditor().getProcess().getName() + ((Node) elem).getUniqueName() + repositoryName2;
            final SQLBuilderDialog builderDialog = sqlbuilers.get(key);
            if (!composite.isDisposed() && builderDialog != null && builderDialog.getShell() != null
                    && !builderDialog.getShell().isDisposed()) {
                builderDialog.getShell().setActive();
            } else {
                connParameters.setRepositoryName(repositoryName2);
                Shell parentShell = new Shell(composite.getShell().getDisplay());
                TextUtil.setDialogTitle(this.part.getTalendEditor().getProcess().getName(), (String) ((Node) elem)
                        .getElementParameter("LABEL").getValue(), elem.getElementName());
                part.addPropertyListener(new IPropertyListener() {

                    /*
                     * (non-Javadoc)
                     * 
                     * @see org.eclipse.ui.IPropertyListener#propertyChanged(java.lang.Object, int)
                     */
                    public void propertyChanged(Object source, int propId) {

                    }

                });
                SQLBuilderDialog dial = new SQLBuilderDialog(parentShell);
                UIUtils.addSqlBuilderDialog(part.getTalendEditor().getProcess().getName(), dial);
                connParameters.setQuery(query);
                dial.setConnParameters(connParameters);
                sqlbuilers.put(key, dial);
                if (Window.OK == dial.open()) {
                    if (!composite.isDisposed() && !connParameters.isNodeReadOnly()) {
                        String sql = connParameters.getQuery();
                        sql = TalendTextUtils.addSQLQuotes(sql);
                        return sql;
                    }
                }
            }
        }
        return null;
    }

    public void dispose() {
        if (widgetFactory != null) {
            widgetFactory.dispose();
        }
        widgetFactory = null;
        sqlbuilers.clear();
        sqlbuilers = null;
        dynamicProperty = null;
        composite = null;
        hashCurControls = null;
        elem = null;
        part = null;
        section = null;
        editionControlHelper = null;
        curParameter = null;
    }

}
