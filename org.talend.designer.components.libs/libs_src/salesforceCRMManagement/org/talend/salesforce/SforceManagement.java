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
package org.talend.salesforce;

import java.util.Calendar;
import java.util.List;

import org.apache.axis.message.MessageElement;

import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.SoapBindingStub;
import com.sforce.soap.partner.UpsertResult;
import com.sforce.soap.partner.sobject.SObject;

/**
 * DOC Administrator class global comment. Detailled comment <br/>
 * 
 * $Id: talend.epf 1 2006-09-29 17:06:40 +0000 (ææäº, 29 ä¹æ 2006) nrousseau $
 * 
 */
public interface SforceManagement {

    public boolean login(String username, String password, String endPoint) throws Exception;

    public boolean login(String username, String password, String endPoint, int commitLevel, boolean exceptionForErrors,
            String errorLogFile) throws Exception;

    public void logout() throws Exception;

    public String[] getTables() throws Exception;

    public Field[] getFields(String tablename) throws Exception;

    public MessageElement newMessageElement(String name, Object value) throws Exception;

    public SaveResult[] insert(String tablename, MessageElement[] nameValues) throws Exception;

    public DeleteResult[] delete(String id) throws Exception;

    public SaveResult[] update(String tablename, String id, MessageElement[] updatefields) throws Exception;

    public UpsertResult[] upsert(String tablename, String upsertkey, MessageElement[] updatefields) throws Exception;

    public List<SObject> query(String queryString) throws Exception;

    public abstract Calendar getServerTimestamp() throws Exception;

    public SoapBindingStub getBinding();
    
    public void setNeedCompression(Boolean needCompression);
}
