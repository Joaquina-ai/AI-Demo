<template>
  <div class="page">
    <router-link to="/devices" class="back">&larr; 返回设备列表</router-link>
    <h1>今日使用明细</h1>
    <p class="date-label">{{ date }}</p>

    <p v-if="loading" class="hint">加载中...</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <div v-else-if="apps.length === 0" class="empty">今日暂无使用记录</div>
    <div v-else>
      <div class="total">今日总计: <strong>{{ formatMs(totalMs) }}</strong></div>
      <table class="usage-table">
        <thead>
          <tr><th>应用</th><th>包名</th><th>使用时长</th></tr>
        </thead>
        <tbody>
          <tr v-for="app in apps" :key="app.package">
            <td class="app-label">{{ app.app_label || app.package }}</td>
            <td class="pkg">{{ app.package }}</td>
            <td class="duration">{{ formatMs(app.used_ms) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { apiFetch } from '../lib/api.js';
import { formatMs } from '../lib/format.js';

const props = defineProps({ id: String });

const apps = ref([]);
const date = ref('');
const loading = ref(true);
const error = ref('');

const totalMs = computed(() => apps.value.reduce((s, a) => s + a.used_ms, 0));

onMounted(async () => {
  try {
    const data = await apiFetch(`/api/admin/devices/${props.id}/today`);
    date.value = data.date;
    apps.value = data.apps;
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.page { max-width: 640px; margin: 0 auto; padding: 1.5rem; }
.back { color: #4f8cff; text-decoration: none; font-size: 0.9rem; }
h1 { font-size: 1.4rem; margin: 0.5rem 0 0.25rem; }
.date-label { color: #888; margin-bottom: 1rem; }
.hint { color: #888; }
.error { color: #e74c3c; }
.empty { color: #999; text-align: center; padding: 2rem; }
.total { margin-bottom: 1rem; font-size: 1.05rem; }
.usage-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 6px rgba(0,0,0,0.06);
}
.usage-table th, .usage-table td {
  padding: 0.6rem 0.8rem;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}
.usage-table th { background: #fafafa; font-weight: 600; font-size: 0.85rem; color: #666; }
.app-label { font-weight: 500; }
.pkg { color: #aaa; font-size: 0.8rem; }
.duration { font-weight: 600; color: #4f8cff; white-space: nowrap; }
</style>
