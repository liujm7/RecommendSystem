package core.collaborativeFiltering;

import core.MathUtility;
import data.utility.Tools;
import entity.Rating;
import entity.RsTable;
import entity.Tuple;
import evaluation.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version: 1.0
 * @author: Liujm
 * @site: https://github.com/liujm7
 * @contact: kaka206@163.com
 * @software: Idea
 * @date： 2017/11/14
 * @package_name: core.collaborativeFiltering
 */
public class AlternatingLeastSquares extends MatrixFactorization {
    final static Logger logger = LoggerFactory.getLogger(AlternatingLeastSquares.class);

    public AlternatingLeastSquares() {
    }

    public AlternatingLeastSquares(int p, int q, int f, String fillMethod) {

        super.initial(p, q, f, fillMethod);
    }

    protected void printParameters(List<Rating> train, List<Rating> test, int epochs, double lambda
            , double minRating, double maxRating) {
        logger.info(getClass().getName());
        logger.info("train,{}", train.size());
        logger.info("test,{}", test == null ? 0 : test.size());
        logger.info("p,{},q,{},f,{}", p, q, f);
        logger.info("epochs,{}", epochs);
        logger.info("lambda,{}", lambda);
        logger.info("minimumRating,{}", minRating);
        logger.info("maximumRating,{}", maxRating);
    }

    protected void stepP(ConcurrentHashMap userRatingsTable, double lambda) {
        for (Object userId : userRatingsTable.keySet()) {
            List<Rating> ratings = (List<Rating>) userRatingsTable.get(userId);

            double[][] Au = new double[f][f];

            double[] du = new double[f];

            for (Rating r : ratings) { // O(Nu * K^2), rating number of user u
                for (int i = 0; i < f; i++) {
                    for (int j = 0; j < f; j++) {
                        Au[i][j] += Q[r.itemId][i] * Q[r.itemId][j];
                    }
                    du[i] += r.score * Q[r.itemId][i];
                }
            }
            // lamda * I + A
            for (int i = 0; i < f; i++) {
                Au[i][i] += lambda * ratings.size();
            }

            double[][] AuReverse = MathUtility.inverseMatrix(Au); // O(K^3)

            for (int i = 0; i < f; i++) {
                double vij = 0;
                for (int j = 0; j < f; j++) {
                    vij += AuReverse[i][j] * du[j];
                }
                P[(Integer) userId][i] = vij;
            }
        }
    }

    protected void stepQ(ConcurrentHashMap itemRatingsTable, double lambda) {
        for (Object itemId : itemRatingsTable.keySet()) {
            List<Rating> ratings = (List<Rating>) itemRatingsTable.get(itemId);

            double[][] Ai = new double[f][f];

            double[] di = new double[f];

            for (Rating r : ratings) {
                for (int i = 0; i < f; i++) {
                    for (int j = 0; j < f; j++) {
                        Ai[i][j] += P[r.userId][i] * P[r.userId][j];
                    }
                    di[i] += r.score * P[r.userId][i];
                }
            }

            for (int i = 0; i < f; i++) {
                Ai[i][i] += lambda * ratings.size();
            }

            double[][] AiReverse = MathUtility.inverseMatrix(Ai);
            for (int i = 0; i < f; i++) {
                double vij = 0;
                for (int j = 0; j < f; j++) {
                    vij += AiReverse[i][j] * di[j];
                }
                Q[(Integer) itemId][i] += vij;
            }
        }
    }

    public void ALS(List<Rating> train, int epochs) {
        ALS(train, null, epochs, 0.0001, 1.0, 5.0);
    }

    public void ALS(List<Rating> train, List<Rating> test, int epochs, double lambda
            , double minRating, double maxRating) {
        List<Rating> trainOrTest = (test == null ? train : test);
        String trainOrTestString = (test == null ? "train" : "test");
        printParameters(train, trainOrTest, epochs, lambda, minRating, maxRating);

        ConcurrentHashMap userRatingsTable = Tools.getUserItemsTable(train);
        ConcurrentHashMap itemRatingsTable = Tools.getItemUsersTable(train);

        double loss = computeLoss(train, lambda);
        for (int epoch = 1; epoch <= epochs; epoch++) {
            stepP(userRatingsTable, lambda);
            stepQ(itemRatingsTable, lambda);

            double finalLoss = computeLoss(train, lambda);

            if (epoch % 5 == 0){
                Tuple maeAndRmse = evaluateMaeRmse(trainOrTest);
                logger.info("epoch:{},loss:{},{}-mae:{},{}-rmse:{}", epoch, loss, trainOrTestString, maeAndRmse.first, trainOrTestString, maeAndRmse.second);
            }
            if (finalLoss < loss) {
                loss = finalLoss;
            }
//            else {
//                break;
//            }
        }
    }

