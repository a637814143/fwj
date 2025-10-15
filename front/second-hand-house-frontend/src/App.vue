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

      <section class="filters-section">
        <HouseFilters
          v-model="filters"
          :loading="loading"
          @submit="handleFilterSubmit"
          @reset="handleFilterReset"
        />
      </section>

      <main class="content">
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
          <HouseList
            :houses="houses"
            :loading="loading"
            :can-manage="canManageHouses"
            :current-user="currentUser"
            :orders-loading="ordersLoading"
            @edit="handleEdit"
            @remove="handleRemove"
            @purchase="handlePurchase"
            @view-reviews="handleViewReviews"
            @review="handleReview"
          />
        </section>
      </main>

      <section class="reviews-section">
        <HouseReviews
          :house="selectedReviewHouse"
          :reviews="houseReviews"
          :loading="reviewsLoading"
          :submitting="reviewSubmitting"
          :current-user="currentUser"
          @submit-review="handleSubmitReview"
          @refresh="handleRefreshReviews"
        />
      </section>

      <section class="wallet-order-section">
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
        />
      </section>
    </template>

    <footer class="footer">
      <small>后端接口地址：{{ apiBaseUrl }}</small>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import axios from 'axios';
import HouseFilters from './components/HouseFilters.vue';
import HouseForm from './components/HouseForm.vue';
import HouseList from './components/HouseList.vue';
import HouseReviews from './components/HouseReviews.vue';
import RoleLogin from './components/RoleLogin.vue';
import WalletPanel from './components/WalletPanel.vue';
import OrderHistory from './components/OrderHistory.vue';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';
const houses = ref([]);
const loading = ref(false);
const selectedHouse = ref(null);
const selectedReviewHouse = ref(null);
const currentUser = ref(null);
const wallet = ref(null);
const walletLoading = ref(false);
const orders = ref([]);
const ordersLoading = ref(false);
const houseReviews = ref([]);
const reviewsLoading = ref(false);
const reviewSubmitting = ref(false);
const messages = reactive({ error: '', success: '' });
const storageKey = 'secondhand-house-current-user';

