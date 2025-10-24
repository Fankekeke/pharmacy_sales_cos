package cc.mrbird.febs.cos.service.impl;

import cc.mrbird.febs.cos.dao.OrderInfoMapper;
import cc.mrbird.febs.cos.entity.*;
import cc.mrbird.febs.cos.service.IDrugInfoService;
import cc.mrbird.febs.cos.service.IOrderInfoService;
import cc.mrbird.febs.cos.service.IPharmacyInventoryService;
import cc.mrbird.febs.cos.service.IPredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.*;


@Service
public class PredictionServiceImpl implements IPredictionService {

    @Autowired
    private IOrderInfoService orderInfoService;

    @Autowired
    private IPharmacyInventoryService pharmacyInventoryService;

    @Resource
    private OrderInfoMapper orderInfoMapper;

    @Resource
    private IDrugInfoService drugInfoService;

    @Override
    public PredictionResult predictSales(Integer drugId, Integer days) {
        // 获取历史销售数据（例如过去90天）
        List<OrderItem> historicalSales = getHistoricalSalesData(drugId, 15);

        // 计算移动平均值
        double movingAverage = calculateMovingAverage(historicalSales, 7); // 7日移动平均

        // 生成预测结果
        PredictionResult result = new PredictionResult();
        List<Double> predictions = new ArrayList<>();
        List<String> dates = new ArrayList<>();

        // 引入趋势因子0.05，使预测值逐日递增
        double trendRate = 0.05;

        // 使用移动平均值作为基础值，并应用趋势因子
        for (int i = 1; i <= days; i++) {
            double predictedValue = new java.math.BigDecimal(movingAverage * (1.0 + trendRate * i))
                    .setScale(1, java.math.RoundingMode.HALF_UP)
                    .doubleValue();
            predictions.add(predictedValue);
            dates.add(LocalDate.now().plusDays(i).toString());
        }

        result.setPredictedValues(predictions);
        result.setDates(dates);
        result.setConfidence(calculateConfidence(historicalSales, movingAverage));

        return result;
    }

    @Override
    public PredictionResult predictInventoryDemand(Integer pharmacyId, Integer days) {
        // 获取药店历史销售数据
        List<DrugSales> historicalSales = getPharmacyHistoricalSales(pharmacyId, 15);

        // 计算每种药品的移动平均需求量
        Map<Integer, Double> avgDailyDemand = new HashMap<>();
        for (DrugSales sales : historicalSales) {
            Integer drugId = sales.getDrugId();
            double avg = calculateMovingAverageForDrug(sales.getDailySales(), 7);
            avgDailyDemand.put(drugId, avg);
        }

        // 计算总库存需求预测
        PredictionResult result = new PredictionResult();
        List<Double> predictions = new ArrayList<>();
        List<String> dates = new ArrayList<>();

        // 引入趋势因子
        double trendRate = 0.05;

        // 计算每天的总需求预测
        for (int i = 1; i <= days; i++) {
            // 基础总需求量
            double baseTotalDemand = avgDailyDemand.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
            // 上架因子（新品上市带来的增长）
            double launchFactor = 1.0 + 0.02 * Math.max(0, i - 10); // 假设第10天后开始影响
            // 下架因子（老产品逐渐减少）
            double delistFactor = 1.0 - 0.02 * Math.max(0, i - 15); // 假设第20天后开始影响
            // 应用所有因子计算最终需求量
            double dailyTotalDemand = baseTotalDemand * (1.0 + trendRate * i) * launchFactor * delistFactor;
            // 保留小数点后一位
            double roundedValue = new java.math.BigDecimal(dailyTotalDemand)
                    .setScale(1, java.math.RoundingMode.HALF_UP)
                    .doubleValue();
            predictions.add(roundedValue);
            dates.add(LocalDate.now().plusDays(i).toString());
        }

        // 为每个药品计算预测值
        Map<String, List<Double>> drugPredictions = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : avgDailyDemand.entrySet()) {
            Integer drugId = entry.getKey();
            Double avgDemand = entry.getValue();

            // 通过drugId获取药品名称
            String drugName = historicalSales.stream()
                    .filter(sale -> sale.getDrugId().equals(drugId))
                    .findFirst()
                    .map(DrugSales::getDrugName)
                    .orElse("Unknown");

            List<Double> drugPredictionList = new ArrayList<>();
            for (int i = 1; i <= days; i++) {
                double predictedValue = new java.math.BigDecimal(avgDemand * (1.0 + trendRate * i))
                        .setScale(1, java.math.RoundingMode.HALF_UP)
                        .doubleValue();
                drugPredictionList.add(predictedValue);
            }
            drugPredictions.put(drugName, drugPredictionList);
        }

