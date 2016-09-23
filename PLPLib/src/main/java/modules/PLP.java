package modules;

import conditions.Condition;
import effects.Effect;
import plpFields.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class PLP {

    protected String name;
    protected double version;
    protected String glueFile;

    protected List<PLPParameter> inputParams;
    protected List<PLPParameter> execParams;
    protected List<PLPParameter> outputParams;
    protected List<PLPParameter> unobservableParams;

    protected List<Variable> variables;
    protected List<Constant> constants;
    protected List<RequiredResource> requiredResources;

    protected List<Condition> preConditions;
    protected List<Condition> concurrencyConditions;

    protected List<ModuleRestriction> concurrentModules;
    protected boolean completelyMutex;

    protected List<Effect> sideEffects;

    List<ProgressMeasure> progressMeasures;


    public PLP(String name) {
        this.name = name;
        this.inputParams = new LinkedList<>();
        this.execParams = new LinkedList<>();
        this.outputParams = new LinkedList<>();
        this.unobservableParams = new LinkedList<>();

        this.variables = new LinkedList<>();
        this.constants = new LinkedList<>();
        this.requiredResources = new LinkedList<>();

        this.preConditions = new LinkedList<>();
        this.concurrencyConditions = new LinkedList<>();
        this.concurrentModules = new LinkedList<>();
        this.sideEffects = new LinkedList<>();
        this.progressMeasures = new LinkedList<>();
    }

    public String getGlueFile() {
        return glueFile;
    }

    public double getVersion() {
        return version;
    }

    public void setGlueFile(String glueFile) {
        this.glueFile = glueFile;
    }

    public void setVersion(double version) {
        this.version = version;
    }

    public String getBaseName() {
        return name;
    }

    public List<PLPParameter> getInputParams() {
        return inputParams;
    }

    public List<PLPParameter> getExecParams() {
        return execParams;
    }

    public List<PLPParameter> getOutputParams() {
        return outputParams;
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public List<Constant> getConstants() {
        return constants;
    }

    public List<String> getConstantsNames() {
        List<String> res = getConstants().stream().map(Constant::getName).collect(Collectors.toCollection(LinkedList::new));
        return res;
    }

    public List<RequiredResource> getRequiredResources() {
        return requiredResources;
    }

    public List<ModuleRestriction> getConcurrentModules() {
        return concurrentModules;
    }

    public List<ProgressMeasure> getProgressMeasures() {
        return progressMeasures;
    }

    public List<PLPParameter> getUnobservableParams() {
        return unobservableParams;
    }

    public List<Condition> getPreConditions() {
        return preConditions;
    }

    public List<Condition> getConcurrencyConditions() {
        return concurrencyConditions;
    }

    public List<Effect> getSideEffects() {
        return sideEffects;
    }

    public boolean isCompletelyMutex() {
        return completelyMutex;
    }

    public void addInputParam(PLPParameter p) {
        inputParams.add(p);
    }

    public void addExecParam(PLPParameter p) {
        execParams.add(p);
    }

    public void addOutputParam(PLPParameter p) {
        outputParams.add(p);
    }

    //TODO: delete the following three
    public void addInputParam(String p) {
        inputParams.add(new PLPParameter(p));
    }

    public void addExecParam(String p) {
        execParams.add(new PLPParameter(p));
    }

    public void addOutputParam(String p) {
        outputParams.add(new PLPParameter(p));
    }

    public void addUnobservableParam(PLPParameter p) { unobservableParams.add(p); }

    public void addVariable(Variable v) { variables.add(v); }

    public void addConstant(Constant c) { constants.add(c); }

    public void addRequiredResource(RequiredResource r) { requiredResources.add(r); }

    public void addPreCondition(Condition c) {
        preConditions.add(c);
    }

    public void addConcurrencyCondition(Condition c) {
        concurrencyConditions.add(c);
    }

    public void addSideEffect(Effect c) {
        sideEffects.add(c);
    }

    public void addModuleRestriction(ModuleRestriction mr) { concurrentModules.add(mr); }

    public void setCompletelyMutex() { completelyMutex = true; }

    public void addProgressMeasure(ProgressMeasure pm) { progressMeasures.add(pm); }

    @Override
    public String toString() {
        return "PLP: " +
                 this.getBaseName() + "\n" +
                " - Execution Parameters: " + Arrays.toString(execParams.toArray()) + "\n" +
                " - Input Parameters: " + Arrays.toString(inputParams.toArray()) + "\n" +
                " - Output Params: " + Arrays.toString(outputParams.toArray()) + "\n" +
                " - Unobservable Params: " + Arrays.toString(unobservableParams.toArray()) + "\n" +
                " - Variables: " + Arrays.toString(variables.toArray()) + "\n" +
                " - Constants: " + Arrays.toString(constants.toArray()) + "\n" +
                " - Required Resources: " + Arrays.toString(requiredResources.toArray()) + "\n" +
                " - Preconditions: " + Arrays.toString(preConditions.toArray()) + "\n" +
                " - Concurrency Conditions: " + Arrays.toString(concurrencyConditions.toArray()) + "\n" +
                " - Concurrent Modules: " + (completelyMutex ?
                        "Completeley Mutex" : Arrays.toString(concurrentModules.toArray())) + "\n" +
                " - Side Effects: " + Arrays.toString(sideEffects.toArray()) + "\n" +
                " - Progress Measures: " + Arrays.toString(progressMeasures.toArray());
    }
}