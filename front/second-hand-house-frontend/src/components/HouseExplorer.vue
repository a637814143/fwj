<template>
  <section class="house-explorer">
    <form class="filters" @submit.prevent="submitFilters">
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

    <section v-if="hasRecommendations" class="recommendations">
      <div class="recommendation">
        <h3>{{ t('explorer.recommendations.sellers.title') }}</h3>
        <ul>
          <li v-for="seller in recommendations.sellers" :key="`seller-${seller.username}`">
            <strong>{{ seller.displayName }}</strong>
            <span class="username">@{{ seller.username }}</span>
            <span class="score">
              {{ t('explorer.recommendations.sellers.score', { score: seller.reputationScore ?? '—' }) }}
            </span>
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

    <PricePredictor :api-base-url="apiBaseUrl" />

    <div v-if="loading" class="loading">{{ t('explorer.states.loading') }}</div>
    <div v-else-if="!houses || houses.length === 0" class="empty">{{ t('explorer.states.empty') }}</div>

    <div v-else class="house-grid">
      <article v-for="house in houses" :key="house.id" class="house-card">
        <div class="status" :class="statusClass(house)">{{ statusLabel(house) }}</div>
        <div class="cover" v-if="coverImage(house)">
          <img :src="coverImage(house)" :alt="house.title" loading="lazy" />
        </div>
        <div class="cover placeholder" v-else>
          <span>{{ t('explorer.labels.noImage') }}</span>
        </div>
        <div class="details">
          <header>
            <h3>{{ house.title }}</h3>
            <p class="address">{{ house.address }}</p>
          </header>
          <div class="pricing">
            <div class="pricing-item">
              <span class="label">{{ t('explorer.labels.fullPrice') }}</span>
              <strong>￥{{ formatCurrency(house.price) }}</strong>
            </div>
            <div class="pricing-item" v-if="house.downPayment">
              <span class="label">{{ t('explorer.labels.downPayment') }}</span>
              <strong>￥{{ formatCurrency(house.downPayment) }}</strong>
            </div>
            <div class="pricing-item" v-if="house.installmentMonthlyPayment">
              <span class="label">{{ t('explorer.labels.installment') }}</span>
              <strong>
                ￥{{ formatCurrency(house.installmentMonthlyPayment) }}
                <small v-if="house.installmentMonths">
                  {{ t('explorer.labels.installmentMonths', { count: house.installmentMonths }) }}
                </small>
              </strong>
            </div>
          </div>
          <p class="description" v-if="house.description">{{ house.description }}</p>
          <dl class="meta">
            <div>
              <dt>{{ t('explorer.labels.area') }}</dt>
              <dd>{{ formatNumber(house.area) }} ㎡</dd>
            </div>
            <div>
              <dt>{{ t('explorer.labels.listingDate') }}</dt>
              <dd>{{ formatDate(house.listingDate) }}</dd>
            </div>
            <div>
              <dt>{{ t('explorer.labels.seller') }}</dt>
              <dd>{{ sellerNameDisplay(house) }}（{{ sellerUsernameDisplay(house) }}）</dd>
            </div>
            <div>
              <dt>{{ t('explorer.labels.contact') }}</dt>
              <dd>{{ contactNumberDisplay(house) }}</dd>
            </div>
          </dl>
          <ul v-if="house.keywords && house.keywords.length" class="keyword-list">
            <li v-for="keyword in house.keywords" :key="`${house.id}-${keyword}`">#{{ keyword }}</li>
          </ul>
        </div>
        <footer class="card-actions">
          <template v-if="canOperate">
            <div class="payment" v-if="isApproved(house)">
              <label>{{ t('explorer.labels.paymentMethod') }}</label>
              <select v-model="selectedPayments[house.id]">
                <option value="FULL">{{ t('explorer.payment.full') }}</option>
                <option value="INSTALLMENT" :disabled="!house.installmentMonthlyPayment">
                  {{ t('explorer.payment.installment') }}
                </option>
              </select>
              <div
                v-if="selectedPayments[house.id] === 'INSTALLMENT'"
                class="installment-card-input"
              >
                <label :for="`installment-card-${house.id}`">
                  {{ t('explorer.labels.installmentCard') }}
                </label>
                <input
                  :id="`installment-card-${house.id}`"
                  type="text"
                  :value="cardNumberFor(house)"
                  inputmode="numeric"
                  maxlength="19"
                  :placeholder="t('explorer.inputs.installmentCard')"
                  @input="updateCardNumber(house, $event.target.value)"
                />
                <p v-if="hasCardError(house)" class="card-error">
                  {{ t('explorer.tips.installmentCardError') }}
                </p>
              </div>
            </div>
            <div class="action-buttons">
              <button
                type="button"
                class="contact"
                :disabled="contactDisabled || !isApproved(house)"
                @click="contactSeller(house)"
              >
                {{ t('explorer.actions.contactSeller') }}
              </button>
              <button
                class="reserve"
                :disabled="reservationDisabled(house)"
                @click="$emit('reserve', house)"
              >
                {{
                  isReservingCurrent(house)
                    ? t('explorer.actions.reserving')
                    : t('explorer.actions.reserve', { deposit: depositAmount(house) })
                }}
              </button>
              <button
                class="purchase"
                :disabled="purchaseDisabled || !isApproved(house)"
                @click="purchase(house)"
              >
                {{ purchaseDisabled ? t('explorer.actions.processing') : t('explorer.actions.purchase') }}
              </button>
            </div>
            <p v-if="house.reservationActive" class="verification-tip">
              {{ reservationNotice(house) }}
            </p>
            <p v-else-if="requiresVerification" class="verification-tip">
              {{ t('explorer.tips.requireVerification') }}
            </p>
            <p v-else-if="house.status === 'SOLD'" class="verification-tip">
              {{ t('explorer.tips.soldOut') }}
            </p>
            <p v-else-if="!isApproved(house)" class="verification-tip">
              {{ t('explorer.tips.awaitingApproval') }}
            </p>
          </template>
          <span v-else class="hint">{{ t('explorer.tips.loginAsBuyer') }}</span>
        </footer>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, inject, reactive, watch, ref, onMounted, onBeforeUnmount } from 'vue';
