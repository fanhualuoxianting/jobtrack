<template>
  <div class="page">
    <el-row :gutter="16">
      <el-col :xs="24" :md="9">
        <el-card>
          <template #header><span class="card-title">上传简历</span></template>
          <el-form label-position="top">
            <el-form-item label="版本名称（可选）">
              <el-input v-model="versionName" maxlength="100" placeholder="留空使用文件名" />
            </el-form-item>
            <el-form-item label="备注（可选）">
              <el-input v-model="uploadNote" maxlength="500" placeholder="如：Java 后端通用版" />
            </el-form-item>
            <el-form-item>
              <el-upload
                ref="uploadRef"
                drag
                :auto-upload="false"
                :limit="1"
                accept=".pdf,.docx"
                :on-change="onFileChange"
                :on-exceed="onFileExceed"
                class="uploader"
              >
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
                <template #tip>
                  <div class="el-upload__tip">仅支持 PDF / DOCX，不超过 10MB；内容相同将被拒绝</div>
                </template>
              </el-upload>
            </el-form-item>
            <el-button
              type="primary"
              class="upload-btn"
              :loading="uploading"
              :disabled="!selectedFile"
              @click="submitUpload"
            >
              上传
            </el-button>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="15">
        <el-card>
          <template #header>
            <div class="card-header">
              <span class="card-title">简历版本</span>
              <el-button text :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
            </div>
          </template>
          <el-table :data="resumes" v-loading="loading" :empty-text="emptyText">
            <el-table-column label="版本" min-width="170">
              <template #default="{ row }">
                <div class="version-cell">
                  <el-icon color="#409eff"><Document /></el-icon>
                  <div>
                    <div class="version-name">
                      {{ row.versionName }}
                      <el-tag v-if="row.isDefault" size="small" type="success">默认</el-tag>
                    </div>
                    <div class="file-name" :title="row.originalFileName">{{ row.originalFileName }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.mimeType.includes('pdf') ? 'danger' : 'primary'">
                  {{ row.mimeType.includes('pdf') ? 'PDF' : 'DOCX' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="大小" width="100" align="right">
              <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column label="上传时间" width="165">
              <template #default="{ row }">{{ formatTime(row.uploadedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link :loading="downloadingId === row.id" @click="handleDownload(row)">下载</el-button>
                <el-button type="primary" link @click="openRename(row)">改名</el-button>
                <el-button v-if="!row.isDefault" type="warning" link @click="handleSetDefault(row)">设默认</el-button>
                <el-button type="danger" link @click="confirmDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="renameVisible" title="修改简历信息" width="440px" destroy-on-close>
      <el-form ref="renameRef" :model="renameForm" :rules="renameRules" label-width="90px">
        <el-form-item label="版本名称" prop="versionName">
          <el-input v-model="renameForm.versionName" maxlength="100" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="renameForm.note" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" :loading="renaming" @click="submitRename">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Document, Refresh, UploadFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadInstance, UploadRawFile } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AxiosError } from 'axios'
import {
  deleteResume,
  downloadResume,
  fetchResumes,
  setDefaultResume,
  updateResume,
  uploadResume,
  type ResumeInfo,
} from '@/api/resume'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const resumes = ref<ResumeInfo[]>([])
const loading = ref(false)
const loadFailed = ref(false)
const emptyText = computed(() => (loadFailed.value ? '加载失败，请稍后重试' : '暂无简历，请在左侧上传'))

async function loadData() {
  loading.value = true
  loadFailed.value = false
  try {
    resumes.value = (await fetchResumes()).data
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

// ---------- 上传 ----------
const uploadRef = ref<UploadInstance>()
const selectedFile = ref<File | null>(null)
const versionName = ref('')
const uploadNote = ref('')
const uploading = ref(false)

function onFileChange(file: UploadFile) {
  const raw = file.raw as UploadRawFile | undefined
  if (!raw) return
  if (raw.size > 10 * 1024 * 1024) {
    ElMessage.error('文件超过 10MB 限制')
    uploadRef.value?.clearFiles()
    selectedFile.value = null
    return
  }
  const ext = raw.name.split('.').pop()?.toLowerCase()
  if (ext !== 'pdf' && ext !== 'docx') {
    ElMessage.error('仅支持 PDF 或 DOCX 文件')
    uploadRef.value?.clearFiles()
    selectedFile.value = null
    return
  }
  selectedFile.value = raw
}

function onFileExceed(files: File[]) {
  // 替换而非堆积
  uploadRef.value?.clearFiles()
  if (files[0]) uploadRef.value?.handleStart(files[0] as UploadRawFile)
}

async function submitUpload() {
  const file = selectedFile.value
  if (!file) return
  uploading.value = true
  try {
    await uploadResume(file, versionName.value, uploadNote.value)
    ElMessage.success('上传成功')
    uploadRef.value?.clearFiles()
    selectedFile.value = null
    versionName.value = ''
    uploadNote.value = ''
    await loadData()
  } catch (e) {
    const err = e as AxiosError<{ code?: string; message?: string }>
    ElMessage.error(err.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

// ---------- 改名 ----------
const renameVisible = ref(false)
const renaming = ref(false)
const renameRef = ref<FormInstance>()
const renameForm = reactive({ id: 0, versionName: '', note: '' })
const renameRules: FormRules = {
  versionName: [{ required: true, message: '版本名称不能为空', trigger: 'blur' }],
}

function openRename(row: ResumeInfo) {
  renameForm.id = row.id
  renameForm.versionName = row.versionName
  renameForm.note = row.note ?? ''
  renameVisible.value = true
}

async function submitRename() {
  const valid = await renameRef.value?.validate().catch(() => false)
  if (!valid) return
  renaming.value = true
  try {
    await updateResume(renameForm.id, {
      versionName: renameForm.versionName.trim(),
      note: renameForm.note.trim() || null,
    })
    ElMessage.success('修改成功')
    renameVisible.value = false
    await loadData()
  } finally {
    renaming.value = false
  }
}

// ---------- 设默认 ----------
async function handleSetDefault(row: ResumeInfo) {
  await setDefaultResume(row.id)
  ElMessage.success(`「${row.versionName}」已设为默认简历`)
  await loadData()
}

// ---------- 下载 ----------
const downloadingId = ref<number | null>(null)
async function handleDownload(row: ResumeInfo) {
  downloadingId.value = row.id
  try {
    await downloadResume(row.id, authStore.accessToken ?? '')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '下载失败')
  } finally {
    downloadingId.value = null
  }
}

// ---------- 删除 ----------
async function confirmDelete(row: ResumeInfo) {
  try {
    await ElMessageBox.confirm(
      `确定删除简历「${row.versionName}」吗？已被投递引用时将被拒绝。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteResume(row.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (e) {
    const err = e as AxiosError<{ message?: string }>
    ElMessage.error(err.response?.data?.message || '删除失败')
  }
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

function formatTime(iso: string): string {
  return iso ? new Date(iso).toLocaleString('zh-CN', { hour12: false }) : '-'
}

onMounted(loadData)
</script>

<style scoped>
.card-title {
  font-weight: 600;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.uploader {
  width: 100%;
}
.upload-btn {
  width: 100%;
}
.version-cell {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.version-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}
.file-name {
  font-size: 12px;
  color: #909399;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
