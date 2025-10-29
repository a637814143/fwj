<template>
  <section class="location-viewer surface-card">
    <header class="viewer-header">
      <div>
        <h2>{{ t('locationViewer.title') }}</h2>
        <p>{{ t('locationViewer.subtitle') }}</p>
      </div>
      <button type="button" :disabled="loading" @click="handleRefresh">
        {{ loading ? t('locationViewer.refreshing') : t('locationViewer.refresh') }}
      </button>
    </header>

    <p v-if="updatedTime" class="updated-label">
      {{ t('locationViewer.updatedAt', { time: updatedTime }) }}
    </p>

    <div class="viewer-body" :class="{ loading }">
      <aside class="list-panel">
        <h3>{{ t('locationViewer.listTitle') }}</h3>
        <p v-if="!items.length" class="empty">{{ t('locationViewer.empty') }}</p>
        <ul v-else>
          <li v-for="item in items" :key="item.key">
            <button
              type="button"
              :class="['list-item', { active: item.key === selectedId }]"
              @click="selectHouse(item.key)"
            >
              <span class="title">{{ item.title }}</span>
              <span class="address" :title="item.address">{{ item.address }}</span>
              <span v-if="item.priceLabel" class="meta">{{ item.priceLabel }}</span>
            </button>
          </li>
        </ul>
      </aside>

      <section class="detail-panel">
        <div v-if="activeHouse" class="detail-card">
          <h3>{{ activeHouse.title }}</h3>
          <p class="address">{{ activeHouse.address }}</p>
          <p v-if="activeHouse.priceLabel" class="price">{{ activeHouse.priceLabel }}</p>

          <div class="actions">
            <button type="button" class="ghost" @click="copyActiveAddress">
              {{ t('locationViewer.actions.copyAddress') }}
            </button>
            <button type="button" :disabled="!mapSearchUrl" @click="openMapSearch">
              {{ t('locationViewer.actions.openMap') }}
            </button>
          </div>

          <p class="hint">{{ t('locationViewer.manualSearchHint') }}</p>
          <p v-if="copyStatus" :class="['status', copyStatusType]">{{ copyStatus }}</p>
        </div>
        <div v-else class="detail-empty">
          <p>{{ t('locationViewer.noSelection') }}</p>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, inject, ref, watch } from 'vue';

const props = defineProps({
  houses: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  updatedAt: {
    type: String,
    default: ''
  },
  focusKey: {
    type: [String, Number],
    default: ''
  }
});

const emit = defineEmits(['refresh']);

const translate = inject('translate', (key, vars) => key);
const settings = inject('appSettings', { language: 'zh' });
const t = (key, vars) => translate(key, vars);

const locale = computed(() => (settings?.language === 'en' ? 'en-US' : 'zh-CN'));

const normalizeTitle = (value) => {
  if (typeof value === 'string' && value.trim()) {
    return value.trim();
  }
  return t('locationViewer.noTitle');
};

const normalizeAddress = (value) => {
  if (typeof value === 'string' && value.trim()) {
    return value.trim();
  }
  return t('locationViewer.noAddress');
};

const formatPriceLabel = (price) => {
  const numeric = Number(price);
  if (!Number.isFinite(numeric) || numeric <= 0) {
    return '';
  }
  const formatted = numeric.toLocaleString(locale.value, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  });
  return t('locationViewer.priceLabel', { price: formatted });
};

const items = computed(() => {
  if (!Array.isArray(props.houses)) {
    return [];
  }
  return props.houses
    .map((house) => ({
      key: house?.id != null ? String(house.id) : String(house?.key ?? ''),
      title: normalizeTitle(house?.title),
      address: normalizeAddress(house?.address),
      priceLabel: formatPriceLabel(house?.price)
    }))
    .filter((item) => item.key && item.address);
});

const selectedId = ref('');
const copyStatus = ref('');
const copyStatusType = ref('');

const updatedTime = computed(() => {
  if (!props.updatedAt) {
    return '';
  }
  const date = new Date(props.updatedAt);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  return date.toLocaleString(locale.value, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  });
});

const activeHouse = computed(() => {
  if (!selectedId.value) {
    return items.value[0] ?? null;
  }
  return items.value.find((item) => item.key === selectedId.value) ?? null;
});

watch(
  () => props.focusKey,
  (key) => {
    if (key == null || key === '') {
      return;
    }
    const normalized = String(key);
    if (items.value.some((item) => item.key === normalized)) {
      selectedId.value = normalized;
      copyStatus.value = '';
      copyStatusType.value = '';
    }
  },
  { immediate: true }
);

watch(
  items,
  (list) => {
    if (!list.some((item) => item.key === selectedId.value)) {
      selectedId.value = list[0]?.key ?? '';
    }
  },
  { immediate: true }
);

const buildMapSearchUrl = (address) => {
  if (!address) {
    return '';
  }
  return `https://uri.amap.com/search?keyword=${encodeURIComponent(address)}`;
};

