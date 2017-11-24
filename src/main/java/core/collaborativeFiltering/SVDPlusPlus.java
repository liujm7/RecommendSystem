package core.collaborativeFiltering;

import core.MathUtility;
import data.utility.Tools;
import entity.Rating;
import entity.RsTable;
import entity.Tuple;
import evaluation.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.SchemaOutputResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version: 1.0
 * @author: Liujm
 * @site: https://github.com/liujm7
 * @contact: kaka206@163.com
 * @software: Idea
 * @date： 2017/11/14
 * @package_name: algorithm
 */
public class SVDPlusPlus {
    final static Logger logger = LoggerFactory.getLogger(SVDPlusPlus.class);

    protected int f = 10; //特征数
    protected int p = 0; //用户数
    protected int q = 0; //商品数
    public double w = 1.0;  // weight of neighbors

    public double[][] P = null; //用户特征矩阵
    public double[][] Q = null;//商品特征矩阵
    public double[][] Z = null;//sum of Yj which j belongs to N(u), N(u) presents items rated by user u
    public double[][] Y = null;//时间

    public double[] bu = null;
    public double[] bi = null;


    public SVDPlusPlus() {
    }

    public SVDPlusPlus(int p, int q, int f, String fillMethod) {
        initial(p, q, f, fillMethod);
    }

    public void initial(int p, int q, int f, String fillMethod) {
        this.p = p;
        this.q = q;
        this.f = f;

        this.bu = new double[p];
        this.bi = new double[q];

        if (fillMethod.equalsIgnoreCase("gaussian")) {
            P = MathUtility.randomGaussian(p, f, 0, 1);
            Q = MathUtility.randomGaussian(q, f, 0, 1);
            Z = MathUtility.randomGaussian(p, f, 0, 1);
            Y = MathUtility.randomGaussian(q, f, 0, 1);
        } else if (fillMethod.equalsIgnoreCase("uniform_df")) {
            P = MathUtility.randomUniform(p, f, 1.0 / Math.sqrt(f));
            Q = MathUtility.randomUniform(q, f, 1.0 / Math.sqrt(f));
            Z = MathUtility.randomUniform(p, f, 1.0 / Math.sqrt(f));
            Y = MathUtility.randomUniform(q, f, 1.0 / Math.sqrt(f));
        } else if (fillMethod.equalsIgnoreCase("uniform")) {
            P = MathUtility.randomUniform(p, f);
            Q = MathUtility.randomUniform(q, f);
            Z = MathUtility.randomUniform(p, f);
            Y = MathUtility.randomUniform(q, f);
        } else {
            P = new double[p][f];
            Q = new double[q][f];
            Z = new double[p][f];
            Y = new double[p][f];
        }

    }

    public double computeMiu(List<Rating> ratings) {
        double miu = 0;
        for (Rating r : ratings) {
            miu += r.score;
        }
        if (ratings.size() > 0) {
            miu /= ratings.size();
        }
        return miu;
    }

    public double predict(int userId, int itemId, double miu) {
        double r = 0.0;
        if (userId >= p || itemId >= q)
            return r + miu;
        for (int i = 0; i < f; i++) {
            r += Q[itemId][i] * (P[userId][i] + Z[userId][i]);
        }
        return r + bu[userId] + bi[itemId] + miu;
    }

    public double computeLoss(List<Rating> ratings, double lambda, double miu) {
        double loss = 0;
        for (Rating r : ratings) {
            double eui = r.score - predict(r.userId, r.itemId, miu);

            double sum_p_i = 0.0;
            double sum_q_j = 0.0;
            double sum_y_j = 0.0;

            for (int i = 0; i < f; i++) {
                sum_p_i += P[r.userId][i] * P[r.userId][i];
                sum_q_j += Q[r.itemId][i] * Q[r.itemId][i];
                sum_y_j += Y[r.itemId][i] * Y[r.itemId][i];
            }
            loss += (eui * eui + lambda * 0.5 * (sum_p_i + sum_q_j + sum_y_j + bu[r.userId] * bu[r.userId] + bi[r.itemId] * bi[r.itemId]));
        }
        return loss;
    }

    /**
     * Description:计算评估的mae和rmse
     *
     * @param ratings :评分
     * @return 返回mae，rmse
     */
    public Tuple<Double, Double> evaluateMaeRmse(List<Rating> ratings, double miu) {
        return evaluateMaeRmse(ratings, miu, 1.0, 5.0);
    }

    /**
     * Description:计算评估的mae和rmse
     *
     * @param ratings   评分
     * @param minRating 最小评分
     * @param maxRating 最大评分
     * @return
     */
    public Tuple<Double, Double> evaluateMaeRmse(List<Rating> ratings, double miu, double minRating, double maxRating) {
        double mae = 0;
        double rmse = 0;
        for (Rating r : ratings) {
            double pui = predict(r.userId, r.itemId, miu);
            double eui = r.score - pui;
            mae += Math.abs(eui);
            rmse += eui * eui;
        }
        if (ratings.size() > 0) {
            mae /= ratings.size();
            rmse = Math.sqrt(rmse / ratings.size());
        }
        return new Tuple<>(mae, rmse);
    }


