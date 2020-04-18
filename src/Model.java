import java.util.Random

public class Model {
    double[] listMobConstants;
    double cImmunity;
    int mobilityLag;
    double fatalityRatio;

    double score = 100.0;

    double immunityVariability = 0.05;
    int mobilityLagVariability = 1;
    double mobilityConstantVariability = 0.1;
    double fatalityVariability = 0.01;

    public Model(float[] listMobConstants, float cImmunity, int mobilityLag, float fatalityRatio){
        listMobConstants = listMobConstants;
        cImmunity = cImmunity;
        mobilityLag = mobilityLag;
        fatalityRatio = fatalityRatio;

    }

    public Model(){
        Random r = new Random();

        listMobConstants = new double[5];
        for (int i = 0; i < 5; i ++) {
            listMobConstants[i] = 2 * (r.nextDouble() - 0.5);
        }


    }

}
