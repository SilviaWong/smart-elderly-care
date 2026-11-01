<template>
  <div class="elder-detail-page" v-loading="loading">
    <!-- 顶栏概览与操作卡片 -->
    <el-card shadow="never" class="header-card">
      <div class="header-content">
        <div class="left-info">
          <el-avatar :size="64" class="elder-avatar">
            {{ elder?.name ? elder.name.slice(0, 1) : '长' }}
          </el-avatar>
          <div class="profile-meta">
            <div class="name-row">
              <span class="elder-name">{{ elder?.name }}</span>
              <el-tag size="small" :type="elder?.gender === 'MALE' ? 'primary' : 'danger'">
                {{ elder?.gender === 'MALE' ? '男' : '女' }}
              </el-tag>
              <span class="elder-age" v-if="elder?.age">{{ elder.age }} 岁</span>
              <el-tag :type="getStatusType(elder?.status || '')" size="small">
                {{ formatStatus(elder?.status || '') }}
              </el-tag>
              <el-tag effect="plain" size="small">{{ formatMobility(elder?.mobilityLevel || '') }}</el-tag>
            </div>
            <div class="sub-row">
              <span>档案编号：<strong>{{ elder?.elderNo }}</strong></span>
              <span style="margin-left: 16px">建档机构：阳光馨园康养中心</span>
              <span style="margin-left: 16px">建档人：{{ elder?.creatorName }}</span>
              <span style="margin-left: 16px">建档时间：{{ elder?.createdAt }}</span>
            </div>
          </div>
        </div>

        <div class="right-actions">
          <el-button @click="$router.push('/elder')">返回列表</el-button>
          <el-button
            v-if="elder?.status !== 'ARCHIVED'"
            type="warning"
            plain
            @click="handleArchive"
          >
            归档档案
          </el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" class="detail-grid">
      <!-- 左列：基本信息与健康档案 -->
      <el-col :span="12">
        <el-card shadow="never" class="info-card">
          <template #header>
            <div class="card-header">
              <span>📋 基本身份档案</span>
            </div>
          </template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="证件类型">{{ elder?.idType || '居民身份证' }}</el-descriptions-item>
            <el-descriptions-item label="证件号码">{{ elder?.idNoMasked || '--' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ elder?.phoneMasked || '--' }}</el-descriptions-item>
            <el-descriptions-item label="出生日期">{{ elder?.birthDate || '--' }}</el-descriptions-item>
            <el-descriptions-item label="家庭常住地址" :span="2">{{ elder?.address || '--' }}</el-descriptions-item>
            <el-descriptions-item label="建档备注" :span="2">{{ elder?.remark || '无' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" class="info-card" style="margin-top: 16px">
          <template #header>
            <div class="card-header">
              <span>🩺 医疗与生活照护概况</span>
            </div>
          </template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="血型">{{ elder?.bloodType ? `${elder.bloodType} 型` : '未登记' }}</el-descriptions-item>
            <el-descriptions-item label="自理等级">{{ formatMobility(elder?.mobilityLevel || '') }}</el-descriptions-item>
            <el-descriptions-item label="过敏药物/食物" :span="2">
              <span class="text-danger">{{ elder?.allergyDesc || '无已知过敏记录' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="慢性病既往史" :span="2">
              {{ elder?.chronicDiseaseDesc || '无重大慢性病' }}
            </el-descriptions-item>
            <el-descriptions-item label="日常用药说明" :span="2">
              {{ elder?.medicationDesc || '暂无用药登记' }}
            </el-descriptions-item>
            <el-descriptions-item label="膳食禁忌与偏好" :span="2">
              {{ elder?.dietTaboo || '常规膳食' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右列：联系人列表与评估记录 -->
      <el-col :span="12">
        <!-- 联系人卡片 -->
        <el-card shadow="never" class="info-card">
          <template #header>
            <div class="card-header">
              <span>👨‍👩‍👧 紧急与家属联系人</span>
              <el-button type="primary" link icon="Plus" @click="openCreateContactDialog">新增联系人</el-button>
            </div>
          </template>
          <el-table :data="elder?.contacts" size="small" border stripe>
            <el-table-column prop="contactName" label="姓名" min-width="90" />
            <el-table-column prop="relationship" label="关系" width="90">
              <template #default="{ row }">
                {{ formatRelationship(row.relationship) }}
              </template>
            </el-table-column>
            <el-table-column prop="phoneMasked" label="电话" min-width="120" />
            <el-table-column label="属性" width="130" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isPrimary === 1" type="success" size="small" style="margin-right: 4px">首要</el-tag>
                <el-tag v-if="row.isEmergency === 1" type="danger" size="small">紧急</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openEditContactDialog(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleDeleteContact(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 能力评估历史卡片 -->
        <el-card shadow="never" class="info-card" style="margin-top: 16px">
          <template #header>
            <div class="card-header">
              <span>📊 能力与风险评估历史</span>
              <el-button type="primary" link icon="Plus" @click="openCreateAssessmentDialog">登记评估</el-button>
            </div>
          </template>
          <el-table :data="elder?.assessments" size="small" border stripe>
            <el-table-column prop="assessmentType" label="评估类型" min-width="90" />
            <el-table-column prop="assessmentDate" label="评估日期" width="105" />
            <el-table-column prop="score" label="综合得分" width="85" align="center" />
            <el-table-column prop="resultLevel" label="结论等级" min-width="100">
              <template #default="{ row }">
                <el-tag effect="plain" size="small">{{ row.resultLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="assessorName" label="评估人" width="90" />
            <el-table-column prop="remark" label="说明" min-width="120" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 联系人增改弹窗 -->
    <el-dialog
      v-model="contactDialogVisible"
      :title="isEditContact ? '编辑联系人' : '新增联系人'"
      width="480px"
      destroy-on-close
    >
      <el-form ref="contactFormRef" :model="contactForm" :rules="contactRules" label-width="90px">
        <el-form-item label="姓名" prop="contactName">
          <el-input v-model="contactForm.contactName" placeholder="联系人姓名" />
        </el-form-item>
        <el-form-item label="与长者关系" prop="relationship">
          <el-select v-model="contactForm.relationship" placeholder="请选择关系" style="width: 100%">
            <el-option label="儿子 (SON)" value="SON" />
            <el-option label="女儿 (DAUGHTER)" value="DAUGHTER" />
            <el-option label="配偶 (SPOUSE)" value="SPOUSE" />
            <el-option label="亲属 (RELATIVE)" value="RELATIVE" />
            <el-option label="监护人/朋友 (OTHER)" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="contactForm.phone" placeholder="手机号码" />
        </el-form-item>
        <el-form-item label="联系地址">
          <el-input v-model="contactForm.address" placeholder="常住地址" />
        </el-form-item>
        <el-form-item label="联系人属性">
          <el-checkbox v-model="contactForm.isPrimary" :true-value="1" :false-value="0">首要联系人（唯一）</el-checkbox>
          <el-checkbox v-model="contactForm.isEmergency" :true-value="1" :false-value="0">紧急通知人</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contactDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingContact" @click="submitContactForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 评估记录弹窗 -->
    <el-dialog
      v-model="assessmentDialogVisible"
      title="登记长者能力评估"
      width="520px"
      destroy-on-close
    >
      <el-form ref="assessmentFormRef" :model="assessmentForm" :rules="assessmentRules" label-width="90px">
        <el-form-item label="评估类型" prop="assessmentType">
          <el-select v-model="assessmentForm.assessmentType" placeholder="选择评估量表" style="width: 100%">
            <el-option label="日常生活自理能力 (ADL量表)" value="ADL" />
            <el-option label="认知能力评估 (MMSE)" value="COGNITIVE" />
            <el-option label="跌倒风险评估 (Morse)" value="FALL_RISK" />
            <el-option label="综合照护等级评定" value="COMPREHENSIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估日期" prop="assessmentDate">
          <el-date-picker
            v-model="assessmentForm.assessmentDate"
            type="date"
            placeholder="执行日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="综合得分" prop="score">
          <el-input-number v-model="assessmentForm.score" :min="0" :max="100" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="评定等级" prop="resultLevel">
          <el-select v-model="assessmentForm.resultLevel" placeholder="选择评定结论" style="width: 100%">
            <el-option label="能力完好 (自理)" value="能力完好" />
            <el-option label="轻度失能" value="轻度失能" />
            <el-option label="中度失能" value="中度失能" />
            <el-option label="重度失能" value="重度失能" />
            <el-option label="特重度失能" value="特重度失能" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估说明">
          <el-input v-model="assessmentForm.remark" type="textarea" :rows="3" placeholder="特别需要护理关照项等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assessmentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingAssessment" @click="submitAssessmentForm">保存评估</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import {
  getElderDetail,
  archiveElder,
  createContact,
  updateContact,
  deleteContact,
  createAssessment,
  ElderDetail,
  ContactItem
} from '@/api/elder'

const route = useRoute()
const elderId = Number(route.params.id)

const loading = ref(false)
const elder = ref<ElderDetail | null>(null)

// 联系人弹窗状态
const contactDialogVisible = ref(false)
const isEditContact = ref(false)
const currentContactId = ref<number | null>(null)
const contactFormRef = ref<FormInstance>()
const submittingContact = ref(false)
const contactForm = reactive({
  contactName: '',
  relationship: 'SON',
  phone: '',
  address: '',
  isPrimary: 1,
  isEmergency: 1
})

const contactRules: FormRules = {
  contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

// 评估弹窗状态
const assessmentDialogVisible = ref(false)
const assessmentFormRef = ref<FormInstance>()
const submittingAssessment = ref(false)
const assessmentForm = reactive({
  assessmentType: 'ADL',
  assessmentDate: new Date().toISOString().slice(0, 10),
  score: 80,
  resultLevel: '轻度失能',
  remark: ''
})

const assessmentRules: FormRules = {
  assessmentType: [{ required: true, message: '请选择评估类型', trigger: 'change' }],
  assessmentDate: [{ required: true, message: '请选择评估日期', trigger: 'change' }],
  resultLevel: [{ required: true, message: '请选择评定等级', trigger: 'change' }]
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getElderDetail(elderId)
    elder.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleArchive() {
  try {
    await ElMessageBox.confirm('确认归档此长者档案吗？', '提示', { type: 'warning' })
    await archiveElder(elderId)
    ElMessage.success('档案归档完成')
    loadDetail()
  } catch (e) {}
}

function openCreateContactDialog() {
  isEditContact.value = false
  currentContactId.value = null
  contactForm.contactName = ''
  contactForm.relationship = 'SON'
  contactForm.phone = ''
  contactForm.address = ''
  contactForm.isPrimary = elder.value?.contacts?.length ? 0 : 1
  contactForm.isEmergency = 1
  contactDialogVisible.value = true
}

function openEditContactDialog(contact: ContactItem) {
  isEditContact.value = true
  currentContactId.value = contact.id
  contactForm.contactName = contact.contactName
  contactForm.relationship = contact.relationship || 'OTHER'
  contactForm.phone = contact.phone || ''
  contactForm.address = contact.address || ''
  contactForm.isPrimary = contact.isPrimary
  contactForm.isEmergency = contact.isEmergency
  contactDialogVisible.value = true
}

async function submitContactForm() {
  if (!contactFormRef.value) return
  contactFormRef.value.validate(async (valid) => {
    if (!valid) return
    submittingContact.value = true
    try {
      if (isEditContact.value && currentContactId.value) {
        await updateContact(elderId, currentContactId.value, contactForm)
        ElMessage.success('联系人更新成功')
      } else {
        await createContact(elderId, contactForm)
        ElMessage.success('联系人添加成功')
      }
      contactDialogVisible.value = false
      loadDetail()
    } finally {
      submittingContact.value = false
    }
  })
}

async function handleDeleteContact(contactId: number) {
  try {
    await ElMessageBox.confirm('确认删除该联系人记录吗？', '提示', { type: 'warning' })
    await deleteContact(elderId, contactId)
    ElMessage.success('已删除联系人')
    loadDetail()
  } catch (e) {}
}

function openCreateAssessmentDialog() {
  assessmentForm.assessmentType = 'ADL'
  assessmentForm.assessmentDate = new Date().toISOString().slice(0, 10)
  assessmentForm.score = 80
  assessmentForm.resultLevel = '轻度失能'
  assessmentForm.remark = ''
  assessmentDialogVisible.value = true
}

async function submitAssessmentForm() {
  if (!assessmentFormRef.value) return
  assessmentFormRef.value.validate(async (valid) => {
    if (!valid) return
    submittingAssessment.value = true
    try {
      await createAssessment(elderId, assessmentForm)
      ElMessage.success('评估记录登记成功')
      assessmentDialogVisible.value = false
      loadDetail()
    } finally {
      submittingAssessment.value = false
    }
  })
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

function formatRelationship(rel: string) {
  const map: Record<string, string> = {
    SON: '儿子',
    DAUGHTER: '女儿',
    SPOUSE: '配偶',
    RELATIVE: '亲属',
    OTHER: '其他'
  }
  return map[rel] || rel || '其他'
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped lang="scss">
.elder-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header-card, .info-card {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .left-info {
    display: flex;
    align-items: center;
    gap: 18px;

    .elder-avatar {
      background: linear-gradient(135deg, #0284c7, #0369a1);
      font-size: 24px;
      font-weight: 700;
      color: #fff;
    }

    .profile-meta {
      display: flex;
      flex-direction: column;
      gap: 6px;

      .name-row {
        display: flex;
        align-items: center;
        gap: 10px;

        .elder-name {
          font-size: 22px;
          font-weight: 700;
          color: #0f172a;
        }

        .elder-age {
          font-size: 14px;
          color: #64748b;
        }
      }

      .sub-row {
        font-size: 13px;
        color: #64748b;
      }
    }
  }

  .right-actions {
    display: flex;
    gap: 12px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 15px;
  color: #1e293b;
}

.text-danger {
  color: #ef4444;
}
</style>
