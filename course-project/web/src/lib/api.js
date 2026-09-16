const API_BASE_URL = import.meta.env.VITE_API_BASE_URL
  || (window.location.protocol === 'file:' ? 'http://localhost:4000/api' : '/api');

export async function apiRequest(path, options = {}) {
  const token = localStorage.getItem('feynman_token');
  const isFormData = options.body instanceof FormData;
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {})
    }
  });

  if (!response.ok) {
    const payload = await response.json().catch(() => ({}));
    throw new Error(payload.message || '请求失败，请稍后重试');
  }

  return response.status === 204 ? null : response.json();
}
