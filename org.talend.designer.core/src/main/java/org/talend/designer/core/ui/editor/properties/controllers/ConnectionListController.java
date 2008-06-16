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

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.fieldassist.DecoratedField;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IControlCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.Element;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.properties.tab.IDynamicProperty;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.ui.editor.cmd.PropertyChangeCommand;
import org.talend.designer.core.ui.editor.connections.Connection;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.process.Process;

/**
 * bqian Connection List controller <br/>
 * 
 * $Id: ColumnListController.java 1 2007-6-2 下午02:04:32 +0000 (下午02:04:32) $
 * 
 */
public class ConnectionListController extends AbstractElementPropertySectionController {

    /**
     * dev ColumnListController constructor comment.
     * 
     * @param parameterBean
     */
    public ConnectionListController(IDynamicProperty dp) {
        super(dp);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.ui.editor.properties2.editors.AbstractElementPropertySectionController#createCommand()
     */
    public Command createCommand(SelectionEvent selectionEvent) {
        Assert.isTrue(selectionEvent.getSource() instanceof CCombo);

        CCombo combo = (CCombo) selectionEvent.getSource();
        String paramName = (String) combo.getData(PARAMETER_NAME);
        IElementParameter param = elem.getElementParameter(paramName);

        String comboValue = new String(""); //$NON-NLS-1$

        for (int j = 0; j < param.getListItemsValue().length; j++) {
            if (combo.getText().equals(param.getListItemsDisplayName()[j])) {
                comboValue = (String) param.getListItemsValue()[j];
            }
        }
        if (comboValue.equals(param.getValue())) { // same value so no need to do anything
            return null;
        }

        return new PropertyChangeCommand(elem, paramName, comboValue);
    }

    IControlCreator cbCtrl = new IControlCreator() {

        public Control createControl(final Composite parent, final int style) {
            CCombo cb = new CCombo(parent, style);
            return cb;
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.ui.editor.properties2.editors.AbstractElementPropertySectionController#createControl()
     */
    @Override
    public Control createControl(final Composite subComposite, final IElementParameter param, final int numInRow,
            final int nbInRow, final int top, final Control lastControl) {
        if (param.getDisplayName().startsWith(Messages.KEY_NOT_FOUND_PREFIX)) {
            param.setDisplayName(EParameterName.CONNECTION_LIST.getDisplayName());
        }

        DecoratedField dField = new DecoratedField(subComposite, SWT.BORDER, cbCtrl);
        if (param.isRequired()) {
            FieldDecoration decoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
                    FieldDecorationRegistry.DEC_REQUIRED);
            dField.addFieldDecoration(decoration, SWT.RIGHT | SWT.TOP, false);
        }

        Control cLayout = dField.getLayoutControl();
        CCombo combo = (CCombo) dField.getControl();
        FormData data;
        if (param.getParentParameter() != null) {
            combo.setData(PARAMETER_NAME, param.getParentParameter().getName() + ":" + param.getName());
        } else {
            combo.setData(PARAMETER_NAME, param.getName());
        }
        combo.setEditable(false);
        cLayout.setBackground(subComposite.getBackground());
        combo.setEnabled(!param.isReadOnly());
        combo.addSelectionListener(listenerSelection);
        if (elem instanceof Node) {
            combo.setToolTipText(VARIABLE_TOOLTIP + param.getVariableName());
        }

        CLabel labelLabel = getWidgetFactory().createCLabel(subComposite, param.getDisplayName());
        data = new FormData();
        if (lastControl != null) {
            data.left = new FormAttachment(lastControl, 0);
        } else {
            data.left = new FormAttachment((((numInRow - 1) * MAX_PERCENT) / nbInRow), 0);
        }
        data.top = new FormAttachment(0, top);
        labelLabel.setLayoutData(data);
        if (numInRow != 1) {
            labelLabel.setAlignment(SWT.RIGHT);
        }
        // *********************
        data = new FormData();
        int currentLabelWidth = STANDARD_LABEL_WIDTH;
        GC gc = new GC(labelLabel);
        Point labelSize = gc.stringExtent(param.getDisplayName());
        gc.dispose();

        if ((labelSize.x + ITabbedPropertyConstants.HSPACE) > currentLabelWidth) {
            currentLabelWidth = labelSize.x + ITabbedPropertyConstants.HSPACE;
        }

        if (numInRow == 1) {
            if (lastControl != null) {
                data.left = new FormAttachment(lastControl, currentLabelWidth);
            } else {
                data.left = new FormAttachment(0, currentLabelWidth);
            }

        } else {
            data.left = new FormAttachment(labelLabel, 0, SWT.RIGHT);
        }
        data.top = new FormAttachment(0, top);
        cLayout.setLayoutData(data);
        Point initialSize = dField.getLayoutControl().computeSize(SWT.DEFAULT, SWT.DEFAULT);

        // **********************
        hashCurControls.put(param.getName(), combo);

        dynamicProperty.setCurRowSize(initialSize.y + ITabbedPropertyConstants.VSPACE);
        updateConnectionList(elem, param);
        combo.setItems(param.getListItemsDisplayName());

        if (param.getValue() != null) {
            combo.setText((String) param.getValue());
        }

        return cLayout;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.ui.editor.properties.controllers.AbstractElementPropertySectionController#estimateRowSize(org.eclipse.swt.widgets.Composite,
     * org.talend.core.model.process.IElementParameter)
     */
    @Override
    public int estimateRowSize(Composite subComposite, IElementParameter param) {
        DecoratedField dField = new DecoratedField(subComposite, SWT.BORDER, cbCtrl);
        Point initialSize = dField.getLayoutControl().computeSize(SWT.DEFAULT, SWT.DEFAULT);
        dField.getLayoutControl().dispose();

        return initialSize.y + ITabbedPropertyConstants.VSPACE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent evt) {
        // do nothing here
    }

    SelectionListener listenerSelection = new SelectionAdapter() {

        public void widgetSelected(SelectionEvent event) {
            // updateRepositoryList();
            Command cmd = createCommand(event);
            executeCommand(cmd);
        }
    };

    @Override
    public void refresh(IElementParameter param, boolean check) {
        CCombo combo = (CCombo) hashCurControls.get(param.getName());
        if (combo == null || combo.isDisposed()) {
            return;
        }
        updateConnectionList(elem, param);

        String[] curConnectionNameList = param.getListItemsDisplayName();
        String[] curConnectionValueList = (String[]) param.getListItemsValue();

        Object value = param.getValue();
        boolean listContainValue = false;
        int numValue = 0;
        for (int i = 0; i < curConnectionValueList.length && !listContainValue; i++) {
            if (curConnectionValueList[i].equals(value)) {
                listContainValue = true;
                numValue = i;
            }
        }

        combo.setItems(curConnectionNameList);
        if (!listContainValue) {
            if (curConnectionNameList.length > 0) {
                elem.setPropertyValue(param.getName(), curConnectionNameList[0]);
                combo.setText(curConnectionNameList[0]);
            }
        } else {
            combo.setText(curConnectionNameList[numValue]);
        }

    }

    public static void renameConnection(String oldConnectionName, String newConnectionName, List<Node> nodesToUpdate) {
        for (Node curNode : nodesToUpdate) {
            renameConnectionInElement(oldConnectionName, newConnectionName, curNode);
            for (Connection connection : (List<Connection>) curNode.getOutgoingConnections()) {
                renameConnectionInElement(oldConnectionName, newConnectionName, connection);
            }
        }
    }

    private static void renameConnectionInElement(String oldConnectionName, String newConnectionName, Element elem) {
        for (IElementParameter curParam : elem.getElementParameters()) {
            if (curParam.getField().equals(EParameterFieldType.CONNECTION_LIST)) {
                if (oldConnectionName.equals(curParam.getValue())) {
                    curParam.setValue(newConnectionName);
                }
            } else if (curParam.getField().equals(EParameterFieldType.TABLE)) {
                final Object[] itemsValue = curParam.getListItemsValue();
                for (int i = 0; i < itemsValue.length; i++) {
                    if (itemsValue[i] instanceof IElementParameter) {
                        IElementParameter param = (IElementParameter) itemsValue[i];
                        if (param.getField().equals(EParameterFieldType.CONNECTION_LIST)) {
                            List<Map<String, Object>> tableValues = (List<Map<String, Object>>) curParam.getValue();
                            for (Map<String, Object> curLine : tableValues) {
                                Object value = curLine.get(param.getName());
                                if (value instanceof Integer) {
                                    String connectionName = (String) param.getListItemsValue()[(Integer) value];
                                    if (connectionName.equals(oldConnectionName)) {
                                        // note: change from "Integer" value stored to "String" value
                                        curLine.put(param.getName(), newConnectionName);
                                    }
                                } else if (value instanceof String) {
                                    curLine.put(param.getName(), newConnectionName);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void updateConnectionList(Process process) {
        for (Node node : (List<Node>) process.getGraphicalNodes()) {
            for (IElementParameter param : node.getElementParametersWithChildrens()) {
                if (param.getField().equals(EParameterFieldType.CONNECTION_LIST)) {
                    updateConnectionList(node, param);
                }
            }
        }

    }

    public static void updateConnectionList(Element elem, IElementParameter param) {
        IConnection[] connections;
        Node source = null;
        if (elem instanceof Node) {
            source = ((Node) elem);
        } else if (elem instanceof Connection) {
            source = ((Connection) elem).getSource();
        } else {
            return;
        }
        String filter = param.getFilter();

        if (filter.startsWith("INPUT:")) {
            Set<IConnection> conns = new HashSet<IConnection>();
            conns.addAll(source.getIncomingConnections());
            String[] f = filter.substring("INPUT:".length()).split("\\|"); //$NON-NLS-1$
            List<String> filterArray = new ArrayList<String>(f.length);
            for (int i = 0; i < f.length; i++) {
                filterArray.add(f[i].trim());
            }

            for (Iterator<IConnection> iter = conns.iterator(); iter.hasNext();) {
                IConnection con = iter.next();
                if (!filterArray.contains(con.getLineStyle().toString())) {
                    iter.remove();
                }
            }
            connections = conns.toArray(new IConnection[conns.size()]);
        } else {
            connections = source.getProcess().getAllConnections(filter);
        }

        String[] connectionNames = new String[connections.length];
        for (int i = 0; i < connectionNames.length; i++) {
            connectionNames[i] = connections[i].getUniqueName();
        }

        Arrays.sort(connectionNames);

        String[] connectionNameList = connectionNames;
        String[] connectionValueList = connectionNames;

        param.setListItemsDisplayName(connectionNameList);
        param.setListItemsValue(connectionValueList);

        if (!ArrayUtils.contains(connectionValueList, param.getValue())) {
            param.setValue("");
        }
    }
}
