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
package org.talend.componentdesigner.ui.composite.xmltree;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.datatools.enablement.oda.xml.util.ui.ATreeNode;
import org.eclipse.datatools.enablement.oda.xml.util.ui.SchemaPopulationUtil;
import org.talend.core.model.ModelPlugin;

/**
 * DOC rli class global comment. Detailled comment
 */
public class ATreeNodeUtil {

    private static ATreeNode rootTreeNode = null;

    static {
        try {
            URL url = ModelPlugin.getDefault().getBundle().getResource("/model/Component.xsd");
            url = FileLocator.toFileURL(url);
            String fileAbsolutePath = url.getFile();
            rootTreeNode = SchemaPopulationUtil.getSchemaTree(fileAbsolutePath, true, 10);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ATreeNode getTreeNodeByPath(String xPath) {
        String[] nodeNameSeq = xPath.split("/");

        ATreeNode resultNode = rootTreeNode;
        for (String nodeName : nodeNameSeq) {
            resultNode = findTreeNode(nodeName, resultNode);
            if (resultNode == null) {
                break;
            }
        }
        return resultNode;
    }

    private static ATreeNode findTreeNode(String nodeName, ATreeNode rootTreeNode) {
        Object[] childNodes = rootTreeNode.getChildren();

        ATreeNode aTreeNode = null;
        for (Object node : childNodes) {
            if (nodeName.equals(((ATreeNode) node).getValue())) {
                aTreeNode = (ATreeNode) node;
            } else {
                continue;
            }
        }
        return aTreeNode;

    }

    public static String[] getChildNodeNames(ATreeNode aTreeNode) {
        return null;
    }

}
