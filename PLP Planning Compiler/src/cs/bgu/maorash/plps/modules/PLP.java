package cs.bgu.maorash.plps.modules;

import cs.bgu.maorash.plps.etc.Condition;
import cs.bgu.maorash.plps.plpFields.PLPParameter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PLP {

    protected String name;

    protected List<PLPParameter> inputParams;
    protected List<PLPParameter> inputExecParams;
    protected List<PLPParameter> outputParams;

    //List<String> variables;
    //List<RequiredResource> requiredResources;

    protected List<Condition> preConditions;
    protected List<Condition> concurrencyConditions;

    protected List<Condition> sideEffects;

    //List<String> concurrentModules;


    public PLP(String baseName) {
        this.name = baseName;
        this.inputParams = new LinkedList<>();
        this.inputExecParams = new LinkedList<>();
        this.outputParams = new LinkedList<>();

        this.preConditions = new LinkedList<>();
        this.concurrencyConditions = new LinkedList<>();
        this.sideEffects = new LinkedList<>();
    }

    public String getName() {
        return name;
    }

    public String getBaseName() {
        return name;
    }

    public List<PLPParameter> getInputParams() {
        return inputParams;
    }

    public List<PLPParameter> getInputExecParams() {
        return inputExecParams;
    }

    public List<PLPParameter> getOutputParams() {
        return outputParams;
    }

    public List<Condition> getPreConditions() {
        return preConditions;
    }

    public List<Condition> getConcurrencyConditions() {
        return concurrencyConditions;
    }

    public List<Condition> getSideEffects() {
        return sideEffects;
    }

    public void addInputParam(PLPParameter s) {
        inputParams.add(s);
    }
    public void addInputParam(String s) {
        inputParams.add(new PLPParameter(s));
    }

    public void addInputExecParam(PLPParameter s) {
        inputExecParams.add(s);
    }
    public void addInputExecParam(String s) {
        inputExecParams.add(new PLPParameter(s));
    }

    public void addOutputParam(PLPParameter s) {
        outputParams.add(s);
    }
    public void addOutputParam(String s) {
        outputParams.add(new PLPParameter(s));
    }

    public void addPreCondition(Condition c) {
        preConditions.add(c);
    }

    public void addConcurrencyCondition(Condition c) {
        concurrencyConditions.add(c);
    }

    public void addSideEffect(Condition c) {
        sideEffects.add(c);
    }

    @Override
    public String toString() {
        return "PLP: " +
                 this.getName() + "\n" +
                " - Execution Parameterss: " + Arrays.toString(inputExecParams.toArray()) + "\n" +
                " - Input Parameters: " + Arrays.toString(inputParams.toArray()) + "\n" +
                " - outputParams: " + Arrays.toString(outputParams.toArray()) + "\n" +
                " - preConditions: " + Arrays.toString(preConditions.toArray()) + "\n" +
                " - concurrencyConditions: " + Arrays.toString(concurrencyConditions.toArray()) + "\n" +
                " - sideEffects: " + Arrays.toString(sideEffects.toArray());
    }
}
