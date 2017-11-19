package entity;

import java.util.Date;

/**
 * @version: 1.0
 * @author: Liujm
 * @site: https://github.com/liujm7
 * @contact: kaka206@163.com
 * @software: Idea
 * @date： 2017/11/10
 * @package_name: entity
 */
public class Rating implements Comparable<Object> {
    public int userId = 0;
    public int itemId = 0;
    public double score = 0;
    public String timestamp;
    public int tiemInterval;

    public Rating(int userId, int itemId, double score) {
        this.userId = userId;
        this.itemId = itemId;
        this.score = score;
    }

    public Rating(int userId, int itemId, double score, String timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.score = score;
        this.timestamp = timestamp;
    }

    public Date getTime() {
        return new Date(Long.parseLong(this.timestamp));
    }

    public static int compare(Rating x, Rating y) {
        if (x.score < y.score) {
            return 1;
        } else if (x.score == x.score) {
            return 0;
        } else {
            return -1;
        }
    }

    public int compareTo(Object object) {

        Rating r = (Rating) object;
        if (this.score < r.score) {
            return 1;
        } else if (this.score == r.score) {
            return 0;
        } else {
            return -1;
        }

    }
}

class RatingComparer implements Comparable<Rating> {

    public int compare(Rating x, Rating y) {
        if (x.score > y.score) {
            return -1;
        } else if (x.score == y.score) {
            return 0;
        } else {
            return 1;
        }
    }

    public int compareTo(Rating o) {
        return 0;
    }
}
