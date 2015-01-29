// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.runprocess.bigdata;

import org.talend.core.model.process.IProcess;
import org.talend.core.model.process.IProcess2;
import org.talend.core.model.properties.Property;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.spark.SparkJavaProcessor;

/**
 * created by rdubois on 28 janv. 2015 Detailled comment
 *
 */
public class RunSparkProcessContext extends RunBigDataProcessContext {

    /**
     * DOC rdubois RunSparkProcessContext constructor comment.
     * 
     * @param process
     */
    public RunSparkProcessContext(IProcess2 process) {
        super(process);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.designer.runprocess.bigdata.RunBigDataProcessContext#createProcessor(org.talend.core.model.process
     * .IProcess, org.talend.core.model.properties.Property, boolean)
     */
    @Override
    protected IProcessor createProcessor(IProcess process, Property property, boolean filenameFromLabel) {
        return new SparkJavaProcessor(process, property, filenameFromLabel);
    }

}
