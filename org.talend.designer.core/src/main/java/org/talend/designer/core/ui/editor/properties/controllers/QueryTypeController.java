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

import java.util.List;
import java.util.Map;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.DecoratedField;
import org.eclipse.jface.fieldassist.IControlCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.builder.connection.Query;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.Element;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.EmfComponent;
import org.talend.designer.core.ui.editor.cmd.PropertyChangeCommand;
import org.talend.designer.core.ui.editor.cmd.QueryGuessCommand;
import org.talend.designer.core.ui.editor.cmd.RepositoryChangeQueryCommand;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.properties.controllers.generator.IDynamicProperty;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.ui.dialog.RepositoryReviewDialog;

/**
 * DOC nrousseau class global comment. Detailled comment
 */
public class QueryTypeController extends AbstractRepositoryController {

    private static final String GUESS_QUERY_NAME = "Guess Query";

    /**
     * DOC nrousseau QueryTypeController constructor comment.
     * 
     * @param dp
     */
    public QueryTypeController(IDynamicProperty dp) {
        super(dp);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.ui.editor.properties.controllers.AbstractRepositoryController#createControl(org.eclipse.swt.widgets.Composite,
     * org.talend.core.model.process.IElementParameter, int, int, int, org.eclipse.swt.widgets.Control)
     */
    @Override
    public Control createControl(Composite subComposite, IElementParameter param, int numInRow, int nbInRow, int top,
            Control lastControl) {
        Control lastControlUsed = super.createControl(subComposite, param, numInRow, nbInRow, top, lastControl);

        IElementParameter queryStoreTypeParameter = param.getChildParameters().get(EParameterName.QUERYSTORE_TYPE.getName());
        if (queryStoreTypeParameter != null) {
            String queryStoreType = (String) queryStoreTypeParameter.getValue();
            if (queryStoreType != null && queryStoreType.equals(EmfComponent.BUILTIN)) {
                lastControlUsed = addGuessQueryButton(subComposite, param, lastControlUsed, numInRow, top);
            }
        }

        return lastControlUsed;
    }

    private Control addGuessQueryButton(Composite subComposite, IElementParameter param, Control lastControl, int numInRow,
            int top) {
        final DecoratedField dField1 = new DecoratedField(subComposite, SWT.PUSH, new IControlCreator() {

            public Control createControl(Composite parent, int style) {
                return new Button(parent, style);
            }
        });
        Button guessQueryButton = null;
        Control buttonControl = dField1.getLayoutControl();
        guessQueryButton = (Button) dField1.getControl();
        guessQueryButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        buttonControl.setBackground(subComposite.getBackground());
        guessQueryButton.setEnabled(true);
        guessQueryButton.setData(NAME, GUESS_QUERY_NAME);
        guessQueryButton.setData(PARAMETER_NAME, param.getName());
        guessQueryButton.setText(GUESS_QUERY_NAME);

        FormData data1 = new FormData();
        data1.left = new FormAttachment(lastControl, 0);
        data1.top = new FormAttachment(0, top);
        data1.height = STANDARD_HEIGHT + 2;

        buttonControl.setLayoutData(data1);
        guessQueryButton.addSelectionListener(listenerSelection);
        return buttonControl;
    }

