<template>
  <div class="house-list">
    <div class="list-header">
      <div>
        <h2>房源列表</h2>
        <p>共 {{ houses.length }} 套房源</p>
      </div>
      <div v-if="totalPages > 1" class="page-indicator">第 {{ currentPage }} / {{ totalPages }} 页</div>
    </div>

    <div v-if="loading" class="loading">数据加载中...</div>
    <div v-else-if="houses.length === 0" class="empty">
      {{ canManage ? '暂未添加房源，请先通过表单添加。' : '暂未发布房源，稍后再来看看吧。' }}
    </div>

    <div v-else class="list-body">
      <div class="card-grid">
        <article v-for="house in displayedHouses" :key="house.id" class="house-card">
          <header class="card-header">
            <button class="title-button" type="button" @click="openDetail(house)">
              <strong>{{ house.title }}</strong>
            </button>
            <span class="price">{{ formatPrice(house.price) }}</span>
          </header>

          <p class="description" v-if="house.description">{{ house.description }}</p>
          <p v-if="house.imageUrls?.length" class="image-count">
            {{ house.imageUrls.length }} 张图片 · 点击标题查看
          </p>

          <dl class="card-meta">
            <div>
              <dt>地址</dt>
              <dd>{{ house.address }}</dd>
            </div>
            <div>
              <dt>面积</dt>
              <dd>{{ formatArea(house.area) }}</dd>
            </div>
            <div>
              <dt>楼层</dt>
              <dd>{{ house.floor ?? '—' }}</dd>
            </div>
            <div>
              <dt>挂牌日期</dt>
              <dd>{{ formatDate(house.listingDate) }}</dd>
            </div>
            <div>
              <dt>关键词</dt>
              <dd>
                <span v-if="formatKeywords(house.keywords) !== '—'" class="keyword">
                  {{ formatKeywords(house.keywords) }}
                </span>
                <span v-else>—</span>
              </dd>
            </div>
          </dl>

          <div class="seller-info">
            <div>
              <span class="label">卖家账号</span>
              <span class="value">{{ house.sellerUsername || '—' }}</span>
            </div>
            <div>
              <span class="label">卖家姓名</span>
              <span class="value">{{ house.sellerName }}</span>
            </div>
            <div>
              <span class="label">联系方式</span>
              <span class="value">{{ maskPhone(house.contactNumber) }}</span>
            </div>
          </div>

          <footer class="card-footer">
            <div v-if="canManage" class="actions">
              <button class="btn" :disabled="!canManage" @click.stop="handleEdit(house)">编辑</button>
              <button class="btn danger" :disabled="!canManage" @click.stop="handleRemove(house)">删除</button>
            </div>
            <div v-else-if="isBuyer" class="actions">
              <button
                class="btn primary"
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
            </div>
            <div v-else class="actions muted">仅支持浏览</div>
          </footer>
        </article>
      </div>

      <nav v-if="totalPages > 1" class="pagination">
        <button class="pager" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">上一页</button>
        <div class="pager-dots">
          <button
            v-for="page in totalPages"
            :key="page"
            class="dot"
            :class="{ active: page === currentPage }"
            @click="goToPage(page)"
          >
            {{ page }}
          </button>
        </div>
        <button
          class="pager"
          :disabled="currentPage === totalPages"
          @click="goToPage(currentPage + 1)"
        >
          下一页
        </button>
      </nav>
    </div>

    <HouseDetailModal v-if="detailHouse" :house="detailHouse" @close="closeDetail" />
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

const PAGE_SIZE = 6;
const currentPage = ref(1);
const totalPages = computed(() => Math.max(1, Math.ceil(houses.value.length / PAGE_SIZE)));
const displayedHouses = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE;
  return houses.value.slice(start, start + PAGE_SIZE);
});

watch(
  () => houses.value,
  () => {
    currentPage.value = 1;
  }
);

watch(totalPages, (value) => {
  if (currentPage.value > value) {
    currentPage.value = value;
  }
});

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

const formatPrice = (value) => {
  const formatted = formatNumber(value);
  return formatted === '-' ? '—' : `￥${formatted}`;
};

const formatArea = (value) => {
  const formatted = formatNumber(value);
  return formatted === '-' ? '—' : `${formatted} ㎡`;
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

const goToPage = (page) => {
  if (page < 1 || page > totalPages.value) {
    return;
  }
  currentPage.value = page;
};

defineExpose({
  openDetail
});
</script>

<style scoped>
.house-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
}

