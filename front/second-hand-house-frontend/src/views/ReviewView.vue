<template>
  <AdminHouseReview
    :houses="houses"
    :loading="loading"
    @refresh="$emit('refresh')"
    @review="$emit('review', $event)"
  />
</template>

<script setup>
import AdminHouseReview from '../components/AdminHouseReview.vue';

const props = defineProps({
  houses: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
});

defineEmits(['refresh', 'review']);
</script>
