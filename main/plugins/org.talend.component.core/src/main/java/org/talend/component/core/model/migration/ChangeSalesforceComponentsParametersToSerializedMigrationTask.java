// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.component.core.model.migration;

import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.component.core.constants.IComponentConstants;
import org.talend.component.core.utils.ComponentsUtils;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.api.schema.SchemaElement;
import org.talend.core.model.components.ModifyComponentsAction;
import org.talend.core.model.components.conversions.IComponentConversion;
import org.talend.core.model.components.filters.IComponentFilter;
import org.talend.core.model.components.filters.NameComponentFilter;
import org.talend.core.model.properties.Item;
import org.talend.designer.core.model.utils.emf.talendfile.ElementParameterType;
import org.talend.designer.core.model.utils.emf.talendfile.NodeType;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;

/**
 * created by hcyi on Nov 18, 2015 Detailled comment
 *
 */
public class ChangeSalesforceComponentsParametersToSerializedMigrationTask extends AbstractComponentParametersMigrationTask {

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.migration.AbstractItemMigrationTask#execute(org.talend.core.model.properties.Item)
     */
    @Override
    public ExecutionResult execute(Item item) {
        final ProcessType processType = getProcessType(item);
        IComponentConversion conversion = new IComponentConversion() {

            @Override
            public void transform(NodeType node) {
                if (node == null) {
                    return;
                }
                boolean modified = false;
                ComponentProperties componentProperties = ComponentsUtils.getComponentProperties(node.getComponentName());
                List<String> propertyFieldNames = componentProperties.getPropertyFieldNames();

                for (String propertyFieldName : propertyFieldNames) {
                    //System.out.println(node.getComponentName() + IComponentConstants.EXP_SEPARATOR + propertyFieldName + "=");//$NON-NLS-1$
                    if (propertyFieldName != null) {
                        SchemaElement schemaElement = ComponentsUtils.getGenericSchemaElement(componentProperties,
                                propertyFieldName);
                        if (schemaElement != null) {
                            String oldParameterName = GenericParametersProvider.getString(node.getComponentName()
                                    + IComponentConstants.EXP_SEPARATOR + propertyFieldName);
                            EList listParamType = node.getElementParameter();
                            for (Object param : listParamType) {
                                ElementParameterType paramType = (ElementParameterType) param;
                                String paramName = paramType.getName();
                                if (paramName != null && paramName.equals(oldParameterName)) {
                                    componentProperties.setValue(schemaElement,
                                            ParameterUtilTool.convertSpecialParameterValue(paramType.getValue()));
                                    // Only remove the ElementParameterType if contains in the component properties
                                    listParamType.remove(processType);
                                    modified = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (modified) {
                    String serializedProperties = componentProperties.toSerialized();
                    if (serializedProperties != null) {
                        ElementParameterType pType = ParameterUtilTool.createParameterType(null,
                                "PROPERTIES", serializedProperties); //$NON-NLS-1$
                        node.getElementParameter().add(pType);
                    }
                }
            }
        };

        for (String name : salesforceComponentsName) {
            IComponentFilter filter = new NameComponentFilter(name);
            try {
                ModifyComponentsAction.searchAndModify(item, processType, filter,
                        Arrays.<IComponentConversion> asList(conversion));
            } catch (PersistenceException e) {
                ExceptionHandler.process(e);
                return ExecutionResult.FAILURE;
            }
        }
        return ExecutionResult.SUCCESS_NO_ALERT;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.migration.IMigrationTask#getOrder()
     */
    @Override
    public Date getOrder() {
        GregorianCalendar gc = new GregorianCalendar(2015, 11, 18, 12, 0, 0);
        return gc.getTime();
    }
}
