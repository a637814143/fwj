<template>
  <section class="location-viewer surface-card">
    <header class="viewer-header">
      <div>
        <h2>{{ t('locationViewer.title') }}</h2>
        <p>{{ t('locationViewer.subtitle') }}</p>
      </div>
      <div class="header-actions">
        <div
          v-if="mapStyleOptions.length"
          class="style-toggle"
          role="group"
          :aria-label="t('locationViewer.styles.label')"
        >
          <button
            v-for="option in mapStyleOptions"
            :key="option.value"
            type="button"
            :class="['style-option', { active: option.active }]"
            :disabled="!option.enabled"
            @click="setMapStyle(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
        <button type="button" :disabled="loading" @click="emit('refresh')">
          {{ loading ? t('locationViewer.refreshing') : t('locationViewer.refresh') }}
        </button>
      </div>
    </header>

    <p v-if="updatedTime" class="updated-label">
      {{ t('locationViewer.updatedAt', { time: updatedTime }) }}
    </p>

    <p v-if="mapFallbackNotice" class="map-fallback">{{ mapFallbackNotice }}</p>

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

        <p v-if="focusSummary" class="focus-summary">{{ focusSummary }}</p>
        <p v-if="viewportSummary" class="viewport-summary">{{ viewportSummary }}</p>
        <p
          v-if="searchStatusMessage"
          class="search-status"
          :class="{ loading: searchLoading }"
        >
          {{ searchStatusMessage }}
        </p>
        <p
          v-if="activeSearchResult && activeSearchResult.address && !searchErrorMessage"
          class="search-status result"
        >
          {{ activeSearchResult.address }}
        </p>
        <p v-if="searchErrorMessage" class="search-status error">{{ searchErrorMessage }}</p>

        <section v-if="showPoiPanel" class="poi-panel">
          <h4>{{ t('locationViewer.pois.title') }}</h4>
          <p v-if="poiLoading" class="poi-status">{{ t('locationViewer.pois.loading') }}</p>
          <p v-else-if="poiError" class="poi-status error">{{ poiError }}</p>
          <p v-else-if="!poiListItems.length" class="poi-status">{{ t('locationViewer.pois.empty') }}</p>
          <ul v-else class="poi-list">
            <li v-for="poi in poiListItems" :key="poi.id" class="poi-item">
              <span class="poi-name">{{ poi.name }}</span>
              <span v-if="poi.typeName" class="poi-type">{{ poi.typeName }}</span>
              <span v-if="poi.distanceLabel" class="poi-distance">{{ poi.distanceLabel }}</span>
            </li>
          </ul>
        </section>

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
  mapConfig: {
    type: Object,
    default: () => ({})
  },
  apiBaseUrl: {
    type: String,
    default: ''
  },
  searchQuery: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['refresh', 'viewport-change']);

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
const mapStyle = ref('street');
const activeMapProvider = ref('');

const mapKey = ref('');
const mapSecurityCode = ref('');

const effectiveApiBaseUrl = computed(() => {
  const raw = typeof props.apiBaseUrl === 'string' ? props.apiBaseUrl.trim() : '';
  if (!raw) {
    return '';
  }
  return raw.endsWith('/') ? raw.slice(0, -1) : raw;
});

const envMapCandidates = [
  import.meta.env.VITE_GAODE_MAP_KEY,
  import.meta.env.VITE_AMAP_KEY,
  '46dff0d2a8f9204d4642f8dd91e10daf'
];

const envSecurityCandidates = [
  import.meta.env.VITE_GAODE_SECURITY_CODE,
  import.meta.env.VITE_AMAP_SECURITY_CODE
];

const pickFirstValidString = (candidates) => {
  for (const value of candidates) {
    if (typeof value === 'string') {
      const trimmed = value.trim();
      if (trimmed) {
        return trimmed;
      }
    }
  }
  return '';
};

const ensureSecurityConfig = () => {
  if (typeof window === 'undefined') {
    return;
  }
  if (mapSecurityCode.value) {
    window._AMapSecurityConfig = {
      ...(window._AMapSecurityConfig || {}),
      securityJsCode: mapSecurityCode.value
    };
  }
};

const syncMapConfiguration = () => {
  const config = props.mapConfig ?? {};
  const keyCandidates = [config.apiKey, config.key, config.mapKey, ...envMapCandidates];
  mapKey.value = pickFirstValidString(keyCandidates);
  const securityCandidates = [
    config.jsSecurityCode,
    config.securityCode,
    config.jsSecurity,
    ...envSecurityCandidates
  ];
  mapSecurityCode.value = pickFirstValidString(securityCandidates);
  ensureSecurityConfig();
};

syncMapConfiguration();

const mapStyleOptions = computed(() => [
  {
    value: 'street',
    label: t('locationViewer.styles.street'),
    active: mapStyle.value === 'street',
    enabled: mapReady.value
  },
  {
    value: 'satellite',
    label: t('locationViewer.styles.satellite'),
    active: mapStyle.value === 'satellite',
    enabled: mapReady.value
  }
]);

const mapFallbackNotice = computed(() => '');

