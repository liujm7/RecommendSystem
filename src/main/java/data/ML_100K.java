package data;

import core.baseline.MeanFilling;
import core.collaborativeFiltering.*;
import data.utility.Tools;
import entity.Rating;
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
public class ML_100K {
    final static Logger logger = LoggerFactory.getLogger(ML_10M.class);


    public static String defalultDirectory = "D:\\学习资源\\推荐算法数据\\ml-100k\\ml-100k\\";
    public static String defaultRatingFile = defalultDirectory + "u.data";
    public static String defalutItemFile = defalultDirectory + "u.item";
    public static String defalutUserFile = defalultDirectory + "u.user";


    public static String baseRatingFile = defalultDirectory + "u1.base";
    public static String testRatingFile = defalultDirectory + "u1.test";

    public static int maxUserId = 943;
    public static int maxItemId = 1682;


    public static void meanFillingTest(List<Rating> baseRatings, List<Rating> testRatings) {
        MeanFilling.globalMeanFilling(baseRatings, testRatings);
        MeanFilling.userMeanFilling(baseRatings, testRatings);
        MeanFilling.itemMeanFilling(baseRatings, testRatings);
    }

    public static void itemKNNTest(List<Rating> baseRatings, List<Rating> testRatings) {
        ItemCF knn = new ItemCF();
        knn.itemCFMaeRmse(baseRatings, testRatings, maxUserId, maxItemId);
    }


    public static void userKNNTest(List<Rating> baseRatings, List<Rating> testRatings) {
        UserCF knn = new UserCF();
        knn.userCFMaeRmse(baseRatings, testRatings, maxUserId, maxItemId);
    }

    public static void matrixFactorizationTest(List<Rating> baseRatings, List<Rating> testRatings) {
        MatrixFactorization matrixFactorization = new MatrixFactorization(maxUserId, maxItemId, 10, "uniform");
        matrixFactorization.SGD(baseRatings, testRatings, 500, 0.01, 0.01, 1, 1, 5);
    }

    public static void biasedMatrixFactorizationTest(List<Rating> baseRatings, List<Rating> testRatings) {
        BiasedMatrixFactorization matrixFactorization = new BiasedMatrixFactorization(maxUserId, maxItemId, 10, "uniform");
        matrixFactorization.SGD(baseRatings, testRatings, 500, 0.01, 0.01, 1, 1, 5);
    }

    public static void SVDPlusPlusTest(List<Rating> baseRatings, List<Rating> testRatings) {
        SVDPlusPlus matrixFactorization = new SVDPlusPlus(maxUserId, maxItemId, 10, "uniform");
        matrixFactorization.SGD(baseRatings, testRatings, 500, 0.01, 0.01, 1, 1, 5);
    }


    public static void AlternatingLeastSquaresTest(List<Rating> baseRatings, List<Rating> testRatings) {
        AlternatingLeastSquares matrixFactorization = new AlternatingLeastSquares(maxUserId, maxItemId, 10, "uniform");
        matrixFactorization.ALS(baseRatings, testRatings, 500, 0.01, 1, 5);
    }

    public static void EuclideanEmbeddingTest(List<Rating> baseRatings, List<Rating> testRatings) {
        EuclideanEmbedding euclideanEmbedding = new EuclideanEmbedding(maxUserId, maxItemId, 10, "uniform");
        euclideanEmbedding.SGD(baseRatings, testRatings, 500, 0.01, 0.01, 1, 1, 5);
    }


    public static void main(String[] args) {
        List<Rating> baseRatings = Tools.getRatings(baseRatingFile);
        List<Rating> testRatings = Tools.getRatings(testRatingFile);

         Tools.updateIndexesToZeroBased(baseRatings);
         Tools.updateIndexesToZeroBased(testRatings);
//        UserKNN userKNNv2 =new UserKNN();
//        userKNN.testTopNRecommend(baseRatings,testRatings);
        ItemKNN itemKNNv2 = new ItemKNN();
        itemKNNv2.testTopNRecommend(baseRatings,testRatings);
//        Tuple tuple=Tools.getMaxUserIdAndItemId(baseRatings);
//        FriendMatrixFactorization matrixFactorization = new FriendMatrixFactorization(maxUserId, maxItemId, 20, "uniform");
//        matrixFactorization.testSGDForTopN(baseRatings, testRatings, 100, 0.01, 0.01, 1, 1, 5);
////        matrixFactorization.testAlsForTopN(baseRatings,testRatings);
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
