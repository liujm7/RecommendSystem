package core.collaborativeFiltering;

import core.MathUtility;
import data.utility.Tools;
import entity.Link;
import entity.Rating;
import entity.RsTable;
import entity.Tuple;
import evaluation.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version: 1.0
 * @author: Liujm
 * @site: https://github.com/liujm7
 * @contact: kaka206@163.com
 * @software: Idea
 * @date： 2017/11/12
 * @package_name: algorithm
 */
public class UserCF {
    final static Logger logger = LoggerFactory.getLogger(UserCF.class);


    public double[] calculateUserMeanRating(List<Rating> ratings, int numberOfUsers) {
        double[] userMeanRating = new double[numberOfUsers];
        int[] userRatingCount = new int[numberOfUsers];
        for (Rating r : ratings) {
            userMeanRating[r.userId] += r.score;
            userRatingCount[r.userId] += 1;
        }
        for (int i = 0; i < numberOfUsers; i++) {
            if (userRatingCount[i] > 0) {
                userMeanRating[i] = userMeanRating[i] / userRatingCount[i];
            }
        }
        return userMeanRating;
    }

    protected double predict(double[] userMeanRatings, double[][] similarities, double[][] ratingsMatrix,
                             int userId, int itemId) {
        double score = userMeanRatings[userId];
        int n = similarities.length;
        double numerator = 0;
        double denominator = 0;
        for (int i = 0; i < n; i++) {
            if (ratingsMatrix[i][itemId] > 0 && similarities[userId][i] > 0) {
                numerator += similarities[userId][i] * (ratingsMatrix[i][itemId] - userMeanRatings[i]);
                denominator += similarities[userId][i];
            }
        }
        score += (denominator > 0 ? numerator / denominator : 0);
        return score;
    }

    protected Tuple evaluateMaeRmse(double[] userMeanRating, double[][] similarities,
                                    double[][] ratingsMatrix, List<Rating> test) {
        double mae = 0.0;
        double rmse = 0.0;
        for (Rating r : test) {
            double pre = predict(userMeanRating, similarities, ratingsMatrix, r.userId, r.itemId);
            double error = pre - r.score;
            mae += Math.abs(error);
            rmse += error * error;
        }
        if (test.size() > 0) {
            mae /= test.size();
            rmse = Math.sqrt(rmse / test.size());
        }
        return new Tuple(mae, rmse);
    }

    public void userCFMaeRmse(List<Rating> train, List<Rating> test, int maxUserId, int maxItemId) {
        double[] userMeanRating = calculateUserMeanRating(train, maxUserId + 1);
        double[][] trainMatrix = Tools.transfrom(train, maxUserId + 1, maxItemId + 1);
        double[][] similarities = MathUtility.computeSimilarity(trainMatrix, "PearsonCorrelation");   // PearsonCorrelation | Cosine

        Tuple result = evaluateMaeRmse(userMeanRating, similarities, trainMatrix, test);
        logger.info("userCF,mae,{},rmse,{}", result.first, result.second);
    }


}
