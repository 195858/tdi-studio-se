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
package org.talend.repository.model.migration;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.common.util.EList;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.migration.AbstractJobMigrationTask;
import org.talend.core.model.properties.Item;
import org.talend.designer.core.model.utils.emf.talendfile.ConnectionType;
import org.talend.designer.core.model.utils.emf.talendfile.ElementParameterType;
import org.talend.designer.core.model.utils.emf.talendfile.ElementValueType;
import org.talend.designer.core.model.utils.emf.talendfile.NodeType;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;
import org.talend.repository.model.ProxyRepositoryFactory;

/**
 * wliu class global comment. Detailled comment <br/>
 * 
 * $Id: talend.epf 1 2009-06-05 17:06:40Z wliu $
 * 
 */
public class ChangeReturnTypeOfServerAliveResult extends AbstractJobMigrationTask {

    private static final String MATCH_REGEX = "\\(\\(\\bString\\b\\)\\bglobalMap\\b\\.get\\("
            + "\"tServerAlive_[1-9]\\d*_SERVER_ALIVE_RESULT\"\\)\\)\\.equals\\(\"(OK|KO)\"\\)"; //$NON-NLS-1$

    public ExecutionResult execute(Item item) {
        ProcessType processType = getProcessType(item);
        if (processType == null) {
            return ExecutionResult.NOTHING_TO_DO;
        }
        try {
            convertItem(item, processType);
            return ExecutionResult.SUCCESS_WITH_ALERT;
        } catch (Exception e) {
            ExceptionHandler.process(e);
            return ExecutionResult.FAILURE;
        }
    }

    private String getReplaceValue(Matcher match) {
        match.reset();
        StringBuffer sb = new StringBuffer();
        // boolean bl = match.find();
        // System.out.println("group:" + match.group());
        while (match.find()) {
            String group = match.group();
            String tmp = "";
            if (group.contains("equals(\"OK\")")) {
                tmp = group.replace("String", "Boolean").replace(".equals(\"OK\")", "");

            } else if (group.contains("equals(\"KO\")")) {
                tmp = group.replace("((String)", "!((Boolean)").replace(".equals(\"KO\")", "");
            }
            match.appendReplacement(sb, tmp);
        }
        match.appendTail(sb);
        return sb.toString();

    }

    private boolean convertItem(Item item, ProcessType processType) throws PersistenceException {
        ProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        boolean modified = false;
        ECodeLanguage language = getProject().getLanguage();
        EList node = processType.getNode();
        EList connections = processType.getConnection();
        Pattern pattern = Pattern.compile(MATCH_REGEX);
        for (Object n : node) {
            NodeType type = (NodeType) n;
            EList elementParameterList = type.getElementParameter();
            for (Object elem : elementParameterList) {
                ElementParameterType elemType = (ElementParameterType) elem;
                if (language.equals(ECodeLanguage.JAVA)) {
                    if (elemType.getValue() != null && elemType.getValue().contains("tServerAlive")) {
                        Matcher match = pattern.matcher(elemType.getValue());
                        if (match.matches()) {
                            String replace = getReplaceValue(match);
                            elemType.setValue(replace);
                            modified = true;
                        }
                    }
                }

                // for table

                EList elemValue = elemType.getElementValue();
                for (Object elemV : elemValue) {
                    ElementValueType elemVal = (ElementValueType) elemV;
                    if (language.equals(ECodeLanguage.JAVA)) {
                        if (elemVal.getValue() != null && elemVal.getValue().contains("tServerAlive")) {
                            Matcher match = pattern.matcher(elemVal.getValue());
                            if (match.matches()) {
                                String replace = getReplaceValue(match);
                                elemVal.setValue(replace);
                                modified = true;
                            }
                        }
                    }
                }

            }

        }
        // the links
        for (Object n : connections) {
            ConnectionType type = (ConnectionType) n;
            EList elementParameterList = type.getElementParameter();
            for (Object elem : elementParameterList) {
                ElementParameterType elemType = (ElementParameterType) elem;
                if (language.equals(ECodeLanguage.JAVA)) {
                    if (elemType.getValue() != null && elemType.getValue().contains("tServerAlive")) {
                        Matcher match = pattern.matcher(elemType.getValue());
                        if (match.matches()) {
                            String replace = getReplaceValue(match);
                            elemType.setValue(replace);
                            modified = true;
                        }
                    }
                }
            }
        }

        if (modified) {
            factory.save(item, true);
        }

        return modified;

    }

    public Date getOrder() {
        GregorianCalendar gc = new GregorianCalendar(2009, 6, 1, 12, 0, 0);
        return gc.getTime();
    }

}
