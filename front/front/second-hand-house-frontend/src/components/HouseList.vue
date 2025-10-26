<template>
  <section class="gallery-shell">
    <header class="gallery-header">
      <div class="headline">
        <p class="eyebrow">甄选好房推荐</p>
        <h2>房源清单</h2>
        <p class="subtitle">覆盖城芯到静谧人居的优质房源，随时挑选理想居所。</p>
      </div>
      <ul class="stats" role="list">
        <li class="stat-item">
          <span class="label">在售</span>
          <span class="value">{{ houses.length }}</span>
        </li>
        <li class="stat-item" v-if="totalPages > 1">
          <span class="label">页码</span>
          <span class="value">{{ currentPage }} / {{ totalPages }}</span>
        </li>
        <li class="stat-item" v-if="isBuyer">
          <span class="label">实名状态</span>
          <span class="value">{{ buyerVerified ? '已认证' : '未认证' }}</span>
        </li>
      </ul>
    </header>

    <div class="panel" v-if="loading">
      <span class="spinner" aria-hidden="true"></span>
      <span>房源加载中，请稍候…</span>
    </div>

    <div class="panel empty" v-else-if="houses.length === 0">
      <h3>暂时没有房源</h3>
      <p>
        {{
          canManage
            ? '还没有发布任何房源，快去添加吧。'
            : '暂无房源上架，稍后再来看新的房子。'
        }}
      </p>
    </div>

    <div v-else class="board">
      <div class="card-wrap">
        <article
          v-for="house in displayedHouses"
          :key="house.id"
          class="house-card"
        >
          <header class="card-top">
            <div class="title-block">
              <button type="button" class="title" @click="openDetail(house)">
                {{ house.title }}
              </button>
              <p class="subtitle" v-if="house.description">{{ house.description }}</p>
            </div>
            <div class="price-block">
              <span class="price">{{ formatPrice(house.price) }}</span>
              <span class="hint">总价</span>
            </div>
          </header>

          <section class="card-body">
            <dl class="spec-grid">
              <div class="spec">
                <dt>地址</dt>
                <dd>{{ house.address || '—' }}</dd>
              </div>
              <div class="spec">
                <dt>面积</dt>
                <dd>{{ formatArea(house.area) }}</dd>
              </div>
              <div class="spec">
                <dt>楼层</dt>
                <dd>{{ house.floor ?? '—' }}</dd>
              </div>
              <div class="spec">
                <dt>挂牌</dt>
                <dd>{{ formatDate(house.listingDate) }}</dd>
              </div>
              <div class="spec">
                <dt>标签</dt>
                <dd>
                  <span v-if="formatKeywords(house.keywords) !== '—'" class="keyword">
                    {{ formatKeywords(house.keywords) }}
                  </span>
                  <span v-else>—</span>
                </dd>
              </div>
              <div class="spec" v-if="house.imageUrls?.length">
                <dt>图片</dt>
                <dd>{{ house.imageUrls.length }} 张</dd>
              </div>
            </dl>

            <div class="seller">
              <p class="seller-title">挂牌经纪</p>
              <div class="seller-grid">
                <div>
                  <span class="label">账号</span>
                  <span class="value">{{ house.sellerUsername || '—' }}</span>
                </div>
                <div>
                  <span class="label">姓名</span>
                  <span class="value">{{ house.sellerName || '—' }}</span>
                </div>
                <div>
                  <span class="label">联系方式</span>
                  <span class="value">{{ maskPhone(house.contactNumber) }}</span>
                </div>
              </div>
            </div>
          </section>

          <footer class="card-actions">
            <template v-if="canManage">
              <button type="button" class="btn outline" @click.stop="handleEdit(house)">
                编辑
              </button>
              <button type="button" class="btn danger" @click.stop="handleRemove(house)">
                删除
              </button>
            </template>
            <template v-else-if="isBuyer">
              <button
                type="button"
                class="btn primary"
                :disabled="purchaseDisabled || !buyerVerified"
                @click.stop="handlePurchase(house)"
              >
                {{
                  !buyerVerified
                    ? '需实名认证'
                    : purchaseDisabled
                    ? '处理中...'
                    : '预约购买'
                }}
              </button>
            </template>
            <span v-else class="readonly">仅供浏览</span>
          </footer>
        </article>
      </div>

      <nav v-if="totalPages > 1" class="pagination" aria-label="分页导航">
        <button
          type="button"
          class="page-btn"
          :disabled="currentPage === 1"
          @click="goToPage(currentPage - 1)"
        >
          上一页
        </button>
        <ul class="page-list" role="list">
          <li v-for="page in totalPages" :key="page">
            <button
              type="button"
              class="page-dot"
              :class="{ active: page === currentPage }"
              @click="goToPage(page)"
            >
              {{ page }}
            </button>
          </li>
        </ul>
        <button
          type="button"
          class="page-btn"
          :disabled="currentPage === totalPages"
          @click="goToPage(currentPage + 1)"
        >
          下一页
        </button>
      </nav>
    </div>

    <HouseDetailModal v-if="detailHouse" :house="detailHouse" @close="closeDetail" />
  </section>
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
    return '—';
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });
};

