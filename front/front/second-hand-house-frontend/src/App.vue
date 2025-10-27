<template>
  <div class="app-shell">
    <div class="orb orb--one" aria-hidden="true"></div>
    <div class="orb orb--two" aria-hidden="true"></div>

    <nav class="top-nav">
      <div class="brand">
        <div class="brand-mark">🏙️</div>
        <div class="brand-copy">
          <strong>Atlas Housing</strong>
          <span>二手房屋智慧管理台</span>
        </div>
      </div>
      <div class="nav-actions">
        <div v-if="currentUser" class="account-chip">
          <div class="account-meta">
            <span class="account-role">{{ roleLabels[currentUser.role] }}</span>
            <span class="account-name">{{ currentUser.displayName }}</span>
          </div>
          <button type="button" class="chip-action" @click="handleLogout">安全退出</button>
        </div>
        <div v-else class="nav-actions__guest">欢迎登录体验</div>
      </div>
    </nav>

    <header class="hero">
      <div class="hero-copy">
        <span class="eyebrow">全链路居住资产管控</span>
        <h1>以苹果级体验呈现房屋运营数据</h1>
        <p>
          以灵动的玻璃质感界面串联房源发布、订单追踪、实名风控与钱包账单，
          让每一次操作都精准、优雅且高效。
        </p>
        <ul class="hero-points">
          <li>实时同步的房源与订单概览</li>
          <li>多角色协同与身份认证守护</li>
          <li>深色玻璃美学与 Flex 布局自适应</li>
        </ul>
      </div>
      <div class="hero-spotlight">
        <div class="hero-stat">
          <span class="hero-stat__label">在售房源</span>
          <strong class="hero-stat__value">{{ totalHouses }}</strong>
          <span class="hero-stat__meta">实时更新</span>
        </div>
        <div class="hero-stat">
          <span class="hero-stat__label">累计订单</span>
          <strong class="hero-stat__value">{{ totalOrders }}</strong>
          <span class="hero-stat__meta">买卖全链路</span>
        </div>
        <div v-if="wallet" class="hero-stat accent">
          <span class="hero-stat__label">钱包余额</span>
          <strong class="hero-stat__value">￥{{ formattedWalletBalance }}</strong>
          <span class="hero-stat__meta">账户实时到账</span>
        </div>
      </div>
    </header>

    <div class="message-stack">
      <div v-if="messages.success" class="message message--success">{{ messages.success }}</div>
      <div v-if="messages.error" class="message message--danger">
        <strong>提示：</strong> {{ messages.error }}
      </div>
    </div>

    <section v-if="!currentUser" class="guest-layout">
      <div class="guest-intro">
        <h2>一体化的二手房资源中台</h2>
        <p>
          登录后即可管理房源、监控订单流转、快速完成实名认证与钱包充值。
          精致的视觉与流畅的交互让运营效率全面提升。
        </p>
        <div class="guest-highlights">
          <div class="guest-card">
            <span class="guest-card__title">实时发布</span>
            <p>Flex 式卡片列表展示房源亮点，长列表支持分页浏览。</p>
          </div>
          <div class="guest-card">
            <span class="guest-card__title">交易洞察</span>
            <p>订单阶段一目了然，订单进度与退款流程随时掌控。</p>
          </div>
          <div class="guest-card">
            <span class="guest-card__title">安全风控</span>
            <p>实名认证与钱包流水全链路追踪，保障信息安全。</p>
          </div>
        </div>
      </div>
      <div class="guest-auth">
        <RoleLogin :api-base-url="apiBaseUrl" @login-success="handleLoginSuccess" />
      </div>
    </section>

    <template v-else>
      <section class="overview">
        <article class="overview-card">
          <span class="overview-card__label">在售房源</span>
          <strong class="overview-card__value">{{ totalHouses }}</strong>
          <span class="overview-card__meta">实时更新的房源池</span>
        </article>
        <article class="overview-card">
          <span class="overview-card__label">累计订单</span>
          <strong class="overview-card__value">{{ totalOrders }}</strong>
          <span class="overview-card__meta">覆盖买卖双方的交易</span>
        </article>
        <article class="overview-card" :class="{ positive: isVerified }">
          <span class="overview-card__label">实名认证</span>
          <strong class="overview-card__value">{{ isVerified ? '已完成' : '待审核' }}</strong>
          <span class="overview-card__meta">保障交易安全</span>
        </article>
        <article class="overview-card accent" v-if="wallet">
          <span class="overview-card__label">钱包余额</span>
          <strong class="overview-card__value">￥{{ formattedWalletBalance }}</strong>
          <span class="overview-card__meta">充值与调账实时同步</span>
        </article>
      </section>

      <div class="dashboard">
        <div class="dashboard-main">
          <HouseSearchBar
            :loading="loading"
            :initial-keyword="searchFilters.keyword"
            @search="handleSearch"
          />
          <HouseList
            ref="houseListRef"
            :houses="houses"
            :loading="loading"
            :can-manage="canManageHouses"
            :current-user="currentUser"
            :orders-loading="ordersLoading"
            @edit="handleEdit"
            @remove="handleRemove"
            @purchase="handlePurchase"
            @view="handleViewHouse"
          />
          <BrowsingHistory
            :history="browsingHistory"
            @select="handleHistorySelect"
            @clear="clearBrowsingHistory"
          />
        </div>

        <aside class="dashboard-side">
          <RealNameVerification
            :api-base-url="apiBaseUrl"
            :current-user="currentUser"
            @verified="handleVerificationUpdate"
          />
          <HouseForm
            :initial-house="selectedHouse"
            :loading="loading"
            :can-manage="canManageHouses"
            :current-user="currentUser"
            @submit="handleSubmit"
            @cancel="handleCancel"
          />
          <WalletPanel
            :wallet="wallet"
            :loading="walletLoading"
            :current-user="currentUser"
            @top-up="handleTopUp"
          />
          <OrderHistory
            :orders="orders"
            :loading="ordersLoading"
            :current-user="currentUser"
            @request-return="handleRequestReturn"
            @update-progress="handleUpdateProgress"
          />
        </aside>
      </div>
    </template>

    <footer class="footer">
      <small>后端接口地址：{{ apiBaseUrl }}</small>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import axios from 'axios';
