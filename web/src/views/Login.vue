<template>
  <div class="login-page">
    <div class="login-card">
      <h2>家长查看端</h2>
      <p class="subtitle">请输入管理密钥</p>
      <form @submit.prevent="onSubmit">
        <input
          v-model="key"
          type="password"
          placeholder="ADMIN_KEY"
          autofocus
        />
        <button type="submit">进入</button>
      </form>
      <p v-if="error" class="error">{{ error }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { setAdminKey, apiFetch } from '../lib/api.js';

const router = useRouter();
const key = ref('');
const error = ref('');

async function onSubmit() {
  error.value = '';
  setAdminKey(key.value);
  try {
    await apiFetch('/api/admin/devices');
    router.push('/devices');
  } catch (e) {
    error.value = '密钥无效，请重试';
    setAdminKey('');
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
}
.login-card {
  background: #fff;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  width: 320px;
  text-align: center;
}
.login-card h2 { margin: 0 0 0.25rem; }
.subtitle { color: #888; margin-bottom: 1.5rem; }
.login-card input {
  width: 100%;
  padding: 0.7rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  box-sizing: border-box;
  margin-bottom: 1rem;
}
.login-card button {
  width: 100%;
  padding: 0.7rem;
  background: #4f8cff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
}
.login-card button:hover { background: #3b6fdb; }
.error { color: #e74c3c; margin-top: 0.75rem; }
</style>
