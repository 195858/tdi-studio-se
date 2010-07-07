// ============================================================================
//
// Copyright (C) 2006-2010 Talend Inc. - www.talend.com
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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.dialogs.EventLoopProgressMonitor;
import org.eclipse.ui.progress.IProgressService;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.IPerformance;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.process.IProcess2;
import org.talend.core.model.process.ITargetExecutionConfig;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.runprocess.ProcessMessage.MsgType;
import org.talend.designer.runprocess.data.TraceData;
import org.talend.designer.runprocess.i18n.Messages;
import org.talend.designer.runprocess.prefs.RunProcessPrefsConstants;
import org.talend.designer.runprocess.ui.ProcessContextComposite;
import org.talend.designer.runprocess.ui.actions.ClearPerformanceAction;
import org.talend.designer.runprocess.ui.actions.ClearTraceAction;
import org.talend.runprocess.data.PerformanceData;

/**
 * Context of a running process. <br/>
 * 
 * $Id$
 * 
 * 
 */
public class RunProcessContext {

    private static final int STRING_LENGTH = 256;

    public static final String PROP_RUNNING = "RunProcessContext.Running"; //$NON-NLS-1$

    public static final String PROP_MONITOR = "RunProcessContext.MonitorPerf"; //$NON-NLS-1$

    public static final String TRACE_MONITOR = "RunProcessContext.MonitorTrace"; //$NON-NLS-1$

    private static final String PROR_SWITCH_TIME = "RunProcesscontext.Message.Watch"; //$NON-NLS-1$

    public static final String PREVIOUS_ROW = "RunProcessContext.PreviousRow";

    private static final String WATCH_PARAM = "--watch"; //$NON-NLS-1$

    public static final String NEXTBREAKPOINT = "RunProcessContext.NextBreakpoint";

    private boolean watchAllowed;

    private Boolean nextBreakpoint = false;

    /** Change property listeners. */
    private final transient PropertyChangeSupport pcsDelegate;

    /** The process. */
    private final IProcess process;

    /** The selected context to run process with. */
    private IContext selectedContext;

    /** The selected server configuration to run process with. */
    private ITargetExecutionConfig selectedTargetExecutionConfig;

    /** Performance monitoring activated. */
    private boolean monitorPerf;

    /** Trace monitoring activated. */
    private boolean monitorTrace;

    private boolean selectAllTrace = false;

    /** Is process running. */
    private boolean running;

    /** The executing process. */
    private Process ps;

    /** Monitor of the running process. */
    private IProcessMonitor psMonitor;

    /** Monitor of the running process. */
    private PerformanceMonitor perfMonitor;

    /** Monitor for Traces of the running process. */
    private TraceMonitor traceMonitor;

    /** Kill is in progress. */
    private boolean killing;

    private boolean lastIsRow = false;

    private final IProcessMessageManager processMessageManager;

    private int statsPort = IProcessor.NO_STATISTICS;

    private int tracesPort = IProcessor.NO_TRACES;

    private org.eclipse.debug.core.model.IProcess debugProcess;

    private boolean saveBeforeRun;

    private boolean clearBeforeExec = true;

    private boolean isTracPause = false;

    private boolean startingMessageWritten;

    private List<PerformanceMonitor> perMonitorList = new ArrayList<PerformanceMonitor>();

    /**
     * Constrcuts a new RunProcessContext.
     * 
     * @param process The process.
     */
    public RunProcessContext(IProcess process) {
        super();

        this.process = process;
        selectedContext = process.getContextManager().getDefaultContext();

        pcsDelegate = new PropertyChangeSupport(this);
        this.processMessageManager = new ProcessMessageManager();

        setMonitorPerf(RunProcessPlugin.getDefault().getPreferenceStore().getBoolean(RunProcessPrefsConstants.ISSTATISTICSRUN));
        setMonitorTrace(RunProcessPlugin.getDefault().getPreferenceStore().getBoolean(RunProcessPrefsConstants.ISTRACESRUN));
        setWatchAllowed(RunProcessPlugin.getDefault().getPreferenceStore().getBoolean(RunProcessPrefsConstants.ISEXECTIMERUN));
        setSaveBeforeRun(RunProcessPlugin.getDefault().getPreferenceStore().getBoolean(RunProcessPrefsConstants.ISSAVEBEFORERUN));
        setClearBeforeExec(RunProcessPlugin.getDefault().getPreferenceStore().getBoolean(
                RunProcessPrefsConstants.ISCLEARBEFORERUN));
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
        if (l == null) {
            throw new IllegalArgumentException();
        }

        processMessageManager.addPropertyChangeListener(l);
        pcsDelegate.addPropertyChangeListener(l);
    }

    protected void firePropertyChange(String property, Object oldValue, Object newValue) {
        if (pcsDelegate.hasListeners(property)) {
            pcsDelegate.firePropertyChange(property, oldValue, newValue);
        }
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
        if (l != null) {
            pcsDelegate.removePropertyChangeListener(l);
            processMessageManager.removePropertyChangeListener(l);
        }
    }

    public IProcess getProcess() {
        return process;
    }

    public void clearMessages() {
        processMessageManager.clearMessages();
    }

