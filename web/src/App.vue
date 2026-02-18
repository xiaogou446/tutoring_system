<script setup>
import { computed, reactive, ref } from 'vue'

const mockRecords = [
  {
    id: 1001,
    sourceUrl: 'https://example.com/item/1001',
    contentBlock:
      '【西湖区】初二学生找数学家教。\n每周三次晚间辅导。\n目标：期末总分提升 30 分。',
    publishedAt: '2026-02-17T20:35:00',
  },
  {
    id: 1002,
    sourceUrl: 'https://example.com/item/1002',
    contentBlock:
      '【滨江区】高一英语辅导。\n周末上午可上课。\n希望老师有带毕业班经验。',
    publishedAt: '2026-02-17T18:10:00',
  },
]

const form = reactive({
  pageNo: 1,
  pageSize: 20,
  sortOrder: 'desc',
  contentKeyword: '',
  gradeKeyword: '',
  subjectKeyword: '',
  addressKeyword: '',
  timeKeyword: '',
  salaryKeyword: '',
  teacherKeyword: '',
})

const records = ref(mockRecords)
const loading = ref(false)
const statusText = ref('请输入筛选条件后按回车或点击“应用筛选”')
const total = ref(mockRecords.length)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / form.pageSize)))

const pagerItems = computed(() => {
  const currentPage = form.pageNo
  const pageCount = totalPages.value
  if (pageCount <= 7) {
    return Array.from({ length: pageCount }, (_, index) => index + 1)
  }

  const items = [1]
  let start = Math.max(2, currentPage - 1)
  let end = Math.min(pageCount - 1, currentPage + 1)

  if (currentPage <= 3) {
    start = 2
    end = 4
  } else if (currentPage >= pageCount - 2) {
    start = pageCount - 3
    end = pageCount - 1
  }

  if (start > 2) {
    items.push('...')
  }
  for (let page = start; page <= end; page += 1) {
    items.push(page)
  }
  if (end < pageCount - 1) {
    items.push('...')
  }

  items.push(pageCount)
  return items
})

