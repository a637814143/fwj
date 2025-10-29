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
              <span class="address" :title="item.rawAddress || item.address">{{ item.address }}</span>
              <span v-if="item.priceLabel" class="meta">{{ item.priceLabel }}</span>
            </button>
          </li>
        </ul>
      </aside>

      <section class="detail-panel">
        <div v-if="activeHouse" class="detail-card">
          <div class="detail-heading">
            <h3>{{ activeHouse.title }}</h3>
            <p class="address">{{ activeHouse.address }}</p>
            <p v-if="activeHouse.priceLabel" class="price">{{ activeHouse.priceLabel }}</p>
          </div>

          <div class="map-wrapper">
            <div ref="mapContainerRef" class="map-container"></div>
            <div v-if="mapLoading" class="map-overlay">
              <span>{{ t('locationViewer.map.locating') }}</span>
            </div>
          </div>

          <div v-if="mapError" class="map-status error">{{ mapError }}</div>
          <div v-else-if="mapDetails" class="map-status success">
            <p v-if="mapDetails.name" class="place">
              {{ t('locationViewer.map.place', { name: mapDetails.name }) }}
            </p>
            <p class="place-address">{{ mapDetails.address }}</p>
            <p class="coords">
              {{
                t('locationViewer.map.coordinates', {
                  lat: formatCoordinate(mapDetails.lat),
                  lng: formatCoordinate(mapDetails.lng)
                })
              }}
            </p>
            <p class="hint">{{ t('locationViewer.map.explore') }}</p>
          </div>
          <div v-else class="map-status idle">{{ t('locationViewer.map.idle') }}</div>

          <div class="actions">
            <button type="button" class="ghost" @click="copyActiveAddress">
              {{ t('locationViewer.actions.copyAddress') }}
            </button>
            <button type="button" :disabled="!activeHouse" @click="locateActiveHouse(true)">
              {{ t('locationViewer.actions.locate') }}
            </button>
          </div>

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
import { computed, inject, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

const markerIcons = {
  iconRetinaUrl: new URL('leaflet/dist/images/marker-icon-2x.png', import.meta.url).toString(),
  iconUrl: new URL('leaflet/dist/images/marker-icon.png', import.meta.url).toString(),
  shadowUrl: new URL('leaflet/dist/images/marker-shadow.png', import.meta.url).toString()
};

L.Icon.Default.mergeOptions(markerIcons);

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
  },
  apiBaseUrl: {
    type: String,
    default: 'http://localhost:8080/api'
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

const sanitizeCoordinate = (value, min, max) => {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return null;
  }
  if (numeric < min || numeric > max) {
    return null;
  }
  return Math.round(numeric * 1_000_000) / 1_000_000;
};

const normalizeSignature = (value) => {
  if (typeof value !== 'string') {
    return '';
  }
  return value.replace(/\s+/g, '').toLowerCase();
};

const items = computed(() => {
  if (!Array.isArray(props.houses)) {
    return [];
  }
  return props.houses
    .map((house) => ({
      key: house?.id != null ? String(house.id) : String(house?.key ?? ''),
      title: normalizeTitle(house?.title),
      rawTitle: typeof house?.title === 'string' ? house.title : '',
      address: normalizeAddress(house?.address),
      rawAddress: typeof house?.address === 'string' ? house.address : '',
      priceLabel: formatPriceLabel(house?.price),
      latitude: sanitizeCoordinate(house?.latitude, -90, 90),
      longitude: sanitizeCoordinate(house?.longitude, -180, 180)
    }))
    .filter((item) => item.key);
});

const selectedId = ref('');
const copyStatus = ref('');
const copyStatusType = ref('');

const mapContainerRef = ref(null);
const mapLoading = ref(false);
const mapError = ref('');
const activeLocation = ref(null);

let mapInstance = null;
let tileLayer = null;
let markerLayer = null;
let locateSequence = 0;

const defaultCenter = [35.8617, 104.1954];
const defaultZoom = 4;
const focusZoom = 16;

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

const coordinateCache = new Map();

const pruneCoordinateCache = (list) => {
  const signatures = new Map(list.map((item) => [item.key, normalizeSignature(item.rawAddress)]));
  for (const [key, entry] of coordinateCache.entries()) {
    const signature = signatures.get(key);
    if (!signature || signature !== entry.sourceAddress) {
      coordinateCache.delete(key);
    }
  }
};

