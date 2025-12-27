<template>
  <section class="house-explorer">
    <form v-if="showFilters" class="filters" @submit.prevent="submitFilters">
      <div class="search-row" ref="searchContainerRef">
        <div class="search-field">
          <input
            ref="keywordInputRef"
            v-model.trim="localFilters.keyword"
            type="search"
            :placeholder="t('explorer.searchPlaceholder')"
            @focus="showHistory"
            @click="showHistory"
          />
          <button type="button" class="history-toggle" @click.stop="toggleHistory">
            {{ t('explorer.history.toggle') }}
          </button>
        </div>
        <button type="submit" class="search-submit">
          {{ t('explorer.actions.search') }}
        </button>
        <transition name="fade">
          <ul v-if="historyVisible && searchHistory.length" class="history-dropdown">
            <li v-for="item in searchHistory" :key="item">
              <button type="button" @click="selectHistory(item)">{{ item }}</button>
            </li>
          </ul>
        </transition>
      </div>
      <header class="explorer-header">
        <div>
          <h2>{{ t('explorer.title') }}</h2>
          <p>{{ t('explorer.subtitle') }}</p>
        </div>
      </header>
      <div class="range">
        <label>
          {{ t('explorer.filters.minPrice') }}
          <input
            v-model.number="localFilters.minPrice"
            type="number"
            min="0"
            step="0.01"
            :placeholder="t('explorer.filters.pricePlaceholder')"
          />
        </label>
        <label>
          {{ t('explorer.filters.maxPrice') }}
          <input
            v-model.number="localFilters.maxPrice"
            type="number"
            min="0"
            step="0.01"
            :placeholder="t('explorer.filters.pricePlaceholder')"
          />
        </label>
        <label>
          {{ t('explorer.filters.minArea') }}
          <input
            v-model.number="localFilters.minArea"
            type="number"
            min="0"
            step="0.1"
            :placeholder="t('explorer.filters.areaPlaceholder')"
          />
        </label>
        <label>
          {{ t('explorer.filters.maxArea') }}
          <input
            v-model.number="localFilters.maxArea"
            type="number"
            min="0"
            step="0.1"
            :placeholder="t('explorer.filters.areaPlaceholder')"
          />
        </label>
      </div>
      <div class="actions">
        <button type="submit">{{ t('explorer.actions.applyFilters') }}</button>
        <button type="button" class="secondary" @click="resetFilters">
          {{ t('explorer.actions.reset') }}
        </button>
      </div>
    </form>

    <section v-if="shouldShowRecommendations" class="recommendations">
      <div class="recommendation">
        <h3>{{ t('explorer.recommendations.sellers.title') }}</h3>
        <ul>
          <li v-for="seller in recommendations.sellers" :key="`seller-${seller.username}`">
            <div class="seller-card">
              <div class="seller-text">
                <strong>{{ seller.displayName }}</strong>
                <span class="username">@{{ seller.username }}</span>
              </div>
              <span class="score">
                {{ t('explorer.recommendations.sellers.score', { score: seller.reputationScore ?? '—' }) }}
              </span>
            </div>
          </li>
        </ul>
      </div>
      <div class="recommendation">
        <h3>{{ t('explorer.recommendations.buyers.title') }}</h3>
        <ul>
          <li v-for="buyer in recommendations.buyers" :key="`buyer-${buyer.username}`">
            <strong>{{ buyer.displayName }}</strong>
            <span class="username">@{{ buyer.username }}</span>
            <span class="score">
              {{ t('explorer.recommendations.buyers.score', { score: buyer.reputationScore ?? '—' }) }}
            </span>
          </li>
        </ul>
      </div>
    </section>

    <transition name="fade">
      <div v-if="showFilters && hasSellerFilter" class="active-seller-filter">
        <span>{{ t('explorer.recommendations.sellers.active', { name: selectedSeller?.displayName }) }}</span>
        <button type="button" @click="clearSellerFilter">{{ t('explorer.recommendations.sellers.clear') }}</button>
      </div>
    </transition>

    <div v-if="loading" class="loading">{{ t('explorer.states.loading') }}</div>
    <div v-else-if="!displayedHouses.length" class="empty">{{ emptyStateMessage }}</div>

    <div v-else class="house-grid">
      <article
        v-for="house in paginatedHouses"
        :key="house.id"
        class="house-card"
        @click="openDetail(house)"
      >
        <div class="status" :class="statusClass(house)">{{ statusLabel(house) }}</div>
        <div class="cover" v-if="coverImage(house)">
          <img :src="coverImage(house)" :alt="house.title" loading="lazy" />
          <div v-if="house.reservationActive" class="reservation-flag">{{ reservationStatusText(house) }}</div>
          <div class="image-overlay">
            <div class="overlay-text">{{ house.title }}</div>
            <span class="overlay-action">{{ t('explorer.actions.viewDetail') }}</span>
          </div>
        </div>
        <div class="cover placeholder" v-else>
          <span>{{ t('explorer.labels.noImage') }}</span>
        </div>
        <footer class="card-footer">
          <button
            type="button"
            class="favorite-toggle"
            :class="{ active: isFavorite(house) }"
            :aria-label="favoriteButtonLabel(house)"
            :title="favoriteButtonLabel(house)"
            @click.stop="toggleFavorite(house)"
          >
            {{ isFavorite(house) ? '⭐' : '☆' }}
          </button>
          <span class="cta">{{ t('explorer.actions.tapForInfo') }}</span>
        </footer>
      </article>
    </div>

    <HouseDetailModal
      v-if="activeHouse"
      :house="activeHouse"
      :can-view-sensitive-info="canViewSensitiveInfo"
      @close="activeHouse = null"
    >
      <template #actions>
        <div v-if="canOperate" class="detail-actions">
          <button
            type="button"
            class="ghost"
            :disabled="contactDisabled || !isApproved(activeHouse)"
            @click.stop="contactSeller(activeHouse)"
          >
            {{ t('explorer.actions.contactSeller') }}
          </button>
          <button
            type="button"
            class="secondary"
            :disabled="reservationDisabled(activeHouse)"
            @click.stop="emit('reserve', activeHouse)"
          >
            {{
              isReservingCurrent(activeHouse)
                ? t('explorer.actions.reserving')
                : t('explorer.actions.reserve', { deposit: depositAmount(activeHouse) })
            }}
          </button>
          <button
            type="button"
            class="primary"
            :disabled="purchaseDisabled || !isApproved(activeHouse)"
            @click.stop="purchase(activeHouse)"
          >
            {{ purchaseDisabled ? t('explorer.actions.processing') : t('explorer.actions.purchase') }}
          </button>
        </div>
        <p v-if="activeHouse?.reservationActive" class="verification-tip">
          {{ reservationNotice(activeHouse) }}
        </p>
        <p v-else-if="requiresVerification" class="verification-tip">
          {{ t('explorer.tips.requireVerification') }}
        </p>
        <p v-else-if="activeHouse?.status === 'SOLD'" class="verification-tip">
          {{ t('explorer.tips.soldOut') }}
        </p>
        <p v-else-if="!isApproved(activeHouse)" class="verification-tip">
          {{ t('explorer.tips.awaitingApproval') }}
        </p>
      </template>
    </HouseDetailModal>
    <div v-if="showPagination" class="pagination">
      <button type="button" class="page-btn" :disabled="!canGoPrevious" @click="goToPreviousPage">
        {{ t('explorer.pagination.prev') }}
      </button>
      <span class="page-indicator">
        {{ t('explorer.pagination.page', { current: currentPage, total: totalPages }) }}
      </span>
      <button type="button" class="page-btn" :disabled="!canGoNext" @click="goToNextPage">
        {{ t('explorer.pagination.next') }}
      </button>
    </div>
  </section>
