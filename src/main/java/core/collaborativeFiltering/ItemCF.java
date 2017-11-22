package core.collaborativeFiltering;

import core.MathUtility;
import data.utility.Tools;
import entity.Rating;
import entity.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @version: 1.0
 * @author: Liujm
 * @site: https://github.com/liujm7
 * @contact: kaka206@163.com
 * @software: Idea
 * @date： 2017/11/15
 * @package_name: core.collaborativeFiltering
 */
public class ItemCF {
    final static Logger logger = LoggerFactory.getLogger(ItemCF.class);


    public double[] calculateItemMeanRating(List<Rating> ratings, int numberOfItems) {
        double[] itemMeanRatings = new double[numberOfItems];
        int[] itemRatingCount = new int[numberOfItems];
        for (Rating r : ratings) {
            itemMeanRatings[r.itemId] += r.score;
            itemRatingCount[r.itemId] += 1;
        }
        for (int i = 0; i < numberOfItems; i++) {
            if (itemRatingCount[i] > 0) {
                itemMeanRatings[i] = itemMeanRatings[i] / itemRatingCount[i];
            }
        }
        return itemMeanRatings;
    }

    protected double predict(double[] itemMeanRatings, double[][] similarities, double[][] ratingsMatrix,
                             int userId, int itemId) {
        double score = itemMeanRatings[itemId];
        int n = similarities.length;
        double numerator = 0;
        double denominator = 0;
        for (int i = 0; i < n; i++) {
            if (ratingsMatrix[userId][i] > 0 && similarities[itemId][i] > 0) {
                denominator += similarities[itemId][i];
                numerator += similarities[itemId][i] * (ratingsMatrix[userId][i] - itemMeanRatings[i]);
            }
        }
        score += (denominator > 0 ? numerator / denominator : 0);
        return score;
    }

    protected Tuple evaluateMaeRmse(double[] itemMeanRatings, double[][] similarities,
                                    double[][] ratingsMatrix, List<Rating> test) {
        double mae = 0.0;
        double rmse = 0.0;
        for (Rating r : test) {
            double pre = predict(itemMeanRatings, similarities, ratingsMatrix, r.userId, r.itemId);
            double error = pre - r.score;
            rmse += error * error;
            mae += Math.abs(error);
        }
        if (test.size() > 0) {
            rmse = Math.sqrt(rmse / test.size());
            mae /= test.size();
        }
        return new Tuple(mae, rmse);
    }


    public void itemCFMaeRmse(List<Rating> train, List<Rating> test, int maxUserId, int maxItemId) {
        double[] itemMeanRatings = calculateItemMeanRating(train, maxItemId + 1);
        double[][] trainMatrix = Tools.transfrom(train, maxUserId + 1, maxItemId + 1);
        double[][] similarities = MathUtility.computeSimilarity(MathUtility.transpose(trainMatrix), "Jaccard");   // PearsonCorrelation | Cosine

        Tuple result = evaluateMaeRmse(itemMeanRatings, similarities, trainMatrix, test);
        logger.info("ItemKNN,mae,{},rmse,{}", result.first, result.second);
    }

}
