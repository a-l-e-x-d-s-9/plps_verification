package distributions;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class ExpDistribution implements Distribution {
    private String lambda;

    public ExpDistribution(String lambda) {
        this.lambda = lambda;
    }

    public String getLambda() {
        return lambda;
    }

    @Override
    public String toString() {
        return "Exp("+lambda+")";
    }
}