import PricePredictor from './PricePredictor.vue';

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
  }
});

const emit = defineEmits(['search', 'reserve', 'purchase', 'contact-seller']);

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
  maxArea: ''
});

const historyStorageKey = 'house-search-history';
const historyVisible = ref(false);
const searchHistory = ref([]);
const searchContainerRef = ref(null);
const keywordInputRef = ref(null);

const selectedPayments = reactive({});
const installmentCards = reactive({});
const cardErrors = reactive({});

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
  },
  { immediate: true, deep: true }
);

watch(
  () => props.houses,
  (list) => {
    const ids = new Set();
    (list ?? []).forEach((house) => {
      if (!house?.id) {
        return;
      }
      const key = String(house.id);
      ids.add(key);
      if (!selectedPayments[key]) {
        selectedPayments[key] = 'FULL';
      }
      if (selectedPayments[key] === 'INSTALLMENT' && !house.installmentMonthlyPayment) {
        selectedPayments[key] = 'FULL';
      }
      if (!(key in installmentCards)) {
        installmentCards[key] = '';
      }
      if (!(key in cardErrors)) {
        cardErrors[key] = false;
      }
      if (selectedPayments[key] !== 'INSTALLMENT') {
        cardErrors[key] = false;
      }
    });
    Object.keys(selectedPayments).forEach((key) => {
      if (!ids.has(String(key))) {
        delete selectedPayments[key];
      }
    });
    Object.keys(installmentCards).forEach((key) => {
      if (!ids.has(String(key))) {
        delete installmentCards[key];
        delete cardErrors[key];
      }
    });
  },
  { immediate: true }
);

watch(
  selectedPayments,
  (methods) => {
    Object.entries(methods).forEach(([key, method]) => {
      if (method !== 'INSTALLMENT') {
        cardErrors[key] = false;
      }
    });
  },
  { deep: true }
);

