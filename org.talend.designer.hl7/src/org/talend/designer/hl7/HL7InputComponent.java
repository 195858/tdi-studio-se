// ============================================================================
//
// Copyright (C) 2006-2010 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.hl7;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.model.components.IODataComponent;
import org.talend.core.model.metadata.ColumnNameChanged;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.process.AbstractExternalNode;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.IComponentDocumentation;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.IExternalData;

/**
 * DOC Administrator class global comment. Detailled comment
 */
public class HL7InputComponent extends AbstractExternalNode {

    public static final String ROOT = "ROOT"; //$NON-NLS-1$

    public static final String GROUP = "GROUP"; //$NON-NLS-1$

    public static final String LOOP = "LOOP"; //$NON-NLS-1$

    public static final String PATH = "PATH"; //$NON-NLS-1$

    public static final String VALUE = "VALUE"; //$NON-NLS-1$

    public static final String TYPE = "TYPE"; //$NON-NLS-1$

    public static final String COLUMN = "COLUMN"; //$NON-NLS-1$

    public static final String ATTRIBUTE = "ATTRIBUTE"; //$NON-NLS-1$s

    public static final String ORDER = "ORDER"; //$NON-NLS-1$

    private HL7Main hl7main;

    @Override
    protected void renameMetadataColumnName(String conectionName, String oldColumnName, String newColumnName) {
    }

    public IComponentDocumentation getComponentDocumentation(String componentName, String tempFolderPath) {
        return null;
    }

    public IExternalData getExternalData() {
        return null;
    }

    public IExternalData getTMapExternalData() {
        return null;
    }

    public void initialize() {

    }

    public void loadDataIn(InputStream inputStream, Reader reader) throws IOException, ClassNotFoundException {

    }

    public void loadDataOut(OutputStream out, Writer writer) throws IOException {

    }

    public int open(Display display) {
        hl7main = new HL7Main(this);
        Shell shell = hl7main.createUI(display);
        while (!shell.isDisposed()) {
            try {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            } catch (Throwable e) {
                if (hl7main.isStandAloneMode()) {
                    e.printStackTrace();
                } else {
                    ExceptionHandler.process(e);
                }
            }
        }
        if (hl7main.isStandAloneMode()) {
            display.dispose();
        }
        return hl7main.getHl7Manager().getUiManager().getUiResponse();
    }

    public int open(Composite parent) {
        return open(parent.getDisplay());
    }

    public void renameInputConnection(String oldName, String newName) {
        List<Map<String, String>> listRoot = (List<Map<String, String>>) this.getElementParameter(ROOT).getValue();
        boolean flagRoot = false;
        String schemaId = oldName + ":";

        for (Map<String, String> map : listRoot) {
            String rowName = map.get(COLUMN);
            if (rowName == null) {
                continue;
            }
            if (rowName.equals(oldName)) {
                map.put(COLUMN, newName);
                flagRoot = true;
            } else if (rowName.startsWith(schemaId)) {
                rowName = newName + rowName.substring(rowName.indexOf(":"));
                map.put(COLUMN, rowName);
                flagRoot = true;
            }
        }
        if (flagRoot) {
            this.getElementParameter(ROOT).setValue(listRoot);
        }
    }

    public void renameOutputConnection(String oldName, String newName) {

    }

    public void setExternalData(IExternalData persistentData) {

    }

    public IMetadataTable getMetadataTable() {
        try {
            return getMetadataList().get(0);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")//$NON-NLS-1$
    public List<Map<String, String>> getTableList(String paraName) {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        List<IElementParameter> eps = (List<IElementParameter>) this.getElementParameters();
        if (eps == null) {
            return list;
        }
        for (int i = 0; i < eps.size(); i++) {
            IElementParameter parameter = eps.get(i);
            if (parameter.getField() == EParameterFieldType.TABLE && parameter.getName().equals(paraName)) {
                list = (List<Map<String, String>>) parameter.getValue();
                break;
            }
        }
        return list;
    }

    public boolean istFileInputHL7() {
        return getComponent().getName().equals("tFileInputHL7"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public boolean isHL7Output() {
        return getComponent().getName().equals("tHL7Output");//$NON-NLS-1$
    }

    @SuppressWarnings("unchecked")//$NON-NLS-1$
    public boolean setTableElementParameter(List<Map<String, String>> epsl, String paraName) {
        List<IElementParameter> eps = (List<IElementParameter>) this.getElementParameters();
        boolean result = true;
        for (int i = 0; i < eps.size(); i++) {
            IElementParameter parameter = eps.get(i);
            if (parameter.getField() == EParameterFieldType.TABLE && parameter.getName().equals(paraName)) {
                List<Map<String, String>> newValues = new ArrayList<Map<String, String>>();
                for (Map<String, String> map : epsl) {
                    Map<String, String> newMap = new HashMap<String, String>();
                    newMap.putAll(map);
                    newValues.add(newMap);
                }

                if (result) {
                    parameter.setValue(newValues);
                }
                break;
            }
        }
        return result;
    }

    public void setValueToParameter(String paraName, Object value) {
        IElementParameter parameter = this.getElementParameter(paraName); //$NON-NLS-N$
        if (parameter != null && value != null) {
            parameter.setValue(value);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.talend.core.model.process.AbstractExternalNode#metadataInputChanged(org.talend.core.model.components.
     * IODataComponent, java.lang.String)
     */
    @Override
    public void metadataInputChanged(IODataComponent dataComponent, String connectionToApply) {
        super.metadataInputChanged(dataComponent, connectionToApply);
        List<Map<String, String>> listRoot = (List<Map<String, String>>) this.getElementParameter(ROOT).getValue();
        boolean flagRoot = false;

        String schemaId = ""; //$NON-NLS-1$
        if (isHL7Output()) {
            schemaId = dataComponent.getConnection().getMetadataTable().getLabel() + ":"; //$NON-NLS-1$
        }

        for (ColumnNameChanged col : dataComponent.getColumnNameChanged()) {
            for (Map<String, String> map : listRoot) {
                if (map.get(COLUMN).equals(schemaId + col.getOldName())) {
                    map.put(COLUMN, schemaId + col.getNewName());
                    flagRoot = true;
                }
            }
        }

        if (flagRoot) {
            this.getElementParameter(ROOT).setValue(listRoot);
        }
    }

}
