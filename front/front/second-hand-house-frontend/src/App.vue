<template>
  <div
    class="app"
    :class="appClasses"
    :style="themeStyles"
  >
    <aside class="sidebar">
      <div class="sidebar__brand">
        <h1>二手房屋管理系统</h1>
        <p>高效管理房源与客户信息</p>
      </div>

      <nav class="sidebar__menu">
        <button type="button" @click="scrollToSection('login')">登录 / 注册</button>
        <button type="button" @click="scrollToSection('house-management')">房源管理</button>
        <button type="button" @click="scrollToSection('history')">浏览历史</button>
        <button type="button" @click="scrollToSection('wallet-orders')">钱包与订单</button>
      </nav>

      <div class="sidebar__controls">
        <div class="control-group">
          <h3>模式切换</h3>
          <div class="mode-toggle">
            <button
              type="button"
              :class="{ active: themeMode === 'day' }"
              @click="themeMode = 'day'"
            >
              白天模式
            </button>
            <button
              type="button"
              :class="{ active: themeMode === 'night' }"
              @click="themeMode = 'night'"
            >
              黑夜模式
            </button>
          </div>
        </div>

        <div class="control-group">
          <h3>主题颜色</h3>
          <div class="color-options">
            <button
              v-for="option in colorOptions"
              :key="option.value"
              type="button"
              :class="['color-swatch', { active: accentColor === option.value }]"
              :style="{ '--swatch-color': option.preview }"
              @click="accentColor = option.value"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <div class="control-group senior-toggle">
          <h3>老年人模式</h3>
          <button
            type="button"
            class="switch"
            :class="{ active: seniorMode }"
            @click="seniorMode = !seniorMode"
          >
            <span>{{ seniorMode ? '已开启' : '点击开启' }}</span>
          </button>
        </div>
      </div>

      <footer class="sidebar__footer">
        <small>后端接口：{{ apiBaseUrl }}</small>
      </footer>
    </aside>

    <div class="main-area">
      <header class="header" id="overview">
        <div class="header__content">
          <div>
            <h2>欢迎使用房源管理面板</h2>
            <p>请先登录或注册账号后再管理房源信息。</p>
          </div>
          <div v-if="currentUser" class="session">
            <span>
              当前角色：<strong>{{ roleLabels[currentUser.role] }}</strong>（{{ currentUser.displayName }}）
            </span>
            <button type="button" class="logout" @click="handleLogout">退出登录</button>
          </div>
        </div>
        <p v-if="messages.success" class="success">{{ messages.success }}</p>
      </header>

      <section v-if="!currentUser" id="login" class="login-section">
        <RoleLogin :api-base-url="apiBaseUrl" @login-success="handleLoginSuccess" />
      </section>

      <template v-else>
        <section v-if="messages.error" class="alert">
          <strong>提示：</strong> {{ messages.error }}
        </section>

        <RealNameVerification
          :api-base-url="apiBaseUrl"
          :current-user="currentUser"
          @verified="handleVerificationUpdate"
        />

        <main class="content" id="house-management">
          <section class="form-section">
            <HouseForm
              :initial-house="selectedHouse"
              :loading="loading"
              :can-manage="canManageHouses"
              :current-user="currentUser"
              @submit="handleSubmit"
              @cancel="handleCancel"
            />
          </section>

          <section class="list-section">
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
          </section>
        </main>

        <section class="history-section" id="history">
          <BrowsingHistory
            :history="browsingHistory"
            @select="handleHistorySelect"
            @clear="clearBrowsingHistory"
          />
        </section>

        <section class="wallet-order-section" id="wallet-orders">
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
        </section>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
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

const colorOptions = [
  { value: 'blue', label: '蓝色', preview: '#2563eb' },
  { value: 'black', label: '黑色', preview: '#111827' },
  { value: 'purple', label: '紫色', preview: '#7c3aed' },
  { value: 'green', label: '绿色', preview: '#16a34a' }
];

const themeMode = ref('day');
const accentColor = ref('blue');
const seniorMode = ref(false);
const preferenceStorageKey = 'secondhand-house-ui-preferences';

const accentPresets = {
  blue: {
    color: '#2563eb',
    gradient: 'linear-gradient(135deg, #2563eb, #1d4ed8)',
    soft: 'rgba(37, 99, 235, 0.16)',
    contrast: '#ffffff',
    softText: '#1e3a8a'
  },
  black: {
    color: '#111827',
    gradient: 'linear-gradient(135deg, #1f2937, #111827)',
    soft: 'rgba(17, 24, 39, 0.22)',
    contrast: '#f9fafb',
    softText: '#f9fafb'
  },
  purple: {
    color: '#7c3aed',
    gradient: 'linear-gradient(135deg, #7c3aed, #5b21b6)',
    soft: 'rgba(124, 58, 237, 0.2)',
    contrast: '#f5f3ff',
    softText: '#4c1d95'
  },
  green: {
    color: '#16a34a',
    gradient: 'linear-gradient(135deg, #22c55e, #15803d)',
    soft: 'rgba(34, 197, 94, 0.22)',
    contrast: '#ecfdf5',
    softText: '#14532d'
  }
};

