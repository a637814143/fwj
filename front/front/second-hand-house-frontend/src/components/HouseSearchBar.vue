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
  background: var(--surface-secondary);
  border-radius: 20px;
  border: 1px solid var(--surface-border);
  padding: 1.1rem 1.35rem;
  backdrop-filter: blur(18px);
  box-shadow: var(--shadow-strong);
}

.field {
  flex: 1;
  min-width: 220px;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

label {
  font-weight: 600;
  color: var(--text-secondary);
}

input {
  padding: 0.7rem 0.85rem;
  border-radius: 0.85rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(15, 23, 42, 0.65);
  color: var(--text-primary);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

input:focus {
  outline: none;
  border-color: rgba(96, 165, 250, 0.8);
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.25);
}

.actions {
  display: flex;
  gap: 0.75rem;
}

button {
  padding: 0.7rem 1.45rem;
  border-radius: 0.85rem;
  border: none;
  background: linear-gradient(135deg, var(--accent), rgba(56, 189, 248, 0.85));
  color: var(--text-primary);
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 15px 28px rgba(37, 99, 235, 0.35);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 18px 32px rgba(37, 99, 235, 0.45);
}

button.secondary {
  background: rgba(148, 163, 184, 0.25);
  color: var(--text-primary);
  box-shadow: none;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}
</style>
