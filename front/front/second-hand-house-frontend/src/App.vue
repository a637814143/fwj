<template>
  <div class="app-shell">
    <div class="background-glow" aria-hidden="true"></div>
    <aside class="sidebar">
      <div class="brand-card">
        <div class="brand-icon">🏡</div>
        <div class="brand-copy">
          <h1>栖居 Studio</h1>
          <p>Second-hand Housing Dashboard</p>
        </div>
      </div>

      <div class="sidebar-session" v-if="currentUser">
        <p class="welcome">欢迎回来，</p>
        <strong class="user-name">{{ currentUser.displayName }}</strong>
        <span class="role-chip">{{ roleLabels[currentUser.role] }}</span>
        <button type="button" class="logout" @click="handleLogout">退出登录</button>
      </div>
      <div class="sidebar-session" v-else>
        <p class="welcome">您好！</p>
        <p>登录后可体验全部功能。</p>
      </div>

      <nav class="menu" aria-label="主要功能导航">
        <button
          v-for="item in menuItems"
          :key="item.id"
          type="button"
          class="menu-item"
          :class="{ active: activeSection === item.id }"
          @click="navigateTo(item.id)"
        >
          <span class="menu-icon" aria-hidden="true">{{ item.icon }}</span>
          <span class="menu-label">{{ item.label }}</span>
        </button>
      </nav>

      <div class="menu-footer">
        <small>后端接口：{{ apiBaseUrl }}</small>
      </div>
    </aside>

    <main class="main-panel">
      <section class="panel hero" id="dashboard">
        <div class="hero-content">
          <h2>二手房屋管理系统</h2>
          <p class="subtitle">以 Apple 风格呈现的数据工作台，轻松掌握房源、订单与钱包信息。</p>
          <p class="hint">{{ currentUser ? '您可以通过左侧菜单快速跳转到所需功能。' : '请先登录或注册账号。' }}</p>
        </div>
      </section>

      <div class="notifications">
        <div v-if="messages.error" class="alert" role="alert">
          <strong>提示：</strong> {{ messages.error }}
        </div>
        <div v-if="messages.success" class="success" role="status">{{ messages.success }}</div>
      </div>

      <section v-if="!currentUser" class="panel card" id="auth">
        <h3 class="panel-title">账户登录 / 注册</h3>
        <RoleLogin :api-base-url="apiBaseUrl" @login-success="handleLoginSuccess" />
      </section>

      <template v-else>
        <section class="panel card" id="verification">
          <h3 class="panel-title">实名认证</h3>
          <RealNameVerification
            :api-base-url="apiBaseUrl"
            :current-user="currentUser"
            @verified="handleVerificationUpdate"
          />
        </section>

        <section class="panel module" id="house-center">
          <div class="module-grid">
            <div class="module-card">
              <div class="module-header">
                <h3>房源维护</h3>
                <p>创建、编辑或删除您的房源信息。</p>
              </div>
              <HouseForm
                :initial-house="selectedHouse"
                :loading="loading"
                :can-manage="canManageHouses"
                :current-user="currentUser"
                @submit="handleSubmit"
                @cancel="handleCancel"
              />
            </div>
            <div class="module-card">
              <div class="module-header">
                <h3>房源列表</h3>
                <p>通过智能筛选快速定位目标房源。</p>
              </div>
              <div class="module-toolbar">
                <HouseSearchBar
                  :loading="loading"
                  :initial-keyword="searchFilters.keyword"
                  @search="handleSearch"
                />
              </div>
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
            </div>
          </div>
        </section>

        <section class="panel card" id="history">
          <div class="module-header">
            <h3>浏览历史</h3>
            <p>快速回溯最近查看过的房源。</p>
          </div>
          <BrowsingHistory
            :history="browsingHistory"
            @select="handleHistorySelect"
            @clear="clearBrowsingHistory"
          />
        </section>

        <section class="panel module" id="wallet-orders">
          <div class="module-grid">
            <div class="module-card">
              <div class="module-header">
                <h3>钱包</h3>
                <p>掌控资金流水，随时充值。</p>
              </div>
              <WalletPanel
                :wallet="wallet"
                :loading="walletLoading"
                :current-user="currentUser"
                @top-up="handleTopUp"
              />
            </div>
            <div class="module-card">
              <div class="module-header">
                <h3>订单</h3>
                <p>查看交易进度并申请售后。</p>
              </div>
              <OrderHistory
                :orders="orders"
                :loading="ordersLoading"
                :current-user="currentUser"
                @request-return="handleRequestReturn"
                @update-progress="handleUpdateProgress"
              />
            </div>
          </div>
        </section>
      </template>

      <footer class="footer panel" id="support">
        <div class="footer-content">
          <h3>帮助与支持</h3>
          <p>如需技术支持，请联系系统管理员或在工单系统中提交反馈。</p>
          <small>当前接口地址：{{ apiBaseUrl }}</small>
        </div>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
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
const activeSection = ref('dashboard');

