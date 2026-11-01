<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="brand-badge">🏡 康养智慧云</div>
        <h2 class="title">智慧养老管理系统</h2>
        <p class="subtitle">面向养老机构的一体化运营与照护平台</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="form"
        :rules="rules"
        class="login-form"
        label-position="top"
        @keyup.enter="handleLogin"
      >
        <el-form-item label="所属机构" prop="orgId">
          <el-select v-model="form.orgId" placeholder="请选择养老机构" style="width: 100%">
            <el-option :value="1" label="阳光馨园康养中心（测试机构1）" />
            <el-option :value="2" label="颐和长者之家（测试机构2）" />
          </el-select>
        </el-form-item>

        <el-form-item label="用户账号" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入管理员或员工账号（如 admin、caregiver1）"
            prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item label="登录密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码（演示初始密码：123456）"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <div class="form-extra">
          <el-checkbox v-model="form.rememberMe">记住登录状态</el-checkbox>
          <span class="default-pwd-hint">默认密码: 123456</span>
        </div>

        <el-button
          type="primary"
          :loading="loading"
          class="submit-btn"
          @click="handleLogin"
        >
          立即登录
        </el-button>
      </el-form>

      <div class="login-footer">
        <p>⚠️ 系统受 RBAC 权限与机构数据隔离保护，所有操作均记录审计日志</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { loginApi } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  orgId: 1,
  username: 'admin',
  password: '',
  rememberMe: true
})

const rules = reactive<FormRules>({
  orgId: [{ required: true, message: '请选择所属机构', trigger: 'change' }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
})

async function handleLogin() {
  if (!loginFormRef.value) return
  loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true

    try {
      const res = await loginApi({
        username: form.username,
        password: form.password,
        orgId: form.orgId
      })

      const data = res.data
      userStore.setToken(data.token)
      userStore.setUserInfo({
        userId: data.userId,
        username: data.username,
        realName: data.realName,
        orgId: data.orgId,
        orgName: data.orgName,
        roles: data.roles
      })
      if (data.orgName) {
        appStore.setOrgName(data.orgName)
      }

      ElMessage.success(`欢迎回来，${data.realName}！`)
      router.push('/dashboard')
    } catch (err: any) {
      // 捕获异常，错误已在 Axios 拦截器中提示
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0369a1 100%);
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 440px;
  background: #ffffff;
  border-radius: 16px;
  padding: 36px 32px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.2), 0 10px 10px -5px rgba(0, 0, 0, 0.04);

  .login-header {
    text-align: center;
    margin-bottom: 28px;

    .brand-badge {
      display: inline-block;
      padding: 4px 12px;
      font-size: 13px;
      font-weight: 600;
      color: #0284c7;
      background: #e0f2fe;
      border-radius: 20px;
      margin-bottom: 12px;
    }

    .title {
      font-size: 24px;
      color: #0f172a;
      font-weight: 700;
      letter-spacing: -0.5px;
      margin-bottom: 6px;
    }

    .subtitle {
      font-size: 14px;
      color: #64748b;
    }
  }

  .form-extra {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;

    .default-pwd-hint {
      font-size: 12px;
      color: #94a3b8;
    }
  }

  .submit-btn {
    width: 100%;
    height: 44px;
    font-size: 16px;
    font-weight: 600;
    border-radius: 8px;
  }

  .login-footer {
    margin-top: 24px;
    text-align: center;
    font-size: 12px;
    color: #94a3b8;
    line-height: 1.5;
  }
}
</style>
