// ============================================================================
//
// Copyright (C) 2006-2009 Talend Inc. - www.talend.com
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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.draw2d.ActionEvent;
import org.eclipse.draw2d.ActionListener;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.talend.commons.ui.image.EImage;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.commons.ui.image.OverlayImage.EPosition;
import org.talend.commons.utils.workbench.gef.SimpleHtmlFigure;
import org.talend.commons.utils.workbench.preferences.GlobalConstant;
import org.talend.core.ui.images.ECoreImage;
import org.talend.core.ui.images.OverlayImageProvider;

/**
 * DOC nrousseau class global comment. Detailled comment <br/>
 * 
 * $Id$
 * 
 */
public class ConnectionTraceFigure extends Figure {

    private static final String FIELD_SEP = "|"; //$NON-NLS-1$

    private static final String FIELD_EQUAL = "="; //$NON-NLS-1$

    private static final Color BACKGROUND = new Color(null, 220, 220, 220);

    private static final int MAX_VARIABLE_WIDTH = 70;

    private static final int MAX_VALUE_WIDTH = 100;

    private Connection connection;

    private boolean maximized;

    private ConnectionTraceFigure tooltip = null;

    private CollapseFigure collapseButton;

    public ConnectionTraceFigure(Connection connection, boolean maximized) {
        ToolbarLayout layout = new ToolbarLayout();
        setLayoutManager(layout);
        // setBorder(new SimpleRaisedBorder());
        this.connection = connection;
        this.maximized = maximized;
        if (maximized) {
            tooltip = new ConnectionTraceFigure(connection, false);
            this.setToolTip(tooltip);
            tooltip.setVisible(false);
        }

    }

    @Override
    public void paint(Graphics graphics) {
        // see bug 2074
        if (GlobalConstant.generatingScreenShoot) {
            return;
        }
        super.paint(graphics);
    }

