import { defineConfig } from 'vite';

const proxy = {
  '/api': {
    target: process.env.BACKEND_URL || 'http://127.0.0.1:8080',
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/api/, ''),
  },
};

export default defineConfig({
  server: { host: '127.0.0.1', port: 5173, strictPort: true, proxy },
  preview: { host: '127.0.0.1', port: 4173, strictPort: true, proxy },
});
