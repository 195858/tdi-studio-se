package com.sforce.soap.partner;

/**
 * Generated by ComplexTypeCodeGenerator.java. Please do not edit.
 */
public interface IMergeRequest  {

      /**
       * element : masterRecord of type {urn:sobject.partner.soap.sforce.com}sObject
       * java type: com.sforce.soap.partner.sobject.SObject
       */

      public com.sforce.soap.partner.sobject.ISObject getMasterRecord();

      public void setMasterRecord(com.sforce.soap.partner.sobject.ISObject masterRecord);

      /**
       * element : recordToMergeIds of type {urn:partner.soap.sforce.com}ID
       * java type: java.lang.String[]
       */

      public java.lang.String[] getRecordToMergeIds();

      public void setRecordToMergeIds(java.lang.String[] recordToMergeIds);


}