    protected ConcurrentHashMap getUserItemsTable(List<Rating> ratings) {
        ConcurrentHashMap userItemsTable = new ConcurrentHashMap();
        for (Rating r : ratings) {
            if (userItemsTable.containsKey(r.userId)) {
                List<Rating> li = (List<Rating>) userItemsTable.get(r.userId);
                li.add(r);
                userItemsTable.put(r.userId, li);
            } else {
                List<Rating> li = new ArrayList<>();
                li.add(r);
                userItemsTable.put(r.userId, li);
            }
        }
        return userItemsTable;
    }

    protected void updataZ(ConcurrentHashMap userItemsTable) {
        for (Object uId : userItemsTable.keySet()) {
            List<Rating> list = (List<Rating>) userItemsTable.get(uId);
            for (Rating r : list) {
                for (int i = 0; i < f; i++) {
                    Z[(Integer) uId][i] += Y[r.itemId][i];
                }
            }
            if (list.size() > 1) {
                for (int i = 0; i < f; i++) {
                    Z[(Integer) uId][i] /= Math.sqrt(list.size());
                }
            }
        }
    }

    protected void updateZ(int uId, List<Rating> ratings, double ru) {// Z = sum(yj), j belongs to N(u), N(u) presents items rated by u
        for (Rating r : ratings) {
            for (int i = 0; i < f; i++) {
                Z[uId][i] += Y[r.itemId][i];
            }
        }
        for (int i = 0; i < f; i++) {
            Z[uId][i] *= ru;
        }
    }

    /**
     * Descriptions:输出参数
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
    protected void printParameters(List<Rating> train, List<Rating> test, int epochs, double gamma, double lambda
            , double decay, double minRating, double maxRating) {
        logger.info(getClass().getName());
        logger.info("train,{}", train.size());
        logger.info("test,{}", test == null ? 0 : test.size());
        logger.info("p,{},q,{},f,{}", p, q, f);
        logger.info("epochs,{}", epochs);
        logger.info("gamma,{}", gamma);
        logger.info("decay,{}", decay);
        logger.info("lambda,{}", lambda);
        logger.info("maximumRating,{}", maxRating);
        logger.info("minimumRating,{}", minRating);
    }

    /**
     * Description: 使用随机梯度进行训练
     *
     * @param train  训练集
     * @param epochs 迭代次数
     */
    public void SGD(List<Rating> train, int epochs) {
        SGD(train, epochs, 0.01, 0.01, 1, 1.0, 5.0);
    }

    /**
     * Description: 使用随机梯度进行训练
     *
     * @param train     训练集
     * @param epochs    迭代次数
     * @param gamma     gamma
     * @param lambda    lambda
     * @param decay     gamma 学习更新率
     * @param minRating 最小分数
     * @param maxRating 最大分数
     */
    public void SGD(List<Rating> train, int epochs, double gamma, double lambda
            , double decay, double minRating, double maxRating) {
        SGD(train, null, epochs, gamma, lambda, decay, minRating, maxRating);
    }


