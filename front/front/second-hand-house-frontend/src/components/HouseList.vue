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
            <th>价格 (元)</th>
            <th>面积 (㎡)</th>
            <th>楼层</th>
            <th>关键词</th>
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
            <td>{{ house.floor ?? '-' }}</td>
            <td>{{ formatKeywords(house.keywords) }}</td>
            <td>{{ formatDate(house.listingDate) }}</td>
            <td>{{ house.sellerUsername || '-' }}</td>
            <td>{{ house.sellerName }}</td>
            <td>{{ maskPhone(house.contactNumber) }}</td>
            <td v-if="canManage" class="actions">
              <button class="btn small" :disabled="!canManage" @click.stop="handleEdit(house)">编辑</button>
              <button class="btn small danger" :disabled="!canManage" @click.stop="handleRemove(house)">删除</button>
            </td>
            <td v-else-if="isBuyer" class="actions">
              <button
                class="btn small"
                :disabled="purchaseDisabled || !buyerVerified"
                @click.stop="handlePurchase(house)"
              >
                {{
                  !buyerVerified
                    ? '需实名认证'
                    : purchaseDisabled
                    ? '处理中...'
                    : '立即购买'
                }}
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

const emit = defineEmits(['edit', 'remove', 'purchase', 'view']);

const { houses, loading, canManage } = toRefs(props);

const isBuyer = computed(() => props.currentUser?.role === 'BUYER');
const buyerVerified = computed(() => props.currentUser?.realNameVerified === true);
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

const formatKeywords = (keywords) => {
  if (!Array.isArray(keywords) || keywords.length === 0) {
    return '—';
  }
  return keywords.join('、');
};

const maskPhone = (value) => {
  if (!value) {
    return '-';
  }
  const phone = String(value).trim();
  if (phone.length <= 4) {
    return '*'.repeat(phone.length);
  }
  const prefix = phone.slice(0, Math.min(3, phone.length - 4));
  const suffix = phone.slice(-4);
  return `${prefix}****${suffix}`;
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
  emit('view', house);
};

const closeDetail = () => {
  detailHouse.value = null;
};

defineExpose({
  openDetail
});
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
  color: var(--app-muted);
  background: var(--app-surface-alt);
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
  background: var(--app-surface);
  color: var(--app-text);
}

thead {
  background: var(--accent-gradient);
  color: var(--accent-contrast);
}

th,
td {
  padding: 0.85rem 1rem;
  text-align: left;
  border-bottom: 1px solid var(--app-border);
}

tbody tr:hover {
  background: var(--app-surface-alt);
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
  color: var(--accent-color);
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
  color: var(--app-muted);
  font-size: 0.85rem;
}

.image-count {
  margin: 0.25rem 0 0;
  color: var(--accent-soft-text);
  font-size: 0.8rem;
  background: var(--accent-soft);
  display: inline-flex;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
}

.actions {
  display: flex;
  gap: 0.5rem;
}

.actions.muted {
  color: var(--app-muted);
  font-size: 0.9rem;
}

.btn.small {
  padding: 0.4rem 0.8rem;
  border-radius: 0.65rem;
  border: none;
  background: var(--accent-gradient);
  color: var(--accent-contrast);
  cursor: pointer;
  box-shadow: 0 8px 16px var(--accent-soft);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.btn.small:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 18px var(--accent-soft);
}

.btn.small.danger {
  background: #ef4444;
}

.btn.small:disabled,
.btn.small.danger:disabled {
  background: var(--accent-soft);
  color: var(--accent-soft-text);
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}
</style>
