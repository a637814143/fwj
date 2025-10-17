<template>
  <section class="house-explorer">
    <header class="explorer-header">
      <div>
        <h2>购房首页</h2>
        <p>按关键字、价格或面积筛选房源，支持一键预定并支付定金。</p>
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
        <div class="cover" v-if="coverImage(house)">
          <img :src="coverImage(house)" :alt="house.title" loading="lazy" />
        </div>
        <div class="cover placeholder" v-else>
          <span>无图片</span>
        </div>
        <div class="details">
          <header>
            <h3>{{ house.title }}</h3>
            <span class="price">￥{{ formatNumber(house.price) }} 万</span>
          </header>
          <p class="description" v-if="house.description">{{ house.description }}</p>
          <dl>
            <div>
              <dt>地址</dt>
              <dd>{{ house.address }}</dd>
            </div>
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
        </div>
        <footer class="card-actions">
          <template v-if="canOperate">
            <button
              type="button"
              class="contact"
              :disabled="contactDisabled"
              @click="contactSeller(house)"
            >
              联系卖家
            </button>
            <button
              class="reserve"
              :disabled="reservationDisabled()"
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
              :disabled="purchaseDisabled"
              @click="$emit('purchase', house)"
            >
              {{ purchaseDisabled ? '处理中...' : '立即购买' }}
            </button>
            <p v-if="requiresVerification" class="verification-tip">
              完成实名认证后才能查看完整信息并进行交易。
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

const localFilters = reactive({
  keyword: '',
  minPrice: '',
  maxPrice: '',
  minArea: '',
  maxArea: ''
});

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

const reservationDisabled = () => {
  if (!canOperate.value) {
    return true;
  }
  if (requiresVerification.value) {
    return true;
  }
  return Boolean(props.reservationLoading);
};
</script>

<style scoped>
.house-explorer {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.explorer-header {
  background: #fff;
  border-radius: 1rem;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.08);
  display: grid;
  gap: 1rem;
}

.explorer-header h2 {
  margin: 0;
  color: #1e293b;
}

.explorer-header p {
  margin: 0.35rem 0 0;
  color: #475569;
}

.filters {
  display: grid;
  gap: 0.75rem;
}

.filters input[type='search'] {
  border: 1px solid #cbd5f5;
  border-radius: 0.75rem;
  padding: 0.65rem 1rem;
  font-size: 0.95rem;
}

.range {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.range label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.85rem;
  color: #475569;
}

.range input {
  border: 1px solid #cbd5f5;
  border-radius: 0.75rem;
  padding: 0.5rem 0.75rem;
}

.actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.actions button {
  border: none;
  border-radius: 999px;
  padding: 0.55rem 1.5rem;
  font-weight: 600;
  cursor: pointer;
  background: #2563eb;
  color: #fff;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.actions button:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(37, 99, 235, 0.25);
}

.actions .secondary {
  background: #e0e7ff;
  color: #1e3a8a;
}

.recommendations {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.recommendation {
  background: linear-gradient(180deg, #fff, #f8fafc);
  border-radius: 1rem;
  padding: 1rem 1.25rem;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.08);
}

.recommendation h3 {
  margin: 0 0 0.75rem;
  color: #1e293b;
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
  gap: 0.25rem;
  padding: 0.75rem;
  border-radius: 0.75rem;
  background: rgba(59, 130, 246, 0.08);
}

.recommendation .username {
  font-size: 0.8rem;
  color: #475569;
}

.recommendation .score {
  font-size: 0.85rem;
  font-weight: 600;
  color: #1d4ed8;
}

.loading,
.empty {
  background: #f8fafc;
  border-radius: 0.75rem;
  padding: 1.25rem;
  text-align: center;
  color: #475569;
}

.house-grid {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.house-card {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 1rem;
  overflow: hidden;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.1);
}

.cover {
  height: 180px;
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

.cover.placeholder span {
  color: #64748b;
}

.details {
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.details header {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: baseline;
}

.details h3 {
  margin: 0;
  color: #1f2937;
}

.price {
  font-weight: 700;
  color: #16a34a;
}

.description {
  margin: 0;
  color: #475569;
  font-size: 0.92rem;
}

.details dl {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 0.5rem 1rem;
  margin: 0;
}

.details dt {
  font-size: 0.75rem;
  color: #94a3b8;
}

.details dd {
  margin: 0;
  font-size: 0.9rem;
  color: #1f2937;
}

.card-actions {
  padding: 1rem 1.25rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.card-actions button {
  border: none;
  border-radius: 0.75rem;
  padding: 0.65rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.contact {
  background: #f8fafc;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}

.contact:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(59, 130, 246, 0.2);
}

.card-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  transform: none;
  box-shadow: none;
}

.reserve {
  background: #f97316;
  color: #fff;
}

.reserve:not(:disabled):hover,
.purchase:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(249, 115, 22, 0.25);
}

.purchase {
  background: #2563eb;
  color: #fff;
}

.hint {
  color: #64748b;
  font-size: 0.9rem;
}

.verification-tip {
  margin: 0;
  font-size: 0.8rem;
  color: #b91c1c;
}
</style>
