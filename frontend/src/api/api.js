import {useAuthStore} from "@/stores/auth.js";

const apiUrl = import.meta.env.VITE_API_URL;

export const apiUrls = {
  download: `${apiUrl}/storage/files`,
  auth: `${apiUrl}/auth/login`,
  websocket: `${apiUrl}/quiz`,
}

export const fetcher = async (url, options = {}) => {
  const authStore = useAuthStore();
  const token = authStore.token;

  return await fetch(`${apiUrl}${url}`, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${token}`,
    },
  });
}

export const postFormData = (url, data) => fetcher(url, {
  method: 'POST',
  body: data,
});
