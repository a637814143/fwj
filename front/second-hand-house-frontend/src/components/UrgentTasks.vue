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
        <div class="task-actions">
          <button type="button" class="open" @click="openTask(task)">
            {{ t('orders.urgent.openTarget') }}
          </button>
          <button type="button" class="mark-read" @click="markRead(task)">
            {{ t('orders.urgent.markRead') }}
          </button>
        </div>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { computed, inject } from 'vue';

const emit = defineEmits(['mark-read', 'navigate']);

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

const markRead = (task) => {
  if (!task) {
    return;
  }
  const key = task.key ?? task.id;
  if (key == null) {
    return;
  }
  emit('mark-read', String(key));
};

const openTask = (task) => {
  if (!task) {
    return;
  }
  emit('navigate', task);
};
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

.task-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6rem;
}

.open {
  border: none;
  background: var(--gradient-primary);
  color: var(--color-text-on-emphasis);
  border-radius: var(--radius-pill);
  padding: 0.35rem 1rem;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.2);
  transition: transform var(--transition-base), box-shadow var(--transition-base),
    opacity var(--transition-base);
}

.open:hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 30px rgba(37, 99, 235, 0.28);
}

.mark-read {
  border: 1px solid rgba(59, 130, 246, 0.45);
  background: rgba(59, 130, 246, 0.08);
  color: #1d4ed8;
  border-radius: var(--radius-pill);
  padding: 0.35rem 0.9rem;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: background var(--transition-base), border-color var(--transition-base),
    box-shadow var(--transition-base);
}

.mark-read:hover,
.mark-read:focus {
  outline: none;
  background: rgba(59, 130, 246, 0.16);
  border-color: rgba(37, 99, 235, 0.6);
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.18);
}
</style>
