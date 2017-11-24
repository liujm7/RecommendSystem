package core.collaborativeFiltering;

import core.MathUtility;
import data.utility.Tools;
import entity.Rating;
import entity.RsTable;
import entity.Tuple;
import evaluation.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
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
public class BiasedMatrixFactorization {
    final static Logger logger = LoggerFactory.getLogger(MatrixFactorization.class);

    protected int f = 10; //特征数
    protected int p = 0; //用户数
    protected int q = 0; //商品数
    public double[][] P = null; //用户特征矩阵
    public double[][] Q = null; //商品特征矩阵

    public double[] bu = null;
    public double[] bi = null;

    public BiasedMatrixFactorization() {
    }

    public BiasedMatrixFactorization(int p, int q, int f, String fillMethod) {
        initial(p, q, f, fillMethod);
    }

    /**
     * Description: 初始化变量
     *
     * @param p          用户数量
     * @param q          商品数量
     * @param f          隐含特征数
     * @param fillMethod 初始化方式
     */
    public void initial(int p, int q, int f, String fillMethod) {
        this.p = p;
        this.q = q;
        this.f = f;

        this.bu = new double[p];
        this.bi = new double[q];

        if (fillMethod.equalsIgnoreCase("gaussian")) {
            P = MathUtility.randomGaussian(p, f, 0, 1);
            Q = MathUtility.randomGaussian(q, f, 0, 1);
        } else if (fillMethod.equalsIgnoreCase("uniform_df")) {
            P = MathUtility.randomUniform(p, f, 1 / Math.sqrt(f));
            Q = MathUtility.randomUniform(q, f, 1 / Math.sqrt(f));
        } else if (fillMethod.equalsIgnoreCase("uniform")) {
            P = MathUtility.randomUniform(p, f);
            Q = MathUtility.randomUniform(q, f);
        } else {
            P = new double[p][f];
            Q = new double[q][f];
        }

    }

    /**
     * Description： 计算全局平均分
     *
     * @param ratings 评分立标
     * @return 全局平均分
     */
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

    /**
     * Description:预测评分
     *
     * @param userId 用户id
     * @param itemId 商品id
     * @param miu    全局评分值
     * @return 返回预测评分
     */
    public double predict(int userId, int itemId, double miu) {
        double r = 0.0;
        for (int i = 0; i < f; i++) {
            r += P[userId][i] * Q[itemId][i];
        }

        return r + bu[userId] + bi[itemId] + miu;
    }

    /**
     * Description:计算损失函数
     *
     * @param ratings 分数
     * @param lambda  参数
     * @param miu     全局评分均值
     * @return 返回计算后的损失值
     */
    public double computeLoss(List<Rating> ratings, double lambda, double miu) {
        double loss = 0;
        for (Rating r : ratings) {
            double eui = r.score - predict(r.userId, r.itemId, miu);

            double sum_p_i = 0.0;
            double sum_q_j = 0.0;
            for (int i = 0; i < f; i++) {
                sum_p_i += P[r.userId][i] * P[r.userId][i];
                sum_q_j += Q[r.itemId][i] * Q[r.itemId][i];
            }
            loss += (eui * eui + lambda * 0.5 * (sum_p_i + sum_q_j + bu[r.userId] * bu[r.userId] + bi[r.itemId] * bi[r.itemId]));
        }
        return loss;
    }

    /**
     * Description:计算评估的mae和rmse
     *
     * @param ratings :评分
     * @param miu     全局评分均值
     * @return 返回mae，rmse
     */
    public Tuple<Double, Double> evaluateMaeRmse(List<Rating> ratings, double miu) {

        return evaluateMaeRmse(ratings, miu, 1.0, 5.0);
    }

