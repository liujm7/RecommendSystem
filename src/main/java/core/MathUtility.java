package core;


import java.util.Random;

/**
 * @version: 1.0
 * @author: Liujm
 * @site: https://github.com/liujm7
 * @contact: kaka206@163.com
 * @software: Idea
 * @date： 2017/11/10
 * @package_name: algorithm
 */
public class MathUtility {
    /**
     * Description: 逻辑函数
     *
     * @param x：变量
     * @return 返回逻辑函数值
     */
    public static double logistic(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }


    /**
     * Description: 计算向量一范数
     *
     * @param vector: 向量
     * @return 一范数
     */
    public static double norm1(double[] vector) {
        double norm = 0.0;
        int len = vector.length;

        for (int i = 0; i < len; i++) {
            norm += Math.abs(i);
        }
        return norm;
    }

    /**
     * Description: 计算向量二范数
     *
     * @param vector: 向量
     * @return 二范数
     */
    public static double norm2(double[] vector) {
        double norm = 0.0;

        for (double aVector : vector) {
            norm += (aVector * aVector);
        }
        return Math.sqrt(norm);
    }

    /**
     * Description: 计算矩阵二范数
     *
     * @param matrix: 矩阵
     * @return 二范数
     */
    public static double norm2(double[][] matrix) {
        if (matrix == null)
            return 0;
        double norm = 0.0;
        int n = matrix[0].length;
        for (double[] aMatrix : matrix) {
            for (int j = 0; j < n; j++) {
                norm += (aMatrix[j] * aMatrix[j]);
            }
        }
        return Math.sqrt(norm);
    }


    /**
     * Description:生成一个高斯分布的随机数
     *
     * @param mean  均值
     * @param stdev 标准差
     * @return 随机数矩阵
     */
    public static double randomGaussian(double mean, double stdev) {
        Random r = new Random();
        if ((stdev == 0.0) || (Double.isNaN(stdev))) {
            return mean;
        } else {
            return mean + stdev * r.nextGaussian();
        }
    }


