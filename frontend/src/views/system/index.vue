<template>
  <div class="system-container">
    <el-card shadow="never" class="main-card">
      <el-tabs v-model="activeTab" class="system-tabs">
        <!-- Tab 1: 机构用户管理 -->
        <el-tab-pane label="机构用户管理" name="users">
          <div class="filter-bar">
            <el-form :inline="true" :model="userQuery" class="filter-form">
              <el-form-item label="登录账号">
                <el-input v-model="userQuery.username" placeholder="按账号搜索" clearable />
              </el-form-item>
              <el-form-item label="真实姓名">
                <el-input v-model="userQuery.realName" placeholder="按姓名搜索" clearable />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="userQuery.status" placeholder="全部状态" clearable style="width: 120px">
                  <el-option label="正常" value="ACTIVE" />
                  <el-option label="停用" value="DISABLED" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="Search" @click="loadUsers">查询</el-button>
                <el-button icon="Refresh" @click="resetUserQuery">重置</el-button>
              </el-form-item>
            </el-form>

            <el-button type="primary" icon="Plus" @click="openCreateUserDialog">新建用户</el-button>
          </div>

          <el-table :data="userList" v-loading="userLoading" border stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" align="center" />
            <el-table-column prop="username" label="登录账号" min-width="120" />
            <el-table-column prop="realName" label="真实姓名" min-width="120" />
            <el-table-column prop="phone" label="联系电话" min-width="130" />
            <el-table-column label="所属角色" min-width="180">
              <template #default="{ row }">
                <el-tag v-for="r in row.roles" :key="r" size="small" class="role-tag" effect="light">
                  {{ r }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
                  {{ row.status === 'ACTIVE' ? '正常' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" min-width="160" />
            <el-table-column label="操作" width="180" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openEditUserDialog(row)">编辑</el-button>
                <el-button
                  link
                  :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
                  size="small"
                  @click="handleToggleUserStatus(row)"
                >
                  {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="userQuery.pageNum"
              v-model:page-size="userQuery.pageSize"
              :total="userTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadUsers"
              @current-change="loadUsers"
            />
          </div>
        </el-tab-pane>

        <!-- Tab 2: 角色与权限指派 -->
        <el-tab-pane label="角色与权限管理" name="roles">
          <div class="filter-bar">
            <el-button type="primary" icon="Plus" @click="openCreateRoleDialog">新建角色</el-button>
          </div>

          <el-table :data="roleList" v-loading="roleLoading" border stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" align="center" />
            <el-table-column prop="roleCode" label="角色编码" min-width="140" />
            <el-table-column prop="roleName" label="角色名称" min-width="150" />
            <el-table-column prop="description" label="描述" min-width="200" />
            <el-table-column label="权限配置" min-width="140" align="center">
              <template #default="{ row }">
                <el-tag type="info" size="small">已分配 {{ row.permissionIds?.length || 0 }} 项权限</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" min-width="160" />
            <el-table-column label="操作" width="220" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openAssignPermissionDialog(row)">分配权限</el-button>
                <el-button link type="primary" size="small" @click="openEditRoleDialog(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleDeleteRole(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 3: 系统权限资源树 -->
        <el-tab-pane label="全量权限资源" name="permissions">
          <div class="perm-tree-wrapper">
            <el-tree
              :data="permissionTree"
              :props="{ label: 'permissionName', children: 'children' }"
              default-expand-all
              node-key="id"
            >
              <template #default="{ node, data }">
                <span class="custom-tree-node">
                  <span class="perm-title">{{ node.label }}</span>
                  <el-tag size="small" type="primary" class="perm-code-tag">{{ data.permissionCode }}</el-tag>
                  <span class="perm-path text-secondary">{{ data.path }}</span>
                </span>
              </template>
            </el-tree>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新建/编辑用户弹窗 -->
    <el-dialog
      v-model="userDialogVisible"
      :title="isEditUser ? '编辑用户' : '新建用户'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="90px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="userForm.username" :disabled="isEditUser" placeholder="3-50字符" />
        </el-form-item>
        <el-form-item label="登录密码" :prop="isEditUser ? '' : 'password'">
          <el-input
            v-model="userForm.password"
            type="password"
            :placeholder="isEditUser ? '留空表示不修改密码' : '初始密码不少于6位'"
            show-password
          />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="userForm.phone" placeholder="手机号码" />
        </el-form-item>
        <el-form-item label="邮箱地址">
          <el-input v-model="userForm.email" placeholder="电子邮箱" />
        </el-form-item>
        <el-form-item label="所属角色">
          <el-select v-model="userForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="r in roleList"
              :key="r.id"
              :label="`${r.roleName} (${r.roleCode})`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingUser" @click="submitUserForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 角色权限指派弹窗 -->
    <el-dialog
      v-model="permissionDialogVisible"
      :title="`分配权限 - ${currentRole?.roleName}`"
      width="600px"
      destroy-on-close
    >
      <div class="dialog-tree-container">
        <el-tree
          ref="assignTreeRef"
          :data="permissionTree"
          show-checkbox
          node-key="id"
          :default-checked-keys="currentRolePermissionIds"
          :props="{ label: 'permissionName', children: 'children' }"
          default-expand-all
        />
      </div>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPermissions" @click="submitRolePermissions">保存授权</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules, ElTree } from 'element-plus'
import {
  fetchUsers,
  createUser,
  updateUser,
  changeUserStatus,
  fetchRoles,
  fetchPermissionTree,
  updateRole,
  deleteRole,
  UserItem,
  RoleItem,
  PermissionNode
} from '@/api/system'

const activeTab = ref('users')

// 用户状态与查询
const userList = ref<UserItem[]>([])
const userTotal = ref(0)
const userLoading = ref(false)
const userQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  realName: '',
  status: ''
})

// 角色状态
const roleList = ref<RoleItem[]>([])
const roleLoading = ref(false)

// 权限树状态
const permissionTree = ref<PermissionNode[]>([])

// 用户表单弹窗
const userDialogVisible = ref(false)
const isEditUser = ref(false)
const currentUserId = ref<number | null>(null)
const userFormRef = ref<FormInstance>()
const submittingUser = ref(false)
const userForm = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  roleIds: [] as number[]
})