const menuItems = computed(() => {
  const items = [
    {
      id: 'dashboard',
      label: currentUser.value ? '工作台' : '欢迎页',
      icon: '✨'
    }
  ];

  if (!currentUser.value) {
    items.push({ id: 'auth', label: '登录 / 注册', icon: '🔐' });
  } else {
    items.push(
      { id: 'verification', label: '实名认证', icon: '🪪' },
      { id: 'house-center', label: '房源管理', icon: '🏠' },
      { id: 'history', label: '浏览历史', icon: '🧭' },
      { id: 'wallet-orders', label: '钱包与订单', icon: '💳' }
    );
  }

  items.push({ id: 'support', label: '帮助与支持', icon: '📞' });
  return items;
});

const navigateTo = async (sectionId) => {
  activeSection.value = sectionId;
  await nextTick();
  const target = document.getElementById(sectionId);
  if (target) {
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
};

watch(
  menuItems,
  (items) => {
    if (!items.some((item) => item.id === activeSection.value)) {
      activeSection.value = items[0]?.id ?? 'dashboard';
    }
  },
  { immediate: true }
);

watch(
  () => currentUser.value,
  () => {
    activeSection.value = menuItems.value[0]?.id ?? 'dashboard';
  }
);

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
.app-shell {
  min-height: 100vh;
  display: flex;
  position: relative;
  background: linear-gradient(130deg, #f8fafc 0%, #e0e7ff 45%, #f5f3ff 100%);
  color: #0f172a;
  overflow-x: hidden;
}

.background-glow {
  position: fixed;
  top: -20%;
  right: -10%;
  width: 60vw;
  height: 60vw;
  background: radial-gradient(circle at center, rgba(79, 70, 229, 0.45), rgba(79, 70, 229, 0));
  filter: blur(120px);
  opacity: 0.7;
  pointer-events: none;
  z-index: 0;
}

.sidebar {
  position: sticky;
  top: 0;
  align-self: flex-start;
  height: 100vh;
  width: 280px;
  padding: 2.5rem 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
  background: rgba(15, 23, 42, 0.82);
  color: #f8fafc;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.25);
  backdrop-filter: blur(22px);
  z-index: 2;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-card {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.brand-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 1rem;
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.4), rgba(129, 140, 248, 0.75));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.35);
}

.brand-copy h1 {
  margin: 0;
  font-size: 1.35rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.brand-copy p {
  margin: 0.25rem 0 0;
  opacity: 0.72;
  font-size: 0.85rem;
}

.sidebar-session {
  background: rgba(255, 255, 255, 0.08);
  border-radius: 1.25rem;
  padding: 1.25rem;
  display: grid;
  gap: 0.35rem;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.sidebar-session .welcome {
  margin: 0;
  font-size: 0.85rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.65;
}

.user-name {
  font-size: 1.1rem;
}

.role-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  font-size: 0.75rem;
  background: rgba(148, 163, 184, 0.2);
  color: #e2e8f0;
  width: fit-content;
}

.logout {
  margin-top: 0.5rem;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.25), rgba(226, 232, 240, 0.4));
  border: none;
  border-radius: 999px;
  color: #0f172a;
  font-weight: 600;
  padding: 0.55rem 1.5rem;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.logout:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 20px rgba(15, 23, 42, 0.25);
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 1rem;
  background: transparent;
  color: inherit;
  border: none;
  cursor: pointer;
  font-size: 0.95rem;
  transition: background 0.2s ease, transform 0.2s ease;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.12);
  transform: translateX(4px);
}