const formatPrice = (value) => {
  const formatted = formatNumber(value);
  return formatted === '—' ? '—' : `￥${formatted}`;
};

const formatArea = (value) => {
  const formatted = formatNumber(value);
  return formatted === '—' ? '—' : `${formatted} ㎡`;
};

const formatDate = (value) => {
  if (!value) {
    return '—';
  }
  try {
    return new Date(value).toLocaleDateString('zh-CN');
  } catch (error) {
    return '—';
  }
};

const formatKeywords = (keywords) => {
  if (!Array.isArray(keywords) || keywords.length === 0) {
    return '—';
  }
  return keywords.join('、');
};

const maskPhone = (value) => {
  if (!value) {
    return '—';
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
:global(body) {
  font-family: 'Source Han Serif SC', 'Noto Serif SC', 'Songti SC', serif;
}

.gallery-shell {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 2rem;
  padding: 2.5rem;
  border-radius: 2rem;
  background: radial-gradient(circle at 20% 20%, rgba(255, 255, 255, 0.9), rgba(241, 232, 220, 0.85)),
    linear-gradient(135deg, rgba(207, 179, 139, 0.75), rgba(171, 130, 83, 0.85));
  box-shadow: 0 24px 60px rgba(84, 52, 32, 0.25);
  overflow: hidden;
}

.gallery-shell::before,
.gallery-shell::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-repeat: no-repeat;
}

.gallery-shell::before {
  background-image: radial-gradient(circle at 10% 10%, rgba(255, 255, 255, 0.35) 0%, transparent 60%),
    radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.22) 0%, transparent 62%);
}

.gallery-shell::after {
  background-image: radial-gradient(circle at 15% 85%, rgba(171, 130, 83, 0.35) 0%, transparent 65%),
    radial-gradient(circle at 90% 75%, rgba(218, 182, 117, 0.28) 0%, transparent 55%);
}

.gallery-header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 1.5rem;
  position: relative;
  z-index: 1;
}

.headline {
  max-width: 32rem;
}

.headline h2 {
  margin: 0.35rem 0 0.75rem;
  font-size: 2.2rem;
  letter-spacing: 0.12em;
  color: #3c2716;
}

.headline .eyebrow {
  margin: 0;
  font-size: 0.9rem;
  letter-spacing: 0.4em;
  text-transform: uppercase;
  color: rgba(60, 39, 22, 0.65);
}

.headline .subtitle {
  margin: 0;
  font-size: 1rem;
  line-height: 1.6;
  color: rgba(60, 39, 22, 0.72);
}

.stats {
  display: flex;
  gap: 1rem;
  margin: 0;
  padding: 0;
  list-style: none;
  align-items: flex-start;
}

.stat-item {
  min-width: 6.5rem;
  padding: 0.75rem 1rem;
  border-radius: 1.2rem;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(156, 111, 66, 0.25);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
  text-align: center;
}

.stat-item .label {
  display: block;
  font-size: 0.75rem;
  letter-spacing: 0.24em;
  color: rgba(86, 56, 34, 0.7);
}

.stat-item .value {
  display: block;
  margin-top: 0.25rem;
  font-size: 1.4rem;
  font-weight: 600;
  color: #51351e;
}

.panel {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  min-height: 10rem;
  padding: 2rem;
  border-radius: 1.5rem;
  background: rgba(255, 255, 255, 0.78);
  border: 1px dashed rgba(140, 100, 63, 0.35);
  color: rgba(80, 53, 31, 0.8);
  text-align: center;
}

.panel.empty h3 {
  margin: 0;
  font-size: 1.5rem;
  color: #422b17;
}

.panel.empty p {
  margin: 0;
  font-size: 0.95rem;
  line-height: 1.6;
}

.spinner {
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  border: 3px solid rgba(140, 100, 63, 0.25);
  border-top-color: rgba(140, 100, 63, 0.6);
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.board {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.card-wrap {
  display: grid;
  gap: 1.75rem;
  grid-template-columns: repeat(auto-fit, minmax(18rem, 1fr));
}

.house-card {
  display: flex;
  flex-direction: column;
  min-height: 20rem;
  padding: 1.75rem;
  border-radius: 1.75rem;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.92), rgba(246, 235, 220, 0.88));
  border: 1px solid rgba(179, 137, 92, 0.25);
  box-shadow: 0 18px 40px rgba(96, 65, 43, 0.18);
  backdrop-filter: blur(6px);
}

