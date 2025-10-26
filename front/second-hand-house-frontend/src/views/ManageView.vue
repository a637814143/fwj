<template>
  <div class="manage-grid">
    <HouseForm
      :initial-house="initialHouse"
      :loading="loading"
      :can-manage="canManage"
      :current-user="currentUser"
      @submit="$emit('submit', $event)"
      @cancel="$emit('cancel')"
    />
    <HouseList
      :houses="houses"
      :loading="loading"
      :can-manage="canManage"
      :current-user="currentUser"
      :can-view-sensitive-info="canViewSensitiveInfo"
      :orders-loading="ordersLoading"
      @edit="$emit('edit', $event)"
      @remove="$emit('remove', $event)"
      @review="$emit('review', $event)"
      @purchase="$emit('purchase', $event)"
      @contact-seller="$emit('contact-seller', $event)"
    />
  </div>
</template>

<script setup>
import HouseForm from '../components/HouseForm.vue';
import HouseList from '../components/HouseList.vue';

const props = defineProps({
  initialHouse: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  canManage: { type: Boolean, default: false },
  currentUser: { type: Object, default: null },
  houses: { type: Array, default: () => [] },
  canViewSensitiveInfo: { type: Boolean, default: false },
  ordersLoading: { type: Boolean, default: false }
});

defineEmits(['submit', 'cancel', 'edit', 'remove', 'review', 'purchase', 'contact-seller']);
</script>
