package core.collaborativeFiltering;

import data.utility.Tools;
import entity.Link;
import entity.Rating;
import entity.RsTable;
import entity.Tuple;
import evaluation.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version: 1.0
 * @author: Liujm
 * @site: https://github.com/liujm7
 * @contact: kaka206@163.com
 * @software: Idea
 * @date： 2017/11/13
 * @package_name: algorithm
 */
public class UserKNN {
    final static Logger logger = LoggerFactory.getLogger(UserKNN.class);

    /**
     * Description: 两个用户 都够买了物品的次数
     *
     * @param itemUsersTable 物品-用户 hashtable
     * @return 两个用户的的相似性分子
     */
    protected RsTable calculateCoOccurrences(ConcurrentHashMap<Integer, List<Rating>> itemUsersTable) {
        RsTable cooccurrences = new RsTable();
        for (int itemsId : itemUsersTable.keySet()) {
            List<Rating> usersRating = itemUsersTable.get(itemsId);
            for (Rating u : usersRating) {
                for (Rating v : usersRating) {
                    if (u.userId == v.userId) {
                        continue;
                    }
                    if (!cooccurrences.containsKey(u.userId, v.userId)) {
                        cooccurrences.put(u.userId, v.userId, 1 / Math.log(usersRating.size() + 1));
                    } else {
                        double value = (double) cooccurrences.get(u.userId, v.userId);
                        cooccurrences.put(u.userId, v.userId, value + 1 / Math.log(usersRating.size() + 1));
                    }
                }
            }
        }

        return cooccurrences;
    }

    /**
     * Description: 计算两个用户的余弦相似性
     *
     * @param coourrencesTable 两个用户的的相似性分子rstable
     * @param userItemsTable   用户-商品评分hashtable
     * @return 用户相似性 rstable
     */
    protected RsTable calculateSimilarities(RsTable coourrencesTable, ConcurrentHashMap<Integer, List<Rating>> userItemsTable) {
        RsTable wuv = new RsTable();
        for (Object uUserId : coourrencesTable.keys()) {
            ConcurrentHashMap subTable = (ConcurrentHashMap) coourrencesTable.get(uUserId);
            List<Rating> uRatings = userItemsTable.get((int) uUserId);
            for (Object vUserId : subTable.keySet()) {
                double coourrences = (double) subTable.get((int) vUserId);
                List<Rating> vRatings = userItemsTable.get((int) vUserId);
                //余弦相似性
                wuv.put(uUserId, vUserId, coourrences / Math.sqrt(uRatings.size() + vRatings.size()));
            }
        }

        return wuv;
    }

    /**
     * Description: 获取用户的topK相似用户
     *
     * @param wTable 用户相似性rsTable
     * @param userId 用户id
     * @param K      KNN的K
     * @return List<Link> 某用户的相似性列表
     */
    protected List<Link> getSimilarUsers(RsTable wTable, int userId, int K) {
        if (K < 1)
            K = 80;
        List<Link> weights = new ArrayList<>();
        ConcurrentHashMap subTable = (ConcurrentHashMap) wTable.get(userId);
        for (Object vId : subTable.keySet()) {
            double w = (double) subTable.get(vId);
            Link l = new Link(userId, (Integer) vId, w);
            weights.add(l);
        }
        //排序
        Collections.sort(weights);
//                , new Comparator<Link>() {
//            @Override
//            public int compare(Link o1, Link o2) {
//                return o1.compareTo(o2);
//            }
//        });
        return weights.subList(0, Math.min(K, weights.size()));
    }

