package data.utility;

import entity.Link;
import entity.Rating;
import entity.RsTable;
import entity.Tuple;


import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version: 1.0
 * @author: Liujm
 * @site: https://github.com/liujm7
 * @contact: kaka206@163.com
 * @software: Idea
 * @date： 2017/11/13
 * @package_name: data.utility
 */
public class Tools {
    /**
     * Description:读取数据，将数据转化成Rating对象的List
     *
     * @param filePath 数据文件路径
     * @return List<Rating>对象
     */
    public static List<Rating> getRatings(String filePath) {
        return getRatings(filePath, "\t");
    }

    /**
     * Description:读取数据，将数据转化成Rating对象的List
     *
     * @param readFilePath 数据文件路径
     * @param separator    数据内容分隔符
     * @return List<Rating>对象
     */
    public static List<Rating> getRatings(String readFilePath, String separator) {
        File file = new File(readFilePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File doesn't exist:" + readFilePath);
        }
        List<Rating> ratings = new ArrayList<>();
        int count = 0;
        try {
            Scanner in = new Scanner(file);
            while (in.hasNext()) {
                count++;
                if (count % 100000 == 0) {
                    System.out.println("counts:" + count);
                }
                String str = in.nextLine();
                String[] elements = str.split(separator);
                if (elements.length == 3) {
                    int userId = Integer.parseInt(elements[0]);
                    int itemId = Integer.parseInt(elements[1]);
                    double score = Double.parseDouble(elements[2]);
                    Rating r = new Rating(userId, itemId, score);
                    ratings.add(r);
                } else if (elements.length == 4) {
                    int userId = Integer.parseInt(elements[0]);
                    int itemId = Integer.parseInt(elements[1]);
                    double score = Double.parseDouble(elements[2]);
                    Rating r = new Rating(userId, itemId, score, elements[3]);
                    ratings.add(r);

                } else if (elements.length == 2) {
                    int userId = Integer.parseInt(elements[0]);
                    int itemId = Integer.parseInt(elements[1]);
                    Rating r = new Rating(userId, itemId, 1.0);
                    ratings.add(r);
                }

            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return ratings;
    }

    /**
     * Description:保存数据
     *
     * @param ratings      评分数据列表
     * @param saveFilePath 保存的数据路径
     */
    public static void writeRatings(List<Rating> ratings, String saveFilePath) {
        writeRatings(ratings, saveFilePath, ",", false, "utf-8");
    }

    /**
     * Description:保存数据
     *
     * @param ratings      评分数据列表
     * @param saveFilePath 保存的数据路径
     * @param separator    保存数据分隔符
     * @param append       数据是否追加
     * @param encoding     数据写入编码格式
     */
    public static void writeRatings(List<Rating> ratings, String saveFilePath, String separator
            , boolean append, String encoding) {
        if (ratings == null) {
            throw new NullPointerException();
        }
        try {
            File file = new File(saveFilePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            OutputStreamWriter oStreamWriter = new OutputStreamWriter(new FileOutputStream(file, append), encoding);
            for (Rating r : ratings) {
                String content = r.userId + separator + r.itemId + separator + r.score + "\n";
                oStreamWriter.append(content);
                oStreamWriter.flush();
            }
            oStreamWriter.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Description:读取时间评分数据
     *
     * @param readFilePath 数据文件路径
     * @param separator    数据内容分隔符
     * @return List<Rating>对象
     */
    public static List<Rating> getTimedRatings(String readFilePath, String separator) {
        return getRatings(readFilePath, separator);
    }


    /**
     * Description:写入时间评分数据
     *
     * @param ratings   评分数据列表
     * @param toFile    保存的数据路径
     * @param separator 保存数据分隔符
     */
    public static void writeTimedRatings(List<Rating> ratings, String toFile, String separator) {
        writeTimedRatings(ratings, toFile, separator, false, "utf-8");
    }

    /**
     * Description:写入时间评分数据
     *
     * @param ratings   评分数据列表
     * @param toFile    保存的数据路径
     * @param separator 保存数据分隔符
     * @param append    数据是否追加
     * @param encoding  数据写入编码格式
     */
    public static void writeTimedRatings(List<Rating> ratings, String toFile, String separator, boolean append, String encoding) {
        if (ratings == null) {
            throw new NullPointerException();
        }
        try {
            File file = new File(toFile);
            if (!file.exists()) {
                file.createNewFile();
            }
            OutputStreamWriter oStreamWriter = new OutputStreamWriter(new FileOutputStream(file, append), encoding);
            for (Rating r : ratings) {
                String content = r.userId + separator + r.itemId + separator + r.score + separator + r.timestamp + "\n";
                oStreamWriter.append(content);
                oStreamWriter.flush();
            }
            oStreamWriter.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Description:列表的id减1
     *
     * @param ratings 评分列表
     */
    public static void updateIndexesToZeroBased(List<Rating> ratings) {
        for (Rating r : ratings) {
            r.userId -= 1;
            r.itemId -= 1;
        }
    }

    /**
     * Description:列表的id减1
     *
     * @param links 关系列表
     */
    public static void updateLinkIndexesToZeroBased(List<Link> links) {
        for (Link l : links) {
            l.from -= 1;
            l.to -= 1;
        }
    }

    /**
     * Description:对评分列表按照用户分组
     *
     * @param ratings 评分列表
     * @return 返回按照用户分组后的hashtable
     */
    public static ConcurrentHashMap getUserItemsTable(List<Rating> ratings) {
        ConcurrentHashMap userItemsTable = new ConcurrentHashMap();
        for (Rating r : ratings) {
            if (userItemsTable.containsKey(r.userId)) {
                List<Rating> li = (List<Rating>) userItemsTable.get(r.userId);
                li.add(r);
            } else {
                List<Rating> li = new ArrayList<>();
                li.add(r);
                userItemsTable.put(r.userId, li);
            }
        }
        return userItemsTable;
    }

    /**
     * Description:对评分列表按照商品分组
     *
     * @param ratings 评分列表
     * @return 返回按照商品分组后的hashtable
     */
    public static ConcurrentHashMap getItemUsersTable(List<Rating> ratings) {
        ConcurrentHashMap itemsUsersTable = new ConcurrentHashMap();
        for (Rating r : ratings) {
            if (itemsUsersTable.containsKey(r.itemId)) {
                List<Rating> li = (List<Rating>) itemsUsersTable.get(r.itemId);
                li.add(r);
                itemsUsersTable.put(r.itemId, li);
            } else {
                List<Rating> li = new ArrayList<>();
                li.add(r);
                itemsUsersTable.put(r.itemId, li);
            }
        }
        return itemsUsersTable;
    }

    /**
     * Description:将评分列表转化成自定义的双重hashtable (用户-商品-评分)
     *
     * @param ratings 评分列表
     * @return 双重hashtable
     */
    public static RsTable getRatingTable(List<Rating> ratings) {
        RsTable table = new RsTable();
        for (Rating r : ratings) {
            if (!table.containsKey(r.userId, r.itemId)) {
                table.put(r.userId, r.itemId, r.score);
            }
        }
        return table;
    }

    /**
     * Description:将评分列表转化成自定义的双重hashtable (商品-用户-评分)
     *
     * @param ratings 评分列表
     * @return 双重hashtable
     */
    public static RsTable getReversedRatingTable(List<Rating> ratings) {
        RsTable table = new RsTable();
        for (Rating r : ratings) {
            if (!table.containsKey(r.itemId, r.userId)) {
                table.put(r.itemId, r.userId, r.score);
            }
        }
        return table;
    }

    /**
     * Description: 截取top-N的推荐列表
     *
     * @param rankedItems           排序后的评分列表
     * @param recommendItemsPerUser 每个用户推荐的商品数
     * @return 返回top-N的推荐列表
     */
    public static List<Rating> getSubset(List<Rating> rankedItems, int recommendItemsPerUser) {
        ConcurrentHashMap userRatingsTable = getUserItemsTable(rankedItems);
        List<Rating> subset = new ArrayList<>();
        for (Object userId : userRatingsTable.keySet()) {
            List<Rating> li = (List<Rating>) userRatingsTable.get(userId);
            subset.addAll(li.subList(0, Math.min(li.size(), recommendItemsPerUser)));
        }
        return subset;
    }


    /**
     * Description:读取数据，将数据转化成Link对象的List
     *
     * @param readFilePath 数据文件路径
     * @param separator    数据内容分隔符
     * @return List<Link>对象
     */
    public static List<Link> getLinks(String readFilePath, String separator) {
        File file = new File(readFilePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File doesn't exist:" + readFilePath);
        }
        List<Link> links = new ArrayList<>();
        try {
            Scanner in = new Scanner(file);
            while (in.hasNext()) {
                String str = in.nextLine();
                String[] elements = str.split(separator);

                int from = Integer.parseInt(elements[0]);
                int to = Integer.parseInt(elements[1]);
                Link r = new Link(from, to);
                links.add(r);
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return links;
    }

    /**
     * Description:保存数据
     *
     * @param links        关系数据列表
     * @param saveFilePath 保存的数据路径
     * @param separator    保存数据分隔符
     * @param append       数据是否追加
     * @param encoding     数据写入编码格式
     */
    public static void writeLinks(List<Link> links, String saveFilePath, String separator
            , boolean append, String encoding) {
        if (links == null) {
            throw new NullPointerException();
        }
        try {
            File file = new File(saveFilePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            OutputStreamWriter oStreamWriter = new OutputStreamWriter(new FileOutputStream(file, append), encoding);
            for (Link l : links) {
                String content = l.from + separator + l.to + separator + l.weight + "\n";
                oStreamWriter.append(content);
                oStreamWriter.flush();
            }
            oStreamWriter.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Description:将关系列表转化成自定义双重hashtable (from -to -weight)
     *
     * @param links 关系列表
     * @return 双重hashtable
     */
    public static RsTable getLinkTable(List<Link> links) {
        RsTable table = new RsTable();
        for (Link l : links) {
            if (!table.containsKey(l.from, l.to)) {
                table.put(l.from, l.to, l.weight);
            }
        }
        return table;
    }


    /**
     * Description:根据关系列表，转化成用户对应的hashtable key为from
     *
     * @param links 关系列表
     * @return 关系hashtable
     */
    public static ConcurrentHashMap getUserLinksTable(List<Link> links) {
        ConcurrentHashMap userLinksTable = new ConcurrentHashMap();
        for (Link l : links) {
            int key = l.from;
            if (userLinksTable.containsKey(key)) {
                List<Link> list = (List<Link>) userLinksTable.get(key);
                list.add(l);
                userLinksTable.put(key, list);
            } else {
                List<Link> list = new ArrayList<>();
                list.add(l);
                userLinksTable.put(key, list);
            }
        }
        return userLinksTable;
    }

    /**
     * Description:根据关系列表，转化成用户对应的hashtable key为to
     *
     * @param links 关系列表
     * @return 关系hashtable
     */
    public static ConcurrentHashMap getUserReverseLinksTable(List<Link> links) {
        ConcurrentHashMap userReverseLinksTable = new ConcurrentHashMap();
        for (Link l : links) {
            int key = l.to;
            if (userReverseLinksTable.containsKey(key)) {
                List<Link> list = (List<Link>) userReverseLinksTable.get(key);
                list.add(l);
                userReverseLinksTable.put(key, list);
            } else {
                List<Link> list = new ArrayList<>();
                list.add(l);
                userReverseLinksTable.put(key, list);
            }
        }
        return userReverseLinksTable;
    }

    /**
     * Description:判断关系列表是否为对称列表
     *
     * @param links 关系列表
     * @return 布尔值
     */
    public static boolean isAsymmetric(List<Link> links) {
        RsTable table = Tools.getLinkTable(links);
        int counter = 0;
        for (Object f : table.keys()) {
            ConcurrentHashMap subTable = (ConcurrentHashMap) table.get(f);
            for (Object t : subTable.keySet()) {
                if (!table.containsKey(t, f)) {
                    counter++;
                }
            }
        }
        System.out.println("counter:" + counter);
        return counter == 0;
    }

    /**
     * Description: 获取最大的userId和ItemId
     *
     * @param ratings 评分列表
     * @return Tuple
     */
    public static Tuple<Integer, Integer> getMaxUserIdAndItemId(List<Rating> ratings) {
        int maxUserId = 0;
        int maxItemId = 0;
        for (Rating r : ratings) {
            if (r.userId > maxUserId) {
                maxUserId = r.userId;
            }
            if (r.itemId > maxItemId) {
                maxItemId = r.itemId;
            }
        }
        return new Tuple<>(maxUserId, maxItemId);
    }


    /**
     * Description: 获取最大的userId1和userId2
     *
     * @param links 关系列表
     * @return Tuple
     */
    public static Tuple<Integer, Integer> getMaxUserId(List<Link> links) {
        int maxUserId = 0;
        int maxUserId2 = 0;
        for (Link l : links) {
            if (l.from > maxUserId)
                maxUserId = l.from;
            if (l.to > maxUserId2)
                maxUserId2 = l.to;
        }
        return new Tuple<>(maxUserId, maxUserId2);
    }


    /**
     * Description: 将评分列表转化成评分矩阵
     *
     * @param ratings 评分列表
     * @param rows    矩阵行数
     * @param columns 矩阵列数
     * @return 评分矩阵
     */
    public static double[][] transfrom(List<Rating> ratings, int rows, int columns) {
        double[][] matrix = new double[rows][columns];
        for (Rating r : ratings) {
            matrix[r.userId][r.itemId] = r.score;
        }
        return matrix;

    }


    /**
     * Description: 将数据切割成训练集和测试结合
     *
     * @param ratings  评分列表
     * @param testSize 测试集比例
     * @return 返回训练集合和测试机集合
     */
    public static Tuple<List<Rating>, List<Rating>> trainAndTestSplit(List<Rating> ratings, double testSize) {
        if (ratings == null)
            throw new IllegalArgumentException("Ratings is null.");

        Random random = new Random();
        List<Rating> trainRatings = new ArrayList<>();
        List<Rating> testRatings = new ArrayList<>();
        for (Rating r : ratings) {
            if (random.nextDouble() <= testSize) {
                testRatings.add(r);
            } else {
                trainRatings.add(r);
            }
        }
        return new Tuple<>(trainRatings, testRatings);

    }


}
