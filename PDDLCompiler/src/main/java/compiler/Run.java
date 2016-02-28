package compiler;


import loader.PLPLoader;

import java.io.File;
import java.io.PrintWriter;

public class Run {
    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Usage: <dir path>");
        }
        else {
            PLPLoader.loadFromDirectory(args[0]);
            PDDLCompiler.setAchievePLPs(PLPLoader.getAchievePLPs());
            PDDLCompiler.setObservePLPs(PLPLoader.getObservePLPs());
            String compiledPDDL = PDDLCompiler.producePDDL();
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(args[0]+"\\pddl_actions.txt", "UTF-8");
                writer.print(compiledPDDL);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (writer != null)
                    writer.close();
            }

        }

        //System.out.println(PDDLCompiler.producePDDL(args[0]));
        /*System.out.println(walkThroughGateway.toString());

        System.out.println("--------------------------------------------");
        System.out.println(observeGateway.toString());

        System.out.println("--------------------------------------------");
        List<ObservationGoal> observableValues = new LinkedList<>();
        observableValues.add(observeGateway.getGoal());

        System.out.println(walkThroughGateway.toPDDL(observableValues));
        System.out.println("--------------------------------------------");
        System.out.println(observeGateway.toPDDL());*/
    }
}
