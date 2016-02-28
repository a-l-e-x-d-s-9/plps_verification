package loader;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class test {
    public static void main(String[] args) {
        PLPLoader.loadFromDirectory("C:\\Users\\maora_000\\Desktop\\PLP-repo\\PLP-examples");
        System.out.println(PLPLoader.getAchievePLPs().get(0).toString());
    }
}
