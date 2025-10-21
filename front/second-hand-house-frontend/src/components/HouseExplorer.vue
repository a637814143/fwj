<template>
  <section class="house-explorer">
    <header class="explorer-header">
      <div>
        <h2>购房首页</h2>
        <p>按关键字、价格或面积筛选房源，系统将展示通过审核的优质房源并支持一键预定与支付。</p>
      </div>
      <form class="filters" @submit.prevent="submitFilters">
        <input v-model.trim="localFilters.keyword" type="search" placeholder="关键词（标题、地址、描述）" />
        <div class="range">
          <label>
            最低价格
            <input v-model.number="localFilters.minPrice" type="number" min="0" step="0.01" placeholder="万元" />
          </label>
          <label>
            最高价格
            <input v-model.number="localFilters.maxPrice" type="number" min="0" step="0.01" placeholder="万元" />
          </label>
          <label>
            最小面积
            <input v-model.number="localFilters.minArea" type="number" min="0" step="0.1" placeholder="㎡" />
          </label>
          <label>
            最大面积
            <input v-model.number="localFilters.maxArea" type="number" min="0" step="0.1" placeholder="㎡" />
          </label>
        </div>
        <div class="actions">
          <button type="submit">筛选房源</button>
          <button type="button" class="secondary" @click="resetFilters">重置</button>
        </div>
      </form>
    </header>

    <section v-if="hasRecommendations" class="recommendations">
      <div class="recommendation">
        <h3>优质卖家</h3>
        <ul>
          <li v-for="seller in recommendations.sellers" :key="`seller-${seller.username}`">
            <strong>{{ seller.displayName }}</strong>
            <span class="username">@{{ seller.username }}</span>
            <span class="score">信誉分 {{ seller.reputationScore }}</span>
          </li>
        </ul>
      </div>
      <div class="recommendation">
        <h3>优质买家</h3>
        <ul>
          <li v-for="buyer in recommendations.buyers" :key="`buyer-${buyer.username}`">
            <strong>{{ buyer.displayName }}</strong>
            <span class="username">@{{ buyer.username }}</span>
            <span class="score">信誉分 {{ buyer.reputationScore }}</span>
          </li>
        </ul>
      </div>
    </section>

    <div v-if="loading" class="loading">房源数据加载中...</div>
    <div v-else-if="!houses || houses.length === 0" class="empty">暂未查询到符合条件的房源。</div>

    <div v-else class="house-grid">
      <article v-for="house in houses" :key="house.id" class="house-card">
        <div class="status" :class="statusClass(house)">{{ statusLabel(house) }}</div>
        <div class="cover" v-if="coverImage(house)">
          <img :src="coverImage(house)" :alt="house.title" loading="lazy" />
        </div>
        <div class="cover placeholder" v-else>
          <span>暂无图片</span>
        </div>
        <div class="details">
          <header>
            <h3>{{ house.title }}</h3>
            <p class="address">{{ house.address }}</p>
          </header>
          <div class="pricing">
            <div class="pricing-item">
              <span class="label">全款价格</span>
              <strong>￥{{ formatNumber(house.price) }} 万</strong>
            </div>
            <div class="pricing-item" v-if="house.installmentMonthlyPayment">
              <span class="label">分期（月供）</span>
              <strong>
                ￥{{ formatNumber(house.installmentMonthlyPayment) }} 万
                <small v-if="house.installmentMonths">× {{ house.installmentMonths }} 期</small>
              </strong>
            </div>
          </div>
          <p class="description" v-if="house.description">{{ house.description }}</p>
          <dl class="meta">
            <div>
              <dt>面积</dt>
              <dd>{{ formatNumber(house.area) }} ㎡</dd>
            </div>
            <div>
              <dt>挂牌日期</dt>
              <dd>{{ formatDate(house.listingDate) }}</dd>
            </div>
            <div>
              <dt>卖家</dt>
              <dd>{{ sellerNameDisplay(house) }}（{{ sellerUsernameDisplay(house) }}）</dd>
            </div>
            <div>
              <dt>联系方式</dt>
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
              <label for="payment-select">支付方式</label>
              <select v-model="selectedPayments[house.id]">
                <option value="FULL">全款支付</option>
                <option value="INSTALLMENT" :disabled="!house.installmentMonthlyPayment">
                  分期付款
                </option>
              </select>
            </div>
            <div class="action-buttons">
              <button
                type="button"
                class="contact"
                :disabled="contactDisabled || !isApproved(house)"
                @click="contactSeller(house)"
              >
                联系卖家
              </button>
              <button
                class="reserve"
                :disabled="reservationDisabled(house)"
                @click="$emit('reserve', house)"
              >
                {{
                  isReservingCurrent(house)
                    ? '预定中...'
                    : `预定（定金 ${depositAmount(house)} 万）`
                }}
              </button>
              <button
                class="purchase"
                :disabled="purchaseDisabled || !isApproved(house)"
                @click="purchase(house)"
              >
                {{ purchaseDisabled ? '处理中...' : '立即购买' }}
              </button>
            </div>
            <p v-if="requiresVerification" class="verification-tip">
              完成实名认证后才能查看完整信息并进行交易。
            </p>
            <p v-else-if="!isApproved(house)" class="verification-tip">
              该房源尚待管理员审核，通过后方可预定或购买。
            </p>
          </template>
          <span v-else class="hint">登录买家账号后可进行预定或购买</span>
        </footer>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, watch } from 'vue';

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
  }
});

