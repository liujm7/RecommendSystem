package core.baseline;

import entity.Rating;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import entity.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version: 1.0
 * @author: Liujm
 * @site: https://github.com/liujm7
 * @contact: kaka206@163.com
 * @software: Idea
 * @date： 2017/11/10
 * @package_name: algorithm
 */

public class MeanFilling {
    final static Logger logger = LoggerFactory.getLogger(MeanFilling.class);

    /**
     * Description:使用全局评分均值来填充未知数据
     *
     * @param train 训练集
     * @param test  测试集
     * @return 返回评估指标对象
     */
    public static Tuple globalMeanFilling(List<Rating> train, List<Rating> test) {
        double miu = 0;
        ConcurrentHashMap<Integer, List<Rating>> table = new ConcurrentHashMap<Integer, List<Rating>>();
        for (Rating r : train) {
            miu += r.score;
        }
        if (train.size() > 1) {
            miu /= train.size();
        }

        double mae = 0;
        double rmse = 0;
        for (Rating r : test) {
            double error = miu - r.score;
            mae += Math.abs(error);
            rmse += error * error;
        }

        if (test.size() > 0) {
            mae /= test.size();
            rmse = Math.sqrt(rmse / test.size());
        }
        logger.info("GlobalMean,mae,{},rmse,{}", mae, rmse);
        return new Tuple(rmse, mae);
    }


    /**
     * Description:使用用户评分均值来填充未知数据
     *
     * @param train 训练集
     * @param test  测试集
     * @return 返回评估指标对象
     */
    public static Tuple userMeanFilling(List<Rating> train, List<Rating> test) {
        double miu = 0;
        ConcurrentHashMap<Integer, List<Rating>> table = new ConcurrentHashMap<Integer, List<Rating>>();
        for (Rating r : train) {
            miu += r.score;
            if (!table.containsKey(r.userId)) {
                List<Rating> ratingList = new ArrayList<Rating>();
                ratingList.add(r);
                table.put(r.userId, ratingList);
            } else {
                List<Rating> ratingList = table.get(r.userId);
                ratingList.add(r);
                table.put(r.userId, ratingList);
            }
        }
        if (train.size() > 1) {
            miu /= train.size();
        }
        ConcurrentHashMap<Integer, Double> userMeanRatings = new ConcurrentHashMap<Integer, Double>();
        for (int userId : table.keySet()) {
            List<Rating> ratingList = table.get(userId);
            double userMeanRating = 0;
            for (Rating r : ratingList) {
                userMeanRating += r.score;
            }
            userMeanRating /= ratingList.size();
            userMeanRatings.put(userId, userMeanRating);
        }

        double mae = 0;
        double rmse = 0;

        for (Rating r : test) {
            double error = 0;
            if (userMeanRatings.containsKey(r.userId)) {
                error = (Double) userMeanRatings.get(r.userId) - r.score;
            } else {
                error = miu - r.score;
            }
            mae += Math.abs(error);
            rmse += error * error;
        }

        if (test.size() > 0) {
            mae /= test.size();
            rmse = Math.sqrt(rmse / test.size());
        }
        logger.info("UserMean,mae,{},rmse,{}", mae, rmse);
        return new Tuple(rmse, mae);
    }

    /**
     * Description:使用item评分均值来填充未知数据
     *
     * @param train 训练集
     * @param test  测试集
     * @return 返回评估指标对象
     */
    public static Tuple itemMeanFilling(List<Rating> train, List<Rating> test) {
        double miu = 0;
        ConcurrentHashMap<Integer, List<Rating>> table = new ConcurrentHashMap<Integer, List<Rating>>();
        for (Rating r : train) {
            miu += r.score;
            if (!table.containsKey(r.itemId)) {
                List<Rating> ratingList = new ArrayList<Rating>();
                ratingList.add(r);
                table.put(r.itemId, ratingList);
            } else {
                List<Rating> ratingList = table.get(r.itemId);
                ratingList.add(r);
                table.put(r.itemId, ratingList);
            }
        }
        if (train.size() > 1) {
            miu /= train.size();
        }
        ConcurrentHashMap<Integer, Double> itemMeanRatings = new ConcurrentHashMap<Integer, Double>();

        for (int itemId : table.keySet()) {
            List<Rating> ratingList = table.get(itemId);
            double itemMeanRating = 0;
            for (Rating r : ratingList) {
                itemMeanRating += r.score;
            }
            itemMeanRating /= ratingList.size();
            itemMeanRatings.put(itemId, itemMeanRating);
        }

        double mae = 0;
        double rmse = 0;

        for (Rating r : test) {
            double error = 0;
            if (itemMeanRatings.containsKey(r.itemId)) {
                error = itemMeanRatings.get(r.itemId) - r.score;
            } else {
                error = miu - r.score;
            }
            mae += Math.abs(error);
            rmse += error * error;
        }

        if (test.size() > 0) {
            mae /= test.size();
            rmse = Math.sqrt(rmse / test.size());
        }
        logger.info("ItemMean,mae,{},rmse,{}", mae, rmse);
        return new Tuple(rmse, mae);
    }


}
