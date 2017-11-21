# RecommendSystem
推荐系统重构版java<br/>
1.Baseline: 实现了基准线预测,评分预测和topN预测均可<br/>
2.MeanFilling:实现了全局均值插值法，用户均值插值法和商品均值插值法 ,评分预测<br/>
3.UserKNN:基于用户的协同过滤KNN算法，实现了评分预测和topN预测<br/>
4.ItemKNN:基于物品的协同过滤KNN算法, 实现了评分预测和topN预测<br/>
5.MatrixFactorization:基本矩阵分解协同过滤，实现了sgd的评分预测和topN预测<br/>
6.BiasedMatrixFactorization:偏差性矩阵分解，实现了sgd的评分预测和topN预测<br/>
7.AlternatingLeastSquares:继承MatrixFactorization，实现了als的评分预测<br/>
8.SVDPlusPlus:SVD++分解，综合考虑用户评分偏差、商品评分偏差和整体评分均值，实现了sgd的评分预测和topN预测<br/>