    public void setTraceData(String data, boolean flag, boolean traceFlag) {
        if (data != null) {
            List childrens = this.getChildren();
            childrens.clear();
            boolean noVarNameDefined = false;

            Figure outlineFigure = new Figure();
            outlineFigure.setLayoutManager(new ToolbarLayout(true));

            if (tooltip != null) {
                collapseButton = new CollapseFigure();
                collapseButton.setCollapsed(connection.getConnectionTrace().isCollapse());
                collapseButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent event) {
                        connection.getConnectionTrace().setCollapse(!connection.getConnectionTrace().isCollapse());
                        collapseButton.setCollapsed(connection.getConnectionTrace().isCollapse());
                        refreshCollapseStatus();
                    }
                });
                outlineFigure.add(collapseButton);
            }
            int sepIndex = data.indexOf(FIELD_SEP); // index separator for row name

            String dataWithoutRowName = data.substring(sepIndex + 1);
            sepIndex = dataWithoutRowName.indexOf(FIELD_SEP);
            String lineNumber = dataWithoutRowName.substring(0, sepIndex);
            SimpleHtmlFigure titleFigure = new SimpleHtmlFigure();

            titleFigure.setText(""); //$NON-NLS-1$
            titleFigure.setText("<font color='#0000FF'> <b> " + connection.getConnectionLabel().getLabelText() //$NON-NLS-1$
                    + "</b></font>"); //$NON-NLS-1$ //$NON-NLS-2$
            if (tooltip != null) {
                titleFigure.setBackgroundColor(ColorConstants.white);
                titleFigure.setOpaque(false);
            }
            titleFigure.getPreferredSize().expand(20, 2);

            SimpleHtmlFigure titleFigureSe = new SimpleHtmlFigure();

            titleFigureSe.setText(" <font color='#808080'>Current row : " + lineNumber + "</font>"); //$NON-NLS-1$ //$NON-NLS-2$
            if (tooltip != null) {
                titleFigureSe.setBackgroundColor(ColorConstants.white);
                titleFigureSe.setOpaque(false);
            }
            titleFigureSe.getPreferredSize().expand(20, 2);

            outlineFigure.add(titleFigure);

            ImageFigure figure = new ImageFigure(getTraceConnectionImage(flag));
            outlineFigure.add(figure);

            outlineFigure.add(titleFigureSe);

            outlineFigure.setBorder(new LineBorder(ColorConstants.darkGray, SWT.LEFT | SWT.RIGHT | SWT.TOP | SWT.BOTTOM));
            add(outlineFigure);

            Dimension size = titleFigure.getPreferredSize().getCopy();

            int variableWidth = 0;
            int valueWidth = 0;
            String lineInfo = dataWithoutRowName.substring(sepIndex + 1);
            StringTokenizer st = new StringTokenizer(lineInfo, FIELD_SEP);
            while (st.hasMoreTokens()) {
                String str = st.nextToken();
                int valueStart = str.indexOf(FIELD_EQUAL);
                if (valueStart != -1) {
                    String formatedVariable = "<font color='#000000'>  <b>" + str.substring(0, valueStart) //$NON-NLS-1$
                            + "</b></font>"; //$NON-NLS-1$
                    String formatedValue = "<font color='#FF8040'> <b>" + str.substring(valueStart + 1) + "</b></font>"; //$NON-NLS-1$ //$NON-NLS-2$
                    SimpleHtmlFigure var = new SimpleHtmlFigure();
                    var.setText(formatedVariable);
                    SimpleHtmlFigure value = new SimpleHtmlFigure();
                    value.setText(formatedValue);
                    Dimension varSize = var.getPreferredSize();
                    varSize.expand(0, 3);
                    var.setPreferredSize(varSize);
                    if (varSize.width > variableWidth) {
                        variableWidth = varSize.width;
                    }
                    Dimension valueSize = value.getPreferredSize();
                    valueSize.expand(0, 3);
                    value.setPreferredSize(valueSize);
                    if (valueSize.width > valueWidth) {
                        valueWidth = valueSize.width;
                    }
                    size.expand(0, varSize.height);
                } else {
                    noVarNameDefined = true;
                    String formatedValue = "<font color='#FF8040'> <b>" + str + "</b></font>"; //$NON-NLS-1$ //$NON-NLS-2$
                    SimpleHtmlFigure value = new SimpleHtmlFigure();
                    value.setText(formatedValue);
                    Dimension valueSize = value.getPreferredSize();
                    if (valueSize.width > valueWidth) {
                        valueWidth = valueSize.width;
                    }
                    size.expand(0, valueSize.height);
                }
            }
            variableWidth += 10;
            valueWidth += 10;

            if (maximized) {
                if (variableWidth > MAX_VARIABLE_WIDTH) {
                    variableWidth = MAX_VARIABLE_WIDTH;
                }

                if (valueWidth > MAX_VALUE_WIDTH) {
                    valueWidth = MAX_VALUE_WIDTH;
                }
            }

            if ((variableWidth + valueWidth) < titleFigure.getPreferredSize().width) {
                valueWidth = titleFigure.getPreferredSize().width - variableWidth;
            }
            if (noVarNameDefined) {
                if (titleFigure.getPreferredSize().width > valueWidth) {
                    valueWidth = titleFigure.getPreferredSize().width;
                }
            }

            st = new StringTokenizer(lineInfo, FIELD_SEP);
            int nbVar = 0;
            Figure variableFigure = null;
            while (st.hasMoreTokens()) {
                String str = st.nextToken();
                int valueStart = str.indexOf(FIELD_EQUAL);
                if (valueStart != -1) {
                    String formatedVariable = "<font color='#000000'>  <b>" + str.substring(0, valueStart) //$NON-NLS-1$
                            + "</b></font>"; //$NON-NLS-1$
                    String formatedValue = "<font color='#FF8040'> <b>" + str.substring(valueStart + 1) + "</b></font>"; //$NON-NLS-1$ //$NON-NLS-2$
                    SimpleHtmlFigure var = new SimpleHtmlFigure();
                    var.setText(formatedVariable);
                    SimpleHtmlFigure value = new SimpleHtmlFigure();
                    value.setText(formatedValue);
                    Dimension valueSize = value.getPreferredSize();
                    valueSize.expand(0, 3);
                    value.setPreferredSize(valueSize);
                    value.setPreferredSize(valueWidth, valueSize.height);
                    var.setBorder(new LineBorder(ColorConstants.darkGray, SWT.RIGHT));
                    Dimension varSize = var.getPreferredSize();
                    varSize.expand(0, 3);
                    var.setPreferredSize(varSize);
                    var.setPreferredSize(variableWidth, varSize.height);

                    ToolbarLayout variableLayout = new ToolbarLayout(true);
                    variableFigure = new Figure();
                    variableFigure.setLayoutManager(variableLayout);
                    variableFigure.add(var);
                    variableFigure.add(value);
                    add(variableFigure);
                } else {
                    String formatedValue = "<font color='#FF8040'> <b> " + str + "</b></font>"; //$NON-NLS-1$ //$NON-NLS-2$
                    SimpleHtmlFigure value = new SimpleHtmlFigure();
                    value.setText(formatedValue);

                    Dimension valueSize = value.getPreferredSize();
                    valueSize.expand(0, 3);
                    value.setPreferredSize(valueSize);
                    value.setPreferredSize(valueWidth, valueSize.height);

                    ToolbarLayout variableLayout = new ToolbarLayout(true);
                    variableFigure = new Figure();
                    variableFigure.setLayoutManager(variableLayout);
                    variableFigure.add(value);

                    add(variableFigure);
                }
                if (tooltip != null) {
                    variableFigure.setBorder(new LineBorder(ColorConstants.darkGray, SWT.LEFT | SWT.RIGHT));
                }
                variableFigure.setOpaque(true);
                if ((nbVar % 2) != 0) {
                    if (tooltip != null) {
                        variableFigure.setBackgroundColor(ColorConstants.white);
                    }
                } else {
                    variableFigure.setBackgroundColor(BACKGROUND);
                }

                nbVar++;
            }
            if (tooltip != null) {
                if (variableFigure != null) {
                    variableFigure.setBorder(new LineBorder(ColorConstants.darkGray, SWT.LEFT | SWT.BOTTOM | SWT.RIGHT));
                }
            }
            if (noVarNameDefined) {
                size.width = valueWidth;
            } else {
                size.width = variableWidth + valueWidth;
            }

            if (size.width < titleFigure.getPreferredSize().width) {
                size.width = titleFigure.getPreferredSize().width;
            }
            size.width = size.width * 2;
            size.expand(5, 3);
            setPreferredSize(size);
            setVisible(true);

        } else {

            if (traceFlag) {
                Image enableImage = getTraceConnectionImage(flag);

                setPreferredSize(enableImage.getImageData().width, enableImage.getImageData().height);

                this.getChildren().clear();
                ImageFigure figure = new ImageFigure(enableImage);
                add(figure);

                setVisible(true);
            } else {
                setPreferredSize(0, 0);
                setVisible(false);
            }
            tooltip = null;

        }
        if (tooltip != null) {
            tooltip.setTraceData(data, flag, traceFlag);
        }
        contents = new ArrayList(getChildren());
        refreshCollapseStatus();
    }

    /**
     * 
     * cLi Comment method "getTraceConnectionImage".
     * 
     * feature 6355.
     */
    private Image getTraceConnectionImage(boolean enable) {
        Image image = null;
        if (enable) {
            image = ImageProvider.getImage(ECoreImage.TRACE_ON);
        } else {
            image = ImageProvider.getImage(ECoreImage.TRACE_OFF);
        }
        if (image != null && connection.getTracesCondition() != null) {
            image = OverlayImageProvider.getImageForOverlay(image, EImage.INFORMATION_SMALL, EPosition.BOTTOM_LEFT);
        }
        return image;
    }

    private List contents = null;

    /**
     * Refresh the collapse status of the content table according to the selection of collapse button.
     */
    private void refreshCollapseStatus() {
        if (collapseButton == null) {
            return;
        }

        boolean collapse = connection.getConnectionTrace().isCollapse();
        if (collapse) {
            List list = new ArrayList(getChildren());
            for (int i = 1; i < list.size(); i++) {
                remove((IFigure) list.get(i));
            }
            IFigure figure = (IFigure) contents.get(0);
            this.getPreferredSize().height = figure.getPreferredSize().height;
            setPreferredSize(getPreferredSize());
        } else {
            removeAll();
            for (int i = 0; i < contents.size(); i++) {
                IFigure figure = (IFigure) contents.get(i);
                add(figure);
            }
            IFigure figure = (IFigure) contents.get(0);
            this.getPreferredSize().height = figure.getPreferredSize().height * contents.size();
            setPreferredSize(getPreferredSize());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.draw2d.Figure#getPreferredSize(int, int)
     */
    @Override
    public Dimension getPreferredSize(int hint, int hint2) {
        // TODO Auto-generated method stub
        return super.getPreferredSize(hint, hint2);
    }
}
