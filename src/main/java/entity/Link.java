package entity;

import java.util.Comparator;

/**
 * @version: 1.0
 * @author: Liujm
 * @site: https://github.com/liujm7
 * @contact: kaka206@163.com
 * @software: Idea
 * @date： 2017/11/13
 * @package_name: entity
 */
public class Link implements Comparable<Link> {
    public int from = 0;
    public int to = 0;
    public double weight = 1;

    public Link(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public Link(int from, int to, double weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }


    @Override
    public int compareTo(Link o) {
        //降序排序
        return this.weight > o.weight ? -1 : 1;
    }
}