watch(
  items,
  (list) => {
    if (!list.some((item) => item.key === selectedId.value)) {
      selectedId.value = list[0]?.key ?? '';
    }
    pruneCoordinateCache(list);
  },
  { immediate: true }
);

const normalizedApiBaseUrl = computed(() => {
  const base = props.apiBaseUrl ?? '';
  if (!base) {
    return '';
  }
  return base.replace(/\/$/, '');
});

const ensureMapReady = async () => {
  if (mapInstance || !mapContainerRef.value) {
    return;
  }
  mapInstance = L.map(mapContainerRef.value, { zoomControl: true, attributionControl: true });
  tileLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '© OpenStreetMap contributors'
  });
  tileLayer.addTo(mapInstance);
  mapInstance.setView(defaultCenter, defaultZoom);
};

const resetMapView = async () => {
  await nextTick();
  if (!mapContainerRef.value) {
    return;
  }
  await ensureMapReady();
  if (!mapInstance) {
    return;
  }
  if (markerLayer) {
    markerLayer.remove();
    markerLayer = null;
  }
  mapInstance.setView(defaultCenter, defaultZoom);
  mapInstance.invalidateSize();
};

const fallbackTitle = (house) => {
  const raw = typeof house?.rawTitle === 'string' ? house.rawTitle.trim() : '';
  if (raw) {
    return raw;
  }
  return house?.title ?? t('locationViewer.noTitle');
};

const buildLocationEntry = (house, lat, lng, name, address) => ({
  lat,
  lng,
  name: name && name.trim() ? name.trim() : fallbackTitle(house),
  address: address && address.trim() ? address.trim() : house?.rawAddress?.trim() || house?.address || '',
  sourceAddress: normalizeSignature(house?.rawAddress)
});

const setMapToCoordinates = async (entry) => {
  await nextTick();
  await ensureMapReady();
  if (!mapInstance) {
    return;
  }
  const position = [entry.lat, entry.lng];
  mapInstance.setView(position, focusZoom);
  if (!markerLayer) {
    markerLayer = L.marker(position);
    markerLayer.addTo(mapInstance);
  } else {
    markerLayer.setLatLng(position);
  }
  mapInstance.invalidateSize();
};

const updateMapForHouse = async (house, { forceRefresh = false } = {}) => {
  locateSequence += 1;
  const token = locateSequence;
  mapError.value = '';
  activeLocation.value = null;

  if (!house) {
    await resetMapView();
    return;
  }

  if (forceRefresh) {
    coordinateCache.delete(house.key);
  }

  const signature = normalizeSignature(house.rawAddress);
  const cached = coordinateCache.get(house.key);
  if (cached && cached.sourceAddress === signature) {
    activeLocation.value = cached;
    await setMapToCoordinates(cached);
    return;
  }

  if (house.latitude != null && house.longitude != null) {
    const entry = buildLocationEntry(house, house.latitude, house.longitude, house.rawTitle, house.rawAddress);
    coordinateCache.set(house.key, entry);
    activeLocation.value = entry;
    await setMapToCoordinates(entry);
    return;
  }

  const address = house.rawAddress?.trim();
  if (!address) {
    mapError.value = t('locationViewer.map.addressMissing');
    await resetMapView();
    return;
  }

  const base = normalizedApiBaseUrl.value;
  if (!base) {
    mapError.value = t('locationViewer.map.error');
    await resetMapView();
    return;
  }

  mapLoading.value = true;
  try {
    const response = await fetch(`${base}/houses/map-search?query=${encodeURIComponent(address)}`);
    if (token !== locateSequence) {
      return;
    }
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const payload = await response.json();
    if (token !== locateSequence) {
      return;
    }
    if (payload?.found && Number.isFinite(payload.latitude) && Number.isFinite(payload.longitude)) {
      const lat = sanitizeCoordinate(payload.latitude, -90, 90);
      const lng = sanitizeCoordinate(payload.longitude, -180, 180);
      if (lat == null || lng == null) {
        mapError.value = t('locationViewer.map.noResult');
        await resetMapView();
        return;
      }
      const entry = buildLocationEntry(
        house,
        lat,
        lng,
        payload.name,
        payload.address
      );
      coordinateCache.set(house.key, entry);
      activeLocation.value = entry;
      await setMapToCoordinates(entry);
      return;
    }
    mapError.value = t('locationViewer.map.noResult');
    await resetMapView();
  } catch (error) {
    if (token === locateSequence) {
      console.warn('Failed to locate house on map', error);
      mapError.value = t('locationViewer.map.error');
      await resetMapView();
    }
  } finally {
    if (token === locateSequence) {
      mapLoading.value = false;
    }
  }
};

