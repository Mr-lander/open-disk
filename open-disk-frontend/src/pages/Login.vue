<template>
  <div class="quark-container">
    <!-- ==== Header ==== -->
    <header class="header">
      <h1 class="logo-gradient">云端文件共享系统</h1>
<!--      <p class="subtitle">基于Spring Boot和Vue.js的开源云端文件共享系统</p>-->
      <!-- 波浪分割线 -->
      <!-- Header 下的波浪，增加渐变并微调位移 -->
      <svg class="wave" viewBox="0 0 1440 120" preserveAspectRatio="none">
        <defs>
          <linearGradient id="waveGrad" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%"  stop-color="white" stop-opacity="0.9"/>
            <stop offset="100%" stop-color="white" stop-opacity="0"/>
          </linearGradient>
        </defs>
        <path d="M0,40 C300,80 600,0 900,40 C1200,80 1440,20 1440,20 L1440,120 L0,120 Z"
              fill="url(#waveGrad)"/>
      </svg>

    </header>

    <!-- ==== 内容区域 ==== -->
    <div class="main-content">
      <!-- 左侧 Feature -->
      <div class="left-column">
        <section class="features">
          <div class="feature-item" v-for="f in features" :key="f">
            <h3>{{ f }}</h3>
          </div>
        </section>
        <!-- 左列：feature + 技术栈 -->
        <!-- tech-stack -->
        <!-- tech-stack -->
        <section class="tech-stack">
          <div
              class="tech-card"
              v-for="(t, idx) in techs"
              :key="t.name"
              :ref="el => techRefs[idx] = el">
          <img :src="t.logo" :alt="t.name" />
          <span>{{ t.name }}</span>
          </div>
        </section>


