<template>
  <section class="history">
    <div class="history__header">
      <h3>浏览历史</h3>
      <button type="button" class="link" :disabled="!history.length" @click="emit('clear')">
        清空
      </button>
    </div>
    <p v-if="!history.length" class="empty">暂无浏览记录。</p>
    <ul v-else class="history__list">
      <li v-for="item in history" :key="`${item.id}-${item.viewedAt}`" class="history__item">
        <div class="info">
          <button type="button" class="title" @click="emit('select', item.id)">
            {{ item.title }}
          </button>
          <p class="meta">
            <span>{{ formatDateTime(item.viewedAt) }}</span>
            <span v-if="item.address">{{ item.address }}</span>
          </p>
          <p v-if="item.keywords?.length" class="tags">
            <span v-for="keyword in item.keywords" :key="keyword" class="tag">{{ keyword }}</span>
          </p>
        </div>
        <div class="price" v-if="item.price != null">
          ￥{{ formatNumber(item.price) }}
        </div>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue';

const props = defineProps({
  history: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['select', 'clear']);

const formatDateTime = (value) => {
  if (!value) {
    return '';
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
};

const formatNumber = (value) => {
  if (value == null || value === '') {
    return '0.00';
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};
</script>

<style scoped>
.history {
  background: var(--surface-secondary);
  border-radius: 22px;
  border: 1px solid var(--surface-border);
  box-shadow: var(--shadow-strong);
  padding: 1.45rem;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
  color: var(--text-primary);
}

.history__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.history__header h3 {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 600;
}

.link {
  border: none;
  background: none;
  color: var(--accent);
  cursor: pointer;
  font-weight: 600;
  padding: 0.25rem 0.5rem;
  border-radius: 0.75rem;
  transition: background 0.2s ease;
}

.link:hover:not(:disabled) {
  background: rgba(96, 165, 250, 0.2);
}

.link:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.empty {
  margin: 0;
  color: var(--text-secondary);
}

.history__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.history__item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  border-bottom: 1px dashed rgba(148, 163, 184, 0.35);
  padding-bottom: 0.75rem;
}

.history__item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.title {
  border: none;
  background: none;
  color: var(--accent);
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.title:hover {
  text-decoration: underline;
}

.meta {
  margin: 0.25rem 0 0;
  display: flex;
  gap: 0.75rem;
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.tags {
  margin: 0.35rem 0 0;
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.tag {
  background: rgba(59, 130, 246, 0.25);
  color: var(--text-primary);
  border-radius: 999px;
  padding: 0.2rem 0.6rem;
  font-size: 0.75rem;
}

.price {
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
}
</style>
