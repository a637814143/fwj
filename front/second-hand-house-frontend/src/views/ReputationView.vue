<template>
  <div class="reputation-panel">
    <AdminReputationBoard
      :loading="loading"
      :overview="overview"
      :users="users"
      :current-user="currentUser"
      @refresh="$emit('refresh')"
      @toggle-blacklist="$emit('toggle-blacklist', $event)"
      @delete-user="$emit('delete-user', $event)"
    />
  </div>
</template>

<script setup>
import AdminReputationBoard from '../components/AdminReputationBoard.vue';

const props = defineProps({
  loading: { type: Boolean, default: false },
  overview: { type: Object, default: null },
  users: { type: Array, default: () => [] },
  currentUser: { type: Object, default: null }
});

defineEmits(['refresh', 'toggle-blacklist', 'delete-user']);
</script>