<!--        <section class="download-buttons">-->
<!--          <button class="download-btn" v-for="b in btns" :key="b">-->
<!--            {{ b }}-->
<!--          </button>-->
<!--        </section>-->
      </div>

      <!-- 右侧 登录 / 注册 -->
      <transition name="fade" mode="out-in">
        <div class="right-column" :key="isLogin">
          <section class="login-form glass">
            <div class="tabs">
              <span :class="{ active: isLogin }" @click="switchTab(true)">登录</span>
              <span :class="{ active: !isLogin }" @click="switchTab(false)">注册</span>
            </div>

            <h2>{{ isLogin ? '账号登录' : '注册新账号' }}</h2>

            <form @submit.prevent="isLogin ? handleLogin() : handleRegister()">
              <div class="form-group">
                <input type="text" v-model="form.name" placeholder="用户名" required />
              </div>

              <div class="form-group">
                <input
                    type="password"
                    v-model="form.password"
                    placeholder="密码"
                    required
                />
              </div>

              <div class="form-group" v-if="!isLogin">
                <input
                    type="password"
                    v-model="form.confirmPassword"
                    placeholder="确认密码"
                    required
                />
              </div>

              <button type="submit" class="login-btn">
                {{ isLogin ? '登录' : '注册' }}
              </button>
            </form>

            <p class="login-agreement">
              {{ isLogin ? '登录' : '注册' }} 即表示您已阅读并同意
              <span>服务及隐私协议</span>
            </p>
          </section>
        </div>
      </transition>
    </div>

    <!-- ==== 优势区块 ==== -->
    <section class="advantages-section">
      <div
          class="adv-item"
          v-for="(adv, i) in advantages"
          :key="i"
          ref="advRefs"
      >
        <span class="adv-icon" v-html="adv.icon"></span>
        <div class="adv-text">
          <h3>{{ adv.title }}</h3>
          <p>{{ adv.desc }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import userApi from '@/api/user.js'
import { useAuthStore } from '@/store/auth.js'

const router = useRouter()
const authStore = useAuthStore()

const isLogin = ref(true)
const form = ref({ name: '', password: '', confirmPassword: '' })
const techRefs = ref([])   // 和 advRefs 一样记得 import { ref }…

const techs = [
  { name: 'Vue 3',           logo: '/logo/Vue.svg' },
  { name: 'Spring Boot 3',   logo: '/logo/spring.svg' },
  { name: 'MinIO',           logo: '/logo/minio.svg' },
  { name: 'Ceph RGW',        logo: '/logo/Ceph.svg' },
  { name: 'Elasticsearch',   logo: '/logo/es.svg' },
  { name: 'Vault',           logo: '/logo/vault.svg' },
  { name: 'Docker Compose',  logo: '/logo/Docker.svg' }
]

/* --- 静态数据 --- */
const features = [
  '基于Spring Boot和Vue.js开发',
  'Vault 统一密钥存储的加密结构',
  'PDF，视频在线预览'
]
// const btns = ['下载客户端', '下载手机端', '下载 TV 端']
const advantages = [
  {
    icon: '👤',
    title: '适合个人用户部署',
    desc: '简单易用，无需繁琐配置，即刻投入使用，数据私密无忧。'
  },
  {
    icon: '💻',
    title: '100% 开源项目',
    desc: '基于开源技术搭建，代码可审计，可二次开发，放心托管。'
  },
  {
    icon: '⚙️',
    title: '低成本一键部署',
    desc: 'Docker-compose 支持，快速启动依赖容器。'
  }
]

/* --- tab 切换 --- */
function switchTab(login) {
  isLogin.value = login
  form.value = { name: '', password: '', confirmPassword: '' }
}

/* --- 登录 / 注册 --- */
async function handleLogin() {
  try {
    const { data } = await userApi.login({
      username: form.value.name,
      password: form.value.password
    })
    authStore.setToken(data.token)
    ElMessage.success('登录成功')
    router.push('/home')
  } catch (err) {
    ElMessage.error(err.response?.data?.error || '登录失败')
  }
}

async function handleRegister() {
  if (form.value.password !== form.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  try {
    await userApi.register({
      name: form.value.name,
      password: form.value.password
    })
    ElMessage.success('注册成功，请登录')
    switchTab(true)
  } catch (err) {
    ElMessage.error(err.response?.data?.error || '注册失败')
  }
}

/* --- Intersection Observer：优势区块入场动画 --- */
const advRefs = ref([])
onMounted(() => {
  const io = new IntersectionObserver(
          entries => {
            entries.forEach(e => {
              if (e.isIntersecting) {
                e.target.classList.add('in-view')
                io.unobserve(e.target)
              }
            })
          },
          { threshold: 0.2 }
      )

  ;[...advRefs.value, ...techRefs.value].forEach(el => el && io.observe(el))
})

</script>

<style scoped>
/* ============ 全局背景 & 动画 ============ */
:host,
.quark-container {
  /* pastel 渐变：淡蓝 → 淡紫 → 粉白 */
  background: linear-gradient(
      120deg,
      #e7f3ff 0%,
      #f0eaff 40%,
      #fff4f8 80%,
      #ffffff 100%
  );
  background-size: 600% 600%;       /* 拉大幅度，色带更长 */
  animation: bgMove 10s ease-in-out infinite alternate;
  min-height: 100vh; /* 关键：确保容器至少和视口一样高 */
  /* display: flex; */ /* 如果需要，可以用来更好地管理子元素布局 */
  /* flex-direction: column; */
}


@keyframes bgMove {
  0%   { background-position: 0% 50%; }
  100% { background-position: 100% 50%; }
}


/* 波浪 */
.wave { width:100%; height:120px; margin-top:-24px; transform:translateY(10px); }

/* Header */
.header { text-align:center; padding: 60px 10px 20px; position:relative; }
.logo-gradient {
  font-size: 48px;
  font-weight: 700;
  background: linear-gradient(90deg, #42B983, #1890ff);
  -webkit-background-clip: text;
  color: transparent;
  animation: hue 6s linear infinite alternate;
}
@keyframes hue {
  from { filter: hue-rotate(0deg); }
  to   { filter: hue-rotate(360deg); }
}
.subtitle { font-size: 18px; color:#555; margin-top:8px; }

/* ===== 布局 ===== */
.main-content { max-width: 1200px; margin:40px auto; display:flex; gap:40px; padding:0 20px; }
.left-column { flex:1; display:flex; flex-direction:column; gap:30px; }
.right-column { flex:0 1 420px; }

/* Features */
.features { display:flex; flex-direction:column; gap:15px; }
.feature-item h3 {
  font-size:18px; padding:16px; border-radius:8px;
  background:rgba(255,255,255,0.6);
  backdrop-filter:blur(3px);
  transition:transform .3s;
}
.feature-item:hover h3 { transform: translateY(-4px) scale(1.02); }

/* 下载按钮 */
.download-buttons { display:flex; gap:12px; flex-wrap:wrap; }
.download-btn {
  flex:1 1 140px; padding:14px 22px; font-size:15px;
  border:none; border-radius:8px; cursor:pointer;
  background:linear-gradient(120deg,#1890ff,#42b983);
  color:#fff; box-shadow:0 4px 8px rgba(24,144,255,.3);
  transition: transform .25s cubic-bezier(.4,.2,.2,1), box-shadow .25s;
}
.download-btn:hover {
  transform:translateY(-3px);
  box-shadow:0 8px 16px rgba(24,144,255,.4);
}

/* 登录卡片（毛玻璃） */
.glass {
  background:rgba(255,255,255,.55);
  backdrop-filter: blur(10px);
  border:1px solid rgba(255,255,255,.3);
  border-radius:16px;
  box-shadow:0 8px 24px rgba(0,0,0,.08);
  padding:35px 32px;
}

/* tab */
.tabs { display:flex; border-bottom:1px solid #eaeaea; margin-bottom:24px; }
.tabs span {
  flex:1; text-align:center; padding:12px 0; cursor:pointer;
  position:relative; font-weight:500; color:#666;
}
.tabs span.active { color:#1890ff; }
.tabs span.active::after {
  content:''; position:absolute; left:50%; bottom:-1px; transform:translateX(-50%);
  width:60%; height:2px; background:#1890ff; border-radius:1px;
}

/* 表单 */
.form-group { margin-bottom:18px; }
.form-group input {
  width: 100%;
  padding: 14px 16px;
  border-radius: 6px;
  border: 1px solid #d0d7de;
  box-sizing: border-box;     /* ← 关键：把 padding 算进 width */
  transition: border-color 0.3s, box-shadow 0.3s;
}

.form-group:focus-within input {
  border-color:#1890ff;
  box-shadow:0 0 0 3px rgba(24,144,255,.2);
}
.login-btn {
  width:100%; padding:14px; margin-top:4px;
  border:none; border-radius:8px; font-size:16px; font-weight:600;
  background:#1890ff; color:#fff;
  transition:background .3s;
}
.login-btn:hover { background:#1478d6; }
.login-agreement { font-size:12px; color:#777; text-align:center; margin-top:18px; }
.login-agreement span { color:#1890ff; cursor:pointer; }

/* 右侧淡入淡出 */
.fade-enter-active, .fade-leave-active { transition:opacity .4s ease; }
.fade-enter-from, .fade-leave-to { opacity:0; }

/* 优势 */
.advantages-section {
  display:flex; gap:20px; padding:40px 20px; margin: 40px auto;
  max-width:1200px; flex-wrap:wrap;
}
.adv-item {
  flex:1 1 300px; display:flex; gap:16px; padding:24px;
  border-radius:12px; background:#fff; box-shadow:0 4px 12px rgba(0,0,0,.05);
  transform:translateY(40px); opacity:0; transition:.6s ease;
}
.adv-item.in-view { transform:none; opacity:1; }
.adv-icon { font-size:32px; line-height:48px; }

/* 响应式 */
@media (max-width: 768px) {
  .main-content { flex-direction:column; }
  .right-column { width:100%; }
}


/* ---------- 技术栈卡片 ---------- */
.tech-stack { display:flex; flex-wrap:wrap; gap:14px; }
.tech-card {
  flex:1 1 120px; max-width:160px;
  background:#fff; border-radius:10px; padding:12px 10px;
  display:flex; flex-direction:column; align-items:center; gap:8px;
  box-shadow:0 3px 10px rgba(0,0,0,.06);
  transform:translateY(30px); opacity:0; transition:.6s;
}
.tech-card img { width:40px; height:40px; }
.tech-card.in-view { transform:none; opacity:1; }

.subtitle {
  margin-top: 12px;
  font-size: 20px;            /* ↗字号 */
  font-weight: 500;           /* 半粗体 */
  color: #444;                /* 深一点 */
  letter-spacing: 0.5px;
  position: relative;
}

/* 底部加一条淡色下划线，微妙但醒目 */
.subtitle::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -8px;
  transform: translateX(-50%);
  width: 220px;              /* 约副标题 60% 宽 */
  height: 3px;
  background: linear-gradient(90deg, #42b883, #1890ff);
  border-radius: 2px;
  opacity: 0.7;
}

</style>
