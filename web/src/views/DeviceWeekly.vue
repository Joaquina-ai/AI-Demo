<template>
  <div class="page">
    <router-link to="/devices" class="back">&larr; 返回设备列表</router-link>
    <h1>近 7 天使用趋势</h1>

    <p v-if="loading" class="hint">加载中...</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <div v-else>
      <!-- Bar chart -->
      <div class="chart">
        <div
          v-for="day in days"
          :key="day.date"
          class="bar-col"
          @click="selectedDate = day.date"
        >
          <div class="bar-value">{{ formatMs(day.total_ms) }}</div>
          <div class="bar" :style="{ height: barHeight(day.total_ms) }"></div>
          <div class="bar-label">{{ shortDate(day.date) }}</div>
        </div>
      </div>

      <!-- Detail of selected day -->
      <div v-if="selectedDay" class="day-detail">
        <h3>{{ selectedDate }} 明细</h3>
        <div v-if="selectedDay.apps.length === 0" class="empty">当日无记录</div>
        <table v-else class="usage-table">
          <thead><tr><th>应用</th><th>时长</th></tr></thead>
          <tbody>
            <tr v-for="app in selectedDay.apps" :key="app.package">
              <td>{{ app.app_label || app.package }}</td>
              <td class="duration">{{ formatMs(app.used_ms) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { apiFetch } from '../lib/api.js';
import { formatMs } from '../lib/format.js';

const props = defineProps({ id: String });

const days = ref([]);
const loading = ref(true);
const error = ref('');
const selectedDate = ref('');

const maxMs = computed(() => Math.max(...days.value.map(d => d.total_ms), 1));
const selectedDay = computed(() => days.value.find(d => d.date === selectedDate.value));

function barHeight(ms) {
  return Math.max((ms / maxMs.value) * 180, 4) + 'px';
}

function shortDate(d) {
  const parts = d.split('-');
  return `${parseInt(parts[1])}/${parseInt(parts[2])}`;
}

onMounted(async () => {
  try {
    const data = await apiFetch(`/api/admin/devices/${props.id}/weekly`);
    days.value = data.days;
    if (data.days.length > 0) {
      selectedDate.value = data.days[data.days.length - 1].date;
    }
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
h1 { font-size: 1.4rem; margin: 0.5rem 0 1rem; }
.hint { color: #888; }
.error { color: #e74c3c; }
.empty { color: #999; padding: 1rem 0; }

.chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 0.5rem;
  height: 230px;
  padding: 0 0.25rem;
  margin-bottom: 1.5rem;
}
.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
}
.bar-value { font-size: 0.7rem; color: #888; margin-bottom: 4px; white-space: nowrap; }
.bar {
  width: 70%;
  max-width: 48px;
  background: #4f8cff;
  border-radius: 6px 6px 0 0;
  transition: height 0.3s;
}
.bar-label { font-size: 0.8rem; color: #666; margin-top: 6px; }

.day-detail { margin-top: 0.5rem; }
.day-detail h3 { font-size: 1.1rem; margin-bottom: 0.5rem; }
.usage-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 6px rgba(0,0,0,0.06);
}
.usage-table th, .usage-table td {
  padding: 0.55rem 0.8rem;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}
.usage-table th { background: #fafafa; font-weight: 600; font-size: 0.85rem; color: #666; }
.duration { font-weight: 600; color: #4f8cff; }
</style>