    public void testAlsForTopN(List<Rating> train, List<Rating> test) {
        testALSForTopN(train, test, 100, 0.001, 1, 5);
    }

    public void testALSForTopN(List<Rating> train, List<Rating> test, int epochs, double lambda
            , double minRating, double maxRating) {
        if (train == null || test == null || train.size() < 1 || test.size() < 1) {
            throw new IllegalArgumentException("训练集和测试集合输入有问题");
        }
        printParameters(train, test, epochs, lambda, minRating, maxRating);
        double loss = computeLoss(train, lambda);
        int[] K = {80};  // recommdation list

        ConcurrentHashMap userRatingsTable = Tools.getUserItemsTable(train);
        ConcurrentHashMap itemRatingsTable = Tools.getItemUsersTable(train);
        RsTable ratingTable = Tools.getRatingTable(train);

        for (int epoch = 1; epoch <= epochs; epoch++) {
            stepP(userRatingsTable, lambda);
            stepQ(itemRatingsTable, lambda);

            double lastLoss = computeLoss(train, lambda);
            if (epoch % 10 == 0) {
                List<Rating> recommendations = getRecommendations(ratingTable, K[K.length - 1]);   // note that, the max K
                for (int k : K) {
                    List<Rating> subset = Tools.getSubset(recommendations, k);
                    Tuple pr = Metrics.computePrecisionAndRecall(subset, test);
                    Tuple cp = Metrics.computeCoverageAndPopularity(subset, train);
                    double map = Metrics.computeMAP(subset, test, k);
                    logger.info("epoch:{},loss:{}K:{},precision:{},recall:{},coverage:{},popularity:{},map:{}.",
                            epoch, loss, k, pr.first, pr.second, cp.first, cp.second, map);
                }
            }

            if (lastLoss < loss) {
                loss = lastLoss;
            } else {
                break;
            }
        }

    }


    /**
     * Description:测试
     */
    public static void example() {
        List<Rating> ratings = new ArrayList<>();

        ratings.add(new Rating(1, 1, 5));
        ratings.add(new Rating(2, 1, 4));
        ratings.add(new Rating(3, 1, 1));
        ratings.add(new Rating(4, 1, 1));
        ratings.add(new Rating(1, 2, 3));
        ratings.add(new Rating(3, 2, 1));
        ratings.add(new Rating(5, 2, 1));
        ratings.add(new Rating(5, 3, 5));
        ratings.add(new Rating(1, 4, 1));
        ratings.add(new Rating(2, 4, 1));
        ratings.add(new Rating(3, 4, 5));
        ratings.add(new Rating(4, 4, 4));
        ratings.add(new Rating(5, 4, 4));

        AlternatingLeastSquares f = new AlternatingLeastSquares(6, 5, 3, "uniform_df");
//        f.ALS(ratings, 500);
        f.testAlsForTopN(ratings, ratings);
        List<Rating> predicts = new ArrayList<>();

        predicts.add(new Rating(5, 1, f.predict(5, 1)));
        predicts.add(new Rating(2, 2, f.predict(2, 2)));
        predicts.add(new Rating(4, 2, f.predict(4, 2)));
        predicts.add(new Rating(1, 3, f.predict(1, 3)));
        predicts.add(new Rating(2, 3, f.predict(2, 3)));
        predicts.add(new Rating(3, 3, f.predict(3, 3)));
        predicts.add(new Rating(4, 3, f.predict(4, 3)));

        for (Rating r : predicts) {
            logger.info("{},{},{}", r.userId, r.itemId, r.score);
        }
    }

    public static void main(String[] args) {
        example();
    }


}
