<template>
  <div class="not-found-container">
    <el-result
      icon="warning"
      title="404 - 页面未找到"
      sub-title="您访问的功能路径不存在或正在开发中"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/dashboard')">返回工作台</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.not-found-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8fafc;
}
</style>
