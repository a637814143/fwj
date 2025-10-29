<template>
  <section class="admin-house-manager surface-card">
    <header class="manager-header">
      <div>
        <h2>{{ t('adminManage.title') }}</h2>
        <p>{{ t('adminManage.subtitle', { count: houses.length }) }}</p>
      </div>
      <div class="header-actions">
        <button type="button" class="refresh" :disabled="loading" @click="$emit('refresh')">
          {{ loading ? t('adminManage.refreshing') : t('adminManage.refresh') }}
        </button>
      </div>
    </header>

    <div class="filters">
      <label>
        <span>{{ t('adminManage.filters.query') }}</span>
        <input
          v-model.trim="filters.query"
          type="search"
          :placeholder="t('adminManage.filters.queryPlaceholder')"
        />
      </label>
      <label>
        <span>{{ t('adminManage.filters.status') }}</span>
        <select v-model="filters.status">
          <option value="">{{ t('adminManage.filters.statusAll') }}</option>
          <option value="PENDING_REVIEW">{{ t('statuses.pending') }}</option>
          <option value="APPROVED">{{ t('statuses.approved') }}</option>
          <option value="REJECTED">{{ t('statuses.rejected') }}</option>
          <option value="SOLD">{{ t('statuses.sold') }}</option>
        </select>
      </label>
    </div>

    <div v-if="loading" class="state loading">{{ t('adminManage.loading') }}</div>
    <div v-else-if="!filteredHouses.length" class="state empty">
      {{ t('adminManage.empty') }}
    </div>
    <div v-else class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>{{ t('adminManage.table.listing') }}</th>
            <th>{{ t('adminManage.table.status') }}</th>
            <th>{{ t('adminManage.table.seller') }}</th>
            <th>{{ t('adminManage.table.updated') }}</th>
            <th>{{ t('adminManage.table.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="house in filteredHouses" :key="house.id ?? house.title">
            <td class="listing-cell">
              <strong>{{ house.title || t('adminManage.unknownTitle') }}</strong>
              <p class="address">{{ house.address || t('adminManage.unknownAddress') }}</p>
              <p v-if="house.price" class="price">￥{{ formatCurrency(house.price) }}</p>
            </td>
            <td>
              <span :class="['status', house.status]">{{ statusLabel(house.status) }}</span>
              <small v-if="house.reviewMessage" class="note">{{ house.reviewMessage }}</small>
            </td>
            <td>
              <div>{{ house.sellerUsername || t('adminManage.unknownSeller') }}</div>
              <div class="note">{{ house.sellerName || '—' }}</div>
              <div class="note">{{ house.contactNumber || '—' }}</div>
            </td>
            <td>{{ formatDate(house.updatedAt || house.listingDate) }}</td>
            <td class="actions">
              <button type="button" class="ghost" @click="openDetails(house)">
                {{ t('adminManage.actions.view') }}
              </button>
              <button
                type="button"
                class="warning"
                :disabled="!canUnlist(house)"
                @click="requestUnlist(house)"
              >
                {{ canUnlist(house) ? t('adminManage.actions.unlist') : t('adminManage.actions.unlistDisabled') }}
              </button>
              <button type="button" class="danger" @click="requestDelete(house)">
                {{ t('adminManage.actions.delete') }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <HouseDetailModal
      v-if="detailVisible"
      :house="activeDetail"
      can-view-sensitive-info
      @close="closeDetails"
    />
  </section>
</template>

<script setup>
import { computed, inject, reactive, ref } from 'vue';
import HouseDetailModal from './HouseDetailModal.vue';

const props = defineProps({
  houses: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  statusLabels: {
    type: Object,
    default: () => ({})
  }
});

const emit = defineEmits(['refresh', 'unlist', 'delete']);

const translate = inject('translate', (key, vars) => key);
const settings = inject('appSettings', { language: 'zh' });
const t = (key, vars) => translate(key, vars);

const locale = computed(() => (settings?.language === 'en' ? 'en-US' : 'zh-CN'));

const filters = reactive({ query: '', status: '' });

const normalizedHouses = computed(() =>
  (props.houses ?? []).filter((item) => item && (item.title || item.address || item.id != null))
);

const queryTokens = computed(() =>
  filters.query
    .split(/\s+/)
    .map((token) => token.trim())
    .filter(Boolean)
);

const filteredHouses = computed(() => {
  const status = filters.status;
  const tokens = queryTokens.value;
  return normalizedHouses.value.filter((house) => {
    if (status && house.status !== status) {
      return false;
    }
    if (!tokens.length) {
      return true;
    }
    const haystack = [
      house.title,
      house.address,
      house.sellerUsername,
      house.sellerName,
      house.reviewMessage
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
    return tokens.every((token) => haystack.includes(token.toLowerCase()));
  });
});

const activeDetail = ref(null);
const detailVisible = computed(() => Boolean(activeDetail.value));

const formatCurrency = (value) => {
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '0.00';
  }
  return num.toLocaleString(locale.value, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const formatDate = (value) => {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '—';
  }
  return date.toLocaleString(locale.value, { hour12: false });
};

const statusLabel = (status) => {
  return props.statusLabels?.[status] ?? status ?? t('adminManage.unknownStatus');
};

const canUnlist = (house) => house?.status === 'APPROVED';

const openDetails = (house) => {
  activeDetail.value = house;
};

const closeDetails = () => {
  activeDetail.value = null;
};

const requestUnlist = (house) => {
  if (!house) {
    return;
  }
  emit('unlist', house);
};

const requestDelete = (house) => {
  if (!house) {
    return;
  }
  emit('delete', house);
};
</script>

<style scoped>
.admin-house-manager {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 1.6rem;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-border) 85%, transparent);
  background: var(--gradient-surface);
  box-shadow: var(--shadow-md);
}

.manager-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.manager-header h2 {
  margin: 0;
  font-size: 1.35rem;
  color: var(--color-text-strong);
}

.manager-header p {
  margin: 0.3rem 0 0;
  color: var(--color-text-soft);
}

.header-actions {
  display: flex;
  gap: 0.6rem;
}

.header-actions .refresh {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.55rem 1.4rem;
  background: var(--gradient-primary);
  color: var(--color-text-on-emphasis);
  font-weight: 600;
  box-shadow: var(--button-primary-shadow);
  cursor: pointer;
}

.header-actions .refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: flex-end;
}

.filters label {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  font-weight: 600;
  color: var(--color-text-strong);
}

.filters input,
.filters select {
  min-width: 200px;
  padding: 0.6rem 0.75rem;
  border-radius: var(--radius-md);
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  background: rgba(255, 255, 255, 0.9);
}

.state {
  padding: 2.2rem;
  border-radius: var(--radius-lg);
  border: 1px dashed color-mix(in srgb, var(--color-border) 80%, transparent);
  text-align: center;
  font-weight: 600;
  color: var(--color-text-soft);
}

.state.loading {
  background: color-mix(in srgb, var(--color-surface) 80%, transparent);
}

.state.empty {
  background: color-mix(in srgb, var(--color-surface) 65%, transparent);
}

.table-wrapper {
  overflow-x: auto;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-border) 85%, transparent);
}