const themeTokens = {
  day: {
    background: '#eef2ff',
    surface: '#ffffff',
    surfaceAlt: '#f8fafc',
    highlight: '#e0f2fe',
    text: '#1f2937',
    muted: '#4b5563',
    border: 'rgba(148, 163, 184, 0.35)',
    shadow: '0 18px 35px rgba(15, 23, 42, 0.12)'
  },
  night: {
    background: '#0b1120',
    surface: '#111827',
    surfaceAlt: '#1e293b',
    highlight: 'rgba(148, 163, 184, 0.18)',
    text: '#e2e8f0',
    muted: '#94a3b8',
    border: 'rgba(148, 163, 184, 0.25)',
    shadow: '0 25px 45px rgba(2, 6, 23, 0.55)'
  }
};

const themeStyles = computed(() => {
  const theme = themeTokens[themeMode.value] ?? themeTokens.day;
  const accent = accentPresets[accentColor.value] ?? accentPresets.blue;

  return {
    '--app-bg': theme.background,
    '--app-surface': theme.surface,
    '--app-surface-alt': theme.surfaceAlt,
    '--app-highlight': theme.highlight,
    '--app-text': theme.text,
    '--app-muted': theme.muted,
    '--app-border': theme.border,
    '--app-shadow': theme.shadow,
    '--accent-color': accent.color,
    '--accent-gradient': accent.gradient,
    '--accent-contrast': accent.contrast,
    '--accent-soft': accent.soft,
    '--accent-soft-text': accent.softText
  };
});

const appClasses = computed(() => [
  themeMode.value === 'night' ? 'mode-night' : 'mode-day',
  { 'senior-mode': seniorMode.value }
]);

const scrollToSection = (sectionId) => {
  const element = document.getElementById(sectionId);
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
};

watch(
  [themeMode, accentColor, seniorMode],
  ([modeValue, accentValue, seniorValue]) => {
    try {
      localStorage.setItem(
        preferenceStorageKey,
        JSON.stringify({
          mode: modeValue,
          accent: accentValue,
          seniorMode: seniorValue
        })
      );
    } catch (error) {
      console.warn('无法保存个性化设置：', error);
    }
  }
);

onMounted(() => {
  try {
    const cachedPreferences = localStorage.getItem(preferenceStorageKey);
    if (cachedPreferences) {
      const parsed = JSON.parse(cachedPreferences);
      if (parsed.mode === 'night' || parsed.mode === 'day') {
        themeMode.value = parsed.mode;
      }
      if (parsed.accent && accentPresets[parsed.accent]) {
        accentColor.value = parsed.accent;
      }
      seniorMode.value = Boolean(parsed.seniorMode);
    }
  } catch (error) {
    console.warn('读取个性化设置失败：', error);
  }
});

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
.app {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 0;
  background: var(--app-bg);
  color: var(--app-text);
  transition: background 0.4s ease, color 0.4s ease;
}

.sidebar {
  background: var(--app-surface);
  box-shadow: var(--app-shadow);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 2.25rem 1.75rem;
  gap: 2rem;
  position: sticky;
  top: 0;
  min-height: 100vh;
}

.sidebar__brand h1 {
  margin: 0;
  font-size: 1.85rem;
  color: var(--app-text);
}

.sidebar__brand p {
  margin: 0.35rem 0 0;
  color: var(--app-muted);
  font-size: 0.95rem;
}

.sidebar__menu {
  display: grid;
  gap: 0.75rem;
}

.sidebar__menu button {
  width: 100%;
  border: 1px solid var(--app-border);
  border-radius: 0.85rem;
  padding: 0.8rem 1rem;
  background: var(--app-surface-alt);
  color: var(--app-text);
  font-weight: 600;
  text-align: left;
  transition: background 0.25s ease, color 0.25s ease, transform 0.25s ease;
}

.sidebar__menu button:hover {
  background: var(--accent-soft);
  color: var(--accent-soft-text);
  transform: translateX(6px);
}

.sidebar__controls {
  display: grid;
  gap: 1.5rem;
}

.control-group h3 {
  margin: 0 0 0.75rem;
  font-size: 1rem;
  color: var(--app-text);
}

