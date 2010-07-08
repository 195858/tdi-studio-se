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
package org.talend.designer.hl7.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.talend.commons.ui.swt.formtools.Form;
import org.talend.core.model.metadata.IMetadataColumn;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.process.EConnectionType;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.IElementParameter;
import org.talend.designer.hl7.HL7InputComponent;
import org.talend.designer.hl7.action.CreateHL7AttributeAction;
import org.talend.designer.hl7.action.CreateHL7ElementAction;
import org.talend.designer.hl7.action.DeleteHL7NodeAction;
import org.talend.designer.hl7.action.HL7DisconnectAction;
import org.talend.designer.hl7.action.HL7FixValueAction;
import org.talend.designer.hl7.action.SetRepetableAction;
import org.talend.designer.hl7.managers.HL7Manager;
import org.talend.designer.hl7.managers.HL7OutputManager;
import org.talend.designer.hl7.ui.data.HL7TreeNode;
import org.talend.designer.hl7.ui.edit.HL7OutputTableViewerProvider;
import org.talend.designer.hl7.ui.edit.HL7TargetTreeViewerProvider;
import org.talend.designer.hl7.ui.edit.SchemaXMLLinker;
import org.talend.designer.hl7.ui.footer.FooterComposite;
import org.talend.designer.hl7.ui.header.HeaderComposite;

/**
 * DOC hwang class global comment. Detailled comment
 */
public class HL7OutputUI extends HL7UI {

    protected HL7OutputManager hl7Manager;

    private Composite hl7UIParent;

    protected HL7InputComponent externalNode;

    private SashForm xmlToSchemaSash;

    protected TableViewer schemaViewer;

    protected TreeViewer xmlViewer;

    private HeaderComposite header;

    private SchemaXMLLinker linker;

    private CreateHL7ElementAction createAction;

    private DeleteHL7NodeAction deleteAction;

    private HL7DisconnectAction disconnectAction;

    private HL7FixValueAction fixValueAction;

    private CreateHL7AttributeAction createAttributeAction;

    private IAction importFromXMLAction;

    private SetRepetableAction setRepetableAction;

    private String selectedText;

    private boolean canModify;

    private String startChar;

    private String endChar;

    public HL7OutputUI(Composite parent, HL7Manager hl7Manager) {
        super(parent, hl7Manager);
        if (hl7Manager instanceof HL7OutputManager) {
            this.hl7Manager = (HL7OutputManager) hl7Manager;
        }
        hl7Manager.getUiManager().setHl7UI(this);
        externalNode = hl7Manager.getHl7Component();

        // add listeners.
        this.hl7UIParent = parent;
        hl7UIParent.setLayout(new GridLayout());

        this.startChar = hl7Manager.getStartChar();
        this.endChar = hl7Manager.getEndChar();
    }

    /**
     * bqian Comment method "init".
     */
    public void init() {
        createContent(hl7UIParent);
    }

    /**
     * Comment method "createContent".
     * 
     * @param child
     */
    private void createContent(Composite mainComposite) {
        // header = new HeaderComposite(mainComposite, SWT.NONE, this.filePath, startChar, endChar, hl7Manager,
        // this.isRepository);
        if (this.hl7Manager.isRepetable()) {
            // header.updateStatus("");
        }
        // Splitter
        xmlToSchemaSash = new SashForm(mainComposite, SWT.HORIZONTAL | SWT.SMOOTH);
        xmlToSchemaSash.setLayoutData(new GridData(GridData.FILL_BOTH));
        xmlToSchemaSash.setBackgroundMode(SWT.INHERIT_FORCE);

        canModify = externalNode.getProcess().isReadOnly();
        addSchemaViewer(xmlToSchemaSash, 300, 110);
        addXMLViewer(xmlToSchemaSash, 400, 110);

        xmlToSchemaSash.setWeights(new int[] { 40, 60 });
        linker = new SchemaXMLLinker(this.xmlToSchemaSash);
        linker.init(schemaViewer.getTable(), xmlViewer);
        linker.setManager(hl7Manager);
        initSchemaTable();
        new FooterComposite(mainComposite, SWT.NONE, hl7Manager);
        Tree xmlTree = xmlViewer.getTree();

        if (xmlTree.getItems().length > 0) {
            TreeItem root = xmlTree.getItem(0);
            TableItem[] tableItems = schemaViewer.getTable().getItems();

            initLinker(root, tableItems);
        }

    }

    protected void createCombo(Composite mainComposite) {

    }

