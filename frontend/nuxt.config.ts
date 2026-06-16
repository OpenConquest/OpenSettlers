import tailwindcss from "@tailwindcss/vite";

export default defineNuxtConfig({
  compatibilityDate: "2025-07-15",
  app: {
    head: {
      link: [
        { rel: "stylesheet", href: "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0" }
      ]
    }
  },
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
    /**
     * Pre-bundle the dependencies Vite would otherwise discover lazily at
     * runtime (which forces a full page reload the first time each is hit).
     * Keep in sync with the list Vite prints in dev when it finds new deps.
     */
    optimizeDeps: {
      include: [
        "@tabler/icons-vue",
        "class-variance-authority",
        "clsx",
        "reka-ui",
        "tailwind-merge",
        "vue-sonner",
      ],
    },
  },
});
