/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.eb.pride.cluster.reanalysis.model.processing;

import uk.ac.eb.pride.cluster.reanalysis.control.engine.ProcessingEngine;
import uk.ac.eb.pride.cluster.reanalysis.control.engine.callback.CallbackNotifier;
import uk.ac.eb.pride.cluster.reanalysis.model.exception.PladipusProcessingException;
import uk.ac.eb.pride.cluster.reanalysis.model.exception.ProcessStepInitialisationException;
import uk.ac.eb.pride.cluster.reanalysis.model.exception.UnspecifiedPladipusException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kenneth Verheggen
 */
public abstract class ProcessingStep implements ProcessingExecutable, AutoCloseable {

    /**
     * The Logger Instance
     */
    protected HashMap<String, String> parameters;
    /**
     * The fully defined class name of the processing step
     */

    protected String processingStepClassName;
    /**
     * The id of the current process / job
     */
    private int processingID = -1;
    /**
     * a notifier for the process proceedings
     */
    private CallbackNotifier callbackNotifier;
    /**
     * a boolean indicating whether the step has finished
     */
    protected boolean isDone = false;
    /**
     * The processing engine for subprocesses
     */
    private ProcessingEngine processingEngine;

    public ProcessingStep() {

    }

    public String getProcessingStepClassName() {
        return processingStepClassName;
    }

    public void setProcessingStepClassName(String processingStepClassName) {
        this.processingStepClassName = processingStepClassName;
    }

    public void setParameters(HashMap<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public HashMap<String, String> getParameters() {
        return parameters;
    }

    public int getProcessingID() {
        return processingID;
    }

    public void setProcessingID(int processingID) {
        this.processingID = processingID;
        this.callbackNotifier = new CallbackNotifier(processingID);
    }

    public CallbackNotifier getCallbackNotifier() {
        if (callbackNotifier == null) {
            callbackNotifier = new CallbackNotifier();
        }
        return callbackNotifier;
    }

    public void startProcess(File executable, List<String> constructArguments) {
        processingEngine = new ProcessingEngine();
        processingEngine.startProcess(executable, constructArguments, getCallbackNotifier());
    }

    public void startProcess(File executable, String[] constructArguments) {
        processingEngine = new ProcessingEngine();
        processingEngine.startProcess(executable, constructArguments, getCallbackNotifier());
    }


    @Override
    public void close() {
        isDone = true;
        //do other stuff that needs to be done to close this step nicely (close streams etc)
    }

    public boolean isIsDone() {
        return isDone;
    }

    private static String getCallerClass() throws ClassNotFoundException {
        StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
        String rawFQN = stElements[3].toString().split("\\(")[0];
        return (rawFQN.substring(0, rawFQN.lastIndexOf('.')));
    }

    public static void main(String[] args) {
        try {
            HashMap<String, String> parameters = new HashMap<>();
            String currentClassName = getCallerClass();
            ProcessingStep step = loadStepFromClassName(currentClassName);
            System.out.println(step.getDescription());
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("-")) {
                    if (i <= args.length - 1 && !args[i + 1].startsWith("-")) {
                        parameters.put(args[i].substring(1), args[i + 1]);
                    } else {
                        parameters.put(args[i], "");
                    }
                }
            }
            step.setParameters(parameters);
            step.doAction();
        } catch (UnspecifiedPladipusException | PladipusProcessingException | ClassNotFoundException | ProcessStepInitialisationException | IOException ex) {
            Logger.getLogger(ProcessingStep.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static ProcessingStep loadStepFromClassName(String className) throws ProcessStepInitialisationException, IOException {
        try {
            Class<?> clazz = Class.forName(className);
            return (ProcessingStep) clazz.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException ex) {
            throw new ProcessStepInitialisationException(ex.getMessage());
        }
    }

}