    /**
     * Description:计算评估的mae和rmse
     *
     * @param ratings   评分
     * @param miu       全局评分均值
     * @param minRating 最小评分
     * @param maxRating 最大评分
     * @return 返回mae，rmse
     */
    public Tuple<Double, Double> evaluateMaeRmse(List<Rating> ratings, double miu, double minRating, double maxRating) {
        double mae = 0;
        double rmse = 0;
        for (Rating r : ratings) {
            double pui = predict(r.userId, r.itemId, miu);
//            if (pui < minRating) {
//                pui = minRating;
//            } else if (pui > maxRating) {
//                pui = maxRating;
//            }
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
        logger.info("minimumRating,{}", minRating);
        logger.info("maximumRating,{}", maxRating);
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

        double loss = computeLoss(train, lambda, miu);
        for (int epoch = 1; epoch <= epochs; epoch++) {
            for (Rating r : train) {
                double pui = predict(r.userId, r.itemId, miu);
//                if (pui > maxRating) {
//                    pui = maxRating;
//                } else if (pui < minRating) {
//                    pui = minRating;
//                }
                double eui = r.score - pui;

                bu[r.userId] += gamma * (eui - lambda * bu[r.userId]);
                bi[r.itemId] += gamma * (eui - lambda * bi[r.itemId]);

                for (int i = 0; i < f; i++) {
                    Q[r.itemId][i] += gamma * (eui * P[r.userId][i] - lambda * Q[r.itemId][i]);
                    P[r.userId][i] += gamma * (eui * Q[r.itemId][i] - lambda * P[r.userId][i]);

                }
            }
            double finalLoss = computeLoss(train, lambda, miu);
            if (epoch % 10 == 0) {
                Tuple maeAndRmse = evaluateMaeRmse(trainOrTest, miu);
                logger.info("epoch:{},loss:{},{}-mae:{},{}-rmse:{}", epoch, loss, trainOrTestString, maeAndRmse.first, trainOrTestString, maeAndRmse.second);

            }

            if (decay != 1.0) {
                gamma *= decay;
            }
            if (finalLoss < loss) {
                loss = finalLoss;
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
//            if ((int) userId == 0) {
//                for (Rating rating : predictRatings.subList(0, Math.min(N, predictRatings.size()))) {
//                    System.out.println(rating.userId + ":" + rating.itemId + ":" + rating.score);
//                }
//            }
            recommendItems.addAll(predictRatings.subList(0, Math.min(N, predictRatings.size())));
        }

        return recommendItems;
    }

    /**
     * Description: 测试使用sgd进行topN推荐效果
     *
     * @param train     训练集
     * @param test      测试集
     * @param epochs    迭代次数(建议5-20次之间)
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
        double loss = computeLoss(train, lambda, miu);

        RsTable ratingTable = Tools.getRatingTable(train);

//        int[] K = {1, 5, 10, 15, 20, 25, 30};
        int[] K = {80};

        for (int epoch = 1; epoch <= epochs; epoch++) {
            for (Rating r : train) {
                double pui = predict(r.userId, r.itemId, miu);

                double eui = r.score - pui;

                bu[r.userId] += gamma * (eui - lambda * bu[r.userId]);
                bi[r.itemId] += gamma * (eui - lambda * bi[r.itemId]);

                for (int i = 0; i < f; i++) {
                    Q[r.itemId][i] += gamma * (eui * P[r.userId][i] - lambda * Q[r.itemId][i]);
                    P[r.userId][i] += gamma * (eui * Q[r.itemId][i] - lambda * P[r.userId][i]);
                }
            }

            double lastLoss = computeLoss(train, lambda, miu);

            List<Rating> recommendations = getRecommendations(ratingTable, miu, K[K.length - 1]);   // note that, the max K
            for (int k : K) {
                List<Rating> subset = Tools.getSubset(recommendations, k);
                Tuple pr = Metrics.computePrecisionAndRecall(subset, test);
                Tuple cp = Metrics.computeCoverageAndPopularity(subset, train);
                double map = Metrics.computeMAP(subset, test, k);
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

        BiasedMatrixFactorization f = new BiasedMatrixFactorization(6, 5, 4, "uniform_df");
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