function formatTime(value) {
  if (!value) {
    return '--'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

function buildParams() {
  const params = new URLSearchParams()
  Object.entries(form).forEach(([key, value]) => {
    const text = String(value ?? '').trim()
    if (text !== '') {
      params.set(key, text)
    }
  })
  return params
}

async function fetchList() {
  loading.value = true
  try {
    const response = await fetch(`/h5/tutoring-info/page?${buildParams().toString()}`)
    const result = await response.json()
    if (!response.ok || !result?.success) {
      throw new Error(result?.message || '接口返回异常')
    }

    records.value = result?.data?.records || []
    total.value = Number(result?.data?.total || 0)
    statusText.value = `加载成功：第 ${form.pageNo} 页 / 共 ${totalPages.value} 页 · 共 ${total.value} 条`
  } catch (error) {
    records.value = []
    total.value = 0
    statusText.value = `请求失败：${error.message}`
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  form.pageNo = 1
  fetchList()
}

function setPage(page) {
  if (typeof page !== 'number' || page === form.pageNo) {
    return
  }
  form.pageNo = page
  fetchList()
}

</script>

<template>
  <main class="page">
    <header class="head">
      <div>
        <h1 class="title">杭州家教信息</h1>
      </div>
      <p class="status">{{ statusText }}</p>
    </header>

    <section class="box web-box">
      <div class="box-head">
        <span>家教筛选与列表</span>
        <span class="tag">desktop</span>
      </div>

      <div class="web-panel">
        <form class="filter-form" @submit.prevent="applyFilters">
          <div class="filter-grid">
          <div class="field">
            <label class="label" for="content-filter">内容筛选</label>
            <input id="content-filter" v-model="form.contentKeyword" list="content-options" placeholder="可输入任意内容" />
          </div>
          <div class="field">
            <label class="label" for="grade-filter">年级筛选</label>
            <input id="grade-filter" v-model="form.gradeKeyword" list="grade-options" placeholder="可选或自定义" />
          </div>
          <div class="field">
            <label class="label" for="subject-filter">科目筛选</label>
            <input id="subject-filter" v-model="form.subjectKeyword" list="subject-options" placeholder="可选或自定义" />
          </div>
          <div class="field">
            <label class="label" for="address-filter">地址筛选</label>
            <input id="address-filter" v-model="form.addressKeyword" list="address-options" placeholder="杭州各区" />
          </div>
          <div class="field">
            <label class="label" for="time-filter">时间筛选</label>
            <input id="time-filter" v-model="form.timeKeyword" placeholder="例如：周末、晚间" />
          </div>
          <div class="field">
            <label class="label" for="salary-filter">薪资筛选</label>
            <input id="salary-filter" v-model="form.salaryKeyword" placeholder="例如：200-300" />
          </div>
          <div class="field">
            <label class="label" for="teacher-filter">教员筛选</label>
            <input id="teacher-filter" v-model="form.teacherKeyword" placeholder="例如：在职教师" />
          </div>
          <div class="field">
            <label class="label" for="sort-filter">发布时间排序</label>
            <select id="sort-filter" v-model="form.sortOrder">
              <option value="desc">最新优先</option>
              <option value="asc">最早优先</option>
            </select>
          </div>
          </div>

          <div class="actions">
            <button type="submit" class="btn-primary" :disabled="loading">
              {{ loading ? '加载中...' : '应用筛选' }}
            </button>
          </div>
        </form>

        <section class="list-panel">
          <article v-for="row in records" :key="row.id" class="item">
            <div class="item-meta">
              <span>ID #{{ row.id }}</span>
              <span>{{ formatTime(row.publishedAt) }}</span>
            </div>
            <p class="content">{{ row.contentBlock }}</p>
            <div class="card-foot">
              <span></span>
              <a :href="row.sourceUrl" target="_blank" rel="noreferrer" class="link">查看原文</a>
            </div>
          </article>
          <p v-if="!records.length" class="empty">暂无匹配结果，请调整筛选条件。</p>
        </section>

        <footer class="pager">
          <p class="meta">第 {{ form.pageNo }} 页 / 共 {{ totalPages }} 页 · 共 {{ total }} 条</p>
          <div class="pager-actions">
            <button
              v-for="node in pagerItems"
              :key="`p-${node}`"
              class="page-btn"
              :class="{ active: node === form.pageNo, dots: node === '...' }"
              :disabled="node === '...'"
              @click="setPage(node)"
            >
              {{ node }}
            </button>
          </div>
          <div class="size-field">
            <span class="size-title">每页展示</span>
            <select id="size-filter" class="size-select" v-model.number="form.pageSize" @change="applyFilters">
              <option :value="20">20 条</option>
              <option :value="50">50 条</option>
              <option :value="100">100 条</option>
            </select>
          </div>
          </footer>
        </div>
    </section>

    <datalist id="content-options">
      <option value="数学提分" />
      <option value="英语口语" />
      <option value="全科作业" />
    </datalist>
    <datalist id="grade-options">
      <option value="一年级" />
      <option value="二年级" />
      <option value="三年级" />
      <option value="四年级" />
      <option value="五年级" />
      <option value="六年级" />
      <option value="初一" />
      <option value="初二" />
      <option value="初三" />
      <option value="高一" />
      <option value="高二" />
      <option value="高三" />
    </datalist>
    <datalist id="subject-options">
      <option value="语文" />
      <option value="数学" />
      <option value="英语" />
      <option value="物理" />
      <option value="化学" />
      <option value="生物" />
      <option value="历史" />
      <option value="地理" />
      <option value="政治" />
    </datalist>
    <datalist id="address-options">
      <option value="上城区" />
      <option value="拱墅区" />
      <option value="西湖区" />
      <option value="滨江区" />
      <option value="钱塘区" />
      <option value="萧山区" />
      <option value="余杭区" />
      <option value="临平区" />
      <option value="富阳区" />
      <option value="临安区" />
    </datalist>
  </main>
</template>