import HouseForm from './components/HouseForm.vue';
import HouseList from './components/HouseList.vue';
import RoleLogin from './components/RoleLogin.vue';
import WalletPanel from './components/WalletPanel.vue';
import OrderHistory from './components/OrderHistory.vue';
import HouseSearchBar from './components/HouseSearchBar.vue';
import BrowsingHistory from './components/BrowsingHistory.vue';
import RealNameVerification from './components/RealNameVerification.vue';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';
const houses = ref([]);
const loading = ref(false);
const selectedHouse = ref(null);
const currentUser = ref(null);
const wallet = ref(null);
const walletLoading = ref(false);
const orders = ref([]);
const ordersLoading = ref(false);
const messages = reactive({ error: '', success: '' });
const storageKey = 'secondhand-house-current-user';
const historyStoragePrefix = 'secondhand-house-history-';
const historyLimit = 10;
const houseListRef = ref(null);
const browsingHistory = ref([]);
const searchFilters = reactive({ keyword: '' });

const client = axios.create({
  baseURL: apiBaseUrl,
  headers: { 'Content-Type': 'application/json' }
});

const roleLabels = {
  SELLER: '卖家',
  BUYER: '买家',
  ADMIN: '系统管理员'
};

const progressStageLabels = {
  RESERVED: '预定',
  VIEWED: '已看房',
  BALANCE_PAID: '已支付尾款',
  HANDED_OVER: '已交房'
};

const isSeller = computed(() => currentUser.value?.role === 'SELLER');
const isBuyer = computed(() => currentUser.value?.role === 'BUYER');

const canManageHouses = computed(
  () => currentUser.value && ['SELLER', 'ADMIN'].includes(currentUser.value.role)
);

const totalHouses = computed(() => houses.value.length);
const totalOrders = computed(() => orders.value.length);
const walletBalance = computed(() => Number(wallet.value?.balance ?? 0));
const isVerified = computed(() => currentUser.value?.realNameVerified === true);

