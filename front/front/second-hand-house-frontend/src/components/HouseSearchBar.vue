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
  background: var(--app-surface);
  border-radius: 1rem;
  padding: 1rem 1.25rem;
  box-shadow: var(--app-shadow);
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
  color: var(--app-text);
}

input {
  padding: 0.65rem 0.75rem;
  border-radius: 0.7rem;
  border: 1px solid var(--app-border);
  background: var(--app-surface-alt);
  color: var(--app-text);
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
}

input:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.actions {
  display: flex;
  gap: 0.75rem;
}

button {
  padding: 0.65rem 1.4rem;
  border-radius: 0.75rem;
  border: none;
  background: var(--accent-gradient);
  color: var(--accent-contrast);
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, opacity 0.25s ease;
}

button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 18px rgba(15, 23, 42, 0.18);
}

button.secondary {
  background: var(--app-surface-alt);
  color: var(--app-text);
  border: 1px solid var(--app-border);
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}
</style>
