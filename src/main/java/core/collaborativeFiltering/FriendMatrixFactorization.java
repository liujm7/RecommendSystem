package core.collaborativeFiltering;

import data.utility.Tools;
import entity.Link;
import entity.Rating;
import entity.RsTable;
import entity.Tuple;
import evaluation.Metrics;

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
public class FriendMatrixFactorization extends MatrixFactorization {
    protected double[][] X = null; // 关系weighted


    public FriendMatrixFactorization() {
    }

    public FriendMatrixFactorization(int p, int q, int f, String fillMethod) {
        initial(p, q, f, fillMethod);
    }

    @Override
    public void initial(int p, int q, int f, String fillMethod) {
        super.initial(p, q, f, fillMethod);
        X = new double[p][f];
    }

    protected void updateX(int uId, List<Link> links, double w) {
        for (int i = 0; i < f; i++) {
            X[uId][i] = 0;
        }

        for (Link t : links) {
            if (t.to > p) {
                continue;
            }
            for (int i = 0; i < f; i++) {
                X[uId][i] += P[t.to][i];
            }
        }

        if (links.size() > 0) {
            for (int i = 0; i < f; i++) {
                X[uId][i] *= (w * 1.0 / links.size());  //  (w / Math.Sqrt(friends.Count));
            }
        }

    }

    protected void updateX(ConcurrentHashMap userLinksTable, double w) {
        for (Object userId : userLinksTable.keySet()) {
            int uId = (Integer) userId;
            List<Link> links = (List<Link>) userLinksTable.get(userId);
            updateX(uId, links, w);
        }
    }

    @Override
    public double predict(int uId, int iId) {
        double r = 0;
        for (int i = 0; i < f; i++) {
            r += P[uId][i] * (Q[iId][i] + X[uId][i]);
        }
        return r;
    }

    private void printParameters(List<Rating> train, List<Rating> test, List<Link> links, double w, int epochs, double gamma, double lambda
            , double decay, double minRating, double maxRating) {
        logger.info(getClass().getName());
        logger.info("train,{}", train.size());
        logger.info("test,{}", test == null ? 0 : test.size());
        logger.info("links,{}", links.size());
        logger.info("w,{}", w);
        logger.info("p,{},q,{},f,{}", p, q, f);
        logger.info("epochs,{}", epochs);
        logger.info("gamma,{}", gamma);
        logger.info("decay,{}", decay);
        logger.info("lambda,{}", lambda);
        logger.info("minimumRating,{}", minRating);
        logger.info("maximumRating,{}", maxRating);
    }


    private void SGD(List<Rating> train, List<Rating> test, List<Link> links, double w, int epochs, double gamma
            , double lambda, double decay, double minRating, double maxRating) {
        List<Rating> trainOrTest = test == null ? train : test;
        String trainOrTestString = test == null ? "train-rmse" : "test-rmse";
        printParameters(train, trainOrTest, links, w, epochs, gamma, lambda, decay, minRating, maxRating);
        ConcurrentHashMap userItemsTable = Tools.getUserItemsTable(train);
        ConcurrentHashMap userLinksTable = Tools.getUserLinksTable(links);

        updateX(userLinksTable, w);
        double loss = computeLoss(train, lambda);

        for (int epoch = 0; epoch < epochs; epoch++) {
            for (Object userId : userItemsTable.keySet()) {
                if (userLinksTable.containsKey(userId)) {
                    List<Link> links1 = (List<Link>) userLinksTable.get(userId);
                    updateX((Integer) userId, links1, w);
                }

                List<Rating> ratings = (List<Rating>) userItemsTable.get(userId);
                for (Rating r : ratings) {
                    double pui = predict(r.userId, r.itemId);
                    double eui = r.score - pui;
                    for (int i = 0; i < f; i++) {
                        P[r.userId][i] += gamma * (eui * (Q[r.itemId][i] * X[r.userId][i]) - lambda * P[r.userId][i]);
                        Q[r.itemId][i] += gamma * (eui * P[r.userId][i] - lambda * Q[r.itemId][i]);
                    }
                }
            }
            double lastLoss = computeLoss(train, lambda);
            Tuple maeAndRmse = evaluateMaeRmse(test, minRating, maxRating);
            logger.info("epoch:{},loss:{},train-mae:{},{}:{}", epoch, loss, maeAndRmse.first, trainOrTestString, maeAndRmse.second);

            if (decay != 1.0) {
                gamma *= decay;
            }
            if (lastLoss < loss) {
                loss = lastLoss;
            } else {
                break;
            }
        }

    }


    /**
     * Description: 测试使用sgd进行topN推荐效果
     *
     * @param train     训练集
     * @param test      测试集
     * @param epochs    迭代次数
     * @param gamma     gamma
     * @param lambda    lambda
     * @param decay     gamma 学习更新率
     * @param minRating 最小分数
     * @param maxRating 最大分数
     */
    public void testSGDForTopN(List<Rating> train, List<Rating> test, List<Link> links, double w, int epochs
            , double gamma, double lambda, double decay, double minRating, double maxRating) {
        printParameters(train, test, links, w, epochs, gamma, lambda, decay, minRating, maxRating);

        RsTable ratingTable = Tools.getRatingTable(train);
        ConcurrentHashMap userItemsTable = Tools.getUserItemsTable(train);
        ConcurrentHashMap userLinksTable = Tools.getUserLinksTable(links);

        updateX(userLinksTable, w);
        double loss = computeLoss(train, lambda);
        int[] K = {1, 5, 10, 15, 20, 25, 30};

        for (int epoch = 1; epoch <= epochs; epoch++) {
            for (Object userId : userItemsTable.keySet()) {
                if (userLinksTable.containsKey(userId)) {
                    List<Link> links1 = (List<Link>) userLinksTable.get(userId);
                    updateX((Integer) userId, links1, w);
                }

                List<Rating> ratings = (List<Rating>) userItemsTable.get(userId);
                for (Rating r : ratings) {
                    double pui = predict(r.userId, r.itemId);
                    double eui = r.score - pui;
                    for (int i = 0; i < f; i++) {
                        P[r.userId][i] += gamma * (eui * (Q[r.itemId][i] * X[r.userId][i]) - lambda * P[r.userId][i]);
                        Q[r.itemId][i] += gamma * (eui * P[r.userId][i] - lambda * Q[r.itemId][i]);
                    }
                }
            }

            double lastLoss = computeLoss(train, lambda);
            if (epoch % 5 == 0) {
                List<Rating> recommendations = getRecommendations(ratingTable, K[K.length - 1]);   // note that, the max K
                for (int k : K) {
                    List<Rating> subset = Tools.getSubset(recommendations, k);
                    Tuple pr = Metrics.computePrecisionAndRecall(subset, test);
                    Tuple cp = Metrics.computeCoverageAndPopularity(subset, train);
                    Double map = Metrics.computeMAP(subset, test, k);
                    logger.info("epoch:{}, lastLoss:{},K:{},precision:{},recall:{},coverage:{},popularity:{},map:{}.",
                            epoch, lastLoss, k, pr.first, pr.second, cp.first, cp.second, map);
                }
            }

            if (decay != 1) {
                gamma *= decay;
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

        FriendMatrixFactorization f = new FriendMatrixFactorization(6, 5, 4, "uniform_df");
        f.SGD(ratings, 500);

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
