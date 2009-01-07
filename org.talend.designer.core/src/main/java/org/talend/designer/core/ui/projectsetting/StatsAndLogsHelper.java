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
package org.talend.designer.core.ui.projectsetting;

import java.util.List;
import java.util.Map;

import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.preference.IPreferenceStore;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.language.LanguageManager;
import org.talend.core.model.general.Project;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.Element;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.StatAndLogsSettings;
import org.talend.designer.core.DesignerPlugin;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.designer.core.model.components.EmfComponent;
import org.talend.designer.core.model.utils.emf.talendfile.ParametersType;
import org.talend.designer.core.model.utils.emf.talendfile.TalendFileFactory;
import org.talend.designer.core.ui.AbstractMultiPageTalendEditor;
import org.talend.designer.core.ui.editor.cmd.ChangeValuesFromRepository;
import org.talend.designer.core.ui.preferences.StatsAndLogsConstants;
import org.talend.designer.core.ui.views.jobsettings.ImplicitContextLoadHelper;
import org.talend.designer.core.ui.views.statsandlogs.StatsAndLogsComposite;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.ui.views.RepositoryContentProvider;
import org.talend.repository.ui.views.RepositoryView;

/**
 * Helper class for Load StatsAndLogs Preferences to EMF project in Project setting
 */
public class StatsAndLogsHelper extends Utils {

    /**
     * 
     * Load StatsAndLogs Preference setting to Project Only load Once
     * 
     * @param pro
     */
    static void loadPreferenceToProject(Project pro) {
        TalendFileFactory talendF = TalendFileFactory.eINSTANCE;

        StatAndLogsSettings stats = PropertiesFactory.eINSTANCE.createStatAndLogsSettings();
        pro.getEmfProject().setStatAndLogsSettings(stats);
        stats.setParameters(talendF.createParametersType());

        ParametersType pType = stats.getParameters();
        StatsAndLogsElement elem = new StatsAndLogsElement();
        pro.setStatsAndLog(elem);
        StatsAndLogsHelper.createStatsAndLogsParameters(elem);
        ElementParameter2ParameterType.saveElementParameters(elem, pType);
    }

    static void createStatsAndLogsParameters(Element elem) {
        statsAndLogsParametersTitlePart(elem);
        statsAndLogsParametersFilePart(elem);
        statsAndLogsParametersDBPart(elem);
        statsAndLogsParametersFinalPart(elem);

    }

