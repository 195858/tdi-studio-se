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
package org.talend.designer.core.ui.editor.connections;

import org.eclipse.draw2d.Clickable;
import org.eclipse.draw2d.Label;
import org.eclipse.swt.graphics.Image;
import org.talend.commons.ui.image.EImage;
import org.talend.commons.ui.image.ImageProvider;

/**
 * Figure used to represent a button with expanded and collapsed icon.
 */
public class CollapseFigure extends Clickable {

    private boolean collapsed;

    /**
     * DOC bqian CollapseFigure constructor comment.
     */
    public CollapseFigure() {
        super();
        setCollapsed(false);
        setStyle(STYLE_TOGGLE);
        // setBorder(new LineBorder(ColorConstants.darkGray, SWT.TOP | SWT.BOTTOM | SWT.LEFT));
    }

    /**
     * isCollapsed Utility method to determine if the IFigure is collapse or not.
     * 
     * @return true if collapse, false otherwise.
     */
    public boolean isCollapsed() {
        return collapsed;
    }

    /**
     * setCollapsed Setter method to change collapsed state of the figure. Will force update to repaint the figure to
     * reflect the changes.
     * 
     * @param b boolean true to set collapsed, false to uncollapse.
     */
    public void setCollapsed(boolean b) {
        collapsed = b;
        Image img;
        if (isCollapsed()) {
            img = ImageProvider.getImage(EImage.TRACES_EXPAND);
        } else {
            img = ImageProvider.getImage(EImage.TRACES_COLLAPSE);
        }
        setContents(new Label(img));
        // revalidate();
        // repaint();
    }
}