const latestGeometries = ref([]);
const visibleGeometryCount = computed(() => latestGeometries.value.length);

const setMapStyle = (style) => {
  if (!style || mapStyle.value === style) {
    return;
  }
  mapStyle.value = style;
};

const viewportSummary = computed(() => {
  if (!visibleGeometryCount.value) {
    return '';
  }
  return t('locationViewer.viewportSummary', { count: visibleGeometryCount.value });
});

const normalizedHouses = computed(() =>
  Array.isArray(props.houses)
    ? props.houses.filter((house) => house && (house.title || house.address))
    : []
);

const activeHouseId = ref('');
const pendingFocusKey = ref('');
const searchKeyword = computed(() =>
  typeof props.searchQuery === 'string' ? props.searchQuery.trim() : ''
);
const activeSearchQuery = ref('');
const searchStatusMessage = ref('');
const searchErrorMessage = ref('');
const searchLoading = ref(false);
const activeSearchResult = ref(null);
const hasActiveSearch = computed(() => Boolean(activeSearchQuery.value));

const normalizedFocusKey = computed(() => {
  const raw = props.focusKey;
  if (raw === undefined || raw === null) {
    return '';
  }
  return String(raw).trim();
});

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

const focusSummary = computed(() => {
  if (hasActiveSearch.value) {
    return '';
  }
  if (!activeSummary.value || !activeSummary.value.title) {
    return '';
  }
  return t('locationViewer.focusSummary', { title: activeSummary.value.title });
});

watch(
  locationItems,
  (items) => {
    if (!items.length) {
      activeHouseId.value = '';
      pendingFocusKey.value = '';
      return;
    }
    const hasPending = pendingFocusKey.value
      ? items.some((item) => item.key === pendingFocusKey.value)
      : false;
    if (hasPending) {
      activeHouseId.value = pendingFocusKey.value;
      return;
    }
    const hasActive = items.some((item) => item.key === activeHouseId.value);
    if (!hasActive) {
      activeHouseId.value = items[0].key;
    }
    if (!hasActiveSearch.value) {
      nextTick(() => {
        focusOnActiveGeometry();
      });
    }
  },
  { immediate: true }
);

