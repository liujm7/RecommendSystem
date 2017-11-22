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
public class ItemKNN {
    final static Logger logger = LoggerFactory.getLogger(ItemKNN.class);

    /**
     * Description: 计算两个商品共同被一个用户同时购买的次数
     *
     * @param userItemsTable 用户-物品 hashtable
     * @return 两个商品的相似性
     */
    protected RsTable calculateCoOccurrences(ConcurrentHashMap<Integer, List<Rating>> userItemsTable) {
        RsTable cooccurrences = new RsTable();
        //遍历用户id
        for (int userId : userItemsTable.keySet()) {
            List<Rating> items = userItemsTable.get(userId);
            for (Rating i : items) {
                for (Rating j : items) {
                    if (i.itemId == j.itemId) {
                        continue;
                    }
                    if (!cooccurrences.containsKey(i.itemId, j.itemId)) {
                        cooccurrences.put(i.itemId, j.itemId, 1.0 / Math.log(items.size() + 1));//降低热门用户的影响
                    } else {
                        double value = (double) cooccurrences.get(i.itemId, j.itemId);
                        cooccurrences.put(i.itemId, j.itemId, value + 1.0 / Math.log(items.size() + 1));
                    }
                }
            }
        }

        return cooccurrences;
    }


    /**
     * Description: 计算两个商品的Jccard相似性
     *
     * @param coourrencesTable 两个用户的的相似性分子rstable
     * @param itemUsersTable   商品-用户评分hashtable
     * @return 商品相似性 rstable
     */
    protected RsTable calculateSimilarities(RsTable coourrencesTable, ConcurrentHashMap<Integer, List<Rating>> itemUsersTable) {
        RsTable wuv = new RsTable();
        //遍历每个商品
        for (Object iItemId : coourrencesTable.keys()) {
            int iId = (Integer) iItemId;
            ConcurrentHashMap subTable = (ConcurrentHashMap) coourrencesTable.get(iItemId);
            //获取该商品id的所有评分--获取评分用户数量
            List<Rating> iRatings = itemUsersTable.get(iId);
            for (Object jItemId : subTable.keySet()) {
                if (iItemId == jItemId) {
                    continue;
                }
                //获取该商品id的所有评分--获取评分用户数量
                List<Rating> jRatings = itemUsersTable.get((int) jItemId);
                //计算商品相似性
                wuv.put(iItemId, jItemId, (double) coourrencesTable.get(iItemId, jItemId) * 1.0
                        / Math.sqrt(iRatings.size() + jRatings.size()));
            }
        }
        return wuv;
    }

    /**
     * Description: 获取商品的topK相似商品
     *
     * @param wTable 商品相似性rsTable
     * @param itemId 商品id
     * @param K      KNN的K
     * @return List<Link> 某商品的相似性列表
     */
    protected List<Link> getSimilarItems(RsTable wTable, int itemId, int K) {
        if (K < 1)
            K = 80;
        //获取itemId对应的所有相似商品和相似商品对应的相似性
        ConcurrentHashMap subTable = (ConcurrentHashMap) wTable.get(itemId);
        List<Link> weights = new ArrayList<>();
        //遍历商品id
        for (Object vId : subTable.keySet()) {
            double w = (double) subTable.get(vId);
            Link l = new Link(itemId, (Integer) vId, w);
            weights.add(l);
        }

        //排序
        Collections.sort(weights);
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
        List<Object> itemsList = ratingTable.getSubKeyList();
        ConcurrentHashMap<Object, List<Link>> similarItemsMap = new ConcurrentHashMap<>();
        for (Object items : itemsList) {
            List<Link> similarItems = getSimilarItems(W, (Integer) items, K);
            similarItemsMap.put(items, similarItems);
        }

        //遍历评分表

        for (Object userId : ratingTable.keys()) { //O(N * M * K)
            //获取目前用户已经进行评分的商品 - 用于过滤
            ConcurrentHashMap<Object, Object> Nu = (ConcurrentHashMap) ratingTable.get(userId);

            for (Object itemId : Nu.keySet()) {
                //获取相似的商品列表
                List<Link> similarItems = similarItemsMap.get(itemId);
                for (Link l : similarItems) {
                    int iId = l.to;
                    //过滤已经评分的商品
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
            ConcurrentHashMap subTable = (ConcurrentHashMap) recommendedTable.get(uId);
            List<Rating> li = new ArrayList<>();
            for (Object iId : subTable.keySet()) {
                double t = (double) subTable.get(iId);
                li.add(new Rating((Integer) uId, (Integer) iId, t));
            }
            Collections.sort(li);
            recommendedItems.addAll(li.subList(0, Math.min(li.size(), N)));
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
        ConcurrentHashMap itemUsersTable = Tools.getItemUsersTable(train);
        ConcurrentHashMap userItemsTable = Tools.getUserItemsTable(train);

        RsTable cooccurrences = calculateCoOccurrences(userItemsTable);
        RsTable wuv = calculateSimilarities(cooccurrences, itemUsersTable);

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
        ConcurrentHashMap itemUsersTable = Tools.getItemUsersTable(train);
        ConcurrentHashMap userItemsTable = Tools.getUserItemsTable(train);

        RsTable coourrrenceTable = calculateCoOccurrences(userItemsTable);
        RsTable wuv = calculateSimilarities(coourrrenceTable, itemUsersTable);

        RsTable ratingTable = Tools.getRatingTable(train);

//        List<Integer> Ns = new ArrayList<>(Arrays.asList(1, 5, 10, 15, 20, 25, 30));
        List<Integer> Ns = new ArrayList<>(Arrays.asList(160));

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
