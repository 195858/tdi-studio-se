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
package org.talend.designer.core.ui.views.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;
import org.talend.commons.ui.image.EImage;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.Element;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.properties.tab.HorizontalTabFactory;
import org.talend.core.properties.tab.TalendPropertyTabDescriptor;
import org.talend.designer.core.DesignerPlugin;
import org.talend.designer.core.ui.editor.connections.Connection;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.notes.Note;
import org.talend.designer.core.ui.editor.properties.connections.MainConnectionComposite;
import org.talend.designer.core.ui.editor.properties.controllers.generator.IDynamicProperty;
import org.talend.designer.core.ui.editor.properties.notes.AbstractNotePropertyComposite;
import org.talend.designer.core.ui.editor.properties.notes.OpaqueNotePropertyComposite;
import org.talend.designer.core.ui.editor.properties.notes.TextNotePropertyComposite;
import org.talend.designer.core.ui.editor.subjobcontainer.SubjobContainer;
import org.talend.designer.core.ui.preferences.TalendDesignerPrefConstants;

/**
 * nrousseau class global comment. Detailled comment <br/>
 * 
 */
public class ComponentSettingsView extends ViewPart implements IComponentSettingsView {

    private static final String PARENT = "parent";

    private static final String CATEGORY = "category";

    private static final String DEFAULT = "default";

    private static final String TABLEVIEW = "table view";

    public static final String ID = "org.talend.designer.core.ui.views.properties.ComponentSettingsView";

    private HorizontalTabFactory tabFactory = null;

    private TalendPropertyTabDescriptor currentSelectedTab;

    private Element element;

    private IDynamicProperty dc = null;

    private boolean cleaned;

    private boolean selectedPrimary;

    private Map<String, Composite> parentMap = null;

    private Map<String, EComponentCategory> categoryMap = null;

    /**
     * Getter for parentMap.
     * 
     * @return the parentMap
     */
    public Map<String, Composite> getParentMap() {
        return this.parentMap;
    }

    /**
     * Getter for categoryMap.
     * 
     * @return the categoryMap
     */
    public Map<String, EComponentCategory> getCategoryMap() {
        return this.categoryMap;
    }

    /**
     * nrousseau ComponentSettings constructor comment.
     */
    public ComponentSettingsView() {
        tabFactory = new HorizontalTabFactory();
        parentMap = new HashMap<String, Composite>();
        categoryMap = new HashMap<String, EComponentCategory>();
    }

