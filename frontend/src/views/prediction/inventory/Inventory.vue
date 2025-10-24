<template>
  <div class="inventory-prediction-container">
    <!-- 药店选择器 -->
    <a-select
      v-model="selectedPharmacyId"
      class="pharmacy-selector"
      placeholder="请选择药店"
      @change="pharmacyCheck"
    >
      <a-select-option
        v-for="pharmacy in pharmacyList"
        :key="pharmacy.id"
        :value="pharmacy.id"
      >
        {{ pharmacy.name }}
      </a-select-option>
    </a-select>

    <!-- 预测内容区域 -->
    <div v-if="inventoryData" class="prediction-content">
      <!-- 总体库存预测图表 -->
      <div
        id="inventory-chart"
        class="inventory-chart"
      ></div>

      <!-- 各药品详细预测 -->
      <a-card class="drug-predictions-card" title="各药品库存预测">
        <a-tabs
          class="drug-tabs"
          @change="onTabChange"
        >
          <a-tab-pane
            v-for="(predictions, drugName) in inventoryData.drugPredictions"
            :key="drugName"
            :tab="drugName"
            force-render
          >
            <div
              :id="'drug-chart-' + drugName.replace(/\s+/g, '-')"
              class="drug-chart"
            ></div>
          </a-tab-pane>
        </a-tabs>
      </a-card>

      <!-- 预测详情信息 -->
      <a-card class="prediction-details-card">
        <template #title>
          <a-icon type="info-circle" theme="twoTone" />
          <strong style="margin-left: 8px;font-size: 18px">库存预测详情</strong>
        </template>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12" :md="8">
            <div class="detail-item">
              <span class="detail-label">模型置信度</span>
              <span class="detail-value confidence-high" v-if="inventoryData.confidence > 0.8">
            {{ (inventoryData.confidence * 100).toFixed(2) }}%
          </span>
              <span class="detail-value confidence-medium" v-else-if="inventoryData.confidence > 0.6">
            {{ (inventoryData.confidence * 100).toFixed(2) }}%
          </span>
              <span class="detail-value confidence-low" v-else>
            {{ (inventoryData.confidence * 100).toFixed(2) }}%
          </span>
            </div>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8">
            <div class="detail-item">
              <span class="detail-label">预测天数</span>
              <span class="detail-value">
            {{ inventoryData.dates.length }} 天
          </span>
            </div>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8">
            <div class="detail-item">
              <span class="detail-label">预测时间范围</span>
              <span class="detail-value">
            {{ inventoryData.dates[0] }} 至 {{ inventoryData.dates[inventoryData.dates.length - 1] }}
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
  name: 'Inventory',
  data () {
    return {
      pharmacyList: [],
      inventoryData: null,
      charts: {}
    }
  },
  mounted () {
    this.getPharmacy()
    this.getInventory()
  },
  methods: {
    getPharmacy () {
      this.$get('/cos/pharmacy-info/list').then((r) => {
        this.pharmacyList = r.data.data
      })
    },
    getInventory (pharmacyId = 1) {
      this.$get('/cos/prediction/inventory', { pharmacyId: pharmacyId }).then((r) => {
        this.inventoryData = r.data.data
        this.$nextTick(() => {
          this.renderCharts()
        })
      })
    },
    pharmacyCheck (value) {
      this.getInventory(value)
    },
    renderCharts () {
      if (!this.inventoryData) return

      // 渲染总体库存预测图表
      this.renderTotalInventoryChart()

      // 渲染各药品预测图表
      this.renderDrugCharts()
    },
    renderDrugCharts () {
      if (!this.inventoryData.drugPredictions) return

      Object.keys(this.inventoryData.drugPredictions).forEach(drugName => {
        // 使用 $nextTick 确保 DOM 更新后再渲染图表
        this.$nextTick(() => {
          this.renderDrugChart(drugName)
        })
      })
    },
    renderTotalInventoryChart () {
      const chartDom = document.getElementById('inventory-chart')
      if (!chartDom) return

      const chart = echarts.init(chartDom)
      this.charts['total'] = chart

      const option = {
        title: {
          text: '药店总体库存预测'
        },
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: this.inventoryData.dates
        },
        yAxis: {
          type: 'value',
          name: '库存数量'
        },
        series: [{
          data: this.inventoryData.predictedValues,
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
        }]
      }
      chart.setOption(option)
    },
    onTabChange (activeKey) {
      // 延迟一小段时间确保DOM已更新
      this.$nextTick(() => {
        // 重新渲染当前激活的药品图表
        this.renderDrugChart(activeKey)
      })
    },
    renderDrugChart (drugName) {
      if (!this.inventoryData.drugPredictions[drugName]) return

      const elementId = 'drug-chart-' + drugName.replace(/\s+/g, '-')
      const chartDom = document.getElementById(elementId)

      if (!chartDom) return

      // 如果图表已存在，先销毁
      if (this.charts[drugName]) {
        this.charts[drugName].dispose()
      }

      const chart = echarts.init(chartDom)
      this.charts[drugName] = chart

      const option = {
        title: {
          text: `${drugName} 库存预测`
        },
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: this.inventoryData.dates
        },
        yAxis: {
          type: 'value',
          name: '库存数量'
        },
        series: [{
          data: this.inventoryData.drugPredictions[drugName],
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
        }]
      }

      chart.setOption(option)
    }
  },
  beforeDestroy () {
    // 销毁所有图表实例
    Object.values(this.charts).forEach(chart => {
      if (chart) {
        chart.dispose()
      }
    })
  }
}
</script>

<style scoped>.inventory-prediction-container {
  width: 100%;
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100%;
}

.pharmacy-selector {
  width: 320px;
  margin-bottom: 24px;
}

.prediction-content {
  width: 100%;
}

.inventory-chart {
  width: 100%;
  height: 450px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 16px;
  margin-bottom: 24px;
}

.drug-predictions-card {
  margin-bottom: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.drug-predictions-card :deep(.ant-card-head) {
  background-color: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.drug-tabs {
  width: 100%;
}

.drug-chart {
  width: 100%;
  height: 350px;
  background: #ffffff;
}

.prediction-details-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 25px;
  width: 55%;
}

.prediction-details-card :deep(.ant-card-head) {
  background-color: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.prediction-details-card :deep(.ant-descriptions-item-label) {
  font-weight: 500;
  color: #595959;
}

.detail-item {
  display: flex;
  flex-direction: column;
  margin-bottom: 12px;
}

.detail-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
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
