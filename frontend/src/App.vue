<template>
  <div id="app-container">
    <header class="app-header">
      <div class="header-content">
        <div class="logo-area">
          <el-icon :size="32" color="#fff"><Watermelon /></el-icon>
          <div class="title-group">
            <h1 class="main-title">园区公共饮水机管理系统</h1>
            <p class="sub-title">设备档案 · 水质抽检与复检闭环管理平台</p>
          </div>
        </div>
        <div class="header-actions">
          <el-radio-group v-model="activeMenu" size="small" @change="onMenuChange">
            <el-radio-button label="/device">设备档案</el-radio-button>
            <el-radio-button label="/inspection">水质抽检</el-radio-button>
            <el-radio-button label="/threshold">阈值管理</el-radio-button>
          </el-radio-group>
          <el-select
            v-model="user"
            size="small"
            style="width: 132px; margin-left: 12px;"
            @change="onUserChange"
          >
            <el-option label="采样人 张三" value="张三" />
            <el-option label="采样人 李四" value="李四" />
            <el-option label="复核人 王五" value="王五" />
            <el-option label="复核人 赵六" value="赵六" />
            <el-option label="admin" value="admin" />
          </el-select>
        </div>
      </div>
    </header>
    <main class="app-main">
      <router-view />
    </main>
    <footer class="app-footer">
      <span>© 2024 园区后勤管理系统 · 公共饮水机水质抽检与复检平台</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { currentUser, setCurrentUser } from '@/utils/user'

const route = useRoute()
const router = useRouter()
const user = ref(currentUser.value)

const menuOf = (path) => {
  if (path.startsWith('/inspection')) return '/inspection'
  if (path.startsWith('/threshold')) return '/threshold'
  return '/device'
}
const activeMenu = ref(menuOf(route.path))

watch(() => route.path, (p) => { activeMenu.value = menuOf(p) })

const onMenuChange = (val) => {
  router.push(val)
}

const onUserChange = (val) => {
  setCurrentUser(val)
}
</script>

<style lang="scss" scoped>
#app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
}

.app-header {
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 50%, #06b6d4 100%);
  color: white;
  padding: 0;
  box-shadow: 0 4px 20px rgba(30, 64, 175, 0.3);

  .header-content {
    max-width: 1600px;
    margin: 0 auto;
    padding: 14px 32px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
  }

  .logo-area {
    display: flex;
    align-items: center;
    gap: 16px;

    .title-group {
      .main-title {
        margin: 0;
        font-size: 20px;
        font-weight: 700;
        letter-spacing: 1px;
      }
      .sub-title {
        margin: 2px 0 0 0;
        font-size: 12px;
        opacity: 0.85;
        font-weight: 300;
      }
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    flex-shrink: 0;

    :deep(.el-radio-button__inner) {
      background: rgba(255, 255, 255, 0.15);
      color: #fff;
      border-color: rgba(255, 255, 255, 0.3);
    }
    :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
      background: #fff;
      color: #1e40af;
      box-shadow: -1px 0 0 0 #fff;
    }
  }
}

.app-main {
  flex: 1;
  max-width: 1600px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 32px;
  box-sizing: border-box;
}

.app-footer {
  background: #1e293b;
  color: #94a3b8;
  text-align: center;
  padding: 16px;
  font-size: 13px;
}
</style>