watch(normalizedFocusKey, (value) => {
  if (!value) {
    return;
  }
  pendingFocusKey.value = value;
  if (locationItems.value.some((item) => item.key === value)) {
    if (activeHouseId.value !== value) {
      activeHouseId.value = value;
    }
    nextTick(() => {
      if (!hasActiveSearch.value) {
        focusOnActiveGeometry();
      }
    });
  }
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
  if (key == null) {
    return;
  }
  const normalized = String(key);
  activeHouseId.value = normalized;
  pendingFocusKey.value = normalized;
  if (hasActiveSearch.value) {
    addressSearchToken += 1;
    resetSearchState({ resetQuery: true });
  }
  nextTick(() => {
    focusOnActiveGeometry();
  });
};

const showLoadingOverlay = computed(() => props.loading || mapBusy.value || !mapReady.value);

const sanitizeAdministrativeName = (value) => {
  if (!value) {
    return '';
  }
  return value.replace(/(自治州|地区|盟)/g, '').trim();
};

const extractRegion = (address) => {
  if (!address || typeof address !== 'string') {
    return '';
  }
  const text = address.replace(/\s+/g, '').trim();
  if (!text) {
    return '';
  }
  const municipalityMatches = text.match(/(北京|上海|天津|重庆)/g);
  if (municipalityMatches?.length) {
    return `${municipalityMatches[municipalityMatches.length - 1]}市`;
  }
  const cityMatches = text.match(/[\u4e00-\u9fa5]{2,6}市/g);
  if (cityMatches?.length) {
    return sanitizeAdministrativeName(cityMatches[cityMatches.length - 1]);
  }
  const countyMatches = text.match(/[\u4e00-\u9fa5]{2,6}(?:县|区|旗)/g);
  if (countyMatches?.length) {
    return sanitizeAdministrativeName(countyMatches[countyMatches.length - 1]);
  }
  const provinceMatch = text.match(/^(.*?(省|自治区|特别行政区))/);
  if (provinceMatch) {
    return sanitizeAdministrativeName(provinceMatch[1]);
  }
  return '';
};

const extractHouseCoordinates = (house) => {
  if (!house || typeof house !== 'object') {
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
  if (lat == null || lng == null) {
    return null;
  }
  return { lat, lng };
};

const haversineDistanceKm = (lat1, lng1, lat2, lng2) => {
  const earthRadiusKm = 6371.0088;
  const toRad = (value) => (value * Math.PI) / 180;
  const latRad1 = toRad(lat1);
  const latRad2 = toRad(lat2);
  const deltaLat = toRad(lat2 - lat1);
  const deltaLng = toRad(lng2 - lng1);
  const a =
    Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
    Math.cos(latRad1) * Math.cos(latRad2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return earthRadiusKm * c;
};

let scriptPromise = null;
let gaodeMap = null;
let AMapNamespace = null;
let infoWindow = null;
let defaultLayer = null;
let satelliteLayer = null;
let roadNetLayer = null;
let resizeObserver = null;
let updateToken = 0;
let lastViewportPayload = null;

const circleStyles = {
  default: {
    radius: 7,
    strokeColor: '#47506c',
    strokeWeight: 2,
    strokeOpacity: 0.9,
    fillColor: '#7c88aa',
    fillOpacity: 0.85,
    zIndex: 60
  },
  active: {
    radius: 10,
    strokeColor: '#c2553a',
    strokeWeight: 3,
    strokeOpacity: 1,
    fillColor: '#f2a489',
    fillOpacity: 0.9,
    zIndex: 80
  }
};

const markerMap = new Map();
const poiMarkers = new Map();
const nearbyPois = ref([]);
const poiLoading = ref(false);
const poiError = ref('');
let placeSearchInstance = null;
let poiSearchToken = 0;
let addressSearchMarker = null;
let addressSearchToken = 0;
let addressSearchAbortController = null;
let stopSearchWatcher = null;

const removeGaodeScripts = () => {
  if (typeof document === 'undefined') {
    return;
  }
  document.querySelectorAll('script[data-gaode-loader]').forEach((script) => {
    if (script.parentNode) {
      script.parentNode.removeChild(script);
    } else {
      script.remove();
    }
  });
};

const loadGaodeMapScript = () => {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('window-unavailable'));
  }
  if (window.AMap) {
    return Promise.resolve(window.AMap);
  }
  if (!mapKey.value) {
    return Promise.reject(new Error('missing-key'));
  }
  if (scriptPromise) {
    return scriptPromise;
  }
  ensureSecurityConfig();
  removeGaodeScripts();
  scriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${mapKey.value}&plugin=AMap.ToolBar,AMap.Scale`;
    script.async = true;
    script.dataset.gaodeLoader = 'gaode-map';
    script.onload = () => {
      if (window.AMap) {
        resolve(window.AMap);
      } else {
        reject(new Error('gaode-unavailable'));
      }
    };
    script.onerror = () => reject(new Error('gaode-script-failed'));
    document.head.appendChild(script);
  }).catch((error) => {
    scriptPromise = null;
    throw error;
  });
  return scriptPromise;
};

const clearPoiMarkers = () => {
  poiMarkers.forEach((marker) => {
    if (marker && typeof marker.setMap === 'function') {
      marker.setMap(null);
    }
  });
  poiMarkers.clear();
};

const clearNearbyPois = () => {
  nearbyPois.value = [];
  poiError.value = '';
  poiLoading.value = false;
  clearPoiMarkers();
};

const parsePoiLocation = (location) => {
  if (!location) {
    return null;
  }
  if (typeof location === 'string') {
    const parts = location.split(',');
    if (parts.length >= 2) {
      const [lng, lat] = parts.map((value) => Number(value));
      if (Number.isFinite(lat) && Number.isFinite(lng)) {
        return { lat, lng };
      }
    }
  } else if (Array.isArray(location) && location.length >= 2) {
    const [lng, lat] = location.map((value) => Number(value));
    if (Number.isFinite(lat) && Number.isFinite(lng)) {
      return { lat, lng };
    }
  } else if (
    typeof location === 'object' &&
    location !== null &&
    Number.isFinite(Number(location.lng)) &&
    Number.isFinite(Number(location.lat))
  ) {
    return { lat: Number(location.lat), lng: Number(location.lng) };
  }
  return null;
};

const formatPoiDistance = (value) => {
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) {
    return '';
  }
  if (numeric >= 1000) {
    return t('locationViewer.pois.distanceKm', { value: (numeric / 1000).toFixed(1) });
  }
  return t('locationViewer.pois.distanceM', { value: Math.round(numeric) });
};

const poiListItems = computed(() =>
  nearbyPois.value.map((poi) => ({
    ...poi,
    distanceLabel: formatPoiDistance(poi.distance)
  }))
);

const showPoiPanel = computed(
  () =>
    mapReady.value ||
    poiLoading.value ||
    Boolean(poiError.value) ||
    nearbyPois.value.length > 0
);

const buildPoiPopup = (poi) => {
  const distance = formatPoiDistance(poi.distance);
  const type = poi.typeName ? `<div class="type">${poi.typeName}</div>` : '';
  const distanceLabel = distance ? `<div class="distance">${distance}</div>` : '';
  const address = poi.address ? `<div class="address">${poi.address}</div>` : '';
  return `
    <div class="poi-popup">
      <strong>${poi.name ?? ''}</strong>
      ${type}
      ${address}
      ${distanceLabel}
    </div>
  `;
};

const buildPoiMarkerContent = (poi) => {
  const label = poi.shortName || poi.name || '';
  return `<div class="poi-marker">${label}</div>`;
};

const ensurePlaceSearch = () => {
  if (!AMapNamespace) {
    return Promise.reject(new Error('gaode-unavailable'));
  }
  if (placeSearchInstance) {
    return Promise.resolve(placeSearchInstance);
  }
  return new Promise((resolve, reject) => {
    try {
      AMapNamespace.plugin(['AMap.PlaceSearch'], () => {
        placeSearchInstance = new AMapNamespace.PlaceSearch({
          pageSize: 20,
          extensions: 'base'
        });
        resolve(placeSearchInstance);
      });
    } catch (error) {
      reject(error);
    }
  });
};

const runNearbySearch = (placeSearch, center) =>
  new Promise((resolve, reject) => {
    try {
      placeSearch.searchNearBy('', center, 2000, (status, result) => {
        if (status === 'complete' && result?.poiList?.pois) {
          resolve(result.poiList.pois);
          return;
        }
        if (status === 'no_data') {
          resolve([]);
          return;
        }
        reject(new Error(status || 'poi-failed'));
      });
    } catch (error) {
      reject(error);
    }
  });

const normalizePoi = (poi) => {
  if (!poi) {
    return null;
  }
  const coords = parsePoiLocation(poi.location);
  if (!coords) {
    return null;
  }
  const rawDistance = Number(poi.distance);
  const typeName = typeof poi.type === 'string' && poi.type.includes(';')
    ? poi.type.split(';')[0]
    : poi.typeName || poi.type || '';
  const name = typeof poi.name === 'string' ? poi.name.trim() : '';
  const shortName = name && name.length > 8 ? `${name.slice(0, 8)}…` : name;
  const idSource = poi.id ?? `${coords.lng},${coords.lat}`;
  return {
    id: String(idSource),
    name,
    shortName,
    address: poi.address || poi.adname || '',
    typeName: typeName,
    distance: Number.isFinite(rawDistance) ? rawDistance : null,
    coords
  };
};

const updatePoiMarkers = (pois) => {
  if (!gaodeMap || !AMapNamespace) {
    return;
  }
  const nextIds = new Set(pois.map((poi) => poi.id));
  poiMarkers.forEach((marker, id) => {
    if (!nextIds.has(id)) {
      if (marker && typeof marker.setMap === 'function') {
        marker.setMap(null);
      }
      poiMarkers.delete(id);
    }
  });
  pois.forEach((poi) => {
    if (!poi.coords) {
      return;
    }
    const position = [poi.coords.lng, poi.coords.lat];
    let marker = poiMarkers.get(poi.id);
    if (!marker) {
      marker = new AMapNamespace.Marker({
        position,
        anchor: 'bottom-center',
        offset: new AMapNamespace.Pixel(0, -12),
        bubble: true,
        zIndex: 45,
        content: buildPoiMarkerContent(poi)
      });
      marker.on('click', () => {
        if (infoWindow) {
          infoWindow.setContent(buildPoiPopup(poi));
          infoWindow.open(gaodeMap, position);
        }
      });
      marker.setMap(gaodeMap);
      poiMarkers.set(poi.id, marker);
    } else {
      marker.setPosition(position);
      if (typeof marker.setContent === 'function') {
        marker.setContent(buildPoiMarkerContent(poi));
      }
    }
  });
};

const refreshNearbyPois = async (geometry) => {
  if (!gaodeMap || !geometry) {
    clearNearbyPois();
    return;
  }
  const token = ++poiSearchToken;
  poiLoading.value = true;
  poiError.value = '';
  try {
    const placeSearch = await ensurePlaceSearch();
    if (token !== poiSearchToken) {
      return;
    }
    const center = new AMapNamespace.LngLat(geometry.coords.lng, geometry.coords.lat);
    const results = await runNearbySearch(placeSearch, center);
    if (token !== poiSearchToken) {
      return;
    }
    const normalized = results
      .map(normalizePoi)
      .filter(Boolean)
      .slice(0, 12);
    nearbyPois.value = normalized;
    updatePoiMarkers(normalized);
  } catch (error) {
    if (token === poiSearchToken) {
      nearbyPois.value = [];
      poiError.value = t('locationViewer.pois.error');
      clearPoiMarkers();
    }
  } finally {
    if (token === poiSearchToken) {
      poiLoading.value = false;
    }
  }
};

const clearAddressSearchMarker = () => {
  if (addressSearchMarker && typeof addressSearchMarker.setMap === 'function') {
    addressSearchMarker.setMap(null);
  }
  addressSearchMarker = null;
};

const resetSearchState = ({ resetQuery = false, preservePois = false } = {}) => {
  if (addressSearchAbortController) {
    addressSearchAbortController.abort();
    addressSearchAbortController = null;
  }
  clearAddressSearchMarker();
  if (!preservePois) {
    clearNearbyPois();
  }
  activeSearchResult.value = null;
  if (resetQuery) {
    activeSearchQuery.value = '';
  }
  searchStatusMessage.value = '';
  searchErrorMessage.value = '';
  searchLoading.value = false;
};

const runAddressSearch = async (keyword) => {
  const query = typeof keyword === 'string' ? keyword.trim() : '';
  if (!query) {
    return;
  }
  activeSearchQuery.value = query;
  const token = ++addressSearchToken;
  searchLoading.value = true;
  searchErrorMessage.value = '';
  searchStatusMessage.value = t('locationViewer.search.searching', { query });

  if (addressSearchAbortController) {
    addressSearchAbortController.abort();
  }
  const abortController = new AbortController();
  addressSearchAbortController = abortController;

  const params = new URLSearchParams({ query });
  const region = extractRegion(query);
  if (region) {
    params.set('city', region);
  }
  const base = effectiveApiBaseUrl.value;
  const url = `${base ? base : ''}/houses/map-search?${params.toString()}`;

  try {
    const map = await ensureMap();
    const response = await fetch(url, { signal: abortController.signal });
    if (token !== addressSearchToken) {
      return;
    }
    if (!response.ok) {
      if (response.status === 404) {
        resetSearchState({ preservePois: false });
        searchErrorMessage.value = t('locationViewer.search.empty', { query });
        emitViewportChange('search');
        return;
      }
      throw new Error(`map-search-${response.status}`);
    }
    let payload;
    try {
      payload = await response.json();
    } catch (error) {
      payload = null;
    }
    if (!payload || payload.found === false) {
      resetSearchState({ preservePois: false });
      searchErrorMessage.value = t('locationViewer.search.empty', { query });
      emitViewportChange('search');
      return;
    }
    const lat = Number(payload.latitude);
    const lng = Number(payload.longitude);
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
      resetSearchState({ preservePois: false });
      searchErrorMessage.value = t('locationViewer.search.empty', { query });
      emitViewportChange('search');
      return;
    }
    const coords = { lat, lng };
    const name =
      typeof payload.name === 'string' && payload.name.trim() ? payload.name.trim() : query;
    const resolvedAddress =
      typeof payload.address === 'string' && payload.address.trim()
        ? payload.address.trim()
        : query;
    const position = [coords.lng, coords.lat];
    clearAddressSearchMarker();
    if (AMapNamespace && map) {
      try {
        const shortLabel = name.length > 10 ? `${name.slice(0, 10)}…` : name;
        addressSearchMarker = new AMapNamespace.Marker({
          position,
          anchor: 'bottom-center',
          offset: new AMapNamespace.Pixel(0, -18),
          bubble: true,
          zIndex: 90,
          content: `<div class="search-marker">${shortLabel}</div>`
        });
        addressSearchMarker.on('click', () => {
          if (infoWindow) {
            infoWindow.setContent(
              buildMarkerContent({ title: name, address: resolvedAddress, price: '' })
            );
            infoWindow.open(map, position);
          }
        });
        addressSearchMarker.setMap(map);
      } catch (error) {
        console.warn('Failed to render search marker', error);
      }
    }
    try {
      const currentZoom = map?.getZoom?.();
      const targetZoom = currentZoom && currentZoom >= 16 ? currentZoom : 16;
      map?.setZoomAndCenter?.(targetZoom, position, true);
    } catch (error) {
      console.warn('Failed to focus search result', error);
    }
    if (infoWindow) {
      infoWindow.setContent(
        buildMarkerContent({ title: name, address: resolvedAddress, price: '' })
      );
      infoWindow.open(map, position);
    }
    activeSearchResult.value = { name, address: resolvedAddress, coords };
    searchStatusMessage.value = t('locationViewer.search.success', { name });
    searchErrorMessage.value = '';
    refreshNearbyPois({ title: name, address: resolvedAddress, coords });
    emitViewportChange('search');
  } catch (error) {
    if (error?.name === 'AbortError') {
      return;
    }
    if (token === addressSearchToken) {
      console.error('Failed to execute map search', error);
      resetSearchState({ preservePois: false });
      searchErrorMessage.value = t('locationViewer.search.error');
      searchStatusMessage.value = '';
    }
  } finally {
    if (token === addressSearchToken) {
      searchLoading.value = false;
      if (addressSearchAbortController === abortController) {
        addressSearchAbortController = null;
      }
    }
  }
};

const handleSearchKeywordChange = (value) => {
  const trimmed = typeof value === 'string' ? value.trim() : '';
  if (!trimmed) {
    addressSearchToken += 1;
    resetSearchState({ resetQuery: true });
    nextTick(() => {
      focusOnActiveGeometry();
    });
    return;
  }
  runAddressSearch(trimmed);
};

const ensureMapResized = () => {
  if (typeof window === 'undefined') {
    return;
  }
  if (gaodeMap && typeof gaodeMap.resize === 'function') {
    try {
      gaodeMap.resize();
    } catch (error) {
      console.warn('Gaode map resize failed', error);
    }
  }
};

const startResizeObserver = () => {
  if (typeof window === 'undefined' || typeof ResizeObserver === 'undefined') {
    return;
  }
  if (!mapContainer.value) {
    return;
  }
  if (resizeObserver) {
    resizeObserver.disconnect();
  }
  resizeObserver = new ResizeObserver(() => {
    ensureMapResized();
  });
  resizeObserver.observe(mapContainer.value);
};

const applyMapStyle = () => {
  if (!gaodeMap || !AMapNamespace) {
    return;
  }
  if (!defaultLayer) {
    defaultLayer = new AMapNamespace.TileLayer();
  }
  if (!satelliteLayer) {
    satelliteLayer = new AMapNamespace.TileLayer.Satellite();
  }
  if (!roadNetLayer) {
    roadNetLayer = new AMapNamespace.TileLayer.RoadNet();
  }
  if (mapStyle.value === 'satellite') {
    gaodeMap.setLayers([satelliteLayer, roadNetLayer]);
  } else {
    gaodeMap.setLayers([defaultLayer]);
    roadNetLayer.setMap(null);
  }
};

const ensureMap = async () => {
  if (gaodeMap) {
    return gaodeMap;
  }
  if (!mapContainer.value) {
    throw new Error('container-missing');
  }
  const AMap = await loadGaodeMapScript();
  AMapNamespace = AMap;
  mapContainer.value.innerHTML = '';
  gaodeMap = new AMap.Map(mapContainer.value, {
    viewMode: '3D',
    zoom: 12,
    center: [116.39747, 39.908823],
    zooms: [3, 20],
    resizeEnable: true
  });
  applyMapStyle();
  if (typeof AMap.ToolBar === 'function') {
    gaodeMap.addControl(new AMap.ToolBar({ position: 'LT' }));
  }
  if (typeof AMap.Scale === 'function') {
    gaodeMap.addControl(new AMap.Scale({ position: 'LB' }));
  }
  mapReady.value = true;
  activeMapProvider.value = 'gaode';
  gaodeMap.on('moveend', () => emitViewportChange('move'));
  gaodeMap.on('zoomend', () => emitViewportChange('move'));
  gaodeMap.on('complete', () => emitViewportChange('init'));
  infoWindow = new AMap.InfoWindow({ anchor: 'bottom-center' });
  nextTick(() => {
    ensureMapResized();
  });
  return gaodeMap;
};

const destroyMap = () => {
  resetSearchState({ resetQuery: true });
  addressSearchToken = 0;
  clearPoiMarkers();
  nearbyPois.value = [];
  poiError.value = '';
  poiLoading.value = false;
  placeSearchInstance = null;
  poiSearchToken = 0;
  markerMap.forEach((marker) => {
    if (marker && typeof marker.setMap === 'function') {
      marker.setMap(null);
    }
  });
  markerMap.clear();
  if (infoWindow) {
    infoWindow.close();
    infoWindow = null;
  }
  if (gaodeMap && typeof gaodeMap.destroy === 'function') {
    gaodeMap.destroy();
  }
  gaodeMap = null;
  mapReady.value = false;
  activeMapProvider.value = '';
};

const buildMarkerContent = (geometry) => {
  const price = geometry.price ? `<div class="price">${geometry.price}</div>` : '';
  return `
    <div class="marker-popup">
      <strong>${geometry.title ?? ''}</strong>
      <div class="address">${geometry.address ?? ''}</div>
      ${price}
    </div>
  `;
};

const updateMarkerAppearance = (marker, active) => {
  if (!marker || !AMapNamespace) {
    return;
  }
  const style = active ? circleStyles.active : circleStyles.default;
  marker.setOptions({
    radius: style.radius,
    strokeColor: style.strokeColor,
    strokeWeight: style.strokeWeight,
    strokeOpacity: style.strokeOpacity,
    fillColor: style.fillColor,
    fillOpacity: style.fillOpacity,
    zIndex: style.zIndex
  });
};

const highlightActiveMarker = () => {
  markerMap.forEach((marker, id) => {
    updateMarkerAppearance(marker, id === activeHouseId.value);
  });
};

const focusOnActiveGeometry = () => {
  if (hasActiveSearch.value) {
    return;
  }
  if (!gaodeMap || !latestGeometries.value.length) {
    return;
  }
  let active = latestGeometries.value.find((geometry) => geometry.id === activeHouseId.value);
  if (!active && pendingFocusKey.value) {
    const pending = latestGeometries.value.find((geometry) => geometry.id === pendingFocusKey.value);
    if (pending) {
      activeHouseId.value = pending.id;
      active = pending;
    }
    pendingFocusKey.value = '';
  }
  if (!active && latestGeometries.value.length) {
    active = latestGeometries.value[0];
    activeHouseId.value = active.id;
  }
  if (!active) {
    return;
  }
  const position = [active.coords.lng, active.coords.lat];
  try {
    const currentZoom = gaodeMap.getZoom?.();
    const targetZoom = currentZoom && currentZoom >= 15 ? currentZoom : 15;
    gaodeMap.setZoomAndCenter(targetZoom, position, true);
    if (infoWindow) {
      infoWindow.setContent(buildMarkerContent(active));
      infoWindow.open(gaodeMap, position);
    }
  } catch (error) {
    console.warn('Failed to focus Gaode marker', error);
  }
  refreshNearbyPois(active);
  emitViewportChange('focus');
};

const renderMarkers = () => {
  if (!gaodeMap || !AMapNamespace) {
    return;
  }
  const activeId = activeHouseId.value;
  const nextIds = new Set(latestGeometries.value.map((geometry) => geometry.id));
  markerMap.forEach((marker, id) => {
    if (!nextIds.has(id)) {
      marker.setMap(null);
      markerMap.delete(id);
    }
  });
  latestGeometries.value.forEach((geometry) => {
    const position = [geometry.coords.lng, geometry.coords.lat];
    let marker = markerMap.get(geometry.id);
    if (!marker) {
      marker = new AMapNamespace.CircleMarker({
        center: position,
        cursor: 'pointer'
      });
      marker.on('click', () => {
        selectHouse(geometry.id);
        focusOnActiveGeometry();
      });
      marker.setMap(gaodeMap);
      markerMap.set(geometry.id, marker);
    } else {
      marker.setCenter(position);
    }
    updateMarkerAppearance(marker, geometry.id === activeId);
  });
  highlightActiveMarker();
  if (!hasActiveSearch.value) {
    focusOnActiveGeometry();
  }
};

const computeViewportPayload = (source = 'auto') => {
  if (!gaodeMap) {
    if (latestGeometries.value.length) {
      const first = latestGeometries.value[0];
      return {
        centerLat: first.coords.lat,
        centerLng: first.coords.lng,
        radiusKm: 3,
        provider: 'gaode',
        source
      };
    }
    return null;
  }
  const center = gaodeMap.getCenter?.();
  if (!center) {
    return null;
  }
  const bounds = gaodeMap.getBounds?.();
  let radiusKm = 3;
  if (bounds) {
    const southWest = bounds.getSouthWest();
    const northEast = bounds.getNorthEast();
    if (southWest && northEast) {
      const diagonal = haversineDistanceKm(
        southWest.getLat(),
        southWest.getLng(),
        northEast.getLat(),
        northEast.getLng()
      );
      radiusKm = Math.max(0.3, diagonal / 2);
    }
  }
  return {
    centerLat: center.getLat?.() ?? center.lat,
    centerLng: center.getLng?.() ?? center.lng,
    radiusKm,
    provider: 'gaode',
    source
  };
};

const emitViewportChange = (source = 'auto') => {
  const payload = computeViewportPayload(source);
  if (!payload) {
    return;
  }
  const rounded = {
    centerLat: Number.parseFloat(Number(payload.centerLat).toFixed(6)),
    centerLng: Number.parseFloat(Number(payload.centerLng).toFixed(6)),
    radiusKm: Number.parseFloat(Number(payload.radiusKm).toFixed(3)),
    provider: payload.provider,
    source,
    focusKey: activeHouseId.value
  };
  const shouldEmit = () => {
    if (!lastViewportPayload) {
      return true;
    }
    if (source === 'focus' && lastViewportPayload.focusKey !== rounded.focusKey) {
      return true;
    }
    const latDiff = Math.abs((lastViewportPayload.centerLat ?? 0) - (rounded.centerLat ?? 0));
    const lngDiff = Math.abs((lastViewportPayload.centerLng ?? 0) - (rounded.centerLng ?? 0));
    const radiusDiff = Math.abs((lastViewportPayload.radiusKm ?? 0) - (rounded.radiusKm ?? 0));
    const providerChanged = lastViewportPayload.provider !== rounded.provider;
    return providerChanged || latDiff > 0.0005 || lngDiff > 0.0005 || radiusDiff > 0.2;
  };
  if (shouldEmit()) {
    lastViewportPayload = rounded;
    emit('viewport-change', rounded);
  }
};

const updateMapMarkers = async () => {
  if (!isMounted.value || props.loading) {
    return;
  }
  if (!locationItems.value.length) {
    latestGeometries.value = [];
    geocodeWarnings.value = [];
    markerMap.forEach((marker) => {
      if (marker && typeof marker.setMap === 'function') {
        marker.setMap(null);
      }
    });
    markerMap.clear();
    lastViewportPayload = null;
    clearNearbyPois();
    return;
  }
  mapBusy.value = true;
  mapError.value = '';
  const token = ++updateToken;
  if (!mapKey.value) {
    mapError.value = t('locationViewer.errors.missingKey');
    mapBusy.value = false;
    return;
  }
  try {
    await ensureMap();
  } catch (error) {
    console.error('Failed to initialise Gaode map', error);
    mapError.value = t('locationViewer.errors.mapInit');
    mapBusy.value = false;
    return;
  }
  if (token !== updateToken) {
    mapBusy.value = false;
    return;
  }
  try {
    mapError.value = '';
    const warnings = [];
    const geometries = [];
    locationItems.value.forEach((item) => {
      const coords = extractHouseCoordinates(item.house);
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
    geocodeWarnings.value = warnings;
    latestGeometries.value = geometries;
    if (!geometries.length) {
      mapError.value = '';
      clearNearbyPois();
      return;
    }
    renderMarkers();
    emitViewportChange('data');
  } catch (error) {
    console.error('Failed to render Gaode markers', error);
    mapError.value = t('locationViewer.errors.mapInit');
  } finally {
    if (token === updateToken) {
      mapBusy.value = false;
    }
  }
};

const resetMapForConfig = () => {
  destroyMap();
  if (typeof window !== 'undefined' && window.AMap) {
    try {
      delete window.AMap;
    } catch (error) {
      window.AMap = null;
    }
  }
  scriptPromise = null;
  removeGaodeScripts();
  if (isMounted.value) {
    nextTick(() => {
      updateMapMarkers();
    });
  }
};

watch(
  () => [
    props.mapConfig?.apiKey,
    props.mapConfig?.key,
    props.mapConfig?.mapKey,
    props.mapConfig?.jsSecurityCode,
    props.mapConfig?.securityCode,
    props.mapConfig?.jsSecurity
  ],
  () => {
    const previousKey = mapKey.value;
    const previousSecurity = mapSecurityCode.value;
    syncMapConfiguration();
    if (mapKey.value !== previousKey) {
      resetMapForConfig();
    } else if (mapSecurityCode.value !== previousSecurity) {
      ensureSecurityConfig();
    }
  }
);

watch(activeHouseId, () => {
  highlightActiveMarker();
});

watch(mapReady, (ready) => {
  if (ready) {
    nextTick(() => {
      ensureMapResized();
      emitViewportChange('init');
    });
  }
});

watch(mapStyle, () => {
  applyMapStyle();
  if (mapReady.value) {
    nextTick(() => {
      ensureMapResized();
      emitViewportChange('style');
    });
  }
});

watch(
  () => props.houses,
  () => {
    updateMapMarkers();
  },
  { deep: true }
);

watch(
  () => props.loading,
  (value) => {
    if (!value) {
      updateMapMarkers();
    }
  }
);

onMounted(() => {
  isMounted.value = true;
  stopSearchWatcher = watch(
    searchKeyword,
    (value) => {
      handleSearchKeywordChange(value);
    },
    { immediate: true }
  );
  nextTick(() => {
    startResizeObserver();
    updateMapMarkers();
  });
});

onBeforeUnmount(() => {
  latestGeometries.value = [];
  destroyMap();
  lastViewportPayload = null;
  if (typeof stopSearchWatcher === 'function') {
    stopSearchWatcher();
    stopSearchWatcher = null;
  }
  if (resizeObserver) {
    resizeObserver.disconnect();
    resizeObserver = null;
  }
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.style-toggle {
  display: inline-flex;
  gap: 0.4rem;
  padding: 0.25rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--color-surface) 82%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
}

.style-option {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.35rem 0.85rem;
  background: transparent;
  color: var(--color-text-soft);
  cursor: pointer;
  font-size: 0.85rem;
  transition: background 0.2s ease, color 0.2s ease;
}

.style-option.active {
  background: var(--gradient-primary);
  color: var(--color-text-on-emphasis);
  box-shadow: var(--shadow-sm);
}

.style-option:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

.map-fallback {
  margin: -0.2rem 0 0;
  color: color-mix(in srgb, var(--color-warning) 60%, var(--color-text-soft) 40%);
  font-size: 0.85rem;
  font-style: italic;
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

.focus-summary,
.viewport-summary {
  margin: -0.4rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.viewport-summary {
  color: color-mix(in srgb, var(--color-text-soft) 70%, var(--color-accent) 30%);
}

.search-status {
  margin: 0.2rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.search-status.error {
  color: var(--color-danger-strong);
  font-weight: 600;
}

.search-status.loading {
  color: color-mix(in srgb, var(--color-accent) 65%, var(--color-text-muted) 35%);
}

.search-status.result {
  color: color-mix(in srgb, var(--color-text-soft) 80%, var(--color-accent) 20%);
  font-size: 0.82rem;
}

.poi-panel {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
  background: color-mix(in srgb, var(--color-surface) 82%, transparent);
  box-shadow: var(--shadow-xs);
}

.poi-panel h4 {
  margin: 0;
  font-size: 1rem;
  color: var(--color-text-strong);
}

.poi-status {
  margin: 0;
  font-size: 0.88rem;
  color: var(--color-text-muted);
}

.poi-status.error {
  color: var(--color-danger-strong);
}

.poi-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.poi-item {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 0.35rem 0.8rem;
  padding: 0.3rem 0;
  border-top: 1px dashed color-mix(in srgb, var(--color-border) 65%, transparent);
}

.poi-item:first-child {
  border-top: none;
  padding-top: 0;
}

.poi-name {
  font-weight: 600;
  color: var(--color-text-strong);
  flex: 1 1 auto;
}

.poi-type {
  color: color-mix(in srgb, var(--color-accent) 75%, var(--color-text-soft) 25%);
  font-size: 0.82rem;
}

.poi-distance {
  margin-left: auto;
  font-size: 0.82rem;
  color: var(--color-text-muted);
}

.poi-marker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--color-accent) 80%, transparent);
  color: var(--color-text-on-emphasis);
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
  box-shadow: var(--shadow-sm);
}

.search-marker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--color-accent) 92%, transparent);
  color: var(--color-text-on-emphasis);
  font-size: 0.78rem;
  font-weight: 600;
  box-shadow: var(--shadow-md);
  white-space: nowrap;
}

.poi-popup {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  min-width: 200px;
  color: var(--color-text-strong);
}

.poi-popup .type {
  color: color-mix(in srgb, var(--color-accent) 75%, var(--color-text-soft) 25%);
  font-weight: 600;
  font-size: 0.85rem;
}

.poi-popup .distance {
  color: var(--color-text-soft);
  font-size: 0.85rem;
}

.poi-popup .address {
  color: var(--color-text-muted);
  font-size: 0.85rem;
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
