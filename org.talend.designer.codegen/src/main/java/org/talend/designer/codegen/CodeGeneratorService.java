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
package org.talend.designer.codegen;

import org.eclipse.core.runtime.jobs.Job;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.ILibraryManagerService;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.language.LanguageManager;
import org.talend.core.model.process.IProcess;
import org.talend.designer.codegen.i18n.Messages;
import org.talend.designer.codegen.model.CodeGeneratorEmittersPoolFactory;
import org.talend.designer.core.ICamelDesignerCoreService;
import org.talend.designer.core.IDesignerCoreService;
import org.talend.repository.model.ComponentsFactoryProvider;

/**
 * DOC bqian class global comment. Provides services for CodeGenerator plugin. <br/>
 * 
 * $Id: talend-code-templates.xml 1 2006-09-29 17:06:40 +0000 (星期五, 29 九月 2006) nrousseau $
 * 
 */
public class CodeGeneratorService implements ICodeGeneratorService {

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.codegen.ICodeGeneratorFactory#getCodeGenerator()
     */
    public ICodeGenerator createCodeGenerator() {
        return new CodeGenerator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.codegen.ICodeGeneratorFactory#getCodeGenerator(org.talend.core.model.process.IProcess,
     * boolean, boolean, boolean, java.lang.String)
     */
    public ICodeGenerator createCodeGenerator(IProcess process, boolean statistics, boolean trace, String... options) {
        return new CodeGenerator(process, statistics, trace, options);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.codegen.ICodeGeneratorService#getRoutineSynchronizer()
     */
    public ITalendSynchronizer createPerlRoutineSynchronizer() {
        return new PerlRoutineSynchronizer();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.codegen.ICodeGeneratorService#createJavaRoutineSynchronizer()
     */
    public ITalendSynchronizer createJavaRoutineSynchronizer() {
        // TODO Auto-generated method stub
        return new JavaRoutineSynchronizer();
    }

    public ITalendSynchronizer createRoutineSynchronizer() {
        ECodeLanguage lan = LanguageManager.getCurrentLanguage();
        if (lan.equals(ECodeLanguage.PERL)) {
            return createPerlRoutineSynchronizer();
        } else if (lan.equals(ECodeLanguage.JAVA)) {
            return createJavaRoutineSynchronizer();
        }
        throw new IllegalArgumentException(Messages.getString("CodeGeneratorService.invalidLanguage1")); //$NON-NLS-1$
    }

    public ITalendSynchronizer createCamelBeanSynchronizer() {
        ECodeLanguage lan = LanguageManager.getCurrentLanguage();
        if (GlobalServiceRegister.getDefault().isServiceRegistered(ICamelDesignerCoreService.class)) {
            ICamelDesignerCoreService service = (ICamelDesignerCoreService) GlobalServiceRegister.getDefault().getService(
                    ICamelDesignerCoreService.class);
            if (lan.equals(ECodeLanguage.JAVA)) {
                return service.createCamelJavaSynchronizer();
            }
        }
        return null;
    }

    public ISQLPatternSynchronizer getSQLPatternSynchronizer() {
        ECodeLanguage lan = LanguageManager.getCurrentLanguage();
        if (lan.equals(ECodeLanguage.PERL)) {
            return new PerlSQLPatternSynchronizer();
        } else if (lan.equals(ECodeLanguage.JAVA)) {
            return new JavaSQLPatternSynchronizer();
        }
        throw new IllegalArgumentException(Messages.getString("CodeGeneratorService.invalidLanguage2")); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.designer.codegen.ICodeGeneratorService#initializeTemplates(org.eclipse.core.runtime.IProgressMonitor)
     */
    public Job initializeTemplates() {
        return CodeGeneratorEmittersPoolFactory.initialize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.codegen.ICodeGeneratorService#refreshTemplates()
     */
    public void refreshTemplates() {
        // this will force to refresh all components libs when install run ctrl+f3
        ILibraryManagerService librairesManagerService = (ILibraryManagerService) GlobalServiceRegister.getDefault().getService(
                ILibraryManagerService.class);
        librairesManagerService.clearCache();

        ComponentsFactoryProvider.getInstance().resetCache();
        CodeGeneratorEmittersPoolFactory.initialize();
        CorePlugin.getDefault().getLibrariesService().syncLibraries();
        IDesignerCoreService designerCoreService = (IDesignerCoreService) GlobalServiceRegister.getDefault().getService(
                IDesignerCoreService.class);
        designerCoreService.getLastGeneratedJobsDateMap().clear();
    }
}
