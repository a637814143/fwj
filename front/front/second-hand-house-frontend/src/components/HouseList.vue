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
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
  padding: 1.5rem;
  border-radius: 1.75rem;
  background: radial-gradient(circle at top, rgba(255, 247, 237, 0.8), rgba(255, 239, 221, 0.95));
  box-shadow: 0 18px 48px rgba(88, 44, 25, 0.18);
  overflow: hidden;
  font-family: 'Noto Serif SC', 'Songti SC', 'SimSun', serif;
}

.house-list::before {
  content: '';
  position: absolute;
  inset: -25% -15% auto;
  height: 70%;
  background: radial-gradient(circle at top, rgba(187, 28, 36, 0.25), transparent 70%);
  z-index: 0;
}

.house-list::after {
  content: '';
  position: absolute;
  inset: auto -20% -35% -20%;
  height: 60%;
  background: radial-gradient(circle at bottom, rgba(201, 128, 35, 0.22), transparent 65%);
  z-index: 0;
}

.list-header {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding: 0.75rem 0 1rem;
  border-bottom: 1px solid rgba(178, 42, 29, 0.18);
}

.list-header h2 {
  margin: 0;
  font-size: 1.75rem;
  letter-spacing: 0.08em;
  font-weight: 600;
  color: #a32020;
}

.list-header p {
  margin: 0.35rem 0 0;
  color: #7c3d1a;
  font-size: 0.95rem;
}

.page-indicator {
  font-size: 0.95rem;
  color: #f9f5f1;
  background: linear-gradient(135deg, #b9272d, #d24a1a);
  padding: 0.35rem 0.85rem;
  border-radius: 999px;
  letter-spacing: 0.12em;
  box-shadow: 0 10px 16px rgba(168, 37, 28, 0.22);
}

.loading,
.empty {
  position: relative;
  z-index: 1;
  padding: 2.75rem;
  text-align: center;
  color: #8c4a25;
  background: linear-gradient(135deg, rgba(255, 245, 233, 0.92), rgba(249, 233, 214, 0.88));
  border-radius: 1.5rem;
  font-size: 1.05rem;
  letter-spacing: 0.05em;
  border: 1px solid rgba(201, 128, 35, 0.25);
}

.list-body {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.card-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
}

.house-card {
  position: relative;
  flex: 1 1 calc(50% - 1.5rem);
  min-width: 300px;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.92), rgba(255, 246, 234, 0.95));
  border-radius: 1.5rem;
  padding: 1.75rem;
  box-shadow: 0 24px 58px rgba(92, 45, 23, 0.16);
  border: 1px solid rgba(194, 121, 45, 0.35);
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
  transition: transform 0.35s ease, box-shadow 0.35s ease;
  overflow: hidden;
}

.house-card::before {
  content: '';
  position: absolute;
  inset: -30% 40% auto -40%;
  height: 70%;
  background: radial-gradient(circle at left, rgba(212, 74, 26, 0.18), transparent 70%);
  z-index: 0;
}

.house-card::after {
  content: '';
  position: absolute;
  inset: auto -30% -45% 35%;
  height: 70%;
  background: radial-gradient(circle at bottom, rgba(180, 40, 36, 0.16), transparent 70%);
  z-index: 0;
}

.house-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 32px 65px rgba(122, 52, 21, 0.24);
}

.card-header {
  position: relative;
  z-index: 1;
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
  color: #7c2a1b;
  cursor: pointer;
  text-align: left;
}

.title-button strong {
  font-size: 1.2rem;
  font-weight: 600;
}

.title-button:hover,
.title-button:focus {
  color: #b8321b;
}