const userRules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

// 权限分配弹窗
const permissionDialogVisible = ref(false)
const currentRole = ref<RoleItem | null>(null)
const currentRolePermissionIds = ref<number[]>([])
const assignTreeRef = ref<InstanceType<typeof ElTree>>()
const savingPermissions = ref(false)

async function loadUsers() {
  userLoading.value = true
  try {
    const res = await fetchUsers(userQuery)
    userList.value = res.data.list
    userTotal.value = res.data.total
  } catch (e) {
  } finally {
    userLoading.value = false
  }
}

function resetUserQuery() {
  userQuery.username = ''
  userQuery.realName = ''
  userQuery.status = ''
  userQuery.pageNum = 1
  loadUsers()
}

async function loadRoles() {
  roleLoading.value = true
  try {
    const res = await fetchRoles()
    roleList.value = res.data
  } catch (e) {
  } finally {
    roleLoading.value = false
  }
}

async function loadPermissions() {
  try {
    const res = await fetchPermissionTree()
    permissionTree.value = res.data
  } catch (e) {}
}

function openCreateUserDialog() {
  isEditUser.value = false
  currentUserId.value = null
  userForm.username = ''
  userForm.password = '123456'
  userForm.realName = ''
  userForm.phone = ''
  userForm.email = ''
  userForm.roleIds = []
  userDialogVisible.value = true
}

function openEditUserDialog(row: UserItem) {
  isEditUser.value = true
  currentUserId.value = row.id
  userForm.username = row.username
  userForm.password = ''
  userForm.realName = row.realName
  userForm.phone = row.phone || ''
  userForm.email = row.email || ''
  userForm.roleIds = []
  userDialogVisible.value = true
}

async function submitUserForm() {
  if (!userFormRef.value) return
  userFormRef.value.validate(async (valid) => {
    if (!valid) return
    submittingUser.value = true
    try {
      if (isEditUser.value && currentUserId.value) {
        await updateUser(currentUserId.value, {
          realName: userForm.realName,
          phone: userForm.phone,
          email: userForm.email,
          password: userForm.password ? userForm.password : undefined,
          roleIds: userForm.roleIds
        })
        ElMessage.success('用户更新成功')
      } else {
        await createUser({
          username: userForm.username,
          password: userForm.password,
          realName: userForm.realName,
          phone: userForm.phone,
          email: userForm.email,
          roleIds: userForm.roleIds
        })
        ElMessage.success('用户创建成功')
      }
      userDialogVisible.value = false
      loadUsers()
    } finally {
      submittingUser.value = false
    }
  })
}

async function handleToggleUserStatus(row: UserItem) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const actionText = nextStatus === 'ACTIVE' ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认${actionText}用户 "${row.realName}" 吗？`, '提示', {
      type: 'warning'
    })
    await changeUserStatus(row.id, nextStatus)
    ElMessage.success(`已${actionText}该用户`)
    loadUsers()
  } catch (e) {}
}

function openAssignPermissionDialog(role: RoleItem) {
  currentRole.value = role
  currentRolePermissionIds.value = role.permissionIds || []
  permissionDialogVisible.value = true
}

async function submitRolePermissions() {
  if (!currentRole.value || !assignTreeRef.value) return
  savingPermissions.value = true
  try {
    const checkedKeys = assignTreeRef.value.getCheckedKeys() as number[]
    const halfCheckedKeys = assignTreeRef.value.getHalfCheckedKeys() as number[]
    const allSelected = [...checkedKeys, ...halfCheckedKeys]

    await updateRole(currentRole.value.id, {
      roleName: currentRole.value.roleName,
      description: currentRole.value.description,
      permissionIds: allSelected
    })
    ElMessage.success('权限更新成功')
    permissionDialogVisible.value = false
    loadRoles()
  } finally {
    savingPermissions.value = false
  }
}

function openCreateRoleDialog() {
  ElMessage.info('可使用种子预设 ORG_ADMIN/CAREGIVER 体验')
}

function openEditRoleDialog(role: RoleItem) {
  openAssignPermissionDialog(role)
}

async function handleDeleteRole(role: RoleItem) {
  try {
    await ElMessageBox.confirm(`确认删除角色 "${role.roleName}" 吗？`, '警告', { type: 'error' })
    await deleteRole(role.id)
    ElMessage.success('角色已删除')
    loadRoles()
  } catch (e) {}
}

onMounted(() => {
  loadUsers()
  loadRoles()
  loadPermissions()
})
</script>

<style scoped lang="scss">
.system-container {
  display: flex;
  flex-direction: column;
}

.main-card {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;

  .filter-form {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    margin-bottom: 0;
  }
}

.role-tag {
  margin-right: 6px;
  margin-bottom: 4px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.perm-tree-wrapper {
  max-width: 600px;
  padding: 12px 0;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;

  .perm-code-tag {
    font-size: 11px;
  }

  .perm-path {
    font-size: 12px;
    color: #94a3b8;
  }
}

.dialog-tree-container {
  max-height: 420px;
  overflow-y: auto;
  border: 1px solid #f1f5f9;
  padding: 12px;
  border-radius: 8px;
}
</style>