const formatCurrency = (value) =>
  Number(value ?? 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });

const formattedWalletBalance = computed(() => formatCurrency(walletBalance.value));

const normalizeHouseResponse = (house) => ({
  ...house,
  listingDate: house?.listingDate ?? '',
  floor: house?.floor ?? null,
  keywords: Array.isArray(house?.keywords) ? [...house.keywords] : [],
  contactNumber: house?.contactNumber ?? '',
  imageUrls: Array.isArray(house?.imageUrls) ? [...house.imageUrls] : []
});

const sanitizeImages = (images) =>
  Array.isArray(images)
    ? images
        .map((item) => (typeof item === 'string' ? item.trim() : ''))
        .filter((item) => item)
    : [];

const sanitizeKeywords = (keywords) =>
  Array.isArray(keywords)
    ? keywords
        .map((item) => (typeof item === 'string' ? item.trim() : ''))
        .filter((item) => item)
    : [];

const fetchHouses = async ({ keyword } = {}) => {
  loading.value = true;
  messages.error = '';
  const params = {};
  const keywordValue = typeof keyword === 'string' ? keyword.trim() : searchFilters.keyword?.trim();
  if (keywordValue) {
    params.keyword = keywordValue;
  }
  try {
    const { data } = await client.get('/houses', { params });
    houses.value = data.map((house) => normalizeHouseResponse(house));
  } catch (error) {
    messages.error = error.response?.data?.detail ?? '加载房源数据失败，请检查后端服务。';
  } finally {
    loading.value = false;
  }
};

const fetchWallet = async ({ silent = false } = {}) => {
  if (!currentUser.value) {
    wallet.value = null;
    return;
  }
  if (!silent) {
    walletLoading.value = true;
  }
  try {
    const { data } = await client.get(`/wallets/${currentUser.value.username}`);
    wallet.value = data;
  } catch (error) {
    const detail = error.response?.data?.detail ?? '加载钱包信息失败。';
    messages.error = detail;
  } finally {
    if (!silent) {
      walletLoading.value = false;
    }
  }
};

const fetchOrders = async ({ silent = false } = {}) => {
  if (!currentUser.value) {
    orders.value = [];
    return;
  }
  if (!silent) {
    ordersLoading.value = true;
  }
  try {
    const { data } = await client.get(`/orders/by-user/${currentUser.value.username}`);
    orders.value = data;
  } catch (error) {
    const detail = error.response?.data?.detail ?? '加载订单信息失败。';
    messages.error = detail;
  } finally {
    if (!silent) {
      ordersLoading.value = false;
    }
  }
};

const guardReadOnly = () => {
  if (!canManageHouses.value) {
    messages.error = '当前角色仅支持浏览房源，如需维护房源请使用卖家或系统管理员账号。';
    messages.success = '';
    return false;
  }
  return true;
};

const normalizeHousePayload = (payload) => {
  const result = { ...payload };
  if (isSeller.value) {
    result.sellerUsername = currentUser.value.username;
    if (!result.sellerName) {
      result.sellerName = currentUser.value.displayName ?? '';
    }
  }
  result.imageUrls = sanitizeImages(payload.imageUrls);
  result.keywords = sanitizeKeywords(payload.keywords);
  if (result.floor === '' || result.floor == null) {
    result.floor = null;
  }
  return result;
};

const getHistoryStorageKey = () =>
  currentUser.value ? `${historyStoragePrefix}${currentUser.value.username}` : null;

const loadBrowsingHistory = () => {
  const key = getHistoryStorageKey();
  if (!key) {
    browsingHistory.value = [];
    return;
  }
  try {
    const cached = localStorage.getItem(key);
    if (cached) {
      const parsed = JSON.parse(cached);
      browsingHistory.value = Array.isArray(parsed) ? parsed : [];
    } else {
      browsingHistory.value = [];
    }
  } catch (error) {
    console.warn('读取浏览历史失败：', error);
    browsingHistory.value = [];
  }
};

