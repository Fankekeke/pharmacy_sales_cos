package cc.mrbird.febs.cos.entity;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class DrugSales {
    private Integer drugId;           // 药品ID
    private String drugName;          // 药品名称
    private Integer pharmacyId;       // 药店ID
    private List<DailySale> dailySales; // 每日销售数据
    private Double totalSales;        // 总销售量
    private Date startDate;           // 数据开始日期
    private Date endDate;             // 数据结束日期
}
