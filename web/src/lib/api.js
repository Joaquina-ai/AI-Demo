const BASE = import.meta.env.VITE_API_BASE || '';

function getAdminKey() {
  return localStorage.getItem('admin_key') || '';
}

function setAdminKey(key) {
  localStorage.setItem('admin_key', key);
}

async function apiFetch(path) {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'X-ADMIN-KEY': getAdminKey() },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error || `HTTP ${res.status}`);
  }
  return res.json();
}

export { getAdminKey, setAdminKey, apiFetch };