const saveBrowsingHistory = () => {
  const key = getHistoryStorageKey();
  if (!key) {
    return;
  }
  try {
    localStorage.setItem(
      key,
      JSON.stringify(browsingHistory.value.slice(0, historyLimit))
    );
  } catch (error) {
    console.warn('无法保存浏览历史：', error);
  }
};

const recordBrowsing = (house) => {
  if (!currentUser.value || !house?.id) {
    return;
  }
  const entry = {
    id: house.id,
    title: house.title,
    viewedAt: new Date().toISOString(),
    price: house.price ?? null,
    address: house.address ?? '',
    keywords: Array.isArray(house.keywords) ? [...house.keywords] : []
  };
  const filtered = browsingHistory.value.filter((item) => item.id !== entry.id);
  browsingHistory.value = [entry, ...filtered].slice(0, historyLimit);
  saveBrowsingHistory();
};

const clearBrowsingHistory = () => {
  browsingHistory.value = [];
  const key = getHistoryStorageKey();
  if (key) {
    localStorage.removeItem(key);
  }
};

const handleSearch = ({ keyword }) => {
  searchFilters.keyword = keyword?.trim() ?? '';
  fetchHouses({ keyword: searchFilters.keyword });
};

const handleViewHouse = (house) => {
  recordBrowsing(house);
};

const handleHistorySelect = (houseId) => {
  const house = houses.value.find((item) => item.id === houseId);
  if (house) {
    houseListRef.value?.openDetail(house);
    recordBrowsing(house);
  }
};

const handleSubmit = async (payload) => {
  if (!guardReadOnly()) {
    return;
  }

  loading.value = true;
  messages.error = '';
  messages.success = '';
  const requestPayload = normalizeHousePayload(payload);

  try {
    let responseHouse;
    if (selectedHouse.value) {
      const { data } = await client.put(`/houses/${selectedHouse.value.id}`, requestPayload);
      responseHouse = data;
      messages.success = `房源《${data.title}》已更新。`;
    } else {
      const { data } = await client.post('/houses', requestPayload);
      responseHouse = data;
      messages.success = `已新增房源《${data.title}》。`;
    }
    await fetchHouses({ keyword: searchFilters.keyword });
    selectedHouse.value = null;
    if (responseHouse) {
      recordBrowsing(normalizeHouseResponse(responseHouse));
    }
  } catch (error) {
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? '保存房源信息失败。';
    }
  } finally {
    loading.value = false;
  }
};

const handleEdit = (house) => {
  selectedHouse.value = normalizeHouseResponse(house);
  messages.error = '';
};

const handleCancel = () => {
  selectedHouse.value = null;
};

const handleRemove = async (house) => {
  if (!guardReadOnly()) {
    return;
  }

  if (!confirm(`确定要删除房源：${house.title} 吗？`)) {
    return;
  }

  loading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    await client.delete(`/houses/${house.id}`);
    await fetchHouses({ keyword: searchFilters.keyword });
    messages.success = `已删除房源《${house.title}》。`;
  } catch (error) {
    messages.error = error.response?.data?.detail ?? '删除房源失败。';
  } finally {
    loading.value = false;
  }
};

const handlePurchase = async (house) => {
  if (!isBuyer.value) {
    messages.error = '只有买家角色可以发起支付。';
    messages.success = '';
    return;
  }
  if (!currentUser.value?.realNameVerified) {
    messages.error = '购买前请先完成实名认证。';
    messages.success = '';
    return;
  }
  ordersLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    const { data } = await client.post('/orders', {
      houseId: house.id,
      buyerUsername: currentUser.value.username
    });
    messages.success = `成功购买房源《${data.houseTitle}》，支付金额 ${Number(data.amount).toFixed(2)} 元。`;
    await fetchWallet({ silent: true });
    await fetchOrders({ silent: true });
  } catch (error) {
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? '支付失败，请稍后再试。';
    }
  } finally {
    ordersLoading.value = false;
  }
};