const submitFilters = () => {
  historyVisible.value = false;
  recordHistory(localFilters.keyword);
  emit('search', { ...localFilters });
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
  historyVisible.value = false;
  emit('search', { ...localFilters });
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

const sanitizeCardNumber = (value) => (value == null ? '' : String(value).replace(/\D/g, '').slice(0, 19));

const cardKey = (house) => {
  if (!house || house.id == null) {
    return '';
  }
  return String(house.id);
};

const cardNumberFor = (house) => {
  const key = cardKey(house);
  return key ? installmentCards[key] ?? '' : '';
};

const updateCardNumber = (house, value) => {
  const key = cardKey(house);
  if (!key) {
    return;
  }
  const sanitized = sanitizeCardNumber(value);
  installmentCards[key] = sanitized;
  if (sanitized.length === 19) {
    cardErrors[key] = false;
  }
};

const hasCardError = (house) => {
  const key = cardKey(house);
  return key ? Boolean(cardErrors[key]) : false;
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

const purchase = (house) => {
  if (!isApproved(house)) {
    return;
  }
  const key = cardKey(house);
  const method = (key && selectedPayments[key] !== undefined)
    ? selectedPayments[key]
    : selectedPayments[house?.id] || 'FULL';
  if (method === 'INSTALLMENT') {
    const cardNumber = cardNumberFor(house);
    if (cardNumber.length !== 19) {
      if (key) {
        cardErrors[key] = true;
      }
      return;
    }
    if (key) {
      cardErrors[key] = false;
    }
    emit('purchase', { house, paymentMethod: method, installmentCardNumber: cardNumber });
    return;
  }
  if (key) {
    cardErrors[key] = false;
  }
  emit('purchase', { house, paymentMethod: method });
};

const statusLabel = (house) => statusLabels.value[house?.status] ?? t('statuses.pending');

const statusClass = (house) => {
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
  background: linear-gradient(135deg, rgba(241, 245, 249, 0.8), rgba(224, 231, 255, 0.85));
  border-radius: var(--radius-lg);
  padding: 1.9rem;
  border: 1px solid rgba(148, 163, 184, 0.28);
  box-shadow: 0 22px 48px rgba(148, 163, 184, 0.25);
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
  border: 1px solid rgba(148, 163, 184, 0.38);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
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
  color: rgba(100, 116, 139, 0.75);
}

.history-toggle {
  border: none;
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
  border-radius: var(--radius-pill);
  padding: 0.45rem 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: background var(--transition-base), color var(--transition-base);
}

.history-toggle:hover {
  background: rgba(37, 99, 235, 0.22);
}

.search-submit {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.65rem 1.6rem;
  background: var(--gradient-primary);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 18px 35px rgba(37, 99, 235, 0.24);
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.search-submit:hover {
  transform: translateY(-1px);
  box-shadow: 0 24px 45px rgba(37, 99, 235, 0.28);
}

.history-dropdown {
  position: absolute;
  top: calc(100% + 0.35rem);
  left: 0;
  width: min(520px, 100%);
  background: rgba(255, 255, 255, 0.96);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(148, 163, 184, 0.3);
  box-shadow: var(--shadow-md);
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
  color: var(--color-text-muted);
  cursor: pointer;
  transition: background var(--transition-base), color var(--transition-base);
}

.history-dropdown li button:hover {
  background: rgba(37, 99, 235, 0.14);
  color: #1d4ed8;
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
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(255, 255, 255, 0.85);
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
  box-shadow: 0 18px 35px rgba(37, 99, 235, 0.24);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.actions button.secondary {
  background: rgba(255, 255, 255, 0.75);
  color: var(--color-text-strong);
  border: 1px solid rgba(148, 163, 184, 0.35);
  box-shadow: none;
}

.actions button:hover {
  transform: translateY(-1px);
  box-shadow: 0 24px 45px rgba(37, 99, 235, 0.28);
}

.recommendations {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 1.1rem;
}

.recommendation {
  background: var(--gradient-surface);
  border-radius: var(--radius-lg);
  padding: 1.25rem;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.12);
  border: 1px solid rgba(148, 163, 184, 0.25);
  backdrop-filter: blur(calc(var(--glass-blur) / 2));
}

.recommendation h3 {
  margin: 0 0 0.65rem;
}

.recommendation ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.recommendation li {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  color: var(--color-text-muted);
}

.recommendation .username {
  font-size: 0.85rem;
  color: var(--color-text-soft);
}

.recommendation .score {
  font-size: 0.85rem;
  color: #2563eb;
  font-weight: 600;
}

.loading,
.empty {
  padding: 2rem;
  border-radius: var(--radius-lg);
  border: 1px dashed rgba(148, 163, 184, 0.4);
  color: var(--color-text-muted);
  text-align: center;
}

.house-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.25rem;
}

.house-card {
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-xl);
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.2);
  box-shadow: 0 22px 45px rgba(148, 163, 184, 0.22);
}