    /**
     * Description: 获取top-N推荐列表，KNN算法，
     *
     * @param ratingTable 评分表
     * @param W           用户相似性表
     * @param K           K个相似性用户
     * @param N           top-N个推荐列表
     * @return 返回整体的推荐性列表
     */
    protected List<Rating> getRecommendations(RsTable ratingTable, RsTable W, int K, int N) {
        if (N < 1)
            N = 10;

        RsTable recommendedTable = new RsTable();
        for (Object userId : ratingTable.keys()) { //O(N*K)
            ConcurrentHashMap Nu = new ConcurrentHashMap((ConcurrentHashMap) ratingTable.get(userId));
            List<Link> simlilarUsers = getSimilarUsers(W, (Integer) userId, K);
            for (Link l : simlilarUsers) {
                int vId = l.to;
                ConcurrentHashMap Nv = new ConcurrentHashMap((ConcurrentHashMap) ratingTable.get(vId));
                for (Object iId : Nv.keySet()) {
                    if (Nu.containsKey(iId)) {
                        continue;
                    }
                    if (recommendedTable.containsKey(userId, iId)) {
                        double t = (double) recommendedTable.get(userId, iId);
                        recommendedTable.put(userId, iId, t + l.weight);
                    } else {
                        recommendedTable.put(userId, iId, l.weight);
                    }
                }
            }
        }

        List<Rating> recommendedItems = new ArrayList<>();
        for (Object uId : recommendedTable.keys()) {
            List<Rating> li = new ArrayList<>();
            ConcurrentHashMap subTable = (ConcurrentHashMap) recommendedTable.get(uId);
            for (Object iId : subTable.keySet()) {
                double t = (double) subTable.get(iId);
                li.add(new Rating((Integer) uId, (Integer) iId, t));
            }
            Collections.sort(li, new Comparator<Rating>() {
                @Override
                public int compare(Rating o1, Rating o2) {
                    return o1.compareTo(o2);
                }
            });
            for (int i = 0; i < Math.min(li.size(), N); i++) {
                recommendedItems.add(li.get(i));
            }
        }
        return recommendedItems;

    }

    /**
     * Description:topN推荐，并输出
     *
     * @param train 训练集
     * @param test  测试集
     * @param K     KNN算法的K值
     * @param N     推荐列表的数量
     */
    public void topNRecommend(List<Rating> train, List<Rating> test, int K, int N) {
        ConcurrentHashMap userItemsTable = Tools.getUserItemsTable(train);
        ConcurrentHashMap itemUsersTable = Tools.getItemUsersTable(train);

        RsTable cooccurrences = calculateCoOccurrences(itemUsersTable);
        RsTable wuv = calculateSimilarities(cooccurrences, userItemsTable);

        RsTable ratingTable = Tools.getRatingTable(train);

        List<Rating> recommendations = getRecommendations(ratingTable, wuv, K, N);

        Tuple<Double, Double> precisionAndRecall = Metrics.computePrecisionAndRecall(recommendations, test);
        Tuple<Double, Double> coverageAndPopularity = Metrics.computeCoverageAndPopularity(recommendations, test);

        logger.info("K(Cosine){},N:{},precision:{},recall:{},Coverage:{},Popularity:{}", K, N
                , precisionAndRecall.first, precisionAndRecall.second, coverageAndPopularity.first, coverageAndPopularity.second);

    }

    /**
     * Description: 测试推荐效果
     *
     * @param train 训练集
     * @param test  测试集
     */
    public void testTopNRecommend(List<Rating> train, List<Rating> test) {
        ConcurrentHashMap userItemsTable = Tools.getUserItemsTable(train);
        ConcurrentHashMap itemUsersTable = Tools.getItemUsersTable(train);

        RsTable coourrrenceTable = calculateCoOccurrences(itemUsersTable);
        RsTable wuv = calculateSimilarities(coourrrenceTable, userItemsTable);

        RsTable ratingTable = Tools.getRatingTable(train);

        //        List<Integer> Ns = new ArrayList<>(Arrays.asList(1, 5, 10, 15, 20, 25, 30));
        List<Integer> Ns = new ArrayList<>(Arrays.asList(80));

        List<Integer> Ks = new ArrayList<>(Arrays.asList(5, 10, 20, 40, 80, 160));
//        List<Integer> Ks = new ArrayList<>(Arrays.asList(80));
        for (int k : Ks) {
            for (int n : Ns) {
                List<Rating> recommendations = getRecommendations(ratingTable, wuv, k, n);
                Tuple precisionAndRecall = Metrics.computePrecisionAndRecall(recommendations, test);
                Tuple coverageAndPopularity = Metrics.computeCoverageAndPopularity(recommendations, train);
                logger.info("K(Cosine){},N:{},precision:{},recall:{},Coverage:{},Popularity:{}", k, n
                        , precisionAndRecall.first, precisionAndRecall.second, coverageAndPopularity.first, coverageAndPopularity.second);
            }
        }
    }


}