    public void switchTime() {
        // TODO should do something here.
        firePropertyChange(PROR_SWITCH_TIME, "true", "false"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public Collection<IProcessMessage> getMessages() {
        return processMessageManager.getMessages();
    }

    public void addMessage(IProcessMessage message) {
        processMessageManager.addMessage(message);
    }

    /**
     * Getter for monitorPerf.
     * 
     * @return the monitorPerf
     */
    public boolean isMonitorPerf() {
        return this.monitorPerf;
    }

    /**
     * Sets the monitorPerf.
     * 
     * @param monitorPerf the monitorPerf to set
     */
    public void setMonitorPerf(boolean monitorPerf) {
        if (this.monitorPerf != monitorPerf) {
            this.monitorPerf = monitorPerf;
            if (process instanceof IProcess2) {
                ((IProcess2) process).setNeedRegenerateCode(true);
            }
            firePropertyChange(PROP_MONITOR, Boolean.valueOf(!monitorPerf), Boolean.valueOf(monitorPerf));
        }
    }

    /**
     * Getter for monitorTrace.
     * 
     * @return the monitorTrace
     */
    public boolean isMonitorTrace() {
        return this.monitorTrace;
    }

    /**
     * Sets the monitorTrace.
     * 
     * @param monitorTrace the monitorTraceto set
     */
    public void setMonitorTrace(boolean monitorTrace) {
        if (this.monitorTrace != monitorTrace) {
            this.monitorTrace = monitorTrace;
            if (process instanceof IProcess2) {
                ((IProcess2) process).setNeedRegenerateCode(true);
            }

            firePropertyChange(TRACE_MONITOR, Boolean.valueOf(!monitorTrace), Boolean.valueOf(monitorTrace));
        }
    }

    /**
     * 
     * ggu Comment method "hasConnectionTrace".
     * 
     * bug 11227
     */
    protected boolean hasConnectionTrace() {
        for (INode node : process.getGraphicalNodes()) {
            List<? extends IConnection> outgoingConnections = node.getOutgoingConnections();
            for (IConnection conn : outgoingConnections) {
                if (conn.isActivate() && conn.isTraceConnection()) {
                    return true;
                }
            }
        }
        return false;

    }

    /**
     * Getter for running.
     * 
     * @return the running
     */
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Sets the running.
     * 
     * @param running the running to set
     */
    public void setRunning(boolean running) {
        if (this.running != running) {
            this.running = running;
            firePropertyChange(PROP_RUNNING, Boolean.valueOf(!running), Boolean.valueOf(running));
        }
    }

    public IContext getSelectedContext() {
        return selectedContext;
    }

    public void setSelectedContext(IContext context) {
        selectedContext = context;
    }

    Thread processMonitorThread;

    private boolean nextRow = false;

    private boolean priviousRow = false;

    private void showErrorMassage(String category) {
        String title = Messages.getString("RunProcessContext.PortErrorTitle", category); //$NON-NLS-1$
        String message = Messages.getString("RunProcessContext.PortErrorMessage", category.toLowerCase()); //$NON-NLS-1$
        MessageDialog.openError(PlatformUI.getWorkbench().getDisplay().getActiveShell(), title, message);
    }

    private void testPort() {

        if (monitorPerf) {
            try {
                findNewStatsPort();
            } catch (Exception e) {
                statsPort = IProcessor.NO_STATISTICS;
            }
            if (getStatisticsPort() == IProcessor.NO_STATISTICS) {
                showErrorMassage(Messages.getString("RunProcessContext.PortErrorStats")); //$NON-NLS-1$
                // disable the check.
                setMonitorPerf(false);
            }

        } else {
            statsPort = IProcessor.NO_STATISTICS;
        }
        if (monitorTrace) {
            try {
                findNewTracesPort();
            } catch (Exception e) {
                tracesPort = IProcessor.NO_TRACES;
            }
            if (getTracesPort() == IProcessor.NO_TRACES) {
                showErrorMassage(Messages.getString("RunProcessContext.PortErrorTraces")); //$NON-NLS-1$
                this.monitorTrace = false;
                // disable the check.
                setMonitorTrace(false);

            }
        } else {
            tracesPort = IProcessor.NO_TRACES;
        }

    }

    /**
     * 
     * cLi Comment method "allowMonitorTrace".
     * 
     * feature 6355, enable trace.
     * 
     * about the variable "monitorTrace", It used for a global trace.
     */
    public boolean allowMonitorTrace() {
        return this.monitorTrace;
    }

    /**
     * 
     * cLi Comment method "checkTraces".
     * 
     * feature 6355
     */
    private void checkTraces() {
        // connection settings for traces.
        boolean found = false;
        if (this.monitorTrace) {
            for (IConnection conn : this.getProcess().getAllConnections(null)) {
                IElementParameter param = conn.getElementParameter(EParameterName.TRACES_CONNECTION_ENABLE.getName());
                if (param != null && Boolean.TRUE.equals(param.getValue())) {
                    found = true;
                    break;
                }
            }
        }
        if (found) {
            this.monitorTrace = true;
        }
    }

    public boolean checkBreakpoint() {
        boolean found = false;
        // if (this.monitorTrace) {
        for (IConnection conn : this.getProcess().getAllConnections(null)) {
            IElementParameter param = conn.getElementParameter(EParameterName.ACTIVEBREAKPOINT.getName());
            IElementParameter param2 = conn.getElementParameter(EParameterName.TRACES_CONNECTION_ENABLE.getName());
            if (param != null && param2 != null && Boolean.TRUE.equals(param.getValue())
                    && Boolean.TRUE.equals(param2.getValue())) {
                found = true;
                break;
            }
        }
        // }
        return found;
    }

    /**
     * Launch the process.
     */
    public void exec(final Shell shell) {
        if (process instanceof org.talend.designer.core.ui.editor.process.Process) {
            org.talend.designer.core.ui.editor.process.Process prs = (org.talend.designer.core.ui.editor.process.Process) process;
            prs.checkDifferenceWithRepository();
        }
        checkTraces();
        setRunning(true);

        if (ProcessContextComposite.promptConfirmLauch(shell, getSelectedContext(), process)) {

            ClearPerformanceAction clearPerfAction = new ClearPerformanceAction();
            clearPerfAction.setProcess(process);
            clearPerfAction.run();

            ClearTraceAction clearTraceAction = new ClearTraceAction();
            clearTraceAction.setProcess(process);
            clearTraceAction.run();
            if (monitorPerf) {
                this.getStatisticsPort();
            }
            final IProcessor processor = getProcessor(process);
            IProgressService progressService = PlatformUI.getWorkbench().getProgressService();

            try {
                progressService.run(false, true, new IRunnableWithProgress() {

                    public void run(final IProgressMonitor monitor) {

                        final EventLoopProgressMonitor progressMonitor = new EventLoopProgressMonitor(monitor);

                        progressMonitor.beginTask(Messages.getString("ProcessComposite.buildTask"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
                        try {
                            testPort();
                            // findNewStatsPort();
                            final IContext context = getSelectedContext();
                            if (monitorPerf) {
                                clearThreads();
                                perfMonitor = new PerformanceMonitor();
                                new Thread(perfMonitor, "PerfMonitor_" + process.getLabel()).start(); //$NON-NLS-1$
                                perMonitorList.add(perfMonitor);
                            }
                            // findNewTracesPort();
                            if (monitorTrace) {
                                traceMonitor = new TraceMonitor();
                                new Thread(traceMonitor, "TraceMonitor_" + process.getLabel()).start(); //$NON-NLS-1$
                            }

                            final String watchParam = RunProcessContext.this.isWatchAllowed() ? WATCH_PARAM : null;
                            processor.setContext(context);
                            processor.setTargetExecutionConfig(getSelectedTargetExecutionConfig());

                            ProcessorUtilities.generateCode(process, context, getStatisticsPort() != IProcessor.NO_STATISTICS,
                                    getTracesPort() != IProcessor.NO_TRACES && hasConnectionTrace(), true, progressMonitor);
                            final boolean[] refreshUiAndWait = new boolean[1];
                            refreshUiAndWait[0] = true;
                            final Display display = shell.getDisplay();
                            new Thread(new Runnable() {

                                public void run() {
                                    display.syncExec(new Runnable() {

                                        public void run() {
                                            try {
                                                startingMessageWritten = false;

                                                // see feature 0004820: The run
                                                // job doesn't verify if code is
                                                // correct
                                                // before launching
                                                if (!JobErrorsChecker.hasErrors(shell)) {
                                                    ps = processor.run(getStatisticsPort(), getTracesPort(), watchParam,
                                                            progressMonitor, processMessageManager);
                                                }

                                                if (ps != null && !progressMonitor.isCanceled()) {

                                                    psMonitor = createProcessMonitor(ps);

                                                    startingMessageWritten = true;

                                                    final String startingPattern = Messages
                                                            .getString("ProcessComposite.startPattern"); //$NON-NLS-1$
                                                    MessageFormat mf = new MessageFormat(startingPattern);
                                                    String welcomeMsg = mf
                                                            .format(new Object[] { process.getLabel(), new Date() });
                                                    processMessageManager.addMessage(new ProcessMessage(MsgType.CORE_OUT,
                                                            welcomeMsg + "\r\n")); //$NON-NLS-1$
                                                    processMonitorThread = new Thread(psMonitor);
                                                    processMonitorThread.start();
                                                } else {
                                                    kill();
                                                    setRunning(false);
                                                }
                                            } catch (Throwable e) {
                                                // catch any Exception or Error
                                                // to kill the process, see bug
                                                // 0003567
                                                Throwable cause = e.getCause();
                                                if (cause != null && cause.getClass().equals(InterruptedException.class)) {
                                                    setRunning(false);
                                                } else {
                                                    ExceptionHandler.process(e);
                                                    addErrorMessage(e);
                                                    kill();
                                                }
                                            } finally {
                                                // progressMonitor.done();
                                                refreshUiAndWait[0] = false;
                                            }
                                        }
                                    });
                                }
                            }, "RunProcess_" + process.getLabel()).start(); //$NON-NLS-1$
                            while (refreshUiAndWait[0] && !progressMonitor.isCanceled()) {
                                if (!display.readAndDispatch()) {
                                    display.sleep();
                                }
                                synchronized (this) {
                                    try {
                                        final long waitTime = 50;
                                        wait(waitTime);
                                    } catch (InterruptedException e) {
                                        // Do nothing
                                    }
                                }

                            }

                        } catch (Throwable e) {
                            // catch any Exception or Error to kill the process,
                            // see bug 0003567
                            ExceptionHandler.process(e);
                            addErrorMessage(e);
                            kill();
                        } finally {
                            progressMonitor.done();
                            // System.out.println("exitValue:" +
                            // ps.exitValue());
                        }
                    }
                });
            } catch (InvocationTargetException e1) {
                addErrorMessage(e1);
            } catch (InterruptedException e1) {
                addErrorMessage(e1);
            }

        } else {
            // See bug 0003567: When a prompt from context is cancelled or a
            // fatal error occurs during a job exec the
            // Kill button have to be pressed manually.
            setRunning(false);
        }
    }

    /**
     * DOC amaumont Comment method "getProcessor".
     * 
     * @return
     */
    protected IProcessor getProcessor(IProcess process) {
        return ProcessorUtilities.getProcessor(process);
    }

    /**
     * Kill the process.
     * 
     * @return Exit code of the process.
     */
    public synchronized int kill() {
        int exitCode;

        if (!killing && isRunning()) {
            killing = true;
            try {
                exitCode = killProcess();
                if (startingMessageWritten) {
                    displayJobEndMessage(exitCode);
                }
            } finally {
                killing = false;
            }
        } else {
            exitCode = 0;
        }

        setRunning(false);
        return exitCode;
    }

    /**
     * DOC yexiaowei Comment method "displayJobEndMessage".
     * 
     * @param exitCode
     */
    private void displayJobEndMessage(int exitCode) {

        final String endingPattern = Messages.getString("ProcessComposite.endPattern"); //$NON-NLS-1$
        MessageFormat mf = new MessageFormat(endingPattern);
        String byeMsg = mf.format(new Object[] { process.getLabel(), new Date(), new Integer(exitCode) });
        byeMsg = (processMessageManager.isLastMessageEndWithCR() ? "" : "\n") + byeMsg; //$NON-NLS-1$ //$NON-NLS-2$
        processMessageManager.addMessage(new ProcessMessage(MsgType.CORE_OUT, byeMsg));
    }

    private int killProcess() {
        if (psMonitor != null) {
            if (perfMonitor != null) {
                perfMonitor.stopThread();
                perfMonitor = null;
            }
            if (traceMonitor != null) {
                traceMonitor.stopThread();
                traceMonitor = null;
            }
            psMonitor.stopThread();
            psMonitor = null;
        }
        int exitCode = 0;
        if (ps != null) { // running process
            ps.destroy();
            try {
                exitCode = ps.exitValue();
            } catch (IllegalThreadStateException itse) {
                // Can be throw on some UNIX system :(
                // but the process is really killed.
            }
            ps = null;
        }
        if (debugProcess != null) {
            try {
                debugProcess.terminate();
            } catch (DebugException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
                ExceptionHandler.process(e);
            }
        }
        return exitCode;
    }

    public void addErrorMessage(Throwable e) {
        StringBuffer message = new StringBuffer(STRING_LENGTH);
        message.append(Messages.getString("ProcessComposite.execFailed")); //$NON-NLS-1$
        message.append(e.getMessage());
        if (e.getCause() != null) {
            message.append("\n["); //$NON-NLS-1$
            message.append(e.getCause().getMessage());
            message.append("]"); //$NON-NLS-1$
        }
        message.append("\n"); //$NON-NLS-1$

        IProcessMessage processMsg = new ProcessMessage(MsgType.CORE_ERR, message.toString());
        processMessageManager.addMessage(processMsg);
    }

    private void findNewStatsPort() {
        statsPort = monitorPerf ? RunProcessPlugin.getDefault().getRunProcessContextManager().getPortForStatistics()
                : IProcessor.NO_STATISTICS;
    }

    public int getStatisticsPort() {
        return statsPort;
    }

    private void findNewTracesPort() {
        tracesPort = monitorTrace ? RunProcessPlugin.getDefault().getRunProcessContextManager().getPortForTraces()
                : IProcessor.NO_TRACES;
    }

    public int getTracesPort() {
        return tracesPort;
    }

    // private int getWatchPort() {
    // int port = watchAllowed ? RunProcessPlugin.getDefault()
    // .getRunProcessContextManager().getPortForWatch(this)
    // : Processor.WATCH_LIMITED;
    // return port;
    // }

    /**
     * Process activity monitor. <br/>
     * 
     * $Id$
     * 
     */
    protected class ProcessMonitor implements IProcessMonitor {

        volatile boolean stopThread;

        /** The monitoring process. */
        private final Process process;

        /** Input stream for stdout of the process. */
        private final BufferedReader outIs;

        /** Input stream for stderr of the process. */
        private final BufferedReader errIs;

        private boolean hasCompilationError = false;

        public ProcessMonitor(Process ps) {

            super();

            this.process = ps;
            this.outIs = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            this.errIs = new BufferedReader(new InputStreamReader(ps.getErrorStream()));
        }

        /**
         * @see java.lang.Runnable#run()
         */
        public void run() {
            while (!stopThread) {
                boolean dataPiped = extractMessages(false);

                boolean ended;
                try {

                    if (!hasCompilationError) {
                        process.exitValue();
                    }

                    // flush remaining messages
                    while (extractMessages(true) && !stopThread) {
                        ;
                    }

                    // Read the end of the stream after the end of the process
                    ended = true;
                    stopThread = true;
                    try {
                        this.process.getInputStream().close();
                    } catch (IOException e) {
                        ExceptionHandler.process(e);
                    }

                    try {
                        this.process.getErrorStream().close();
                    } catch (IOException e) {
                        ExceptionHandler.process(e);
                    }

                } catch (IllegalThreadStateException itse) {
                    ended = false;
                } catch (Exception e) {
                    ended = false;
                }

                if (!dataPiped && !ended) {
                    synchronized (this) {
                        try {
                            final long waitTime = 100;
                            wait(waitTime);
                        } catch (InterruptedException e) {
                            // Do nothing
                        }
                    }
                }
            }

            kill();
        }

        public void stopThread() {
            stopThread = true;
            synchronized (this) {
                notify();
            }
        }

        private boolean extractMessages(boolean flush) {
            IProcessMessage messageOut = null;
            IProcessMessage messageErr = null;
            try {
                messageErr = extractMessage(errIs, MsgType.STD_ERR, flush);
                if (messageErr != null) {
                    if (messageErr.getContent().contains("Unresolved compilation problem")) { //$NON-NLS-1$
                        hasCompilationError = true;
                    }
                    processMessageManager.addMessage(messageErr);
                }
                messageOut = extractMessage(outIs, MsgType.STD_OUT, flush);
                if (messageOut != null) {
                    processMessageManager.addMessage(messageOut);
                }
            } catch (IOException ioe) {
                addErrorMessage(ioe);
                ExceptionHandler.process(ioe);
            }
            return messageOut != null || messageErr != null;
        }

        /**
         * Extract a message from a stream.
         * 
         * @param is Input stream to be read.
         * @param type Type of message read.
         * @param flush
         * @return the message extracted or null if no message was present.
         * @throws IOException Extraction failure.
         */
        private IProcessMessage extractMessage(final BufferedReader is, MsgType type, boolean flush) throws IOException {

            IProcessMessage msg;
            if (is.ready()) {

                StringBuilder sb = new StringBuilder();

                String data = null;
                long timeStart = System.currentTimeMillis();
                while (is.ready()) {
                    data = is.readLine();
                    if (data == null) {
                        break;
                    }
                    sb.append(data).append("\n"); //$NON-NLS-1$
                    if (sb.length() > 1024 || System.currentTimeMillis() - timeStart > 100) {
                        break;
                    }
                }

                msg = new ProcessMessage(type, sb.toString());
            } else {
                msg = null;
            }
            return msg;
        }

    }

    /**
     * Performance monitor. <br/>
     * 
     * $Id$
     * 
     */
    private class PerformanceMonitor implements Runnable {

        private volatile boolean stopThread;

        private Set<IPerformance> performanceDataSet = new HashSet<IPerformance>();

        public PerformanceMonitor() {
            super();
        }

        /**
         * @see java.lang.Runnable#run()
         */
        public void run() {
            // final int acceptTimeout = 30000;

            // Waiting connection from process
            Socket processSocket = null;
            ServerSocket serverSock = null;
            do {
                try {
                    serverSock = new ServerSocket(getStatisticsPort());
                    // serverSock.setSoTimeout(acceptTimeout);
                    processSocket = serverSock.accept();
                } catch (IOException e) {
                    ExceptionHandler.process(e);
                    stopThread |= !isRunning();
                } finally {
                    try {
                        if (serverSock != null) {
                            serverSock.close();
                        }
                    } catch (IOException e1) {
                        // e1.printStackTrace();
                        ExceptionHandler.process(e1);
                    }
                }
            } while (processSocket == null && !stopThread);

            if (processSocket != null && !stopThread) {
                try {
                    InputStream in = processSocket.getInputStream();
                    LineNumberReader reader = new LineNumberReader(new InputStreamReader(in));
                    while (!stopThread) {

                        String line = reader.readLine();

                        if (line != null) {
                            if (line.startsWith("0")) {
                                // 0 = job information
                                // 1 = connection information
                                continue;
                            }
                            String[] infos = line.split("\\|");
                            if (infos.length < 5 || !infos[1].equals(infos[2]) || !infos[1].equals(infos[3])) {
                                // we only take actually informations for the main jobs, other informations won't be
                                // used.
                                continue;
                            }

                            // "0|GnqOsQ|GnqOsQ|GnqOsQ|iterate1|exec1" -->"iterate1|exec1"
                            if (line.trim().length() > 22) {
                                String temp = line.substring(line.indexOf("|") + 1); // remove the 0|
                                temp = temp.substring(temp.indexOf("|") + 1); // remove the first GnqOsQ|
                                temp = temp.substring(temp.indexOf("|") + 1); // remove the second GnqOsQ|
                                temp = temp.substring(temp.indexOf("|") + 1); // remove the third GnqOsQ|
                                line = temp;
                            }
                        }
                        final String data = line;

                        // // for feature:11356
                        // if (data != null && data.split("\\|").length == 2) {
                        // continue;
                        // }

                        if (data == null) {
                            stopThread = true;
                        } else {
                            final PerformanceData perfData = new PerformanceData(data);
                            String connectionId = perfData.getConnectionId();
                            // handle connectionId as row1.1 and row1
                            connectionId = connectionId.split("\\.")[0]; //$NON-NLS-1$
                            final IConnection conn = findConnection(connectionId);
                            if (conn != null && conn instanceof IPerformance) {
                                final IPerformance performance = (IPerformance) conn;
                                if (!performanceDataSet.contains(performance)) {
                                    performance.resetStatus();
                                }
                                performanceDataSet.add(performance);
                                Display.getDefault().syncExec(new Runnable() {

                                    public void run() {
                                        if (data != null) {
                                            if (perfData.isClearCommand()) {
                                                performance.clearPerformanceDataOnUI();
                                            } else {
                                                performance.setPerformanceData(data);
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }
                    // clear status for running next time
                    for (IPerformance performance : performanceDataSet) {
                        performance.resetStatus();
                    }
                } catch (IOException e) {
                    // Do nothing : process is ended
                } finally {
                    try {
                        processSocket.close();
                    } catch (IOException ioe) {
                        // Do nothing
                    }
                }
            }
        }

        public void stopThread() {
            stopThread = true;
            synchronized (this) {
                notify();
            }
        }

        private INode findNode(final String nodeId) {
            INode node = null;
            for (Iterator<? extends INode> i = process.getGraphicalNodes().iterator(); node == null && i.hasNext();) {
                INode psNode = i.next();
                if (nodeId.equals(psNode.getUniqueName())) {
                    node = psNode;
                }
            }
            return node;
        }

        private IConnection findConnection(final String connectionId) {
            IConnection conn = null;
            IConnection[] conns = process.getAllConnections(null);
            for (int i = 0; i < conns.length; i++) {
                if (connectionId.equals(conns[i].getUniqueName())) {
                    conn = conns[i];
                }
            }
            return conn;
        }
    }

    /**
     * Trace monitor. <br/>
     * 
     * $Id$
     * 
     */
    private class TraceMonitor implements Runnable {

        private volatile boolean stopThread;

        private volatile boolean userow = false;

        int dataSize = 0;

        int readSize = 0;

        private List connectionSize = new ArrayList();

        public TraceMonitor() {
            super();
            isTracPause = false;
        }

        /**
         * @see java.lang.Runnable#run()
         */
        public void run() {
            // final int acceptTimeout = 30000;

            // Waiting connection from process
            Socket processSocket = null;
            ServerSocket serverSock = null;
            final Map<IConnection, Map<String, String>> connAndTraces = new HashMap<IConnection, Map<String, String>>();
            do {
                try {
                    serverSock = new ServerSocket(getTracesPort());
                    // serverSock.setSoTimeout(acceptTimeout);
                    processSocket = serverSock.accept();
                } catch (IOException e) {
                    ExceptionHandler.process(e);
                    try {
                        if (serverSock != null) {
                            serverSock.close();
                        }
                    } catch (IOException e1) {
                        // e1.printStackTrace();
                        ExceptionHandler.process(e1);
                    } finally {
                        try {
                            if (serverSock != null) {
                                serverSock.close();
                            }
                        } catch (IOException e1) {
                            // e1.printStackTrace();
                            ExceptionHandler.process(e1);
                        }
                    }
                    stopThread |= !isRunning();
                }
            } while (processSocket == null && !stopThread);

            if (processSocket != null && !stopThread) {
                try {
                    InputStream in = processSocket.getInputStream();
                    LineNumberReader reader = new LineNumberReader(new InputStreamReader(in));

                    boolean lastIsPrivious = false;
                    boolean lastRow = false;
                    final List<Map<String, String>> connectionData = new ArrayList<Map<String, String>>();
                    while (!stopThread) {
                        final String data = reader.readLine();
                        PrintWriter pred = new java.io.PrintWriter(new java.io.BufferedWriter(new java.io.OutputStreamWriter(
                                processSocket.getOutputStream())), true);
                        if (data == null) {
                            stopThread = true;
                        } else if ("ID_STATUS".equals(data)) {
                            if (isTracPause()) {
                                pred.println("PAUSE"); //$NON-NLS-1$
                            } else if (lastIsRow) {
                                pred.println("NEXT_ROW");
                            } else {
                                // testing only
                                pred.println("NEXT_BREAKPOINT");
                            }
                            continue;
                        } else if ("UI_STATUS".equals(data)) {
                            // wait for UI here, for next click, then send STATUS_OK
                            if (!checkBreakpoint()) {
                                firePropertyChange(NEXTBREAKPOINT, true, false);
                            } else {
                                firePropertyChange(NEXTBREAKPOINT, false, true);
                            }
                            if (isNextPoint()) {

                                firePropertyChange(PREVIOUS_ROW, false, true);
                                pred.println("STATUS_OK");
                                setNextBreakPoint(false);
                                lastIsRow = false;
                            } else if (isNextRow()) {
                                firePropertyChange(PREVIOUS_ROW, false, true);
                                if (readSize > 0) {
                                    pred.println("STATUS_WAITING");
                                    if (lastIsPrivious) {
                                        readSize = readSize - connectionSize.size();
                                        lastIsPrivious = false;
                                    }
                                    for (int b = 0; b < connectionSize.size(); b++) {
                                        if ((dataSize - readSize < connectionData.size())) {
                                            if (readSize >= 0) {
                                                final Map<String, String> nextRowTrace = connectionData.get(dataSize - readSize);
                                                if (nextRowTrace != null) {
                                                    String connectionId = null;
                                                    for (String key : nextRowTrace.keySet()) {
                                                        if (!key.contains("[MAIN]") && !key.contains(":")) {
                                                            connectionId = key;
                                                        }
                                                    }
                                                    final IConnection connection = findConnection(connectionId);
                                                    if (connection != null) {
                                                        Display.getDefault().syncExec(new Runnable() {

                                                            public void run() {
                                                                connection.setTraceData(nextRowTrace);
                                                            }
                                                        });
                                                    }
                                                }
                                            }
                                            readSize = readSize - 1;
                                        }
                                    }
                                    if (readSize == 0) {
                                        lastIsPrivious = false;
                                    }
                                    lastRow = true;
                                } else {
                                    pred.println("STATUS_OK");
                                }
                                setNextRow(false);
                                lastIsRow = true;

                            } else if (isPriviousRow()) {
                                lastIsPrivious = true;
                                if (lastRow || readSize == 0) {
                                    readSize = readSize + connectionSize.size();
                                    lastRow = false;
                                }

                                for (int b = 0; b < connectionSize.size(); b++) {
                                    readSize = readSize + 1;
                                    if (dataSize - readSize >= 0) {
                                        final Map<String, String> previousRowTrace = connectionData.get(dataSize - readSize);
                                        if (previousRowTrace != null) {
                                            String connectionId = null;
                                            for (String key : previousRowTrace.keySet()) {
                                                if (!key.contains("[MAIN]") && !key.contains(":")) {
                                                    connectionId = key;
                                                }
                                            }
                                            final IConnection connection = findConnection(connectionId);
                                            if (connection != null) {
                                                Display.getDefault().syncExec(new Runnable() {

                                                    public void run() {
                                                        connection.setTraceData(previousRowTrace);
                                                    }
                                                });
                                            }
                                            if (dataSize - readSize == 0) {
                                                firePropertyChange(PREVIOUS_ROW, true, false);
                                            }
                                        } else {
                                            readSize = dataSize;

                                        }
                                    }
                                }
                                pred.println("STATUS_WAITING");
                                setPreviousRow(false);
                            } else {
                                if (dataSize == connectionSize.size()) {
                                    firePropertyChange(PREVIOUS_ROW, true, false);
                                }
                                lastIsRow = false;
                                pred.println("STATUS_WAITING");
                            }
                            continue;
                        } else {
                            TraceData traceData = new TraceData(data);
                            final String idPart = traceData.getElementId();
                            String id = null;
                            boolean isMapTrace = false;
                            if (idPart != null) {
                                if (idPart.endsWith("[MAIN]")) {
                                    id = idPart.substring(0, idPart.indexOf("[MAIN]"));
                                    isMapTrace = true;
                                } else if (idPart.contains(":") && idPart.split(":").length == 2) {
                                    id = idPart.split(":")[0];
                                    isMapTrace = true;
                                } else {
                                    id = idPart;
                                }
                            }
                            final IConnection connection = findConnection(id);

                            if (connection != null) {
                                Map<String, String> traceMap = connAndTraces.get(connection);
                                if (traceMap == null) {
                                    traceMap = new HashMap<String, String>();
                                    connAndTraces.put(connection, traceMap);
                                }
                                traceMap.put(idPart, data);
                                if (isMapTrace) {
                                    continue;
                                }

                                connectionData.add(traceMap);
                                dataSize++;
                                if (connectionData.size() > (connectionSize.size() * 6) - 1) {
                                    for (int i = 0; i < connectionSize.size(); i++) {
                                        connectionData.remove(0);
                                        dataSize = dataSize - 1;
                                    }
                                }
                                Display.getDefault().syncExec(new Runnable() {

                                    public void run() {
                                        if (data != null) {
                                            connection.setTraceData(connAndTraces.get(connection));
                                            connAndTraces.clear();
                                        }
                                    }
                                });
                            }
                        }
                    }
                } catch (IOException e) {
                    // Do nothing : process is ended
                } finally {
                    try {
                        processSocket.close();
                    } catch (IOException ioe) {
                        // Do nothing
                    }
                }
            }
        }

        public void stopThread() {
            stopThread = true;
            synchronized (this) {
                notify();
            }
        }

        private IConnection findConnection(final String connectionId) {
            IConnection connection = null;
            for (Iterator<? extends INode> i = process.getGraphicalNodes().iterator(); connection == null && i.hasNext();) {
                INode psNode = i.next();
                for (IConnection connec : psNode.getOutgoingConnections()) {
                    if (connec.getName().equals(connectionId)) {
                        connection = connec;
                        if (!connectionSize.contains(connection)) {
                            connectionSize.add(connection);
                        }
                    }
                }
            }
            return connection;
        }
    }

    /**
     * Getter for watchAllowed.
     * 
     * @return the watchAllowed
     */
    public boolean isWatchAllowed() {
        return this.watchAllowed;
    }

    /**
     * Sets the watchAllowed.
     * 
     * @param watchAllowed the watchAllowed to set
     */
    public void setWatchAllowed(boolean watchAllowed) {
        if (this.watchAllowed != watchAllowed) {
            this.watchAllowed = watchAllowed;
            firePropertyChange(PROR_SWITCH_TIME, Boolean.valueOf(!watchAllowed), Boolean.valueOf(watchAllowed));
        }
    }

    public ITargetExecutionConfig getSelectedTargetExecutionConfig() {
        return this.selectedTargetExecutionConfig;
    }

    public void setSelectedTargetExecutionConfig(ITargetExecutionConfig selectedTargetExecutionConfiguration) {
        this.selectedTargetExecutionConfig = selectedTargetExecutionConfiguration;
    }

    /**
     * DOC amaumont Comment method "createProcessMonitor".
     * 
     * @param process
     * @return
     */
    protected IProcessMonitor createProcessMonitor(Process process) {
        return new ProcessMonitor(process);
    }

    public void addDebugResultToConsole(IProcessMessage message) {
        processMessageManager.addDebugResultToConsole(message);

    }

    public void setDebugProcess(org.eclipse.debug.core.model.IProcess debugProcess) {
        this.debugProcess = debugProcess;
    }

    public void setSaveBeforeRun(boolean saveBeforeRun) {
        this.saveBeforeRun = saveBeforeRun;
    }

    /**
     * Getter for saveBeforeRun.
     * 
     * @return the saveBeforeRun
     */
    public boolean isSaveBeforeRun() {
        return this.saveBeforeRun;
    }

    public boolean isClearBeforeExec() {
        return this.clearBeforeExec;
    }

    public void setClearBeforeExec(boolean clearBeforeExec) {
        this.clearBeforeExec = clearBeforeExec;
    }

    /**
     * Getter for isTracPause.
     * 
     * @return the isTracPause
     */
    public boolean isTracPause() {
        return this.isTracPause;
    }

    /**
     * Sets the isTracPause.
     * 
     * @param isTracPause the isTracPause to set
     */
    public void setTracPause(boolean isTracPause) {
        this.isTracPause = isTracPause;
    }

    public Process getSystemProcess() {
        return ps;
    }

    // /**
    // * DOC xzhang Comment method "startPerformanceMonitor".
    // *
    // * For bug 5430, modifyed by xzhang
    // */
    // public void startPerformanceMonitor() {
    // final IProcessor processor = getProcessor(process);
    // IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
    //
    // try {
    // progressService.run(false, true, new IRunnableWithProgress() {
    //
    // public void run(final IProgressMonitor monitor) {
    //
    // final EventLoopProgressMonitor progressMonitor = new EventLoopProgressMonitor(monitor);
    //
    //                    progressMonitor.beginTask(Messages.getString("ProcessComposite.buildTask"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
    // try {
    // testPort();
    // // findNewStatsPort();
    // final IContext context = getSelectedContext();
    // if (monitorPerf) {
    // clearThreads();
    // perfMonitor = new PerformanceMonitor();
    //                            new Thread(perfMonitor, "PerfMonitor_" + process.getLabel()).start(); //$NON-NLS-1$
    // perMonitorList.add(perfMonitor);
    // }
    // // findNewTracesPort();
    // if (monitorTrace) {
    // traceMonitor = new TraceMonitor();
    //                            new Thread(traceMonitor, "TraceMonitor_" + process.getLabel()).start(); //$NON-NLS-1$
    // }
    // } catch (Throwable e) {
    // ExceptionHandler.process(e);
    // addErrorMessage(e);
    // kill();
    // }
    // }
    // });
    // } catch (InvocationTargetException e1) {
    // addErrorMessage(e1);
    // } catch (InterruptedException e1) {
    // addErrorMessage(e1);
    // }
    // }

    private void clearThreads() {
        for (PerformanceMonitor perMonitor : perMonitorList) {
            perMonitor.stopThread();
            perMonitor = null;
        }
        perMonitorList.clear();
    }

    /**
     * DOC Administrator Comment method "setNextBreakPoint".
     */
    public void setNextBreakPoint(Boolean nextBreakpoint) {
        // TODO Auto-generated method stub
        this.nextBreakpoint = nextBreakpoint;
    }

    /**
     * DOC Administrator Comment method "isNextPoint".
     * 
     * @return
     */
    private boolean isNextPoint() {
        // TODO Auto-generated method stub
        return nextBreakpoint;
    }

    /**
     * DOC Administrator Comment method "setNextRow".
     * 
     * @param b
     */
    public void setNextRow(boolean b) {
        // TODO Auto-generated method stub
        this.nextRow = b;
    }

    public boolean isNextRow() {
        return this.nextRow;
    }

    /**
     * DOC Administrator Comment method "setPreviousRow".
     * 
     * @param b
     */
    public void setPreviousRow(boolean b) {
        // TODO Auto-generated method stub
        this.priviousRow = b;
    }

    public boolean isPriviousRow() {
        return this.priviousRow;
    }

    public void setLastIsRow(boolean lastIsRow) {
        this.lastIsRow = lastIsRow;
    }

    public boolean isSelectAllTrace() {
        return this.selectAllTrace;
    }

    public void setSelectAllTrace(boolean selectAllTrace) {
        this.selectAllTrace = selectAllTrace;
    }

}