.mode-toggle {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.mode-toggle button {
  border: 1px solid var(--app-border);
  border-radius: 999px;
  padding: 0.55rem 0.75rem;
  background: transparent;
  color: var(--app-text);
  font-weight: 600;
  transition: background 0.3s ease, color 0.3s ease, box-shadow 0.3s ease;
}

.mode-toggle button.active {
  background: var(--accent-gradient);
  color: var(--accent-contrast);
  border-color: transparent;
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.color-options {
  display: grid;
  gap: 0.65rem;
}

.color-swatch {
  position: relative;
  border: 1px solid transparent;
  border-radius: 0.85rem;
  padding: 0.75rem 1rem 0.75rem 3.25rem;
  background: var(--app-surface-alt);
  color: var(--app-text);
  font-weight: 600;
  text-align: left;
  transition: transform 0.3s ease, box-shadow 0.3s ease, border-color 0.3s ease;
}

.color-swatch::before {
  content: '';
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  width: 1.65rem;
  height: 1.65rem;
  border-radius: 0.55rem;
  background: var(--swatch-color);
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.2);
}

.color-swatch.active {
  border-color: var(--accent-color);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.15);
  transform: translateX(4px);
}

.color-swatch:hover {
  transform: translateX(6px);
}

.switch {
  width: 100%;
  border: 1px solid var(--app-border);
  border-radius: 999px;
  padding: 0.65rem 1rem;
  background: var(--app-surface-alt);
  color: var(--app-text);
  font-weight: 600;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  transition: background 0.3s ease, color 0.3s ease, border-color 0.3s ease;
}

.switch.active {
  background: var(--accent-soft);
  color: var(--accent-soft-text);
  border-color: var(--accent-color);
}

.sidebar__footer {
  margin-top: auto;
  color: var(--app-muted);
  font-size: 0.85rem;
}

.main-area {
  padding: 2.5rem 2.75rem;
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.header {
  background: var(--accent-gradient);
  color: var(--accent-contrast);
  padding: 2rem;
  border-radius: 1.5rem;
  box-shadow: var(--app-shadow);
  display: grid;
  gap: 1.25rem;
}

.header__content {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.header h2 {
  margin: 0 0 0.35rem;
  font-size: 2rem;
}

.header p {
  margin: 0;
  color: var(--accent-contrast);
  opacity: 0.9;
}

.session {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  color: var(--accent-contrast);
}

.session span {
  font-weight: 600;
}

.logout {
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 999px;
  color: var(--accent-contrast);
  font-weight: 600;
  padding: 0.55rem 1.4rem;
  transition: background 0.25s ease, transform 0.25s ease;
}

.logout:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

.login-section {
  display: flex;
  justify-content: center;
}

.alert {
  background: rgba(239, 68, 68, 0.15);
  border-left: 4px solid rgba(239, 68, 68, 0.85);
  border-radius: 0.85rem;
  color: #fca5a5;
  padding: 1rem 1.5rem;
}

.success {
  background: var(--accent-soft);
  border-left: 4px solid var(--accent-color);
  border-radius: 0.85rem;
  color: var(--accent-soft-text);
  margin: 0;
  padding: 0.85rem 1.25rem;
}

.content {
  display: grid;
  gap: 1.75rem;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
}

.wallet-order-section {
  display: grid;
  gap: 1.75rem;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
}

.history-section {
  display: flex;
  flex-direction: column;
}

.form-section,
.list-section {
  background: var(--app-surface);
  border-radius: 1.25rem;
  box-shadow: var(--app-shadow);
  padding: 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.list-section {
  gap: 1.1rem;
}

.app.mode-night .sidebar__menu button,
.app.mode-night .switch,
.app.mode-night .color-swatch {
  box-shadow: none;
}

.app.senior-mode {
  font-size: 1.08rem;
  grid-template-columns: 330px 1fr;
}

.app.senior-mode .sidebar {
  padding: 2.75rem 2.25rem;
}

.app.senior-mode .main-area {
  padding: 3rem;
}

.app.senior-mode .sidebar__menu button,
.app.senior-mode .mode-toggle button,
.app.senior-mode .color-swatch,
.app.senior-mode .switch,
.app.senior-mode .logout,
.app.senior-mode .success,
.app.senior-mode .alert {
  font-size: 1.05rem;
}

@media (max-width: 1080px) {
  .app {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
    min-height: auto;
    border-bottom-left-radius: 1.5rem;
    border-bottom-right-radius: 1.5rem;
  }

  .main-area {
    padding: 1.75rem 1.5rem 2.5rem;
  }
}

@media (max-width: 720px) {
  .header {
    padding: 1.75rem;
  }

  .content,
  .wallet-order-section {
    grid-template-columns: minmax(0, 1fr);
  }

  .form-section,
  .list-section {
    padding: 1.35rem;
  }
}

@media (max-width: 520px) {
  .mode-toggle {
    grid-template-columns: 1fr;
  }

  .color-swatch {
    padding-left: 3rem;
  }

  .main-area {
    padding: 1.5rem 1.1rem 2rem;
  }
}
</style>