.menu-item.active {
  background: rgba(255, 255, 255, 0.2);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.menu-icon {
  font-size: 1.2rem;
}

.menu-footer {
  margin-top: auto;
  font-size: 0.75rem;
  opacity: 0.7;
}

.main-panel {
  flex: 1;
  padding: 2.5rem 3rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
  position: relative;
  z-index: 1;
}

.panel {
  background: rgba(255, 255, 255, 0.78);
  border-radius: 1.75rem;
  padding: 2rem;
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(18px);
}

.panel.hero {
  padding: 2.5rem;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.65), rgba(129, 140, 248, 0.35));
  color: #0b1120;
}

.hero-content h2 {
  margin: 0 0 0.75rem;
  font-size: 2rem;
  font-weight: 700;
}

.hero-content .subtitle {
  margin: 0 0 0.75rem;
  font-size: 1.05rem;
  line-height: 1.6;
  max-width: 42rem;
}

.hero-content .hint {
  margin: 0;
  font-size: 0.95rem;
  opacity: 0.8;
}

.notifications {
  display: grid;
  gap: 1rem;
}

.alert,
.success {
  border-radius: 1rem;
  padding: 1rem 1.5rem;
  font-size: 0.95rem;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.alert {
  background: rgba(248, 113, 113, 0.12);
  color: #991b1b;
  border: 1px solid rgba(248, 113, 113, 0.3);
}

.success {
  background: rgba(52, 211, 153, 0.15);
  color: #065f46;
  border: 1px solid rgba(52, 211, 153, 0.3);
}

.panel.card {
  padding: 1.75rem;
}

.panel-title {
  margin: 0 0 1.5rem;
  font-size: 1.25rem;
  font-weight: 600;
}

.module {
  padding: 2rem 2.5rem;
}

.module-grid {
  display: grid;
  gap: 1.75rem;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  align-items: flex-start;
}

.module-card {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 1.5rem;
  padding: 1.75rem;
  box-shadow: 0 25px 45px rgba(15, 23, 42, 0.1);
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.module-header h3 {
  margin: 0;
  font-size: 1.25rem;
}

.module-header p {
  margin: 0.35rem 0 0;
  font-size: 0.9rem;
  color: #475569;
}

.module-toolbar {
  margin-bottom: 1rem;
}

.footer {
  text-align: left;
  background: rgba(15, 23, 42, 0.8);
  color: #e2e8f0;
}

.footer-content h3 {
  margin: 0 0 0.75rem;
}

.footer-content p {
  margin: 0 0 0.5rem;
  font-size: 0.95rem;
}

.footer-content small {
  opacity: 0.7;
}

@media (max-width: 1200px) {
  .sidebar {
    width: 240px;
    padding: 2rem 1.5rem;
  }

  .main-panel {
    padding: 2rem;
  }
}

@media (max-width: 1024px) {
  .app-shell {
    flex-direction: column;
  }

  .sidebar {
    position: relative;
    width: 100%;
    height: auto;
    flex-direction: row;
    align-items: center;
    flex-wrap: wrap;
    gap: 1rem;
  }

  .menu {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 0.5rem;
  }

  .menu-item {
    flex: 1 1 140px;
    justify-content: center;
  }

  .menu-footer {
    width: 100%;
    text-align: center;
  }

  .main-panel {
    padding: 2rem 1.5rem 3rem;
  }
}

@media (max-width: 640px) {
  .panel,
  .panel.hero,
  .module-card {
    padding: 1.5rem;
  }

  .hero-content h2 {
    font-size: 1.6rem;
  }

  .module-grid {
    grid-template-columns: 1fr;
  }
}
</style>