</template>

<script setup>
import { computed, inject, reactive, watch, ref, onMounted, onBeforeUnmount } from 'vue';
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
  currentUser: {
    type: Object,
    default: null
  },
  canViewSensitiveInfo: {
    type: Boolean,
    default: false
  },
  filters: {
    type: Object,
    default: () => ({})
  },
  recommendations: {
    type: Object,
    default: () => ({ sellers: [], buyers: [] })
  },
  purchaseLoading: {
    type: Boolean,
    default: false
  },
  reservationLoading: {
    type: Boolean,
    default: false
  },
  reservationTarget: {
    type: [Number, String],
    default: null
  },
  apiBaseUrl: {
    type: String,
    required: true
  },
  favoriteIds: {
    type: Array,
    default: () => []
  },
  canFavorite: {
    type: Boolean,
    default: false
  },
  pageSize: {
    type: Number,
    default: 6
  },
  showFilters: {
    type: Boolean,
    default: true
  },
  showRecommendations: {
    type: Boolean,
    default: true
  },
  emptyMessage: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['search', 'reserve', 'purchase', 'contact-seller', 'toggle-favorite']);

const settings = inject('appSettings', { language: 'zh' });
const translate = inject('translate', (key) => key);
const t = (key, vars) => translate(key, vars);
const locale = computed(() => (settings?.language === 'en' ? 'en-US' : 'zh-CN'));

