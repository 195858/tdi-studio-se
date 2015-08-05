// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.runprocess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.graphics.Image;
import org.talend.commons.ui.runtime.image.ECoreImage;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.commons.ui.runtime.image.ImageUtils.ICON_SIZE;
import org.talend.core.model.components.IComponent;
import org.talend.core.model.process.Problem;
import org.talend.core.model.process.TalendProblem;
import org.talend.core.ui.images.CoreImageProvider;
import org.talend.repository.model.ComponentsFactoryProvider;

/**
 * DOC chuang class global comment. Detailled comment
 */
public class ErrorDetailTreeBuilder {

    private static final String GENERAL_ERROR = "General"; //$NON-NLS-1$

    Map<String, JobErrorEntry> jobs = new HashMap<String, JobErrorEntry>();

    /**
     * DOC chuang Comment method "createTreeInput".
     * 
     * @param errors
     * @param jobNames
     * @return
     */
    public List<JobErrorEntry> createTreeInput(List<Problem> errors, Set<String> jobIds) {
        for (Problem error : errors) {
            if (error instanceof TalendProblem) {
                TalendProblem talendProblem = (TalendProblem) error;
                if (talendProblem != null && talendProblem.getJobInfo() != null) {
                    String jobId = talendProblem.getJobInfo().getJobId();
                    if (!jobIds.contains(jobId)) {
                        continue;
                    }
                    String componentName = GENERAL_ERROR;
                    // System.out.println("tp----" + talendProblem.getElement().getClass());
                    JobErrorEntry jobEntry = getJobEntry(talendProblem.getJavaUnitName());
                    jobEntry.addItem(componentName, talendProblem);
                }

            } else {
                if (error != null && error.getJobInfo() != null) {
                    String jobId = error.getJobInfo().getJobId();
                    if (!jobIds.contains(jobId)) {
                        continue;
                    }
                    String componentName = error.getNodeName();
                    JobErrorEntry jobEntry = getJobEntry(error.getJobInfo().getJobName());
                    jobEntry.addItem(componentName, error);
                }
            }
        }

        return new ArrayList<JobErrorEntry>(jobs.values());
    }

    private JobErrorEntry getJobEntry(String name) {
        JobErrorEntry entry = jobs.get(name);
        if (entry == null) {
            entry = new JobErrorEntry();
            jobs.put(name, entry);
            entry.setLabel(name);
        }
        return entry;
    }

    /**
     * 
     * DOC chuang ErrorDetailTreeBuilder class global comment. Detailled comment
     */
    interface IContainerEntry {

        public String getLabel();

        public boolean hasChildren();

        public List getChildren();

        public Image getImage();
    }

    /**
     * 
     * DOC chuang ErrorDetailTreeBuilder class global comment. Detailled comment
     */
    class JobErrorEntry implements IContainerEntry {

        private String label;

        private Map<String, ComponentErrorEntry> componentEntryMap = new HashMap<String, ComponentErrorEntry>();

        public void setLabel(String label) {
            this.label = label;
        }

        public void addItem(String name, Problem problem) {
            ComponentErrorEntry entry = componentEntryMap.get(name);
            if (entry == null) {
                entry = new ComponentErrorEntry();
                componentEntryMap.put(name, entry);
                entry.setLabel(name);
            }
            entry.addItem(problem);
        }

        public List getChildren() {
            return new ArrayList<ComponentErrorEntry>(componentEntryMap.values());
        }

        public Image getImage() {
            return ImageProvider.getImage(ECoreImage.PROCESS_ICON);
        }

        public String getLabel() {
            return label;
        }

        public boolean hasChildren() {
            return componentEntryMap.values().size() > 0;
        }

    }

    /**
     * 
     * DOC chuang ErrorDetailTreeBuilder class global comment. Detailled comment
     */
    class ComponentErrorEntry implements IContainerEntry {

        private String label;

        private List<Problem> errors = new ArrayList<Problem>();

        private Image icon;

        public void setLabel(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public void addItem(Problem problem) {
            errors.add(problem);
            if (icon == null && problem.getNodeName() != null) {
                IComponent component = ComponentsFactoryProvider.getInstance().get(problem.getComponentName());
                icon = CoreImageProvider.getComponentIcon(component, ICON_SIZE.ICON_16);
            }
        }

        public List getChildren() {
            return errors;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.talend.designer.runprocess.ErrorDetailTreeBuilder.IContainerEntry#getImage()
         */
        public Image getImage() {
            if (label.equals(GENERAL_ERROR)) {
                return ImageProvider.getImage(ECoreImage.UNKNOWN);
            } else if (icon != null) {
                return icon;
            }
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.talend.designer.runprocess.ErrorDetailTreeBuilder.IContainerEntry#hasChildren()
         */
        public boolean hasChildren() {
            return errors.size() > 0;
        }

    }

}