.price {
  font-weight: 600;
  font-size: 1.15rem;
  color: #b9272d;
  background: linear-gradient(125deg, #b9272d, #e87524);
  -webkit-background-clip: text;
  color: transparent;
}

.description {
  position: relative;
  z-index: 1;
  margin: 0;
  color: #5d331d;
  font-size: 0.97rem;
  line-height: 1.7;
}

.image-count {
  position: relative;
  z-index: 1;
  margin: 0;
  color: #a53c1a;
  font-size: 0.82rem;
  background: rgba(212, 96, 32, 0.15);
  display: inline-flex;
  padding: 0.3rem 0.85rem;
  border-radius: 999px;
  align-self: flex-start;
}

.card-meta {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 1rem;
  margin: 0;
}

.card-meta div {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.card-meta dt {
  font-size: 0.78rem;
  color: #c0612d;
  letter-spacing: 0.12em;
}

.card-meta dd {
  margin: 0;
  color: #4b2717;
  font-weight: 600;
}

.keyword {
  display: inline-block;
  background: rgba(185, 39, 45, 0.12);
  border-radius: 999px;
  padding: 0.3rem 0.85rem;
  font-size: 0.78rem;
  color: #a32020;
}

.seller-info {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 0.85rem;
  padding: 0.85rem 1.15rem;
  background: rgba(255, 248, 238, 0.88);
  border-radius: 1.1rem;
  border: 1px solid rgba(210, 131, 47, 0.3);
}

.seller-info .label {
  display: block;
  font-size: 0.78rem;
  color: #bd6b2e;
  letter-spacing: 0.08em;
}

.seller-info .value {
  display: block;
  font-size: 0.98rem;
  font-weight: 600;
  color: #4b2717;
}

.card-footer {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.actions {
  display: flex;
  gap: 0.85rem;
  align-items: center;
}

.actions.muted {
  color: #b6713c;
  font-size: 0.92rem;
}

.btn {
  padding: 0.5rem 1.4rem;
  border-radius: 999px;
  border: none;
  background: rgba(166, 52, 25, 0.12);
  color: #7c2a1b;
  cursor: pointer;
  font-weight: 600;
  letter-spacing: 0.05em;
  transition: transform 0.3s ease, box-shadow 0.3s ease, background 0.3s ease;
}

.btn:hover:not(:disabled) {
  transform: translateY(-2px);
  background: rgba(166, 52, 25, 0.24);
  box-shadow: 0 12px 18px rgba(122, 52, 21, 0.22);
}

.btn:disabled {
  background: rgba(189, 176, 160, 0.45);
  color: rgba(124, 42, 27, 0.45);
  cursor: not-allowed;
  box-shadow: none;
}

.btn.primary {
  background: linear-gradient(135deg, #b9272d, #e06a24);
  color: #fff7e6;
  box-shadow: 0 16px 28px rgba(185, 39, 45, 0.25);
}

.btn.primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #a32020, #cf5c21);
}

.btn.danger {
  background: linear-gradient(135deg, #8f1d22, #b9272d);
  color: #fff4e6;
  box-shadow: 0 16px 28px rgba(143, 29, 34, 0.24);
}

.btn.danger:hover:not(:disabled) {
  background: linear-gradient(135deg, #7d181d, #a32020);
}

.pagination {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1.25rem;
  padding: 0.75rem 0;
}

.pager {
  padding: 0.55rem 1.6rem;
  border-radius: 999px;
  border: none;
  background: rgba(185, 39, 45, 0.1);
  color: #7c2a1b;
  cursor: pointer;
  font-weight: 600;
  letter-spacing: 0.1em;
  transition: background 0.3s ease, transform 0.3s ease;
}

.pager:hover:not(:disabled) {
  background: rgba(185, 39, 45, 0.18);
  transform: translateY(-2px);
}

.pager:disabled {
  background: rgba(189, 176, 160, 0.35);
  color: rgba(124, 42, 27, 0.5);
  cursor: not-allowed;
}

.pager-dots {
  display: flex;
  gap: 0.6rem;
}

.dot {
  width: 2.35rem;
  height: 2.35rem;
  border-radius: 50%;
  border: none;
  background: rgba(185, 39, 45, 0.1);
  color: #7c2a1b;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.dot:hover {
  background: rgba(185, 39, 45, 0.2);
}

.dot.active {
  background: linear-gradient(135deg, #b9272d, #e06a24);
  color: #fff9f2;
  box-shadow: 0 14px 26px rgba(185, 39, 45, 0.28);
}

@media (max-width: 960px) {
  .house-card {
    flex: 1 1 100%;
  }

  .card-meta {
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  }
}

@media (max-width: 640px) {
  .house-list {
    padding: 1.25rem;
  }

  .list-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .pagination {
    flex-direction: column;
    gap: 0.85rem;
  }

  .pager-dots {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>
