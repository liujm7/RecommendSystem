package entity;

/**
 * @version: 1.0
 * @author: Liujm
 * @site: https://github.com/liujm7
 * @contact: kaka206@163.com
 * @software: Idea
 * @date： 2017/11/13
 * @package_name: entity
 */
public class Tuple<A, B> {
    public final A first;
    public final B second;

    public Tuple(A a, B b) {
        this.first = a;
        this.second = b;
    }
}
