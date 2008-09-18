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
package org.talend.designer.core.ui.action;

import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.talend.commons.ui.swt.formtools.LabelledText;
import org.talend.core.model.process.IElementParameter;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.ui.editor.cmd.PropertyChangeCommand;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.nodes.NodePart;

/**
 * 
 * DOC YeXiaowei class global comment. Detailled comment
 */
public class ParallelExecutionAction extends SelectionAction {

    public static final String ID = "org.talend.designer.core.ui.editor.action.ParallelExecutionAction"; //$NON-NLS-1$

    private static final String TEXT_PARALLEL = Messages.getString("ParallelExecutionCommand.Parallel");

    private boolean parallelEnable; 

    private String numberParallel = "0";

    private Node node;

    public ParallelExecutionAction(IWorkbenchPart part) {
        super(part);
        setId(ID);
        setText(TEXT_PARALLEL);
    }

    @Override
    protected boolean calculateEnabled() {
        if (getSelectedObjects() == null || getSelectedObjects().isEmpty()) {
            return false;
        }

        List parts = getSelectedObjects();
        if (parts.size() != 1) {
            return false;
        }

        Object o = parts.get(0);
        if (o instanceof NodePart) {
            NodePart nodePart = (NodePart) o;
            node = (Node) nodePart.getModel();
            IElementParameter enableParallelizeParameter = node.getElementParameter(EParameterName.PARALLELIZE.getName());
            if (enableParallelizeParameter != null) {
                parallelEnable = (Boolean) enableParallelizeParameter.getValue();
            }
            return parallelEnable;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        
        IElementParameter enableParallelizeParameter = node.getElementParameter(EParameterName.PARALLELIZE.getName());
        if (enableParallelizeParameter != null) {
            parallelEnable = (Boolean) enableParallelizeParameter.getValue();
        }
        IElementParameter numberParallelizeParameter = node.getElementParameter(EParameterName.PARALLILIZE_NUMBER.getName());
        if (numberParallelizeParameter != null) {
            numberParallel = (String) numberParallelizeParameter.getValue();
        }
        
        Dialog dialog = new ParallelDialog(getWorkbenchPart().getSite().getShell());
        if (dialog.open() == Dialog.OK) {
            Command command = new PropertyChangeCommand(node, EParameterName.PARALLELIZE.getName(), parallelEnable);
            execute(command);
        }
    }

    /**
     * 
     * DOC YeXiaowei ParallelExecutionAction class global comment. Detailled comment
     */
    class ParallelDialog extends Dialog {

        private Button enableButton;

        private LabelledText numberText;

        protected ParallelDialog(Shell parentShell) {
            super(parentShell);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
         */
        @Override
        protected Control createDialogArea(Composite parent) {

            Composite bgComposite = new Composite(parent, SWT.NULL);
            bgComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            bgComposite.setLayout(gridLayout);

            enableButton = new Button(bgComposite, SWT.CHECK);
            GridData data = new GridData(GridData.FILL_HORIZONTAL);
            data.horizontalSpan = 2;
            enableButton.setLayoutData(data);
            enableButton.setText(Messages.getString("ParallelExecutionCommand.enableParallel"));

            numberText = new LabelledText(bgComposite, Messages.getString("ParallelExecutionCommand.numberParallel"), true);

            enableButton.addSelectionListener(new SelectionAdapter() {

                /*
                 * (non-Javadoc)
                 * 
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                @Override
                public void widgetSelected(SelectionEvent e) {
                    numberText.setEditable(enableButton.getSelection());
                }
            });

            enableButton.setSelection(parallelEnable);
            numberText.setText(numberParallel);
            numberText.setEditable(parallelEnable);

            return bgComposite;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.dialogs.Dialog#okPressed()
         */
        @Override
        protected void okPressed() {
            if (numberTextValid()) {
                setParametersValue();
                super.okPressed();
            } else {
                MessageDialog.openError(null, "Talend", Messages.getString("ParallelExecutionCommand.numberInvalid"));
            }
        }

        /**
         * DOC YeXiaowei Comment method "setParametersValue".
         */
        private void setParametersValue() {
            IElementParameter enableParallelizeParameter = node.getElementParameter(EParameterName.PARALLELIZE.getName());
            if (enableParallelizeParameter != null) {
                enableParallelizeParameter.setValue(enableButton.getSelection());
                parallelEnable = enableButton.getSelection();
            }
            IElementParameter numberParallelizeParameter = node.getElementParameter(EParameterName.PARALLILIZE_NUMBER.getName());
            if (numberParallelizeParameter != null) {
                numberParallelizeParameter.setValue(numberText.getText());
            }
        }

        /**
         * DOC YeXiaowei Comment method "numberTextValid".
         * 
         * @return
         */
        private boolean numberTextValid() {
            String text = numberText.getText().trim();
            if (text == null || text.equals("")) {
                return false;
            }
            try {
                int number = Integer.parseInt(text);
                return number >= 0;
            } catch (Exception e) {
                return false;
            }
        }

    }
}
