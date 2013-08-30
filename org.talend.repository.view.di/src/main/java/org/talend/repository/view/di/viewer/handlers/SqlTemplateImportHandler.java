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
package org.talend.repository.view.di.viewer.handlers;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.repository.items.importexport.handlers.imports.ImportRepTypeHandler;
import org.talend.repository.items.importexport.handlers.model.ItemRecord;
import org.talend.repository.model.RepositoryConstants;

/**
 * DOC ggu class global comment. Detailled comment
 */
public class SqlTemplateImportHandler extends ImportRepTypeHandler {

    /**
     * DOC ggu SqlTemplateImportHandler constructor comment.
     */
    public SqlTemplateImportHandler() {
        super();
    }

    @Override
    protected boolean validRelativePath(IPath relativePath) {
        boolean valid = super.validRelativePath(relativePath);
        if (valid) { // ignore system items
            ERepositoryObjectType type = ERepositoryObjectType.SQLPATTERNS;
            if (type != null) {
                IPath pah = relativePath.makeRelativeTo(new Path(type.getFolder()));
                pah = pah.removeFirstSegments(1); // remove for database name
                if (new Path(RepositoryConstants.SYSTEM_DIRECTORY).isPrefixOf(pah)) {
                    valid = false; // system items
                }
            }
        }
        return valid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.repository.items.importexport.handlers.imports.ImportRepTypeHandler#isSameName(org.talend.repository
     * .items.importexport.ui.wizard.imports.models.ItemRecord, org.talend.core.model.repository.IRepositoryViewObject)
     */
    @Override
    protected boolean isSameName(ItemRecord itemRecord, IRepositoryViewObject repObject) {
        boolean sameName = super.isSameName(itemRecord, repObject);
        if (sameName) {
            // To check SQLPattern in same path. see bug 0005038: unable to add a SQLPattern into repository.
            if (!repObject.getPath().equals(itemRecord.getProperty().getItem().getState().getPath())) {
                sameName = false; // not in same folder
            }
        }
        return sameName;
    }

}
