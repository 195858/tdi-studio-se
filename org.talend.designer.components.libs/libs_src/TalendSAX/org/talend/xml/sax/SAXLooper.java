package org.talend.xml.sax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.talend.xml.sax.function.inter.Function;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
/**
 * This class is the interface for tFileInputXML component. DOC s class global comment. Detailled comment
 * 
 * $Id: SAXLooper.java,v 1.1 2008/03/21 07:20:39 xzhang Exp $
 */
public class SAXLooper {

    // loop path in the tFileInputXML component.
    private String loopPath;

    // node paths special which tab will be read as the row value
    private String[] nodePaths;

    private LoopEntry entry;

    private SAXLoopCompositeHandler result;

    /**
     * DOC xzhang SAXLooper constructor comment.
     * 
     * @param loopPath :loop path in the tFileInputXML component.
     * @param nodePaths :node paths special which tab will be read as the row value
     */
    public SAXLooper(String loopPath, String[] nodePaths) {
        this.loopPath = loopPath;
        this.nodePaths = nodePaths;
        initLoopEntry();
    }

    /**
     * 
     * DOC wliu SAXLooper constructor comment.
     * 
     * @param rootPath:root loop path in the tFileInputMSXML component
     * @param mapping:LOOP_PATH=sub loop path, MAPPING=query columns
     */
    private String rootPath = null;

    private LoopEntry[] entries = null;

    private String[] arrLoopPath = null;

    private String[][] arrNodePaths = null;

    private String[] arrOrigLoopPath = null;

    public SAXLooper(String rootPath, String[] arrLoopPath, String[][] arrNodePaths) {

        this.arrOrigLoopPath = arrLoopPath;

        // this.arrLoopPath = new String[arrLoopPath.length];

        this.arrOrigLoopPath = arrLoopPath;

        this.arrNodePaths = arrNodePaths;

        String tmpRootPath = rootPath;
        if (tmpRootPath.endsWith("/")) {
            tmpRootPath = tmpRootPath.substring(0, tmpRootPath.length() - 1);
        }

        this.rootPath = tmpRootPath;

        this.arrLoopPath = getLoopPaths(arrLoopPath);
        entries = new LoopEntry[arrLoopPath.length];

        initLoopEntries();
    }

    private String[] getLoopPaths(String[] arrLoops) {

        String[] loopPaths = new String[arrLoops.length];

        for (int i = 0; i < arrLoops.length; i++) {
            String column = arrLoops[i];
            String resultCol = this.rootPath;
            String[] splits = column.split("/");
            for (String tmp : splits) {
                if (tmp.equals("..")) {
                    resultCol = resultCol.substring(0, resultCol.lastIndexOf("/"));
                } else if (tmp.equals(".")) {
                } else if (!("").equals(tmp)) {
                    resultCol += "/" + tmp;
                }
            }

            loopPaths[i] = resultCol;
        }

        return loopPaths;
    }

