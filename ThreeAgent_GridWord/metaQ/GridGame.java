package metaQ;


/**
 * @author Elegate
 * @version 1.0
 */
public interface GridGame
{
    void train(double alpha, double beta);

    void testTrainingResult();

    boolean gameover();

    void getAction();
    
    int getLocationNext(int location, int action);
    
    void resetGame();

    void setReward();

    boolean isGoal(int agent, int location);

    double getEquilibrium(int agent, int s1, int s2, int s3);
    
    int[] getEquilibriumAction(int s1,int s2, int s3);

    void selfIntro();
}