const emit = defineEmits(['search', 'reserve', 'purchase', 'contact-seller']);

const listingStatusLabels = {
  PENDING_REVIEW: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回'
};

const localFilters = reactive({
  keyword: '',
  minPrice: '',
  maxPrice: '',
  minArea: '',
  maxArea: ''
});

const selectedPayments = reactive({});

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
      ids.add(String(house.id));
      if (!selectedPayments[house.id]) {
        selectedPayments[house.id] = 'FULL';
      }
      if (selectedPayments[house.id] === 'INSTALLMENT' && !house.installmentMonthlyPayment) {
        selectedPayments[house.id] = 'FULL';
      }
    });
    Object.keys(selectedPayments).forEach((key) => {
      if (!ids.has(String(key))) {
        delete selectedPayments[key];
      }
    });
  },
  { immediate: true }
);

const submitFilters = () => {
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
  emit('search', { ...localFilters });
};

const coverImage = (house) => {
  if (!house || !Array.isArray(house.imageUrls)) {
    return '';
  }
  return house.imageUrls.find((url) => url && url.trim().length > 0) ?? '';
};

const formatNumber = (value) => {
  if (value == null || value === '') {
    return '0';
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '0';
  }
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });
};

const formatDate = (value) => {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleDateString('zh-CN');
};

const depositAmount = (house) => {
  const price = Number(house?.price ?? 0);
  if (!Number.isFinite(price)) {
    return '0.00';
  }
  return (price * 0.1).toFixed(2);
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
  if (!props.reservationLoading) {
    return false;
  }
  return !isReservingCurrent(house);
};

const purchase = (house) => {
  if (!isApproved(house)) {
    return;
  }
  const method = selectedPayments[house.id] || 'FULL';
  emit('purchase', { house, paymentMethod: method });
};

const statusLabel = (house) => listingStatusLabels[house?.status] ?? '待审核';

const statusClass = (house) => {
  switch (house?.status) {
    case 'APPROVED':
      return 'approved';
    case 'REJECTED':
      return 'rejected';
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
  gap: 1.1rem;
}

.filters input[type='search'] {
  padding: 0.85rem 1.1rem;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
}

.filters input[type='search']::placeholder {
  color: rgba(100, 116, 139, 0.8);
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
  padding: 2.2rem 1.5rem;
  text-align: center;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(226, 232, 240, 0.65);
  color: var(--color-text-muted);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.6);
}

.house-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.4rem;
}

.house-card {
  position: relative;
  display: flex;
  flex-direction: column;
  background: var(--gradient-surface);
  border-radius: calc(var(--radius-lg) + 0.2rem);
  overflow: hidden;
  box-shadow: 0 28px 60px rgba(15, 23, 42, 0.18);
  border: 1px solid rgba(148, 163, 184, 0.28);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.house-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 36px 80px rgba(15, 23, 42, 0.22);
}

