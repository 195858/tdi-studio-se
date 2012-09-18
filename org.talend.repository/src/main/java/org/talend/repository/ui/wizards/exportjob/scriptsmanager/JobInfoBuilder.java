// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.ui.wizards.exportjob.scriptsmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Properties;

import org.talend.commons.utils.VersionUtils;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.general.Project;
import org.talend.core.model.process.JobInfo;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.repository.ProjectManager;
import org.talend.repository.documentation.ExportFileResource;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobScriptsManager.ExportChoice;

public class JobInfoBuilder {
	private String dir = null;

	private String jobPropertyFile = null;

    private String filename = null;
    
    private ExportFileResource process = null;
    
    private static final String UNDER_LINE_CHAR = "_"; //$NON-NLS-1$
    
    private static final String PROJECT_ID = "projectId"; //$NON-NLS-1$
    
    private static final String PROJECT_NAME = "projectName"; //$NON-NLS-1$
    
    private static final String JOB_ID = "jobId"; //$NON-NLS-1$
    
    private static final String JOB_NAME = "job"; //$NON-NLS-1$
    
    private static final String JOB_VERSION = "jobVersion"; //$NON-NLS-1$
    
    private static final String DATE = "date"; //$NON-NLS-1$
    
    private static final String BRANCH = "branch"; //$NON-NLS-1$
    
    private static final String COMMANDLINE_VERSION = "cmdLineVersion"; //$NON-NLS-1$
    
    private static final String CONTEXT_NAME = "contextName"; //$NON-NLS-1$
    
    private static final String APPLY_CONTEXY_CHILDREN = "applyContextToChildren"; //$NON-NLS-1$
    
    private static final String ADD_STATIC_CODE = "statistics"; //$NON-NLS-1$
    
    private Map<ExportChoice, Object> exportChoice;

    /**
     * Constructure.
     * 
     * @param root
     * @param jarFile
     * @param jarName
     * @param includeDirs
     */
    JobInfoBuilder(ExportFileResource process,String root, String jobInfoFile) {
    	this.process = process;
        this.dir = root;
        this.jobPropertyFile = jobInfoFile;
        File file = new File(jobInfoFile);
        this.filename = file.getName();
    }
    
    JobInfoBuilder(ExportFileResource process,Map<ExportChoice, Object> exportChoice,String root, String jobInfoFile) {
    	this.process = process;
        this.dir = root;
        this.jobPropertyFile = jobInfoFile;
        File file = new File(jobInfoFile);
        this.filename = file.getName();
        this.exportChoice = exportChoice;
    }

    private Properties getPropery(){
    	Properties property = new Properties();
        return property;
    }

    /**
     * Builds the jar file.
     * 
     * @throws Exception
     */
    public void buildProperty() {
    	Properties property = getPropery();
        exportJobInfo(property);
    }

    private void exportJobInfo(Properties propertyFile) {
    	File jobInfoFile = new File(jobPropertyFile);
    	try {
			if (!jobInfoFile.exists())   
				jobInfoFile.createNewFile();   
			InputStream fis = new FileInputStream(jobInfoFile);   
			propertyFile.load(fis);   
			fis.close();
			OutputStream fos = new FileOutputStream(jobPropertyFile);
			setProperty(propertyFile);
			propertyFile.store(fos, "just for tac");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}   
    }
    
    private void setProperty(Properties propertyFile){
    	ProcessItem processItem = (ProcessItem) process.getItem();
        JobInfo jobInfo = new JobInfo(processItem, processItem.getProcess().getDefaultContext(), processItem.getProperty().getVersion());
        Project currentProject = ProjectManager.getInstance().getCurrentProject();
        propertyFile.setProperty(PROJECT_ID, String.valueOf(currentProject.getEmfProject().getId()));
        propertyFile.setProperty(PROJECT_NAME, currentProject.getLabel());
        
        String branchKey = IProxyRepositoryFactory.BRANCH_SELECTION + UNDER_LINE_CHAR + currentProject.getTechnicalLabel();
        Context ctx = CoreRuntimePlugin.getInstance().getContext();
        RepositoryContext rc = (RepositoryContext) ctx.getProperty(Context.REPOSITORY_CONTEXT_KEY);
        if (rc.getFields().containsKey(branchKey)
                && rc.getFields().get(branchKey) != null) {
           String branchSelection = rc.getFields().get(branchKey);
           propertyFile.setProperty(BRANCH, branchSelection);
            
        }
        
        propertyFile.setProperty(JOB_ID, jobInfo.getJobId());
        propertyFile.setProperty(JOB_NAME, jobInfo.getJobName());
        propertyFile.setProperty(JOB_VERSION, jobInfo.getJobVersion());
        propertyFile.setProperty(CONTEXT_NAME, jobInfo.getContextName());
        
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        propertyFile.setProperty(DATE, df.format(System.currentTimeMillis()));
        
        if(exportChoice != null){
        	propertyFile.setProperty(APPLY_CONTEXY_CHILDREN, String.valueOf(exportChoice.get(ExportChoice.applyToChildren)));
        	propertyFile.setProperty(ADD_STATIC_CODE, String.valueOf(exportChoice.get(ExportChoice.addStatistics)));
        }
        propertyFile.setProperty(COMMANDLINE_VERSION, VersionUtils.getVersion());
        
    }
}