.card-top {
  display: flex;
  justify-content: space-between;
  gap: 1.25rem;
  align-items: flex-start;
}

.title-block {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.title {
  padding: 0;
  background: none;
  border: none;
  margin: 0;
  text-align: left;
  font-size: 1.35rem;
  font-weight: 600;
  color: #3f2919;
  cursor: pointer;
  transition: color 0.2s ease;
}

.title:hover,
.title:focus-visible {
  color: #8c3b1e;
  outline: none;
}

.card-top .subtitle {
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.5;
  color: rgba(70, 45, 27, 0.75);
}

.price-block {
  text-align: right;
  min-width: 6.5rem;
}

.price {
  display: block;
  font-size: 1.4rem;
  font-weight: 600;
  color: #8c3b1e;
}

.price-block .hint {
  display: block;
  margin-top: 0.25rem;
  font-size: 0.75rem;
  letter-spacing: 0.3em;
  color: rgba(140, 59, 30, 0.65);
}

.card-body {
  margin-top: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.spec-grid {
  display: grid;
  gap: 1rem 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
}

.spec dt {
  margin-bottom: 0.3rem;
  font-size: 0.75rem;
  letter-spacing: 0.28em;
  color: rgba(74, 47, 29, 0.6);
}

.spec dd {
  margin: 0;
  font-size: 0.95rem;
  color: #3f2919;
}

.keyword {
  display: inline-flex;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  background: rgba(200, 147, 90, 0.18);
  color: #8c3b1e;
  font-size: 0.85rem;
}

.seller {
  padding: 1.25rem;
  border-radius: 1.25rem;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(200, 161, 120, 0.3);
}

.seller-title {
  margin: 0 0 0.75rem;
  font-size: 0.85rem;
  letter-spacing: 0.24em;
  color: rgba(64, 43, 26, 0.7);
}

.seller-grid {
  display: grid;
  gap: 0.75rem 1rem;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
}

.seller-grid .label {
  display: block;
  font-size: 0.72rem;
  letter-spacing: 0.2em;
  color: rgba(74, 47, 29, 0.55);
}

.seller-grid .value {
  display: block;
  margin-top: 0.25rem;
  font-size: 0.95rem;
  color: #3f2919;
  word-break: break-all;
}

.card-actions {
  margin-top: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  padding: 0.55rem 1.4rem;
  border-radius: 999px;
  font-size: 0.95rem;
  cursor: pointer;
  border: none;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
  font-weight: 500;
}

.btn.primary {
  color: #fff;
  background: linear-gradient(135deg, #b6743d, #8c3b1e);
  box-shadow: 0 10px 24px rgba(140, 59, 30, 0.2);
}

.btn.primary:disabled {
  background: linear-gradient(135deg, rgba(182, 116, 61, 0.5), rgba(140, 59, 30, 0.4));
  cursor: not-allowed;
  box-shadow: none;
}

.btn.outline {
  color: #8c3b1e;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(140, 59, 30, 0.4);
}

.btn.danger {
  color: #fff;
  background: linear-gradient(135deg, #c75c3a, #a7321c);
  box-shadow: 0 10px 24px rgba(167, 50, 28, 0.24);
}

.btn:hover:not(:disabled),
.btn:focus-visible:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 28px rgba(60, 39, 22, 0.18);
  outline: none;
}

.btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 8px 18px rgba(60, 39, 22, 0.12);
}

.readonly {
  font-size: 0.85rem;
  color: rgba(64, 43, 26, 0.6);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.page-btn {
  padding: 0.5rem 1.25rem;
  border-radius: 999px;
  border: none;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(140, 59, 30, 0.35);
  color: #8c3b1e;
  font-size: 0.95rem;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-btn:not(:disabled):hover,
.page-btn:not(:disabled):focus-visible {
  background: rgba(140, 59, 30, 0.12);
  transform: translateY(-1px);
  outline: none;
}

.page-list {
  display: flex;
  gap: 0.5rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.page-dot {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 999px;
  border: none;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid transparent;
  color: #6b4025;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.page-dot:hover,
.page-dot:focus-visible {
  transform: translateY(-1px);
  border-color: rgba(140, 59, 30, 0.35);
  outline: none;
}

.page-dot.active {
  background: linear-gradient(135deg, #b6743d, #8c3b1e);
  color: #fff;
  box-shadow: 0 10px 24px rgba(140, 59, 30, 0.3);
}

@media (max-width: 768px) {
  .gallery-shell {
    padding: 1.5rem;
  }

  .headline h2 {
    font-size: 1.8rem;
  }

  .card-wrap {
    grid-template-columns: 1fr;
  }

  .house-card {
    padding: 1.5rem;
  }
}
</style>
