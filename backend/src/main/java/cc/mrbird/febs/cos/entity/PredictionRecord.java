package cc.mrbird.febs.cos.entity;

import lombok.Data;

import java.util.Date;

// 预测记录表（用于模型持续优化）
@Data
public class PredictionRecord {
    private Integer id;
    private Integer drugId; // 药品ID
    private Integer pharmacyId; // 药店ID
    private Date predictionDate; // 预测日期
    private String predictionData; // 预测数据(json)
    private Date createTime; // 创建时间
}