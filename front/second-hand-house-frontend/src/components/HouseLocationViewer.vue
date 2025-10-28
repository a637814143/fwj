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
        <div class="map-wrapper">
          <div
            ref="mapContainer"
            class="map-canvas"
            role="region"
            :aria-label="t('locationViewer.aria.map')"
          ></div>
          <div v-if="mapError" class="map-message error">{{ mapError }}</div>
          <div v-else-if="showLoadingOverlay" class="map-message loading">
            {{ loading ? t('locationViewer.loading') : t('locationViewer.mapLoading') }}
          </div>
        </div>

        <div v-if="activeSummary" class="active-summary">
          <h3>{{ activeSummary.title }}</h3>
          <p>{{ activeSummary.address }}</p>
          <p v-if="activeSummary.price" class="price">{{ activeSummary.price }}</p>
        </div>

        <p v-if="geocodeWarnings.length" class="map-warning">
          {{ t('locationViewer.errors.partial', { count: geocodeWarnings.length }) }}
        </p>
        <ul v-if="geocodeWarnings.length" class="warning-list">
          <li v-for="item in geocodeWarnings" :key="item.key">
            <span class="title">{{ item.title }}</span>
            <span class="address">{{ item.address }}</span>
          </li>
        </ul>
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
              <span class="action">
                {{ item.key === activeHouseId ? t('locationViewer.item.active') : t('locationViewer.item.focus') }}
              </span>
            </button>
          </li>
        </ul>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, inject, onBeforeUnmount, onMounted, ref, watch } from 'vue';

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

const mapContainer = ref(null);
const mapReady = ref(false);
const mapBusy = ref(false);
const mapError = ref('');
const geocodeWarnings = ref([]);
const isMounted = ref(false);

const defaultMapKey = 'YZFBZ-RGPC5-6XLIX-ID5GC-3V336-M5BYA';
const mapKey = import.meta.env.VITE_TENCENT_MAP_KEY || defaultMapKey;

const normalizedHouses = computed(() =>
  Array.isArray(props.houses)
    ? props.houses.filter((house) => house && (house.title || house.address))
    : []
);

const activeHouseId = ref(null);
const pendingNavigationKey = ref(null);

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

const activeListItem = computed(() =>
  locationItems.value.find((item) => item.key === activeHouseId.value) ?? null
);

const activeSummary = computed(() => {
  if (!activeListItem.value) {
    return null;
  }
  return {
    title: activeListItem.value.title,
    address: activeListItem.value.address,
    price: activeListItem.value.priceLabel
  };
});

watch(
  locationItems,
  (items) => {
    if (!items.length) {
      activeHouseId.value = null;
      pendingNavigationKey.value = null;
      return;
    }
    const hasActive = items.some((item) => item.key === activeHouseId.value);
    if (!hasActive) {
      activeHouseId.value = items[0].key;
    }
    if (
      pendingNavigationKey.value &&
      !items.some((item) => item.key === pendingNavigationKey.value)
    ) {
      pendingNavigationKey.value = null;
    }
  },
  { immediate: true }
);

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
  pendingNavigationKey.value = key;
};

const showLoadingOverlay = computed(() => props.loading || mapBusy.value || !mapReady.value);

const geocodeCache = new Map();
const pendingGeocodes = new Map();

const extractRegion = (address) => {
  if (!address || typeof address !== 'string') {
    return '';
  }
  const text = address.trim();
  const provinceMatch = text.match(/^(.*?(省|自治区|特别行政区))/);
  if (provinceMatch) {
    return provinceMatch[1];
  }
  const municipalityMatch = text.match(/(北京|上海|天津|重庆)/);
  if (municipalityMatch) {
    return `${municipalityMatch[1]}市`;
  }
  const cityMatch = text.match(/([\u4e00-\u9fa5]{2,6}市)/);
  if (cityMatch) {
    return cityMatch[1];
  }
  return '';
};

