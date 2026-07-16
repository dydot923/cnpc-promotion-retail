import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

declare const process: {
  env: Record<string, string | undefined>;
};

const BACKEND_URL = process.env.VITE_BACKEND_URL || "http://localhost:18083";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    proxy: {
      "/api": BACKEND_URL,
      "/actuator": BACKEND_URL
    }
  }
});
