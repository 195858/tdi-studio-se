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
package org.talend.designer.dbmap.language.hive;

import org.talend.designer.dbmap.language.generation.DbGenerationManager;

/**
 * created by wchen on 2012-8-13 Detailled comment
 * 
 */
public class HiveGenerationManager extends DbGenerationManager {

    /**
     * DOC talend2 HiveGenerationManager constructor comment.
     * 
     * @param language
     */
    public HiveGenerationManager() {
        super(new HiveLanguage());
    }

}