const statusLabels = computed(() => ({
  PENDING_REVIEW: t('statuses.pending'),
  APPROVED: t('statuses.approved'),
  REJECTED: t('statuses.rejected'),
  SOLD: t('statuses.sold')
}));

const localFilters = reactive({
  keyword: '',
  minPrice: '',
  maxPrice: '',
  minArea: '',
  maxArea: '',
  sellerUsername: ''
});

const selectedSeller = ref(null);
const activeHouse = ref(null);

const historyStorageKey = 'house-search-history';
const historyVisible = ref(false);
const searchHistory = ref([]);
const searchContainerRef = ref(null);
const keywordInputRef = ref(null);

const canOperate = computed(() => props.currentUser?.role === 'BUYER');
const requiresVerification = computed(
  () => props.currentUser?.role === 'BUYER' && !props.currentUser?.realNameVerified
);
const purchaseDisabled = computed(
  () => props.purchaseLoading || props.loading || requiresVerification.value
);
const contactDisabled = computed(() => requiresVerification.value);

const hasRecommendations = computed(() => {
  const sellers = Array.isArray(props.recommendations?.sellers)
    ? props.recommendations.sellers.length
    : 0;
  const buyers = Array.isArray(props.recommendations?.buyers)
    ? props.recommendations.buyers.length
    : 0;
  return sellers + buyers > 0;
});

const shouldShowRecommendations = computed(() => props.showRecommendations && hasRecommendations.value);

const favoriteIdSet = computed(
  () => new Set((Array.isArray(props.favoriteIds) ? props.favoriteIds : []).map((value) => String(value)))
);

const pageSizeValue = computed(() => {
  const numeric = Number(props.pageSize);
  if (!Number.isFinite(numeric) || numeric <= 0) {
    return 6;
  }
  return Math.max(1, Math.floor(numeric));
});

const displayedHouses = computed(() => {
  if (!Array.isArray(props.houses)) {
    return [];
  }
  return props.houses.filter((house) => house && typeof house === 'object');
});

const currentPage = ref(1);

const totalPages = computed(() => {
  const count = displayedHouses.value.length;
  if (count === 0) {
    return 1;
  }
  return Math.max(1, Math.ceil(count / pageSizeValue.value));
});

const paginatedHouses = computed(() => {
  const size = pageSizeValue.value;
  const start = (currentPage.value - 1) * size;
  return displayedHouses.value.slice(start, start + size);
});

const showPagination = computed(() => totalPages.value > 1);
const canGoPrevious = computed(() => currentPage.value > 1);
const canGoNext = computed(() => currentPage.value < totalPages.value);

const goToPreviousPage = () => {
  if (canGoPrevious.value) {
    currentPage.value -= 1;
  }
};

const goToNextPage = () => {
  if (canGoNext.value) {
    currentPage.value += 1;
  }
};

const emptyStateMessage = computed(() => {
  if (typeof props.emptyMessage === 'string') {
    const trimmed = props.emptyMessage.trim();
    if (trimmed) {
      return trimmed;
    }
  }
  return t('explorer.states.empty');
});

const hasSellerFilter = computed(() => Boolean(localFilters.sellerUsername));

