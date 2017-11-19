package data;

import core.baseline.MeanFilling;
import core.collaborativeFiltering.BiasedMatrixFactorization;
import core.collaborativeFiltering.FriendMatrixFactorization;
import core.collaborativeFiltering.MatrixFactorization;
import core.collaborativeFiltering.SVDPlusPlus;
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

    public static void main(String[] args) {
        List<Rating> baseRatings = Tools.getRatings(trainRatingFile);
        List<Rating> testRatings = Tools.getRatings(testRatingFile);

        Tools.updateIndexesToZeroBased(baseRatings);
        Tools.updateIndexesToZeroBased(testRatings);
//        UserKNNv2 userKNNv2 =new UserKNNv2();
//        userKNNv2.testTopNRecommend(baseRatings,testRatings);
//        ItemKNNv2 itemKNNv2 = new ItemKNNv2();
//        itemKNNv2.testTopNRecommend(baseRatings,testRatings);
//        Tuple tuple=Tools.getMaxUserIdAndItemId(baseRatings);
        BiasedMatrixFactorization matrixFactorization = new BiasedMatrixFactorization(maxUserId, maxItemId, 20, "uniform_df");
        for (double gamma = 0.01; gamma < 0.1; gamma += 0.01)
            matrixFactorization.testSGDForTopN(baseRatings, testRatings, 500, gamma, 0.01, 0.99, 1, 5);
//        matrixFactorization.testAlsForTopN(baseRatings,testRatings);
        //        meanFillingTest(baseRatings,testRatings);
//        userKNNTest(baseRatings,testRatings);
//        itemKNNTest(baseRatings, testRatings);
//        matrixFactorizationTest(baseRatings,testRatings);
//        biasedMatrixFactorizationTest(baseRatings,testRatings);
//        SVDPlusPlusTest(baseRatings,testRatings);

//        AlternatingLeastSquaresTest(baseRatings,testRatings);
//        EuclideanEmbeddingTest(baseRatings, testRatings);
    }
}
