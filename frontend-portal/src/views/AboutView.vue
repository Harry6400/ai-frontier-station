<script setup>
import PortalTopbar from '../components/PortalTopbar.vue'

const foundations = [
  {
    title: '前后台双端分离',
    description: '前台负责内容门户与品牌形象，后台负责内容录入、编辑、发布和结构维护。'
  },
  {
    title: '统一内容中心',
    description: '资讯、论文、项目、公司动态、技术实践统一进入内容表，便于长期扩展。'
  },
  {
    title: '来源与标签抽象',
    description: '来源表和标签表提前独立，为后续采集、推荐、专题和搜索留出空间。'
  }
]

const roadmap = [
  '接入 huashu-design 主导的高保真门户设计与组件规范',
  '补强前台筛选、阅读体验和内容详情的可视化表达',
  '增加 GitHub / 论文 / 公司动态的外部采集与同步能力',
  '继续扩展专题、热度排序、收藏、周报等产品层能力'
]

const designNotes = [
  {
    question: '为什么这不是普通内容收藏夹？',
    answer: '因为它的核心业务是 AI 内容的组织、精选、发布和展示，而不是单纯的信息录入。'
  },
  {
    question: '为什么要拆出标签和来源？',
    answer: '这是为了支持筛选、统计、推荐和后续外部内容同步，而不是把内容写死在一张表里。'
  },
  {
    question: '为什么前后台拆成两个工程？',
    answer: '前台和后台的用户目标、视觉语言和后续迭代方向不同，拆分后更利于长期维护。'
  }
]
</script>

<template>
  <div class="portal-shell">
    <PortalTopbar context-label="Project Scope" context-value="产品定位与架构说明" />

    <main class="page-stack">
      <section class="section-shell about-hero">
        <span class="eyebrow-line">Project Positioning</span>
        <h1>这个系统的目标，不是做一个普通后台，而是为未来的 AI 信息聚合平台搭出前台与中台的基础结构。</h1>
        <p class="lead-copy">
          现在它已经是一套可运行、可持续维护的前后端分离系统；以后它还可以继续接入 GitHub 热门项目、
          论文摘要、公司动态和技术实践内容源。
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
            <span class="section-kicker">Design Notes</span>
            <h2>为什么这样设计</h2>
          </div>
          <p>这一部分用“问题 + 回答方向”的形式，帮助你更快理解系统为什么这样组织。</p>
        </div>

        <div class="answer-grid">
          <article v-for="item in designNotes" :key="item.question" class="answer-card">
            <h3>{{ item.question }}</h3>
            <p>{{ item.answer }}</p>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>
