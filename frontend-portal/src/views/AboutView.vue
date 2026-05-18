<script setup>
import PortalTopbar from '../components/PortalTopbar.vue'

const foundations = [
  {
    title: '前后台双端分离',
    description: '前台负责内容门户与品牌形象，后台负责内容录入、编辑、发布和结构维护。Vue 3 + Spring Boot + MySQL 技术栈。'
  },
  {
    title: '统一内容中心',
    description: '资讯、论文、项目、公司动态、技术实践统一进入内容表，支持 Markdown 渲染和安全清洗。'
  },
  {
    title: '多数据源集成',
    description: '已接入 GitHub 仓库导入、arXiv 论文搜索、HuggingFace 每日热门论文，支持一键导入为平台内容。'
  },
  {
    title: 'AI 辅助内容整理',
    description: '集成阿里百炼和 MiMo AI 能力，支持自动生成导读、推荐理由、标签建议和重要性评分。'
  },
  {
    title: '安全加固',
    description: 'API Key 采用 AES-256-GCM 加密存储，管理员登录采用 Spring Security + JWT 认证。'
  }
]

const roadmap = [
  'GitHub 项目热度同步（定期刷新 star/fork/语言等字段）',
  'RSS 订阅源接入（OpenAI Blog、Google AI Blog 等技术博客）',
  '定时任务与自动化采集（论文、项目、动态的定期同步）',
  '部署上线（Docker 容器化 + CI/CD + 云平台部署）'
]

const architectureAnswers = [
  {
    question: '为什么这不是普通管理系统？',
    answer: '因为它的核心业务是 AI 内容的组织、精选、发布和展示，而不是单纯的信息录入。系统围绕 AI 内容源、外部引用、标签体系、AI 导读、GitHub API 和百炼 API 能力开关来构建。'
  },
  {
    question: '为什么数据库要拆出标签和来源？',
    answer: '这是为了支持筛选、统计、推荐和后续外部内容同步，而不是把内容写死在一张表里。来源表支持 GitHub、arXiv、HuggingFace 等多种数据源类型。'
  },
  {
    question: '为什么前后台拆成两个工程？',
    answer: '前台和后台的用户目标、视觉语言和后续迭代方向不同，拆分后更利于长期维护。前台注重品牌和阅读体验，后台注重运营效率。'
  },
  {
    question: 'API Key 如何保证安全？',
    answer: 'API Key 采用 AES-256-GCM 加密后存储到数据库，主密钥通过环境变量配置。前端填写密钥后，后端验证并加密存储，重启后自动加载，无需重复输入。'
  }
]
</script>

<template>
  <div class="portal-shell">
    <PortalTopbar context-label="Project Scope" context-value="平台说明与技术架构" />

    <main class="page-stack">
      <section class="section-shell about-hero">
        <span class="eyebrow-line">Project Positioning</span>
        <h1>AI 前沿情报站：面向 AI 开发者的信息聚合与精选平台</h1>
        <p class="lead-copy">
          这是一个完整的前后端分离 Web 系统，已实现内容管理、多数据源集成、AI 辅助整理和安全认证。
          这是一个围绕 AI 内容源构建的信息聚合与精选平台，聚焦论文、项目与技术动态的统一管理。
        </p>
      </section>

      <section class="system-grid">
        <section class="section-shell">
          <div class="section-head">
            <div>
              <span class="section-kicker">Current Foundation</span>
              <h2>当前主干能力</h2>
            </div>
          </div>

          <div class="architecture-list">
            <article v-for="item in foundations" :key="item.title" class="architecture-item">
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
            </article>
          </div>
        </section>

        <section class="section-shell">
          <div class="section-head">
            <div>
              <span class="section-kicker">Roadmap</span>
              <h2>后续路线</h2>
            </div>
          </div>

          <div class="roadmap-list">
            <article v-for="(item, index) in roadmap" :key="item" class="roadmap-item">
              <span>{{ String(index + 1).padStart(2, '0') }}</span>
              <p>{{ item }}</p>
            </article>
          </div>
        </section>
      </section>

      <section class="section-shell">
        <div class="section-head">
          <div>
            <span class="section-kicker">Architecture Q&A</span>
            <h2>架构设计要点</h2>
          </div>
          <p>以下以"问题 + 回答方向"的形式，帮助理解系统的核心架构决策。</p>
        </div>

        <div class="answer-grid">
          <article v-for="item in architectureAnswers" :key="item.question" class="answer-card">
            <h3>{{ item.question }}</h3>
            <p>{{ item.answer }}</p>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>
