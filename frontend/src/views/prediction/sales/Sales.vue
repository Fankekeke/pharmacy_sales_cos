<template>
  <div class="sales-prediction-container">
    <!-- 药品选择器 -->
    <a-select
      v-model="selectedDrugId" style="width: 300px"
      placeholder="请选择药品"
      @change="onDrugChange"
    >
      <a-select-option
        v-for="item in drugList"
        :key="item.id"
        :value="item.id"
      >
        {{ item.name }}
      </a-select-option>
    </a-select>

    <!-- 预测图表区域 -->
    <div class="chart-container">
      <div
        id="sales-chart"
        class="sales-chart"
      ></div>

      <!-- 预测详情卡片 -->
      <a-card
        v-if="predictionData"
        class="prediction-details-card"
      >
        <template #title>
          <a-icon type="pie-chart"/>
          <strong style="margin-left: 8px;font-size: 18px">销售预测详情</strong>
        </template>
        <a-row :gutter="16">
          <a-col :span="24">
            <div class="detail-item">
              <span class="detail-label">模型置信度</span>
              <span
                :class="[
            'detail-value',
            {
              'confidence-high': predictionData.confidence > 0.8,
              'confidence-medium': predictionData.confidence > 0.6 && predictionData.confidence <= 0.8,
              'confidence-low': predictionData.confidence <= 0.6
            }
          ]"
              >
          {{ (predictionData.confidence * 100).toFixed(2) }}%
        </span>
            </div>
          </a-col>
          <a-col :span="24">
            <div class="detail-item">
              <span class="detail-label">预测时间范围</span>
              <span class="detail-value">
          {{ predictionData.dates[0] }} 至 {{ predictionData.dates[predictionData.dates.length - 1] }}
        </span>
            </div>
          </a-col>
          <a-col :span="24">
            <div class="detail-item">
              <span class="detail-label">预测天数</span>
              <span class="detail-value">
          {{ predictionData.dates.length }} 天
        </span>
            </div>
          </a-col>
        </a-row>
      </a-card>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
export default {
  name: 'Sales',
  data () {
    return {
      selectedDrugId: 1,
      drugList: [],
      dataList: [],
      predictionData: null,
      chart: null
    }
  },
  mounted () {
    this.getDrugList()
    this.getDrugSales()
    // 初始化图表
    this.$nextTick(() => {
      this.initChart()
    })
  },
  methods: {
    getDrugList () {
      this.$get('/cos/drug-info/list').then((r) => {
        this.drugList = r.data.data
      })
    },
    getDrugSales (drugId = 1) {
      this.$get('/cos/prediction/sales', { drugId: drugId }).then((r) => {
        this.predictionData = r.data.data
        this.dataList = r.data.data
        this.updateChart()
      })
    },
    onDrugChange (value) {
      this.$get('/cos/prediction/sales', { drugId: value }).then((r) => {
        this.predictionData = r.data.data
        this.updateChart()
      })
    },
    initChart () {
      const chartDom = document.getElementById('sales-chart')
      this.chart = echarts.init(chartDom)
    },
    updateChart () {
      if (!this.chart || !this.predictionData) return

      const option = {
        title: {
          text: '药品销售预测'
        },
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: this.predictionData.dates
        },
        yAxis: {
          type: 'value',
          name: '销售数量'
        },
        series: [
          {
            data: this.predictionData.predictedValues,
            type: 'line',
            smooth: true,
            markPoint: {
              data: [
                { type: 'max', name: '最大值' },
                { type: 'min', name: '最小值' }
              ]
            },
            markLine: {
              data: [
                { type: 'average', name: '平均值' }
              ]
            }
          }
        ]
      }
      this.chart.setOption(option)
    }
  },
  beforeDestroy () {
    if (this.chart) {
      this.chart.dispose()
    }
  }
}
</script>

<style scoped>.sales-prediction-container {
  width: 100%;
  padding: 20px;
}

.chart-container {
  width: 100%;
  margin-top: 20px;
}

.sales-chart {
  width: 100%;
  height: 450px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.prediction-details-card {
  width: 35%;
  margin-top: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 25px;
}

.prediction-details-card :deep(.ant-card-head) {
  background-color: #f7f9fc;
  border-bottom: 1px solid #e8e8e8;
}

.prediction-details-card :deep(.ant-descriptions-row > th) {
  font-weight: 500;
  color: #595959;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-label {
  font-size: 14px;
  color: #8c8c8c;
}

.detail-value {
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.confidence-high {
  color: #52c41a;
}

.confidence-medium {
  color: #faad14;
}

.confidence-low {
  color: #ff4d4f;
}
</style>
