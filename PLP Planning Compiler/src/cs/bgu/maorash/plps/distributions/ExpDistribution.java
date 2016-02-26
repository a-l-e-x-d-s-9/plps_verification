package cs.bgu.maorash.plps.distributions;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class ExpDistribution {
    private String lambda;

    public ExpDistribution(String lambda) {
        this.lambda = lambda;
    }

    public String getLambda() {
        return lambda;
    }
}
