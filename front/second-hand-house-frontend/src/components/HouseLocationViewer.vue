<template>
  <section class="location-viewer surface-card">
    <header class="viewer-header">
      <div>
        <h2>{{ t('locationViewer.title') }}</h2>
        <p>{{ t('locationViewer.subtitle') }}</p>
      </div>
      <button type="button" :disabled="loading" @click="emit('refresh')">
        {{ loading ? t('locationViewer.refreshing') : t('locationViewer.refresh') }}
      </button>
    </header>

    <p v-if="updatedTime" class="updated-label">
      {{ t('locationViewer.updatedAt', { time: updatedTime }) }}
    </p>

    <div class="viewer-body">
      <div class="map-area">
        <div class="map-placeholder">
          <h3>{{ t('locationViewer.placeholder.heading') }}</h3>
          <p>{{ t('locationViewer.placeholder.description') }}</p>
          <p v-if="activeHighlight" class="active-highlight">{{ activeHighlight }}</p>
        </div>
        <div v-if="loading" class="loading-overlay">{{ t('locationViewer.loading') }}</div>
      </div>

      <aside class="list-panel">
        <h3>{{ t('locationViewer.listTitle') }}</h3>
        <p v-if="!locationItems.length" class="empty">{{ t('locationViewer.empty') }}</p>
        <ul v-else>
          <li v-for="item in locationItems" :key="item.key">
            <button
              type="button"
              :class="['list-item', { active: item.key === activeHouseId }]"
              @click="selectHouse(item.key)"
            >
              <span class="title">{{ item.title }}</span>
              <span class="address" :title="item.address">{{ item.address }}</span>
              <span v-if="item.priceLabel" class="meta">{{ item.priceLabel }}</span>
              <span class="action">{{ item.key === activeHouseId ? t('locationViewer.item.active') : t('locationViewer.item.focus') }}</span>
            </button>
          </li>
        </ul>
      </aside>
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
  }
});

const emit = defineEmits(['refresh']);

const translate = inject('translate', (key, vars) => key);
const settings = inject('appSettings', { language: 'zh' });
const t = (key, vars) => translate(key, vars);

const locale = computed(() => (settings?.language === 'en' ? 'en-US' : 'zh-CN'));

const normalizedHouses = computed(() =>
  Array.isArray(props.houses)
    ? props.houses.filter((house) => house && (house.title || house.address))
    : []
);

const activeHouseId = ref(null);

const formatPrice = (value) => {
  const num = Number(value);
  if (!Number.isFinite(num) || num <= 0) {
    return '';
  }
  return num.toLocaleString(locale.value, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  });
};

const locationItems = computed(() =>
  normalizedHouses.value.map((house, index) => {
    const key = house?.id != null ? String(house.id) : `idx-${index}`;
    const title = house?.title?.trim() || t('locationViewer.noTitle');
    const address = house?.address?.trim() || t('locationViewer.noAddress');
    const priceLabel = formatPrice(house?.price);
    return {
      key,
      title,
      address,
      priceLabel: priceLabel ? t('locationViewer.priceLabel', { price: priceLabel }) : '',
      house
    };
  })
);

watch(
  locationItems,
  (items) => {
    if (!items.length) {
      activeHouseId.value = null;
      return;
    }
    if (!items.some((item) => item.key === activeHouseId.value)) {
      activeHouseId.value = items[0].key;
    }
  },
  { immediate: true }
);

const activeHouse = computed(() => {
  if (!activeHouseId.value) {
    return null;
  }
  return locationItems.value.find((item) => item.key === activeHouseId.value)?.house ?? null;
});

const activeTitle = computed(() => activeHouse.value?.title?.trim() || t('locationViewer.noTitle'));
const activeAddress = computed(
  () => activeHouse.value?.address?.trim() || t('locationViewer.noAddress')
);

const activeHighlight = computed(() => {
  if (!activeHouse.value) {
    return '';
  }
  return t('locationViewer.placeholder.focus', {
    title: activeTitle.value,
    address: activeAddress.value
  });
});

const updatedTime = computed(() => {
  if (!props.updatedAt) {
    return '';
  }
  const date = new Date(props.updatedAt);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  return date.toLocaleString(locale.value, { hour12: false });
});

const selectHouse = (key) => {
  activeHouseId.value = key;
};
</script>

<style scoped>
.location-viewer {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  background: var(--gradient-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  padding: 1.8rem;
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
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.viewer-header button:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: var(--button-primary-shadow-hover);
}

.viewer-header button:disabled {
  opacity: 0.7;
  cursor: wait;
}

.updated-label {
  margin: 0;
  color: var(--color-text-soft);
  font-size: 0.9rem;
}

.viewer-body {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 1.25rem;
}

.map-area {
  position: relative;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
  overflow: hidden;
  min-height: 320px;
}

.map-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 2.5rem;
  gap: 0.8rem;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--color-surface-soft) 88%, transparent),
      color-mix(in srgb, var(--color-surface) 92%, transparent));
}

.map-placeholder h3 {
  margin: 0;
  font-size: 1.15rem;
  color: var(--color-text-strong);
}

.map-placeholder p {
  margin: 0;
  color: var(--color-text-soft);
  max-width: 32ch;
}

.active-highlight {
  font-weight: 600;
  color: var(--color-accent);
}

.loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.18);
  color: var(--color-text-on-emphasis);
  font-weight: 600;
}

.list-panel {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
  padding: 1.1rem;
  background: var(--panel-card-bg);
}

.list-panel h3 {
  margin: 0;
  font-size: 1.1rem;
  color: var(--color-text-strong);
}

.list-panel .empty {
  margin: 0;
  color: var(--color-text-soft);
  font-size: 0.95rem;
}

.list-panel ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.list-item {
  width: 100%;
  border: 1px solid color-mix(in srgb, var(--color-border) 68%, transparent);
  border-radius: var(--radius-lg);
  padding: 0.85rem 1rem;
  background: color-mix(in srgb, var(--color-surface) 85%, transparent);
  text-align: left;
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.4rem;
  transition: transform var(--transition-base), box-shadow var(--transition-base),
    border-color var(--transition-base);
}

.list-item .title {
  font-weight: 600;
  color: var(--color-text-strong);
}

.list-item .address {
  color: var(--color-text-soft);
  font-size: 0.92rem;
}

.list-item .meta {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.list-item .action {
  font-size: 0.85rem;
  color: var(--color-accent);
  font-weight: 600;
}

.list-item:not(.active):hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
  border-color: color-mix(in srgb, var(--color-accent) 35%, transparent);
}

.list-item.active {
  border-color: color-mix(in srgb, var(--color-accent) 55%, transparent);
  box-shadow: var(--shadow-md);
}

@media (max-width: 1024px) {
  .viewer-body {
    grid-template-columns: 1fr;
  }

  .map-area {
    min-height: 260px;
  }
}

@media (max-width: 640px) {
  .location-viewer {
    padding: 1.4rem;
  }

  .map-placeholder {
    padding: 2rem 1.4rem;
  }
}
</style>