    /**
     * Description:生成一个高斯分布的随机数矩阵
     *
     * @param rows    矩阵的行数
     * @param columns 矩阵的列数
     * @param mean    均值
     * @param stdev   标准差
     * @return 随机数矩阵
     */
    public static double[][] randomGaussian(int rows, int columns, double mean, double stdev) {
        double[][] matrix = new double[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                matrix[i][j] = randomGaussian(mean, stdev);  // Random filled by 2-d Gaussian
            }
        }
        return matrix;
    }


    /**
     * Description:生成一个0-1的随机数矩阵
     *
     * @param rows    矩阵行数
     * @param columns 矩阵列数
     * @return 随机数矩阵
     */
    public static double[][] randomUniform(int rows, int columns) {
        double[][] matrix = new double[rows][columns];
        Random r = new Random();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                matrix[i][j] = r.nextDouble();
            }
        }
        return matrix;
    }


    /**
     * Description:生成一个0-factor的随机数矩阵
     *
     * @param rows    矩阵行数
     * @param columns 矩阵列数
     * @param factor  倍数
     * @return 随机数矩阵
     */
    public static double[][] randomUniform(int rows, int columns, double factor) {
        double[][] matrix = new double[rows][columns];
        Random r = new Random();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                matrix[i][j] = r.nextDouble() * factor;
            }
        }
        return matrix;
    }


    /**
     * Description: 求解矩阵的转置
     *
     * @param matrix 评分矩阵
     * @return 矩阵
     */
    public static double[][] transpose(double[][] matrix) {
        if (matrix == null)
            return null;
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] matrixT = new double[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrixT[j][i] = matrix[i][j];
            }
        }
        return matrixT;
    }


    public static double[][] inverseMatrix(double[][] matrix) {
        int m = matrix.length;
        int n = matrix[0].length;
        double[][] array = new double[m][2 * n];
        for (int k = 0; k < m; k++) {
            for (int t = 0; t < 2 * n; t++) {
                array[k][t] = 0.0000000;
            }
        }

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                array[i][j] = matrix[i][j];
            }
        }

        for (int k = 0; k < m; k++) {
            for (int t = n; t < 2 * n; t++) {
                if ((t - k) == m) {
                    array[k][t] = 1.0;
                } else {
                    array[k][t] = 0;
                }
            }
        }

        for (int k = 0; k < m; k++) {
            if (array[k][k] != 1) {
                double bs = array[k][k];
                array[k][k] = 1;
                for (int p = k + 1; p < 2 * n; p++) {
                    array[k][p] /= bs;
                }
            }
            for (int q = 0; q < m; q++) {
                if (q != k) {
                    double bs = array[q][k];
                    for (int p = 0; p < 2 * n; p++) {
                        array[q][p] -= bs * array[k][p];
                    }
                }
            }
        }
        double[][] inverseMatrix = new double[m][n];
        for (int x = 0; x < m; x++) {
            for (int y = n; y < 2 * n; y++) {
                inverseMatrix[x][y - n] = array[x][y];
            }
        }
        return inverseMatrix;
    }

    /**
     * Description: 计算 Jaccard 相关性系数
     *
     * @param matrix:评分矩阵
     * @return 相似性矩阵
     */
    public static double[][] jaccard(double[][] matrix) {
        int m = matrix.length;
        int n = matrix[0].length;
        double[][] similarity = new double[m][m];
        for (int i = 0; i < m - 1; i++) {
            for (int j = i + 1; j < m; j++) {
                int numerator = 0;
                int denominator = 0;
                for (int item = 0; item < n; item++) {
                    if (matrix[i][item] > 0 && matrix[j][item] > 0)
                        numerator++;
                    if (matrix[i][item] > 0 || matrix[j][item] > 0)
                        denominator++;
                }
                if (0 < denominator) {
                    similarity[i][j] = numerator * 1.0 / (denominator - numerator);
                    similarity[j][i] = similarity[i][j];
                }
            }
        }
        return similarity;
    }

    /**
     * Description:计算余弦相似度
     *
     * @param matrix 评分矩阵
     * @return 相似性矩阵
     */
    public static double[][] cosine(double[][] matrix) {
        int m = matrix.length;
        int n = matrix[0].length;
        double[][] similarity = new double[m][m];

        for (int i = 0; i < m - 1; i++) {
            for (int j = i + 1; j < m; j++) {
                double numerator = 0;
                double denominatorI = 0;
                double denominatorJ = 0;
                for (int item = 0; item < n; item++) {
                    if (matrix[i][item] > 0 && matrix[j][item] > 0) {
                        numerator += matrix[i][item] * matrix[j][item];
                    }
                    if (matrix[i][item] > 0) {
                        denominatorI += matrix[i][item] * matrix[i][item];
                    }
                    if (matrix[j][item] > 0) {
                        denominatorJ += matrix[j][item] * matrix[j][item];
                    }
                }
                double denominator = Math.sqrt(denominatorI * denominatorJ);
                if (0 < denominator) {
                    similarity[i][j] = numerator / denominator;
                    similarity[j][i] = similarity[i][j];
                }
            }
        }
        return similarity;
    }

    /**
     * Description:计算修正后的余弦相似性
     *
     * @param matrix 评分矩阵
     * @return 相似性矩阵
     */
    public static double[][] correctedCosine(double[][] matrix) {
        int m = matrix.length;
        int n = matrix[0].length;
        double[][] similarity = new double[m][m];
        double[] averageRating = new double[m];
        for (int i = 0; i < m; i++) {
            double sum = 0.0;
            int count = 0;
            for (int item = 0; item < n; item++) {
                if (matrix[i][item] > 0) {
                    count++;
                    sum += matrix[i][item];
                }
            }
            if (count > 0) {
                averageRating[i] = sum / count;
            }
        }
        for (int i = 0; i < m - 1; i++) {
            for (int j = i + 1; j < m; j++) {
                double numerator = 0;
                double denominatorI = 0;
                double denominatorJ = 0;

                for (int item = 0; item < n; item++) {
                    if (matrix[i][item] > 0 && matrix[j][item] > 0) {
                        numerator += (matrix[i][item] - averageRating[i]) * (matrix[j][item] - averageRating[j]);
                    }
                    if (matrix[i][item] > 0) {
                        denominatorI += (matrix[i][item] - averageRating[i]) * (matrix[i][item] - averageRating[i]);
                    }
                    if (matrix[j][item] > 0) {
                        denominatorJ += (matrix[j][item] - averageRating[j]) * (matrix[j][item] - averageRating[j]);
                    }
                    double denominator = Math.sqrt(denominatorI * denominatorJ);
                    if (0 < denominator) {
                        similarity[i][j] = numerator / denominator;
                        similarity[j][i] = similarity[i][j];
                    }
                }
            }
        }
        return similarity;
    }

    /**
     * Description: 计算皮尔森相关性
     *
     * @param matrix 评分矩阵
     * @return 相似性矩阵
     */
    public static double[][] pearsonCorrelation(double[][] matrix) {
        int m = matrix.length;
        int n = matrix[0].length;
        double[][] similarity = new double[m][m];
        double[] averageRating = new double[m];
        for (int i = 0; i < m; i++) {
            double sum = 0.0;
            int count = 0;
            for (int item = 0; item < n; item++) {
                if (matrix[i][item] > 0) {
                    count++;
                    sum += matrix[i][item];
                }
            }
            if (count > 0) {
                averageRating[i] = sum / count;
            }
        }
        for (int i = 0; i < m - 1; i++) {
            for (int j = i + 1; j < m; j++) {
                double numerator = 0;
                double denominatorI = 0;
                double denominatorJ = 0;

                for (int item = 0; item < n; item++) {
                    if (matrix[i][item] > 0 && matrix[j][item] > 0) {
                        double numeratorI = matrix[i][item] - averageRating[i];
                        double numeratorJ = matrix[j][item] - averageRating[j];
                        numerator += numeratorI * numeratorJ;
                        denominatorI += numeratorI * numeratorI;
                        denominatorJ += numeratorJ * numeratorJ;
                    }
                    double denominator = Math.sqrt(denominatorI * denominatorJ);
                    if (0 < denominator) {
                        similarity[i][j] = numerator / denominator;
                        similarity[j][i] = similarity[i][j];
                    }
                }
            }
        }
        return similarity;


    }

    /**
     * Description: 计算相似性
     *
     * @param matrix         评分矩阵
     * @param similarityType 相似性类型
     * @return 相似性矩阵
     */
    public static double[][] computeSimilarity(double[][] matrix, String similarityType) {
        if (matrix == null) {
            return null;
        }
        if (similarityType.equalsIgnoreCase("Jaccard")) {
            return jaccard(matrix);
        } else if (similarityType.equalsIgnoreCase("Cosine")) {
            return cosine(matrix);
        } else if (similarityType.equalsIgnoreCase("CorrectedCosine")) {
            return correctedCosine(matrix);
        } else if (similarityType.equalsIgnoreCase("PearsonCorrelation")
                || similarityType.equalsIgnoreCase("Pearson")) {
            return pearsonCorrelation(matrix);
        }

        return cosine(matrix);
    }


    /**
     * Description: 计算相似性(默认余弦相似性)
     *
     * @param matrix：评分矩阵
     * @return 相似性矩阵
     */
    public static double[][] computeSimilarity(double[][] matrix) {
        return computeSimilarity(matrix, "Cosine");
    }


    public static void main(String[] args) {
        double[][] matrix = new double[2][2];
        matrix[0][0] = 1;
        matrix[0][1] = 3;
        matrix[1][0] = 2;
        matrix[1][1] = 3;
        double[][] inverseMatrix = pearsonCorrelation(matrix);
        for (int i = 0; i < inverseMatrix.length; i++) {
            for (int j = 0; j < inverseMatrix[0].length; j++) {
                System.out.println(inverseMatrix[i][j]);
            }
        }


    }
}