.table-wrapper table {
  width: 100%;
  border-collapse: collapse;
  background: rgba(255, 255, 255, 0.92);
}

.table-wrapper th,
.table-wrapper td {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  text-align: left;
  vertical-align: top;
}

.table-wrapper th {
  background: color-mix(in srgb, var(--color-surface) 78%, transparent);
  color: var(--color-text-strong);
  font-size: 0.95rem;
}

.listing-cell {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.listing-cell .address {
  margin: 0;
  color: var(--color-text-soft);
  font-size: 0.9rem;
}

.listing-cell .price {
  margin: 0;
  color: var(--color-accent);
  font-weight: 600;
}

.status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem 0.6rem;
  border-radius: var(--radius-pill);
  font-size: 0.85rem;
  background: color-mix(in srgb, var(--color-surface) 75%, transparent);
  color: var(--color-text-strong);
}

.status.APPROVED {
  color: #2f855a;
  background: rgba(47, 133, 90, 0.12);
}

.status.REJECTED {
  color: #c05621;
  background: rgba(192, 86, 33, 0.14);
}

.status.SOLD {
  color: #4a5568;
  background: rgba(74, 85, 104, 0.14);
}

.note {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.actions button {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.45rem 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.actions button:hover {
  transform: translateY(-1px);
}

.actions .ghost {
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
  color: var(--color-text-strong);
}

.actions .warning {
  background: rgba(224, 174, 96, 0.32);
  color: #8b5e11;
}

.actions .warning:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.actions .danger {
  background: rgba(204, 95, 95, 0.85);
  color: #fff;
}

@media (max-width: 960px) {
  .filters {
    flex-direction: column;
    align-items: stretch;
  }

  .table-wrapper table {
    font-size: 0.92rem;
  }

  .actions {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
