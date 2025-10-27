<template>
  <div class="house-list">
    <div class="list-header">
      <div class="list-header__info">
        <h2>房源列表</h2>
        <p>共 {{ houses.length }} 套房源</p>
      </div>
      <div class="list-controls" v-if="houses.length">
        <label class="page-size">
          <span>每页</span>
          <select v-model.number="pageSize">
            <option v-for="option in pageSizeOptions" :key="option" :value="option">{{ option }}</option>
          </select>
        </label>
      </div>
    </div>

    <div v-if="loading" class="loading">数据加载中...</div>
    <div v-else-if="houses.length === 0" class="empty">
      {{ canManage ? '暂未添加房源，请先通过表单添加。' : '暂未发布房源，稍后再来看看吧。' }}
    </div>

    <template v-else>
      <div class="card-grid">
        <article v-for="house in pagedHouses" :key="house.id" class="house-card">
          <header class="house-card__header">
            <div class="house-card__title">
              <button class="title-button" type="button" @click="openDetail(house)">
                <strong>{{ house.title }}</strong>
              </button>
              <p v-if="house.description" class="description">{{ house.description }}</p>
            </div>
            <div class="house-card__price">
              <span class="label">总价</span>
              <span class="value">￥{{ formatNumber(house.price) }}</span>
            </div>
          </header>

          <div class="house-card__meta">
            <div class="meta-item">
              <span class="meta-label">地理位置</span>
              <span class="meta-value">{{ house.address }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">面积</span>
              <span class="meta-value">{{ formatNumber(house.area) }} ㎡</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">楼层</span>
              <span class="meta-value">{{ house.floor ?? '—' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">挂牌日期</span>
              <span class="meta-value">{{ formatDate(house.listingDate) }}</span>
            </div>
          </div>

          <div class="house-card__people">
            <div class="person">
              <span class="meta-label">卖家账号</span>
              <span class="meta-value">{{ house.sellerUsername || '—' }}</span>
            </div>
            <div class="person">
              <span class="meta-label">卖家姓名</span>
              <span class="meta-value">{{ house.sellerName }}</span>
            </div>
            <div class="person">
              <span class="meta-label">联系方式</span>
              <span class="meta-value">{{ maskPhone(house.contactNumber) }}</span>
            </div>
          </div>

          <div v-if="house.imageUrls?.length" class="house-card__media">
            <span>{{ house.imageUrls.length }} 张图片 · 点击标题查看</span>
          </div>

          <div class="house-card__tags" v-if="formatKeywords(house.keywords) !== '—'">
            <span
              v-for="keyword in house.keywords"
              :key="keyword"
              class="tag"
            >
              {{ keyword }}
            </span>
          </div>

          <footer class="house-card__footer">
            <div class="footnote">最后更新：{{ formatDate(house.updatedAt ?? house.listingDate) }}</div>
            <div class="actions" v-if="canManage">
              <button class="btn" :disabled="!canManage" @click.stop="handleEdit(house)">编辑</button>
              <button class="btn danger" :disabled="!canManage" @click.stop="handleRemove(house)">
                删除
              </button>
            </div>
            <div class="actions" v-else-if="isBuyer">
              <button
                class="btn highlight"
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
            <div class="actions muted" v-else>仅支持浏览</div>
          </footer>
        </article>
      </div>

      <div class="pagination" v-if="totalPages > 1">
        <button type="button" class="pager" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">
          上一页
        </button>
        <div class="pagination__pages">
          <button
            v-for="page in displayPages"
            :key="page"
            type="button"
            class="pager"
            :class="{ active: currentPage === page }"
            @click="goToPage(page)"
          >
            {{ page }}
          </button>
        </div>
        <div class="pagination__status">第 {{ currentPage }} / {{ totalPages }} 页</div>
        <button
          type="button"
          class="pager"
          :disabled="currentPage === totalPages"
          @click="goToPage(currentPage + 1)"
        >
          下一页
        </button>
      </div>
    </template>

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

const isBuyer = computed(() => props.currentUser?.role === 'BUYER');
const buyerVerified = computed(() => props.currentUser?.realNameVerified === true);
const purchaseDisabled = computed(() => props.ordersLoading || loading.value);
const detailHouse = ref(null);
const pageSizeOptions = [4, 6, 8, 12];
const pageSize = ref(6);
const currentPage = ref(1);

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

const totalPages = computed(() => {
  const length = houses.value?.length ?? 0;
  if (length === 0) {
    return 0;
  }
  return Math.ceil(length / pageSize.value);
});

const pagedHouses = computed(() => {
  if (!Array.isArray(houses.value) || houses.value.length === 0) {
    return [];
  }
  const start = (currentPage.value - 1) * pageSize.value;
  return houses.value.slice(start, start + pageSize.value);
});

const displayPages = computed(() => {
  const pages = totalPages.value;
  if (pages <= 1) {
    return [1];
  }
  const range = new Set();
  const windowSize = 2;
  for (let index = currentPage.value - windowSize; index <= currentPage.value + windowSize; index += 1) {
    if (index >= 1 && index <= pages) {
      range.add(index);
    }
  }
  range.add(1);
  range.add(pages);
  return Array.from(range).sort((a, b) => a - b);
});

const goToPage = (page) => {
  if (totalPages.value === 0) {
    return;
  }
  const safePage = Math.min(totalPages.value, Math.max(1, page));
  currentPage.value = safePage;
};

watch(
  () => [houses.value?.length, pageSize.value],
  () => {
    if (totalPages.value === 0) {
      currentPage.value = 1;
      return;
    }
    if (currentPage.value > totalPages.value) {
      currentPage.value = totalPages.value;
    }
  }
);

watch(
  () => houses.value,
  () => {
    currentPage.value = 1;
  }
);
</script>

<style scoped>
.house-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.list-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
}

.list-header__info h2 {
  margin: 0;
  font-size: 1.45rem;
  font-weight: 600;
}

.list-header__info p {
  margin: 0.35rem 0 0;
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.list-controls {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.page-size {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.45rem 0.85rem;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(15, 23, 42, 0.55);
  color: var(--text-secondary);
}

.page-size select {
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-weight: 600;
  padding-right: 1.25rem;
  appearance: none;
}

.loading,
.empty {
  padding: 2rem;
  text-align: center;
  color: var(--text-secondary);
  background: rgba(15, 23, 42, 0.55);
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  backdrop-filter: blur(14px);
}

.card-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 1.25rem;
}

.house-card {
  flex: 1 1 320px;
  background: var(--surface-secondary);
  border-radius: 22px;
  border: 1px solid var(--surface-border);
  padding: 1.4rem;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
  backdrop-filter: blur(18px);
  box-shadow: var(--shadow-strong);
}

.house-card__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.house-card__title {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.title-button {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  border: none;
  background: none;
  padding: 0;
  margin: 0;
  font: inherit;
  color: var(--accent);
  cursor: pointer;
}

.title-button strong {
  font-size: 1.15rem;
  font-weight: 600;
}

.title-button:hover,
.title-button:focus {
  text-decoration: underline;
}

.description {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.4;
}

.house-card__price {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.house-card__price .label {
  color: var(--text-muted);
  font-size: 0.8rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.house-card__price .value {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--text-primary);
}

.house-card__meta,
.house-card__people {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.75rem 1.25rem;
}

.meta-item,
.person {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.meta-label {
  font-size: 0.75rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.meta-value {
  color: var(--text-primary);
  font-size: 0.95rem;
}

.house-card__media {
  padding: 0.5rem 0.75rem;
  border-radius: 12px;
  background: rgba(96, 165, 250, 0.15);
  color: var(--accent);
  font-size: 0.85rem;
  width: fit-content;
}

.house-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.tag {
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.18);
  color: var(--text-primary);
  font-size: 0.75rem;
}

.house-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.footnote {
  color: var(--text-muted);
  font-size: 0.8rem;
}

.actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.actions.muted {
  color: var(--text-muted);
  font-size: 0.9rem;
}

.btn {
  border: none;
  border-radius: 999px;
  padding: 0.5rem 1.35rem;
  font-weight: 600;
  background: rgba(148, 163, 184, 0.25);
  color: var(--text-primary);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(148, 163, 184, 0.25);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn.danger {
  background: rgba(248, 113, 113, 0.25);
  color: #fecaca;
}

.btn.highlight {
  background: linear-gradient(135deg, var(--accent), rgba(56, 189, 248, 0.8));
  color: var(--text-primary);
  box-shadow: 0 18px 35px rgba(56, 189, 248, 0.3);
}

.pagination {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
  justify-content: center;
  padding-top: 0.5rem;
}

.pagination__pages {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.pagination__status {
  color: var(--text-muted);
  font-size: 0.85rem;
}

.pager {
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(15, 23, 42, 0.6);
  color: var(--text-primary);
  padding: 0.45rem 1.1rem;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
}

.pager:hover:not(:disabled) {
  background: rgba(59, 130, 246, 0.35);
  transform: translateY(-1px);
}

.pager:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pager.active {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.9), rgba(99, 102, 241, 0.85));
  border-color: rgba(99, 102, 241, 0.65);
}

@media (max-width: 768px) {
  .card-grid {
    flex-direction: column;
  }

  .house-card {
    flex: 1 1 auto;
  }

  .house-card__meta,
  .house-card__people {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
}
</style>
