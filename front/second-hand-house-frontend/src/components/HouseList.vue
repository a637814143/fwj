<template>
  <div class="house-list">
    <div class="list-header">
      <h2>房源列表</h2>
      <p>共 {{ houses.length }} 套房源</p>
    </div>

    <div v-if="loading" class="loading">数据加载中...</div>
    <div v-else-if="houses.length === 0" class="empty">
      {{ canManage ? '暂未添加房源，请先通过表单添加。' : '暂未发布房源，稍后再来看看吧。' }}
    </div>

    <div v-else class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>标题</th>
            <th>地址</th>
            <th>价格 (万元)</th>
            <th>面积 (㎡)</th>
            <th>挂牌日期</th>
            <th>卖家账号</th>
            <th>卖家姓名</th>
            <th>联系方式</th>
            <th>房源图片</th>
            <th v-if="canManage || isBuyer">操作</th>
            <th v-else>权限</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="house in houses" :key="house.id">
            <td>
              <strong>{{ house.title }}</strong>
              <p class="description" v-if="house.description">{{ house.description }}</p>
            </td>
            <td>{{ house.address }}</td>
            <td>{{ formatNumber(house.price) }}</td>
            <td>{{ formatNumber(house.area) }}</td>
            <td>{{ formatDate(house.listingDate) }}</td>
            <td>{{ house.sellerUsername || '-' }}</td>
            <td>{{ house.sellerName }}</td>
            <td>{{ house.contactNumber }}</td>
            <td>
              <div v-if="house.imageUrls && house.imageUrls.length" class="thumbnails">
                <img
                  v-for="(url, index) in house.imageUrls"
                  :key="index"
                  :src="url"
                  :alt="`房源图片${index + 1}`"
                  loading="lazy"
                />
              </div>
              <span v-else class="muted">暂未提供</span>
            </td>
            <td v-if="canManage" class="actions">
              <template v-if="canEditHouse(house) || canDeleteHouse(house)">
                <button
                  v-if="canEditHouse(house)"
                  class="btn small"
                  :disabled="!canEditHouse(house)"
                  @click="handleEdit(house)"
                >
                  编辑
                </button>
                <button
                  v-if="canDeleteHouse(house)"
                  class="btn small danger"
                  :disabled="!canDeleteHouse(house)"
                  @click="handleRemove(house)"
                >
                  删除
                </button>
              </template>
              <span v-else class="muted">仅限发布者维护</span>
            </td>
            <td v-else-if="isBuyer" class="actions">
              <button class="btn small secondary" @click="handleContactSeller(house)">联系卖家</button>
              <button class="btn small" :disabled="purchaseDisabled" @click="handlePurchase(house)">
                {{ purchaseDisabled ? '处理中...' : '立即购买' }}
              </button>
            </td>
            <td v-else class="actions muted">仅支持浏览</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, toRefs } from 'vue';

const props = defineProps({
  houses: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  canManage: {
    type: Boolean,
    default: true
  },
  currentUser: {
    type: Object,
    default: null
  },
  ordersLoading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['edit', 'remove', 'purchase', 'contact-seller']);

const { houses, loading, canManage } = toRefs(props);

const isBuyer = computed(() => props.currentUser?.role === 'BUYER');
const isAdmin = computed(() => props.currentUser?.role === 'ADMIN');
const isSeller = computed(() => props.currentUser?.role === 'SELLER');
const purchaseDisabled = computed(() => props.ordersLoading || loading.value);

const formatNumber = (value) => {
  if (value == null || value === '') {
    return '-';
  }
  return Number(value).toLocaleString('zh-CN', {
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

const handleEdit = (house) => {
  if (!canEditHouse(house)) {
    return;
  }
  emit('edit', house);
};

const handleRemove = (house) => {
  if (!canDeleteHouse(house)) {
    return;
  }
  emit('remove', house);
};

const handlePurchase = (house) => {
  if (!isBuyer.value || purchaseDisabled.value) {
    return;
  }
  emit('purchase', house);
};

const handleContactSeller = (house) => {
  if (!house?.sellerUsername) {
    return;
  }
  emit('contact-seller', {
    sellerUsername: house.sellerUsername,
    sellerName: house.sellerName
  });
};

const canEditHouse = (house) => {
  if (!canManage.value) {
    return false;
  }
  if (isAdmin.value) {
    return true;
  }
  return isSeller.value && props.currentUser?.username === house.sellerUsername;
};

const canDeleteHouse = (house) => {
  if (!canManage.value) {
    return false;
  }
  if (isAdmin.value) {
    return true;
  }
  return isSeller.value && props.currentUser?.username === house.sellerUsername;
};
</script>

<style scoped>
.house-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.loading,
.empty {
  padding: 2rem;
  text-align: center;
  color: #475569;
  background: #f8fafc;
  border-radius: 1rem;
}

.table-wrapper {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  border-radius: 1rem;
  overflow: hidden;
  background: #fff;
}

thead {
  background: #1d4ed8;
  color: #fff;
}

th,
td {
  padding: 0.85rem 1rem;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

tbody tr:hover {
  background: #f1f5f9;
}

.description {
  margin: 0.35rem 0 0;
  color: #64748b;
  font-size: 0.85rem;
}

.thumbnails {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.thumbnails img {
  width: 72px;
  height: 48px;
  object-fit: cover;
  border-radius: 0.5rem;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.08);
}

.actions {
  display: flex;
  gap: 0.5rem;
}

.actions.muted {
  color: #94a3b8;
  font-size: 0.9rem;
}

.muted {
  color: #94a3b8;
  font-size: 0.85rem;
}

.btn.small {
  padding: 0.4rem 0.8rem;
  border-radius: 0.65rem;
  border: none;
  background: #2563eb;
  color: #fff;
}

.btn.small.secondary {
  background: #f1f5f9;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}

.btn.small.danger {
  background: #ef4444;
}

.btn.small:disabled,
.btn.small.danger:disabled {
  background: #cbd5f5;
  color: #475569;
  cursor: not-allowed;
}
</style>
