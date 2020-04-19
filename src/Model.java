import java.util.Random

public class Model {
    double[] mobilityConstants;
    double immunityConstant;
    int lag;
    double fatalityRatio;

    double score = 100.0;

    double immunityVariability = 0.05;
    int mobilityLagVariability = 1;
    double mobilityConstantVariability = 0.1;
    double fatalityVariability = 0.01;

    public Model(double[] listMobConstants, float cImmunity, int mobilityLag, float deathRatio){
        mobilityConstants = listMobConstants;
        immunityConstant = cImmunity;
        lag = mobilityLag;
        fatalityRatio = deathRatio;
    }

    public Model(){
        Random r = new Random();

        mobilityConstants = new double[5];
        for (int i = 0; i < 5; i ++) {
            mobilityConstants[i] = 2 * (r.nextDouble() - 0.5);
        }
        immunityConstant = r.nextDouble();
        lag = 16;
        fatalityRatio = r.nextDouble() / 2;
    }

    public Model mutate(){

    }

    public Model predict(int current, double[] mobility, double infectionRate, int population){

    }



}