const handleTopUp = async ({ amount, reference }) => {
  if (!currentUser.value) {
    messages.error = '请先登录后再使用钱包功能。';
    messages.success = '';
    return;
  }
  walletLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    const { data } = await client.post(`/wallets/${currentUser.value.username}/top-up`, {
      amount,
      reference
    });
    wallet.value = data;
    messages.success = '钱包充值成功。';
  } catch (error) {
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? '钱包充值失败。';
    }
  } finally {
    walletLoading.value = false;
  }
};

const handleRequestReturn = async ({ orderId, reason }) => {
  if (!currentUser.value) {
    messages.error = '请先登录后再申请退换。';
    messages.success = '';
    return;
  }
  ordersLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    const { data } = await client.post(`/orders/${orderId}/return`, {
      requesterUsername: currentUser.value.username,
      reason
    });
    messages.success = `订单《${data.houseTitle}》已退换成功。`;
    await fetchWallet({ silent: true });
    await fetchOrders({ silent: true });
  } catch (error) {
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? '退换请求失败。';
    }
  } finally {
    ordersLoading.value = false;
  }
};

const handleUpdateProgress = async ({ orderId, stage }) => {
  if (!currentUser.value) {
    messages.error = '请先登录后再更新订单进度。';
    messages.success = '';
    return;
  }
  ordersLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    const { data } = await client.post(`/orders/${orderId}/progress`, {
      requesterUsername: currentUser.value.username,
      stage
    });
    orders.value = orders.value.map((order) =>
      order.id === data.id ? data : order
    );
    messages.success = `交易进度已更新为${progressStageLabels[stage] ?? stage}。`;
  } catch (error) {
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? '更新交易进度失败。';
    }
  } finally {
    ordersLoading.value = false;
  }
};

const handleVerificationUpdate = (response) => {
  if (!response) {
    return;
  }
  currentUser.value = {
    ...currentUser.value,
    ...response
  };
  messages.error = '';
  messages.success = response.message ?? '实名认证信息已更新。';
  try {
    localStorage.setItem(storageKey, JSON.stringify(currentUser.value));
  } catch (error) {
    console.warn('无法保存认证状态：', error);
  }
  loadBrowsingHistory();
};

const handleLoginSuccess = (user) => {
  currentUser.value = user;
  messages.success = user.message ?? '';
  messages.error = '';
  try {
    localStorage.setItem(storageKey, JSON.stringify(user));
  } catch (error) {
    console.warn('无法持久化登录状态：', error);
  }
  loadBrowsingHistory();
  fetchHouses({ keyword: searchFilters.keyword });
  fetchWallet();
  fetchOrders();
};

const handleLogout = () => {
  const key = getHistoryStorageKey();
  currentUser.value = null;
  houses.value = [];
  selectedHouse.value = null;
  wallet.value = null;
  orders.value = [];
  walletLoading.value = false;
  ordersLoading.value = false;
  messages.error = '';
  messages.success = '';
  localStorage.removeItem(storageKey);
  if (key) {
    localStorage.removeItem(key);
  }
  browsingHistory.value = [];
  searchFilters.keyword = '';
};

onMounted(() => {
  try {
    const cached = localStorage.getItem(storageKey);
    if (cached) {
      const user = JSON.parse(cached);
      currentUser.value = user;
      messages.success = '已恢复上次的登录状态。';
      loadBrowsingHistory();
      fetchHouses({ keyword: searchFilters.keyword });
      fetchWallet();
      fetchOrders();
      return;
    }
  } catch (error) {
    console.warn('恢复登录状态失败：', error);
    localStorage.removeItem(storageKey);
  }
  fetchHouses({ keyword: searchFilters.keyword });
});
</script>

<style scoped>
.app-shell {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: clamp(1.5rem, 2vw + 1rem, 2.75rem);
  padding: clamp(1.5rem, 2vw + 1.5rem, 3.5rem);
  max-width: 1400px;
  margin: 0 auto;
  color: var(--text-primary);
  overflow: hidden;
}

.app-shell > :not(.orb) {
  position: relative;
  z-index: 1;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.55;
  pointer-events: none;
  z-index: 0;
}

