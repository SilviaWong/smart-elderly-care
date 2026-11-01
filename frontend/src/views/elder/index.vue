<template>
  <div class="elder-page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="档案编号">
          <el-input v-model="query.elderNo" placeholder="按编号精确查询" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="老人姓名">
          <el-input v-model="query.name" placeholder="模糊搜索姓名" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="query.phone" placeholder="手机号码" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="档案状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="待入住 (PENDING)" value="PENDING" />
            <el-option label="在住 (IN_HOUSE)" value="IN_HOUSE" />
            <el-option label="已退住 (DISCHARGED)" value="DISCHARGED" />
            <el-option label="已归档 (ARCHIVED)" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="自理能力">
          <el-select v-model="query.mobilityLevel" placeholder="全部等级" clearable style="width: 130px">
            <el-option label="自理" value="SELF_CARE" />
            <el-option label="半失能" value="PARTIAL" />
            <el-option label="介护" value="ASSISTED" />
            <el-option label="特护卧床" value="BEDRIDDEN" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="loadElders">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="table-toolbar">
        <div class="title-tip">
          <span class="title">老人档案名册</span>
          <span class="desc">共登记 {{ total }} 位长者档案，支持敏感信息脱敏与跨机构隔离保护</span>
        </div>
        <el-button type="primary" icon="Plus" @click="openCreateDialog">长者建档录入</el-button>
      </div>

      <el-table :data="elderList" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="elderNo" label="档案编号" min-width="140">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="viewDetail(row.id)">
              {{ row.elderNo }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" min-width="110" />
        <el-table-column prop="gender" label="性别" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.gender === 'MALE' ? 'primary' : (row.gender === 'FEMALE' ? 'danger' : 'info')" size="small">
              {{ row.gender === 'MALE' ? '男' : (row.gender === 'FEMALE' ? '女' : '未知') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="年龄" width="90" align="center">
          <template #default="{ row }">
            {{ row.age ? `${row.age} 岁` : '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="phoneMasked" label="联系电话" min-width="130" />
        <el-table-column prop="idNoMasked" label="证件号码" min-width="170" />
        <el-table-column label="主要联系人" min-width="170">
          <template #default="{ row }">
            <span>{{ row.primaryContactName }}</span>
            <span v-if="row.primaryContactPhone !== '--'" class="text-secondary" style="margin-left: 6px; font-size: 12px">
              ({{ row.primaryContactPhone }})
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="mobilityLevel" label="自理等级" min-width="110" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" size="small">{{ formatMobility(row.mobilityLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="建档时间" min-width="160" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row.id)">全景档案</el-button>
            <el-button link type="primary" size="small" @click="openEditDialog(row.id)">编辑</el-button>
            <el-button
              v-if="row.status !== 'ARCHIVED'"
              link
              type="warning"
              size="small"
              @click="handleArchive(row)"
            >
              归档
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadElders"
          @current-change="loadElders"
        />
      </div>
    </el-card>

    <!-- 新增/编辑老人建档弹窗 -->
    <el-dialog
      v-model="formDialogVisible"
      :title="isEdit ? '编辑长者档案' : '新长者建档录入'"
      width="720px"
      destroy-on-close
    >
      <el-form ref="elderFormRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="档案编号" prop="elderNo">
              <el-input v-model="form.elderNo" :disabled="isEdit" placeholder="例如 ELD-2026-001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="老人姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio value="MALE">男</el-radio>
                <el-radio value="FEMALE">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker
                v-model="form.birthDate"
                type="date"
                placeholder="选择出生日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="证件类型" prop="idType">
              <el-select v-model="form.idType" placeholder="请选择证件类型" style="width: 100%">
                <el-option label="居民身份证" value="ID_CARD" />
                <el-option label="护照" value="PASSPORT" />
                <el-option label="其他证件" value="OTHER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证件号码" prop="idNo">
              <el-input
                v-model="form.idNo"
                :placeholder="isEdit ? '留空表示不修改' : '18位身份证号'"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" placeholder="长者常用手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="血型">
              <el-select v-model="form.bloodType" placeholder="选择血型" clearable style="width: 100%">
                <el-option label="A 型" value="A" />
                <el-option label="B 型" value="B" />
                <el-option label="AB 型" value="AB" />
                <el-option label="O 型" value="O" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="自理等级">
              <el-select v-model="form.mobilityLevel" placeholder="选择自理等级" style="width: 100%">
                <el-option label="自理" value="SELF_CARE" />
                <el-option label="半失能" value="PARTIAL" />
                <el-option label="介护" value="ASSISTED" />
                <el-option label="特护卧床" value="BEDRIDDEN" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="isEdit">
            <el-form-item label="档案状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="待入住" value="PENDING" />
                <el-option label="在住" value="IN_HOUSE" />
                <el-option label="已退住" value="DISCHARGED" />
                <el-option label="已归档" value="ARCHIVED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="家庭常住地址">
          <el-input v-model="form.address" placeholder="原户籍或居住地址" />
        </el-form-item>

        <el-divider content-position="left">健康概况与特别关照</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="过敏药物/食物">
              <el-input v-model="form.allergyDesc" type="textarea" :rows="2" placeholder="青霉素、海鲜等" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="慢性病既往史">
              <el-input v-model="form.chronicDiseaseDesc" type="textarea" :rows="2" placeholder="高血压、二型糖尿病等" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="日常用药说明">
              <el-input v-model="form.medicationDesc" type="textarea" :rows="2" placeholder="降压药每日早一片等" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="饮食禁忌/偏好">
              <el-input v-model="form.dietTaboo" type="textarea" :rows="2" placeholder="低盐低脂、忌辛辣、清真等" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="档案备注说明">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="性格特点、家属诉求等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitElderForm">保存档案</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { fetchElders, createElder, updateElder, archiveElder, getElderDetail, ElderItem } from '@/api/elder'

const router = useRouter()

const loading = ref(false)
const elderList = ref<ElderItem[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  elderNo: '',
  name: '',
  phone: '',
  status: '',
  mobilityLevel: ''
})

const formDialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const elderFormRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  elderNo: '',
  name: '',
  gender: 'MALE',
  birthDate: '',
  idType: 'ID_CARD',
  idNo: '',
  phone: '',
  address: '',
  bloodType: '',
  mobilityLevel: 'PARTIAL',
  status: 'PENDING',
  allergyDesc: '',
  chronicDiseaseDesc: '',
  medicationDesc: '',
  dietTaboo: '',
  remark: ''
})

const rules: FormRules = {
  elderNo: [{ required: true, message: '请输入档案编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入老人姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }]
}

async function loadElders() {
  loading.value = true
  try {
    const res = await fetchElders(query)
    elderList.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.elderNo = ''
  query.name = ''
  query.phone = ''
  query.status = ''
  query.mobilityLevel = ''
  query.pageNum = 1
  loadElders()
}

function viewDetail(id: number) {
  router.push(`/elder/detail/${id}`)
}

function openCreateDialog() {
  isEdit.value = false
  editId.value = null
  const nowStr = new Date().toISOString().slice(2, 7).replace('-', '')
  form.elderNo = `ELD-${nowStr}-${Math.floor(100 + Math.random() * 900)}`
  form.name = ''
  form.gender = 'MALE'
  form.birthDate = ''
  form.idType = 'ID_CARD'
  form.idNo = ''
  form.phone = ''
  form.address = ''
  form.bloodType = ''
  form.mobilityLevel = 'PARTIAL'
  form.status = 'PENDING'
  form.allergyDesc = ''
  form.chronicDiseaseDesc = ''
  form.medicationDesc = ''
  form.dietTaboo = ''
  form.remark = ''
  formDialogVisible.value = true
}

async function openEditDialog(id: number) {
  isEdit.value = true
  editId.value = id
  try {
    const res = await getElderDetail(id)
    const d = res.data
    form.elderNo = d.elderNo
    form.name = d.name
    form.gender = d.gender || 'MALE'
    form.birthDate = d.birthDate || ''
    form.idType = d.idType || 'ID_CARD'
    form.idNo = ''
    form.phone = d.phoneMasked || ''
    form.address = d.address || ''
    form.bloodType = d.bloodType || ''
    form.mobilityLevel = d.mobilityLevel || 'PARTIAL'
    form.status = d.status || 'PENDING'
    form.allergyDesc = d.allergyDesc || ''
    form.chronicDiseaseDesc = d.chronicDiseaseDesc || ''
    form.medicationDesc = d.medicationDesc || ''
    form.dietTaboo = d.dietTaboo || ''
    form.remark = d.remark || ''
    formDialogVisible.value = true
  } catch (e) {}
}

async function submitElderForm() {
  if (!elderFormRef.value) return
  elderFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value && editId.value) {
        await updateElder(editId.value, {
          name: form.name,
          gender: form.gender,
          birthDate: form.birthDate,
          idType: form.idType,
          idNo: form.idNo ? form.idNo : undefined,
          phone: form.phone,
          address: form.address,
          bloodType: form.bloodType,
          mobilityLevel: form.mobilityLevel,
          status: form.status,
          allergyDesc: form.allergyDesc,
          chronicDiseaseDesc: form.chronicDiseaseDesc,
          medicationDesc: form.medicationDesc,
          dietTaboo: form.dietTaboo,
          remark: form.remark
        })
        ElMessage.success('老人档案更新成功')
      } else {
        await createElder(form)
        ElMessage.success('老人建档成功')
      }
      formDialogVisible.value = false
      loadElders()
    } finally {
      submitting.value = false
    }
  })
}

async function handleArchive(row: ElderItem) {
  try {
    await ElMessageBox.confirm(`确认将老人 "${row.name}" (${row.elderNo}) 档案归档吗？归档后仍保留全部历史数据。`, '档案归档确认', {
      type: 'warning'
    })
    await archiveElder(row.id)
    ElMessage.success('档案归档完成')
    loadElders()
  } catch (e) {}
}

function formatMobility(code: string) {
  const map: Record<string, string> = {
    SELF_CARE: '自理',
    PARTIAL: '半失能',
    ASSISTED: '介护',
    BEDRIDDEN: '特护卧床'
  }
  return map[code] || code || '--'
}

function formatStatus(status: string) {
  const map: Record<string, string> = {
    PENDING: '待入住',
    IN_HOUSE: '在住',
    DISCHARGED: '已退住',
    ARCHIVED: '已归档'
  }
  return map[status] || status
}

function getStatusType(status: string) {
  const map: Record<string, any> = {
    PENDING: 'warning',
    IN_HOUSE: 'success',
    DISCHARGED: 'info',
    ARCHIVED: ''
  }
  return map[status] || 'info'
}

onMounted(() => {
  loadElders()
})
</script>

<style scoped lang="scss">
.elder-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card, .table-card {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  margin-bottom: 0;
}

.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  .title-tip {
    .title {
      font-size: 16px;
      font-weight: 600;
      color: #0f172a;
      margin-right: 8px;
    }
    .desc {
      font-size: 13px;
      color: #64748b;
    }
  }
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