const markerIcon = (fill, stroke) => {
  const svg = `<?xml version="1.0" encoding="UTF-8"?>
  <svg xmlns="http://www.w3.org/2000/svg" width="36" height="48" viewBox="0 0 36 48">
    <path d="M18 1C9.163 1 2 8.163 2 17c0 12.188 16 30 16 30s16-17.812 16-30C34 8.163 26.837 1 18 1z" fill="${fill}" stroke="${stroke}" stroke-width="2"/>
    <circle cx="18" cy="18" r="6" fill="#ffffff"/>
  </svg>`;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
};

let mapInstance = null;
let markerLayer = null;
let latestGeometries = [];
let scriptPromise = null;
let updateToken = 0;

const loadTencentMapScript = () => {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('window-unavailable'));
  }
  if (window.TMap) {
    return Promise.resolve(window.TMap);
  }
  if (!mapKey) {
    return Promise.reject(new Error('missing-key'));
  }
  if (scriptPromise) {
    return scriptPromise;
  }
  scriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = `https://map.qq.com/api/gljs?v=1.exp&libraries=service&key=${mapKey}`;
    script.async = true;
    script.onload = () => {
      if (window.TMap) {
        resolve(window.TMap);
      } else {
        reject(new Error('tmap-unavailable'));
      }
    };
    script.onerror = () => reject(new Error('script-failed'));
    document.head.appendChild(script);
  }).catch((error) => {
    scriptPromise = null;
    throw error;
  });
  return scriptPromise;
};

const ensureMap = async () => {
  if (!mapContainer.value) {
    throw new Error('container-missing');
  }
  const TMap = await loadTencentMapScript();
  if (!mapInstance) {
    mapInstance = new TMap.Map(mapContainer.value, {
      center: new TMap.LatLng(39.908823, 116.39747),
      zoom: 11,
      pitch: 0
    });
  }
  if (!markerLayer) {
    markerLayer = new TMap.MultiMarker({
      map: mapInstance,
      styles: {
        default: new TMap.MarkerStyle({
          width: 30,
          height: 42,
          anchor: { x: 15, y: 42 },
          src: markerIcon('#6f7a99', '#47506c')
        }),
        active: new TMap.MarkerStyle({
          width: 32,
          height: 44,
          anchor: { x: 16, y: 44 },
          src: markerIcon('#d28a7c', '#a04f3f')
        })
      },
      geometries: []
    });
  }
  mapReady.value = true;
  return TMap;
};

const geocodeHouse = async (house) => {
  if (!house) {
    return null;
  }
  const candidates = ['latitude', 'lat'];
  const longitudeFields = ['longitude', 'lng'];
  let lat = null;
  let lng = null;
  for (const field of candidates) {
    const value = Number(house?.[field]);
    if (Number.isFinite(value)) {
      lat = value;
      break;
    }
  }
  for (const field of longitudeFields) {
    const value = Number(house?.[field]);
    if (Number.isFinite(value)) {
      lng = value;
      break;
    }
  }
  if (Number.isFinite(lat) && Number.isFinite(lng)) {
    return { lat, lng };
  }
  const address = house?.address?.trim();
  if (!address) {
    return null;
  }
  if (geocodeCache.has(address)) {
    return geocodeCache.get(address);
  }
  if (pendingGeocodes.has(address)) {
    return pendingGeocodes.get(address);
  }
  const params = new URLSearchParams({
    address,
    key: mapKey,
    output: 'json',
    get_poi: '0'
  });
  const region = extractRegion(address);
  if (region) {
    params.set('region', region);
  }
  const request = fetch(`https://apis.map.qq.com/ws/geocoder/v1/?${params.toString()}`)
    .then((response) => {
      if (!response.ok) {
        throw new Error('geocoder-http');
      }
      return response.json();
    })
    .then((payload) => {
      if (payload?.status === 0 && payload?.result?.location) {
        const location = payload.result.location;
        const resolved = {
          lat: Number(location.lat),
          lng: Number(location.lng)
        };
        if (Number.isFinite(resolved.lat) && Number.isFinite(resolved.lng)) {
          geocodeCache.set(address, resolved);
          return resolved;
        }
      }
      throw new Error('geocoder-empty');
    })
    .catch((error) => {
      console.warn('Failed to geocode address', address, error);
      return null;
    })
    .finally(() => {
      pendingGeocodes.delete(address);
    });
  pendingGeocodes.set(address, request);
  return request;
};