        result.setPredictedValues(predictions);
        result.setDates(dates);
        result.setDrugPredictions(drugPredictions); // 需要在PredictionResult中添加此字段
        result.setConfidence(0.85); // 简单设定置信度
        return result;
    }

    /**
     * 计算移动平均值
     */
    private double calculateMovingAverage(List<OrderItem> data, int period) {
        if (data.size() < period) {
            period = data.size();
        }

        double sum = 0.0;
        // 取最近period天的数据计算平均值
        for (int i = Math.max(0, data.size() - period); i < data.size(); i++) {
            sum += data.get(i).getQuantity();
        }

        return period > 0 ? sum / period : 0.0;
    }

    /**
     * 为特定药品计算移动平均值
     */
    private double calculateMovingAverageForDrug(List<DailySale> dailySales, int period) {
        if (dailySales.size() < period) {
            period = dailySales.size();
        }

        double sum = 0.0;
        for (int i = Math.max(0, dailySales.size() - period); i < dailySales.size(); i++) {
            sum += dailySales.get(i).getQuantity();
        }

        return period > 0 ? sum / period : 0.0;
    }

    /**
     * 计算预测置信度
     */
    private double calculateConfidence(List<OrderItem> historicalData, double average) {
        if (historicalData.isEmpty()) {
            return 0.0;
        };

        // 计算标准差来评估数据波动性
        double variance = 0.0;
        for (OrderItem item : historicalData) {
            variance += Math.pow(item.getQuantity() - average, 2);
        }
        variance /= historicalData.size();
        double stdDev = Math.sqrt(variance);

        // 简单的置信度计算：数据越稳定，置信度越高
        // 这里使用一个简单的反比例关系
        double coefficientOfVariation = (average > 0) ? (stdDev / average) : 1.0;
        return Math.max(0.0, Math.min(1.0, 1.0 - coefficientOfVariation));
    }

    /**
     * 获取药品历史销售数据
     */
    private List<OrderItem> getHistoricalSalesData(Integer drugId, int days) {
        // 计算查询起始日期
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        List<OrderItem> historicalSales = orderInfoMapper.getHistoricalSalesByDrugId(
                drugId,
                startDate.atStartOfDay(),
                endDate.atStartOfDay()
        );
        // 确保返回的数据按日期排序
        if (historicalSales != null) {
            // 如果需要按日期排序，可以在这里添加排序逻辑
             Collections.sort(historicalSales, Comparator.comparing(OrderItem::getDate));
            return historicalSales;
        }
        return Collections.emptyList();

    }

    /**
     * 获取药店历史销售数据
     */
    private List<DrugSales> getPharmacyHistoricalSales(Integer pharmacyId, int days) {
        // 计算查询起始日期
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        List<DailySale> historicalSales = orderInfoMapper.getPharmacyHistoricalSales(
                pharmacyId,
                startDate.atStartOfDay(),
                endDate.atStartOfDay()
        );
        // 获取所有药品
        List<DrugInfo> drugInfoList = drugInfoService.list();
        List<DrugSales> result = new ArrayList<>();

        // 按药品分组处理销售数据
        Map<Integer, List<DailySale>> drugSalesMap = new HashMap<>();
        if (historicalSales != null) {
            for (DailySale dailySale : historicalSales) {
                Integer drugId = dailySale.getDrugId();
                drugSalesMap.computeIfAbsent(drugId, k -> new ArrayList<>()).add(dailySale);
            }
        }

        // 构建DrugSales对象
        for (DrugInfo drugInfo : drugInfoList) {
            DrugSales drugSales = new DrugSales();
            drugSales.setDrugId(drugInfo.getId());
            drugSales.setDrugName(drugInfo.getName());
            drugSales.setDailySales(drugSalesMap.getOrDefault(drugInfo.getId(), new ArrayList<>()));
            result.add(drugSales);
        }

        return result;
    }
}
