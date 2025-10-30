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

          <div class="manual-search" role="group" :aria-label="t('locationViewer.search.label')">
            <label class="manual-search-label" :for="`map-search-input-${activeHouse.key}`">
              {{ t('locationViewer.search.label') }}
            </label>
            <div class="manual-search-controls">
              <input
                :id="`map-search-input-${activeHouse.key}`"
                v-model="manualSearchQuery"
                type="text"
                :placeholder="t('locationViewer.search.placeholder')"
                :disabled="manualSearchLoading"
                @keyup.enter="handleManualSearch"
              />
              <button
                type="button"
                :disabled="manualSearchLoading"
                @click="handleManualSearch"
              >
                {{ manualSearchLoading ? t('locationViewer.search.searching') : t('locationViewer.search.action') }}
              </button>
            </div>
            <p class="manual-search-hint">{{ t('locationViewer.search.hint') }}</p>
            <p
              v-if="manualSearchMessage"
              :class="['manual-search-status', manualSearchMessageType]"
              role="status"
            >
              {{ manualSearchMessage }}
            </p>
            <div v-if="manualSearchSuggestions.length" class="manual-search-suggestions">
              <p class="manual-search-suggestions-title">{{ t('locationViewer.search.suggestionsTitle') }}</p>
              <ul>
                <li
                  v-for="suggestion in manualSearchSuggestions"
                  :key="suggestion.id"
                  class="manual-search-suggestion"
                >
                  <button
                    type="button"
                    class="suggestion-action"
                    :disabled="manualSearchLoading || mapLoading"
                    @click="applyManualSuggestion(suggestion)"
                  >
                    {{ t('locationViewer.search.suggestionsApply') }}
                  </button>
                  <div class="suggestion-text">
                    <p class="name">{{ suggestion.name }}</p>
                    <p class="address">{{ suggestion.address }}</p>
                  </div>
                </li>
              </ul>
            </div>
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

const isWithinChina = (lat, lng) =>
  Number.isFinite(lat) &&
  Number.isFinite(lng) &&
  lat >= CHINA_BOUNDS.south &&
  lat <= CHINA_BOUNDS.north &&
  lng >= CHINA_BOUNDS.west &&
  lng <= CHINA_BOUNDS.east;