const focusOnActiveGeometry = () => {
  if (!mapInstance || !latestGeometries.length) {
    return;
  }
  const active =
    latestGeometries.find((geometry) => geometry.id === activeHouseId.value) ||
    latestGeometries[0];
  if (!active) {
    return;
  }
  try {
    if (typeof mapInstance.setCenter === 'function') {
      mapInstance.setCenter(active.position);
    }
    if (typeof mapInstance.getZoom === 'function' && typeof mapInstance.setZoom === 'function') {
      const currentZoom = mapInstance.getZoom();
      if (!currentZoom || currentZoom < 16) {
        mapInstance.setZoom(16);
      }
    }
  } catch (error) {
    console.warn('Failed to focus map marker', error);
  }
};

const highlightActiveMarker = () => {
  if (!markerLayer || !latestGeometries.length) {
    return;
  }
  const geometries = latestGeometries.map((geometry) => ({
    ...geometry,
    styleId: geometry.id === activeHouseId.value ? 'active' : 'default'
  }));
  latestGeometries = geometries;
  markerLayer.setGeometries(geometries);
  focusOnActiveGeometry();
  openExternalMapIfReady();
};

const openExternalMap = (geometry) => {
  if (typeof window === 'undefined' || !geometry?.properties?.coords) {
    return;
  }
  const coords = geometry.properties.coords;
  const title = geometry.properties?.title ?? '';
  const address = geometry.properties?.address ?? '';
  const marker = `coord:${coords.lat},${coords.lng};title:${encodeURIComponent(title)};addr:${encodeURIComponent(address)}`;
  const url = `https://map.qq.com/?type=marker&isopeninfowin=1&markertype=1&marker=${marker}&zoom=18`;
  window.open(url, '_blank', 'noopener');
};

const openExternalMapIfReady = () => {
  if (!pendingNavigationKey.value) {
    return;
  }
  const key = pendingNavigationKey.value;
  const geometry = latestGeometries.find((item) => item.id === key);
  if (!geometry || !geometry.properties?.coords) {
    return;
  }
  pendingNavigationKey.value = null;
  openExternalMap(geometry);
};

const updateMapMarkers = async () => {
  if (!isMounted.value || props.loading) {
    return;
  }
  if (!mapKey) {
    mapError.value = t('locationViewer.errors.missingKey');
    return;
  }
  if (!locationItems.value.length) {
    latestGeometries = [];
    geocodeWarnings.value = [];
    if (markerLayer) {
      markerLayer.setGeometries([]);
    }
    return;
  }
  if (typeof window === 'undefined') {
    return;
  }
  mapBusy.value = true;
  mapError.value = '';
  const token = ++updateToken;
  try {
    const TMap = await ensureMap();
    const results = await Promise.all(
      locationItems.value.map((item) => geocodeHouse(item.house))
    );
    if (token !== updateToken) {
      return;
    }
    const geometries = [];
    const warnings = [];
    results.forEach((coords, index) => {
      const item = locationItems.value[index];
      if (!coords) {
        warnings.push(item);
        return;
      }
      const position = new TMap.LatLng(coords.lat, coords.lng);
      geometries.push({
        id: item.key,
        position,
        styleId: item.key === activeHouseId.value ? 'active' : 'default',
        properties: {
          title: item.title,
          address: item.address,
          price: item.priceLabel,
          coords
        }
      });
    });
    latestGeometries = geometries;
    geocodeWarnings.value = warnings;
    if (markerLayer) {
      markerLayer.setGeometries(geometries);
    }
    if (!geometries.length) {
      mapError.value = warnings.length
        ? t('locationViewer.errors.geocode')
        : t('locationViewer.errors.mapInit');
    } else {
      focusOnActiveGeometry();
      openExternalMapIfReady();
    }
  } catch (error) {
    console.error('Failed to update Tencent Map', error);
    mapError.value = mapError.value || t('locationViewer.errors.mapInit');
  } finally {
    if (token === updateToken) {
      mapBusy.value = false;
    }
  }
};

