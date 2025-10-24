package cc.mrbird.febs.cos.dao;

import cc.mrbird.febs.cos.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author FanK
 */
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 分页获取订单信息
     *
     * @param page      分页对象
     * @param orderInfo 订单信息
     * @return 结果
     */
    IPage<LinkedHashMap<String, Object>> selectOrderPage(Page<OrderInfo> page, @Param("orderInfo") OrderInfo orderInfo);

    /**
     * 根据用户月份获取绩效
     *
     * @param staffCode 员工编号
     * @param year      年份
     * @param month     月份
     * @return 结果
     */
    List<InventoryStatistics> selectPerformanceByStaffCodeMonth(@Param("staffCode") String staffCode, @Param("year") String year, @Param("month") String month);

    /**
     * 查询总收益
     *
     * @return 结果
     */
    BigDecimal selectOrderPrice(@Param("pharmacyId") Integer pharmacyId);

    /**
     * 获取本月订单信息
     *
     * @return 结果
     */
    List<OrderInfo> selectOrderByMonth(@Param("pharmacyId")Integer pharmacyId);

    /**
     * 获取本年订单信息
     *
     * @return 结果
     */
    List<OrderInfo> selectOrderByYear(@Param("pharmacyId")Integer pharmacyId);

    /**
     * 十天内订单数量统计
     *
     * @return 结果
     */
    List<LinkedHashMap<String, Object>> selectOrderNumWithinDays(@Param("pharmacyId") Integer pharmacyId);

    /**
     * 十天内订单收益统计
     *
     * @return 结果
     */
    List<LinkedHashMap<String, Object>> selectOrderPriceWithinDays(@Param("pharmacyId") Integer pharmacyId);

    /**
     * 订单销售药品类别统计
     *
     * @return 结果
     */
    List<LinkedHashMap<String, Object>> selectOrderDrugType();

    /**
     * 获取用户订单物流
     *
     * @param userId 用户ID
     * @return 结果
     */
    List<LinkedHashMap<String, Object>> selectOrderLogistics(@Param("userId") Integer userId);

    /**
     * 根据时间获取订单信息
     *
     * @param year  年度
     * @param month 月度
     * @return 结果
     */
    List<OrderInfo> selectOrderByCheckMonth(@Param("year") Integer year, @Param("month") Integer month);

    /**
     * 获取药品历史销售数据
     *
     * @param drugId  药品ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 结果
     */
    List<OrderItem> getHistoricalSalesByDrugId(@Param("drugId") Integer drugId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取药店历史销售数据
     *
     * @param pharmacyId 药房ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 结果
     */
    List<DailySale> getPharmacyHistoricalSales(@Param("pharmacyId") Integer pharmacyId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
