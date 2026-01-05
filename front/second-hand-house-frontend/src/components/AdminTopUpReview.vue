<template>
  <section class="admin-topup-review">
    <header>
      <div>
        <h2>{{ t('adminTopUps.title') }}</h2>
        <p>{{ t('adminTopUps.subtitle') }}</p>
      </div>
      <button type="button" @click="$emit('refresh')">{{ t('adminTopUps.refresh') }}</button>
    </header>

    <div v-if="loading" class="loading">{{ t('adminTopUps.loading') }}</div>
    <div v-else-if="!topUps || topUps.length === 0" class="empty">{{ t('adminTopUps.empty') }}</div>
    <table v-else>
      <thead>
        <tr>
          <th>{{ t('adminTopUps.columns.id') }}</th>
          <th>{{ t('adminTopUps.columns.user') }}</th>
          <th>{{ t('adminTopUps.columns.role') }}</th>
          <th>{{ t('adminTopUps.columns.amount') }}</th>
          <th>{{ t('adminTopUps.columns.reference') }}</th>
          <th>{{ t('adminTopUps.columns.createdAt') }}</th>
          <th>{{ t('adminTopUps.columns.actions') }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in topUps" :key="item.id">
          <td>{{ item.id }}</td>
          <td>
            <strong>{{ item.displayName || item.username }}</strong>
            <div class="muted">@{{ item.username }}</div>
          </td>
          <td>{{ roleLabel(item.role) }}</td>
          <td>￥{{ formatAmount(item.amount) }}</td>
          <td>
            <span class="reference" :title="item.reference || '-'">
              {{ item.reference || '-' }}
            </span>
          </td>
          <td>{{ formatTime(item.createdAt) }}</td>
          <td class="actions">
            <button type="button" class="primary" @click="decide(item, 'APPROVE')">
              {{ t('adminTopUps.actions.approve') }}
            </button>
            <button type="button" class="secondary" @click="decide(item, 'REJECT')">
              {{ t('adminTopUps.actions.reject') }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { computed, inject } from 'vue';

const props = defineProps({
  topUps: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['refresh', 'decide']);

const translate = inject('translate', (key, vars) => key);
const settings = inject('appSettings', { language: 'zh' });
const t = (key, vars) => translate(key, vars);

const locale = computed(() => (settings?.language === 'en' ? 'en-US' : 'zh-CN'));

const roleLabels = computed(() => {
  const labels = t('roles');
  return labels && typeof labels === 'object' ? labels : {};
});

const formatAmount = (value) => {
  if (value == null) {
    return '0.00';
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '0.00';
  }
  return num.toLocaleString(locale.value, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const formatTime = (value) => {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString(locale.value, { hour12: false });
};

const roleLabel = (value) => roleLabels.value?.[String(value).toLowerCase()] ?? value ?? '-';

const decide = (item, decision) => {
  emit('decide', { topUpId: item.id, decision });
};
</script>

<style scoped>
.admin-topup-review {
  display: flex;
  flex-direction: column;
  gap: 1.4rem;
  background: var(--gradient-surface);
  border-radius: var(--radius-lg);
  padding: 1.8rem;
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(var(--glass-blur));
}

header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

header h2 {
  margin: 0;
  color: var(--color-text-strong);
}

header p {
  margin: 0.3rem 0 0;
  color: var(--color-text-soft);
}

header button {
  align-self: center;
  padding: 0.45rem 1.1rem;
  border-radius: var(--radius-pill);
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.loading,
.empty {
  margin: 0;
  color: var(--color-text-soft);
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid rgba(148, 163, 184, 0.25);
  word-break: break-word;
  vertical-align: top;
}

th {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.muted {
  color: var(--color-text-soft);
  font-size: 0.85rem;
}

.reference {
  color: var(--color-text-strong);
  white-space: pre-line;
}

.actions {
  display: flex;
  gap: 0.45rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.actions button {
  padding: 0.4rem 0.85rem;
  border-radius: var(--radius-pill);
  border: none;
  cursor: pointer;
  font-weight: 600;
}

.actions .primary {
  background: var(--color-success);
  color: #fff;
}

.actions .secondary {
  background: #f8fafc;
  color: var(--color-text-strong);
  border: 1px solid rgba(148, 163, 184, 0.3);
}

.actions button:hover {
  filter: brightness(0.98);
}
</style>
