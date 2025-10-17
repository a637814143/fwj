<template>
  <form class="search-bar" @submit.prevent="submit">
    <div class="field">
      <label for="keyword">关键词</label>
      <input
        id="keyword"
        v-model.trim="keyword"
        type="text"
        placeholder="输入地点、描述或标签，支持多个关键词"
        :disabled="loading"
      />
    </div>
    <div class="actions">
      <button type="submit" :disabled="loading">{{ loading ? '搜索中...' : '搜索' }}</button>
      <button type="button" class="secondary" :disabled="loading" @click="reset">重置</button>
    </div>
  </form>
</template>

<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  initialKeyword: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['search']);

const keyword = ref(props.initialKeyword ?? '');

watch(
  () => props.initialKeyword,
  (value) => {
    keyword.value = value ?? '';
  }
);

const submit = () => {
  emit('search', {
    keyword: keyword.value
  });
};

const reset = () => {
  keyword.value = '';
  emit('search', { keyword: '' });
};
</script>

<style scoped>
.search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: flex-end;
  background: #fff;
  border-radius: 1rem;
  padding: 1rem 1.25rem;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.field {
  flex: 1;
  min-width: 220px;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

label {
  font-weight: 600;
  color: #1f2937;
}

input {
  padding: 0.65rem 0.75rem;
  border-radius: 0.7rem;
  border: 1px solid #cbd5f5;
  background: #f8fafc;
}

input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2);
}

.actions {
  display: flex;
  gap: 0.75rem;
}

button {
  padding: 0.65rem 1.4rem;
  border-radius: 0.75rem;
  border: none;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

button.secondary {
  background: #e2e8f0;
  color: #1f2937;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
