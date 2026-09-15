import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // The backend origin is fixed in development; production serves both from one origin,
    // which is why the API base URL is an environment variable rather than a hard-coded host.
    strictPort: true,
  },
});
