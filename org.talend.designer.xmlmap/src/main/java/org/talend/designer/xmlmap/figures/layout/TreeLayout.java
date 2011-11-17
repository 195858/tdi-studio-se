// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.xmlmap.figures.layout;

import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ScrollPane;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.talend.designer.xmlmap.figures.treeNode.TableTree;
import org.talend.designer.xmlmap.figures.treesettings.FilterContainer;
import org.talend.designer.xmlmap.figures.treesettings.InputTreeSettingContainer;
import org.talend.designer.xmlmap.figures.treesettings.OutputTreeSettingContainer;
import org.talend.designer.xmlmap.model.emf.xmlmap.AbstractInOutTree;

/**
 * wchen class global comment. Detailled comment
 */
public class TreeLayout extends ToolbarLayout {

    private AbstractInOutTree abstractTree;

    public TreeLayout(AbstractInOutTree outputTree) {
        this.abstractTree = outputTree;
    }

    @Override
    public void layout(IFigure parent) {
        List children = parent.getChildren();
        int numChildren = children.size();
        Rectangle clientArea = transposer.t(parent.getClientArea());
        int x = clientArea.x;
        int y = clientArea.y;
        int availableHeight = clientArea.height;

        Dimension prefSizes[] = new Dimension[numChildren];
        Dimension minSizes[] = new Dimension[numChildren];

        int wHint = -1;
        int hHint = -1;
        if (isHorizontal()) {
            hHint = parent.getClientArea(Rectangle.SINGLETON).height;
        } else {
            wHint = parent.getClientArea(Rectangle.SINGLETON).width;
        }
        IFigure child;
        int totalHeight = 0;
        int totalMinHeight = 0;
        int prefMinSumHeight = 0;

        for (int i = 0; i < numChildren; i++) {
            child = (IFigure) children.get(i);

            prefSizes[i] = transposer.t(getChildPreferredSize(child, wHint, hHint));
            minSizes[i] = transposer.t(getChildMinimumSize(child, wHint, hHint));

            totalHeight += prefSizes[i].height;
            totalMinHeight += minSizes[i].height;
        }
        totalHeight += (numChildren - 1) * spacing;
        totalMinHeight += (numChildren - 1) * spacing;
        prefMinSumHeight = totalHeight - totalMinHeight;

        int amntShrinkHeight = totalHeight - Math.max(availableHeight, totalMinHeight);

        if (amntShrinkHeight < 0) {
            amntShrinkHeight = 0;
        }

        for (int i = 0; i < numChildren; i++) {
            int amntShrinkCurrentHeight = 0;
            int prefHeight = prefSizes[i].height;
            int minHeight = minSizes[i].height;
            int prefWidth = prefSizes[i].width;
            int minWidth = minSizes[i].width;
            Rectangle newBounds = new Rectangle(x, y, prefWidth, prefHeight);

            child = (IFigure) children.get(i);
            if (abstractTree != null) {
                if (child instanceof OutputTreeSettingContainer || child instanceof InputTreeSettingContainer) {
                    if (!abstractTree.isActivateCondensedTool()) {
                        child.setBounds(new Rectangle(x, y, 0, 0));
                        continue;
                    }

                }

                if (child instanceof FilterContainer) {
                    if (!abstractTree.isActivateExpressionFilter()) {
                        child.setBounds(new Rectangle(x, y, 0, 0));
                        continue;
                    }
                }
            }

            int width = Math.min(prefWidth, transposer.t(child.getMaximumSize()).width);
            if (matchWidth)
                width = transposer.t(child.getMaximumSize()).width;
            width = Math.max(minWidth, Math.min(clientArea.width, width));
            newBounds.width = width;

            child.setBounds(transposer.t(newBounds));

            amntShrinkHeight -= amntShrinkCurrentHeight;
            prefMinSumHeight -= (prefHeight - minHeight);
            y += newBounds.height + spacing;

            if (child instanceof ScrollPane) {
                IFigure contents = ((ScrollPane) child).getViewport().getContents();
                if (contents instanceof TableTree) {
                    ((TableTree) contents).setDefautTableWidth(newBounds.width);
                }
            }

        }
    }

    protected Dimension calculatePreferredSize(IFigure container, int wHint, int hHint) {
        Insets insets = container.getInsets();
        if (isHorizontal()) {
            wHint = -1;
            if (hHint >= 0)
                hHint = Math.max(0, hHint - insets.getHeight());
        } else {
            hHint = -1;
            if (wHint >= 0)
                wHint = Math.max(0, wHint - insets.getWidth());
        }

        List children = container.getChildren();
        Dimension prefSize = calculateChildrenSize(children, wHint, hHint, true);
        // Do a second pass, if necessary
        if (wHint >= 0 && prefSize.width > wHint) {
            prefSize = calculateChildrenSize(children, prefSize.width, hHint, true);
        } else if (hHint >= 0 && prefSize.width > hHint) {
            prefSize = calculateChildrenSize(children, wHint, prefSize.width, true);
        }

        prefSize.height += Math.max(0, children.size() - 1) * spacing;
        return transposer.t(prefSize).expand(insets.getWidth(), insets.getHeight()).union(getBorderPreferredSize(container));
    }

    private Dimension calculateChildrenSize(List children, int wHint, int hHint, boolean preferred) {
        Dimension childSize;
        IFigure child;
        int height = 0, width = 0;
        for (int i = 0; i < children.size(); i++) {
            child = (IFigure) children.get(i);
            if (abstractTree != null) {
                if (child instanceof OutputTreeSettingContainer || child instanceof InputTreeSettingContainer) {
                    if (!abstractTree.isActivateCondensedTool()) {
                        continue;
                    }
                }

                if (child instanceof FilterContainer) {
                    if (!abstractTree.isActivateExpressionFilter()) {
                        continue;
                    }
                }
            }
            childSize = transposer.t(preferred ? getChildPreferredSize(child, wHint, hHint) : getChildMinimumSize(child, wHint,
                    hHint));
            height += childSize.height;
            width = Math.max(width, childSize.width);

            // header figure must be the first figure , or there will be problem here
            if (abstractTree.isMinimized()) {
                break;
            }
        }
        return new Dimension(width, height);
    }

}
