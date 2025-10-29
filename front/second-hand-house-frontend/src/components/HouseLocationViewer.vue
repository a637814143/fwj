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
import 'leaflet/dist/leaflet.css';

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
const activeMapProvider = ref('');

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

const cacheKey = (address, provider) => `${provider ?? 'auto'}::${address}`;

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

const geocodeWithTencent = async (address) => {
  if (!mapKey) {
    return null;
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
  try {
    const response = await fetch(`https://apis.map.qq.com/ws/geocoder/v1/?${params.toString()}`);
    if (!response.ok) {
      throw new Error('geocoder-http');
    }
    const payload = await response.json();
    const location = payload?.result?.location;
    if (payload?.status === 0 && location) {
      const coords = {
        lat: Number(location.lat),
        lng: Number(location.lng)
      };
      if (Number.isFinite(coords.lat) && Number.isFinite(coords.lng)) {
        return coords;
      }
    }
  } catch (error) {
    console.warn('Tencent geocoder failed', error);
  }
  return null;
};

const geocodeWithOsm = async (address) => {
  const params = new URLSearchParams({
    q: address,
    format: 'json',
    limit: '1',
    addressdetails: '0'
  });
  try {
    const response = await fetch(`https://nominatim.openstreetmap.org/search?${params.toString()}`, {
      headers: {
        Accept: 'application/json',
        'Accept-Language': settings?.language === 'en' ? 'en' : 'zh-CN'
      }
    });
    if (!response.ok) {
      throw new Error('osm-geocoder-http');
    }
    const payload = await response.json();
    if (Array.isArray(payload) && payload.length > 0) {
      const first = payload[0];
      const coords = {
        lat: Number(first.lat),
        lng: Number(first.lon)
      };
      if (Number.isFinite(coords.lat) && Number.isFinite(coords.lng)) {
        return coords;
      }
    }
  } catch (error) {
    console.warn('OpenStreetMap geocoder failed', error);
  }
  return null;
};

const geocodeHouse = async (house, providerHint = '') => {
  if (!house) {
    return null;
  }
  const latitudeFields = ['latitude', 'lat'];
  const longitudeFields = ['longitude', 'lng'];
  let lat = null;
  let lng = null;
  for (const field of latitudeFields) {
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
  const provider = providerHint || (mapKey ? 'tencent' : 'osm');
  const key = cacheKey(address, provider);
  if (geocodeCache.has(key)) {
    return geocodeCache.get(key);
  }
  if (pendingGeocodes.has(key)) {
    return pendingGeocodes.get(key);
  }

  const perform = async () => {
    let coords = null;
    if (provider === 'tencent') {
      coords = await geocodeWithTencent(address);
      if (!coords) {
        coords = await geocodeWithOsm(address);
      }
    } else {
      coords = await geocodeWithOsm(address);
      if (!coords && mapKey) {
        coords = await geocodeWithTencent(address);
      }
    }
    if (coords) {
      geocodeCache.set(key, coords);
    }
    return coords;
  };

  const request = perform().finally(() => {
    pendingGeocodes.delete(key);
  });
  pendingGeocodes.set(key, request);
  return request;
};

const markerIcon = (fill, stroke) => {
  const svg = `<?xml version="1.0" encoding="UTF-8"?>
  <svg xmlns="http://www.w3.org/2000/svg" width="36" height="48" viewBox="0 0 36 48">
    <path d="M18 1C9.163 1 2 8.163 2 17c0 12.188 16 30 16 30s16-17.812 16-30C34 8.163 26.837 1 18 1z" fill="${fill}" stroke="${stroke}" stroke-width="2"/>
    <circle cx="18" cy="18" r="6" fill="#ffffff"/>
  </svg>`;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
};

let tencentNamespace = null;
let scriptPromise = null;
let mapInstance = null;
let markerLayer = null;
let leafletModule = null;
let leafletMap = null;
let leafletMarkerLayer = null;
let updateToken = 0;
let latestGeometries = [];

const resetTencentMap = () => {
  if (markerLayer && typeof markerLayer.setGeometries === 'function') {
    markerLayer.setGeometries([]);
  }
  markerLayer = null;
  if (mapInstance && typeof mapInstance.destroy === 'function') {
    mapInstance.destroy();
  }
  mapInstance = null;
  tencentNamespace = null;
};

const resetLeafletMap = () => {
  if (leafletMarkerLayer && typeof leafletMarkerLayer.clearLayers === 'function') {
    leafletMarkerLayer.clearLayers();
  }
  leafletMarkerLayer = null;
  if (leafletMap && typeof leafletMap.remove === 'function') {
    leafletMap.remove();
  }
  leafletMap = null;
};

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

const ensureTencentMap = async () => {
  const TMap = await loadTencentMapScript();
  if (!mapContainer.value) {
    throw new Error('container-missing');
  }
  if (!mapInstance) {
    mapInstance = new TMap.Map(mapContainer.value, {
      center: new TMap.LatLng(39.908823, 116.39747),
      zoom: 12,
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
  tencentNamespace = TMap;
  mapReady.value = true;
  activeMapProvider.value = 'tencent';
  return 'tencent';
};

const ensureLeafletMap = async () => {
  if (!mapContainer.value) {
    throw new Error('container-missing');
  }
  if (!leafletModule) {
    leafletModule = await import('leaflet');
  }
  const { default: L } = leafletModule;
  if (!leafletMap) {
    mapContainer.value.innerHTML = '';
    leafletMap = L.map(mapContainer.value, {
      center: [39.908823, 116.39747],
      zoom: 12,
      zoomControl: true
    });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(leafletMap);
    leafletMarkerLayer = L.layerGroup().addTo(leafletMap);
  }
  mapReady.value = true;
  activeMapProvider.value = 'leaflet';
  return 'leaflet';
};

const ensureMap = async () => {
  if (!mapContainer.value) {
    throw new Error('container-missing');
  }
  if (activeMapProvider.value === 'tencent' && mapInstance) {
    return 'tencent';
  }
  if (activeMapProvider.value === 'leaflet' && leafletMap) {
    return 'leaflet';
  }
  if (mapKey) {
    try {
      return await ensureTencentMap();
    } catch (error) {
      console.warn('Tencent map unavailable, switching to Leaflet fallback', error);
      resetTencentMap();
    }
  }
  return ensureLeafletMap();
};

const focusOnActiveGeometry = () => {
  if (!latestGeometries.length) {
    return;
  }
  const active =
    latestGeometries.find((geometry) => geometry.id === activeHouseId.value) ||
    latestGeometries[0];
  if (!active) {
    return;
  }
  try {
    if (activeMapProvider.value === 'tencent' && mapInstance && tencentNamespace) {
      const position = new tencentNamespace.LatLng(active.coords.lat, active.coords.lng);
      mapInstance.setCenter(position);
      if (typeof mapInstance.getZoom === 'function' && typeof mapInstance.setZoom === 'function') {
        const currentZoom = mapInstance.getZoom();
        if (!currentZoom || currentZoom < 15) {
          mapInstance.setZoom(15);
        }
      }
    } else if (activeMapProvider.value === 'leaflet' && leafletMap && leafletModule) {
      leafletMap.setView([active.coords.lat, active.coords.lng], Math.max(leafletMap.getZoom() ?? 13, 15), {
        animate: true
      });
    }
  } catch (error) {
    console.warn('Failed to focus map marker', error);
  }
};

const openExternalMap = (geometry) => {
  if (typeof window === 'undefined' || !geometry?.coords) {
    return;
  }
  const { coords, title, address } = geometry;
  let url = '';
  if (activeMapProvider.value === 'tencent' && mapKey) {
    const marker = `coord:${coords.lat},${coords.lng};title:${encodeURIComponent(title ?? '')};addr:${encodeURIComponent(address ?? '')}`;
    url = `https://map.qq.com/?type=marker&isopeninfowin=1&markertype=1&marker=${marker}&zoom=18`;
  } else {
    url = `https://www.openstreetmap.org/?mlat=${coords.lat}&mlon=${coords.lng}#map=18/${coords.lat}/${coords.lng}`;
  }
  window.open(url, '_blank', 'noopener');
};

const openExternalMapIfReady = () => {
  if (!pendingNavigationKey.value) {
    return;
  }
  const key = pendingNavigationKey.value;
  const geometry = latestGeometries.find((item) => item.id === key);
  if (!geometry || !geometry.coords) {
    return;
  }
  pendingNavigationKey.value = null;
  openExternalMap(geometry);
};

const renderMarkers = (provider) => {
  if (!latestGeometries.length) {
    return;
  }
  if (provider === 'tencent' && markerLayer && tencentNamespace) {
    const geometries = latestGeometries.map((geometry) => ({
      id: geometry.id,
      position: new tencentNamespace.LatLng(geometry.coords.lat, geometry.coords.lng),
      styleId: geometry.id === activeHouseId.value ? 'active' : 'default',
      properties: {
        title: geometry.title,
        address: geometry.address,
        price: geometry.price,
        coords: geometry.coords
      }
    }));
    markerLayer.setGeometries(geometries);
  } else if (provider === 'leaflet' && leafletMarkerLayer && leafletModule) {
    const { default: L } = leafletModule;
    leafletMarkerLayer.clearLayers();
    latestGeometries.forEach((geometry) => {
      const isActive = geometry.id === activeHouseId.value;
      const marker = L.circleMarker([geometry.coords.lat, geometry.coords.lng], {
        radius: isActive ? 8 : 6,
        color: isActive ? '#d28a7c' : '#47506c',
        weight: 2,
        fillColor: isActive ? '#f1b6a9' : '#7c88aa',
        fillOpacity: 0.85
      });
      marker.bindPopup(
        `<strong>${geometry.title}</strong><br />${geometry.address}${
          geometry.price ? `<br />${geometry.price}` : ''
        }`
      );
      marker.on('click', () => {
        selectHouse(geometry.id);
      });
      marker.addTo(leafletMarkerLayer);
      if (isActive) {
        marker.openPopup();
      }
    });
  }
  focusOnActiveGeometry();
  openExternalMapIfReady();
};

const updateMapMarkers = async () => {
  if (!isMounted.value || props.loading) {
    return;
  }
  if (!locationItems.value.length) {
    latestGeometries = [];
    geocodeWarnings.value = [];
    if (markerLayer) {
      markerLayer.setGeometries([]);
    }
    if (leafletMarkerLayer) {
      leafletMarkerLayer.clearLayers();
    }
    return;
  }
  if (typeof window === 'undefined') {
    return;
  }
  mapBusy.value = true;
  mapError.value = '';
  const token = ++updateToken;
  let provider = activeMapProvider.value;
  try {
    provider = await ensureMap();
  } catch (error) {
    console.warn('Map initialisation failed, attempting Leaflet fallback', error);
    try {
      provider = await ensureLeafletMap();
    } catch (leafletError) {
      console.error('Failed to initialise any map provider', leafletError);
      mapError.value = t('locationViewer.errors.mapInit');
      return;
    }
  }

  try {
    const results = await Promise.all(
      locationItems.value.map((item) => geocodeHouse(item.house, provider))
    );
    if (token !== updateToken) {
      return;
    }
    const warnings = [];
    const geometries = [];
    results.forEach((coords, index) => {
      const item = locationItems.value[index];
      if (!coords) {
        warnings.push(item);
        return;
      }
      geometries.push({
        id: item.key,
        title: item.title,
        address: item.address,
        price: item.priceLabel,
        coords
      });
    });
    latestGeometries = geometries;
    geocodeWarnings.value = warnings;
    if (!geometries.length) {
      mapError.value = warnings.length
        ? t('locationViewer.errors.geocode')
        : t('locationViewer.errors.mapInit');
      return;
    }
    renderMarkers(provider);
  } catch (error) {
    console.error('Failed to update map markers', error);
    if (provider === 'tencent') {
      try {
        await ensureLeafletMap();
        renderMarkers('leaflet');
        mapError.value = '';
      } catch (fallbackError) {
        console.error('Leaflet fallback failed', fallbackError);
        mapError.value = t('locationViewer.errors.mapInit');
      }
    } else {
      mapError.value = t('locationViewer.errors.mapInit');
    }
  } finally {
    if (token === updateToken) {
      mapBusy.value = false;
    }
  }
};

const highlightActiveMarker = () => {
  if (!mapReady.value || !latestGeometries.length) {
    return;
  }
  try {
    renderMarkers(activeMapProvider.value);
  } catch (error) {
    console.warn('Failed to highlight marker on current provider', error);
    if (activeMapProvider.value === 'tencent') {
      ensureLeafletMap()
        .then(() => renderMarkers('leaflet'))
        .catch((fallbackError) => {
          console.error('Leaflet highlight fallback failed', fallbackError);
        });
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
  resetTencentMap();
  resetLeafletMap();
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
