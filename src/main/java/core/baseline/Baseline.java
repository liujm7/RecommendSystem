package core.baseline;

import data.utility.Tools;
import entity.Rating;
import entity.RsTable;
import entity.Tuple;
import evaluation.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static data.utility.Tools.getUserItemsTable;

/**
 * @version: 1.0
 * @author: Liujm
 * @site: https://github.com/liujm7
 * @contact: kaka206@163.com
 * @software: Idea
 * @date： 2017/11/20
 * @package_name: core.baseline
 */
public class Baseline {
    final static Logger logger = LoggerFactory.getLogger(Baseline.class);

    protected int p = 0; //用户数
    protected int q = 0; //商品数

    public double[] bu = null;
    public double[] bi = null;
    public int[] cu = null;
    public int[] ci = null;

    public Baseline() {
    }

    /**
     * Description:初始化
     *
     * @param p 用户数
     * @param q 商品数
     */
    public Baseline(int p, int q) {
        this.p = p;
        this.q = q;

        this.bu = new double[p];
        this.bi = new double[q];
        this.cu = new int[p];
        this.ci = new int[q];
    }

    /**
     * Description:计算全局均值，用户均值和商品均值
     *
     * @param ratings
     * @return
     */
    public double computeMiuAndBi(List<Rating> ratings) {
        double miu = 0;
        for (Rating r : ratings) {
            miu += r.score;
            bu[r.userId] += r.score;
            bi[r.itemId] += r.score;
            cu[r.userId] += 1;
            ci[r.itemId] += 1;
        }

        for (int i = 0; i < p; p++) {
            bu[i] /= cu[i];
        }

        for (int i = 0; i < q; q++) {
            bi[i] /= ci[i];
        }

        if (ratings.size() > 0) {
            miu /= ratings.size();
        }
        return miu;
    }


    public double predict(int userId, int itemId, double miu) {
        double r = 0.0;
        if (userId >= p || itemId >= q)
            return miu;

        //原式 (bu-miu) + (bi-miu) + miu
        return bu[userId] + bi[itemId] - miu;
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
     * Description: 预测＋评估
     *
     * @param train 训练集
     * @param test  测试集
     */
    public void baseLineEvaluate(List<Rating> train, List<Rating> test) {
        double miu = computeMiuAndBi(train);
        int K = 40;
        RsTable ratingTable = Tools.getRatingTable(train);
        List<Rating> recommendations = getRecommendations(ratingTable, miu, K);

        List<Rating> subset = Tools.getSubset(recommendations, K);
        Tuple pr = Metrics.computePrecisionAndRecall(subset, test);
        double map = Metrics.computeMAP(subset, test, K);
        Tuple cp = Metrics.computeCoverageAndPopularity(subset, train);

        Tuple maeAndRmse = evaluateMaeRmse(test, miu);
        logger.info("MAE:{},RMSE:{}", maeAndRmse.first, maeAndRmse.second);
        logger.info("K:{},precision:{},recall:{},coverage:{},popularity:{},map:{}.",
                K, pr.first, pr.second, cp.first, cp.second, map);
    }

}
