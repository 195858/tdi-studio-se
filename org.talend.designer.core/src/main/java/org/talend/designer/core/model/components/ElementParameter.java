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
package org.talend.designer.core.model.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.IElement;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.IElementParameterDefaultValue;
import org.talend.core.model.process.INode;
import org.talend.core.model.properties.Item;
import org.talend.designer.core.DesignerPlugin;
import org.talend.designer.core.ui.preferences.TalendDesignerPrefConstants;

/**
 * Each parameter of the components are read and written in this class. <br/>
 * 
 * $Id$
 * 
 */
public class ElementParameter implements IElementParameter {

    private static final int NB_LINES_DEFAULT = 3;

    private String name, displayName;

    private EParameterFieldType field;

    private EComponentCategory category;

    private boolean show = true, required = false, readOnly = false;

    private Object value;

    private IElement element;

    // used for CLOSED_LIST / TABLE
    private String[] itemsDisplayName;

    // used for CLOSED_LIST / TABLE
    private String[] itemsDisplayCodeName;

    // used for CLOSED_LIST / TABLE
    private String[] itemsShowIf;

    // used for CLOSED_LIST / TABLE
    private String[] itemsNotShowIf;

    // used for CLOSED_LIST
    private Object[] itemsValue;

    // used for CLOSED_LIST / TABLE
    private String[] itemsRepository;

    // used for CLOSED_LIST
    private Object defaultClosedListValue;

    private boolean basedOnSchema = false;

    private int nbLines = NB_LINES_DEFAULT, numRow = 0; // Default values

    private String repositoryValue;

    private boolean repositoryValueUsed = false;

    private String showIf = null;

    private String notShowIf = null;

    private List<IElementParameterDefaultValue> defaultValues = new ArrayList<IElementParameterDefaultValue>();

    private String filter = null;

    private boolean noCheck = false;

    private String context, groupName, groupDisplayName;

    private Map<String, IElementParameter> childParameters;

    private IElementParameter parentParameter;

    private int currentRow; // for Table only

    private Item linkedRepositoryItem;

    private boolean contextMode;

    private String labelFromRepository;

