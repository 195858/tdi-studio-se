// ============================================================================
//
// Copyright (C) 2006-2013 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.gefabstractmap.figures.treesettings;

import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.talend.commons.ui.expressionbuilder.IExpressionBuilderDialogController;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.IService;
import org.talend.designer.gefabstractmap.figures.manager.TableManager;
import org.talend.designer.gefabstractmap.resource.ImageInfo;
import org.talend.designer.gefabstractmap.resource.ImageProviderMapper;
import org.talend.expressionbuilder.IExpressionBuilderDialogService;

/**
 * wchen class global comment. Detailled comment
 */
public abstract class FilterContainer extends Figure {

    private static final int DEFAULT_HEIGHT = 40;

    private TableManager tableManager;

    private ImageFigure button;

    private FilterTextArea textArea;

    private Composite parent;

    public FilterContainer(TableManager tableManager, Composite parent) {
        this.parent = parent;
        this.tableManager = tableManager;
        createContent();
    }

    private void createContent() {
        FilterContainerLayout manager = new FilterContainerLayout();
        manager.setVertical(false);
        manager.setSpacing(5);
        setLayoutManager(manager);

        textArea = new FilterTextArea();
        textArea.setText(tableManager.getExpressionFilter());
        this.add(textArea);

        textArea.setOpaque(true);
        setBackgroundColor(ColorConstants.white);

        button = new ImageFigure(ImageProviderMapper.getImage(ImageInfo.FILTER_BUTTON));
        addButtonListener();
        this.add(button);

        setOpaque(true);
        setBackgroundColor(ColorConstants.yellow);
        setBorder(new MarginBorder(2));

    }

    protected abstract Command getFilterChangeCommand(Object model, String newValue);

    private void addButtonListener() {
        IService expressionBuilderDialogService = GlobalServiceRegister.getDefault().getService(
                IExpressionBuilderDialogService.class);

        final IExpressionBuilderDialogController dialog = ((IExpressionBuilderDialogService) expressionBuilderDialogService)
                .getExpressionBuilderInstance(parent, null, null);

        button.addMouseListener(new MouseListener() {

            @Override
            public void mousePressed(MouseEvent me) {
                if (dialog instanceof TrayDialog) {
                    TrayDialog parentDialog = (TrayDialog) dialog;
                    dialog.setDefaultExpression(tableManager.getExpressionFilter());
                    if (Window.OK == parentDialog.open()) {
                        String expressionForTable = dialog.getExpressionForTable();
                        tableManager.setExpressionFilter(expressionForTable);
                        tableManager.getEditPart().getViewer().getEditDomain().getCommandStack()
                                .execute(getFilterChangeCommand(tableManager.getModel(), expressionForTable));
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseDoubleClicked(MouseEvent me) {
                // TODO Auto-generated method stub

            }

        });
    }

    public void update() {
        textArea.setText(tableManager.getExpressionFilter());
    }

    class FilterContainerLayout extends ToolbarLayout {

        @Override
        public void layout(IFigure parent) {
            List children = parent.getChildren();
            int numChildren = children.size();

            Rectangle clientArea = parent.getClientArea();
            int x = clientArea.x;
            int y = clientArea.y;

            Dimension prefSizes[] = new Dimension[numChildren];
            Dimension minSizes[] = new Dimension[numChildren];

            int wHint = -1;
            int hHint = -1;
            hHint = parent.getClientArea(Rectangle.SINGLETON).height;
            wHint = parent.getClientArea(Rectangle.SINGLETON).width;

            IFigure child;

            for (int i = 0; i < numChildren; i++) {
                child = (IFigure) children.get(i);
                prefSizes[i] = getChildPreferredSize(child, wHint, hHint);
                minSizes[i] = getChildMinimumSize(child, wHint, hHint);

            }
            Insets insets = parent.getBorder().getInsets(null);

            if (numChildren >= 2) {
                int avaliableWith = clientArea.width - insets.left - insets.right - spacing;
                IFigure child0 = (IFigure) children.get(0);

                Rectangle newBounds = new Rectangle(x + insets.left, y, prefSizes[0].width, DEFAULT_HEIGHT);
                newBounds.width = avaliableWith - prefSizes[1].width;
                child0.setBounds(newBounds);

                x = x + newBounds.width + spacing;

                IFigure child1 = (IFigure) children.get(1);
                newBounds = new Rectangle(x, y, prefSizes[1].width, DEFAULT_HEIGHT);
                child1.setBounds(newBounds);
            }

        }

        @Override
        protected Dimension calculatePreferredSize(IFigure container, int wHint, int hHint) {
            Dimension dimension = super.calculatePreferredSize(container, wHint, hHint);
            Insets insets = container.getBorder().getInsets(null);
            dimension.height = DEFAULT_HEIGHT + insets.top + insets.bottom;
            return dimension;
        }

    }

}