    /**
     * DOC zwang Comment method "getPreference".
     * 
     * @return
     */
    private IPreferenceStore getPreference() {
        // TODO Auto-generated method stub
        return DesignerPlugin.getDefault().getPreferenceStore();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
        tabFactory.initComposite(parent);
        tabFactory.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                TalendPropertyTabDescriptor descriptor = (TalendPropertyTabDescriptor) selection.getFirstElement();

                if (descriptor == null) {
                    return;
                }

                if (currentSelectedTab != null
                        && (!currentSelectedTab.getElement().equals(descriptor.getElement()) || currentSelectedTab.getCategory() != descriptor
                                .getCategory())) {
                    for (Control curControl : tabFactory.getTabComposite().getChildren()) {
                        curControl.dispose();
                    }
                }

                if (element == null || !element.equals(descriptor.getElement()) || currentSelectedTab == null
                        || currentSelectedTab.getCategory() != descriptor.getCategory() || selectedPrimary) {
                    element = descriptor.getElement();
                    currentSelectedTab = descriptor;

                    createDynamicComposite(tabFactory.getTabComposite(), descriptor.getElement(), descriptor.getCategory());

                    selectedPrimary = false;
                }
            }
        });
    }

    /**
     * yzhang Comment method "createDynamicComposite".
     * 
     * @param parent
     * @param element
     * @param category
     */
    private void createDynamicComposite(Composite parent, Element element, EComponentCategory category) {
        // DynamicComposite dc = null;

        if (element instanceof Node) {
            // dc = new DynamicComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.NO_FOCUS, category, element);
            if (category == EComponentCategory.BASIC) {
                getParentMap().put(ComponentSettingsView.PARENT, parent);
                getCategoryMap().put(ComponentSettingsView.CATEGORY, category);
                // getElementMap().put(ComponentSettingsView.ELEMENT, element);
                createButtonListener();
                // tabFactory.getTabbedPropertyComposite().setVisible(true);
                if (ComponentSettingsView.DEFAULT.equals(getPreference().getString(TalendDesignerPrefConstants.VIEW_OPTIONS))) {
                    tabFactory.getTabbedPropertyComposite().setCompactView(true);
                    tabFactory.getTabbedPropertyComposite().getCompactButton().setImage(
                            ImageProvider.getImage(EImage.COMPACT_VIEW));
                    tabFactory.getTabbedPropertyComposite().getTableButton().setImage(
                            ImageProvider.getImage(EImage.NO_TABLE_VIEW));
                    dc = new MultipleThreadDynamicComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.NO_FOCUS, category,
                            element, true);
                } else if (ComponentSettingsView.TABLEVIEW.equals(getPreference().getString(
                        TalendDesignerPrefConstants.VIEW_OPTIONS))) {
                    tabFactory.getTabbedPropertyComposite().setCompactView(false);
                    tabFactory.getTabbedPropertyComposite().getCompactButton().setImage(
                            ImageProvider.getImage(EImage.NO_COMPACT_VIEW));
                    tabFactory.getTabbedPropertyComposite().getTableButton().setImage(ImageProvider.getImage(EImage.TABLE_VIEW));
                    dc = new MultipleThreadDynamicComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.NO_FOCUS, category,
                            element, false);
                }
            } else if (category == EComponentCategory.ADVANCED_CONTEXT) {
                dc = new AdvancedContextComposite(parent, SWT.NONE, element);
            } else {
                tabFactory.getTabbedPropertyComposite().getCompactButton().setVisible(false);
                tabFactory.getTabbedPropertyComposite().getTableButton().setVisible(false);
                dc = new MultipleThreadDynamicComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.NO_FOCUS, category, element,
                        true);
            }
        } else if (element instanceof Connection) {
            dc = new MainConnectionComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.NO_FOCUS, category, element);
        } else if (element instanceof Note) {

            if (category == EComponentCategory.BASIC) {

                if (parent.getLayout() instanceof FillLayout) {
                    FillLayout layout = (FillLayout) parent.getLayout();
                    layout.type = SWT.VERTICAL;
                    layout.marginHeight = 0;
                    layout.marginWidth = 0;
                    layout.spacing = 0;
                }
                ScrolledComposite scrolled = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
                scrolled.setExpandHorizontal(true);
                scrolled.setExpandVertical(true);

                scrolled.setMinWidth(600);
                scrolled.setMinHeight(400);

                Composite composite = tabFactory.getWidgetFactory().createComposite(scrolled);
                scrolled.setContent(composite);
                composite.setLayout(new FormLayout());
                FormData d = new FormData();
                d.left = new FormAttachment(0, 0);
                d.right = new FormAttachment(100, 0);
                d.top = new FormAttachment(0, 0);
                d.bottom = new FormAttachment(100, 0);
                composite.setLayoutData(d);

                AbstractNotePropertyComposite c1 = new OpaqueNotePropertyComposite(composite, (Note) element, tabFactory);
                AbstractNotePropertyComposite c2 = new TextNotePropertyComposite(composite, (Note) element, tabFactory);
                FormData data = new FormData();
                data.top = new FormAttachment(c1.getComposite(), 20, SWT.DOWN);
                data.left = new FormAttachment(0, 0);
                data.right = new FormAttachment(100, 0);
                c2.getComposite().setLayoutData(data);

                parent.layout();
            }
        } else {
            tabFactory.getTabbedPropertyComposite().getCompactButton().setVisible(false);
            tabFactory.getTabbedPropertyComposite().getTableButton().setVisible(false);
            dc = new MultipleThreadDynamicComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.NO_FOCUS, category, element, true);
        }

        if (parent.getChildren().length == 0) {
            if (parent.getLayout() instanceof FillLayout) {
                FillLayout layout = (FillLayout) parent.getLayout();
                layout.type = SWT.VERTICAL;
                layout.marginHeight = 0;
                layout.marginWidth = 0;
                layout.spacing = 0;
            }

            Composite composite = tabFactory.getWidgetFactory().createComposite(parent);

            composite.setLayout(new FormLayout());
            FormData d = new FormData();
            d.left = new FormAttachment(2, 0);
            d.right = new FormAttachment(100, 0);
            d.top = new FormAttachment(5, 0);
            d.bottom = new FormAttachment(100, 0);
            composite.setLayoutData(d);

            Label alertText = new Label(composite, SWT.NONE);
            alertText.setText("No advanced settings.");
            alertText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            parent.layout();
        }
        if (dc != null) {
            dc.refresh();
        }
    }

    /**
     * DOC zwang Comment method "createButtons".
     */
    private void createButtonListener() {
        // TODO Auto-generated method stub
        // tabFactory.getTabbedPropertyComposite().getComposite().setBackground(
        // ImageProvider.getImage(EImage.COMPOSITE_BACKGROUND).getBackground());
        tabFactory.getTabbedPropertyComposite().getCompactButton().setVisible(true);
        tabFactory.getTabbedPropertyComposite().getTableButton().setVisible(true);

        tabFactory.getTabbedPropertyComposite().getCompactButton().addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                // TODO Auto-generated method stub

            }

            public void widgetSelected(SelectionEvent e) {
                // TODO Auto-generated method stub
                tabFactory.getTabbedPropertyComposite().setCompactView(true);
                getPreference().setValue(TalendDesignerPrefConstants.VIEW_OPTIONS, ComponentSettingsView.DEFAULT);
                tabFactory.getTabbedPropertyComposite().getCompactButton().setImage(ImageProvider.getImage(EImage.COMPACT_VIEW));
                tabFactory.getTabbedPropertyComposite().getTableButton().setImage(ImageProvider.getImage(EImage.NO_TABLE_VIEW));

                if (getDc() != null) {
                    // getDc().setCompactView(false);
                    getDc().dispose();
                    if (getParentMap().get(ComponentSettingsView.PARENT) != null
                            && getCategoryMap().get(ComponentSettingsView.CATEGORY) != null) {
                        dc = new MultipleThreadDynamicComposite(getParentMap().get(ComponentSettingsView.PARENT), SWT.H_SCROLL
                                | SWT.V_SCROLL | SWT.NO_FOCUS, getCategoryMap().get(ComponentSettingsView.CATEGORY), element,
                                true);
                        dc.refresh();
                    }
                }
            }
        });

        tabFactory.getTabbedPropertyComposite().getTableButton().addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {

            }

            public void widgetSelected(SelectionEvent e) {
                // TODO Auto-generated method stub
                tabFactory.getTabbedPropertyComposite().setCompactView(false);
                getPreference().setValue(TalendDesignerPrefConstants.VIEW_OPTIONS, ComponentSettingsView.TABLEVIEW);
                tabFactory.getTabbedPropertyComposite().getCompactButton().setImage(
                        ImageProvider.getImage(EImage.NO_COMPACT_VIEW));
                tabFactory.getTabbedPropertyComposite().getTableButton().setImage(ImageProvider.getImage(EImage.TABLE_VIEW));

                if (getDc() != null) {
                    // getDc().setCompactView(false);
                    getDc().dispose();
                    if (getParentMap().get(ComponentSettingsView.PARENT) != null
                            && getCategoryMap().get(ComponentSettingsView.CATEGORY) != null) {
                        dc = new MultipleThreadDynamicComposite(getParentMap().get(ComponentSettingsView.PARENT), SWT.H_SCROLL
                                | SWT.V_SCROLL | SWT.NO_FOCUS, getCategoryMap().get(ComponentSettingsView.CATEGORY), element,
                                false);
                        dc.refresh();
                    }
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        if (selectedPrimary) {
            if (getViewSite() != null) {
                getViewSite().getShell().setFocus();
            }
        } else {
            if (tabFactory.getTabComposite() != null) {
                tabFactory.getTabComposite().setFocus();
            }
        }
    }

    public boolean isCleaned() {
        return this.cleaned;
    }

    public void cleanDisplay() {
        tabFactory.setInput(null);
        tabFactory.setTitle(null, null);
        tabFactory.getTabbedPropertyComposite().getComposite()
                .setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        tabFactory.getTabbedPropertyComposite().getCompactButton().setVisible(false);
        tabFactory.getTabbedPropertyComposite().getTableButton().setVisible(false);
        if (tabFactory.getTabComposite() != null) {
            for (Control curControl : tabFactory.getTabComposite().getChildren()) {
                curControl.dispose();
            }
        }
        this.element = null;
        cleaned = true;
        selectedPrimary = true;
    }

    public void setElement(Element elem) {
        if (currentSelectedTab != null && currentSelectedTab.getElement().equals(elem) && !cleaned) {
            return;
        }

        EComponentCategory[] categories = getCategories(elem);
        final List<TalendPropertyTabDescriptor> descriptors = new ArrayList<TalendPropertyTabDescriptor>();
        for (EComponentCategory category : categories) {
            TalendPropertyTabDescriptor d = new TalendPropertyTabDescriptor(category);
            d.setElement(elem);
            descriptors.add(d);
            // if (category.hadSubCategories()) {
            // for (EComponentCategory subCategory : category.getSubCategories()) {
            // TalendPropertyTabDescriptor subc = new TalendPropertyTabDescriptor(subCategory);
            // subc.setElement(elem);
            // d.addSubItem(subc);
            // }
            // }
        }

        tabFactory.setInput(descriptors);
        setPropertiesViewerTitle(elem);
        cleaned = false;
        tabFactory.setSelection(new IStructuredSelection() {

            public Object getFirstElement() {
                return null;
            }

            public Iterator iterator() {
                return null;
            }

            public int size() {
                return 0;
            }

            public Object[] toArray() {
                return null;
            }

            public List toList() {
                List<TalendPropertyTabDescriptor> d = new ArrayList<TalendPropertyTabDescriptor>();

                if (descriptors.size() > 0) {
                    if (currentSelectedTab != null) {
                        for (TalendPropertyTabDescriptor ds : descriptors) {
                            if (ds.getCategory() == currentSelectedTab.getCategory()) {
                                d.add(ds);
                                return d;
                            }
                        }
                    }
                    d.add(descriptors.get(0));
                }
                return d;
            }

            public boolean isEmpty() {
                return false;
            }

        });

    }

    public void updatePropertiesViewerTitle() {
        if (this.element != null) {
            setPropertiesViewerTitle(this.element);
        }
    }

    /**
     * yzhang Comment method "setPropertiesViewerTitle".
     * 
     * @param elem
     */
    private void setPropertiesViewerTitle(Element elem) {
        String label = null;
        Image image = null;
        if (elem instanceof Node) {
            label = ((Node) elem).getLabel();

            String uniqueName = ((Node) elem).getUniqueName();
            if (!label.equals(uniqueName)) {
                label = label + "(" + uniqueName + ")";
            }

            image = new Image(Display.getDefault(), ((Node) elem).getComponent().getIcon24().getImageData());
        } else if (elem instanceof Connection) {
            label = ((Connection) elem).getElementName();
            image = ImageProvider.getImage(EImage.RIGHT_ICON);
        } else if (elem instanceof Note) {
            label = "Note";
            image = ImageProvider.getImage(EImage.PASTE_ICON);
        } else if (elem instanceof SubjobContainer) {
            label = "Subjob";
            image = ImageProvider.getImage(EImage.PASTE_ICON);
        }
        tabFactory.setTitle(label, image);
    }

    /**
     * yzhang Comment method "getCategories".
     * 
     * @param elem
     * @return
     */
    private EComponentCategory[] getCategories(Element elem) {
        if (elem instanceof Connection) {
            return EElementType.CONNECTION.getCategories();
        } else if (elem instanceof Node) {
            // if (isAdvancedType(elem)) {
            return EElementType.ADVANCED_NODE.getCategories();
            // } else {
            // return EElementType.NODE.getCategories();
            // }
        } else if (elem instanceof Note) {
            return EElementType.NOTE.getCategories();
        } else if (elem instanceof SubjobContainer) {
            return EElementType.NOTE.getCategories();
        }
        return null;
    }

    public Element getElement() {
        return element;
    }

    /**
     * yzhang Comment method "isAdvancedType".
     * 
     * @param elem
     * @return
     */
    private boolean isAdvancedType(Element elem) {
        for (IElementParameter param : elem.getElementParameters()) {
            if (param.getCategory().equals(EComponentCategory.ADVANCED)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Getter for dc.
     * 
     * @return the dc
     */
    public Composite getDc() {
        return (Composite) this.dc;
    }

    // /**
    // * Getter for elementMap.
    // *
    // * @return the elementMap
    // */
    // public Map<String, Element> getElementMap() {
    // return this.elementMap;
    // }
}
