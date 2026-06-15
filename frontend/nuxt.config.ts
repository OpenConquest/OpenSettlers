import tailwindcss from "@tailwindcss/vite";

export default defineNuxtConfig({
  compatibilityDate: "2025-07-15",
  devtools: { enabled: true },
  css: ["./app/assets/css/main.css", "vue-sonner/style.css"],
  modules: ["shadcn-nuxt"],

  /**
   * Auto-import components by their file name (no directory prefix), so
   * `components/game/GameCanvas.vue` is used as `<GameCanvas>` and the shadcn
   * UI primitives keep their bare names.
   */
  components: [{ path: "~/components", pathPrefix: false }],

  /**
   * Public runtime configuration, overridable through environment variables:
   *   NUXT_PUBLIC_API_BASE — REST base URL of the Quarkus backend
   *   NUXT_PUBLIC_WS_BASE  — WebSocket base URL of the Quarkus backend
   */
  runtimeConfig: {
    public: {
      apiBase: "http://localhost:8080",
      wsBase: "ws://localhost:8080",
    },
  },

  shadcn: {
    /**
     * Prefix for all the imported component
     */
    prefix: "",
    /**
     * Directory that the component lives in.
     * @default "@/components/ui"
     */
    componentDir: "@/components/ui",
  },
  vite: {
    plugins: [tailwindcss()],
  },
});