    private static void statsAndLogsParametersTitlePart(Element elem) {
        ElementParameter param;
        IPreferenceStore preferenceStore = DesignerPlugin.getDefault().getPreferenceStore();

        List<IElementParameter> paramList = (List<IElementParameter>) elem.getElementParameters();

        String languagePrefix = LanguageManager.getCurrentLanguage().toString() + "_";

        param = new ElementParameter(elem);
        param.setName(EParameterName.UPDATE_COMPONENTS.getName());
        param.setValue(Boolean.FALSE);
        param.setDisplayName(EParameterName.UPDATE_COMPONENTS.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(1);
        param.setReadOnly(true);
        param.setRequired(false);
        param.setShow(false);
        paramList.add(param);

        param = new ElementParameter(elem);
        param.setName(EParameterName.ON_STATCATCHER_FLAG.getName());
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.ON_STATCATCHER_FLAG.getName()));
        param.setDisplayName(EParameterName.ON_STATCATCHER_FLAG.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(1);
        paramList.add(param);

        param = new ElementParameter(elem);
        param.setName(EParameterName.ON_LOGCATCHER_FLAG.getName());
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.ON_LOGCATCHER_FLAG.getName()));
        param.setDisplayName(EParameterName.ON_LOGCATCHER_FLAG.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(1);
        paramList.add(param);

        param = new ElementParameter(elem);
        param.setName(EParameterName.ON_METERCATCHER_FLAG.getName());
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.ON_METERCATCHER_FLAG.getName()));
        param.setDisplayName(EParameterName.ON_METERCATCHER_FLAG.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(1);
        paramList.add(param);

        // on console
        param = new ElementParameter(elem);
        param.setName(EParameterName.ON_CONSOLE_FLAG.getName());
        param.setValue(Boolean.FALSE);
        param.setDisplayName(EParameterName.ON_CONSOLE_FLAG.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(2);
        param.setShowIf("(ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')");
        paramList.add(param);
    }

    private static void statsAndLogsParametersFilePart(Element elem) {
        ElementParameter param;
        IPreferenceStore preferenceStore = DesignerPlugin.getDefault().getPreferenceStore();

        List<IElementParameter> paramList = (List<IElementParameter>) elem.getElementParameters();

        String languagePrefix = LanguageManager.getCurrentLanguage().toString() + "_";
        // on files
        param = new ElementParameter(elem);
        param.setName(EParameterName.ON_FILES_FLAG.getName());
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.ON_FILES_FLAG.getName()));
        param.setDisplayName(EParameterName.ON_FILES_FLAG.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(10);
        param.setShowIf("(ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')");
        paramList.add(param);

        // file path
        param = new ElementParameter(elem);
        param.setName(EParameterName.FILE_PATH.getName());
        param.setValue(addQuotes(replaceSlash(preferenceStore.getString(languagePrefix + EParameterName.FILE_PATH.getName()))));
        param.setDisplayName(EParameterName.FILE_PATH.getDisplayName());
        param.setField(EParameterFieldType.DIRECTORY);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param
                .setShowIf("(ON_FILES_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')");
        param.setNumRow(11);
        paramList.add(param);

        // stats file name
        param = new ElementParameter(elem);
        param.setName(EParameterName.FILENAME_STATS.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.FILENAME_STATS.getName())));
        param.setDisplayName(EParameterName.FILENAME_STATS.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setShowIf("(ON_FILES_FLAG == 'true' and ON_STATCATCHER_FLAG == 'true')");
        param.setRequired(true);
        param.setNumRow(12);

        paramList.add(param);

        param = new ElementParameter(elem);
        param.setName(EParameterName.FILENAME_LOGS.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.FILENAME_LOGS.getName())));
        param.setDisplayName(EParameterName.FILENAME_LOGS.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setShowIf("(ON_FILES_FLAG == 'true' and ON_LOGCATCHER_FLAG == 'true')");
        param.setNumRow(13);
        param.setRequired(true);
        paramList.add(param);

        param = new ElementParameter(elem);
        param.setName(EParameterName.FILENAME_METTER.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.FILENAME_METTER.getName())));
        param.setDisplayName(EParameterName.FILENAME_METTER.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setShowIf("(ON_FILES_FLAG == 'true' and ON_METERCATCHER_FLAG == 'true')");
        param.setRequired(true);
        param.setNumRow(14);
        paramList.add(param);
    }

    static void changeRepositoryConnection(Element process, StatsAndLogsComposite statsComposite) {
        String propertyType = (String) ElementParameter2ParameterType.getParameterValue(process, EParameterName.PROPERTY_TYPE
                .getName());

        String id = (String) (ElementParameter2ParameterType.getParameterValue(process, EParameterName.PROPERTY_TYPE.getName()));

        String connectionLabel = (String) (ElementParameter2ParameterType.getParameterValue(process,
                EParameterName.REPOSITORY_PROPERTY_TYPE.getName()));

        RepositoryContentProvider contentProvider = (RepositoryContentProvider) RepositoryView.show().getViewer()
                .getContentProvider();
        RepositoryNode repositoryNode = (contentProvider).getMetadataConNode();
        IElementParameter elementParameter = process.getElementParameter(ImplicitContextLoadHelper
                .getExtraParameterName(EParameterName.PROPERTY_TYPE));
        IElementParameter parameterRepositoryType = elementParameter.getChildParameters().get(
                EParameterName.REPOSITORY_PROPERTY_TYPE.getName());
        if (parameterRepositoryType != null) {
            parameterRepositoryType.setLinkedRepositoryItem(ImplicitContextLoadHelper.findConnectionItemByLabel(contentProvider,
                    repositoryNode, connectionLabel));
        }

        Connection repositoryConnection = null;
        Map<String, ConnectionItem> repositoryConnectionItemMap = statsComposite.getRepositoryConnectionItemMap();

        if (repositoryConnectionItemMap.containsKey(id)) {
            repositoryConnection = repositoryConnectionItemMap.get(id).getConnection();
        } else {
            repositoryConnection = null;
        }
        ChangeValuesFromRepository cmd1 = new ChangeValuesFromRepository(process, repositoryConnection, ImplicitContextLoadHelper
                .getExtraParameterName(EParameterName.PROPERTY_TYPE)
                + ":" + EParameterName.PROPERTY_TYPE.getName(), propertyType);

        ChangeValuesFromRepository cmd2 = new ChangeValuesFromRepository(process, repositoryConnection, ImplicitContextLoadHelper
                .getExtraParameterName(EParameterName.PROPERTY_TYPE)
                + ":" + EParameterName.REPOSITORY_PROPERTY_TYPE.getName(), id);
        cmd2.setMaps(statsComposite.getRepositoryTableMap());

        AbstractMultiPageTalendEditor part = ((org.talend.designer.core.ui.editor.process.Process) process).getEditor();
        if (part instanceof AbstractMultiPageTalendEditor) {
            Object adapter = (part).getTalendEditor().getAdapter(CommandStack.class);
            if (adapter != null) {
                CommandStack commandStack = ((CommandStack) adapter);
                commandStack.execute(cmd1);
                commandStack.execute(cmd2);
            }
        }
    }

    private static void statsAndLogsParametersDBPart(Element elem) {
        ElementParameter param;
        IPreferenceStore preferenceStore = DesignerPlugin.getDefault().getPreferenceStore();

        List<IElementParameter> paramList = (List<IElementParameter>) elem.getElementParameters();

        // checks current language, if it is perl, set languageType to 0(default value), otherwise to 1.
        int languageType = 0;
        if (LanguageManager.getCurrentLanguage().equals(ECodeLanguage.JAVA)) {
            languageType = 1;
        }

        String languagePrefix = LanguageManager.getCurrentLanguage().toString() + "_";

        // on database
        param = new ElementParameter(elem);
        param.setName(EParameterName.ON_DATABASE_FLAG.getName());
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.ON_DATABASE_FLAG.getName()));
        param.setDisplayName(EParameterName.ON_DATABASE_FLAG.getDisplayName()); // On Database
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(50);
        param.setShowIf("(ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')");
        paramList.add(param);

        ElementParameter parentPropertyType = new ElementParameter(elem);
        parentPropertyType.setName(EParameterName.PROPERTY_TYPE.getName());
        parentPropertyType.setDisplayName(EParameterName.PROPERTY_TYPE.getDisplayName());
        parentPropertyType.setValue("");
        parentPropertyType.setCategory(EComponentCategory.STATSANDLOGS);
        parentPropertyType.setField(EParameterFieldType.PROPERTY_TYPE);
        parentPropertyType.setRepositoryValue("DATABASE"); //$NON-NLS-1$
        parentPropertyType.setNumRow(51);
        parentPropertyType
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')");
        paramList.add(parentPropertyType);

        param = new ElementParameter(elem);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setName(EParameterName.PROPERTY_TYPE.getName());
        param.setDisplayName(EParameterName.PROPERTY_TYPE.getDisplayName());
        param.setListItemsDisplayName(new String[] { EmfComponent.TEXT_BUILTIN, EmfComponent.TEXT_REPOSITORY });
        param.setListItemsDisplayCodeName(new String[] { EmfComponent.BUILTIN, EmfComponent.REPOSITORY });
        param.setListItemsValue(new String[] { EmfComponent.BUILTIN, EmfComponent.REPOSITORY });
        param.setValue(preferenceStore.getString(languagePrefix + EParameterName.PROPERTY_TYPE.getName()));
        param.setNumRow(51);
        param.setField(EParameterFieldType.TECHNICAL);
        param.setRepositoryValue("DATABASE"); //$NON-NLS-1$
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')");

        param.setParentParameter(parentPropertyType);
        // paramList.add(param);

        param = new ElementParameter(elem);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setName(EParameterName.REPOSITORY_PROPERTY_TYPE.getName());
        param.setDisplayName(EParameterName.REPOSITORY_PROPERTY_TYPE.getDisplayName());
        param.setListItemsDisplayName(new String[] {});
        param.setListItemsValue(new String[] {});
        param.setNumRow(51);
        param.setField(EParameterFieldType.TECHNICAL);
        param.setValue(preferenceStore.getString(languagePrefix + EParameterName.REPOSITORY_PROPERTY_TYPE.getName()// +
        // ProjectSettingManager
                // .
                // CONNECTION_ITEM_LABEL
                )); //$NON-NLS-1$
        param.setShow(false);
        param.setRequired(true);
        // paramList.add(param);
        param.setParentParameter(parentPropertyType);

        // dbType
        param = new ElementParameter(elem);
        param.setName(EParameterName.DB_TYPE.getName());
        String type = preferenceStore.getString(languagePrefix + EParameterName.DB_TYPE.getName());
        if (type == null || "".equals(type.trim())) { //$NON-NLS-1$
            type = StatsAndLogsConstants.DB_COMPONENTS[languageType][0];
        }
        param.setValue(type);
        param.setDisplayName(EParameterName.DB_TYPE.getDisplayName());
        param.setField(EParameterFieldType.CLOSED_LIST);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setListItemsDisplayName(StatsAndLogsConstants.DISPLAY_DBNAMES[languageType]);
        param.setListItemsValue(StatsAndLogsConstants.DB_COMPONENTS[languageType]);
        param.setListRepositoryItems(StatsAndLogsConstants.REPOSITORY_ITEMS[languageType]);
        param.setListItemsDisplayCodeName(StatsAndLogsConstants.CODE_LIST[languageType]);
        param.setNumRow(52);
        param.setRepositoryValue("TYPE"); //$NON-NLS-1$
        param.setRequired(true);
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')");
        paramList.add(param);

        // dbVersion
        param = new ElementParameter(elem);
        param.setName(EParameterName.DB_VERSION.getName());
        param.setValue(StatsAndLogsConstants.ORACLE_VERSION_DRIVER[1]);
        param.setDisplayName(EParameterName.DB_VERSION.getDisplayName());
        param.setField(EParameterFieldType.CLOSED_LIST);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setListItemsDisplayName(StatsAndLogsConstants.ORACLE_VERSION_DISPLAY);
        param.setListItemsValue(StatsAndLogsConstants.ORACLE_VERSION_DRIVER);
        // param.setListRepositoryItems(StatsAndLogsConstants.REPOSITORY_ITEMS[languageType]);
        param.setListItemsDisplayCodeName(StatsAndLogsConstants.ORACLE_VERSION_CODE);
        param.setNumRow(52);
        param.setRepositoryValue("DB_VERSION"); //$NON-NLS-1$
        param.setRequired(true);
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (DB_TYPE == 'OCLE') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')");
        paramList.add(param);

        // host
        param = new ElementParameter(elem);
        param.setName(EParameterName.HOST.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.HOST.getName())));
        param.setDisplayName(EParameterName.HOST.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(53);
        param.setRepositoryValue("SERVER_NAME"); //$NON-NLS-1$
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true') and (DB_TYPE!='SQLITE' and DB_TYPE!='ACCESS')");
        paramList.add(param);

        // port
        param = new ElementParameter(elem);
        param.setName(EParameterName.PORT.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.PORT.getName())));
        param.setDisplayName(EParameterName.PORT.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(53);
        param.setRepositoryValue("PORT"); //$NON-NLS-1$
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true') and (DB_TYPE!='SQLITE' and DB_TYPE!='ACCESS' and DB_TYPE!='FIREBIRD')");
        paramList.add(param);

        // dbName
        param = new ElementParameter(elem);
        param.setName(EParameterName.DBNAME.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.DBNAME.getName())));
        param.setDisplayName(EParameterName.DBNAME.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(54);
        param.setRepositoryValue("SID"); //$NON-NLS-1$
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true') and (DB_TYPE!='SQLITE' and DB_TYPE!='ACCESS' and DB_TYPE!='FIREBIRD')");
        paramList.add(param);

        // additional parameters
        param = new ElementParameter(elem);
        param.setName(EParameterName.PROPERTIES.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.PROPERTIES.getName())));
        param.setDisplayName(EParameterName.PROPERTIES.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(54);
        param.setRepositoryValue("PROPERTIES_STRING"); //$NON-NLS-1$
        param
                .setShowIf("(DB_TYPE=='SQL_SERVER' or DB_TYPE=='MYSQL' or DB_TYPE=='INFORMIX') and (ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')");
        paramList.add(param);
        // schema
        param = new ElementParameter(elem);
        param.setName(EParameterName.SCHEMA_DB.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.SCHEMA_DB.getName())));
        param.setDisplayName(EParameterName.SCHEMA_DB.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(54);
        param.setRepositoryValue("SCHEMA"); //$NON-NLS-1$
        param
                .setShowIf("(DB_TYPE=='OCLE' or DB_TYPE=='POSTGRESQL') and (ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')");
        paramList.add(param);

        // username
        param = new ElementParameter(elem);
        param.setName(EParameterName.USER.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.USER.getName())));
        param.setDisplayName(EParameterName.USER.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(55);
        param.setRequired(true);
        param.setRepositoryValue("USERNAME"); //$NON-NLS-1$
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')and (DB_TYPE!='SQLITE')");
        paramList.add(param);

        // password
        param = new ElementParameter(elem);
        param.setName(EParameterName.PASS.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.PASS.getName())));
        param.setDisplayName(EParameterName.PASS.getDisplayName());
        param.setField(EParameterFieldType.TEXT);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(55);
        param.setRequired(true);
        param.setRepositoryValue("PASSWORD"); //$NON-NLS-1$
        param
                .setShowIf("(ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true') and (DB_TYPE!='SQLITE')");
        paramList.add(param);
        // databse file path
        param = new ElementParameter(elem);
        param.setName(EParameterName.DBFILE.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.DBFILE.getName())));
        param.setDisplayName(EParameterName.DBFILE.getDisplayName());
        param.setField(EParameterFieldType.FILE);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(56);
        param.setRepositoryValue("FILE"); //$NON-NLS-1$
        param
                .setShowIf("(DB_TYPE=='SQLITE' or DB_TYPE=='ACCESS' or DB_TYPE=='FIREBIRD') and (ON_DATABASE_FLAG == 'true') and (ON_STATCATCHER_FLAG == 'true' or ON_LOGCATCHER_FLAG == 'true' or ON_METERCATCHER_FLAG == 'true')");
        paramList.add(param);
        // Stats table
        param = new ElementParameter(elem);
        param.setName(EParameterName.TABLE_STATS.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.TABLE_STATS.getName())));
        param.setDisplayName(EParameterName.TABLE_STATS.getDisplayName());
        param.setField(EParameterFieldType.DBTABLE);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(57);
        param.setShowIf("(ON_DATABASE_FLAG == 'true' and ON_STATCATCHER_FLAG == 'true')");
        paramList.add(param);

        // Log table
        param = new ElementParameter(elem);
        param.setName(EParameterName.TABLE_LOGS.getName());
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.TABLE_LOGS.getName())));
        param.setDisplayName(EParameterName.TABLE_LOGS.getDisplayName());
        param.setField(EParameterFieldType.DBTABLE);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(58);
        param.setShowIf("(ON_DATABASE_FLAG == 'true' and ON_LOGCATCHER_FLAG == 'true')");
        paramList.add(param);

        // Metter table
        param = new ElementParameter(elem);
        param.setName(EParameterName.TABLE_METER.getName()); //$NON-NLS-1$
        param.setValue(addQuotes(preferenceStore.getString(languagePrefix + EParameterName.TABLE_METER.getName())));
        param.setDisplayName(EParameterName.TABLE_METER.getDisplayName());
        param.setField(EParameterFieldType.DBTABLE);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(59);
        param.setShowIf("(ON_DATABASE_FLAG == 'true' and ON_METERCATCHER_FLAG == 'true')");
        paramList.add(param);
    }

    private static void statsAndLogsParametersFinalPart(Element elem) {
        ElementParameter param;
        IPreferenceStore preferenceStore = DesignerPlugin.getDefault().getPreferenceStore();

        List<IElementParameter> paramList = (List<IElementParameter>) elem.getElementParameters();

        String languagePrefix = LanguageManager.getCurrentLanguage().toString() + "_";

        // Catch runtime errors
        param = new ElementParameter(elem);
        param.setName("CATCH_RUNTIME_ERRORS"); //$NON-NLS-1$
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.CATCH_RUNTIME_ERRORS.getName()));
        param.setDisplayName(EParameterName.CATCH_RUNTIME_ERRORS.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(90);
        param.setShowIf("ON_LOGCATCHER_FLAG == 'true'");
        paramList.add(param);

        // Catch user errors
        param = new ElementParameter(elem);
        param.setName("CATCH_USER_ERRORS"); //$NON-NLS-1$
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.CATCH_USER_ERRORS.getName()));
        param.setDisplayName(EParameterName.CATCH_USER_ERRORS.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(90);
        param.setShowIf("ON_LOGCATCHER_FLAG == 'true'");
        paramList.add(param);

        // Catch user warning
        param = new ElementParameter(elem);
        param.setName("CATCH_USER_WARNING"); //$NON-NLS-1$
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.CATCH_USER_WARNING.getName()));
        param.setDisplayName(EParameterName.CATCH_USER_WARNING.getDisplayName());
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(90);
        param.setShowIf("ON_LOGCATCHER_FLAG == 'true'");
        paramList.add(param);

        // Catch realtime statistics
        param = new ElementParameter(elem);
        param.setName("CATCH_REALTIME_STATS"); //$NON-NLS-1$
        param.setValue(preferenceStore.getBoolean(languagePrefix + EParameterName.CATCH_REALTIME_STATS.getName()));
        param.setDisplayName(EParameterName.CATCH_REALTIME_STATS.getDisplayName() + " ("
                + EParameterName.TSTATCATCHER_STATS.getDisplayName() + ")");
        param.setField(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.STATSANDLOGS);
        param.setNumRow(91);
        param.setShowIf("ON_STATCATCHER_FLAG == 'true'");
        paramList.add(param);
    }

}