    private void initLinker(TreeItem node, TableItem[] tableItems) {
        HL7TreeNode treeNode = (HL7TreeNode) node.getData();
        IMetadataColumn column = treeNode.getColumn();
        if (column != null) {
            if (this.gethl7Manager().getHl7Component().isHL7Output() && treeNode.getChildren().size() <= 0) {
                for (int i = 0; i < tableItems.length; i++) {
                    IMetadataColumn mColumn = (IMetadataColumn) tableItems[i].getData();
                    if (mColumn.getLabel().equals(column.getLabel())) {
                        linker.addLoopLink(tableItems[i], tableItems[i].getData(), xmlViewer.getTree(), treeNode, true);
                        break;
                    }
                }
            }
            for (int i = 0; i < tableItems.length; i++) {
                IMetadataColumn mColumn = (IMetadataColumn) tableItems[i].getData();
                if (mColumn.getLabel().equals(column.getLabel())) {
                    linker.addLoopLink(tableItems[i], tableItems[i].getData(), xmlViewer.getTree(), treeNode, true);
                    break;
                }
            }
        }
        TreeItem[] children = node.getItems();
        for (int i = 0; i < children.length; i++) {
            initLinker(children[i], tableItems);
        }
    }

    public void redrawLinkers() {
        linker.removeAllLinks();
        if (xmlViewer.getTree().getItems().length <= 0) {
            return;
        }
        TreeItem root = xmlViewer.getTree().getItem(0);
        if (this.gethl7Manager().getHl7Component().isHL7Output()) {
            if (this.hl7Manager != null) {
                List<HL7TreeNode> treeData = this.hl7Manager.getTreeData(this.hl7Manager.getCurrentSchema());
                if (treeData != null && treeData.size() > 0) {
                    HL7TreeNode rootTreeData = treeData.get(0);
                    for (TreeItem item : xmlViewer.getTree().getItems()) {
                        if (rootTreeData == item.getData()) {
                            root = item;
                            break;
                        }
                    }
                }
            }
        }

        TableItem[] tableItems = schemaViewer.getTable().getItems();
        initLinker(root, tableItems);
        if (linker.linkSize() == 0) {
            linker.updateLinksStyleAndControlsSelection(xmlViewer.getTree(), true);
        }
    }

    public void refreshXMLViewer(HL7TreeNode targetNode) {

        updateStatus();

        xmlViewer.getTree().setData("row", hl7Manager.getCurrentSchema());
        this.xmlViewer.refresh();
    }

    protected void initSchemaTable() {
        if (externalNode.isHL7Output()) {// !externalNode.istWriteXMLField() && !externalNode.istMDMOutput()) {
            IMetadataTable metadataTable = this.externalNode.getMetadataList().get(0);
            if (metadataTable != null) {
                List<IMetadataColumn> columnList = metadataTable.getListColumns();
                schemaViewer.setInput(columnList);
            } else {
                schemaViewer.setInput(new ArrayList<IMetadataColumn>());
            }

        } else {
            IConnection inConn = null;
            for (IConnection conn : externalNode.getIncomingConnections()) {
                if ((conn.getLineStyle().equals(EConnectionType.FLOW_MAIN))
                        || (conn.getLineStyle().equals(EConnectionType.FLOW_REF))) {
                    inConn = conn;
                    break;
                }
            }
            if (inConn != null) {
                List<IMetadataColumn> columnList = inConn.getMetadataTable().getListColumns();
                schemaViewer.setInput(columnList);
            } else {
                schemaViewer.setInput(new ArrayList<IMetadataColumn>());
            }
        }

    }