watch(
  () => props.filters,
  (value) => {
    if (!value) {
      return;
    }
    Object.entries(value).forEach(([key, val]) => {
      if (key in localFilters) {
        localFilters[key] = val ?? '';
      }
    });
    if (value.sellerUsername) {
      selectedSeller.value = {
        username: value.sellerUsername,
        displayName: value.sellerDisplayName ?? value.sellerUsername
      };
    } else {
      selectedSeller.value = null;
    }
  },
  { immediate: true, deep: true }
);

watch(
  () => props.houses,
  () => {
    currentPage.value = 1;
  }
);

watch(
  () => props.pageSize,
  () => {
    currentPage.value = 1;
  }
);

watch(
  totalPages,
  (pages) => {
    if (currentPage.value > pages) {
      currentPage.value = pages;
    }
    if (pages <= 0) {
      currentPage.value = 1;
    }
  }
);

const submitFilters = () => {
  historyVisible.value = false;
  recordHistory(localFilters.keyword);
  const extra = selectedSeller.value
    ? { sellerDisplayName: selectedSeller.value.displayName }
    : { sellerDisplayName: '' };
  emit('search', { ...localFilters, ...extra });
};

const clearSellerFilter = () => {
  selectedSeller.value = null;
  localFilters.sellerUsername = '';
  emit('search', { ...localFilters, sellerDisplayName: '' });
};

const isFavorite = (house) => {
  if (!house || house.id == null) {
    return false;
  }
  return favoriteIdSet.value.has(String(house.id));
};

const favoriteButtonLabel = (house) => {
  if (!props.canFavorite) {
    return t('favorites.loginRequired');
  }
  return isFavorite(house) ? t('favorites.remove') : t('favorites.add');
};

const toggleFavorite = (house) => {
  emit('toggle-favorite', house);
};

const contactSeller = (house) => {
  if (!house?.sellerUsername) {
    return;
  }
  emit('contact-seller', {
    sellerUsername: house.sellerUsername,
    sellerName: house.sellerName
  });
};

const resetFilters = () => {
  Object.keys(localFilters).forEach((key) => {
    localFilters[key] = '';
  });
  selectedSeller.value = null;
  historyVisible.value = false;
  emit('search', { ...localFilters, sellerDisplayName: '' });
};

const loadHistory = () => {
  if (typeof window === 'undefined') {
    return [];
  }
  try {
    const raw = window.localStorage.getItem(historyStorageKey);
    if (!raw) {
      return [];
    }
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed.slice(0, 9);
    }
  } catch (error) {
    console.warn('Failed to restore search history', error);
  }
  return [];
};

const persistHistory = (history) => {
  if (typeof window === 'undefined') {
    return;
  }
  try {
    window.localStorage.setItem(historyStorageKey, JSON.stringify(history));
  } catch (error) {
    console.warn('Failed to persist search history', error);
  }
};

const recordHistory = (value) => {
  const keyword = (value ?? '').trim();
  if (!keyword) {
    return;
  }
  const unique = [keyword, ...searchHistory.value.filter((item) => item !== keyword)].slice(0, 9);
  searchHistory.value = unique;
  persistHistory(unique);
};

const showHistory = () => {
  if (searchHistory.value.length > 0) {
    historyVisible.value = true;
  }
};

const toggleHistory = () => {
  if (!searchHistory.value.length) {
    historyVisible.value = false;
    return;
  }
  historyVisible.value = !historyVisible.value;
};

const selectHistory = (keyword) => {
  localFilters.keyword = keyword;
  historyVisible.value = false;
  submitFilters();
  keywordInputRef.value?.blur();
};

const handleClickOutside = (event) => {
  if (!historyVisible.value) {
    return;
  }
  const container = searchContainerRef.value;
  if (container && !container.contains(event.target)) {
    historyVisible.value = false;
  }
};

searchHistory.value = loadHistory();

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside);
});

const coverImage = (house) => {
  if (!house || !Array.isArray(house.imageUrls)) {
    return '';
  }
  return house.imageUrls.find((url) => url && url.trim().length > 0) ?? '';
};

