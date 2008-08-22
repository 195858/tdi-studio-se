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
package org.talend.designer.scd.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.talend.designer.scd.ScdManager;
import org.talend.designer.scd.model.Type3Field;
import org.talend.designer.scd.util.DragDropManager;
import org.talend.designer.scd.util.TableEditorManager;

/**
 * DOC hcw class global comment. Detailled comment
 */
public class Type3Section extends ScdSection implements IDragDropDelegate {

    private static final String PREVIOUS_HEADER = "previous value";

    private static final String CURRENT_HEADER = "current value";

    private static final int CURRENT_COLUMN_INDEX = 0;

    private static final int PREVIOUS_COLUMN_INDEX = 1;

    private static final String PREVIOUS_NAME_PREFIX = "previous_";

    private Table table;

    private TableEditorManager editorManager;

    private DragDropManager dragDropManager;

    private List<Type3Field> tableModel;

    private TableViewer tableViewer;

    private List<String> inputColumns;

    private List<String> outputColumns;

    /**
     * DOC hcw Type3Section constructor comment.
     * 
     * @param parent
     * @param width
     * @param height
     */
    public Type3Section(Composite parent, int width, int height, ScdManager scdManager) {
        super(parent, width, height, scdManager, true);
        editorManager = new TableEditorManager();
        dragDropManager = new DragDropManager();
        dragDropManager.addDragSupport(table, this);
        dragDropManager.addDropSupport(table, this);
        tableModel = new ArrayList<Type3Field>();

    }

