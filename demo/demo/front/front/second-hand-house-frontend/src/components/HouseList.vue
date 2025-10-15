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
              <button class="title-button" type="button" @click="openDetail(house)">
                <strong>{{ house.title }}</strong>
              </button>
              <p class="description" v-if="house.description">{{ house.description }}</p>
              <p v-if="house.imageUrls?.length" class="image-count">
                {{ house.imageUrls.length }} 张图片 · 点击标题查看
              </p>
            </td>
            <td>{{ house.address }}</td>
            <td>{{ formatNumber(house.price) }}</td>
            <td>{{ formatNumber(house.area) }}</td>
            <td>{{ formatDate(house.listingDate) }}</td>
            <td>{{ house.sellerUsername || '-' }}</td>
            <td>{{ house.sellerName }}</td>
            <td>{{ house.contactNumber }}</td>
            <td v-if="canManage" class="actions">
              <button class="btn small" :disabled="!canManage" @click.stop="handleEdit(house)">编辑</button>
              <button class="btn small danger" :disabled="!canManage" @click.stop="handleRemove(house)">删除</button>
            </td>
            <td v-else-if="isBuyer" class="actions">
              <button class="btn small" :disabled="purchaseDisabled" @click.stop="handlePurchase(house)">
                {{ purchaseDisabled ? '处理中...' : '立即购买' }}
              </button>
            </td>
            <td v-else class="actions muted">仅支持浏览</td>
          </tr>
        </tbody>
      </table>
    </div>

    <HouseDetailModal v-if="detailHouse" :house="detailHouse" @close="closeDetail" />
  </div>
</template>

<script setup>
import { computed, ref, toRefs } from 'vue';
import HouseDetailModal from './HouseDetailModal.vue';

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
const detailHouse = ref(null);

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

const openDetail = (house) => {
  detailHouse.value = {
    ...house,
    imageUrls: Array.isArray(house.imageUrls) ? [...house.imageUrls] : []
  };
};

const closeDetail = () => {
  detailHouse.value = null;
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

.title-button {
  display: inline-flex;
  gap: 0.35rem;
  align-items: center;
  background: none;
  border: none;
  padding: 0;
  margin: 0;
  font: inherit;
  color: #1d4ed8;
  cursor: pointer;
}

.title-button strong {
  font-size: 1rem;
}

.title-button:hover,
.title-button:focus {
  text-decoration: underline;
}

.description {
  margin: 0.35rem 0 0;
  color: #64748b;
  font-size: 0.85rem;
}

.image-count {
  margin: 0.25rem 0 0;
  color: #0f172a;
  font-size: 0.8rem;
  background: #e0f2fe;
  display: inline-flex;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
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
  cursor: pointer;
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
