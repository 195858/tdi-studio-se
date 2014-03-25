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
package org.talend.designer.codegen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.talend.commons.exception.SystemException;
import org.talend.commons.utils.generation.JavaUtils;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.properties.SQLPatternItem;
import org.talend.designer.runprocess.IRunProcessService;

/**
 * SQLPattern synchronizer of java project.
 * 
 * bqian class global comment. Detailled comment <br/>
 * 
 * $Id: JavaRoutineSynchronizer.java JavaRoutineSynchronizer 2007-2-2 下午03:29:12 +0000 (下午03:29:12, 2007-2-2 2007)
 * yzhang $
 * 
 */
public class PerlSQLPatternSynchronizer extends AbstractSQLPatternSynchronizer {

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.codegen.ISQLPatternSynchronizer#getSQLPattern(org.talend.core.model.properties.SQLPatternItem)
     */
    public IFile getSQLPatternFile(SQLPatternItem item) throws SystemException {
        try {
            IRunProcessService service = CodeGeneratorActivator.getDefault().getRunProcessService();
            IProject javaProject = service.getProject(ECodeLanguage.PERL);
            initSQLPatternFolder(javaProject, item);

            IFile file = javaProject.getFile(JavaUtils.JAVA_SQLPATTERNS_DIRECTORY + "/" + item.getEltName() + "/" //$NON-NLS-1$ //$NON-NLS-2$
                    + JavaUtils.JAVA_SYSTEM_ROUTINES_DIRECTORY + "/" + item.getProperty().getLabel() //$NON-NLS-1$
                    + JavaUtils.JAVA_SQLPATTERN_EXTENSION);
            return file;
        } catch (CoreException e) {
            throw new SystemException(e);
        }
    }

    private void initSQLPatternFolder(IProject javaProject, SQLPatternItem item) throws CoreException {
        IPath path = new Path(JavaUtils.JAVA_SQLPATTERNS_DIRECTORY);
        IFolder rep = javaProject.getFolder(path);
        if (!rep.exists()) {
            rep.create(true, true, null);
        }
        path = path.append(item.getEltName());
        rep = javaProject.getFolder(path);
        if (!rep.exists()) {
            rep.create(true, true, null);
        }

        path = path.append(JavaUtils.JAVA_SYSTEM_ROUTINES_DIRECTORY);
        rep = javaProject.getFolder(path);
        if (!rep.exists()) {
            rep.create(true, true, null);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.codegen.ISQLPatternSynchronizer#syncSQLPattern(org.talend.core.model.properties.SQLPatternItem,
     * boolean)
     */
    public void syncSQLPattern(SQLPatternItem routineItem, boolean copyToTemp) throws SystemException {
        FileOutputStream fos = null;
        try {
            IFile file = getSQLPatternFile(routineItem);

            if (copyToTemp) {
                String routineContent = new String(routineItem.getContent().getInnerContent());
                File f = file.getLocation().toFile();
                fos = new FileOutputStream(f);
                fos.write(routineContent.getBytes());
                fos.close();
            }
            file.refreshLocal(1, null);
        } catch (CoreException e) {
            throw new SystemException(e);
        } catch (IOException e) {
            throw new SystemException(e);
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                // ignore me even if i'm null
            }
        }
    }

}
