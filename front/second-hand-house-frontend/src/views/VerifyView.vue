<template>
  <RealNameVerification
    :api-base-url="apiBaseUrl"
    :current-user="currentUser"
    @verified="$emit('verified', $event)"
  />
</template>

<script setup>
import RealNameVerification from '../components/RealNameVerification.vue';

const props = defineProps({
  apiBaseUrl: { type: String, default: '' },
  currentUser: { type: Object, default: null }
});

defineEmits(['verified']);
</script>
