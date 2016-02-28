package cs.bgu.maorash.plps.loader;

import cs.bgu.maorash.plps.modules.PLP;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class TestLoader {
    public static void main(String[] args) {
        PLPLoader.loadFromDirectory("C:\\Users\\maora_000\\Desktop\\PLP-lib\\PLP-examples");
        for (PLP plp : PLPLoader.getAchievePLPs())
            System.out.println(plp.toString());
        for (PLP plp : PLPLoader.getDetectPLPs())
            System.out.println(plp.toString());
        for (PLP plp : PLPLoader.getMaintainPLPs())
            System.out.println(plp.toString());
        for (PLP plp : PLPLoader.getObservePLPs())
            System.out.println(plp.toString());

    }
}
