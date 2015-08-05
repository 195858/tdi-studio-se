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
package org.talend.scheduler.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.talend.core.model.process.JobInfo;
import org.talend.repository.job.deletion.IJobResourceProtection;
import org.talend.repository.job.deletion.JobResource;
import org.talend.scheduler.i18n.Messages;

/**
 * 
 * A Task encapulates the task properties. <br/>
 * 
 * $Id$
 * 
 */
public class ScheduleTask implements IJobResourceProtection {

    private Map<String, JobResource> idAndResource = new HashMap<String, JobResource>();

    private static final int TASK_FIELD_NUMBER = 6;

    private static final String SPACE = " "; //$NON-NLS-1$

    private String protectionId;

    private JobResource currentResource;

    String day;

    String month;

    String weekly;

    String hour;

    String minute;

    String command;

    /**
     * get the mode of the task.
     * 
     * @return CommandModeType
     */
    public CommandModeType getTaskMode() {
        if (context != null && jobInfo != null && project != null) {
            return CommandModeType.TalendJob;
        } else {
            return CommandModeType.Crontab;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.job.deletion.IJobResourceProtection#calculateProtectedIds()
     */
    public String[] calculateProtectedIds() {
        Set<String> set = idAndResource.keySet();
        return set.toArray(new String[set.size()]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.job.deletion.IJobResourceProtection#getProjectedIds()
     */
    public String[] getProtectedIds() {
        return calculateProtectedIds();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.job.deletion.IJobResourceProtection#getJobResource(java.lang.String)
     */
    public JobResource getJobResource(String id) {
        return idAndResource.get(id);
    }

    public void initProtectionIdAndResource() {
        // String[] splited = job.split(String.valueOf(IPath.SEPARATOR));
        // String absJobName = splited[(splited.length - 1)];
        protectionId = "schedule_taks" + taskNo + "_" + project + "_" + jobInfo.getJobName(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        currentResource = new JobResource(project, jobInfo);
        idAndResource.put(protectionId, currentResource);
        if (subJobs != null) {
            for (JobInfo subJob : subJobs) {
                String subJobId = "sub_job_of_" + jobInfo.getJobName() + taskNo + "_" + project + "_" + subJob.getJobName(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                idAndResource.put(subJobId, new JobResource(project, subJob));
            }
        }
    }

    String context;

    String fullJobNameAndPath;

    JobInfo jobInfo;

    String project;

    JobInfo[] subJobs;

    int taskNo;

    /**
     * Sets the taksNo.
     * 
     * @param taksNo the taksNo to set
     */
    public void setTaskNo(int taskNo) {
        this.taskNo = taskNo;
    }

    /**
     * Sets the subJobs.
     * 
     * @param subJobs the subJobs to set
     */
    public void setSubJobs(JobInfo[] subJobs) {
        this.subJobs = subJobs;
    }

    /**
     * DOC dev Comment method "getContext".
     * 
     * @return context
     */
    public String getContext() {
        return context;
    }

    /**
     * DOC dev Comment method "getJob".
     * 
     * @return job
     */
    public JobInfo getJob() {
        return jobInfo;
    }

    /**
     * DOC dev Comment method "getProject".
     * 
     * @return project
     */
    public String getProject() {
        return project;
    }

    /**
     * DOC dev Comment method "setContext".
     * 
     * @param context String
     */
    public void setContext(String context) {
        this.context = context;
    }

    /**
     * DOC dev Comment method "setJob".
     * 
     * @param job String
     */
    public void setJobInfo(JobInfo jobInfo) {
        this.jobInfo = jobInfo;
    }

    /**
     * DOC dev Comment method "setProject".
     * 
     * @param project String
     */
    public void setProject(String project) {
        this.project = project;
    }

    public ScheduleTask() {
    }

    public ScheduleTask(String plainCommand) throws SchedulerException {
        setPlainCommand(plainCommand);
    }

    /**
     * Sets the plain command to task's property.
     * 
     * @param plainCommand String
     * @throws SchedulerException SchedulerException
     */
    public void setPlainCommand(String plainCommand) throws SchedulerException {
        if (plainCommand.indexOf(SPACE) == -1) {
            throw new SchedulerException(Messages.getString("ScheduleTask.scheCustomFormatWrong")); //$NON-NLS-1$
        }

        String[] strs = plainCommand.split(SPACE);
        List<String> list = new ArrayList<String>();
        for (String string : strs) {
            if (string == null || string.trim().equals("")) { //$NON-NLS-1$
                continue;
            }
            list.add(string);
        }

        if (list.size() < TASK_FIELD_NUMBER) {
            throw new SchedulerException(Messages.getString("ScheduleTask.scheCustomFormatWrong")); //$NON-NLS-1$
        }

        String minuteTmp = list.get(0);
        checkFormat(minuteTmp, "([0-9]+(,[0-9])*)+", "Minute"); //$NON-NLS-1$ //$NON-NLS-2$
        setMinute(minuteTmp);

        String hourTmp = list.get(1);
        checkFormat(hourTmp, "([0-9]((,|-)[0-9])*)+", "Hour"); //$NON-NLS-1$ //$NON-NLS-2$
        setHour(hourTmp);

        String monthTmp = list.get(2);
        checkFormat(monthTmp, "([0-9]((,|-)[0-9])*)+|\\*", "Month"); //$NON-NLS-1$ //$NON-NLS-2$
        setMonth(monthTmp);

        String dayTmp = list.get(3);
        checkFormat(dayTmp, "([0-9]((,|-)[0-9])*)+|\\*", "Day"); //$NON-NLS-1$ //$NON-NLS-2$
        setDay(dayTmp);

        String weekDayTmp = list.get(4);
        checkFormat(weekDayTmp, "([0-9](,[0-9])*)+|\\*", "Day of week"); //$NON-NLS-1$ //$NON-NLS-2$
        setWeekly(weekDayTmp);

        setCommand(getCommand(plainCommand));

    }

    /**
     * DOC dev Comment method "getCommand".
     * 
     * @param plainCommand String
     * @return String
     */
    private String getCommand(String plainCommand) {
        if (plainCommand == null) {
            return plainCommand;
        }

        for (int i = 0; i < TASK_FIELD_NUMBER - 1; i++) {
            plainCommand = plainCommand.trim();
            plainCommand = plainCommand.substring(plainCommand.indexOf(" ")); //$NON-NLS-1$
        }
        return plainCommand.trim();
    }

    /**
     * Check the command format with regex method description.
     * 
     * @param commandInput String
     * @param pattern String
     * @param detail String
     * @return boolean
     * @throws SchedulerException SchedulerException
     */
    private boolean checkFormat(String commandInput, String pattern, String detail) throws SchedulerException {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(commandInput);
        if (m.matches()) {
            return true;
        }

        throw new SchedulerException(Messages.getString("ScheduleTask.scheCustomFormatWrong2") + detail); //$NON-NLS-1$

    }

    /**
     * Getter for command.
     * 
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * Sets the command.
     * 
     * @param command the command to set
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * Getter for day.
     * 
     * @return the day
     */
    public String getDay() {
        return day;
    }

    /**
     * Sets the day.
     * 
     * @param day the day to set
     */
    public void setDay(String day) {
        this.day = day;
    }

    /**
     * Getter for hour.
     * 
     * @return the hour
     */
    public String getHour() {
        return hour;
    }

    /**
     * Sets the hour.
     * 
     * @param hour the hour to set
     */
    public void setHour(String hour) {
        this.hour = hour;
    }

    /**
     * Getter for minute.
     * 
     * @return the minute
     */
    public String getMinute() {
        return minute;
    }

    /**
     * Sets the minute.
     * 
     * @param minute the minute to set
     */
    public void setMinute(String minute) {
        this.minute = minute;
    }

    /**
     * Getter for month.
     * 
     * @return the month
     */
    public String getMonth() {
        return month;
    }

    /**
     * Sets the month.
     * 
     * @param month the month to set
     */
    public void setMonth(String month) {
        this.month = month;
    }

    /**
     * Getter for weekly.
     * 
     * @return the weekly
     */
    public String getWeekly() {
        return weekly;
    }

    /**
     * Sets the weekly.
     * 
     * @param weekly the weekly to set
     */
    public void setWeekly(String weekly) {
        this.weekly = weekly;
    }

    public String getPlainCommand() {
        String space = SPACE;
        StringBuilder sb = new StringBuilder();
        sb.append(getMinute()).append(space).append(getHour()).append(space).append(getMonth()).append(space).append(getDay())
                .append(space).append(getWeekly()).append(space).append(getCommand());

        return sb.toString();
    }

    public String getFullJobNameAndPath() {
        return this.fullJobNameAndPath;
    }

    public void setFullJobNameAndPath(String fullJobNameAndPath) {
        this.fullJobNameAndPath = fullJobNameAndPath;
    }
}
