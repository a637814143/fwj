<template>
  <HouseExplorer
    :houses="houses"
    :loading="loading"
    :current-user="currentUser"
    :can-view-sensitive-info="canViewSensitiveInfo"
    :filters="filters"
    :recommendations="recommendations"
    :purchase-loading="purchaseLoading"
    :reservation-loading="reservationLoading"
    :reservation-target="reservationTarget"
    :api-base-url="apiBaseUrl"
    @search="$emit('search', $event)"
    @reserve="$emit('reserve', $event)"
    @purchase="$emit('purchase', $event)"
    @contact-seller="$emit('contact-seller', $event)"
  />
</template>

<script setup>
import HouseExplorer from '../components/HouseExplorer.vue';

const props = defineProps({
  houses: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  currentUser: { type: Object, default: null },
  canViewSensitiveInfo: { type: Boolean, default: false },
  filters: { type: Object, default: () => ({}) },
  recommendations: { type: Object, default: () => ({ sellers: [], buyers: [] }) },
  purchaseLoading: { type: Boolean, default: false },
  reservationLoading: { type: Boolean, default: false },
  reservationTarget: { type: Object, default: null },
  apiBaseUrl: { type: String, default: '' }
});

defineEmits(['search', 'reserve', 'purchase', 'contact-seller']);
</script>
