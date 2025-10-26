<template>
  <div class="admin-panels">
    <AdminOrderReview
      :orders="orders"
      :loading="loading"
      @refresh="$emit('refresh')"
      @release="$emit('release', $event)"
    />
  </div>
</template>

<script setup>
import AdminOrderReview from '../components/AdminOrderReview.vue';

const props = defineProps({
  orders: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
});

defineEmits(['refresh', 'release']);
</script>
