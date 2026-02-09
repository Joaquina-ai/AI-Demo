<template>
  <div class="page">
    <h1>设备列表</h1>
    <p v-if="loading" class="hint">加载中...</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <div v-else-if="devices.length === 0" class="empty">
      暂无设备。请先在孩子手机上安装并注册 App。
    </div>
    <div v-else class="device-grid">
      <div v-for="d in devices" :key="d.device_id" class="device-card">
        <div class="device-name">{{ d.nickname || d.device_id }}</div>
        <div class="device-meta">
          最近上报: {{ d.last_seen_at ? timeAgo(d.last_seen_at) : '从未' }}
        </div>
        <div class="device-actions">
          <router-link :to="`/devices/${d.device_id}/today`" class="btn">今日用时</router-link>
          <router-link :to="`/devices/${d.device_id}/weekly`" class="btn btn-outline">近7天</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { apiFetch } from '../lib/api.js';

const devices = ref([]);
const loading = ref(true);
const error = ref('');

function timeAgo(iso) {
  const diff = Date.now() - new Date(iso).getTime();
  const min = Math.floor(diff / 60000);
  if (min < 1) return '刚刚';
  if (min < 60) return `${min} 分钟前`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr} 小时前`;
  return `${Math.floor(hr / 24)} 天前`;
}

onMounted(async () => {
  try {
    const data = await apiFetch('/api/admin/devices');
    devices.value = data.devices;
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.page { max-width: 600px; margin: 0 auto; padding: 1.5rem; }
h1 { font-size: 1.5rem; margin-bottom: 1rem; }
.hint { color: #888; }
.error { color: #e74c3c; }
.empty { color: #999; text-align: center; padding: 3rem 1rem; }
.device-grid { display: flex; flex-direction: column; gap: 1rem; }
.device-card {
  background: #fff;
  border-radius: 12px;
  padding: 1.25rem;
  box-shadow: 0 1px 6px rgba(0,0,0,0.08);
}
.device-name { font-size: 1.1rem; font-weight: 600; }
.device-meta { color: #888; font-size: 0.85rem; margin: 0.4rem 0 0.8rem; }
.device-actions { display: flex; gap: 0.5rem; }
.btn {
  display: inline-block;
  padding: 0.45rem 1rem;
  background: #4f8cff;
  color: #fff;
  border-radius: 8px;
  text-decoration: none;
  font-size: 0.9rem;
}
.btn:hover { background: #3b6fdb; }
.btn-outline {
  background: transparent;
  color: #4f8cff;
  border: 1px solid #4f8cff;
}
.btn-outline:hover { background: #f0f5ff; }
</style>