const formatCurrency = (value) => {
  if (value == null || value === '') {
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

const formatNumber = (value) => {
  if (value == null || value === '') {
    return '0';
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '0';
  }
  return num.toLocaleString(locale.value, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });
};

const formatDate = (value) => {
  if (!value) {
    return '—';
  }
  try {
    return new Date(value).toLocaleDateString(locale.value);
  } catch (error) {
    return String(value);
  }
};

const depositAmount = (house) => {
  const downPayment = Number(house?.downPayment);
  if (Number.isFinite(downPayment) && downPayment > 0) {
    return formatCurrency(downPayment);
  }
  const price = Number(house?.price ?? 0);
  if (!Number.isFinite(price) || price <= 0) {
    return '0.00';
  }
  return formatCurrency(price * 0.1);
};

const maskName = (value) => {
  if (!value) {
    return '—';
  }
  const text = String(value);
  if (text.length === 1) {
    return `${text}*`;
  }
  return `${text.slice(0, 1)}**`;
};

const maskUsername = (value) => {
  if (!value) {
    return '—';
  }
  const text = String(value);
  if (text.length <= 2) {
    return '*'.repeat(text.length);
  }
  return `${text.slice(0, 1)}${'*'.repeat(text.length - 2)}${text.slice(-1)}`;
};

const maskPhone = (value) => {
  if (!value) {
    return '—';
  }
  const digits = String(value).trim();
  if (digits.length <= 4) {
    return '*'.repeat(digits.length);
  }
  return `${digits.slice(0, 3)}****${digits.slice(-4)}`;
};

const shouldMask = (house) => {
  if (props.canViewSensitiveInfo) {
    return false;
  }
  if (!house) {
    return true;
  }
  if (props.currentUser?.username && props.currentUser.username === house.sellerUsername) {
    return false;
  }
  return true;
};

const sellerNameDisplay = (house) => {
  const name = house?.sellerName ?? '—';
  return shouldMask(house) ? maskName(name) : name;
};

const sellerUsernameDisplay = (house) => {
  const username = house?.sellerUsername ?? '—';
  return shouldMask(house) ? maskUsername(username) : username;
};

const contactNumberDisplay = (house) => {
  const contact = house?.contactNumber ?? '—';
  return shouldMask(house) ? maskPhone(contact) : contact;
};

const isReservingCurrent = (house) => {
  if (!props.reservationLoading || props.reservationTarget == null) {
    return false;
  }
  return String(props.reservationTarget) === String(house.id);
};

const isApproved = (house) => house?.status === 'APPROVED';

const reservationDisabled = (house) => {
  if (!canOperate.value) {
    return true;
  }
  if (requiresVerification.value) {
    return true;
  }
  if (!isApproved(house)) {
    return true;
  }
  if (house?.reservationActive) {
    return true;
  }
  if (!props.reservationLoading) {
    return false;
  }
  return !isReservingCurrent(house);
};

const reservationNotice = (house) => {
  if (!house?.reservationActive) {
    return '';
  }
  return house.reservationOwnedByRequester
    ? t('explorer.tips.reservedByYou')
    : t('explorer.tips.reservedByOthers');
};

const reservationStatusText = (house) => {
  if (!house?.reservationActive) {
    return '';
  }
  return house.reservationOwnedByRequester
    ? t('explorer.tips.reservedByYou')
    : t('explorer.tips.reservedByOthers');
};

const openDetail = (house) => {
  activeHouse.value = house;
};

const purchase = (house) => {
  if (!isApproved(house)) {
    return;
  }
  emit('purchase', { house, paymentMethod: 'FULL' });
};

const statusLabel = (house) => {
  if (house?.reservationActive) {
    return reservationStatusText(house) || t('statuses.reserved');
  }
  return statusLabels.value[house?.status] ?? t('statuses.pending');
};

const statusClass = (house) => {
  if (house?.reservationActive) {
    return 'reserved';
  }
  switch (house?.status) {
    case 'APPROVED':
      return 'approved';
    case 'REJECTED':
      return 'rejected';
    case 'SOLD':
      return 'sold';
    default:
      return 'pending';
  }
};
</script>

<style scoped>
.house-explorer {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.explorer-header {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  background: linear-gradient(135deg, rgba(236, 246, 255, 0.96), rgba(214, 233, 255, 0.9));
  border-radius: var(--radius-lg);
  padding: 1.9rem;
  border: 1px solid color-mix(in srgb, var(--color-border) 90%, transparent);
  box-shadow: 0 22px 48px rgba(73, 128, 189, 0.18);
  backdrop-filter: blur(calc(var(--glass-blur) / 2));
}

.explorer-header h2 {
  margin: 0;
  font-size: 1.6rem;
  color: var(--color-text-strong);
}

.explorer-header p {
  margin: 0;
  color: var(--color-text-muted);
  max-width: 48rem;
}

.filters {
  display: flex;
  flex-direction: column;
  gap: 1.4rem;
}

.search-row {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  gap: 0.9rem;
  align-items: center;
}

.search-field {
  flex: 1 1 280px;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: var(--radius-pill);
  border: 1px solid color-mix(in srgb, var(--color-border) 90%, transparent);
  background: rgba(246, 250, 255, 0.96);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.search-field input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 1rem;
  outline: none;
  color: var(--color-text-strong);
}

.search-field input::placeholder {
  color: color-mix(in srgb, var(--color-text-soft) 70%, rgba(0, 0, 0, 0.3));
}

.history-toggle {
  border: none;
  background: rgba(72, 139, 214, 0.14);
  color: #1f5f9e;
  border-radius: var(--radius-pill);
  padding: 0.45rem 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: background var(--transition-base), color var(--transition-base);
}

.history-toggle:hover {
  background: rgba(72, 139, 214, 0.24);
}

.search-submit {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.65rem 1.6rem;
  background: var(--gradient-primary);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 18px 38px rgba(73, 128, 189, 0.2);
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.search-submit:hover {
  transform: translateY(-1px);
  box-shadow: 0 24px 50px rgba(150, 132, 118, 0.32);
}

.history-dropdown {
  position: absolute;
  top: calc(100% + 0.35rem);
  left: 0;
  width: min(520px, 100%);
  background: rgba(248, 252, 255, 0.98);
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  box-shadow: 0 18px 36px rgba(73, 128, 189, 0.18);
  padding: 0.6rem;
  list-style: none;
  margin: 0;
  z-index: 12;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.history-dropdown li button {
  width: 100%;
  text-align: left;
  border: none;
  background: transparent;
  padding: 0.55rem 0.65rem;
  border-radius: var(--radius-md);
  color: var(--color-text-soft);
  cursor: pointer;
  transition: background var(--transition-base), color var(--transition-base);
}

.history-dropdown li button:hover {
  background: rgba(72, 139, 214, 0.12);
  color: #1f5f9e;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.range {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.85rem;
}

.range label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-weight: 600;
  color: var(--color-text-strong);
}

.range input {
  padding: 0.6rem 0.9rem;
  border-radius: var(--radius-md);
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  background: rgba(255, 255, 255, 0.88);
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.85rem;
}

.actions button {
  padding: 0.65rem 1.35rem;
  border-radius: var(--radius-pill);
  border: none;
  font-weight: 600;
  cursor: pointer;
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: 0 18px 35px rgba(73, 128, 189, 0.22);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.actions button.secondary {
  background: rgba(240, 247, 255, 0.96);
  color: var(--color-text-strong);
  border: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
  box-shadow: none;
}

.actions button:hover {
  transform: translateY(-1px);
  box-shadow: 0 24px 45px rgba(150, 132, 118, 0.3);
}

.recommendations {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 1.1rem;
}

.recommendation {
  background: rgba(246, 250, 255, 0.96);
  border-radius: var(--radius-lg);
  padding: 1.25rem;
  box-shadow: 0 20px 45px rgba(73, 128, 189, 0.15);
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
}

.recommendation h3 {
  margin: 0 0 0.65rem;
  color: var(--color-text-strong);
}

.recommendation ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.recommendation li {
  padding: 0;
}

.recommendation .username {
  font-size: 0.85rem;
  color: var(--color-text-soft);
}

.recommendation .score {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

.recommendation li {
  display: block;
}

.seller-card {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.9rem 1.1rem;
  border-radius: var(--radius-md);
  border: 1px solid color-mix(in srgb, var(--color-border) 82%, transparent);
  background: rgba(236, 246, 255, 0.98);
  text-align: left;
  cursor: default;
}

.seller-text {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.seller-text strong {
  font-size: 1.05rem;
  color: var(--color-text-strong);
}

.seller-text .username {
  color: var(--color-text-soft);
}

.seller-card .score {
  color: var(--color-text-muted);
}

.active-seller-filter {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.65rem 1.1rem;
  border-radius: var(--radius-pill);
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  background: rgba(210, 231, 255, 0.6);
  color: var(--color-text-strong);
}

.active-seller-filter button {
  border: none;
  background: transparent;
  color: var(--color-accent);
  font-weight: 600;
  cursor: pointer;
}

.loading,
.empty {
  padding: 2rem;
  border-radius: var(--radius-lg);
  border: 1px dashed color-mix(in srgb, var(--color-border) 70%, transparent);
  color: var(--color-text-muted);
  text-align: center;
}

.house-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.25rem;
  justify-content: center;
}

.house-card {
  display: flex;
  flex-direction: column;
  background: rgba(248, 252, 255, 0.96);
  border-radius: var(--radius-xl);
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--color-border) 75%, transparent);
  box-shadow: 0 22px 45px rgba(73, 128, 189, 0.2);
  width: min(360px, 100%);
  margin: 0 auto;
}

.status {
  padding: 0.45rem 0.9rem;
  font-weight: 600;
  font-size: 0.85rem;
  letter-spacing: 0.02em;
}

.status.pending {
  background: rgba(104, 180, 255, 0.16);
  color: #0f549c;
}

.status.approved {
  background: rgba(130, 214, 186, 0.18);
  color: #0f6b52;
}

.status.rejected {
  background: rgba(255, 196, 196, 0.2);
  color: #a43131;
}

.status.reserved {
  background: rgba(250, 204, 21, 0.2);
  color: #854d0e;
}

.status.sold {
  background: rgba(186, 198, 214, 0.26);
  color: var(--color-text-strong);
}

:global(body[data-theme='dark']) :deep(.house-explorer .explorer-header) {
  background: linear-gradient(135deg, rgba(72, 60, 54, 0.9), rgba(40, 34, 31, 0.9));
  border-color: color-mix(in srgb, rgba(141, 126, 112, 0.45) 65%, transparent);
  box-shadow: 0 26px 56px rgba(34, 26, 23, 0.52);
}

:global(body[data-theme='dark']) :deep(.house-explorer .status.pending) {
  background: rgba(214, 189, 120, 0.24);
  color: #f1deb5;
}

:global(body[data-theme='dark']) :deep(.house-explorer .status.approved) {
  background: rgba(150, 176, 162, 0.26);
  color: #d7e5d9;
}

:global(body[data-theme='dark']) :deep(.house-explorer .status.rejected) {
  background: rgba(198, 132, 122, 0.26);
  color: #f3d6cf;
}

:global(body[data-theme='dark']) :deep(.house-explorer .status.reserved) {
  background: rgba(250, 204, 21, 0.28);
  color: #fcd34d;
}

:global(body[data-theme='dark']) :deep(.house-explorer .status.sold) {
  background: rgba(128, 120, 114, 0.28);
  color: rgba(244, 236, 228, 0.9);
}

.cover {
  position: relative;
  padding-top: 66%;
  overflow: hidden;
}

.cover img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.reservation-flag {
  position: absolute;
  top: 0.85rem;
  left: 0.85rem;
  padding: 0.4rem 0.8rem;
  border-radius: var(--radius-pill);
  background: rgba(250, 204, 21, 0.95);
  color: #7c2d12;
  font-weight: 700;
  box-shadow: 0 12px 28px rgba(124, 45, 18, 0.18);
}

.cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(214, 233, 255, 0.6);
  color: var(--color-text-muted);
  font-size: 0.95rem;
}

.details {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  padding: 1.25rem;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
}

.card-title {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.details h3 {
  margin: 0;
  font-size: 1.2rem;
  color: var(--color-text-strong);
}

.address {
  margin: 0.2rem 0 0;
  color: var(--color-text-muted);
}

.pricing {
  display: grid;
  gap: 0.65rem;
}

.pricing-item {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.pricing-item .label {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.pricing-item strong {
  font-size: 1.1rem;
  color: var(--color-text-strong);
}

.pricing-item small {
  font-size: 0.85rem;
  color: var(--color-text-soft);
  margin-left: 0.25rem;
}

.description {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 0.65rem;
  margin: 0;
}

.meta div {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.meta dt {
  font-weight: 600;
  color: var(--color-text-soft);
}

.meta dd {
  margin: 0;
  color: var(--color-text-strong);
}

.keyword-list {
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin: 0;
  padding: 0;
}

.keyword-list li {
  background: rgba(72, 139, 214, 0.14);
  color: #1f5f9e;
  padding: 0.32rem 0.75rem;
  border-radius: var(--radius-pill);
  font-size: 0.88rem;
}

.card-actions {
  margin-top: auto;
  padding: 1.15rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  border-top: 1px solid color-mix(in srgb, var(--color-border) 75%, transparent);
  background: rgba(236, 246, 255, 0.9);
}

.favorite-toggle {
  border: none;
  background: transparent;
  font-size: 1.75rem;
  line-height: 1;
  color: color-mix(in srgb, var(--color-text-soft) 70%, var(--color-text-strong));
  filter: drop-shadow(0 4px 10px rgba(0, 0, 0, 0.12));
  transition: transform var(--transition-base), color var(--transition-base);
}

.favorite-toggle:not(.active) {
  color: color-mix(in srgb, var(--color-text-soft) 35%, transparent);
}

.favorite-toggle:hover {
  transform: scale(1.06);
  color: var(--color-accent);
}

.favorite-toggle.active {
  color: #f5b301;
  filter: drop-shadow(0 6px 14px rgba(245, 179, 1, 0.35));
}

.payment {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.payment label {
  font-size: 0.85rem;
  color: var(--color-text-soft);
}

.payment select {
  padding: 0.55rem 0.75rem;
  border-radius: var(--radius-md);
  border: 1px solid rgba(148, 163, 184, 0.35);
}

.card-error {
  margin: 0;
  color: #dc2626;
  font-size: 0.8rem;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.85rem;
  align-items: center;
  justify-content: flex-start;
}

.detail-actions button {
  padding: 0.78rem 1.4rem;
  border-radius: var(--radius-pill);
  font-weight: 700;
  letter-spacing: 0.01em;
  min-width: 140px;
  box-shadow: 0 14px 24px rgba(73, 128, 189, 0.18);
}

.detail-actions .ghost {
  background: rgba(72, 139, 214, 0.12);
  color: #1f5f9e;
  border: 1px solid color-mix(in srgb, var(--color-border) 88%, transparent);
}

.detail-actions .secondary {
  background: rgba(104, 180, 255, 0.16);
  color: #0f549c;
  border: 1px solid color-mix(in srgb, var(--color-border) 90%, transparent);
}

.detail-actions .primary {
  background: var(--gradient-primary);
  color: #0a1a2f;
  border: none;
  box-shadow: 0 18px 32px rgba(73, 128, 189, 0.22);
}

.action-buttons button {
  padding: 0.6rem 0.9rem;
  border-radius: var(--radius-pill);
  border: none;
  font-weight: 600;
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.action-buttons .contact {
  background: rgba(72, 139, 214, 0.14);
  color: #1f5f9e;
}

.action-buttons .reserve {
  background: rgba(98, 206, 255, 0.14);
  color: #0b6a8f;
}

.action-buttons .purchase {
  background: var(--gradient-primary);
  color: #0a1a2f;
  box-shadow: 0 15px 30px rgba(73, 128, 189, 0.22);
}

.action-buttons button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.action-buttons button:hover:not(:disabled) {
  transform: translateY(-2px);
}

.verification-tip {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.hint {
  color: var(--color-text-soft);
  font-size: 0.9rem;
}

.pagination {
  margin-top: 1.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1.25rem;
}

.page-btn {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.55rem 1.5rem;
  background: color-mix(in srgb, var(--color-accent) 24%, rgba(255, 255, 255, 0.92));
  color: var(--color-text-strong);
  font-weight: 600;
  box-shadow: 0 12px 24px rgba(150, 132, 118, 0.22);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.page-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 18px 36px rgba(150, 132, 118, 0.26);
}

.page-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

.page-indicator {
  font-weight: 600;
  color: var(--color-text-muted);
}

@media (min-width: 768px) {
  .action-buttons {
    flex-direction: row;
  }
  .action-buttons button {
    flex: 1;
  }
}
</style>
