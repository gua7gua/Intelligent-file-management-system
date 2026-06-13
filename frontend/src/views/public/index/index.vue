<template>
  <div>
    <section class="home-hero" aria-labelledby="home-title">
      <div class="hero-main">
        <div class="hero-kicker">公开档案服务</div>
        <h1 class="hero-title" id="home-title">智能档案管理系统公众门户</h1>
        <p class="hero-copy">
          面向社会公众开放非密、公开、未销毁的正式档案元数据检索，电子文件下载需登录公众账号并重新完成访问鉴权。
        </p>
        <div class="hero-actions">
          <router-link to="/public/search" class="button"><span class="icon" aria-hidden="true">查</span>公开档案检索</router-link>
          <router-link to="/public/collection" class="button secondary"><span class="icon" aria-hidden="true">集</span>提交征集清单</router-link>
          <router-link to="/public/overview" class="button ghost">进入公众概览</router-link>
        </div>
        <form class="quick-search" @submit.prevent="handleQuickSearch">
          <label class="sr-only" for="quickKeyword">公开档案关键词</label>
          <input id="quickKeyword" v-model="quickKeyword" type="search" placeholder="输入题名、责任者或年度关键词" />
          <button class="button secondary" type="submit">检索</button>
        </form>
      </div>

      <aside class="archive-visual" aria-label="公开馆藏概况">
        <div class="shelf" aria-hidden="true">
          <span v-for="i in 7" :key="i" class="box-spine"></span>
        </div>
        <div class="visual-note">
          <strong>公开统计口径</strong>
          <span>仅汇总正式入库、已上架或纯电子已归档、非密、公开、未销毁档案。</span>
        </div>
        <div class="notice">今日公开检索服务正常，下载操作需登录后记录访问日志。</div>
      </aside>
    </section>

    <div v-if="loading" class="notice">正在加载公开数据…</div>
    <div v-else-if="loadError" class="notice danger">加载失败：{{ loadError }}</div>

    <template v-else>
      <section class="grid four stat-grid" aria-label="公开馆藏统计">
        <div class="metric">
          <span class="label">公开档案总量</span>
          <span class="value">{{ homeData.stats.openArchiveCount.toLocaleString() }}</span>
          <span class="note">正式档案公开范围</span>
        </div>
        <div class="metric">
          <span class="label">近 30 日新增公开</span>
          <span class="value">{{ homeData.stats.latestOpenCount.toLocaleString() }}</span>
          <span class="note">后台入库后进入范围</span>
        </div>
        <div class="metric">
          <span class="label">可在线预览</span>
          <span class="value">{{ homeData.stats.electronicFileCount.toLocaleString() }}</span>
          <span class="note">有正式电子文件</span>
        </div>
        <div class="metric">
          <span class="label">征集入库公开</span>
          <span class="value">{{ homeData.stats.collectionCount.toLocaleString() }}</span>
          <span class="note">协议确认后公开</span>
        </div>
      </section>

      <section class="split-section">
        <div class="card panel">
          <h2 class="section-title">公开档案分类分布</h2>
          <div v-for="cat in homeData.categories" :key="cat.name" class="category-row">
            <strong>{{ cat.name }}</strong>
            <div class="bar"><span :style="{ width: categoryPercent(cat.count) + '%' }"></span></div>
            <span class="muted">{{ cat.count.toLocaleString() }} 件</span>
          </div>
        </div>

        <div class="card panel">
          <h2 class="section-title">社会征集办理进度</h2>
          <ol class="process-list">
            <li><span class="step-no">1</span><span><strong>提交意向</strong><br /><span class="muted">公众填写征集清单，拖拽文件只解析文件名。</span></span></li>
            <li><span class="step-no">2</span><span><strong>联系判断</strong><br /><span class="muted">后台管理员确认征集方向并约定到馆时间。</span></span></li>
            <li><span class="step-no">3</span><span><strong>到馆接收</strong><br /><span class="muted">前台验收实物和电子介质，校验在线协议记录。</span></span></li>
            <li><span class="step-no">4</span><span><strong>入库公开</strong><br /><span class="muted">后台入库、上架后才进入公开利用范围。</span></span></li>
          </ol>
        </div>
      </section>

      <section class="toolbar">
        <div>
          <h2 class="section-title">最近公开批次</h2>
          <p class="page-subtitle">列表仅显示批次级公开统计，不展示未公开档案题名、档号、架位或电子文件。</p>
        </div>
        <router-link to="/public/search" class="button ghost">查看全部</router-link>
      </section>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>档案标题</th>
              <th>来源</th>
              <th>分类</th>
              <th>责任者</th>
              <th>年度</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="homeData.recentArchives.length === 0">
              <td colspan="6"><div class="empty">暂无公开档案</div></td>
            </tr>
            <tr v-for="archive in homeData.recentArchives" :key="archive.id">
              <td>{{ archive.title }}</td>
              <td>{{ sourceLabel(archive.sourceType) }}</td>
              <td>{{ archive.category }}</td>
              <td>{{ archive.responsible }}</td>
              <td>{{ archive.formedYear }}</td>
              <td>
                <router-link :to="`/public/search?archiveId=${archive.id}`" class="button ghost">查看详情</router-link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPublicHome } from '@/api/public'