const filters = reactive({
  keyword: '',
  sellerUsername: '',
  minPrice: '',
  maxPrice: '',
  minArea: '',
  maxArea: '',
  listedFrom: '',
  listedTo: ''
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

const isSeller = computed(() => currentUser.value?.role === 'SELLER');
const isBuyer = computed(() => currentUser.value?.role === 'BUYER');

const canManageHouses = computed(
  () => currentUser.value && ['SELLER', 'ADMIN'].includes(currentUser.value.role)
);

const buildHouseQuery = () => {
  const params = {};
  if (filters.keyword) params.keyword = filters.keyword;
  if (filters.sellerUsername) params.sellerUsername = filters.sellerUsername;
  if (filters.minPrice !== '' && filters.minPrice != null) params.minPrice = filters.minPrice;
  if (filters.maxPrice !== '' && filters.maxPrice != null) params.maxPrice = filters.maxPrice;
  if (filters.minArea !== '' && filters.minArea != null) params.minArea = filters.minArea;
  if (filters.maxArea !== '' && filters.maxArea != null) params.maxArea = filters.maxArea;
  if (filters.listedFrom) params.listedFrom = filters.listedFrom;
  if (filters.listedTo) params.listedTo = filters.listedTo;
  return params;
};

const fetchHouses = async () => {
  loading.value = true;
  messages.error = '';
  try {
    const { data } = await client.get('/houses', { params: buildHouseQuery() });
    houses.value = data.map((house) => ({
      ...house,
      listingDate: house.listingDate ?? '',
      imageUrls: Array.isArray(house.imageUrls) ? house.imageUrls : []
    }));
    if (selectedReviewHouse.value) {
      const refreshed = houses.value.find((item) => item.id === selectedReviewHouse.value.id);
      if (refreshed) {
        selectedReviewHouse.value = refreshed;
      }
    }
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
  if (Array.isArray(result.imageUrls)) {
    result.imageUrls = result.imageUrls
      .map((url) => (url == null ? '' : String(url).trim()))
      .filter((url) => url);
  } else {
    result.imageUrls = [];
  }
  return result;
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
    if (selectedHouse.value) {
      const { data } = await client.put(`/houses/${selectedHouse.value.id}`, requestPayload);
      houses.value = houses.value.map((house) =>
        house.id === data.id ? data : house
      );
      messages.success = `房源《${data.title}》已更新。`;
    } else {
      const { data } = await client.post('/houses', requestPayload);
      houses.value = [...houses.value, data];
      messages.success = `已新增房源《${data.title}》。`;
    }
    selectedHouse.value = null;
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
  selectedHouse.value = { ...house };
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
    houses.value = houses.value.filter((item) => item.id !== house.id);
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
  ordersLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    const { data } = await client.post('/orders', {
      houseId: house.id,
      buyerUsername: currentUser.value.username
    });
    messages.success = `成功购买房源《${data.houseTitle}》，支付金额 ${Number(data.amount).toFixed(2)} 万元。`;
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

const handleLoginSuccess = (user) => {
  currentUser.value = user;
  messages.success = user.message ?? '';
  messages.error = '';
  try {
    localStorage.setItem(storageKey, JSON.stringify(user));
  } catch (error) {
    console.warn('无法持久化登录状态：', error);
  }
  fetchHouses();
  fetchWallet();
  fetchOrders();
};

const handleLogout = () => {
  currentUser.value = null;
  houses.value = [];
  selectedHouse.value = null;
  selectedReviewHouse.value = null;
  houseReviews.value = [];
  reviewsLoading.value = false;
  reviewSubmitting.value = false;
  wallet.value = null;
  orders.value = [];
  walletLoading.value = false;
  ordersLoading.value = false;
  messages.error = '';
  messages.success = '';
  localStorage.removeItem(storageKey);
};

const handleFilterSubmit = () => {
  fetchHouses();
};

const handleFilterReset = () => {
  fetchHouses();
};

const fetchHouseReviews = async (house, { silent = false } = {}) => {
  if (!house?.id) {
    return;
  }
  if (!silent) {
    reviewsLoading.value = true;
  }
  messages.error = '';
  try {
    const { data } = await client.get(`/houses/${house.id}/reviews`);
    houseReviews.value = data;
  } catch (error) {
    const detail = error.response?.data?.detail ?? '加载评价失败。';
    messages.error = detail;
  } finally {
    if (!silent) {
      reviewsLoading.value = false;
    }
  }
};

const handleViewReviews = (house) => {
  selectedReviewHouse.value = house;
  fetchHouseReviews(house);
};

const handleReview = (house) => {
  if (!isBuyer.value) {
    messages.error = '登录买家账号后才可以评价房源。';
    messages.success = '';
    return;
  }
  selectedReviewHouse.value = house;
  fetchHouseReviews(house);
};

const handleSubmitReview = async ({ rating, comment }) => {
  if (!isBuyer.value) {
    messages.error = '只有买家可以提交评价。';
    return;
  }
  if (!selectedReviewHouse.value) {
    messages.error = '请先选择需要评价的房源。';
    return;
  }
  reviewSubmitting.value = true;
  messages.error = '';
  messages.success = '';
  try {
    const { data } = await client.post(`/houses/${selectedReviewHouse.value.id}/reviews`, {
      buyerUsername: currentUser.value.username,
      buyerDisplayName: currentUser.value.displayName ?? currentUser.value.username,
      rating,
      comment
    });
    houseReviews.value = [data, ...houseReviews.value];
    messages.success = '评价提交成功。';
  } catch (error) {
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? '提交评价失败，请稍后再试。';
    }
  } finally {
    reviewSubmitting.value = false;
  }
};

const handleRefreshReviews = () => {
  if (selectedReviewHouse.value) {
    fetchHouseReviews(selectedReviewHouse.value);
  }
};

onMounted(() => {
  try {
    const cached = localStorage.getItem(storageKey);
    if (cached) {
      const user = JSON.parse(cached);
      currentUser.value = user;
      messages.success = '已恢复上次的登录状态。';
      fetchHouses();
      fetchWallet();
      fetchOrders();
      return;
    }
  } catch (error) {
    console.warn('恢复登录状态失败：', error);
    localStorage.removeItem(storageKey);
  }
  fetchHouses();
});
</script>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  margin: 0 auto;
  max-width: 1200px;
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

.content {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.filters-section,
.reviews-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.wallet-order-section {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.form-section,
.list-section {
  background: white;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.1);
  padding: 1.5rem;
}

.footer {
  text-align: center;
  color: #6b7280;
  padding: 1.5rem 0 0.5rem;
}
</style>