.status {
  padding: 0.45rem 0.9rem;
  font-weight: 600;
  font-size: 0.85rem;
  letter-spacing: 0.02em;
}

.status.pending {
  background: rgba(234, 179, 8, 0.15);
  color: #b45309;
}

.status.approved {
  background: rgba(16, 185, 129, 0.15);
  color: #047857;
}

.status.rejected {
  background: rgba(239, 68, 68, 0.15);
  color: #b91c1c;
}

.status.sold {
  background: rgba(148, 163, 184, 0.18);
  color: var(--color-text-strong);
}

:global(body[data-theme='dark']) :deep(.house-explorer .explorer-header) {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.9), rgba(15, 23, 42, 0.92));
  border-color: color-mix(in srgb, rgba(148, 163, 184, 0.4) 65%, transparent);
  box-shadow: 0 26px 56px rgba(2, 6, 23, 0.55);
}

:global(body[data-theme='dark']) :deep(.house-explorer .status.pending) {
  background: rgba(253, 224, 71, 0.16);
  color: #facc15;
}

:global(body[data-theme='dark']) :deep(.house-explorer .status.approved) {
  background: rgba(74, 222, 128, 0.16);
  color: #bbf7d0;
}

:global(body[data-theme='dark']) :deep(.house-explorer .status.rejected) {
  background: rgba(248, 113, 113, 0.18);
  color: #fecaca;
}

:global(body[data-theme='dark']) :deep(.house-explorer .status.sold) {
  background: rgba(148, 163, 184, 0.22);
  color: rgba(226, 232, 240, 0.9);
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

.cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(226, 232, 240, 0.6);
  color: var(--color-text-muted);
  font-size: 0.95rem;
}

.details {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  padding: 1.25rem;
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
  background: rgba(37, 99, 235, 0.12);
  color: #2563eb;
  padding: 0.3rem 0.65rem;
  border-radius: var(--radius-pill);
  font-size: 0.85rem;
}

.card-actions {
  margin-top: auto;
  padding: 1.15rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(248, 250, 252, 0.85);
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

.installment-card-input {
  display: grid;
  gap: 0.35rem;
  margin-top: 0.45rem;
}

.installment-card-input label {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.installment-card-input input {
  padding: 0.55rem 0.75rem;
  border-radius: var(--radius-md);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.9);
}

.installment-card-input input:focus {
  outline: none;
  border-color: rgba(99, 102, 241, 0.55);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
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

.action-buttons button {
  padding: 0.6rem 0.9rem;
  border-radius: var(--radius-pill);
  border: none;
  font-weight: 600;
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.action-buttons .contact {
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
}

.action-buttons .reserve {
  background: rgba(250, 204, 21, 0.18);
  color: #b45309;
}

.action-buttons .purchase {
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: 0 15px 30px rgba(37, 99, 235, 0.18);
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

@media (min-width: 768px) {
  .action-buttons {
    flex-direction: row;
  }
  .action-buttons button {
    flex: 1;
  }
}
</style>