const mapSearchUrl = computed(() => {
  const address = activeHouse.value?.address ?? '';
  if (!address || address === t('locationViewer.noAddress')) {
    return '';
  }
  return buildMapSearchUrl(address);
});

const openMapSearch = () => {
  if (!mapSearchUrl.value || typeof window === 'undefined') {
    return;
  }
  window.open(mapSearchUrl.value, '_blank', 'noopener');
};

const selectHouse = (key) => {
  selectedId.value = key;
  copyStatus.value = '';
  copyStatusType.value = '';
};

const handleRefresh = () => {
  copyStatus.value = '';
  copyStatusType.value = '';
  emit('refresh');
};

const copyActiveAddress = async () => {
  copyStatus.value = '';
  copyStatusType.value = '';
  const address = activeHouse.value?.address;
  if (!address || address === t('locationViewer.noAddress')) {
    copyStatus.value = t('locationViewer.status.copyUnavailable');
    copyStatusType.value = 'error';
    return;
  }
  if (typeof navigator === 'undefined') {
    copyStatus.value = t('locationViewer.status.copyManual', { address });
    copyStatusType.value = 'info';
    return;
  }
  if (navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(address);
      copyStatus.value = t('locationViewer.status.copySuccess');
      copyStatusType.value = 'success';
      return;
    } catch (error) {
      console.warn('Failed to copy address to clipboard', error);
    }
  }
  copyStatus.value = t('locationViewer.status.copyManual', { address });
  copyStatusType.value = 'info';
};
</script>

<style scoped>
.location-viewer {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  background: var(--gradient-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  padding: 1.75rem;
  box-shadow: var(--shadow-md);
}

.viewer-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.viewer-header h2 {
  margin: 0;
  color: var(--color-text-strong);
  font-size: 1.45rem;
}

.viewer-header p {
  margin: 0.3rem 0 0;
  color: var(--color-text-soft);
  font-size: 0.95rem;
}

.viewer-header button {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.55rem 1.4rem;
  background: var(--gradient-primary);
  color: var(--color-text-on-emphasis);
  font-weight: 600;
  box-shadow: var(--button-primary-shadow);
  cursor: pointer;
}

.viewer-header button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.updated-label {
  margin: -0.3rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.viewer-body {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 2fr);
  gap: 1.5rem;
}

.viewer-body.loading {
  opacity: 0.65;
  pointer-events: none;
}

.list-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.list-panel h3 {
  margin: 0;
  font-size: 1.1rem;
  color: var(--color-text-strong);
}

.list-panel ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.list-panel .empty {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.list-item {
  width: 100%;
  text-align: left;
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  border-radius: var(--radius-lg);
  padding: 0.75rem 1rem;
  background: color-mix(in srgb, var(--color-surface) 85%, transparent);
  color: inherit;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  transition: border 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.list-item .title {
  font-weight: 600;
  color: var(--color-text-strong);
}

.list-item .address {
  color: var(--color-text-soft);
  font-size: 0.9rem;
}

.list-item .meta {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.list-item.active {
  border-color: color-mix(in srgb, var(--color-primary) 60%, var(--color-border) 40%);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.detail-panel {
  border-radius: var(--radius-xl);
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  background: color-mix(in srgb, var(--color-surface) 90%, transparent);
  padding: 1.6rem;
  display: flex;
  align-items: stretch;
  justify-content: center;
}

.detail-card {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  width: 100%;
}

.detail-card h3 {
  margin: 0;
  font-size: 1.3rem;
  color: var(--color-text-strong);
}

.detail-card .address {
  margin: 0;
  color: var(--color-text-soft);
  font-size: 1rem;
}

.detail-card .price {
  margin: 0.25rem 0 0;
  color: var(--color-text-muted);
}

.actions {
  display: flex;
  gap: 0.8rem;
  flex-wrap: wrap;
}

.actions button {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.55rem 1.4rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: var(--button-primary-shadow);
  background: var(--gradient-primary);
  color: var(--color-text-on-emphasis);
}

.actions button.ghost {
  background: transparent;
  color: var(--color-text-strong);
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  box-shadow: none;
}

.actions button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.hint {
  margin: 0.25rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.status {
  margin: 0.35rem 0 0;
  font-size: 0.85rem;
}

.status.success {
  color: color-mix(in srgb, var(--color-success) 70%, var(--color-text-soft) 30%);
}

.status.error {
  color: color-mix(in srgb, var(--color-danger) 75%, var(--color-text-soft) 25%);
}

.status.info {
  color: color-mix(in srgb, var(--color-primary) 65%, var(--color-text-soft) 35%);
}

.detail-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  color: var(--color-text-muted);
  font-size: 0.95rem;
}

@media (max-width: 1024px) {
  .viewer-body {
    grid-template-columns: 1fr;
  }

  .detail-panel {
    min-height: 220px;
  }
}
</style>