    private IElementParameter getQueryTextElementParameter(Element elem) {
        for (IElementParameter param : (List<IElementParameter>) elem.getElementParameters()) {
            if (param.getField() == EParameterFieldType.MEMO_SQL) {
                return param;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.ui.editor.properties.controllers.AbstractRepositoryController#createButtonCommand(org.eclipse.swt.widgets.Button)
     */
    @Override
    protected Command createButtonCommand(Button button) {
        if (button.getData(NAME).equals(GUESS_QUERY_NAME)) {
            return getGuessQueryCommand();
        }
        if (button.getData(NAME).equals(REPOSITORY_CHOICE)) {
            RepositoryReviewDialog dialog = new RepositoryReviewDialog(button.getShell(),
                    ERepositoryObjectType.METADATA_CON_QUERY, null);
            if (dialog.open() == RepositoryReviewDialog.OK) {
                RepositoryNode node = dialog.getResult();
                while (node.getObject().getProperty().getItem() == null
                        || (!(node.getObject().getProperty().getItem() instanceof ConnectionItem))) {
                    node = node.getParent();
                }
                String id = node.getObject().getProperty().getId();
                String name = dialog.getResult().getObject().getLabel();
                String paramName = (String) button.getData(PARAMETER_NAME);
                IElementParameter param = elem.getElementParameter(paramName);

                String value = id + " - " + name;
                param.setValue(value);

                Map<String, Query> repositoryQueryStoreMap = this.dynamicProperty.getRepositoryQueryStoreMap();
                if (repositoryQueryStoreMap.containsKey(value)) {
                    Query query = repositoryQueryStoreMap.get(value);
                    IElementParameter queryText = getQueryTextElementParameter(elem);
                    if (queryText != null) {
                        return new RepositoryChangeQueryCommand(elem, query, name, value);
                    }
                }

            }
        }
        return null;
    }

    /**
     * DOC nrousseau Comment method "getGuessQueryCommand".
     * 
     * @return
     */
    private QueryGuessCommand getGuessQueryCommand() {
        Map<String, IMetadataTable> repositoryTableMap = dynamicProperty.getRepositoryTableMap();
        IMetadataTable newRepositoryMetadata = null;
        String realTableName = null;
        String realTableId = null;

        // Only for getting the real table name.
        if (elem.getPropertyValue(EParameterName.SCHEMA_TYPE.getName()).equals(EmfComponent.REPOSITORY)) {
            String paramName;
            IElementParameter repositorySchemaTypeParameter = elem.getElementParameter(EParameterName.REPOSITORY_SCHEMA_TYPE
                    .getName());
            Object repositoryControl = hashCurControls.get(repositorySchemaTypeParameter.getName());

            paramName = EParameterName.REPOSITORY_SCHEMA_TYPE.getName();

            if (repositoryControl != null) {

                String selectedComboItem = ((CCombo) repositoryControl).getText();
                if (selectedComboItem != null && selectedComboItem.length() > 0) {
                    String value = new String(""); //$NON-NLS-1$
                    for (int i = 0; i < elem.getElementParameters().size(); i++) {
                        IElementParameter param = elem.getElementParameters().get(i);
                        if (param.getName().equals(paramName)) {
                            for (int j = 0; j < param.getListItemsValue().length; j++) {
                                if (selectedComboItem.equals(param.getListItemsDisplayName()[j])) {
                                    value = (String) param.getListItemsValue()[j];
                                }
                            }
                        }
                    }
                    if (elem instanceof Node) {
                        if (repositoryTableMap.containsKey(value)) {
                            IMetadataTable repositoryMetadata = repositoryTableMap.get(value);
                            realTableName = repositoryMetadata.getTableName();
                            realTableId = repositoryMetadata.getId();
                        }
                    }
                }
            }
        }// Ends

        QueryGuessCommand cmd = null;
        Node node = null;
        if (elem instanceof Node) {
            node = (Node) elem;
        } else { // else instanceof Connection
            node = ((org.talend.designer.core.ui.editor.connections.Connection) elem).getSource();
        }

        newRepositoryMetadata = node.getMetadataList().get(0);

        if (newRepositoryMetadata == null) {
            String schemaSelected = (String) node.getPropertyValue(EParameterName.REPOSITORY_SCHEMA_TYPE.getName());
            if (repositoryTableMap != null && schemaSelected != null && repositoryTableMap.containsKey(schemaSelected)) {
                // repositoryMetadata = repositoryTableMap.get(schemaSelected);
            } else if (newRepositoryMetadata == null) {
                MessageDialog.openWarning(new Shell(), "Alert", "Nothing to guess.");
                return cmd;
            }
        }
        cmd = new QueryGuessCommand(node, newRepositoryMetadata);

        cmd.setMaps(dynamicProperty.getTableIdAndDbTypeMap(), dynamicProperty.getTableIdAndDbSchemaMap(), repositoryTableMap);
        String type = getValueFromRepositoryName("TYPE");
        cmd.setParameters(realTableId, realTableName, type);
        return cmd;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.ui.editor.properties.controllers.AbstractRepositoryController#createComboCommand(org.eclipse.swt.custom.CCombo)
     */
    @Override
    protected Command createComboCommand(CCombo combo) {
        String paramName = (String) combo.getData(PARAMETER_NAME);

        IElementParameter param = elem.getElementParameter(paramName);
        IElementParameter switchParam = elem.getElementParameter(EParameterName.REPOSITORY_ALLOW_AUTO_SWITCH.getName());

        String name = param.getName();
        String value = combo.getText();

        for (int j = 0; j < param.getListItemsValue().length; j++) {
            if (combo.getText().equals(param.getListItemsDisplayName()[j])) {
                value = (String) param.getListItemsValue()[j];
            }
        }
        if (name.equals(EParameterName.QUERYSTORE_TYPE.getName())) {
            if (elem instanceof Node) {
                String querySelected;
                Query repositoryQuery = null;
                Map<String, Query> repositoryQueryStoreMap = this.dynamicProperty.getRepositoryQueryStoreMap();
                IElementParameter repositoryParam = param.getParentParameter().getChildParameters().get(
                        EParameterName.REPOSITORY_QUERYSTORE_TYPE.getName());
                querySelected = (String) repositoryParam.getValue();

                if (repositoryQueryStoreMap.containsKey(querySelected)) {
                    repositoryQuery = repositoryQueryStoreMap.get(querySelected);
                }/*
                 * else if (dynamicProperty.getRepositoryQueryStoreMap().size() > 0) { repositoryQuery = (Query)
                 * dynamicProperty.getRepositoryQueryStoreMap().values().toArray()[0]; }
                 */

                if (switchParam != null) {
                    switchParam.setValue(Boolean.FALSE);
                }

                if (repositoryQuery != null) {
                    return new RepositoryChangeQueryCommand(elem, repositoryQuery, name, value);
                } else {
                    return new PropertyChangeCommand(elem, name, value);
                }

            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.ui.editor.properties.controllers.AbstractRepositoryController#getRepositoryChoiceParamName()
     */
    @Override
    protected String getRepositoryChoiceParamName() {
        return EParameterName.REPOSITORY_QUERYSTORE_TYPE.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.ui.editor.properties.controllers.AbstractRepositoryController#getRepositoryTypeParamName()
     */
    @Override
    protected String getRepositoryTypeParamName() {
        return EParameterName.QUERYSTORE_TYPE.getName();
    }
}
