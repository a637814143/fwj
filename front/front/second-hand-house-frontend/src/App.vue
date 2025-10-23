<template>
  <div class="app">
    <header class="header">
      <h1>二手房屋管理系统</h1>
      <p>请先登录或注册账号后再管理房源信息。</p>
      <div v-if="currentUser" class="session">
        <span>
          当前角色：<strong>{{ roleLabels[currentUser.role] }}</strong>（{{ currentUser.displayName }}）
        </span>
        <button type="button" class="logout" @click="handleLogout">退出登录</button>
      </div>
      <p v-if="messages.success" class="success">{{ messages.success }}</p>
    </header>

    <section v-if="!currentUser" class="login-section">
      <RoleLogin :api-base-url="apiBaseUrl" @login-success="handleLoginSuccess" />
    </section>

    <template v-else>
      <section v-if="messages.error" class="alert">
        <strong>提示：</strong> {{ messages.error }}
      </section>

      <div class="workspace">
        <aside class="sidebar">
          <div class="menu-card">
            <h2>功能导航</h2>
            <nav class="menu" role="navigation" aria-label="功能菜单">
              <button
                v-for="item in menuItems"
                :key="item.value"
                type="button"
                :class="['menu-item', { active: item.value === activeSection, attention: item.attention }]"
                @click="selectSection(item.value)"
              >
                <span class="menu-label">{{ item.label }}</span>
                <span v-if="item.description" class="menu-description">{{ item.description }}</span>
              </button>
            </nav>
          </div>
        </aside>

        <div class="main-panel">
          <section v-if="activeSection === 'houses'" class="panel houses-panel">
            <div class="houses-layout">
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
            </div>
          </section>

          <section v-else-if="activeSection === 'history'" class="panel history-panel">
            <BrowsingHistory
              :history="browsingHistory"
              @select="handleHistorySelect"
              @clear="clearBrowsingHistory"
            />
          </section>

          <section v-else-if="activeSection === 'wallet'" class="panel wallet-panel-container">
            <WalletPanel
              :wallet="wallet"
              :loading="walletLoading"
              :current-user="currentUser"
              @top-up="handleTopUp"
            />
          </section>

          <section v-else-if="activeSection === 'orders'" class="panel orders-panel">
            <OrderHistory
              :orders="orders"
              :loading="ordersLoading"
              :current-user="currentUser"
              @request-return="handleRequestReturn"
              @update-progress="handleUpdateProgress"
            />
          </section>

          <section v-else class="panel profile-panel">
            <UserProfile
              :current-user="currentUser"
              :role-label="roleLabels[currentUser.role]"
              :wallet="wallet"
            />
            <RealNameVerification
              :api-base-url="apiBaseUrl"
              :current-user="currentUser"
              @verified="handleVerificationUpdate"
            />
          </section>
        </div>
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
import UserProfile from './components/UserProfile.vue';

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
const activeSection = ref('houses');

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

const menuItems = computed(() => [
  {
    value: 'houses',
    label: '房源管理',
    description: canManageHouses.value
      ? '发布与维护房源信息'
      : '浏览房源与查看详情',
    attention: false
  },
  {
    value: 'history',
    label: '浏览历史',
    description: '查看最近的房源访问记录',
    attention: false
  },
  {
    value: 'wallet',
    label: '我的钱包',
    description: '余额查询与快捷充值',
    attention: false
  },
  {
    value: 'orders',
    label: '订单记录',
    description: '交易进度跟踪与售后',
    attention: false
  },
  {
    value: 'profile',
    label: '个人中心',
    description: currentUser.value?.realNameVerified
      ? '查看账号信息'
      : '完善个人资料与实名认证',
    attention: !currentUser.value?.realNameVerified
  }
]);

const selectSection = (value) => {
  activeSection.value = value;
};

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
  activeSection.value = 'houses';
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
  display: flex;
  flex-direction: column;
  margin: 0 auto;
  max-width: 1280px;
  padding: 1.5rem;
  gap: 1.5rem;
}

.header {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  padding: 1.5rem;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(37, 99, 235, 0.2);
  display: grid;
  gap: 1rem;
}

.header h1 {
  margin: 0 0 0.5rem;
  font-size: 2rem;
}

.session {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.logout {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 999px;
  color: #fff;
  cursor: pointer;
  font-weight: 600;
  padding: 0.5rem 1.25rem;
  transition: background 0.2s ease, transform 0.2s ease;
}

.logout:hover {
  background: rgba(255, 255, 255, 0.35);
  transform: translateY(-1px);
}

.login-section {
  display: flex;
  justify-content: center;
}

.alert {
  background: #fee2e2;
  border-left: 4px solid #ef4444;
  border-radius: 0.75rem;
  color: #991b1b;
  padding: 1rem 1.5rem;
}

.success {
  background: rgba(34, 197, 94, 0.15);
  border-left: 4px solid #22c55e;
  border-radius: 0.75rem;
  color: #f0fdf4;
  margin: 0;
  padding: 0.75rem 1rem;
}

.workspace {
  display: flex;
  align-items: flex-start;
  gap: 1.5rem;
}

.sidebar {
  flex: 0 0 240px;
  display: flex;
}

.menu-card {
  background: #fff;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.1);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  width: 100%;
}

.menu-card h2 {
  margin: 0;
  font-size: 1.25rem;
  color: #1f2937;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.menu-item {
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  border-radius: 0.85rem;
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  text-align: left;
  color: #1f2937;
  transition: border-color 0.2s ease, background 0.2s ease, transform 0.2s ease;
  font-size: 0.95rem;
}

.menu-item .menu-label {
  font-weight: 600;
  font-size: 1rem;
}

.menu-item .menu-description {
  color: #64748b;
  font-size: 0.85rem;
}

.menu-item.active {
  border-color: #2563eb;
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.15);
  transform: translateY(-1px);
}

.menu-item:not(.active):hover {
  border-color: #94a3b8;
  background: #fff;
}

.menu-item.attention {
  border-color: #f97316;
  background: rgba(251, 191, 36, 0.12);
}

.menu-item.attention .menu-label {
  color: #c2410c;
}

.main-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.panel {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.houses-layout {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  align-items: stretch;
}

.form-section,
.list-section {
  background: #fff;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.1);
  padding: 1.5rem;
}

.list-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.profile-panel {
  align-items: stretch;
}

.footer {
  text-align: center;
  color: #6b7280;
  padding: 1.5rem 0 0.5rem;
}

@media (max-width: 1024px) {
  .workspace {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .houses-layout {
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  }
}

@media (max-width: 640px) {
  .app {
    padding: 1rem;
  }

  .menu-card {
    padding: 1.25rem;
  }

  .form-section,
  .list-section {
    padding: 1.25rem;
  }
}
</style>
