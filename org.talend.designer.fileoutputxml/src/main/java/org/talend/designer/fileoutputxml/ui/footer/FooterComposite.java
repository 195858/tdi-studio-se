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
package org.talend.designer.fileoutputxml.ui.footer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.talend.designer.fileoutputxml.i18n.Messages;
import org.talend.designer.fileoutputxml.managers.FOXManager;

/**
 * amaumont class global comment. Detailled comment <br/>
 * 
 * $Id: FooterComposite.java,v 1.1 2007/06/12 07:20:39 gke Exp $
 * 
 */
public class FooterComposite extends Composite {

    private FOXManager foxManager;

    /**
     * amaumont FooterComposite constructor comment.
     * 
     * @param parent
     * @param style
     * @param mapperManager
     */
    public FooterComposite(Composite parent, int style, FOXManager generatorManager) {
        this(parent, style);
        this.foxManager = generatorManager;
    }

    public FooterComposite(Composite parent, int style) {
        super(parent, style);
        createComponents();
    }

    /**
     * amaumont Comment method "createComponents".
     */
    private void createComponents() {

        GridData footerCompositeGridData = new GridData(GridData.FILL_HORIZONTAL);
        this.setLayoutData(footerCompositeGridData);

        FormLayout formLayout = new FormLayout();
        this.setLayout(formLayout);

        Button okButton = new Button(this, SWT.NONE);
        okButton.setText(Messages.getString("FooterComposite.0")); //$NON-NLS-1$
        FormData okFormData = new FormData();
        Point minSize = okButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
        okFormData.width = Math.max(IDialogConstants.BUTTON_WIDTH, minSize.x);

        okButton.setLayoutData(okFormData);
        okButton.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                foxManager.getUiManager().closeFOX(SWT.OK);
            }

        });

        Button cancelButton = new Button(this, SWT.NONE);
        cancelButton.setText(Messages.getString("FooterComposite.1")); //$NON-NLS-1$
        cancelButton.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                foxManager.getUiManager().closeFOX(SWT.CANCEL);
            }

        });

        FormData cancelFormData = new FormData();
        minSize = cancelButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
        cancelFormData.width = Math.max(IDialogConstants.BUTTON_WIDTH, minSize.x);
        cancelButton.setLayoutData(cancelFormData);

        Button autoMapButton = new Button(this, SWT.NONE);
        autoMapButton.setToolTipText(Messages.getString("FooterComposite.AutoMapTip"));
        autoMapButton.setText(Messages.getString("FooterComposite.AutoMap")); //$NON-NLS-1$
        autoMapButton.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                foxManager.getUiManager().autoMap();
            }

        });
        FormData autoMapFormData = new FormData();
        minSize = autoMapButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
        autoMapFormData.width = Math.max(IDialogConstants.BUTTON_WIDTH, minSize.x);
        autoMapButton.setLayoutData(autoMapFormData);

        cancelFormData.right = new FormAttachment(100, -5);
        okFormData.right = new FormAttachment(cancelButton, -5);
        autoMapFormData.left = new FormAttachment(0, 5);

    }
}