    public ElementParameter(final IElement element) {
        this.element = element;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IDesignerElementParameter#setName(java.lang.String)
     */
    public void setName(final String s) {
        name = s;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IDesignerElementParameter#getVariableName()
     */
    public String getVariableName() {
        return "__" + name + "__"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void setCategory(final EComponentCategory cat) {
        category = cat;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IDesignerElementParameter#getCategory()
     */
    public EComponentCategory getCategory() {
        return this.category;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IDesignerElementParameter#setDisplayName(java.lang.String)
     */
    public void setDisplayName(final String s) {
        displayName = s;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IDesignerElementParameter#setField(org.talend.core.model.designer.EParameterFieldType)
     */
    public void setField(final EParameterFieldType type) {
        field = type;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IDesignerElementParameter#setValue(java.lang.Object)
     */
    public void setValue(final Object o) {
        value = o;
    }

    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IDesignerElementParameter#getDisplayName()
     */
    public String getDisplayName() {
        return displayName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IDesignerElementParameter#getField()
     */
    public EParameterFieldType getField() {
        return field;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IDesignerElementParameter#getValue()
     */
    public Object getValue() {
        return value;
    }

    public void setListItemsDisplayName(final String[] list) {
        itemsDisplayName = list;
    }

    public String[] getListItemsDisplayName() {
        return itemsDisplayName;
    }

    public void setListItemsDisplayCodeName(final String[] list) {
        itemsDisplayCodeName = list;
    }

    public String[] getListItemsDisplayCodeName() {
        return itemsDisplayCodeName;
    }

    public void setListItemsValue(final Object[] list) {
        itemsValue = list;
    }

    public Object[] getListItemsValue() {
        return itemsValue;
    }

    public void setDefaultClosedListValue(Object o) {
        defaultClosedListValue = o;
    }

    public Object getDefaultClosedListValue() {
        return defaultClosedListValue;
    }

    public void setListRepositoryItems(final String[] list) {
        itemsRepository = list;
    }

    public String[] getListRepositoryItems() {
        return itemsRepository;
    }

    public void setListItemsShowIf(String[] list) {
        itemsShowIf = list;
    }

    public String[] getListItemsShowIf() {
        return itemsShowIf;
    }

    public void setListItemsNotShowIf(String[] list) {
        itemsNotShowIf = list;
    }

    public String[] getListItemsNotShowIf() {
        return itemsNotShowIf;
    }

    public int getIndexOfItemFromList(String item) {
        int index = 0;
        boolean found = false;
        if (itemsDisplayCodeName != null) {
            for (int i = 0; i < itemsDisplayCodeName.length && !found; i++) {
                String string = itemsDisplayCodeName[i];
                if (string.equals(item)) {
                    found = true;
                    index = i;
                }
            }
        }
        for (int i = 0; i < itemsValue.length && !found; i++) {
            String string = (String) itemsValue[i];
            if (string.equals(item)) {
                found = true;
                index = i;
            }
        }
        return index;
    }

    public int getNbLines() {
        return this.nbLines;
    }

    public void setNbLines(final int nbLines) {
        this.nbLines = nbLines;
    }

    public int getNumRow() {
        return this.numRow;
    }

    public void setNumRow(final int numRow) {
        this.numRow = numRow;
    }

    public boolean isReadOnly() {
        if (element != null) {
            return (this.readOnly || element.isReadOnly());
        }
        return this.readOnly;
    }

    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isRequired() {
        return this.required;
    }

    public void setRequired(final boolean required) {
        this.required = required;
    }

    public void setShow(final boolean show) {
        this.show = show;
    }

    public String getRepositoryValue() {
        return this.repositoryValue;
    }

    public void setRepositoryValue(String repositoryValue) {
        this.repositoryValue = repositoryValue;
    }

    public boolean isRepositoryValueUsed() {
        return this.repositoryValueUsed;
    }

    public void setRepositoryValueUsed(boolean repositoryUsed) {
        this.repositoryValueUsed = repositoryUsed;
    }

    public String getShowIf() {
        return showIf;
    }

    public void setShowIf(String showIf) {
        this.showIf = showIf;
    }

    public String getNotShowIf() {
        return notShowIf;
    }

    public void setNotShowIf(String notShowIf) {
        this.notShowIf = notShowIf;
    }

    public boolean isShow(List<? extends IElementParameter> listParam) {
        boolean showParameter = false;

        if (((showIf != null) || (notShowIf != null)) && show) {
            if (showIf != null) {
                showParameter = Expression.evaluate(showIf, listParam);
            } else {
                showParameter = !Expression.evaluate(notShowIf, listParam);
            }
        } else {
            showParameter = show;
        }
        return showParameter;
    }

    public boolean isShow(String conditionShowIf, String conditionNotShowIf, List<? extends IElementParameter> listParam) {
        boolean showParameter = false;

        if (((conditionShowIf != null) || (conditionNotShowIf != null)) && show) {
            if (conditionShowIf != null) {
                showParameter = Expression.evaluate(conditionShowIf, listParam, this);
            } else {
                showParameter = !Expression.evaluate(conditionNotShowIf, listParam, this);
            }
        } else {
            showParameter = show;
        }
        return showParameter;
    }

    public List<IElementParameterDefaultValue> getDefaultValues() {
        return this.defaultValues;
    }

    public void setDefaultValues(List<IElementParameterDefaultValue> defaultValues) {
        this.defaultValues = defaultValues;
    }

    public void setValueToDefault(List<? extends IElementParameter> listParam) {
        for (IElementParameterDefaultValue defaultValue : defaultValues) {
            boolean setDefaultValue = false;
            String conditionIf = defaultValue.getIfCondition();
            String conditionNotIf = defaultValue.getNotIfCondition();

            if ((conditionIf != null) || (conditionNotIf != null)) {
                if (conditionIf != null) {
                    setDefaultValue = Expression.evaluate(conditionIf, listParam);
                } else {
                    setDefaultValue = !Expression.evaluate(conditionNotIf, listParam);
                }
            }
            if (setDefaultValue) {
                if (this.field.equals(EParameterFieldType.CHECK) || this.field.equals(EParameterFieldType.RADIO)) {
                    setValue(new Boolean(defaultValue.getDefaultValue().toString()));
                } else {
                    setValue(defaultValue.getDefaultValue());
                }
            }
        }
    }

    public IElement getElement() {
        return element;
    }

    public void setElement(IElement element) {
        this.element = element;
    }

    public boolean isBasedOnSchema() {
        return basedOnSchema;
    }

    public void setBasedOnSchema(boolean basedOnSchema) {
        this.basedOnSchema = basedOnSchema;
    }

    @Override
    public String toString() {
        if (value == null) {
            return name + ": null"; //$NON-NLS-1$
        }
        return name + ": " + value.toString(); //$NON-NLS-1$
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    /**
     * Getter for noCheck.
     * 
     * @return the noCheck
     */
    public boolean isNoCheck() {
        IPreferenceStore preferenceStore = DesignerPlugin.getDefault().getPreferenceStore();

        if (!preferenceStore.getBoolean(TalendDesignerPrefConstants.PROPERTY_CODE_CHECK)) {
            // if the check has been completely disabled then no check.
            // if not disabled in the preferences, then it will depends on the next conditions.
            return true;
        }

        if (!(element instanceof INode)) {
            return true;
        }
        return noCheck;
    }

    /**
     * Sets the noCheck.
     * 
     * @param noCheck the noCheck to set
     */
    public void setNoCheck(boolean noCheck) {
        this.noCheck = noCheck;
    }

    /**
     * Getter for context.
     * 
     * @return the context
     */
    public String getContext() {
        return context;
    }

    /**
     * Sets the context.
     * 
     * @param context the context to set
     */
    public void setContext(String context) {
        this.context = context;
    }

    /**
     * Getter for childParameters.
     * 
     * @return the childParameters
     */
    public Map<String, IElementParameter> getChildParameters() {
        if (childParameters == null) {
            childParameters = new HashMap<String, IElementParameter>();
        }
        return childParameters;
    }

    public IElementParameter getParentParameter() {
        return parentParameter;
    }

    public void setParentParameter(IElementParameter parentParameter) {
        this.parentParameter = parentParameter;
        parentParameter.getChildParameters().put(this.getName(), this);
    }

    public boolean isDisplayedByDefault() {
        return this.show;
    }

    /**
     * Getter for currentRow.
     * 
     * @return the currentRow
     */
    public int getCurrentRow() {
        return this.currentRow;
    }

    /**
     * Sets the currentRow.
     * 
     * @param currentRow the currentRow to set
     */
    public void setCurrentRow(int currentRow) {
        this.currentRow = currentRow;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IElementParameter#getGroup()
     */
    public String getGroup() {
        return this.groupName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IElementParameter#setGroup(java.lang.String)
     */
    public void setGroup(String groupName) {
        this.groupName = groupName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IElementParameter#getGroupDisplayName()
     */
    public String getGroupDisplayName() {
        return this.groupDisplayName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IElementParameter#setGroupDisplayName(java.lang.String)
     */
    public void setGroupDisplayName(String groupDisplayName) {
        this.groupDisplayName = groupDisplayName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IElementParameter#getLinkedRepositoryItem()
     */
    public Item getLinkedRepositoryItem() {
        return linkedRepositoryItem;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IElementParameter#setLinkedRepositoryItem(org.talend.core.model.properties.Item)
     */
    public void setLinkedRepositoryItem(Item item) {
        this.linkedRepositoryItem = item;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IElementParameter#getContextMode()
     */
    public boolean isContextMode() {
        return this.contextMode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IElementParameter#setContextMode(java.lang.String)
     */
    public void setContextMode(boolean mode) {
        this.contextMode = mode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IElementParameter#getLabelFromRepository()
     */
    public String getLabelFromRepository() {
        return this.labelFromRepository;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IElementParameter#setLabelFromRepository(java.lang.String)
     */
    public void setLabelFromRepository(String label) {
        this.labelFromRepository = label;

    }
}
