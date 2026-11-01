<template>
  <div class="dashboard-page">
    <!-- 顶部欢迎卡片 -->
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-header">
        <div>
          <h2>欢迎回来，{{ userStore.userInfo?.realName || '机构主管' }}</h2>
          <p class="desc">智慧养老运营平台 · 当前处于阶段 1（工程基础骨架就绪）</p>
        </div>
        <el-button type="primary" :loading="checking" @click="handleCheckHealth">
          测试后端健康探测 (/api/v1/health)
        </el-button>
      </div>

      <!-- 后端联调探针反馈区 -->
      <div v-if="healthResult" class="health-box">
        <el-alert
          :title="`后端连接测试：${healthResult.status === 'UP' ? '通讯正常 (UP)' : '异常'}`"
          type="success"
          :closable="false"
          show-icon
        >
          <div class="health-details">
            <span>服务应用：<strong>{{ healthResult.application }}</strong></span>
            <span>接口版本：<strong>{{ healthResult.version }}</strong></span>
            <span>服务端时钟：<strong>{{ healthResult.serverTime }}</strong></span>
          </div>
        </el-alert>
      </div>
    </el-card>

    <!-- 运营总览看板预留骨架 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">在住在册老人</div>
          <div class="stat-value text-primary">-- <span class="unit">位</span></div>
          <div class="stat-tip">阶段 3 接入老人主档案</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">当前床位占用率</div>
          <div class="stat-value text-success">-- <span class="unit">%</span></div>
          <div class="stat-tip">阶段 4 接入空间与床位分配</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">今日待派工单</div>
          <div class="stat-value text-warning">-- <span class="unit">单</span></div>
          <div class="stat-tip">阶段 5 接入服务工单状态机</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">待处理健康预警</div>
          <div class="stat-value text-danger">-- <span class="unit">条</span></div>
          <div class="stat-tip">阶段 6 接入健康指标与规则预警</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { checkBackendHealth, HealthInfo } from '@/api/health'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const checking = ref(false)
const healthResult = ref<HealthInfo | null>(null)

async function handleCheckHealth() {
  checking.value = true
  try {
    const res = await checkBackendHealth()
    healthResult.value = res.data
    ElMessage.success('后端健康检查通信成功')
  } catch (err: any) {
    ElMessage.error(err.message || '后端连接超时，请确认后端已启动')
  } finally {
    checking.value = false
  }
}
</script>

<style scoped lang="scss">
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.welcome-card {
  border-radius: 12px;
  border: 1px solid #e2e8f0;

  .welcome-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    h2 {
      font-size: 20px;
      color: #0f172a;
      margin-bottom: 6px;
    }

    .desc {
      color: #64748b;
      font-size: 14px;
    }
  }

  .health-box {
    margin-top: 20px;

    .health-details {
      display: flex;
      gap: 24px;
      margin-top: 8px;
      font-size: 13px;
      color: #334155;
    }
  }
}

.stat-cards {
  .stat-card {
    border-radius: 12px;
    border: 1px solid #e2e8f0;

    .stat-title {
      font-size: 14px;
      color: #64748b;
      margin-bottom: 12px;
    }

    .stat-value {
      font-size: 32px;
      font-weight: 700;
      margin-bottom: 8px;

      .unit {
        font-size: 14px;
        font-weight: 400;
        color: #94a3b8;
      }
    }

    .stat-tip {
      font-size: 12px;
      color: #94a3b8;
    }

    .text-primary { color: #2563eb; }
    .text-success { color: #10b981; }
    .text-warning { color: #f59e0b; }
    .text-danger { color: #ef4444; }
  }
}
</style>
