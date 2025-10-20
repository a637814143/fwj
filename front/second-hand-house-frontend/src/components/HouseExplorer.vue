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
  gap: 1.5rem;
}

.explorer-header {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background: linear-gradient(135deg, #f8fafc, #eef2ff);
  border-radius: 1rem;
  padding: 1.5rem;
}

.filters {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.filters input[type='search'] {
  padding: 0.75rem 1rem;
  border-radius: 0.75rem;
  border: 1px solid #cbd5f5;
  background: #fff;
}

.range {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.75rem;
}

.range label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-weight: 600;
  color: #1e293b;
}

.range input {
  padding: 0.5rem 0.75rem;
  border-radius: 0.65rem;
  border: 1px solid #cbd5f5;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.actions button {
  padding: 0.6rem 1.2rem;
  border-radius: 0.75rem;
  border: none;
  font-weight: 600;
  cursor: pointer;
  background: #2563eb;
  color: #fff;
}

.actions button.secondary {
  background: #e2e8f0;
  color: #1e293b;
}

.recommendations {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.recommendation {
  background: #fff;
  border-radius: 1rem;
  padding: 1rem;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.recommendation h3 {
  margin: 0 0 0.5rem;
}

.recommendation ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.recommendation li {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  color: #334155;
}

.recommendation .username {
  font-size: 0.85rem;
  color: #64748b;
}

.recommendation .score {
  font-size: 0.85rem;
  color: #2563eb;
}

.loading,
.empty {
  padding: 2rem;
  text-align: center;
  border-radius: 1rem;
  background: #fff;
  box-shadow: inset 0 0 0 1px #e2e8f0;
  color: #475569;
}

.house-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.25rem;
}

.house-card {
  position: relative;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 1.25rem;
  overflow: hidden;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
}

.status {
  position: absolute;
  top: 1rem;
  left: 1rem;
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #fff;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.2);
  z-index: 1;
}

.status.approved {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}

.status.pending {
  background: linear-gradient(135deg, #facc15, #f97316);
}

.status.rejected {
  background: linear-gradient(135deg, #f87171, #ef4444);
}

.cover {
  height: 200px;
  background: #e2e8f0;
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
  color: #475569;
  font-weight: 600;
}

.details {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.25rem;
}

.details header {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.details h3 {
  margin: 0;
  font-size: 1.2rem;
  color: #1e293b;
}

.details .address {
  margin: 0;
  color: #64748b;
  font-size: 0.9rem;
}

.pricing {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.pricing-item {
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  border-radius: 0.85rem;
  padding: 0.75rem 1rem;
  min-width: 140px;
  box-shadow: inset 0 0 0 1px #e2e8f0;
}

.pricing-item .label {
  font-size: 0.8rem;
  color: #64748b;
}

.pricing-item strong {
  font-size: 1rem;
  color: #1e293b;
}

.pricing-item small {
  margin-left: 0.25rem;
  color: #475569;
}

.description {
  margin: 0;
  color: #475569;
  line-height: 1.5;
}

.meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 0.75rem;
  margin: 0;
}

.meta dt {
  margin: 0;
  color: #64748b;
  font-size: 0.8rem;
}

.meta dd {
  margin: 0;
  font-weight: 600;
  color: #1e293b;
}

.keyword-list {
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin: 0;
  padding: 0;
}

.keyword-list li {
  padding: 0.25rem 0.75rem;
  background: #eef2ff;
  color: #4338ca;
  border-radius: 999px;
  font-size: 0.8rem;
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.25rem;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}

.payment {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.9rem;
}

.payment select {
  padding: 0.5rem 0.75rem;
  border-radius: 0.65rem;
  border: 1px solid #cbd5f5;
  background: #fff;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.action-buttons button {
  flex: 1 1 120px;
  padding: 0.65rem 1rem;
  border-radius: 0.85rem;
  border: none;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.action-buttons button.contact {
  background: #e0f2fe;
  color: #0369a1;
}

.action-buttons button.reserve {
  background: #fef3c7;
  color: #b45309;
}

.action-buttons button.purchase {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
}

.action-buttons button:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}

.action-buttons button:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 18px rgba(37, 99, 235, 0.25);
}

.verification-tip {
  margin: 0;
  font-size: 0.85rem;
  color: #f97316;
}

.hint {
  color: #64748b;
  font-size: 0.9rem;
}

@media (max-width: 640px) {
  .house-grid {
    grid-template-columns: 1fr;
  }
}
</style>
