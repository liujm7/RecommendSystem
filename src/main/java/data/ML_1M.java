package data;

import core.baseline.MeanFilling;
import core.baseline.Baseline;
import core.collaborativeFiltering.*;
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
 * @date： 2017/11/16
 * @package_name: data
 */
public class ML_1M {
    final static Logger logger = LoggerFactory.getLogger(ML_10M.class);


    public static String defalultDirectory = "D:\\学习资源\\推荐算法数据\\ml-1m\\ml-1m\\";
    public static String defaultRatingFile = defalultDirectory + "ratings.dat";
    public static String defalutItemFile = defalultDirectory + "movies.dat";

    public static String trainRatingFile = defalultDirectory + "u1.train";
    public static String testRatingFile = defalultDirectory + "u1.test";

    public static int maxUserId = 6040;
    public static int maxItemId = 3952;


    public static void updateDataInformation() {
        List<Rating> ratings = Tools.getRatings(defaultRatingFile, "::");
        Tuple q = Tools.getMaxUserIdAndItemId(ratings);
        maxUserId = (int) q.first;
        maxItemId = (int) q.second;
        logger.info("maxUserId:{},maxItemId:{}", maxUserId, maxItemId);
    }

    public static void spilt(double testSize) {
        List<Rating> ratings = Tools.getRatings(defaultRatingFile, "::");
        Tuple<List<Rating>, List<Rating>> data = Tools.trainAndTestSplit(ratings, testSize);
        Tools.writeTimedRatings(data.first, trainRatingFile, "\t");
        Tools.writeTimedRatings(data.second, testRatingFile, "\t");
        logger.info("切割结束.");
    }

    public static void main(String[] args) {
//        spilt(0.5);
        List<Rating> baseRatings = Tools.getRatings(trainRatingFile);
        List<Rating> testRatings = Tools.getRatings(testRatingFile);
        Tools.updateIndexesToZeroBased(baseRatings);
        Tools.updateIndexesToZeroBased(testRatings);
        SVDPlusPlus euclideanEmbedding = new SVDPlusPlus(maxUserId, maxItemId, 100, "uniform_df");
        euclideanEmbedding.testSGDForTopN(baseRatings, testRatings, 20, 0.02, 0.01, 1, 1, 5);

    }
}