.orb--one {
  width: 420px;
  height: 420px;
  top: -160px;
  right: -120px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.65), transparent 70%);
}

.orb--two {
  width: 360px;
  height: 360px;
  bottom: -140px;
  left: -160px;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.55), transparent 70%);
}

.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1.5rem;
  padding: 1.2rem 1.6rem;
  border-radius: 26px;
  background: var(--surface-primary);
  border: 1px solid var(--surface-border);
  backdrop-filter: blur(24px);
  box-shadow: var(--shadow-strong);
}

.brand {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 3rem;
  height: 3rem;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.95), rgba(99, 102, 241, 0.65));
  font-size: 1.5rem;
  box-shadow: 0 18px 35px rgba(59, 130, 246, 0.35);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.brand-copy strong {
  font-size: 1.25rem;
  letter-spacing: 0.04em;
}

.brand-copy span {
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.nav-actions__guest {
  padding: 0.5rem 0.85rem;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.2);
  color: var(--text-secondary);
  font-weight: 600;
}

.account-chip {
  display: flex;
  align-items: center;
  gap: 1.2rem;
  padding: 0.6rem 1.1rem;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(12px);
}

.account-meta {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.account-role {
  font-size: 0.8rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.account-name {
  font-weight: 600;
}

.chip-action {
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--accent), var(--accent-strong));
  color: var(--text-primary);
  font-weight: 600;
  padding: 0.45rem 1.35rem;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  box-shadow: 0 18px 35px rgba(59, 130, 246, 0.35);
}

.chip-action:hover {
  transform: translateY(-2px);
  box-shadow: 0 22px 40px rgba(59, 130, 246, 0.45);
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: clamp(1.5rem, 4vw, 3.25rem);
  padding: clamp(1.8rem, 4vw, 3rem);
  border-radius: 32px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.45), rgba(30, 64, 175, 0.65));
  border: 1px solid rgba(148, 163, 184, 0.35);
  box-shadow: var(--shadow-strong);
  overflow: hidden;
}

.hero::after {
  content: '';
  position: absolute;
  inset: auto -25% -35% auto;
  width: 55%;
  height: 120%;
  background: radial-gradient(circle, rgba(96, 165, 250, 0.5), transparent 65%);
  pointer-events: none;
}

.hero-copy {
  max-width: 620px;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.eyebrow {
  font-size: 0.85rem;
  letter-spacing: 0.4em;
  text-transform: uppercase;
  color: rgba(226, 232, 240, 0.8);
}

.hero h1 {
  margin: 0;
  font-size: clamp(2.2rem, 5vw, 3.15rem);
  font-weight: 700;
  letter-spacing: 0.015em;
}

.hero p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.75;
}

.hero-points {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  color: var(--text-primary);
  font-weight: 500;
}

.hero-points li::before {
  content: '•';
  margin-right: 0.5rem;
  color: var(--accent);
}

.hero-spotlight {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
  min-width: 220px;
}

.hero-stat {
  position: relative;
  padding: 1.4rem 1.6rem;
  border-radius: 24px;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid rgba(148, 163, 184, 0.28);
  backdrop-filter: blur(22px);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  box-shadow: var(--shadow-strong);
}

.hero-stat::after {
  content: '';
  position: absolute;
  inset: -35% 40% auto -35%;
  height: 150%;
  background: radial-gradient(circle, rgba(96, 165, 250, 0.4), transparent 75%);
  opacity: 0.8;
  pointer-events: none;
}

.hero-stat.accent {
  background: linear-gradient(145deg, rgba(37, 99, 235, 0.75), rgba(14, 165, 233, 0.65));
  border-color: rgba(96, 165, 250, 0.5);
}

