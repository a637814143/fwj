<template>
  <div class="orders-grid">
    <UrgentTasks
      v-if="showUrgentTasks"
      :tasks="tasks"
      :progress-labels="progressLabels"
      @mark-read="$emit('mark-read', $event)"
    />
    <WalletPanel
      :wallet="wallet"
      :loading="walletLoading"
      :current-user="currentUser"
      @top-up="$emit('top-up', $event)"
    />
    <OrderHistory
      :orders="orders"
      :loading="ordersLoading"
      :current-user="currentUser"
      :can-view-sensitive-info="canViewSensitiveInfo"
      :progress-labels="progressLabels"
      :progress-order="progressOrder"
      @request-return="$emit('request-return', $event)"
      @schedule-viewing="$emit('schedule-viewing', $event)"
      @advance-progress="$emit('advance-progress', $event)"
      @confirm-viewing="$emit('confirm-viewing', $event)"
    />
  </div>
</template>

<script setup>
import OrderHistory from '../components/OrderHistory.vue';
import UrgentTasks from '../components/UrgentTasks.vue';
import WalletPanel from '../components/WalletPanel.vue';

const props = defineProps({
  showUrgentTasks: { type: Boolean, default: false },
  tasks: { type: Array, default: () => [] },
  progressLabels: { type: Object, default: () => ({}) },
  progressOrder: { type: Array, default: () => [] },
  wallet: { type: Object, default: null },
  walletLoading: { type: Boolean, default: false },
  currentUser: { type: Object, default: null },
  orders: { type: Array, default: () => [] },
  ordersLoading: { type: Boolean, default: false },
  canViewSensitiveInfo: { type: Boolean, default: false }
});

defineEmits([
  'mark-read',
  'top-up',
  'request-return',
  'schedule-viewing',
  'advance-progress',
  'confirm-viewing'
]);
</script>