.status {
  position: absolute;
  top: 1.1rem;
  left: 1.1rem;
  padding: 0.35rem 0.85rem;
  border-radius: var(--radius-pill);
  font-size: 0.78rem;
  font-weight: 600;
  color: #fff;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  box-shadow: 0 14px 24px rgba(15, 23, 42, 0.2);
  z-index: 1;
}

.status.approved {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}

.status.pending {
  background: linear-gradient(135deg, #fbbf24, #f97316);
}

.status.rejected {
  background: linear-gradient(135deg, #f87171, #ef4444);
}

.cover {
  height: 210px;
  background: linear-gradient(135deg, rgba(226, 232, 240, 0.8), rgba(203, 213, 225, 0.6));
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover.placeholder {
  color: var(--color-text-muted);
  font-weight: 600;
}

.details {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  padding: 1.35rem 1.4rem;
}

.details header {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.details h3 {
  margin: 0;
  font-size: 1.25rem;
  color: var(--color-text-strong);
}

.details .address {
  margin: 0;
  color: var(--color-text-soft);
  font-size: 0.92rem;
}

.pricing {
  display: flex;
  flex-wrap: wrap;
  gap: 0.9rem;
}

.pricing-item {
  display: flex;
  flex-direction: column;
  background: rgba(248, 250, 252, 0.8);
  border-radius: var(--radius-md);
  padding: 0.8rem 1rem;
  min-width: 150px;
  border: 1px solid rgba(148, 163, 184, 0.25);
}

.pricing-item .label {
  font-size: 0.82rem;
  color: var(--color-text-soft);
}

.pricing-item strong {
  font-size: 1.05rem;
  color: var(--color-text-strong);
}

.pricing-item small {
  margin-left: 0.25rem;
  color: var(--color-text-muted);
}

.description {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.55;
}

.meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 0.85rem;
  margin: 0;
}

.meta div {
  background: rgba(241, 245, 249, 0.75);
  border-radius: var(--radius-md);
  padding: 0.75rem 0.9rem;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.meta dt {
  margin: 0;
  color: var(--color-text-soft);
  font-size: 0.82rem;
}

.meta dd {
  margin: 0;
  font-weight: 600;
  color: var(--color-text-strong);
}

.keyword-list {
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  margin: 0;
  padding: 0;
}

.keyword-list li {
  padding: 0.3rem 0.8rem;
  background: rgba(224, 231, 255, 0.75);
  color: #4338ca;
  border-radius: var(--radius-pill);
  font-size: 0.82rem;
  font-weight: 600;
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
  padding: 1.4rem;
  border-top: 1px solid rgba(226, 232, 240, 0.7);
  background: rgba(248, 250, 252, 0.85);
}

.payment {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.92rem;
}

.payment select {
  padding: 0.6rem 0.85rem;
  border-radius: var(--radius-md);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.9);
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 0.85rem;
}

.action-buttons button {
  flex: 1 1 130px;
  padding: 0.7rem 1.1rem;
  border-radius: var(--radius-pill);
  border: none;
  font-weight: 600;
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base),
    background var(--transition-base);
}

.action-buttons button.contact {
  background: rgba(224, 242, 254, 0.9);
  color: #0369a1;
  border: 1px solid rgba(14, 165, 233, 0.3);
}

.action-buttons button.reserve {
  background: rgba(254, 243, 199, 0.9);
  color: #b45309;
  border: 1px solid rgba(234, 179, 8, 0.28);
}

.action-buttons button.purchase {
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: 0 20px 38px rgba(37, 99, 235, 0.28);
}

.action-buttons button:disabled {
  background: rgba(226, 232, 240, 0.9);
  color: rgba(148, 163, 184, 0.9);
  cursor: not-allowed;
  box-shadow: none;
}

.action-buttons button:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 26px 44px rgba(37, 99, 235, 0.28);
}

.verification-tip {
  margin: 0;
  font-size: 0.88rem;
  color: #b45309;
  background: rgba(254, 215, 170, 0.35);
  border-radius: var(--radius-md);
  padding: 0.6rem 0.85rem;
}

.hint {
  color: var(--color-text-soft);
  font-size: 0.92rem;
}

@media (max-width: 640px) {
  .house-grid {
    grid-template-columns: 1fr;
  }
}
</style>
