<template>
  <el-container class="layout-container">
    <!-- 侧边导航栏 -->
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '230px'" class="layout-aside">
      <div class="sidebar-logo">
        <div class="logo-icon">🏡</div>
        <span v-if="!appStore.sidebarCollapsed" class="logo-text">智慧养老云平台</span>
      </div>
      <el-menu
        :default-active="activeRoute"
        class="sidebar-menu"
        :collapse="appStore.sidebarCollapsed"
        router
        background-color="#0f172a"
        text-color="#94a3b8"
        active-text-color="#38bdf8"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>运营工作台</template>
        </el-menu-item>
        <el-menu-item index="/elder">
          <el-icon><User /></el-icon>
          <template #title>老人档案</template>
        </el-menu-item>
        <el-menu-item index="/admission">
          <el-icon><House /></el-icon>
          <template #title>入住与床位</template>
        </el-menu-item>
        <el-menu-item index="/serviceorder">
          <el-icon><Tickets /></el-icon>
          <template #title>服务工单</template>
        </el-menu-item>
        <el-menu-item index="/health">
          <el-icon><FirstAidKit /></el-icon>
          <template #title>健康管理</template>
        </el-menu-item>
        <el-menu-item index="/care">
          <el-icon><Suitcase /></el-icon>
          <template #title>护理计划</template>
        </el-menu-item>
        <el-menu-item index="/dining">
          <el-icon><Bowl /></el-icon>
          <template #title>餐饮管理</template>
        </el-menu-item>
        <el-menu-item index="/finance">
          <el-icon><Money /></el-icon>
          <template #title>财务管理</template>
        </el-menu-item>
        <el-menu-item index="/iot">
          <el-icon><Cpu /></el-icon>
          <template #title>设备物联</template>
        </el-menu-item>
        <el-menu-item index="/risk">
          <el-icon><Warning /></el-icon>
          <template #title>安全风险</template>
        </el-menu-item>
        <el-menu-item index="/report">
          <el-icon><DataLine /></el-icon>
          <template #title>统计报表</template>
        </el-menu-item>
        <el-menu-item index="/system">
          <el-icon><Setting /></el-icon>
          <template #title>系统设置</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="main-wrapper">
      <!-- 顶栏导航 -->
      <el-header height="60px" class="layout-header">
        <div class="header-left">
          <el-button link @click="appStore.toggleSidebar" class="toggle-btn">
            <el-icon :size="20">
              <Fold v-if="!appStore.sidebarCollapsed" />
              <Expand v-else />
            </el-icon>
          </el-button>
          <el-tag type="success" effect="plain" class="org-badge">
            🏢 {{ appStore.currentOrgName }}
          </el-tag>
        </div>

        <div class="header-right">
          <el-dropdown trigger="click">
            <span class="user-profile">
              <el-avatar :size="32" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
              <span class="username">{{ userStore.userInfo?.realName || '机构管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容主体 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import {
  Odometer,
  User,
  House,
  Tickets,
  FirstAidKit,
  Suitcase,
  Bowl,
  Money,
  Cpu,
  Warning,
  DataLine,
  Setting,
  Fold,
  Expand,
  ArrowDown
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const activeRoute = computed(() => route.path)

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
  width: 100vw;
  display: flex;
}

.layout-aside {
  background-color: #0f172a;
  transition: width 0.3s ease;
  overflow-x: hidden;
  border-right: 1px solid #1e293b;
  display: flex;
  flex-direction: column;

  .sidebar-logo {
    height: 60px;
    display: flex;
    align-items: center;
    padding: 0 16px;
    gap: 12px;
    background-color: #090d16;
    border-bottom: 1px solid #1e293b;

    .logo-icon {
      font-size: 24px;
    }

    .logo-text {
      color: #f8fafc;
      font-size: 16px;
      font-weight: 600;
      white-space: nowrap;
    }
  }

  .sidebar-menu {
    border-right: none;
    flex: 1;
    overflow-y: auto;
  }
}

.main-wrapper {
  display: flex;
  flex-direction: column;
  background-color: #f1f5f9;
}

.layout-header {
  background-color: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .org-badge {
      font-weight: 500;
    }
  }

  .header-right {
    .user-profile {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      color: #334155;
      font-size: 14px;
    }
  }
}

.layout-main {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}
</style>