watch(() => props.loading, (value) => {
  if (!value) {
    updateMapMarkers();
  }
});

watch(locationItems, () => {
  updateMapMarkers();
});

watch(activeHouseId, () => {
  highlightActiveMarker();
});

onMounted(() => {
  isMounted.value = true;
  updateMapMarkers();
});

onBeforeUnmount(() => {
  latestGeometries = [];
  if (markerLayer && typeof markerLayer.setGeometries === 'function') {
    markerLayer.setGeometries([]);
  }
  markerLayer = null;
  if (mapInstance && typeof mapInstance.destroy === 'function') {
    mapInstance.destroy();
  }
  mapInstance = null;
});
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
  cursor: pointer;
}

.viewer-header button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.updated-label {
  margin: -0.2rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.viewer-body {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 1.6rem;
}

.map-area {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.map-wrapper {
  position: relative;
  border-radius: var(--radius-xl);
  overflow: hidden;
  min-height: 320px;
  background: color-mix(in srgb, var(--color-surface) 70%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-border) 85%, transparent);
}

.map-canvas {
  width: 100%;
  height: 100%;
  min-height: 320px;
}

.map-message {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: color-mix(in srgb, var(--color-surface) 90%, transparent 10%);
  color: var(--color-text-soft);
  text-align: center;
  font-weight: 600;
}

.map-message.loading {
  background: color-mix(in srgb, var(--color-surface) 86%, transparent 14%);
}

.map-message.error {
  background: color-mix(in srgb, var(--color-danger) 12%, var(--color-surface) 88%);
  color: var(--color-danger-strong);
}

.active-summary {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.85rem 1rem;
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--color-surface) 78%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
}

.active-summary h3 {
  margin: 0;
  font-size: 1.05rem;
  color: var(--color-text-strong);
}

.active-summary p {
  margin: 0;
  color: var(--color-text-soft);
}

.active-summary .price {
  color: var(--color-accent);
  font-weight: 600;
}

.map-warning {
  margin: 0;
  color: color-mix(in srgb, var(--color-accent) 70%, var(--color-text-soft) 30%);
  font-size: 0.9rem;
  font-weight: 600;
}

.warning-list {
  margin: 0;
  padding: 0 0 0 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  color: var(--color-text-soft);
}

.warning-list .title {
  font-weight: 600;
  margin-right: 0.35rem;
}

.list-panel {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.list-panel h3 {
  margin: 0;
  font-size: 1.1rem;
  color: var(--color-text-strong);
}

.list-panel .empty {
  margin: 0;
  color: var(--color-text-muted);
}

.list-panel ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.list-item {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.35rem;
  padding: 0.75rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-border) 75%, transparent);
  background: color-mix(in srgb, var(--color-surface) 75%, transparent);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.list-item:hover {
  border-color: color-mix(in srgb, var(--color-accent) 40%, var(--color-border) 60%);
  transform: translateY(-1px);
}

.list-item.active {
  border-color: color-mix(in srgb, var(--color-accent) 65%, transparent);
  box-shadow: var(--shadow-sm);
  background: color-mix(in srgb, var(--color-accent) 12%, var(--color-surface) 88%);
}

.list-item .title {
  font-weight: 600;
  color: var(--color-text-strong);
}

.list-item .address {
  font-size: 0.9rem;
  color: var(--color-text-soft);
}

.list-item .meta {
  font-size: 0.85rem;
  color: var(--color-accent);
}

.list-item .action {
  margin-top: 0.1rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

@media (max-width: 960px) {
  .viewer-body {
    grid-template-columns: 1fr;
  }
}
</style>