.hero-stat__label {
  font-size: 0.8rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.hero-stat__value {
  font-size: 2.1rem;
  font-weight: 700;
}

.hero-stat__meta {
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.message-stack {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.message {
  border-radius: 20px;
  padding: 0.85rem 1.4rem;
  backdrop-filter: blur(16px);
  border: 1px solid transparent;
}

.message--success {
  background: rgba(52, 211, 153, 0.2);
  border-color: rgba(52, 211, 153, 0.45);
  color: #bbf7d0;
}

.message--danger {
  background: rgba(248, 113, 113, 0.18);
  border-color: rgba(248, 113, 113, 0.4);
  color: #fecaca;
}

.guest-layout {
  display: flex;
  flex-wrap: wrap;
  gap: clamp(1.5rem, 3vw, 2.5rem);
  align-items: stretch;
  padding: clamp(1.5rem, 3vw, 2.5rem);
  border-radius: 28px;
  background: var(--surface-primary);
  border: 1px solid var(--surface-border);
  box-shadow: var(--shadow-strong);
  backdrop-filter: blur(18px);
}

.guest-intro {
  flex: 1 1 320px;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  color: var(--text-secondary);
}

.guest-intro h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: clamp(1.8rem, 4vw, 2.4rem);
}

.guest-highlights {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
}

.guest-card {
  padding: 1.2rem;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid rgba(148, 163, 184, 0.25);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.guest-card__title {
  font-weight: 600;
  color: var(--text-primary);
}

.guest-auth {
  flex: 1 1 360px;
  display: flex;
  align-items: stretch;
  justify-content: center;
}

.guest-auth :deep(.auth-panel) {
  width: 100%;
  max-width: 420px;
}

.overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1.25rem;
}

.overview-card {
  position: relative;
  border-radius: 24px;
  padding: 1.6rem;
  background: var(--surface-primary);
  border: 1px solid var(--surface-border);
  backdrop-filter: blur(20px);
  box-shadow: var(--shadow-strong);
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  overflow: hidden;
}

.overview-card::after {
  content: '';
  position: absolute;
  inset: -30% 45% auto -30%;
  height: 140%;
  background: radial-gradient(circle, rgba(96, 165, 250, 0.4), transparent 70%);
  opacity: 0.7;
  pointer-events: none;
}

.overview-card.accent::after {
  background: radial-gradient(circle, rgba(56, 189, 248, 0.65), transparent 75%);
}

.overview-card.positive::after {
  background: radial-gradient(circle, rgba(52, 211, 153, 0.55), transparent 75%);
}

.overview-card__label {
  font-size: 0.85rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.overview-card__value {
  font-size: clamp(1.9rem, 3vw, 2.6rem);
  font-weight: 700;
  z-index: 1;
}

.overview-card__meta {
  color: var(--text-secondary);
  font-size: 0.9rem;
  z-index: 1;
}

.overview-card.accent {
  background: linear-gradient(145deg, rgba(37, 99, 235, 0.75), rgba(14, 165, 233, 0.65));
  border: 1px solid rgba(96, 165, 250, 0.5);
}

.dashboard {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: clamp(1.5rem, 3vw, 2rem);
}

.dashboard-main {
  flex: 1 1 620px;
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.dashboard-side {
  flex: 1 1 360px;
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.dashboard-side :deep(.house-form),
.dashboard-side :deep(.verification),
.dashboard-side :deep(.wallet-panel),
.dashboard-side :deep(.order-history) {
  width: 100%;
}

.dashboard-main :deep(.history) {
  margin-top: 0.5rem;
}

.footer {
  text-align: center;
  color: var(--text-muted);
  padding: 1.5rem 0 0.75rem;
  font-size: 0.9rem;
}

@media (max-width: 1080px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-spotlight {
    flex-direction: row;
    flex-wrap: wrap;
    width: 100%;
  }

  .hero-stat {
    flex: 1 1 200px;
  }
}

@media (max-width: 768px) {
  .top-nav,
  .hero,
  .guest-layout {
    padding: 1.35rem;
  }

  .top-nav {
    flex-direction: column;
    align-items: flex-start;
  }

  .account-chip {
    width: 100%;
    justify-content: space-between;
  }

  .dashboard-side,
  .dashboard-main {
    flex: 1 1 100%;
  }
}

@media (max-width: 520px) {
  .app-shell {
    padding: 1.25rem;
    gap: 1.5rem;
  }

  .hero-spotlight {
    flex-direction: column;
  }
}
</style>