    @Override
    protected void createContents(Composite composite) {
        inputColumns = scdManager.getInputColumnNames();
        outputColumns = scdManager.getOutputColumnNames();
        tableViewer = new TableViewer(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        table = tableViewer.getTable();
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        table.setLayoutData(gd);

        TableViewerColumn currentColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        currentColumn.getColumn().setText(CURRENT_HEADER);
        currentColumn.getColumn().setWidth(200);
        currentColumn.setEditingSupport(new Type3EditingSupport(tableViewer, 0));

        TableViewerColumn previousColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        previousColumn.getColumn().setText(PREVIOUS_HEADER);
        previousColumn.getColumn().setWidth(200);
        previousColumn.setEditingSupport(new Type3EditingSupport(tableViewer, 1));

        tableViewer.setLabelProvider(new TableLabelProvider());
        tableViewer.setContentProvider(new ContentProvider());

    }

    @Override
    public List<String> getUsedFields() {
        List<String> fields = new ArrayList<String>();
        for (Type3Field field : tableModel) {
            if (StringUtils.isNotEmpty(field.getPreviousValue())) {
                fields.add(field.getPreviousValue());
            }
        }
        return fields;
    }

    public Table getTable() {
        return table;
    }

    @Override
    protected void addToolbarListener() {
        addEntryItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (inputColumns.size() > 0 && outputColumns.size() > 0) {
                    Type3Field field = new Type3Field(outputColumns.get(0), inputColumns.get(0));
                    tableModel.add(field);
                    tableViewer.setInput(tableModel);
                }
            }
        });

        removeEntryItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                int[] selection = tableViewer.getTable().getSelectionIndices();
                if (selection == null || selection.length == 0) {
                    return;
                }
                tableViewer.cancelEditing();
                for (int i = selection.length - 1; i >= 0; i--) {
                    tableModel.remove(selection[i]);
                }
                tableViewer.setInput(tableModel);
            }
        });

        moveUpEntryItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                int[] selection = tableViewer.getTable().getSelectionIndices();
                if (selection == null || selection.length == 0) {
                    return;
                }
                tableViewer.cancelEditing();
                List<Integer> updateIndices = new ArrayList<Integer>(); // indices of item which will be updated
                int[] newSelectionIndices = adjustIndicesUp(selection, tableModel, updateIndices);
                // refresh
                tableViewer.setInput(tableModel);

            }
        });

        moveDownEntryItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                int[] selection = tableViewer.getTable().getSelectionIndices();
                if (selection == null || selection.length == 0) {
                    return;
                }
                tableViewer.cancelEditing();
                List<Integer> updateIndices = new ArrayList<Integer>(); // indices of item which will be updated
                int[] newSelectionIndices = adjustIndicesDown(selection, tableModel, updateIndices);
                // refresh
                tableViewer.setInput(tableModel);
            }
        });

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.scd.ui.IDragDropDelegate#getDragItemsAsText()
     */
    public String getDragItemsAsText() {
        TableItem[] selection = table.getSelection();
        StringBuffer buf = new StringBuffer();
        // number of selected elements
        buf.append(selection.length);
        for (int i = 0, n = selection.length; i < n; i++) {
            buf.append('|');
            buf.append(selection[i].getText(1));
        }

        return buf.toString();

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.scd.ui.IDragDropDelegate#onDropItems(java.lang.String, org.eclipse.swt.graphics.Point)
     */
    public void onDropItems(String data, Point position) {

        String[] items = data.split("\\|");
        // skip items[0], which is the number of selected elements
        for (int i = 1; i < items.length; i++) {
            Type3Field field = new Type3Field();
            field.setCurrentValue("");
            field.setPreviousValue(items[i]);
            tableModel.add(field);
        }
        tableViewer.setInput(tableModel);

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.scd.ui.IDragDropDelegate#isDropAllowed(java.lang.String)
     */
    public boolean isDropAllowed(String data) {
        return true;
    }

    public void setTableInput(List<Type3Field> tableModel) {
        if (tableModel == null) {
            this.tableModel.clear();
        } else {
            this.tableModel = tableModel;
        }
        tableViewer.setInput(tableModel);
    }

    public List<Type3Field> getTableData() {
        return tableModel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.scd.ui.IDragDropDelegate#removeDragItems()
     */
    public void removeDragItems() {
        TableItem[] selection = table.getSelection();
        for (TableItem item : selection) {
            Type3Field field = (Type3Field) item.getData();
            tableModel.remove(field);
            editorManager.removeEditors(item);
        }
        table.remove(table.getSelectionIndices());
    }

    static class TableLabelProvider extends LabelProvider implements ITableLabelProvider {

        public String getColumnText(Object element, int columnIndex) {
            switch (columnIndex) {
            case CURRENT_COLUMN_INDEX:
                return ((Type3Field) element).getCurrentValue();
            case PREVIOUS_COLUMN_INDEX:
                return ((Type3Field) element).getPreviousValue();
            }
            return "";
        }

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }
    }

    static class ContentProvider implements IStructuredContentProvider {

        public Object[] getElements(Object inputElement) {
            return ((List) inputElement).toArray();
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }

    class Type3EditingSupport extends EditingSupport {

        private CellEditor editor;

        private int column;

        private ColumnViewer viewer;

        public Type3EditingSupport(ColumnViewer viewer, int column) {
            super(viewer);
            this.column = column;
            this.viewer = viewer;

            switch (column) {
            case CURRENT_COLUMN_INDEX:
                editor = new ComboBoxCellEditor(((TableViewer) viewer).getTable(), outputColumns.toArray(new String[outputColumns
                        .size()]));
                break;
            case PREVIOUS_COLUMN_INDEX:
                editor = new ComboBoxCellEditor(((TableViewer) viewer).getTable(), inputColumns.toArray(new String[inputColumns
                        .size()]));
                break;

            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.EditingSupport#canEdit(java.lang.Object)
         */
        @Override
        protected boolean canEdit(Object element) {
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.EditingSupport#getCellEditor(java.lang.Object)
         */
        @Override
        protected CellEditor getCellEditor(Object element) {
            return editor;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.EditingSupport#getValue(java.lang.Object)
         */
        @Override
        protected Object getValue(Object element) {
            switch (column) {
            case CURRENT_COLUMN_INDEX:
                return outputColumns.indexOf(((Type3Field) element).getCurrentValue());
            case PREVIOUS_COLUMN_INDEX:
                return inputColumns.indexOf(((Type3Field) element).getPreviousValue());
            }
            return 0;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.EditingSupport#setValue(java.lang.Object, java.lang.Object)
         */
        @Override
        protected void setValue(Object element, Object value) {
        }

        @Override
        protected void saveCellEditorValue(CellEditor cellEditor, ViewerCell cell) {
            Integer value = (Integer) cellEditor.getValue();
            int index = tableViewer.getTable().getSelectionIndex();
            if (value == null || value < 0 || index < 0 || index >= tableModel.size()) {
                return;
            }
            try {
                Type3Field field = tableModel.get(index);
                switch (column) {
                case CURRENT_COLUMN_INDEX:
                    field.setCurrentValue(outputColumns.get(value));
                    break;
                case PREVIOUS_COLUMN_INDEX:
                    field.setPreviousValue(inputColumns.get(value));
                    scdManager.fireFieldChange();
                    break;
                }

                tableViewer.refresh();
            } catch (Exception e) {
            }
        }
    }

}
