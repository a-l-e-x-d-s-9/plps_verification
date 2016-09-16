package loader;


import conditions.*;
import distributions.*;
import effects.*;
import plpEtc.ConfidenceInterval;
import plpEtc.FieldType;
import plpEtc.Predicate;
import plpEtc.Range;
import modules.*;
import org.w3c.dom.*;
import plpFields.*;

import javax.xml.parsers.*;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class PLPLoader {

    private static List<AchievePLP> achievePLPs;
    private static List<ObservePLP> observePLPs;
    private static List<DetectPLP> detectPLPs;
    private static List<MaintainPLP> maintainPLPs;

    public static List<AchievePLP> getAchievePLPs() {
        return achievePLPs;
    }

    public static List<ObservePLP> getObservePLPs() {
        return observePLPs;
    }

    public static List<DetectPLP> getDetectPLPs() {
        return detectPLPs;
    }

    public static List<MaintainPLP> getMaintainPLPs() {
        return maintainPLPs;
    }

    public static void main(String[] args) {
        /*PLPLoader.loadFromDirectory(args[0]);
        for (MaintainPLP oplp : PLPLoader.getMaintainPLPs())
            System.out.println(oplp.toString());

        for (AchievePLP aPLP : PLPLoader.getAchievePLPs())
            System.out.println(aPLP.toString());
        for (PLPParameter execParam : PLPLoader.getAchievePLPs().get(0).getExecParams()) {
            System.out.println(execParam.toString());
            System.out.println(execParam.simpleString());
        }*/
    }

    public static void loadFromDirectory(String dir) {
        achievePLPs = new LinkedList<>();
        observePLPs = new LinkedList<>();
        detectPLPs = new LinkedList<>();
        maintainPLPs = new LinkedList<>();

        // Complete path with with '/' or '\' according to what to OS uses
        if (!dir.endsWith("\\") && dir.contains("\\")) { dir = dir.concat("\\"); }
        else if (!dir.endsWith("/")) { dir = dir.concat("/"); }

        File folder = new File(dir);
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("Path " + dir + " is not a legal directory");
        }
        File[] files = folder.listFiles();

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String tempName = file.getName();
            if (file.isFile() &&
                    tempName.substring(tempName.lastIndexOf(".") + 1).equalsIgnoreCase("xml")) {
                loadFromFile(dir.concat(tempName));
            }
        }
    }

    private static void loadFromFile(String fileName) {
        try {
            File plpFile = new File(fileName);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(plpFile);

            doc.getDocumentElement().normalize();

            Element rootElement = doc.getDocumentElement();
            String PLPname = rootElement.getAttribute("name");

            PLP plp;
            String nodeNameWithoutNS = rootElement.getNodeName().substring(rootElement.getNodeName().indexOf(':')+1);
            switch (nodeNameWithoutNS) {
                case "achieve_plp":
                    plp = new AchievePLP(PLPname);
                    achievePLPs.add((AchievePLP) plp);
                    LoadAchieveFields(rootElement, (AchievePLP) plp);
                    break;
                case "maintain_plp":
                    plp = new MaintainPLP(PLPname);
                    maintainPLPs.add((MaintainPLP) plp);
                    LoadMaintainFields(rootElement, (MaintainPLP) plp);
                    break;
                case "observe_plp":
                    plp = new ObservePLP(PLPname);
                    observePLPs.add((ObservePLP) plp);
                    LoadObserveFields(rootElement, (ObservePLP) plp);
                    break;
                case "detect_plp":
                    plp = new DetectPLP(PLPname);
                    detectPLPs.add((DetectPLP) plp);
                    LoadDetectFields(rootElement, (DetectPLP) plp);
                    break;
                default:
                    return;
            }

            String version = rootElement.getAttribute("version");
            String glueFile = rootElement.getAttribute("glue_file_location");

            if (version != null) plp.setVersion(Double.parseDouble(version));
            if (glueFile != null) plp.setGlueFile(glueFile);

            Node currentNode = rootElement.getElementsByTagName("parameters").item(0);
            if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
                LoadParameters((Element) currentNode, plp);
            }

            currentNode = rootElement.getElementsByTagName("variables").item(0);
            if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
                LoadVariables((Element) currentNode, plp);
            }

            currentNode = rootElement.getElementsByTagName("constants").item(0);
            if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
                LoadConstants((Element) currentNode, plp);
            }

            currentNode = rootElement.getElementsByTagName("required_resources").item(0);
            if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
                LoadRequiredResources((Element) currentNode, plp);
            }

            currentNode = rootElement.getElementsByTagName("preconditions").item(0);
            if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
                LoadPreconditions((Element) currentNode, plp);
            }

            currentNode = rootElement.getElementsByTagName("concurrency_conditions").item(0);
            if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
                LoadConcurrencyConditions((Element) currentNode, plp);
            }

            currentNode = rootElement.getElementsByTagName("concurrent_modules").item(0);
            if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
                LoadConcurrentModules((Element) currentNode, plp);
            }

            currentNode = rootElement.getElementsByTagName("side_effects").item(0);
            if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
                LoadSideEffects((Element) currentNode, plp);
            }

            currentNode = rootElement.getElementsByTagName("progress_measures").item(0);
            if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
                LoadProgressMeasures((Element) currentNode, plp);
            }


            // Validate Observe PLPs
            for (ObservePLP oplp : getObservePLPs()) {
                // Check to see if there is an observe PLP that observes a condition but doesn't have a result param
                if (!oplp.isGoalParameter() && oplp.getResultParameter() == null) {
                    throw new RuntimeException("Observe PLP: " + oplp.getBaseName() + " is missing the result parameter from the parameter list");
                }
                // Check to see if there is an observe PLP that observes a parameter that isn't listed as an output param
                if (oplp.isGoalParameter()) {
                    boolean foundParamMatch = false;
                    for (PLPParameter outputParam : oplp.getOutputParams()) {
                        if (outputParam.toString().equals(oplp.getGoal().toString()))
                            foundParamMatch = true;
                    }
                    if (!foundParamMatch)
                        throw new RuntimeException("Observe PLP: " + oplp.getBaseName() + " has a parameter observation goal that isn't listed as an output parameter");
                }
            }
            // Validate Detect PLPs
            for (DetectPLP dplp : getDetectPLPs()) {
                // Check to see if there is a detect PLP that doesn't have a result param
                if (dplp.getResultParameter() == null) {
                    throw new RuntimeException("Detect PLP: " + dplp.getBaseName() + " is missing the result parameter from the parameter list");
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void LoadDetectFields(Element rootElement, DetectPLP plp) {
        Node currentNode = rootElement.getElementsByTagName("detection_goal").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            plp.setGoal(parseConditions((Element) currentNode).get(0));
            plp.setResultParameterName(((Element) currentNode).getAttribute("result_parameter_name"));
        }
        currentNode = rootElement.getElementsByTagName("success_prob_given_condition").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<ConditionalProb> condProbList = parseProb((Element) currentNode);
            condProbList.forEach(plp::addSuccessProbGivenCond);
        }
        currentNode = rootElement.getElementsByTagName("failure_termination_condition").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<Condition> condList = parseConditions((Element) currentNode);
            plp.setFailTerminationCond(condList.get(0));
        }
    }

    private static void LoadMaintainFields(Element rootElement, MaintainPLP plp) {
        Node currentNode = rootElement.getElementsByTagName("maintained_condition").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            plp.setMaintainedCondition(parseConditions((Element) currentNode).get(0));

            if (((Element) currentNode).getElementsByTagName("initially_true").getLength() > 0)
                plp.setInitiallyTrue(true);
            else {
                plp.setInitiallyTrue(false);
                NodeList timeUntilTrueNL = ((Element) currentNode).getElementsByTagName("time_until_true");
                if (timeUntilTrueNL.getLength() > 0) {
                    Node timeUntilTrueNode =  timeUntilTrueNL.item(0);
                    plp.setTimeUntilTrue(parseRuntime((Element) timeUntilTrueNode));
                }
            }
            //plp.setInitiallyTrue(Boolean.parseBoolean(((Element) currentNode).getAttribute("initially_true")));
        }
        currentNode = rootElement.getElementsByTagName("success_termination_condition").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            plp.setSuccessTerminationCondition(parseConditions((Element) currentNode).get(0));
        }
        currentNode = rootElement.getElementsByTagName("failure_termination_conditions").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<Condition> condList = parseConditions((Element) currentNode);
            condList.forEach(plp::addFailureTerminationConditions);
        }
        currentNode = rootElement.getElementsByTagName("success_probability").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<ConditionalProb> condProbList = parseProb((Element) currentNode);
            condProbList.forEach(plp::addSuccessProb);
        }
        currentNode = rootElement.getElementsByTagName("failure_modes").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            LoadFailureModes((Element) currentNode, plp);
        }
        currentNode = rootElement.getElementsByTagName("general_failure_probability").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<ConditionalProb> condProbList = parseProb((Element) currentNode);
            condProbList.forEach(plp::addGeneralFailureProb);
        }
        currentNode = rootElement.getElementsByTagName("runtime_given_success").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<ConditionalDist> condDistList = parseRuntime((Element) currentNode);
            condDistList.forEach(plp::addSuccessRuntime);
        }
        currentNode = rootElement.getElementsByTagName("runtime_given_failure").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<ConditionalDist> condDistList = parseRuntime((Element) currentNode);
            condDistList.forEach(plp::addFailureRuntime);
        }
    }

    private static void LoadObserveFields(Element rootElement, ObservePLP plp) {
        boolean goalCondition = rootElement.getElementsByTagName("observation_goal_condition").getLength() > 0;
        Node currentNode;
        if (goalCondition) {
            currentNode = rootElement.getElementsByTagName("observation_goal_condition").item(0);
            if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
                plp.setGoal(parseConditions((Element) currentNode).get(0));
                plp.setResultParameterName(((Element) currentNode).getAttribute("result_parameter_name"));
            }
            currentNode = rootElement.getElementsByTagName("correct_condition_observation_probability").item(0);
            if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
                List<ConditionalProb> condProbList = parseProb((Element) currentNode);
                condProbList.forEach(plp::addCorrectObservationProb);
            }
        }
        else {
            currentNode = rootElement.getElementsByTagName("observation_goal_parameter").item(0);
            if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
                LoadObservationGoalParameter(plp, (Element) currentNode);
            }
            currentNode = rootElement.getElementsByTagName("correct_param_observation_probability").item(0);
            if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
                LoadCorrectObservationContinuous(plp, (Element) currentNode);
            }
        }
        currentNode = rootElement.getElementsByTagName("failure_to_observe_probability").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<ConditionalProb> condProbList = parseProb((Element) currentNode);
            condProbList.forEach(plp::addFailureToObserveProb);
        }
        currentNode = rootElement.getElementsByTagName("failure_termination_condition").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<Condition> condList = parseConditions((Element) currentNode);
            plp.setFailTerminationCond(condList.get(0));
        }
        currentNode = rootElement.getElementsByTagName("runtime_given_success").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<ConditionalDist> condDistList = parseRuntime((Element) currentNode);
            condDistList.forEach(plp::addSuccessRuntime);
        }
        currentNode = rootElement.getElementsByTagName("runtime_given_failure").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<ConditionalDist> condDistList = parseRuntime((Element) currentNode);
            condDistList.forEach(plp::addFailureRuntime);
        }
    }

    private static void LoadCorrectObservationContinuous(ObservePLP plp, Element currentNode) {
        NodeList probs = currentNode.getElementsByTagName("probability_given_observed_value");
        if (probs.getLength() > 0) {
            List<ConditionalProb> condProbList = parseProb((Element) probs.item(0));
            condProbList.forEach(plp::addCorrectObservationProb);
        }
        NodeList intervals = currentNode.getElementsByTagName("confidence_interval");
        if (intervals.getLength() > 0) {
            Element lowerBoundElement = (Element) ((Element) intervals.item(0)).getElementsByTagName("lower_bound").item(0);
            Element upperBoundElement = (Element) ((Element) intervals.item(0)).getElementsByTagName("upper_bound").item(0);
            Range range = new Range(lowerBoundElement.getAttribute("value"),
                    Boolean.parseBoolean(lowerBoundElement.getAttribute("inclusive")),
                    upperBoundElement.getAttribute("value"),
                    Boolean.parseBoolean(upperBoundElement.getAttribute("inclusive")));

            Element conLevelElement = (Element) currentNode.getElementsByTagName("confidence_level").item(0);

            plp.setCorrectObservationConfidence(new ConfidenceInterval(range,
                            Double.parseDouble(conLevelElement.getAttribute("value"))));
        }
    }

    private static void LoadObservationGoalParameter(ObservePLP plp, Element currentNode) {
        Element paramElement = (Element) currentNode.getElementsByTagName("param").item(0);
        PLPParameter plpParam = new PLPParameter(paramElement.getAttribute("name"));

        NodeList paramFields = paramElement.getElementsByTagName("field");
        for (int j = 0; j < paramFields.getLength(); j++) {
            if (paramFields.item(j).getNodeType() == Node.ELEMENT_NODE) {
                plpParam.addParamFieldValue(((Element) paramFields.item(j)).getAttribute("value"));
            }
        }
        plp.setGoal(plpParam);
    }

    private static void LoadAchieveFields(Element rootElement, AchievePLP plp) {
        Node currentNode = rootElement.getElementsByTagName("achievement_goal").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<Condition> condList = parseConditions((Element) currentNode);
            plp.setGoal(condList.get(0));
        }
        currentNode = rootElement.getElementsByTagName("success_termination_condition").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<Condition> condList = parseConditions((Element) currentNode);
            plp.setSuccessTerminationCond(condList.get(0));
        }
        currentNode = rootElement.getElementsByTagName("success_probability").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<ConditionalProb> condProbList = parseProb((Element) currentNode);
            condProbList.forEach(plp::addSuccessProb);
        }
        currentNode = rootElement.getElementsByTagName("failure_modes").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            LoadFailureModes((Element) currentNode, plp);
        }
        currentNode = rootElement.getElementsByTagName("general_failure_probability").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<ConditionalProb> condProbList = parseProb((Element) currentNode);
            condProbList.forEach(plp::addGeneralFailureProb);
        }
        currentNode = rootElement.getElementsByTagName("failure_termination_condition").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<Condition> condList = parseConditions((Element) currentNode);
            plp.setFailTerminationCond(condList.get(0));
        }
        currentNode = rootElement.getElementsByTagName("runtime_given_success").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<ConditionalDist> condDistList = parseRuntime((Element) currentNode);
            condDistList.forEach(plp::addSuccessRuntime);
        }
        currentNode = rootElement.getElementsByTagName("runtime_given_failure").item(0);
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            List<ConditionalDist> condDistList = parseRuntime((Element) currentNode);
            condDistList.forEach(plp::addFailureRuntime);
        }
    }

    private static List<ConditionalDist> parseRuntime(Element rootNode) {
        List<ConditionalDist> result = new LinkedList<>();
        NodeList childs = rootNode.getChildNodes();

        for (int j=0;j<childs.getLength();j++) {
            if (childs.item(j).getNodeType() == Node.ELEMENT_NODE) {
                if (childs.item(j).getNodeName().equals("conditional_distribution")) {
                    Element condDistElement = (Element) childs.item(j);
                    List<Condition> innerConditions = parseConditions(condDistElement);
                    Element distElement = (Element) condDistElement.getElementsByTagName("distribution").item(0);
                    Distribution dist = parseDistribution(distElement);
                    result.add(new ConditionalDist(dist, innerConditions.get(0)));
                }
                else if (childs.item(j).getNodeName().equals("distribution")) {
                    Distribution dist = parseDistribution((Element) childs.item(j));
                    result.add(new ConditionalDist(dist, null));
                }
            }
        }
        return result;
    }

    private static Distribution parseDistribution(Element distElement) {
        NodeList childNodes = distElement.getElementsByTagName("uniform");
        if (childNodes.getLength() > 0) {
            Element uniformElement = (Element) childNodes.item(0);
            String lower_bound = ((Element) uniformElement.getElementsByTagName("lower_bound").item(0)).getAttribute("value");
            String upper_bound = ((Element) uniformElement.getElementsByTagName("upper_bound").item(0)).getAttribute("value");
            return new UniformDistribution(lower_bound,upper_bound);
        }
        childNodes = distElement.getElementsByTagName("normal");
        if (childNodes.getLength() > 0) {
            Element normalElement = (Element) childNodes.item(0);
            String mean = ((Element) normalElement.getElementsByTagName("mean").item(0)).getAttribute("value");
            String variance = ((Element) normalElement.getElementsByTagName("variance").item(0)).getAttribute("value");
            return new NormalDistribution(mean,variance);
        }
        childNodes = distElement.getElementsByTagName("gamma");
        if (childNodes.getLength() > 0) {
            Element gammaElement = (Element) childNodes.item(0);
            String k = ((Element) gammaElement.getElementsByTagName("k-shape").item(0)).getAttribute("value");
            String alpha = ((Element) gammaElement.getElementsByTagName("alpha-shape").item(0)).getAttribute("value");
            return new GammaDistribution(k,alpha);
        }
        childNodes = distElement.getElementsByTagName("exp");
        if (childNodes.getLength() > 0) {
            Element expElement = (Element) childNodes.item(0);
            String lambda = ((Element) expElement.getElementsByTagName("lambda-rate").item(0)).getAttribute("value");
            return new ExpDistribution(lambda);
        }
        childNodes = distElement.getElementsByTagName("other_dist");
        if (childNodes.getLength() > 0) {
            Element unkownElement = (Element) childNodes.item(0);
            return new UnkownDistribution(unkownElement.getAttribute("description"));
        }
        throw new UnsupportedOperationException("Can't parse a given distribution " + distElement.toString());
    }

    private static void LoadFailureModes(Element failureModesElement, PLP plp) {
        NodeList failureModes = failureModesElement.getElementsByTagName("failure_mode");
        for (int i=0 ; i<failureModes.getLength() ; i++) {
            if (failureModes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element failureModeElement = (Element) failureModes.item(i);
                List<Condition> innerCondition = parseConditions(failureModeElement);
                List<ConditionalProb> innerCondProb = parseProb(failureModeElement);
                FailureMode fm = new FailureMode(innerCondition.get(0));
                for (ConditionalProb cp : innerCondProb) {
                    fm.addProb(cp);
                }
                if (plp.getClass().isAssignableFrom(AchievePLP.class))
                    ((AchievePLP) plp).addFailureMode(fm);
                else if (plp.getClass().isAssignableFrom(MaintainPLP.class))
                    ((MaintainPLP) plp).addFailureMode(fm);
            }
        }
    }

    private static List<ConditionalProb> parseProb(Element rootNode) {
        List<ConditionalProb> result = new LinkedList<>();
        NodeList childs = rootNode.getChildNodes();
        for (int j=0;j<childs.getLength();j++) {
            if (childs.item(j).getNodeType() == Node.ELEMENT_NODE) {
                if (childs.item(j).getNodeName().equals("conditional_probability")) {
                    Element condProbElement = (Element) childs.item(j);
                    List<Condition> innerConditions = parseConditions(condProbElement);
                    Element probElement = (Element) condProbElement.getElementsByTagName("probability").item(0);
                    String prob = probElement.getAttribute("value");
                    result.add(new ConditionalProb(prob, innerConditions.get(0)));
                }
                else if (childs.item(j).getNodeName().equals("probability")) {
                    String prob = ((Element) childs.item(j)).getAttribute("value");
                    result.add(new ConditionalProb(prob, null));
                }
            }
        }
        return result;
    }

    private static void LoadProgressMeasures(Element measuresElement, PLP plp) {
        NodeList measures = measuresElement.getElementsByTagName("progress_measure");
        for (int i=0;i<measures.getLength();i++) {
            if (measures.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element pmElement = (Element) measures.item(i);
                String frequency = pmElement.getAttribute("frequency");
                List<Condition> innerCond = parseConditions(pmElement);
                ProgressMeasure pm = new ProgressMeasure(Double.parseDouble(frequency), innerCond.get(0));

                plp.addProgressMeasure(pm);
            }
        }
    }

    private static void LoadSideEffects(Element seNode, PLP plp) {
        List<Effect> effects = parseEffects(seNode);
        effects.forEach(plp::addSideEffect);
    }

    private static List<Effect> parseEffects(Element effectsNode) {
        List<Effect> result = new LinkedList<>();
        NodeList childNodes = effectsNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Effect c = null;
                switch (childNodes.item(i).getNodeName()) {
                    case "conditional_effect":
                        c = parseConditionalEffect((Element) childNodes.item(i));
                        break;
                    case "forall_effect":
                        c = parseForallEffect((Element) childNodes.item(i));
                        break;
                    case "not_effect":
                        c = parseNotEffect((Element) childNodes.item(i));
                        break;
                    case "predicate_effect":
                        c = parsePredicate((Element) childNodes.item(i));
                        break;
                    case "assignment_effect":
                        c = parseAssignmentEffect((Element) childNodes.item(i));
                        break;
                }
                if (c != null)
                    result.add(c);
            }
        }
        return result;
    }

    private static Effect parseAssignmentEffect(Element assignmentElement) {
        Element paramElement = (Element) assignmentElement.getElementsByTagName("param").item(0);
        Element expressionElement = (Element) assignmentElement.getElementsByTagName("expression").item(0);
        PLPParameter plpParam = new PLPParameter(paramElement.getAttribute("name"));

        NodeList paramFields = paramElement.getElementsByTagName("field");
        for (int j = 0; j < paramFields.getLength(); j++) {
            if (paramFields.item(j).getNodeType() == Node.ELEMENT_NODE) {
                plpParam.addParamFieldValue(((Element) paramFields.item(j)).getAttribute("value"));
            }
        }
        AssignmentEffect ae = new AssignmentEffect(plpParam, expressionElement.getAttribute("value"));
        ae.setDescription(assignmentElement.getAttribute("key_description"));
        return ae;
    }

    private static Effect parseNotEffect(Element nEffectElement) {
        Effect predEff = parsePredicate((Element) nEffectElement.getElementsByTagName("predicate_effect").item(0));
        return new NotEffect((Predicate) predEff);
    }

    private static Effect parseForallEffect(Element feEffectElement) {
        List<Effect> innerEffects = parseEffects(feEffectElement);
        ForAllEffect faEff = new ForAllEffect(innerEffects.get(0));

        NodeList children = feEffectElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) children.item(i);
                if (childElement.getNodeName().equals("param")) {
                    faEff.addParam(childElement.getAttribute("name"));
                }
            }
        }
        return faEff;
    }

    private static Effect parseConditionalEffect(Element condEffectElement) {
        List<Effect> innerEffects = parseEffects(condEffectElement);
        List<Condition> innerConditions = parseConditions(condEffectElement);
        return new ConditionalEffect(innerConditions.get(0),innerEffects.get(0));
    }

    private static void LoadConcurrentModules(Element conModulesElement, PLP plp) {
        NodeList childs = conModulesElement.getElementsByTagName("complete_mutex");
        if (childs.getLength() > 0)
            plp.setCompletelyMutex();
        else {
            childs = conModulesElement.getElementsByTagName("module");
            for (int i = 0; i < childs.getLength(); i++) {
                if (childs.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    ModuleRestriction.ConcurrencyType type;
                    switch (((Element) childs.item(i)).getAttribute("concurrency_type")) {
                        case "mutex":
                            type = ModuleRestriction.ConcurrencyType.Mutex;
                            break;
                        default:
                            type = ModuleRestriction.ConcurrencyType.Parallel;
                    }
                    ModuleRestriction rest = new ModuleRestriction(((Element) childs.item(i)).getAttribute("name"), type);
                    plp.addModuleRestriction(rest);
                }
            }
        }
    }

    private static void LoadConcurrencyConditions(Element concurrencyConditionsNode, PLP plp) {
        List<Condition> conditions = parseConditions(concurrencyConditionsNode);
        conditions.forEach(plp::addConcurrencyCondition);
    }

    private static void LoadPreconditions(Element preConditionsNode, PLP plp) {
        List<Condition> conditions = parseConditions(preConditionsNode);
        conditions.forEach(plp::addPreCondition);
    }

    private static List<Condition> parseConditions(Element conditionsNode) {
        List<Condition> result = new LinkedList<>();
        NodeList childNodes = conditionsNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Condition c = null;
                switch (childNodes.item(i).getNodeName()) {
                    case "predicate_condition":
                        c = parsePredicate((Element) childNodes.item(i));
                        break;
                    case "formula_condition":
                        c = parseFormula((Element) childNodes.item(i));
                        break;
                    case "not_condition":
                        c = parseNotCondition((Element) childNodes.item(i));
                        break;
                    case "forall_condition":
                        c = parseQuantifiedCondition((Element) childNodes.item(i), QuantifiedCondition.Quantifier.FORALL);
                        break;
                    case "exists_condition":
                        c = parseQuantifiedCondition((Element) childNodes.item(i), QuantifiedCondition.Quantifier.EXISTS);
                        break;
                    case "AND":
                        c = parseBitwiseCondition((Element) childNodes.item(i), BitwiseOperation.Operation.AND);
                        break;
                    case "OR":
                        c = parseBitwiseCondition((Element) childNodes.item(i), BitwiseOperation.Operation.OR);
                        break;
                }
                if (c != null)
                    result.add(c);
            }
        }
        return result;
    }

    private static Condition parseBitwiseCondition(Element bwElement, BitwiseOperation.Operation op) {
        List<Condition> innerConditions = parseConditions(bwElement);
        BitwiseOperation bwCond = new BitwiseOperation(op);
        innerConditions.forEach(bwCond::addCondition);
        return bwCond;
    }

    private static Condition parseQuantifiedCondition(Element faElement, QuantifiedCondition.Quantifier q) {
        List<Condition> innerConditions = parseConditions(faElement);
        QuantifiedCondition qCond = new QuantifiedCondition(innerConditions.get(0), q);

        NodeList children = faElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) children.item(i);
                if (childElement.getNodeName().equals("param")) {
                    qCond.addParam(childElement.getAttribute("name"));
                }
            }
        }
        return qCond;
    }

    private static Condition parseNotCondition(Element notElement) {
        List<Condition> innerConditions = parseConditions(notElement);
        return new NotCondition(innerConditions.get(0));
    }

    private static Condition parseFormula(Element formulaElement) { // TODO: check end cases
        NodeList expressions = formulaElement.getElementsByTagName("expression");
        NodeList ranges = formulaElement.getElementsByTagName("inside_range");
        String leftExp = ((Element) expressions.item(0)).getAttribute("value");
        Formula f;
        if (ranges.getLength() > 0) {
            Range range = parseRangesList((Element) ranges.item(0),"range").get(0);
            f = new Formula(leftExp,range);
        }
        else {
            String rightExp = ((Element) expressions.item(1)).getAttribute("value");
            NodeList operators = formulaElement.getElementsByTagName("operator");
            String operator = ((Element) operators.item(0)).getAttribute("type");
            operator = operator.replace("less","<");
            operator = operator.replace("greater",">");
            operator = operator.replace("less_equal","<=");
            operator = operator.replace("greater_equal",">=");
            f = new Formula(leftExp,rightExp,operator);
        }
        f.setDescription(formulaElement.getAttribute("key_description"));
        return f;
    }

    private static Predicate parsePredicate(Element predElement) {
        Predicate result = new Predicate(predElement.getAttribute("name"));
        NodeList fields = predElement.getElementsByTagName("field");
        for (int j = 0; j < fields.getLength(); j++) {
            if (fields.item(j).getNodeType() == Node.ELEMENT_NODE) {
                result.addValue(((Element) fields.item(j)).getAttribute("value"));
            }
        }
        return result;
    }

    private static void LoadRequiredResources(Element resourcesNode, PLP plp) {
        NodeList resources = resourcesNode.getElementsByTagName("resource");
        for (int i = 0; i < resources.getLength(); i++) {
            if (resources.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element currResource = (Element) resources.item(i);
                Element currStatus = (Element) currResource.getElementsByTagName("status").item(0);
                RequiredResource.RequirementStatus status;
                switch (currStatus.getAttribute("type")) {
                    case "frequency":
                        status = RequiredResource.RequirementStatus.Frequency;
                        break;
                    default:
                        status = RequiredResource.RequirementStatus.Exclusive;
                        break;
                }

                RequiredResource plpRR = new RequiredResource(currResource.getAttribute("name"),status);

                try {
                    if (currStatus.hasAttribute("frequency"))
                        plpRR.setFrequency(Double.parseDouble(currStatus.getAttribute("frequency")));
                    if (currStatus.hasAttribute("duration"))
                        plpRR.setDuration(Double.parseDouble(currStatus.getAttribute("duration")));
                    if (currStatus.hasAttribute("quantity"))
                        plpRR.setQuantity(Double.parseDouble(currStatus.getAttribute("quantity")));
                } catch (Exception e) {
                    throw new NumberFormatException("Required Resource " + plpRR.getName() +
                            " has frequency/duration/quantity that is not a real number");
                }
                plp.addRequiredResource(plpRR);
            }
        }
    }

    private static void LoadConstants(Element constantsNode, PLP plp) {
        NodeList constants = constantsNode.getElementsByTagName("constant");
        for (int i = 0; i < constants.getLength(); i++) {
            if (constants.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element currConst = (Element) constants.item(i);
                Constant plpConstant = new Constant(currConst.getAttribute("name"));
                String stringType = currConst.getAttribute("type");
                FieldType type = getFieldTypeFromString(stringType);
                plpConstant.setType(type);

                String value = currConst.getAttribute("value");
                if (!value.equals(""))
                    plpConstant.setValue(value);

                plp.addConstant(plpConstant);
            }
        }
    }

    private static void LoadVariables(Element variablesNode, PLP plp) {
        NodeList varList = variablesNode.getElementsByTagName("var");
        for (int i = 0; i < varList.getLength(); i++) {
            if (varList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element currVar = (Element) varList.item(i);
                String stringType = currVar.getAttribute("type");
                FieldType type = getFieldTypeFromString(stringType);

                Variable plpVar = new Variable(currVar.getAttribute("name"),type);

                List<Range> ranges = parseRangesList(currVar,"possible_range");
                for (Range range : ranges) {
                    plpVar.addRange(range);
                }

                NodeList possible_values = currVar.getElementsByTagName("possible_value");
                for (int j = 0; j < possible_values.getLength(); j++) {
                    if (possible_values.item(j).getNodeType() == Node.ELEMENT_NODE) {
                        plpVar.addPossibleValue(((Element) possible_values.item(j)).getAttribute("value"));
                    }
                }

                plp.addVariable(plpVar);
            }
        }
    }

    private static List<Range> parseRangesList(Element rootElement, String rangeTagName) {
        List<Range> ranges = new LinkedList<>();
        NodeList possible_ranges = rootElement.getElementsByTagName(rangeTagName);
        for (int j = 0; j < possible_ranges.getLength(); j++) {
            if (possible_ranges.item(j).getNodeType() == Node.ELEMENT_NODE) {
                Element re = (Element) possible_ranges.item(j);
                Range range = new Range(re.getAttribute("min_value"),
                                            Boolean.parseBoolean(re.getAttribute("min_inclusive")),
                                            re.getAttribute("max_value"),
                                            Boolean.parseBoolean(re.getAttribute("max_inclusive")));
                ranges.add(range);
            }
        }
        return ranges;
    }

    private static FieldType getFieldTypeFromString(String stringType) {
        FieldType type;
        switch (stringType) {
            case "boolean":
                type = FieldType.Boolean;
                break;
            case "real":
                type = FieldType.Real;
                break;
            case "integer":
                type = FieldType.Integer;
                break;
            case "string":
                type = FieldType.String;
                break;
            default:
                type = FieldType.OtherType;
        }
        return type;
    }

    private static void LoadParameters(Element parametersNode, PLP plp) {
        String[] parameterTypes = {"execution_parameters", "input_parameters"
                                        ,"output_parameters", "non_observable"};

        for (String currType : parameterTypes) {
            Node currentNode = parametersNode.getElementsByTagName(currType).item(0);
            if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
                NodeList paramList = ((Element) currentNode).getElementsByTagName("param");
                for (int i = 0; i < paramList.getLength(); i++) {
                    if (paramList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element currParam = (Element) paramList.item(i);
                        PLPParameter plpParam = new PLPParameter(currParam.getAttribute("name"));

                        String error_param = currParam.getAttribute("error_param");
                        String freq = currParam.getAttribute("read_frequency");
                        if (!error_param.equals(""))
                            plpParam.setErrorParam(error_param);
                        if (!freq.equals("")) {
                            try {
                                plpParam.setReadFrequency(Double.parseDouble(freq));
                            } catch (Exception e) {
                                throw new NumberFormatException("Parameter read frequency: " + freq +
                                        " is not a real number");
                            }
                        }

                        NodeList paramFields = currParam.getElementsByTagName("field");
                        for (int j = 0; j < paramFields.getLength(); j++) {
                            if (paramFields.item(j).getNodeType() == Node.ELEMENT_NODE) {
                                plpParam.addParamFieldValue(((Element) paramFields.item(j)).getAttribute("value"));
                            }
                        }

                        switch (currType) {
                            case "execution_parameters":
                                plp.addExecParam(plpParam);
                                break;
                            case "input_parameters":
                                plp.addInputParam(plpParam);
                                break;
                            case "output_parameters":
                                plp.addOutputParam(plpParam);
                                break;
                            default:
                                plp.addUnobservableParam(plpParam);
                                break;
                        }
                    }
                }
            }
        }
    }


}
