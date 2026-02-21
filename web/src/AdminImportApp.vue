<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'

const tabs = [
  { key: 'import', title: '导入中心', desc: '批量 URL 导入与受理反馈' },
  { key: 'logs', title: '运行日志', desc: '统一查看 Java/Python 运行日志' },
]

const activeTab = ref('import')

const auth = reactive({
  checking: true,
  authenticated: false,
  userId: null,
  username: '',
  loggingIn: false,
  loginError: '',
})

const loginForm = reactive({
  username: 'admin',
  password: '',
})

const form = reactive({
  platformCode: '',
  urlsText: '',
})

const platformOptions = ref([])
const loadingPlatforms = ref(false)
const submitting = ref(false)
const statusText = ref('请先登录后台，再选择平台并粘贴 URL（每行一个）')
const lastResult = ref(null)
const progressLoading = ref(false)
const progressError = ref('')
const progressData = ref(null)
const progressTimer = ref(0)
const runtimeLogFilters = reactive({
  runtime: 'java',
  level: '',
  keyword: '',
  startTime: '',
  endTime: '',
  limit: 50,
})
const runtimeLogsLoading = ref(false)
const runtimeLogsError = ref('')
const runtimeLogResult = ref(null)

const POLL_INTERVAL_MS = 3000

const parsedUrls = computed(() =>
  form.urlsText
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter((item) => item !== ''),
)

const previewCount = computed(() => parsedUrls.value.length)

const isProgressDone = computed(() => !!progressData.value?.allFinished)

function clearProgressState() {
  progressError.value = ''
  progressData.value = null
  stopProgressPolling()
}

function stopProgressPolling() {
  if (progressTimer.value) {
    window.clearInterval(progressTimer.value)
    progressTimer.value = 0
  }
}

function clearForm() {
  form.urlsText = ''
  lastResult.value = null
  clearProgressState()
  statusText.value = '已清空输入，可重新粘贴 URL 列表'
}

async function readResult(response) {
  const result = await response.json()
  if (!response.ok || !result?.success) {
    throw new Error(result?.message || '请求失败，请稍后重试')
  }
  return result.data
}

async function fetchTaskProgress() {
  if (!lastResult.value?.taskIds?.length) {
    return
  }
  progressLoading.value = true
  progressError.value = ''
  try {
    const response = await fetch('/admin/import/tasks/progress', {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        taskIds: lastResult.value.taskIds,
      }),
    })
    progressData.value = await readResult(response)
    const progress = progressData.value
    statusText.value = `执行进度：${progress.finishedCount}/${progress.totalCount}（成功 ${progress.successCount}，失败 ${progress.failedCount}）`
    if (progress.allFinished) {
      stopProgressPolling()
    }
  } catch (error) {
    progressError.value = error.message
    statusText.value = `进度查询失败：${error.message}`
    stopProgressPolling()
  } finally {
    progressLoading.value = false
  }
}

function startProgressPolling() {
  stopProgressPolling()
  fetchTaskProgress()
  progressTimer.value = window.setInterval(() => {
    fetchTaskProgress()
  }, POLL_INTERVAL_MS)
}

async function fetchProfile() {
  auth.checking = true
  auth.loginError = ''
  try {
    const response = await fetch('/admin/auth/profile', {
      credentials: 'include',
    })
    const data = await readResult(response)
    auth.authenticated = true
    auth.userId = data.userId
    auth.username = data.username
    statusText.value = `登录状态已确认，当前管理员：${data.username}`
  } catch {
    auth.authenticated = false
    auth.userId = null
    auth.username = ''
    statusText.value = '未登录，请先完成后台登录'
  } finally {
    auth.checking = false
  }
}

async function login() {
  auth.loggingIn = true
  auth.loginError = ''
  try {
    const response = await fetch('/admin/auth/login', {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: loginForm.username,
        password: loginForm.password,
      }),
    })
    await readResult(response)
    loginForm.password = ''
    await fetchProfile()
    await fetchPlatforms()
  } catch (error) {
    auth.loginError = error.message
    statusText.value = `登录失败：${error.message}`
  } finally {
    auth.loggingIn = false
  }
}

