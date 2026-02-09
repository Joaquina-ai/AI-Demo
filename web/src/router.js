import { createRouter, createWebHistory } from 'vue-router';
import DeviceList from './views/DeviceList.vue';
import DeviceToday from './views/DeviceToday.vue';
import DeviceWeekly from './views/DeviceWeekly.vue';
import Login from './views/Login.vue';
import { getAdminKey } from './lib/api.js';

const routes = [
  { path: '/', redirect: '/devices' },
  { path: '/login', component: Login, meta: { public: true } },
  { path: '/devices', component: DeviceList },
  { path: '/devices/:id/today', component: DeviceToday, props: true },
  { path: '/devices/:id/weekly', component: DeviceWeekly, props: true },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  if (!to.meta.public && !getAdminKey()) {
    return '/login';
  }
});

export default router;
