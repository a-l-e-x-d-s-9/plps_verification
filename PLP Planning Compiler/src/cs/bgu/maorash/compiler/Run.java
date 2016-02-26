package cs.bgu.maorash.compiler;


public class Run {
    public static void main(String[] args) {

        System.out.println(PDDLCompiler.producePDDL(args[0]));
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