async function logout() {
  try {
    await fetch('/admin/auth/logout', {
      method: 'POST',
      credentials: 'include',
    })
  } finally {
    stopProgressPolling()
    auth.authenticated = false
    auth.userId = null
    auth.username = ''
    platformOptions.value = []
    form.platformCode = ''
    lastResult.value = null
    progressData.value = null
    progressError.value = ''
    runtimeLogResult.value = null
    runtimeLogsError.value = ''
    statusText.value = '已退出登录'
  }
}

async function fetchPlatforms() {
  if (!auth.authenticated) {
    statusText.value = '请先登录后再加载平台列表'
    return
  }
  loadingPlatforms.value = true
  try {
    const response = await fetch('/admin/import/platform-options', {
      credentials: 'include',
    })
    const data = await readResult(response)
    platformOptions.value = data || []
    if (!form.platformCode && platformOptions.value.length > 0) {
      form.platformCode = platformOptions.value[0].platformCode
    }
    statusText.value = `平台加载成功：${platformOptions.value.length} 个可用平台`
  } catch (error) {
    platformOptions.value = []
    statusText.value = `平台加载失败：${error.message}`
  } finally {
    loadingPlatforms.value = false
  }
}

async function fetchRuntimeLogs() {
  if (!auth.authenticated) {
    runtimeLogsError.value = '请先登录后再查询运行日志'
    return
  }
  runtimeLogsLoading.value = true
  runtimeLogsError.value = ''
  try {
    const payload = {
      runtime: runtimeLogFilters.runtime,
      level: runtimeLogFilters.level || undefined,
      keyword: runtimeLogFilters.keyword || undefined,
      startTime: runtimeLogFilters.startTime || undefined,
      endTime: runtimeLogFilters.endTime || undefined,
      limit: runtimeLogFilters.limit,
    }
    const response = await fetch('/admin/runtime-logs/query', {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    })
    runtimeLogResult.value = await readResult(response)
  } catch (error) {
    runtimeLogsError.value = error.message
  } finally {
    runtimeLogsLoading.value = false
  }
}

async function submitImport() {
  if (!auth.authenticated) {
    statusText.value = '请先登录后再提交导入任务'
    return
  }
  submitting.value = true
  lastResult.value = null
  clearProgressState()
  try {
    const payload = {
      platformCode: form.platformCode,
      urls: parsedUrls.value,
    }
    const response = await fetch('/admin/import/tasks', {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    })
    const data = await readResult(response)
    lastResult.value = data
    statusText.value = `受理成功：共 ${data.acceptedCount} 条任务已入队`
    if (data.taskIds?.length) {
      startProgressPolling()
    }
  } catch (error) {
    statusText.value = `提交失败：${error.message}`
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchProfile().then(() => {
    if (auth.authenticated) {
      fetchPlatforms()
    }
  })
})

watch(activeTab, (tabKey) => {
  if (tabKey === 'logs' && auth.authenticated) {
    fetchRuntimeLogs()
  }
})

onUnmounted(() => {
  stopProgressPolling()
})
</script>

