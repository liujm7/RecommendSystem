package data;

import core.baseline.MeanFilling;
import core.collaborativeFiltering.BiasedMatrixFactorization;
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
 * @package_name: data.utility
 */
public class ML_10M {
    final static Logger logger = LoggerFactory.getLogger(ML_10M.class);


    public static String defalultDirectory = "D:\\学习资源\\推荐算法数据\\ml-10m\\ml-10M100K\\";
    public static String defaultRatingFile = defalultDirectory + "ratings.dat";
    public static String defalutItemFile = defalultDirectory + "movies.dat";

    public static String trainRatingFile = defalultDirectory + "u1.train";
    public static String testRatingFile = defalultDirectory + "u1.test";

    public static int maxUserId = 71567;
    public static int maxItemId = 65133;


    public static void updateDataInformation() {
        List<Rating> ratings = Tools.getRatings(defaultRatingFile, "::");
        Tuple q = Tools.getMaxUserIdAndItemId(ratings);
        maxUserId = (int) q.first;
        maxItemId = (int) q.second;
        logger.info("maxUserId:{},maxItemId:{}", maxUserId, maxItemId);
    }

    public static void spilt() {
        List<Rating> ratings = Tools.getRatings(defaultRatingFile, "::");
        Tuple<List<Rating>, List<Rating>> data = Tools.trainAndTestSplit(ratings, 0.2);
        Tools.writeTimedRatings(data.first, trainRatingFile, "\t");
        Tools.writeTimedRatings(data.second, testRatingFile, "\t");
        logger.info("切割结束.");
    }

    public static void meanFillingTest() {
        List<Rating> trainRating = Tools.getRatings(trainRatingFile, "\t");
        List<Rating> testRating = Tools.getRatings(testRatingFile, "\t");
        Tools.updateIndexesToZeroBased(trainRating);
        Tools.updateIndexesToZeroBased(testRating);

        MeanFilling.globalMeanFilling(trainRating, testRating);
        MeanFilling.itemMeanFilling(trainRating, testRating);
        MeanFilling.userMeanFilling(trainRating, testRating);
    }

    /**
     * 目前本机内存溢出，未测试
     *
     * @param args
     */
    public static void main(String[] args) {
//        updateDataInformation();
//        spilt();
//        meanFillingTest();
        List<Rating> baseRatings = Tools.getRatings(trainRatingFile);
        List<Rating> testRatings = Tools.getRatings(testRatingFile);

        Tools.updateIndexesToZeroBased(baseRatings);
        Tools.updateIndexesToZeroBased(testRatings);
        BiasedMatrixFactorization euclideanEmbedding = new BiasedMatrixFactorization(maxUserId, maxItemId, 50, "uniform_df");
        euclideanEmbedding.testSGDForTopN(baseRatings, testRatings, 350, 0.0008, 0.001, 1, 1, 5);

    }
}