    /**
     * Description: 使用随机梯度进行训练
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
    public void SGD(List<Rating> train, List<Rating> test, int epochs, double gamma, double lambda
            , double decay, double minRating, double maxRating) {
        List<Rating> trainOrTest = (test == null ? train : test);
        String trainOrTestString = (test == null ? "train" : "test");
        printParameters(train, trainOrTest, epochs, gamma, lambda, decay, minRating, maxRating);
        double miu = computeMiu(train);
        ConcurrentHashMap userItemsTable = getUserItemsTable(train);
        updataZ(userItemsTable);

        double loss = computeLoss(train, lambda, miu);
        for (int epoch = 1; epoch <= epochs; epoch++) {
            for (Object userId : userItemsTable.keySet()) {
                int uId = (Integer) userId;
                List<Rating> li = (List<Rating>) userItemsTable.get(userId);
                double ru = w / Math.sqrt(li.size());
                updateZ(uId, li, ru);  // NOTE: different from the provided in Java, posite here to reduce complexity.
                double[] sum = new double[f];

                for (Rating r : li) {
                    double pui = predict(r.userId, r.itemId, miu);
                    double eui = r.score - pui;

                    bu[r.userId] += gamma * (eui - lambda * bu[r.userId]);
                    bi[r.itemId] += gamma * (eui - lambda * bi[r.itemId]);

                    for (int i = 0; i < f; i++) {
                        sum[i] += eui * ru * Q[r.itemId][i];
                        P[r.userId][i] += gamma * (eui * Q[r.itemId][i] - lambda * P[r.userId][i]);
                        Q[r.itemId][i] += gamma * (eui * (P[r.userId][i] + Z[r.userId][i]) - lambda * Q[r.itemId][i]);

                    }
                }
                for (Rating r : li) {
                    for (int i = 0; i < f; i++) {
                        Y[r.itemId][i] += gamma * (sum[i] - lambda * Q[r.itemId][i]);
                    }
                }
            }
            double lastLoss = computeLoss(train, lambda, miu);

            if (epoch % 5 == 0) {
                Tuple maeAndRmse = evaluateMaeRmse(trainOrTest, miu);
                logger.info("epoch:{},loss:{},{}-mae:{},{}-rmse:{}", epoch, loss, trainOrTestString, maeAndRmse.first, trainOrTestString, maeAndRmse.second);
            }

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
     * Description: 获取全局推荐列表
     *
     * @param ratingTable 评分表
     * @param miu         全局评分均值
     * @param N           topN
     * @return 评分列表
     */
    protected List<Rating> getRecommendations(RsTable ratingTable, double miu, int N) {
        List<Rating> recommendItems = new ArrayList<>();
        ArrayList list = ratingTable.getSubKeyList();
        for (Object userId : ratingTable.keys()) {
            ConcurrentHashMap Nu = (ConcurrentHashMap) ratingTable.get(userId);
            List<Rating> predictRatings = new ArrayList<>();
            for (Object itemId : list) {
                if (!Nu.containsKey(itemId)) {
                    double p = predict((Integer) userId, (Integer) itemId, miu);
                    predictRatings.add(new Rating((Integer) userId, (Integer) itemId, p));
                }
            }
            Collections.sort(predictRatings);
            recommendItems.addAll(predictRatings.subList(0, Math.min(N, predictRatings.size())));
        }

        return recommendItems;
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
    public void testSGDForTopN(List<Rating> train, List<Rating> test, int epochs, double gamma
            , double lambda, double decay, double minRating, double maxRating) {
        printParameters(train, test, epochs, gamma, lambda, decay, minRating, maxRating);
        double miu = computeMiu(train);
        ConcurrentHashMap userItemsTable = getUserItemsTable(train);
        updataZ(userItemsTable);

//        int[] K = {1, 5, 10, 15, 20, 25, 30};
        int[] K = {80};

        RsTable ratingTable = Tools.getRatingTable(train);
        double loss = computeLoss(train, lambda, miu);
        for (int epoch = 1; epoch <= epochs; epoch++) {
            for (Object userId : userItemsTable.keySet()) {
                int uId = (Integer) userId;
                List<Rating> li = (List<Rating>) userItemsTable.get(userId);
                double ru = w / Math.sqrt(li.size());
                updateZ(uId, li, ru);  // NOTE: different from the provided in Java, posite here to reduce complexity.
                double[] sum = new double[f];

                for (Rating r : li) {
                    double pui = predict(r.userId, r.itemId, miu);
                    double eui = r.score - pui;

                    bu[r.userId] += gamma * (eui - lambda * bu[r.userId]);
                    bi[r.itemId] += gamma * (eui - lambda * bi[r.itemId]);

                    for (int i = 0; i < f; i++) {
                        sum[i] += eui * ru * Q[r.itemId][i];
                        P[r.userId][i] += gamma * (eui * Q[r.itemId][i] - lambda * P[r.userId][i]);
                        Q[r.itemId][i] += gamma * (eui * (P[r.userId][i] + Z[r.userId][i]) - lambda * Q[r.itemId][i]);

                    }
                }
                for (Rating r : li) {
                    for (int i = 0; i < f; i++) {
                        Y[r.itemId][i] += gamma * (sum[i] - lambda * Q[r.itemId][i]);
                    }
                }
            }

            double lastLoss = computeLoss(train, lambda, miu);

            List<Rating> recommendations = getRecommendations(ratingTable, miu, K[K.length - 1]);   // note that, the max K
            for (int k : K) {
                List<Rating> subset = Tools.getSubset(recommendations, k);
                Tuple pr = Metrics.computePrecisionAndRecall(subset, test);
                double map = Metrics.computeMAP(subset, test, k);
                Tuple cp = Metrics.computeCoverageAndPopularity(subset, train);
                logger.info("epoch:{}, lastLoss:{},K:{},precision:{},recall:{},coverage:{},popularity:{},map:{}.",
                        epoch, lastLoss, k, pr.first, pr.second, cp.first, cp.second, map);
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

        SVDPlusPlus f = new SVDPlusPlus(6, 5, 4, "uniform_df");
        Double miu = f.computeMiu(ratings);
        f.SGD(ratings, 500);


        List<Rating> predicts = new ArrayList<>();

        predicts.add(new Rating(5, 1, f.predict(5, 1, miu)));
        predicts.add(new Rating(2, 2, f.predict(2, 2, miu)));
        predicts.add(new Rating(4, 2, f.predict(4, 2, miu)));
        predicts.add(new Rating(1, 3, f.predict(1, 3, miu)));
        predicts.add(new Rating(2, 3, f.predict(2, 3, miu)));
        predicts.add(new Rating(3, 3, f.predict(3, 3, miu)));
        predicts.add(new Rating(4, 3, f.predict(4, 3, miu)));

        for (Rating r : predicts) {
            logger.info("{},{},{}", r.userId, r.itemId, r.score);
        }
    }

    public static void main(String[] args) {
        example();
    }


}