<template>
  <main class="admin-page">
    <header class="admin-head">
      <div>
        <p class="eyebrow">Tutor Admin System</p>
        <h1 class="title">家教后台管理系统</h1>
        <p class="desc">统一登录、导入、日志的后台工作台（白色系专业控制台）</p>
      </div>
      <div v-if="auth.authenticated" class="user-chip">
        <span class="user-tag">在线</span>
        <span class="user-name">{{ auth.username }}（#{{ auth.userId }}）</span>
        <button class="btn mini" type="button" @click="logout">退出登录</button>
      </div>
    </header>

    <section v-if="auth.checking" class="panel loading-panel">
      <p class="panel-title">正在校验登录态...</p>
      <p class="status">请稍候，系统正在确认后台会话。</p>
    </section>

    <section v-else-if="!auth.authenticated" class="console-layout login-layout">
      <aside class="panel nav-shell">
        <p class="nav-head">功能导航</p>
        <button class="nav-item active" type="button">导入中心</button>
        <button class="nav-item" type="button" disabled>运行日志</button>
      </aside>

      <section class="panel main-panel">
        <h2 class="panel-title">后台登录</h2>
        <p class="status">请输入管理员账号密码，登录后进入「家教后台管理系统」。</p>
        <div class="field">
          <label for="username" class="label">用户名</label>
          <input id="username" v-model="loginForm.username" type="text" autocomplete="username" />
        </div>
        <div class="field">
          <label for="password" class="label">密码</label>
          <input
            id="password"
            v-model="loginForm.password"
            type="password"
            autocomplete="current-password"
            @keyup.enter="login"
          />
        </div>
        <p v-if="auth.loginError" class="error-tip">{{ auth.loginError }}</p>
        <div class="actions one-col">
          <button class="btn primary" type="button" :disabled="auth.loggingIn || !loginForm.password" @click="login">
            {{ auth.loggingIn ? '登录中...' : '登录并进入系统' }}
          </button>
        </div>
      </section>
    </section>

    <section v-else class="console-layout shell-layout">
      <aside class="panel nav-shell">
        <p class="nav-head">功能导航</p>
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="nav-item"
          :class="{ active: activeTab === tab.key }"
          type="button"
          @click="activeTab = tab.key"
        >
          <span class="nav-title">{{ tab.title }}</span>
          <span class="nav-desc">{{ tab.desc }}</span>
        </button>
      </aside>

      <section v-if="activeTab === 'import'" class="panel main-panel">
        <h2 class="panel-title">导入中心</h2>
        <p class="status">{{ statusText }}</p>

        <div class="import-grid">
          <div>
            <div class="field">
              <label for="platform" class="label">平台选择</label>
              <select id="platform" v-model="form.platformCode" :disabled="loadingPlatforms || !platformOptions.length">
                <option v-for="item in platformOptions" :key="item.platformCode" :value="item.platformCode">
                  {{ item.platformName }}（{{ item.platformCode }}）
                </option>
              </select>
            </div>

            <div class="field">
              <label for="urls" class="label">URL 列表（每行一个）</label>
              <textarea
                id="urls"
                v-model="form.urlsText"
                placeholder="https://example.com/a&#10;https://example.com/b"
                rows="10"
              />
              <p class="hint">当前预览 {{ previewCount }} 条，超出 10 条会被拒绝。</p>
            </div>

            <div class="actions">
              <button class="btn primary" type="button" :disabled="submitting || previewCount === 0" @click="submitImport">
                {{ submitting ? '正在受理...' : '提交导入任务' }}
              </button>
              <button class="btn ghost" type="button" :disabled="loadingPlatforms" @click="fetchPlatforms">
                {{ loadingPlatforms ? '刷新中...' : '刷新平台列表' }}
              </button>
              <button class="btn ghost" type="button" :disabled="submitting" @click="clearForm">清空输入</button>
            </div>
          </div>

          <div class="panel nested-panel">
            <h3 class="sub-title">受理反馈</h3>
            <div v-if="lastResult" class="result-grid">
              <article class="metric-card">
                <p class="metric-title">提交条数</p>
                <p class="metric-value">{{ lastResult.submittedCount }}</p>
              </article>
              <article class="metric-card">
                <p class="metric-title">受理条数</p>
                <p class="metric-value">{{ lastResult.acceptedCount }}</p>
              </article>
              <article class="metric-card">
                <p class="metric-title">去重条数</p>
                <p class="metric-value">{{ lastResult.deduplicatedCount }}</p>
              </article>
            </div>

            <div v-if="lastResult" class="task-list">
              <p class="task-title">受理任务 ID</p>
              <div class="task-tags">
                <span v-for="taskId in lastResult.taskIds" :key="taskId" class="task-tag">#{{ taskId }}</span>
              </div>
            </div>

            <div v-if="lastResult" class="task-list progress-wrap">
              <div class="progress-head">
                <p class="task-title">导入进度</p>
                <span class="progress-state" :class="{ done: isProgressDone }">
                  {{ isProgressDone ? '已完成' : '进行中' }}
                </span>
              </div>
              <p v-if="progressLoading" class="hint">正在刷新任务进度...</p>
              <p v-if="progressError" class="error-tip">{{ progressError }}</p>
              <div v-if="progressData" class="result-grid">
                <article class="metric-card">
                  <p class="metric-title">总任务</p>
                  <p class="metric-value">{{ progressData.totalCount }}</p>
                </article>
                <article class="metric-card">
                  <p class="metric-title">已完成</p>
                  <p class="metric-value">{{ progressData.finishedCount }}</p>
                </article>
                <article class="metric-card">
                  <p class="metric-title">失败数</p>
                  <p class="metric-value">{{ progressData.failedCount }}</p>
                </article>
              </div>
              <div v-if="progressData?.items?.length" class="progress-items">
                <article v-for="item in progressData.items" :key="item.taskId" class="progress-item">
                  <p class="progress-line">
                    <span class="task-tag">#{{ item.taskId }}</span>
                    <span>{{ item.status }}</span>
                  </p>
                  <p v-if="item.latestErrorType || item.latestErrorMessage" class="hint">
                    {{ item.latestErrorType || 'DETAIL' }}：{{ item.latestErrorMessage || '无' }}
                  </p>
                </article>
              </div>
            </div>

            <div v-if="!lastResult" class="notice">
              <p>说明：</p>
              <p>1) 接口走 Cookie 鉴权，登录态失效时请重新登录。</p>
              <p>2) 同一请求内 URL 自动去重，并回传去重统计。</p>
              <p>3) 导入受理后异步执行，可在日志中心追踪任务。</p>
            </div>
          </div>
        </div>
      </section>

      <section v-else class="panel main-panel">
        <h2 class="panel-title">运行日志中心</h2>
        <p class="status">支持 runtime/级别/关键字/时间范围筛选（仅在线查看，不提供下载）。</p>

        <div class="log-filter-grid">
          <div class="field">
            <label for="runtime" class="label">运行端</label>
            <select id="runtime" v-model="runtimeLogFilters.runtime">
              <option value="java">java</option>
              <option value="python">python</option>
            </select>
          </div>
          <div class="field">
            <label for="level" class="label">级别</label>
            <select id="level" v-model="runtimeLogFilters.level">
              <option value="">全部</option>
              <option value="INFO">INFO</option>
              <option value="WARN">WARN</option>
              <option value="ERROR">ERROR</option>
            </select>
          </div>
          <div class="field">
            <label for="keyword" class="label">关键字</label>
            <input id="keyword" v-model="runtimeLogFilters.keyword" type="text" placeholder="例如：timeout / keyword-hit" />
          </div>
          <div class="field">
            <label for="startTime" class="label">开始时间</label>
            <input id="startTime" v-model="runtimeLogFilters.startTime" type="datetime-local" />
          </div>
          <div class="field">
            <label for="endTime" class="label">结束时间</label>
            <input id="endTime" v-model="runtimeLogFilters.endTime" type="datetime-local" />
          </div>
          <div class="field">
            <label for="limit" class="label">返回条数</label>
            <input id="limit" v-model.number="runtimeLogFilters.limit" type="number" min="1" max="200" />
          </div>
        </div>

        <div class="actions two-col">
          <button class="btn primary" type="button" :disabled="runtimeLogsLoading" @click="fetchRuntimeLogs">
            {{ runtimeLogsLoading ? '查询中...' : '查询日志' }}
          </button>
          <button class="btn ghost" type="button" :disabled="runtimeLogsLoading" @click="runtimeLogFilters.keyword = ''">
            清空关键字
          </button>
        </div>

        <p v-if="runtimeLogsError" class="error-tip">{{ runtimeLogsError }}</p>

        <div v-if="runtimeLogResult" class="task-list">
          <p class="task-title">查询结果：{{ runtimeLogResult.runtime }} / {{ runtimeLogResult.totalCount }} 条</p>
          <div class="runtime-log-list">
            <article v-for="(item, idx) in runtimeLogResult.items" :key="`${item.timestamp}-${idx}`" class="runtime-log-item">
              <p class="runtime-log-head">
                <span class="task-tag">{{ item.level }}</span>
                <span class="runtime-meta">{{ item.timestamp }}</span>
              </p>
              <p class="runtime-log-message">{{ item.message }}</p>
            </article>
          </div>
        </div>
      </section>
    </section>
  </main>
</template>