const items = computed(() => {
  if (!Array.isArray(props.houses)) {
    return [];
  }
  return props.houses
    .map((house) => ({
      id: house?.id ?? null,
      key: house?.id != null ? String(house.id) : String(house?.key ?? ''),
      title: normalizeTitle(house?.title),
      rawTitle: typeof house?.title === 'string' ? house.title : '',
      address: normalizeAddress(house?.address),
      rawAddress: typeof house?.address === 'string' ? house.address : '',
      priceLabel: formatPriceLabel(house?.price),
      latitude: sanitizeCoordinate(house?.latitude, CHINA_BOUNDS.south, CHINA_BOUNDS.north),
      longitude: sanitizeCoordinate(house?.longitude, CHINA_BOUNDS.west, CHINA_BOUNDS.east)
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
const manualSearchQuery = ref('');
const manualSearchMessage = ref('');
const manualSearchMessageType = ref('info');
const manualSearchLoading = ref(false);
const manualSearchSuggestions = ref([]);

const CHINA_BOUNDS = Object.freeze({
  south: 17.0,
  west: 73.0,
  north: 54.5,
  east: 136.5
});

const MUNICIPALITY_PATTERN = /(北京|上海|天津|重庆)/gu;
const CITY_PATTERN = /([\p{Script=Han}]{2,6}市)/gu;
const COUNTY_PATTERN = /([\p{Script=Han}]{2,6}(?:县|区|旗))/gu;

const findLastMatch = (pattern, input) => {
  if (!(pattern instanceof RegExp) || typeof input !== 'string') {
    return '';
  }
  let match;
  let last = '';
  pattern.lastIndex = 0;
  while ((match = pattern.exec(input)) !== null) {
    last = typeof match[1] === 'string' && match[1] ? match[1] : match[0] ?? '';
  }
  pattern.lastIndex = 0;
  return last;
};

const sanitizeAdministrativeNameHint = (value) => {
  if (typeof value !== 'string') {
    return '';
  }
  return value.replace(/(自治州|地区|盟)/gu, '').trim();
};

const resolveCityHintFromText = (text) => {
  if (typeof text !== 'string') {
    return '';
  }
  const normalized = text.replace(/\s+/gu, '').trim();
  if (!normalized) {
    return '';
  }
  const municipality = findLastMatch(MUNICIPALITY_PATTERN, normalized);
  if (municipality) {
    return `${municipality}市`;
  }
  const city = findLastMatch(CITY_PATTERN, normalized);
  if (city) {
    return sanitizeAdministrativeNameHint(city);
  }
  const county = findLastMatch(COUNTY_PATTERN, normalized);
  if (county) {
    return sanitizeAdministrativeNameHint(county);
  }
  return '';
};

const mapConfig = ref({ apiKey: '', jsSecurityCode: '' });
let mapConfigPromise = null;
let mapConfigFetched = false;
let mapScriptPromise = null;
let mapInstance = null;
let mapMarker = null;
let mapBounds = null;
let locateSequence = 0;

const defaultCenter = { lat: 35.86166, lng: 104.195397 };
const defaultZoom = 5;
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

watch(
  normalizedApiBaseUrl,
  () => {
    mapConfigFetched = false;
    mapConfigPromise = null;
    mapConfig.value = { apiKey: '', jsSecurityCode: '' };
  },
  { immediate: false }
);

const ensureMapConfig = async () => {
  if (mapConfigFetched && mapConfig.value.apiKey) {
    return mapConfig.value;
  }
  if (mapConfigPromise) {
    return mapConfigPromise;
  }
  const base = normalizedApiBaseUrl.value;
  if (!base) {
    mapConfigFetched = true;
    mapConfig.value = { apiKey: '', jsSecurityCode: '' };
    return mapConfig.value;
  }
  mapConfigPromise = (async () => {
    try {
      const response = await fetch(`${base}/houses/map-config`);
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const payload = await response.json();
      mapConfig.value = {
        apiKey: typeof payload?.apiKey === 'string' ? payload.apiKey.trim() : '',
        jsSecurityCode: typeof payload?.jsSecurityCode === 'string' ? payload.jsSecurityCode.trim() : ''
      };
      mapConfigFetched = true;
    } catch (error) {
      console.warn('Failed to load Gaode map config', error);
      mapConfig.value = { apiKey: '', jsSecurityCode: '' };
      mapConfigFetched = false;
    }
    return mapConfig.value;
  })();
  try {
    return await mapConfigPromise;
  } finally {
    mapConfigPromise = null;
  }
};

const loadGaodeLibrary = async () => {
  if (typeof window === 'undefined') {
    throw new Error('Gaode map is unavailable in this environment');
  }
  if (window.AMap) {
    return window.AMap;
  }
  const config = await ensureMapConfig();
  if (!config.apiKey) {
    throw new Error('Missing Gaode map API key');
  }
  if (!mapScriptPromise) {
    if (config.jsSecurityCode) {
      window._AMapSecurityConfig = { securityJsCode: config.jsSecurityCode };
    }
    mapScriptPromise = new Promise((resolve, reject) => {
      const existingScript = document.querySelector('script[data-amap-sdk]');
      const handleLoad = () => {
        if (window.AMap) {
          resolve(window.AMap);
        } else {
          reject(new Error('Gaode map script loaded without global AMap'));
        }
      };
      const handleError = () => {
        mapScriptPromise = null;
        reject(new Error('Failed to load Gaode map script'));
      };
      if (existingScript) {
        existingScript.addEventListener('load', handleLoad, { once: true });
        existingScript.addEventListener('error', handleError, { once: true });
        return;
      }
      const script = document.createElement('script');
      script.setAttribute('data-amap-sdk', 'true');
      script.async = true;
      script.src = `https://webapi.amap.com/maps?v=2.0&key=${config.apiKey}&plugin=AMap.Scale,AMap.ToolBar,AMap.ControlBar,AMap.Geocoder,AMap.PlaceSearch`;
      script.onload = handleLoad;
      script.onerror = handleError;
      document.head.appendChild(script);
    });
  }
  return mapScriptPromise;
};

const ensureMapReady = async () => {
  await nextTick();
  if (mapInstance) {
    mapInstance.resize?.();
    return true;
  }
  if (!mapContainerRef.value) {
    return false;
  }
  try {
    const AMap = await loadGaodeLibrary();
    mapInstance = new AMap.Map(mapContainerRef.value, {
      viewMode: '2D',
      zoom: defaultZoom,
      center: [defaultCenter.lng, defaultCenter.lat],
      resizeEnable: true,
      animateEnable: true,
      mapStyle: 'amap://styles/whitesmoke'
    });
    try {
      mapInstance.setStatus?.({
        doubleClickZoom: true,
        dragEnable: true,
        keyboardEnable: false,
        scrollWheel: true,
        touchZoom: true
      });
    } catch (error) {
      console.debug('Failed to configure Gaode map status', error);
    }
    try {
      mapBounds = new AMap.Bounds(
        [CHINA_BOUNDS.west, CHINA_BOUNDS.south],
        [CHINA_BOUNDS.east, CHINA_BOUNDS.north]
      );
      mapInstance.setLimitBounds?.(mapBounds);
    } catch (error) {
      console.debug('Failed to set Gaode map bounds', error);
    }
    try {
      mapInstance.addControl?.(new AMap.Scale());
      mapInstance.addControl?.(new AMap.ToolBar());
    } catch (error) {
      console.debug('Failed to add Gaode map controls', error);
    }
    mapInstance.on?.('complete', () => {
      mapInstance?.resize?.();
    });
    return true;
  } catch (error) {
    console.warn('Failed to initialise Gaode map', error);
    mapError.value = t('locationViewer.map.error');
    return false;
  }
};

const resetMapView = async () => {
  const ready = await ensureMapReady();
  if (!ready || !mapInstance) {
    return;
  }
  if (mapMarker) {
    mapMarker.setMap(null);
    mapMarker = null;
  }
  mapInstance.setZoomAndCenter?.(defaultZoom, [defaultCenter.lng, defaultCenter.lat]);
  mapInstance.setCenter?.([defaultCenter.lng, defaultCenter.lat]);
  mapInstance.resize?.();
};

const fallbackTitle = (house, override) => {
  if (typeof override === 'string' && override.trim()) {
    return override.trim();
  }
  const raw = typeof house?.rawTitle === 'string' ? house.rawTitle.trim() : '';
  if (raw) {
    return raw;
  }
  const normalized = typeof house?.title === 'string' ? house.title.trim() : '';
  if (normalized) {
    return normalized;
  }
  return t('locationViewer.noTitle');
};

const fallbackAddress = (house, override) => {
  if (typeof override === 'string' && override.trim()) {
    return override.trim();
  }
  const raw = typeof house?.rawAddress === 'string' ? house.rawAddress.trim() : '';
  if (raw) {
    return raw;
  }
  const normalized = typeof house?.address === 'string' ? house.address.trim() : '';
  if (normalized) {
    return normalized;
  }
  return t('locationViewer.noAddress');
};

const deriveHouseAddress = (house) => {
  const raw = typeof house?.rawAddress === 'string' ? house.rawAddress.trim() : '';
  if (raw) {
    return raw;
  }
  const normalized = typeof house?.address === 'string' ? house.address.trim() : '';
  return normalized;
};

const resolveCityHintForHouse = (house) => {
  const address = deriveHouseAddress(house);
  return resolveCityHintFromText(address);
};

const resolveCityHintForQuery = (query, house) => {
  const queryHint = resolveCityHintFromText(query);
  if (queryHint) {
    return queryHint;
  }
  return resolveCityHintForHouse(house);
};

const buildLocationEntry = (house, lat, lng, name, address, signatureOverride) => {
  const resolvedName = fallbackTitle(house, name);
  const resolvedAddress = fallbackAddress(house, address);
  const signature = signatureOverride ?? normalizeSignature(house?.rawAddress ?? resolvedAddress ?? resolvedName);
  return {
    lat,
    lng,
    name: resolvedName,
    address: resolvedAddress,
    sourceAddress: signature
  };
};

const setMapToCoordinates = async (entry) => {
  const ready = await ensureMapReady();
  if (!ready || !mapInstance || typeof window === 'undefined' || !window.AMap) {
    return;
  }
  const position = [entry.lng, entry.lat];
  if (!mapMarker) {
    mapMarker = new window.AMap.Marker({
      position,
      anchor: 'bottom-center'
    });
  } else {
    mapMarker.setPosition(position);
  }
  mapMarker.setMap(mapInstance);
  mapInstance.setZoomAndCenter?.(focusZoom, position);
  mapInstance.panTo?.(position);
  mapInstance.resize?.();
};

const requestMapLocation = async (query, { house = null, city, signatureOverride, fallbackName } = {}) => {
  const base = normalizedApiBaseUrl.value;
  if (!base) {
    return { status: 'error', suggestions: [] };
  }
  const trimmed = typeof query === 'string' ? query.trim() : '';
  if (!trimmed) {
    return { status: 'error', suggestions: [] };
  }
  const params = new URLSearchParams({ query: trimmed });
  const explicitCity = typeof city === 'string' ? city.trim() : '';
  const fallbackCity = explicitCity || resolveCityHintForHouse(house);
  if (fallbackCity) {
    params.append('city', fallbackCity);
  }
  if (house?.id != null) {
    params.append('houseId', String(house.id));
  }
  try {
    const response = await fetch(`${base}/houses/map-search?${params.toString()}`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const payload = await response.json();
    const rawSuggestions = Array.isArray(payload?.suggestions) ? payload.suggestions : [];
    const seenSuggestionKeys = new Set();
    const suggestions = rawSuggestions
      .map((item, index) => {
        const lat = sanitizeCoordinate(item?.latitude, CHINA_BOUNDS.south, CHINA_BOUNDS.north);
        const lng = sanitizeCoordinate(item?.longitude, CHINA_BOUNDS.west, CHINA_BOUNDS.east);
        if (lat == null || lng == null || !isWithinChina(lat, lng)) {
          return null;
        }
        const name = typeof item?.name === 'string' && item.name.trim() ? item.name.trim() : fallbackName ?? trimmed;
        const address =
          typeof item?.address === 'string' && item.address.trim() ? item.address.trim() : fallbackName ?? trimmed;
        const key = `${lat.toFixed(6)},${lng.toFixed(6)}`;
        if (seenSuggestionKeys.has(key)) {
          return null;
        }
        seenSuggestionKeys.add(key);
        const identifier = `${key}::${index.toString(36)}::${normalizeSignature(address) || normalizeSignature(name)}`;
        return {
          id: identifier,
          name,
          address,
          lat,
          lng
        };
      })
      .filter(Boolean);
    if (
      payload?.found &&
      Number.isFinite(payload.latitude) &&
      Number.isFinite(payload.longitude)
    ) {
      const lat = sanitizeCoordinate(payload.latitude, CHINA_BOUNDS.south, CHINA_BOUNDS.north);
      const lng = sanitizeCoordinate(payload.longitude, CHINA_BOUNDS.west, CHINA_BOUNDS.east);
      if (lat != null && lng != null && isWithinChina(lat, lng)) {
        const entry = buildLocationEntry(
          house,
          lat,
          lng,
          payload.name ?? fallbackName ?? trimmed,
          payload.address ?? fallbackName ?? trimmed,
          signatureOverride
        );
        return { status: 'success', entry, suggestions };
      }
    }
    if (suggestions.length) {
      return { status: 'suggestions', suggestions };
    }
    return { status: 'not-found', suggestions: [] };
  } catch (error) {
    console.warn('Failed to request Gaode location', error);
    return { status: 'error', suggestions: [] };
  }
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
    if (isWithinChina(cached.lat, cached.lng)) {
      activeLocation.value = cached;
      await setMapToCoordinates(cached);
      return;
    }
    coordinateCache.delete(house.key);
  }

  if (house.latitude != null && house.longitude != null) {
    const lat = sanitizeCoordinate(house.latitude, CHINA_BOUNDS.south, CHINA_BOUNDS.north);
    const lng = sanitizeCoordinate(house.longitude, CHINA_BOUNDS.west, CHINA_BOUNDS.east);
    if (lat != null && lng != null && isWithinChina(lat, lng)) {
      const entry = buildLocationEntry(house, lat, lng, house.rawTitle, house.rawAddress);
      coordinateCache.set(house.key, entry);
      activeLocation.value = entry;
      await setMapToCoordinates(entry);
      return;
    }
  }

  const address = house.rawAddress?.trim();
  if (!address) {
    mapError.value = t('locationViewer.map.addressMissing');
    await resetMapView();
    return;
  }

  mapLoading.value = true;
  try {
    const { status, entry } = await requestMapLocation(address, {
      house,
      city: resolveCityHintForHouse(house),
      signatureOverride: signature,
      fallbackName: address
    });
    if (token !== locateSequence) {
      return;
    }
    if (status === 'success' && entry) {
      coordinateCache.set(house.key, entry);
      activeLocation.value = entry;
      await setMapToCoordinates(entry);
      return;
    }
    if (status === 'not-found') {
      mapError.value = t('locationViewer.map.noResult');
    } else {
      mapError.value = t('locationViewer.map.error');
    }
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
  (house) => {
    manualSearchQuery.value = deriveHouseAddress(house) || '';
    manualSearchMessage.value = '';
    manualSearchMessageType.value = 'info';
    manualSearchSuggestions.value = [];
  },
  { immediate: true }
);

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

const handleManualSearch = async () => {
  const query = typeof manualSearchQuery.value === 'string' ? manualSearchQuery.value.trim() : '';
  manualSearchMessage.value = '';
  manualSearchMessageType.value = 'info';
  manualSearchSuggestions.value = [];
  if (!query) {
    manualSearchMessage.value = t('locationViewer.search.required');
    manualSearchMessageType.value = 'error';
    return;
  }
  locateSequence += 1;
  const token = locateSequence;
  mapError.value = '';
  manualSearchLoading.value = true;
  mapLoading.value = true;
  try {
    const house = activeHouse.value ?? null;
    const houseSignature = normalizeSignature(house?.rawAddress ?? deriveHouseAddress(house) ?? '');
    const querySignature = normalizeSignature(query);
    const shouldPersist = Boolean(house) && querySignature && houseSignature && querySignature === houseSignature;
    const signatureOverride = shouldPersist && houseSignature
      ? houseSignature
      : `manual::${querySignature || Date.now().toString(36)}`;
    const { status, entry, suggestions } = await requestMapLocation(query, {
      house,
      city: resolveCityHintForQuery(query, house),
      signatureOverride,
      fallbackName: query
    });
    if (token !== locateSequence) {
      return;
    }
    manualSearchSuggestions.value = Array.isArray(suggestions) ? suggestions.slice(0, 8) : [];
    if (status === 'success' && entry) {
      activeLocation.value = entry;
      await setMapToCoordinates(entry);
      if (shouldPersist && house) {
        coordinateCache.set(house.key, entry);
      }
      manualSearchMessage.value = t('locationViewer.search.success', { name: entry.name });
      manualSearchMessageType.value = 'success';
      mapError.value = '';
      return;
    }
    if (status === 'suggestions' && manualSearchSuggestions.value.length) {
      manualSearchMessage.value = t('locationViewer.search.suggestions');
      manualSearchMessageType.value = 'info';
      mapError.value = t('locationViewer.map.noResult');
      await resetMapView();
      return;
    }
    manualSearchMessageType.value = 'error';
    if (status === 'not-found') {
      manualSearchMessage.value = t('locationViewer.search.failure');
      mapError.value = t('locationViewer.map.noResult');
    } else {
      manualSearchMessage.value = t('locationViewer.search.error');
      mapError.value = t('locationViewer.map.error');
    }
    await resetMapView();
  } catch (error) {
    if (token === locateSequence) {
      console.warn('Manual map search failed', error);
      manualSearchMessage.value = t('locationViewer.search.error');
      manualSearchMessageType.value = 'error';
      mapError.value = t('locationViewer.map.error');
      await resetMapView();
    }
  } finally {
    manualSearchLoading.value = false;
    if (token === locateSequence) {
      mapLoading.value = false;
    }
  }
};

const applyManualSuggestion = async (suggestion) => {
  if (!suggestion) {
    return;
  }
  const lat = sanitizeCoordinate(suggestion.lat, CHINA_BOUNDS.south, CHINA_BOUNDS.north);
  const lng = sanitizeCoordinate(suggestion.lng, CHINA_BOUNDS.west, CHINA_BOUNDS.east);
  if (lat == null || lng == null || !isWithinChina(lat, lng)) {
    return;
  }
  locateSequence += 1;
  const token = locateSequence;
  mapError.value = '';
  manualSearchMessageType.value = 'info';
  mapLoading.value = true;
  try {
    const house = activeHouse.value ?? null;
    const houseSignature = normalizeSignature(house?.rawAddress ?? deriveHouseAddress(house) ?? '');
    const fallbackSignature = normalizeSignature(suggestion.address ?? suggestion.name ?? '');
    const signatureOverride = houseSignature || (fallbackSignature ? `suggestion::${fallbackSignature}` : undefined);
    const entry = buildLocationEntry(
      house,
      lat,
      lng,
      suggestion.name,
      suggestion.address,
      signatureOverride
    );
    activeLocation.value = entry;
    await setMapToCoordinates(entry);
    if (token !== locateSequence) {
      return;
    }
    manualSearchMessage.value = t('locationViewer.search.appliedSuggestion', { name: entry.name });
    manualSearchMessageType.value = 'success';
    manualSearchQuery.value = entry.address;
    mapError.value = '';
    if (house) {
      const storedSource = houseSignature || entry.sourceAddress;
      coordinateCache.set(house.key, { ...entry, sourceAddress: storedSource });
    }
  } catch (error) {
    if (token === locateSequence) {
      console.warn('Failed to apply manual suggestion', error);
      manualSearchMessage.value = t('locationViewer.search.error');
      manualSearchMessageType.value = 'error';
      mapError.value = t('locationViewer.map.error');
      await resetMapView();
    }
  } finally {
    if (token === locateSequence) {
      mapLoading.value = false;
    }
  }
};

const handleRefresh = () => {
  copyStatus.value = '';
  copyStatusType.value = '';
  mapError.value = '';
  manualSearchMessage.value = '';
  manualSearchMessageType.value = 'info';
  manualSearchSuggestions.value = [];
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
  if (mapMarker) {
    mapMarker.setMap(null);
    mapMarker = null;
  }
  if (mapInstance?.destroy) {
    mapInstance.destroy();
  }
  mapInstance = null;
  mapBounds = null;
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

.manual-search {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
  background: color-mix(in srgb, var(--color-surface) 85%, transparent);
  padding: 0.95rem 1.1rem;
}

.manual-search-label {
  font-weight: 600;
  color: var(--color-text-strong);
}

.manual-search-controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.manual-search-controls input {
  flex: 1 1 240px;
  border-radius: var(--radius-pill);
  border: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
  background: color-mix(in srgb, var(--color-surface) 92%, transparent);
  padding: 0.55rem 1rem;
  font-size: 0.95rem;
  color: var(--color-text-strong);
  min-width: 0;
  outline: none;
  transition: border 0.2s ease, box-shadow 0.2s ease;
}

.manual-search-controls input:focus {
  border-color: color-mix(in srgb, var(--color-primary) 70%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-primary) 15%, transparent);
}

.manual-search-controls button {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.55rem 1.4rem;
  background: var(--gradient-primary);
  color: var(--color-text-on-emphasis);
  font-weight: 600;
  box-shadow: var(--button-primary-shadow);
  cursor: pointer;
}

.manual-search-controls button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.manual-search-hint {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.manual-search-status {
  margin: 0;
  font-size: 0.9rem;
}

.manual-search-status.success {
  color: var(--color-success);
}

.manual-search-status.error {
  color: var(--color-danger);
}

.manual-search-suggestions {
  margin-top: 0.75rem;
  padding: 0.75rem;
  border-radius: var(--radius-md);
  border: 1px solid color-mix(in srgb, var(--color-border) 85%, transparent);
  background: color-mix(in srgb, var(--color-surface) 80%, transparent 20%);
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.manual-search-suggestions-title {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-muted);
  font-weight: 600;
}

.manual-search-suggestions ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.manual-search-suggestion {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
}

.suggestion-action {
  border: 1px solid color-mix(in srgb, var(--color-primary) 70%, transparent);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--color-primary) 10%, transparent 90%);
  color: var(--color-primary);
  font-weight: 600;
  padding: 0.35rem 0.95rem;
  cursor: pointer;
  white-space: nowrap;
}

.suggestion-action:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.manual-search-suggestion .suggestion-text {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.manual-search-suggestion .name {
  margin: 0;
  font-weight: 600;
  color: var(--color-text-strong);
}

.manual-search-suggestion .address {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-muted);
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