import type { PublicHomeData } from '@/types/public'

const router = useRouter()

const quickKeyword = ref('')
const loading = ref(true)
const loadError = ref('')

const defaultHomeData: PublicHomeData = {
  stats: { openArchiveCount: 0, electronicFileCount: 0, collectionCount: 0, latestOpenCount: 0 },
  categories: [],
  recentArchives: [],
}
const homeData = ref<PublicHomeData>(defaultHomeData)

function handleQuickSearch() {
  const keyword = quickKeyword.value.trim()
  const target = keyword ? `/public/search?keyword=${encodeURIComponent(keyword)}` : '/public/search'
  router.push(target)
}

function categoryPercent(count: number): number {
  const max = Math.max(...homeData.value.categories.map((c) => c.count), 1)
  return Math.round((count / max) * 100)
}

function sourceLabel(sourceType: string): string {
  const map: Record<string, string> = { transfer: '移交入库', collection: '社会征集', compilation: '编撰' }
  return map[sourceType] || sourceType
}

onMounted(async () => {
  try {
    homeData.value = await getPublicHome()
  } catch (e: any) {
    loadError.value = e.message || '未知错误'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.home-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 24px;
  align-items: stretch;
  margin-bottom: 20px;
}

.hero-main {
  display: grid;
  align-content: center;
  min-height: 360px;
  padding: 34px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: #ffffff;
  box-shadow: var(--shadow);
}

.hero-kicker {
  width: fit-content;
  margin-bottom: 12px;
  padding: 5px 9px;
  border-radius: 999px;
  color: var(--primary-strong);
  background: var(--primary-soft);
  font-size: 13px;
  font-weight: 800;
}

.hero-title {
  max-width: 700px;
  margin: 0;
  font-size: 36px;
  line-height: 1.18;
  font-weight: 850;
}

.hero-copy {
  max-width: 720px;
  margin: 14px 0 0;
  color: var(--muted);
  font-size: 16px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.quick-search {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  margin-top: 22px;
}

.quick-search input {
  min-height: 42px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.archive-visual {
  display: grid;
  gap: 14px;
  min-height: 360px;
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: #fbfcfd;
  box-shadow: var(--shadow);
}

.shelf {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  align-items: end;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: #ffffff;
}

.box-spine {
  min-height: 104px;
  border: 1px solid #c7d5df;
  border-radius: 4px;
  background: #edf5f5;
}

.box-spine:nth-child(2n) {
  min-height: 132px;
  background: #f7efe0;
}

.box-spine:nth-child(3n) {
  min-height: 86px;
  background: #eaf2fb;
}

.visual-note {
  display: grid;
  gap: 8px;
  padding: 12px;
  border-radius: var(--radius);
  background: var(--surface-muted);
  color: var(--muted);
}

.stat-grid {
  margin-bottom: 20px;
}

.split-section {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.8fr);
  gap: 16px;
  margin-top: 16px;
}

.category-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr) 72px;
  gap: 12px;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}

.category-row:last-child {
  border-bottom: 0;
}

.bar {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--surface-muted);
}

.bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--primary);
}

.process-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.process-list li {
  display: grid;
  grid-template-columns: 34px 1fr;
  gap: 10px;
  align-items: start;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.step-no {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  background: var(--primary);
  font-weight: 800;
}

@media (max-width: 980px) {
  .home-hero,
  .split-section {
    grid-template-columns: 1fr;
  }

  .hero-title {
    font-size: 30px;
  }
}
</style>
