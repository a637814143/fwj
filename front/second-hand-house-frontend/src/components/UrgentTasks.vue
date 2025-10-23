<template>
  <section class="urgent-tasks surface-card">
    <header>
      <div>
        <h2>{{ title }}</h2>
        <p>{{ subtitle }}</p>
      </div>
      <span class="badge">{{ badgeText }}</span>
    </header>
    <p v-if="!hasTasks" class="empty">{{ emptyText }}</p>
    <ul v-else class="task-list">
      <li
        v-for="task in tasks"
        :key="task.key ?? task.id"
        :class="['task-item', { soon: task.isSoon }]"
      >
        <div class="task-head">
          <strong>{{ task.houseTitle }}</strong>
          <span class="stage">{{ stageLabel(task.stage) }}</span>
        </div>
        <p class="task-desc">{{ task.description }}</p>
        <p v-if="task.timeLabel" class="task-time">
          {{ t('orders.urgent.upcomingViewing', { time: task.timeLabel }) }}
        </p>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { computed, inject } from 'vue';

const props = defineProps({
  tasks: {
    type: Array,
    default: () => []
  },
  progressLabels: {
    type: Object,
    default: () => ({})
  }
});

const t = inject('translate', (key, vars = {}) => {
  if (typeof key !== 'string') {
    return '';
  }
  if (!Object.keys(vars).length) {
    return key;
  }
  return key.replace(/\{(\w+)\}/g, (_, name) => vars[name] ?? `{${name}}`);
});

const hasTasks = computed(() => Array.isArray(props.tasks) && props.tasks.length > 0);
const title = computed(() => t('orders.urgent.title'));
const subtitle = computed(() => t('orders.urgent.subtitle'));
const emptyText = computed(() => t('orders.urgent.empty'));
const badgeText = computed(() => t('orders.urgent.badge', { count: props.tasks.length }));

const stageLabel = (stage) => props.progressLabels?.[stage] ?? stage ?? '';
</script>

<style scoped>
.urgent-tasks {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  padding: 1.6rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
  background: var(--gradient-surface);
}

.urgent-tasks header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.urgent-tasks h2 {
  margin: 0;
  font-size: 1.25rem;
  color: var(--color-text-strong);
}

.urgent-tasks p {
  margin: 0;
  color: var(--color-text-soft);
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-pill);
  background: rgba(37, 99, 235, 0.16);
  color: #1d4ed8;
  font-weight: 600;
}

.empty {
  padding: 0.85rem 1rem;
  border-radius: var(--radius-md);
  background: rgba(248, 250, 252, 0.75);
  border: 1px dashed rgba(148, 163, 184, 0.4);
}

.task-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.task-item {
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: var(--radius-md);
  padding: 1rem 1.1rem;
  background: rgba(255, 255, 255, 0.72);
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
}

.task-item.soon {
  border-color: rgba(239, 68, 68, 0.4);
  box-shadow: 0 14px 32px rgba(239, 68, 68, 0.18);
}

.task-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
}

.task-head strong {
  color: var(--color-text-strong);
}

.stage {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  padding: 0.15rem 0.65rem;
  border-radius: var(--radius-pill);
  background: rgba(148, 163, 184, 0.18);
}

.task-desc {
  margin: 0;
  font-size: 0.95rem;
  color: var(--color-text-strong);
}

.task-time {
  margin: 0;
  font-size: 0.9rem;
  color: var(--color-text-muted);
}
</style>
