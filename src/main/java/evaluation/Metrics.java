package evaluation;

import data.utility.Tools;
import entity.Rating;
import entity.RsTable;
import entity.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;

/**
 * @version: 1.0
 * @author: Liujm
 * @site: https://github.com/liujm7
 * @contact: kaka206@163.com
 * @software: Idea
 * @date： 2017/11/13
 * @package_name: evaluation
 */
public class Metrics {
    public static Tuple<Double, Double> computePrecisionAndRecall(List<Rating> recommendations, List<Rating> test) {
        RsTable recommendedTable = new RsTable();
        for (Rating r : recommendations) {
            if (!recommendedTable.containsKey(r.userId, r.itemId))
                recommendedTable.put(r.userId, r.itemId, r.score);
        }

        AtomicInteger hit = new AtomicInteger(0);
        for (Rating r : test) {
            if (recommendedTable.containsKey(r.userId, r.itemId)) {
                hit.getAndIncrement();
            }

        }
        Double precision = 0.0;
        Double recall = 0.0;
        System.out.println("命中数量:" + hit);
        System.out.println("推荐数量:" + recommendations.size());
        System.out.println("测试数量:" + test.size());
        if (recommendations.size() > 0) {
            precision = hit.doubleValue() / recommendations.size();
        }
        if (test.size() > 0) {
            recall = hit.doubleValue() / test.size();
        }

        return new Tuple<>(precision, recall);
    }

    public static Tuple<Double, Double> computeCoverageAndPopularity(List<Rating> recommendations, List<Rating> train) {

        ConcurrentHashMap trainTable = Tools.getItemUsersTable(train);
        ConcurrentHashMap recommendedTable = Tools.getItemUsersTable(recommendations);

        Double coverage = 0.0;
        if (trainTable.keySet().size() > 0) {
            coverage = recommendedTable.keySet().size() * 1.0 / trainTable.keySet().size();
        }

        Double popularity = 0.0;
        for (Rating r : recommendations) {
            if (r == null) {
                continue;
            }
            List<Rating> li = (List<Rating>) trainTable.get(r.itemId);
            if (li == null) {
                continue;
            }
            popularity += Math.log(1 + li.size());


        }
        popularity /= recommendations.size();

        return new Tuple<>(coverage, popularity);
    }


    public static double computeMAP(List<Rating> recommendations, List<Rating> test, int k) {
        ConcurrentHashMap recommendedRatings = Tools.getUserItemsTable(recommendations);
        RsTable testTable = Tools.getRatingTable(test);
        AtomicInteger validateUserCounter = new AtomicInteger(0);
        double MAP = 0;
        for (Object userId : testTable.keys()) {
            if (testTable.containsMainKey(userId)) {
                List<Rating> recommendedUserRatings = (List<Rating>) recommendedRatings.get(userId);
                ConcurrentHashMap testUserRatings = (ConcurrentHashMap) testTable.get(userId);
                if (recommendedUserRatings == null || recommendedUserRatings.size() < 1) {
                    continue;
                }
                int length = Math.min(k, recommendedUserRatings.size());

                int[] accuracy = new int[length];
                int correctlyPredictItems = 0;
                for (int i = 0; i < length; i++) {
                    if (testUserRatings.containsKey(recommendedUserRatings.get(i).itemId)) {
                        correctlyPredictItems++;
                        accuracy[i] = correctlyPredictItems;
                    }

                }
                if (correctlyPredictItems > 0) {
                    double averagePrecisionPerUser = 0.0;
                    for (int i = 0; i < length; i++) {
                        averagePrecisionPerUser += (accuracy[i] * 1.0 / (i + 1));
                    }
                    averagePrecisionPerUser /= length;
                    MAP += averagePrecisionPerUser;
                }
                validateUserCounter.getAndIncrement(); // recommended users which also in test
            }
        }
        if (validateUserCounter.get() > 0) {
            return MAP / validateUserCounter.get();
        }
        return 0.0;
    }

    public static void MAPTest() {
        List<Rating> test = new ArrayList<>();
        test.add(new Rating(1, 1, 1));
        test.add(new Rating(1, 2, 1));
        test.add(new Rating(1, 3, 1));
        test.add(new Rating(1, 4, 1));
        test.add(new Rating(1, 5, 1));


        List<Rating> recommendations = new ArrayList<>();
        recommendations.add(new Rating(1, 6, 1));
        recommendations.add(new Rating(1, 7, 1));
        recommendations.add(new Rating(1, 8, 1));
        recommendations.add(new Rating(1, 9, 1));
        recommendations.add(new Rating(1, 10, 1));

        for (int i = 1; i <= 5; i++) {
            double MAP = computeMAP(recommendations, test, i);
            System.out.println("MAP@" + i + "," + MAP);
        }

    }

    public static void main(String[] args) {
        MAPTest();
    }
}
