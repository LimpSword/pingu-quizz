import {useAuthStore} from "@/stores/auth.js";

const apiUrl = 'http://localhost:8080/api';

export const fetcher = async (url, options = {}) => {
  const authStore = useAuthStore();
  const token = authStore.token;

  const response = await fetch(`${apiUrl}${url}`, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error(response.statusText);
  }

  return response.json();
}

export const postFormData = (url, data) => fetcher(url, {
  method: 'POST',
  body: data,
});
