package core.collaborativeFiltering;

import entity.Rating;
import entity.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @version: 1.0
 * @author: Liujm
 * @site: https://github.com/liujm7
 * @contact: kaka206@163.com
 * @software: Idea
 * @date： 2017/11/14
 * @package_name: algorithm
 */
public class EuclideanEmbedding extends BiasedMatrixFactorization {
    final static Logger logger = LoggerFactory.getLogger(EuclideanEmbedding.class);

    public EuclideanEmbedding() {
    }

    public EuclideanEmbedding(int p, int q, int f, String fillMethod) {
        initial(p, q, f, fillMethod);
    }

    @Override
    public double predict(int userId, int itemId, double miu) {
        double r = 0;
        for (int i = 0; i < f; i++) {
            double e = P[userId][i] - Q[itemId][i];
            r += e * e;
        }
        return bu[userId] + bi[itemId] + miu - r;
    }

    @Override
    public void SGD(List<Rating> train, List<Rating> test, int epochs, double gamma, double lambda
            , double decay, double minRating, double maxRating) {
        List<Rating> trainOrTest = (test == null ? train : test);
        String trainOrTestString = (test == null ? "train" : "test");
        printParameters(train, trainOrTest, epochs, gamma, lambda, decay, minRating, maxRating);
        double miu = computeMiu(train);

        double loss = computeLoss(train, lambda, miu);
        for (int epoch = 1; epoch <= epochs; epoch++) {
            for (Rating r : train) {
                double pui = predict(r.userId, r.itemId, miu);
                double eui = r.score - pui;
                bu[r.userId] += gamma * (eui - lambda * bu[r.userId]);
                bi[r.itemId] += gamma * (eui - lambda * bi[r.itemId]);

                for (int i = 0; i < f; i++) {
                    double delta = (gamma * (P[r.userId][i] - Q[r.itemId][i]) * (eui + lambda));
                    P[r.userId][i] -= delta;
                    Q[r.itemId][i] += delta;
                }
            }
            double lastLoss = computeLoss(train, lambda, miu);

            if (epoch % 5 == 0){
                Tuple maeAndRmse = evaluateMaeRmse(trainOrTest, miu);
                logger.info("epoch:{},loss:{},{}-mae:{},{}-rmse:{}", epoch, loss, trainOrTestString, maeAndRmse.first, trainOrTestString, maeAndRmse.second);

            }

            if (decay != 1.0) {
                gamma *= decay;
            }
            if (lastLoss < loss) {
                loss = lastLoss;
            } else {
                break;
            }
        }
    }

    /**
     * Description:测试
     */
    public static void example() {
        List<Rating> ratings = new ArrayList<>();

        ratings.add(new Rating(1, 1, 5));
        ratings.add(new Rating(2, 1, 4));
        ratings.add(new Rating(3, 1, 1));
        ratings.add(new Rating(4, 1, 1));
        ratings.add(new Rating(1, 2, 3));
        ratings.add(new Rating(3, 2, 1));
        ratings.add(new Rating(5, 2, 1));
        ratings.add(new Rating(5, 3, 5));
        ratings.add(new Rating(1, 4, 1));
        ratings.add(new Rating(2, 4, 1));
        ratings.add(new Rating(3, 4, 5));
        ratings.add(new Rating(4, 4, 4));
        ratings.add(new Rating(5, 4, 4));

        EuclideanEmbedding f = new EuclideanEmbedding(6, 5, 4, "uniform_df");
        Double miu = f.computeMiu(ratings);
        f.SGD(ratings, 500);


        List<Rating> predicts = new ArrayList<>();

        predicts.add(new Rating(5, 1, f.predict(5, 1, miu)));
        predicts.add(new Rating(2, 2, f.predict(2, 2, miu)));
        predicts.add(new Rating(4, 2, f.predict(4, 2, miu)));
        predicts.add(new Rating(1, 3, f.predict(1, 3, miu)));
        predicts.add(new Rating(2, 3, f.predict(2, 3, miu)));
        predicts.add(new Rating(3, 3, f.predict(3, 3, miu)));
        predicts.add(new Rating(4, 3, f.predict(4, 3, miu)));

        for (Rating r : predicts) {
            logger.info("{},{},{}", r.userId, r.itemId, r.score);
        }
    }

    public static void main(String[] args) {
        example();
    }

}