.list-header h2 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: #0f172a;
}

.list-header p {
  margin: 0.25rem 0 0;
  color: #64748b;
}

.page-indicator {
  font-size: 0.9rem;
  color: #1f2937;
  background: rgba(15, 23, 42, 0.05);
  padding: 0.4rem 0.8rem;
  border-radius: 999px;
}

.loading,
.empty {
  padding: 2.5rem;
  text-align: center;
  color: #475569;
  background: linear-gradient(135deg, rgba(241, 245, 249, 0.9), rgba(226, 232, 240, 0.7));
  border-radius: 1.5rem;
  font-size: 1rem;
  letter-spacing: 0.02em;
}

.list-body {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.card-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 1.25rem;
}

.house-card {
  flex: 1 1 calc(50% - 1.25rem);
  min-width: 280px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 1.5rem;
  padding: 1.5rem;
  box-shadow: 0 24px 45px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(12px);
  display: flex;
  flex-direction: column;
  gap: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.12);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.house-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 32px 55px rgba(15, 23, 42, 0.12);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
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
  color: #0f172a;
  cursor: pointer;
  text-align: left;
}

.title-button strong {
  font-size: 1.1rem;
  font-weight: 600;
}

.title-button:hover,
.title-button:focus {
  color: #2563eb;
}

.price {
  font-weight: 600;
  color: #111827;
  font-size: 1.1rem;
  background: linear-gradient(135deg, #2563eb, #60a5fa);
  -webkit-background-clip: text;
  color: transparent;
}

.description {
  margin: 0;
  color: #475569;
  font-size: 0.95rem;
  line-height: 1.6;
}

.image-count {
  margin: 0;
  color: #0f172a;
  font-size: 0.8rem;
  background: rgba(37, 99, 235, 0.12);
  display: inline-flex;
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  align-self: flex-start;
}

.card-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0.85rem;
  margin: 0;
}

.card-meta div {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.card-meta dt {
  font-size: 0.75rem;
  text-transform: uppercase;
  color: #94a3b8;
  letter-spacing: 0.08em;
}

.card-meta dd {
  margin: 0;
  color: #0f172a;
  font-weight: 500;
}

.keyword {
  display: inline-block;
  background: rgba(15, 23, 42, 0.05);
  border-radius: 999px;
  padding: 0.25rem 0.75rem;
  font-size: 0.75rem;
  color: #1e293b;
}

.seller-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: rgba(248, 250, 252, 0.8);
  border-radius: 1rem;
}

.seller-info .label {
  display: block;
  font-size: 0.75rem;
  color: #94a3b8;
  letter-spacing: 0.05em;
}

.seller-info .value {
  display: block;
  font-size: 0.95rem;
  font-weight: 500;
  color: #111827;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.actions.muted {
  color: #94a3b8;
  font-size: 0.9rem;
}

.btn {
  padding: 0.5rem 1.2rem;
  border-radius: 999px;
  border: none;
  background: rgba(15, 23, 42, 0.08);
  color: #0f172a;
  cursor: pointer;
  font-weight: 500;
  transition: background 0.3s ease, transform 0.3s ease;
}

.btn:hover:not(:disabled) {
  transform: translateY(-2px);
}

.btn:disabled {
  background: rgba(148, 163, 184, 0.3);
  color: #94a3b8;
  cursor: not-allowed;
}

.btn.primary {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: white;
}

.btn.danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: white;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  padding: 0.5rem 0;
}

.pager {
  padding: 0.5rem 1.4rem;
  border-radius: 999px;
  border: none;
  background: rgba(15, 23, 42, 0.08);
  color: #0f172a;
  cursor: pointer;
  font-weight: 500;
}

.pager:disabled {
  background: rgba(148, 163, 184, 0.2);
  color: #94a3b8;
  cursor: not-allowed;
}

.pager-dots {
  display: flex;
  gap: 0.5rem;
}

.dot {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 50%;
  border: none;
  background: rgba(15, 23, 42, 0.06);
  color: #1f2937;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.dot.active {
  background: linear-gradient(135deg, #2563eb, #60a5fa);
  color: white;
  box-shadow: 0 12px 22px rgba(37, 99, 235, 0.25);
}

@media (max-width: 960px) {
  .house-card {
    flex: 1 1 100%;
  }

  .card-meta {
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  }
}

@media (max-width: 640px) {
  .pagination {
    flex-direction: column;
    gap: 0.75rem;
  }

  .pager-dots {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>
