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
            <td v-if="canManage" class="actions">
              <button class="btn small" :disabled="!canManage" @click="handleEdit(house)">编辑</button>
              <button class="btn small danger" :disabled="!canManage" @click="handleRemove(house)">删除</button>
            </td>
            <td v-else-if="isBuyer" class="actions">
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

const emit = defineEmits(['edit', 'remove', 'purchase']);

const { houses, loading, canManage } = toRefs(props);

const isBuyer = computed(() => props.currentUser?.role === 'BUYER');
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
  if (!canManage.value) {
    return;
  }
  emit('edit', house);
};

const handleRemove = (house) => {
  if (!canManage.value) {
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

.actions {
  display: flex;
  gap: 0.5rem;
}

.actions.muted {
  color: #94a3b8;
  font-size: 0.9rem;
}

.btn.small {
  padding: 0.4rem 0.8rem;
  border-radius: 0.65rem;
  border: none;
  background: #2563eb;
  color: #fff;
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
