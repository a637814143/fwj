<template>
  <div class="house-list">
    <div class="list-header">
      <h2>房源列表</h2>
      <div class="list-meta">
        <span class="stat-chip">共 {{ houses.length }} 套房源</span>
        <span class="stat-chip muted">每页展示 {{ pageSize }} 套</span>
      </div>
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
          <tr v-for="house in paginatedHouses" :key="house.id">
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

    <div v-if="houses.length > pageSize" class="pagination">
      <button type="button" class="page-btn" :disabled="!canGoPrev" @click="goPrev">上一页</button>
      <div class="page-info">
        <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
        <span>共 {{ houses.length }} 套</span>
      </div>
      <button type="button" class="page-btn" :disabled="!canGoNext" @click="goNext">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, toRefs, watch } from 'vue';
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

const pageSize = ref(6);
const currentPage = ref(1);

const isBuyer = computed(() => props.currentUser?.role === 'BUYER');
const buyerVerified = computed(() => props.currentUser?.realNameVerified === true);
const purchaseDisabled = computed(() => props.ordersLoading || loading.value);
const detailHouse = ref(null);

const totalPages = computed(() => {
  if (!houses.value.length) {
    return 1;
  }
  return Math.max(1, Math.ceil(houses.value.length / pageSize.value));
});

const paginatedHouses = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return houses.value.slice(start, start + pageSize.value);
});

const canGoPrev = computed(() => currentPage.value > 1);
const canGoNext = computed(() => currentPage.value < totalPages.value);

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

const goToPage = (page) => {
  const normalized = Math.min(Math.max(page, 1), totalPages.value);
  currentPage.value = normalized;
};

const goPrev = () => {
  if (canGoPrev.value) {
    goToPage(currentPage.value - 1);
  }
};

const goNext = () => {
  if (canGoNext.value) {
    goToPage(currentPage.value + 1);
  }
};

watch(
  () => houses.value.length,
  () => {
    goToPage(1);
  }
);

watch(totalPages, (value) => {
  if (currentPage.value > value) {
    currentPage.value = value;
  }
});
</script>

<style scoped>
.house-list {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.list-meta {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.stat-chip {
  padding: 0.35rem 0.85rem;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.18);
  color: #4338ca;
  font-size: 0.82rem;
  font-weight: 500;
}

.stat-chip.muted {
  background: rgba(148, 163, 184, 0.18);
  color: #475569;
}

.loading,
.empty {
  padding: 2.25rem;
  text-align: center;
  color: #475569;
  background: rgba(241, 245, 249, 0.8);
  border-radius: 1.25rem;
  border: 1px solid rgba(226, 232, 240, 0.7);
}

.table-wrapper {
  overflow-x: auto;
  border-radius: 1.5rem;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 860px;
}

thead {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.28), rgba(59, 130, 246, 0.2));
  color: #0f172a;
}

th,
td {
  padding: 0.95rem 1.1rem;
  text-align: left;
  border-bottom: 1px solid rgba(226, 232, 240, 0.7);
}

tbody tr:hover {
  background: rgba(191, 219, 254, 0.18);
}

.title-button {
  display: inline-flex;
  gap: 0.4rem;
  align-items: center;
  background: none;
  border: none;
  padding: 0;
  margin: 0;
  font: inherit;
  color: #4338ca;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.title-button:hover,
.title-button:focus {
  transform: translateX(2px);
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
  font-size: 0.78rem;
  background: rgba(191, 219, 254, 0.6);
  display: inline-flex;
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
}

.actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.actions.muted {
  color: #94a3b8;
  font-size: 0.9rem;
}

.btn.small {
  padding: 0.45rem 0.9rem;
  border-radius: 0.75rem;
  border: none;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.9), rgba(59, 130, 246, 0.75));
  color: #fff;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.btn.small:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 18px rgba(79, 70, 229, 0.25);
}

.btn.small.danger {
  background: linear-gradient(135deg, rgba(248, 113, 113, 0.95), rgba(239, 68, 68, 0.9));
}

.btn.small:disabled,
.btn.small.danger:disabled {
  background: rgba(148, 163, 184, 0.45);
  color: rgba(15, 23, 42, 0.6);
  cursor: not-allowed;
  box-shadow: none;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  border-radius: 1rem;
  padding: 0.85rem 1.25rem;
  background: rgba(15, 23, 42, 0.04);
}

.page-btn {
  padding: 0.5rem 1.25rem;
  border-radius: 999px;
  border: none;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.85), rgba(14, 165, 233, 0.7));
  color: #fff;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.page-btn:disabled {
  background: rgba(148, 163, 184, 0.4);
  color: rgba(15, 23, 42, 0.6);
  cursor: not-allowed;
  box-shadow: none;
}

.page-btn:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 15px rgba(59, 130, 246, 0.25);
}

.page-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 0.85rem;
  color: #475569;
}

@media (max-width: 960px) {
  table {
    min-width: 100%;
  }

  .pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .page-info {
    flex-direction: row;
    justify-content: space-between;
  }
}
</style>
