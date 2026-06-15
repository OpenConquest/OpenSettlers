<script setup lang="ts">
/** Application shell: hosts the router outlet and the global toast layer. */
</script>

<template>
  <div class="min-h-screen bg-background text-foreground antialiased">
    <NuxtPage />
    <Toaster position="bottom-center" rich-colors />
  </div>
</template>
