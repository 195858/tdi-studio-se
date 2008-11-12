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
package org.talend.designer.alfrescooutput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.SystemException;
import org.talend.core.model.components.IODataComponent;
import org.talend.core.model.process.AbstractExternalNode;
import org.talend.core.model.process.IComponentDocumentation;
import org.talend.core.model.process.IExternalData;
import org.talend.core.model.temp.ECodePart;
import org.talend.designer.alfrescooutput.data.AlfrescoOutputModelManager;
import org.talend.designer.codegen.ICodeGeneratorService;

/**
 * Component class of tAlfrescoOutput
 * 
 * @author Marc Dutoo - Open Wide SA
 * 
 * $Id: AlfrescoOutputComponent.java,v 1.1 2008/10/07 21:27:31 mdutoo Exp $
 * 
 */
public class AlfrescoOutputComponent extends AbstractExternalNode {
    
    private AlfrescoOutputModelManager modelManager; // created on open()

	private AlfrescoOutputManager alfrescoOutputManager;

	public AlfrescoOutputComponent() {
        super();
    }

    public AlfrescoOutputModelManager getModelManager() {
		return modelManager;
	}

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IExternalNode#initialize()
     */
    public void initialize() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IExternalNode#getComponentDocumentation(java.lang.String, java.lang.String)
     */
    public IComponentDocumentation getComponentDocumentation(String componentName, String tempFolderPath) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IExternalNode#getExternalData()
     */
    public IExternalData getExternalData() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IExternalNode#loadDataIn(java.io.InputStream, java.io.Reader)
     */
    public void loadDataIn(InputStream inputStream, Reader reader) throws IOException, ClassNotFoundException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IExternalNode#loadDataOut(java.io.OutputStream, java.io.Writer)
     */
    public void loadDataOut(OutputStream out, Writer writer) throws IOException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IExternalNode#open(org.eclipse.swt.widgets.Composite)
     */
    public int open(Composite parent) {
        return open(parent.getDisplay());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IExternalNode#open(org.eclipse.swt.widgets.Display)
     */
    public int open(Display display) {
        alfrescoOutputManager = new AlfrescoOutputManager(this);
        Shell shell = alfrescoOutputManager.createUI(display);
        while (!shell.isDisposed()) {
            try {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            } catch (Throwable e) {
                ExceptionHandler.process(e);
            }
        }
        return alfrescoOutputManager.getAlfrescoModelDialog().getReturnCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IExternalNode#setExternalData(org.talend.core.model.process.IExternalData)
     */
    public void setExternalData(IExternalData persistentData) {
        // do nothing here
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.AbstractExternalNode#renameMetadataColumnName(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    protected void renameMetadataColumnName(String conectionName, String oldColumnName, String newColumnName) {
    	// used in super's metadataInputChanged() and metadataOutputChanged()
    	// but since we don't need them, we don't need renameMetadataColumnName() either
    }

    @Override
    public void metadataInputChanged(IODataComponent dataComponent, String connectionToApply) {
    	// column mappings are automatically remapped by talend on change
    	// since they are done using COLUMN_LIST fields
    }

    @Override
    public void metadataOutputChanged(IODataComponent dataComponent, String connectionToApply) {
    	// column mappings are automatically remapped by talend on change
    	// since they are done using COLUMN_LIST fields
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.INode#getGeneratedCode()
     */
    public String getGeneratedCode() {
        try {
            ICodeGeneratorService service = AlfrescoOutputPlugin.getDefault().getCodeGeneratorService();
            return service.createCodeGenerator().generateComponentCode(this, ECodePart.MAIN);
        } catch (SystemException e) {
            ExceptionHandler.process(e);
        }
        return "";
    }

    public void renameInputConnection(String oldName, String newName) {

    }

    public void renameOutputConnection(String oldName, String newName) {

    }

}