    /**
     * create xml viewer.
     * 
     * @param mainComposite
     * @param form
     * @param width
     * @param height
     */
    private void addXMLViewer(final Composite mainComposite, final int width, final int height) {

        // Group Schema Viewer
        Group group = Form.createGroup(mainComposite, 1, "Linker Target", height);
        // group.setBackgroundMode(SWT.INHERIT_FORCE);

        xmlViewer = new TreeViewer(group, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        xmlViewer.getControl().setLayoutData(gridData);
        xmlViewer.setUseHashlookup(true);
        Tree tree = xmlViewer.getTree();
        if (canModify) {
            tree.setEnabled(false);
        }
        tree.setLinesVisible(true);
        tree.setBackground(tree.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
        column1.setText("XML Tree");
        column1.setWidth(170);

        // Related Column
        TreeColumn column2 = new TreeColumn(tree, SWT.CENTER);
        column2.setText("Related Column");
        column2.setWidth(100);

        TreeColumn column3 = new TreeColumn(tree, SWT.CENTER);
        column3.setText("Node Status");
        column3.setWidth(90);

        TreeColumn column4 = new TreeColumn(tree, SWT.CENTER);
        column4.setText("Default Value");
        column4.setWidth(90);

        tree.setHeaderVisible(true);
        // tree.setBackgroundMode(SWT.INHERIT_NONE);
        HL7TargetTreeViewerProvider provider = new HL7TargetTreeViewerProvider();
        xmlViewer.setLabelProvider(provider);

        xmlViewer.setCellModifier(new ICellModifier() {

            public boolean canModify(Object element, String property) {
                HL7TreeNode node = (HL7TreeNode) element;
                if (property.equals("C1")) {
                    if (node.getLabel() != null && node.getLabel().length() > 0) {
                        return true;
                    }
                }
                if (property.equals("C4")) {
                    if (node.getDefaultValue() != null && node.getDefaultValue().length() > 0) {
                        return true;
                    }
                }
                return false;
            }

            public Object getValue(Object element, String property) {
                HL7TreeNode node = (HL7TreeNode) element;
                if (property.equals("C1")) {
                    return node.getLabel();
                }
                if (property.equals("C4")) {
                    return node.getDefaultValue();
                }

                return null;
            }

            public void modify(Object element, String property, Object value) {
                TreeItem treeItem = (TreeItem) element;
                HL7TreeNode node = (HL7TreeNode) treeItem.getData();
                if (property.equals("C1")) {
                    node.setLabel((String) value);
                }
                if (property.equals("C4")) {
                    node.setDefaultValue((String) value);
                }
                xmlViewer.refresh(node);
            }
        });
        xmlViewer.setColumnProperties(new String[] { "C1", "C2", "C3", "C4" }); //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        CellEditor editor = new TextCellEditor(xmlViewer.getTree());

        editor.addListener(new DialogErrorXMLLabelCellEditor(editor, "C1"));

        // add by wzhang for bug 8572. set Default value column to be edit.
        CellEditor editorDefault = new TextCellEditor(xmlViewer.getTree());
        editorDefault.addListener(new DialogErrorXMLLabelCellEditor(editorDefault, "C4"));

        xmlViewer.setCellEditors(new CellEditor[] { editor, null, null, editorDefault });
        xmlViewer.setContentProvider(provider);

        xmlViewer.setInput(this.hl7Manager.getTreeData());
        xmlViewer.expandAll();
        createAction();
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager manager) {
                HL7OutputUI.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(xmlViewer.getControl());
        xmlViewer.getControl().setMenu(menu);
        xmlViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                // TODO Auto-generated method stub

            }

        });
        refreshXMLViewer(null);
    }

    /**
     * Comment method "fillContextMenu".
     * 
     * @param manager
     */
    protected void fillContextMenu(IMenuManager manager) {
        if (!xmlViewer.getSelection().isEmpty()) {
            manager.add(createAction);
            createAction.init();
            manager.add(createAttributeAction);
            createAttributeAction.init();
            manager.add(new Separator());
            manager.add(deleteAction);
            deleteAction.init();
            manager.add(disconnectAction);
            disconnectAction.init();
            manager.add(fixValueAction);
            fixValueAction.init();
            manager.add(new Separator());
            manager.add(setRepetableAction);
            setRepetableAction.init();
        }
    }

    // just judge if select the advance model
    private boolean getValue() {
        IElementParameter elementParameter = externalNode.getElementParameter("MERGE");
        if (elementParameter != null) {
            return (Boolean) elementParameter.getValue();
        }
        return false;
    }

    private void createAction() {
        createAction = new CreateHL7ElementAction(xmlViewer, this, "Add Sub-element");
        createAttributeAction = new CreateHL7AttributeAction(xmlViewer, this, "Add Attribute");
        deleteAction = new DeleteHL7NodeAction(xmlViewer, this, "Delete");
        disconnectAction = new HL7DisconnectAction(xmlViewer, this, "Disconnect Linker");
        fixValueAction = new HL7FixValueAction(xmlViewer, this, "Set A Fix Value");
        setRepetableAction = new SetRepetableAction(xmlViewer, this, "Set As Repetable Element", this.getValue());

    }

    private void addSchemaViewer(final Composite mainComposite, final int width, final int height) {
        // Group Schema Viewer
        final Group group = Form.createGroup(mainComposite, 1, "Linker Source", height);
        // add by wzhang. add a combo for tFileOutputMSXML.
        createCombo(group);
        schemaViewer = new TableViewer(group);

        // schemaViewer.set
        // schemaViewer.getTable().setBackground(schemaViewer.getTable().getDisplay().getSystemColor(SWT.COLOR_WHITE));

        HL7OutputTableViewerProvider provider = new HL7OutputTableViewerProvider();
        schemaViewer.setContentProvider(provider);
        schemaViewer.setLabelProvider(provider);

        GridData data2 = new GridData(GridData.FILL_BOTH);
        Table table = schemaViewer.getTable();
        // see bug 7087
        if (canModify) {
            table.setEnabled(false);
        }
        // table.setLinesVisible(true);
        table.setHeaderVisible(true);
        TableColumn column1 = new TableColumn(table, SWT.LEFT);
        column1.setText("Schema List");
        column1.setWidth(100);
        table.setLayoutData(data2);

    }

    public Composite gethl7UIParent() {
        return this.hl7UIParent;
    }

    public HL7Manager gethl7Manager() {
        return this.hl7Manager;
    }

    /**
     * DOC gke HL7UI class global comment. Detailled comment <br/>
     * 
     */
    class DialogErrorXMLLabelCellEditor implements ICellEditorListener {

        CellEditor editor;

        String property;

        Boolean validateLabel;

        public void applyEditorValue() {
            String text = getControl().getText();
            onValueChanged(text, true, property);
        }

        public void cancelEditor() {
        }

        public void editorValueChanged(boolean oldValidState, boolean newValidState) {
            onValueChanged(getControl().getText(), false, property);
        }

        private void onValueChanged(final String newValue, boolean showAlertIfError, String property) {
            final Text text = getControl();
            HL7TreeNode selectNode = null;
            ISelection selection = xmlViewer.getSelection();
            if (selection instanceof TreeSelection) {
                Object obj = ((TreeSelection) selection).getFirstElement();
                if (obj instanceof HL7TreeNode) {
                    selectNode = (HL7TreeNode) obj;
                }
            }
            String errorMessage = null;

            if (errorMessage == null) {
                text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_WHITE));
            } else {
                text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_RED));
                if (showAlertIfError) {
                    text.setText(selectedText);
                    MessageDialog.openError(text.getShell(), "Invalid XML label.", errorMessage);
                }
            }
        }

        public DialogErrorXMLLabelCellEditor(CellEditor editor, String property) {
            super();
            this.property = property;
            this.editor = editor;
        }

        private Text getControl() {
            return (Text) editor.getControl();
        }

    }

    public void setSelectedText(String label) {
        selectedText = label;
    }

    public void updateStatus() {

        // List<HL7TreeNode> allRootTreeData = hl7Manager.getTreeData();
        // int num = 0, rootNum = 0;
        // int groupNum = 0;
        // List<HL7TreeNode> onLoopNodes = new ArrayList<HL7TreeNode>();
        // for (HL7TreeNode node : allRootTreeData) {
        // HL7TreeNode rootHL7TreeNode = hl7Manager.getRootHL7TreeNode(node);
        // if (rootHL7TreeNode != null) {
        // if (existedLoopNode(rootHL7TreeNode)) {
        // num++;
        // } else {
        // onLoopNodes.add(rootHL7TreeNode);
        // }
        // rootNum++;
        // if (existedGroupNode(rootHL7TreeNode)) {
        // groupNum++;
        // } else {
        // // onLoopNodes.add(rootHL7TreeNode);
        // }
        // }
        // }
        // if (this.getValue()) {
        // if (num != rootNum || groupNum != rootNum) {
        // String message = Messages.getString("HL7UI.NoLoopOfAdvance");
        // if (rootNum > 1) {
        // message = "";
        // for (HL7TreeNode node : onLoopNodes) {
        // message += node.getRow() + ",";
        // }
        // message = message.substring(0, message.length() - 1);
        // message += Messages.getString("HL7UI.needLoop");
        // }
        // header.updateStatus(message);
        // } else {
        // header.clearStatus();
        //
        // }
        // } else {
        // if (num != rootNum) {
        // String message = Messages.getString("HL7UI.NoLoop");
        //
        // if (rootNum > 1) {
        // message = "";
        // for (HL7TreeNode node : onLoopNodes) {
        // message += node.getRow() + ",";
        // }
        // message = message.substring(0, message.length() - 1);
        // message += Messages.getString("HL7UI.needLoop");
        // }
        // header.updateStatus(message);
        // } else {
        // header.clearStatus();
        //
        // }
        // }

    }

    private void judgeRepository() {

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.hl7.ui.HL7UI#getHeader()
     */
    @Override
    public HeaderComposite getHeader() {
        return this.header;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.hl7.ui.HL7UI#initlinkers()
     */
    @Override
    protected void initlinkers() {
        redrawLinkers();
    }
}
