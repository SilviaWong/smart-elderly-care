<template>
  <el-card shadow="never" class="placeholder-card">
    <el-empty :description="`${moduleName} · 将在后续阶段完成接入`">
      <template #extra>
        <el-tag type="info">模块化单体 · 遵循阶段计划逐步开发</el-tag>
      </template>
    </el-empty>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const moduleNames: Record<string, string> = {
  '/elder': '老人档案管理 (阶段 3)',
  '/admission': '空间资源与入住管理 (阶段 4)',
  '/serviceorder': '服务项目与工单流转 (阶段 5)',
  '/health': '体征监测与健康预警 (阶段 6)',
  '/care': '护理等级与计划调度 (阶段 7)',
  '/dining': '餐饮订餐与菜单管理 (阶段 9)',
  '/finance': '费用账单与收款退款 (阶段 10)',
  '/iot': '设备台账与物联感知 (阶段 11)',
  '/risk': '安全与风险事件 (阶段 8)',
  '/report': '运营报表与监管导出 (阶段 12/13)',
  '/system': '系统用户与机构权限 (阶段 2)'
}

const moduleName = computed(() => moduleNames[route.path] || '业务功能模块')
</script>

<style scoped>
.placeholder-card {
  min-height: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}
</style>