    /**
     * Parse the XML file. Buffer the result in LoopEntry.
     * 
     * @param fileURL file URL
     */
    public void parse(String fileURL) {
        try {
            DefaultHandler hd = null;
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            if (rootPath == null || rootPath.equals("")) {
                hd = newHandler();
            } else {
                hd = newHandler2();
            }
            saxParser.parse(fileURL, hd);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parse the XML file. Buffer the result in LoopEntry.
     * 
     * @param is InputStream
     */
    public void parse(java.io.InputStream is) {
        try {
            DefaultHandler hd = null;
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            if (rootPath == null || rootPath.equals("")) {
                hd = newHandler();
            } else {
                hd = newHandler2();
            }
            saxParser.parse(is, hd);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Get result iterator. This must be call after the parse method.
     * 
     * @return Iterator
     */
    public Iterator<Map<String, String>> iterator() {
        return new SAXLoopIterator(this.entry);
    }

    public Iterator<Map<String, Map<String, String>>> multiIterator() {
        return new SAXMultiLoopIterator(this, this.entries);
    }

    /**
     * create a handler
     * 
     * @return
     */
    private DefaultHandler newHandler() {
        result = new SAXLoopCompositeHandler();
        LoopEntry tmpEntry = this.entry;
        while (tmpEntry != null) {
            result.register(new SAXLoopHandler(tmpEntry));
            tmpEntry = tmpEntry.getSubLoop();
        }
        return result;
    }

    /**
     * create a handler
     * 
     * @return
     */
    private DefaultHandler newHandler2() {

        // if (rootPath == null || rootPath.equals("")) {
        result = new SAXLoopCompositeHandler();
        // } else {
        // result = new SAXLoopCompositeHandler(this);
        // }
        for (int i = 0; i < this.entries.length; i++) {
            LoopEntry tmpEntry = this.entries[i];
            while (tmpEntry != null) {
                result.register(new SAXLoopHandler(this, tmpEntry));
                tmpEntry = tmpEntry.getSubLoop();
            }

        }
        return result;
    }

    /**
     * init the LoopEntry according by "nodePaths" and "loopPath".
     * 
     * @return
     */
    private void initLoopEntry() {
        Map<String, LoopEntry> entryMap = new HashMap<String, LoopEntry>();

        FunctionRegister funcRegister = new FunctionRegister();// list of all the functions
        Function function = null;
        // function is in looppath
        if (this.loopPath.indexOf("/*[") >= 0 && this.loopPath.indexOf(")]") >= 0) {
            if (nodePaths.length > 0) {
                String strTmp = loopPath.substring(loopPath.lastIndexOf("/"));
                String strFuncName = strTmp.substring(strTmp.indexOf("*[") + 2, strTmp.indexOf("("));

                if (funcRegister.isFuncRegistered(strFuncName)) {
                    function = funcRegister.getFunction(strFuncName);
                }
            }
            loopPath = loopPath.substring(0, loopPath.lastIndexOf("/"));
        }
        // parse the node path to loopEntry
        for (String column : nodePaths) {
            String resultCol = this.loopPath;
            String tmpLoopPath = null;
            String[] splits = column.split("/");
            for (String tmp : splits) {
                if (tmp.equals("..")) {
                    resultCol = resultCol.substring(0, resultCol.lastIndexOf("/"));
                    tmpLoopPath = resultCol;
                } else if (tmp.equals(".")) {
                    tmpLoopPath = resultCol;
                } else {

                    if (tmp.indexOf("*[") >= 0 && tmp.indexOf(")]") >= 0) {// has funcion in column
                        resultCol = resultCol.substring(0, resultCol.lastIndexOf("/"));
                        tmpLoopPath = resultCol;// find the function node

                        // get the function name
                        String strFuncName = tmp.substring(tmp.indexOf("*[") + 2, tmp.indexOf("("));

                        if (funcRegister.isFuncRegistered(strFuncName)) {
                            function = funcRegister.getFunction(strFuncName);
                        }

                    } else {
                        resultCol += "/" + tmp;
                    }

                }
            }
            if (tmpLoopPath == null) {
                tmpLoopPath = loopPath;
            }
            if (!entryMap.containsKey(tmpLoopPath)) {
                entryMap.put(tmpLoopPath, new LoopEntry(tmpLoopPath));
            }

            if (function == null) {
                entryMap.get(tmpLoopPath).addPath(resultCol, column);
            } else {// add the exist function to the loopentry
                entryMap.get(tmpLoopPath).addPath(column, column);
                entryMap.get(tmpLoopPath).addFunction(column, function);
                function = null;
            }

        }

        // set sub entry
        String[] splits = loopPath.split("/");
        String path = "";
        LoopEntry tmpentry = null;
        for (String tmp : splits) {
            if (tmp.trim().length() > 0) {
                path += "/" + tmp;
                if (entryMap.containsKey(path)) {
                    if (tmpentry == null) {
                        tmpentry = entryMap.get(path);
                        this.entry = tmpentry;
                    } else {
                        tmpentry.setSubLoop(entryMap.get(path));
                        tmpentry = entryMap.get(path);

                    }
                }
            }
        }
    }

    private void initLoopEntries() {

        for (int i = 0; i < this.entries.length; i++) {

            Map<String, LoopEntry> entryMap = new HashMap<String, LoopEntry>();

            FunctionRegister funcRegister = new FunctionRegister();// list of all the functions
            Function function = null;
            // function is in looppath
            if (this.arrLoopPath[i].indexOf("/*[") >= 0 && this.arrLoopPath[i].indexOf(")]") >= 0) {
                if (arrNodePaths[i].length > 0) {
                    String strTmp = arrLoopPath[i].substring(arrLoopPath[i].lastIndexOf("/"));
                    String strFuncName = strTmp.substring(strTmp.indexOf("*[") + 2, strTmp.indexOf("("));

                    if (funcRegister.isFuncRegistered(strFuncName)) {
                        function = funcRegister.getFunction(strFuncName);
                    }
                }
                arrLoopPath[i] = arrLoopPath[i].substring(0, arrLoopPath[i].lastIndexOf("/"));
            }
            // parse the node path to loopEntry
            for (String column : arrNodePaths[i]) {
                String resultCol = this.arrLoopPath[i];
                String tmpLoopPath = null;
                String[] splits = column.split("/");
                for (String tmp : splits) {
                    if (tmp.equals("..")) {
                        resultCol = resultCol.substring(0, resultCol.lastIndexOf("/"));
                        tmpLoopPath = resultCol;
                    } else if (tmp.equals(".")) {
                        tmpLoopPath = resultCol;
                    } else {

                        if (tmp.indexOf("*[") >= 0 && tmp.indexOf(")]") >= 0) {// has funcion in column
                            resultCol = resultCol.substring(0, resultCol.lastIndexOf("/"));
                            tmpLoopPath = resultCol;// find the function node

                            // get the function name
                            String strFuncName = tmp.substring(tmp.indexOf("*[") + 2, tmp.indexOf("("));

                            if (funcRegister.isFuncRegistered(strFuncName)) {
                                function = funcRegister.getFunction(strFuncName);
                            }

                        } else {
                            resultCol += "/" + tmp;
                        }

                    }
                }
                if (tmpLoopPath == null) {
                    tmpLoopPath = arrLoopPath[i];
                }
                if (!entryMap.containsKey(tmpLoopPath)) {
                    entryMap.put(tmpLoopPath, new LoopEntry(tmpLoopPath));
                }

                if (function == null) {
                    entryMap.get(tmpLoopPath).addPath(resultCol, column);
                } else {// add the exist function to the loopentry
                    entryMap.get(tmpLoopPath).addPath(column, column);
                    entryMap.get(tmpLoopPath).addFunction(column, function);
                    function = null;
                }

            }

            // set sub entry
            String[] splits = arrLoopPath[i].split("/");
            String path = "";
            LoopEntry tmpentry = null;
            for (String tmp : splits) {
                if (tmp.trim().length() > 0) {
                    path += "/" + tmp;
                    if (entryMap.containsKey(path)) {
                        if (tmpentry == null) {
                            tmpentry = entryMap.get(path);
                            this.entries[i] = tmpentry;
                        } else {
                            tmpentry.setSubLoop(entryMap.get(path));
                            tmpentry = entryMap.get(path);

                        }
                    }
                }
            }
            // ================add for bug7632 start==========================
            tmpentry.setOriginalLoopPath(this.arrOrigLoopPath[i]);
            // =======================bug7632 end=============================

        }// for(int i=0;i<length;i++)

    }

    // ===============add for bug7632 begin======================================
    private List<String> arrOrder = new ArrayList<String>(); // is used to tell the order of the xml data

    protected List<String> getLoopOrders() {
        return this.arrOrder;
    }

    protected void addLoopOrder(String string) {
        this.arrOrder.add(string);
    }

    protected void clearLoopOrderArr() {
        this.arrOrder.clear();
    }

    // =================end================================
    /**
     * Testing code
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {

            Runtime.getRuntime().gc();
            long start = Runtime.getRuntime().freeMemory();
            long startall = Runtime.getRuntime().maxMemory();
            long timeStart = System.currentTimeMillis();

            String file = "./src/org/talend/xml/sax/in.xml";
            String rootPath = "/orderdata/order/header";
            String[] loopPath = new String[] { "/.", "/../line-detail" };
            String[][] pathList = new String[][] {
                    { "cust-vendor-num", "cust-vendor-num" + "/@xsi:nil", "cust", "cust" + "/@xsi:nil" },
                    { "prod-num", "prod-num" + "/@xsi:nil", "custom-var", "custom-var" + "/@xsi:nil" } };
            // String rootPath = "/orderdata/order/";
            // String[] loopPath = new String[] { "/header", "line-detail" };
            // String[][] pathList = new String[][] {
            // { "cust-vendor-num", "cust-vendor-num" + "/@xsi:nil", "cust", "cust" + "/@xsi:nil" },
            // { "prod-num", "prod-num" + "/@xsi:nil", "custom-var", "custom-var" + "/@xsi:nil" } };
            // String file = "./src/org/talend/xml/sax/in.xml";
            // String loopPath = "/row/subrow/*[name()]";
            // String[] pathList = new String[] { ".", "." + "/@xsi:nil" };

            // String file = "./src/org/talend/xml/sax/in1.xml";
            // String loopPath = "/schools/school/class/student";// "/areas/area/street/home";
            // String[] pathList = new String[] { "../../@school_name", "../@class_name", "@stu_no", "name", "name" +
            // "/@xsi:nil",
            // "age", "age" + "/@xsi:nil", "sex", "sex" + "/@xsi:nil", "/*[name()]" };// new String[] { "@number",
            // "owner",
            // "house", "../../@city", "../@name",
            // "../@id",
            // "../length","../../../@provite" };

            // String file = "F:/test/XML/multiNS.xml";
            // String loopPath = "/pol:root/pol:order/pol:project";
            // String[] pathList = new String[] { "pol:code", "pol:name", "pol:customer/pol:code",
            // "pol:customer/pol:name" };

            // String file = "F:/to others/toMicheal/outt.xml";
            // String loopPath = "/REF-ETI/ID-COURIE/DOC-REF/EXPLRE-DOC-REF";
            // String[] pathList = new String[] { "../../../@REF-ETI", "../../../AUTEUR", "../../@ID-COURIE",
            // "../../INFO-COURIE/STE-JUR-CT", "../../INFO-COURIE/POL-NUM-STE", "../../INFO-COURIE/PERS-NUM",
            // "../../INFO-COURIE/ID-ARV", "../../INFO-COURIE/COD-EVC", "../../VAR-COURIE/MASTER-I",
            // "../../VAR-COURIE/INTERFACE", "../../VAR-COURIE/INTERFACE/CPOST-APPORT-INT", "../../GEST-REJET",
            // "../@DOC-REF", "../../DOC-EXTN/ID-DOC-EXTN", "COD-ROUTAG", "POSTAL-COD", "NUM-PST-DEST" };

            // SAXLooper looper = new SAXLooper(loopPath, pathList);
            // looper.parse(file);
            // Iterator<Map<String, String>> it = looper.iterator();
            // while (it.hasNext()) {
            // Map<String, String> tmp = it.next();
            // for (String value : pathList) {
            // System.out.print("|" + tmp.get(value));
            // }
            // System.out.println();
            // }
            SAXLooper looper = new SAXLooper(rootPath, loopPath, pathList);
            looper.parse(file);

            Iterator<Map<String, Map<String, String>>> iter = looper.multiIterator();

            while (iter.hasNext()) {
                Map<String, Map<String, String>> map = iter.next();
                // Map<String, String> tmpMap = (Map<String, String>) map.get("value");
                int i;
                for (i = 0; i < loopPath.length; i++) {
                    if (map.get(loopPath[i]) != null) {
                        break;
                    }
                }

                for (int j = 0; j < pathList[i].length; j++) {
                    System.out.print(map.get(loopPath[i]).get(pathList[i][j]));
                }
                System.out.println();

            }

            System.out.println("==Time=" + (System.currentTimeMillis() - timeStart));

            System.out.println("==Memory start all =" + startall);
            System.out.println("==Memory start free =" + start);
            System.out.println("==Memory end all =" + Runtime.getRuntime().maxMemory());
            System.out.println("==Memory end free =" + Runtime.getRuntime().freeMemory());
            System.out.println("==Memory=" + (start - Runtime.getRuntime().freeMemory()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
