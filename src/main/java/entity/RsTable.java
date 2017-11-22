package entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version: 1.0
 * @author: Liujm
 * @site: https://github.com/liujm7
 * @contact: kaka206@163.com
 * @software: Idea
 * @date： 2017/11/13
 * @package_name: entity
 */
public class RsTable {
    private class subKeyComparer implements Comparator {
        private ConcurrentHashMap subKeyTable = null;

        public subKeyComparer(ConcurrentHashMap subKeyTable) {
            this.subKeyTable = subKeyTable;
        }

        public int compare(Object x, Object y) {
            int f = (Integer) subKeyTable.get(x);
            int s = (Integer) subKeyTable.get(y);

            if (f < s)
                return -1;
            else if (f == s)
                return 0;
            else return 1;
        }
    }


    private ConcurrentHashMap<Object, ConcurrentHashMap<Object, Object>> main = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Object, Object> subKeyAll = new ConcurrentHashMap<>();

    public RsTable() {
    }

    public boolean containsMainKey(Object mainKey) {
        return main.containsKey(mainKey);
    }

    public boolean containsKey(Object mainKey, Object subKey) {
        if (null == mainKey || null == subKey) {
            throw new NullPointerException();
        }
        if (main.containsKey(mainKey)) {
            if (main.get(mainKey).containsKey(subKey)) {
                return true;
            }
        }
        return false;
    }

    public void put(Object mainKey, Object subKey, Object value) {
        if (null == mainKey || null == subKey) {
            throw new NullPointerException();
        }
        if (!containsMainKey(mainKey)) {
            main.put(mainKey, new ConcurrentHashMap<Object, Object>());
        }

        ConcurrentHashMap<Object, Object> itemTable = main.get(mainKey);
        itemTable.put(subKey, value);
        main.put(subKey, itemTable);

        if (!subKeyAll.containsKey(subKey)) {
            subKeyAll.put(subKey, subKeyAll.size() + 1);
        }

    }

    public Object get(Object mainKey) {
        if (null == mainKey) {
            throw new NullPointerException();
        }

        if (!containsMainKey(mainKey)) {
            main.put(mainKey, new ConcurrentHashMap<>());
        }
        return main.get(mainKey);

    }

    public Object get(Object mainKey, Object subKey) {
        if (null == mainKey || null == subKey) {
            throw new NullPointerException();
        }


        if (!containsMainKey(mainKey)) {
            main.put(mainKey, new ConcurrentHashMap<>());
        }

        ConcurrentHashMap<Object, Object> subTable = (ConcurrentHashMap) main.get(mainKey);

        if (!subTable.containsKey(subKey)) {
            subTable.put(subKey, 0.0);
        }
        return subTable.get(subKey);

    }

    public Set<Object> keys() {
        return main.keySet();
    }

    public int size() {

        return main.size();
    }

    public ConcurrentHashMap<Object, Object> getSubKeyAll() {
        return subKeyAll;
    }

    public ArrayList getSubKeyList() {
        ArrayList list = new ArrayList(subKeyAll.keySet());
        list.sort(new subKeyComparer(subKeyAll));
        return list;
    }

}