watch(
  activeHouse,
  (house, previous) => {
    const previousKey = previous?.key ?? '';
    const previousSignature = normalizeSignature(previous?.rawAddress ?? '');
    const nextSignature = normalizeSignature(house?.rawAddress ?? '');
    if (house?.key === previousKey && previousSignature === nextSignature) {
      return;
    }
    updateMapForHouse(house ?? null, { forceRefresh: false });
  },
  { immediate: true }
);

const mapDetails = computed(() => {
  if (!activeLocation.value) {
    return null;
  }
  return {
    name: activeLocation.value.name,
    address: activeLocation.value.address,
    lat: activeLocation.value.lat,
    lng: activeLocation.value.lng
  };
});

const selectHouse = (key) => {
  selectedId.value = key;
  copyStatus.value = '';
  copyStatusType.value = '';
};

const handleRefresh = () => {
  copyStatus.value = '';
  copyStatusType.value = '';
  mapError.value = '';
  emit('refresh');
};

const locateActiveHouse = (force = false) => {
  if (!activeHouse.value) {
    return;
  }
  updateMapForHouse(activeHouse.value, { forceRefresh: force });
};

const copyActiveAddress = async () => {
  copyStatus.value = '';
  copyStatusType.value = '';
  const address = activeHouse.value?.rawAddress;
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

const formatCoordinate = (value) => {
  if (!Number.isFinite(value)) {
    return '';
  }
  return Number(value).toFixed(6);
};

onMounted(() => {
  nextTick(ensureMapReady);
});

onBeforeUnmount(() => {
  coordinateCache.clear();
  if (mapInstance) {
    mapInstance.remove();
  }
  mapInstance = null;
  tileLayer = null;
  markerLayer = null;
});
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
  gap: 1rem;
  width: 100%;
}

.detail-heading h3 {
  margin: 0;
  font-size: 1.3rem;
  color: var(--color-text-strong);
}

.detail-heading .address {
  margin: 0.35rem 0 0;
  color: var(--color-text-soft);
  font-size: 0.95rem;
}

.detail-heading .price {
  margin: 0.1rem 0 0;
  color: var(--color-primary);
  font-weight: 600;
}

.map-wrapper {
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--color-border) 75%, transparent);
  min-height: 320px;
}

.map-container {
  width: 100%;
  height: 100%;
}

.map-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--color-surface) 70%, transparent 30%);
  color: var(--color-text-strong);
  font-weight: 600;
  font-size: 0.95rem;
  text-align: center;
  padding: 1rem;
}

.map-status {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.95rem;
}

.map-status.success {
  color: var(--color-text-strong);
}

.map-status.success .coords {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.85rem;
  color: var(--color-text-soft);
}

.map-status.success .hint {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.map-status.error {
  color: var(--color-danger);
  font-weight: 600;
}

.map-status.idle {
  color: var(--color-text-muted);
}

.actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.actions button {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.55rem 1.4rem;
  cursor: pointer;
  font-weight: 600;
  background: var(--gradient-primary);
  color: var(--color-text-on-emphasis);
  box-shadow: var(--button-primary-shadow);
}

.actions button.ghost {
  background: transparent;
  color: var(--color-primary);
  border: 1px solid color-mix(in srgb, var(--color-primary) 70%, transparent);
  box-shadow: none;
}

.actions button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.status {
  margin: 0;
  font-size: 0.9rem;
}

.status.success {
  color: var(--color-success);
}

.status.error {
  color: var(--color-danger);
}

.status.info {
  color: var(--color-text-muted);
}

.detail-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--color-text-soft);
}

@media (max-width: 1024px) {
  .viewer-body {
    grid-template-columns: 1fr;
  }

  .detail-panel {
    padding: 1.25rem;
  }
}
</style>
