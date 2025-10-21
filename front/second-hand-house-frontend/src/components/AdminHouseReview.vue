<template>
  <section class="review-board">
    <header class="review-header">
      <div>
        <h2>房源审核工作台</h2>
        <p>共有 <strong>{{ pendingCount }}</strong> 套待审核房源，审核通过后方可对买家展示。</p>
      </div>
      <button type="button" class="refresh" @click="$emit('refresh')">
        刷新列表
      </button>
    </header>

    <div v-if="loading" class="state loading">正在加载待审核房源，请稍候...</div>
    <div v-else-if="pendingCount === 0" class="state empty">
      <h3>当前暂无新的待审核房源</h3>
      <p>卖家提交房源资料后将出现在此处，请及时完成审核。</p>
    </div>
    <div v-else class="card-grid">
      <article v-for="house in houses" :key="house.id" class="review-card">
        <div class="visual" :class="{ placeholder: !firstImage(house) }">
          <img v-if="firstImage(house)" :src="firstImage(house)" :alt="house.title" />
          <span v-else>暂无封面</span>
          <span class="badge">待审核</span>
        </div>
        <div class="content">
          <header class="card-header">
            <h3>{{ house.title }}</h3>
            <p class="address">{{ house.address }}</p>
          </header>
          <p v-if="house.description" class="description">{{ house.description }}</p>

          <dl class="meta">
            <div>
              <dt>挂牌价</dt>
              <dd>￥{{ formatPrice(house.price) }} 万</dd>
            </div>
            <div>
              <dt>分期方案</dt>
              <dd>
                ￥{{ formatPrice(house.installmentMonthlyPayment) }} 万 ×
                {{ house.installmentMonths || '—' }} 期
              </dd>
            </div>
            <div>
              <dt>房源面积</dt>
              <dd>{{ formatNumber(house.area) }} ㎡</dd>
            </div>
            <div>
              <dt>挂牌日期</dt>
              <dd>{{ formatDate(house.listingDate) }}</dd>
            </div>
            <div>
              <dt>楼层</dt>
              <dd>{{ house.floor != null ? `${house.floor} 层` : '—' }}</dd>
            </div>
          </dl>

          <div class="keywords" v-if="Array.isArray(house.keywords) && house.keywords.length">
            <span v-for="keyword in house.keywords" :key="`${house.id}-${keyword}`">#{{ keyword }}</span>
          </div>

          <div class="certificate" :class="{ missing: !house.propertyCertificateUrl }">
            <span>产权证明：</span>
            <a
              v-if="house.propertyCertificateUrl"
              :href="house.propertyCertificateUrl"
              target="_blank"
              rel="noopener noreferrer"
            >
              查看证明
            </a>
            <span v-else>卖家尚未提供</span>
          </div>

          <div class="seller">
            <div>
              <span class="label">卖家账号</span>
              <strong>{{ house.sellerUsername }}</strong>
            </div>
            <div>
              <span class="label">联系人</span>
              <strong>{{ house.sellerName }}</strong>
            </div>
            <div>
              <span class="label">联系电话</span>
              <strong>{{ house.contactNumber }}</strong>
            </div>
          </div>

          <footer class="actions">
            <button class="approve" type="button" @click="review(house, 'APPROVED')">审核通过</button>
            <button class="reject" type="button" @click="review(house, 'REJECTED')">驳回申请</button>
          </footer>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  houses: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['refresh', 'review']);

const pendingCount = computed(() => props.houses.length);

const formatPrice = (value) => {
  if (value == null || Number.isNaN(Number(value))) {
    return '0.00';
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const formatNumber = (value) => {
  if (value == null || Number.isNaN(Number(value))) {
    return '—';
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });
};

const formatDate = (value) => {
  if (!value) {
    return '—';
  }
  return new Date(value).toLocaleDateString('zh-CN');
};

const firstImage = (house) => {
  if (!house || !Array.isArray(house.imageUrls)) {
    return '';
  }
  return house.imageUrls.find((item) => item && item.trim().length > 0) ?? '';
};

const review = (house, status) => {
  if (!house?.id) {
    return;
  }
  emit('review', { houseId: house.id, status });
};
</script>

<style scoped>
.review-board {
  display: flex;
  flex-direction: column;
  gap: 1.6rem;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.75rem 2rem;
  border-radius: calc(var(--radius-lg) + 0.25rem);
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.16), rgba(59, 130, 246, 0.18));
  border: 1px solid rgba(37, 99, 235, 0.25);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(calc(var(--glass-blur) / 2));
}

.review-header h2 {
  margin: 0;
  font-size: 1.65rem;
  color: var(--color-text-strong);
}

.review-header p {
  margin: 0.4rem 0 0;
  color: #0369a1;
}

.refresh {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.65rem 1.6rem;
  background: var(--gradient-primary);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 18px 34px rgba(14, 165, 233, 0.3);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.refresh:hover {
  transform: translateY(-2px);
  box-shadow: 0 26px 48px rgba(14, 165, 233, 0.34);
}

.state {
  padding: 2.6rem 1.65rem;
  border-radius: var(--radius-xl);
  text-align: center;
  font-size: 1rem;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(226, 232, 240, 0.7);
  color: var(--color-text-muted);
  backdrop-filter: blur(calc(var(--glass-blur) / 2));
}

.state.loading {
  color: #0369a1;
  background: rgba(14, 165, 233, 0.12);
  border-color: rgba(14, 165, 233, 0.28);
}

.state.empty h3 {
  margin: 0 0 0.4rem;
  color: var(--color-text-strong);
}

.state.empty p {
  margin: 0;
  color: var(--color-text-muted);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.6rem;
}

.review-card {
  display: flex;
  flex-direction: column;
  background: var(--gradient-surface);
  border-radius: calc(var(--radius-lg) + 0.25rem);
  overflow: hidden;
  box-shadow: 0 28px 60px rgba(15, 23, 42, 0.22);
  border: 1px solid rgba(226, 232, 240, 0.65);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.review-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 38px 90px rgba(15, 23, 42, 0.25);
}

.visual {
  position: relative;
  height: 190px;
  background: linear-gradient(135deg, rgba(186, 230, 253, 0.95), rgba(147, 197, 253, 0.85));
  display: flex;
  align-items: center;
  justify-content: center;
}

.visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.visual.placeholder {
  color: var(--color-text-strong);
  font-weight: 600;
}

.badge {
  position: absolute;
  top: 1.1rem;
  left: 1.1rem;
  padding: 0.4rem 1rem;
  border-radius: var(--radius-pill);
  background: rgba(234, 179, 8, 0.92);
  color: #78350f;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  box-shadow: 0 12px 18px rgba(250, 204, 21, 0.3);
}

.content {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
  padding: 1.6rem;
}

.card-header h3 {
  margin: 0;
  font-size: 1.35rem;
  color: var(--color-text-strong);
}

.card-header .address {
  margin: 0.35rem 0 0;
  color: var(--color-text-muted);
}

.description {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.55;
}

.meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.9rem;
  margin: 0;
}

.meta div {
  background: rgba(241, 245, 249, 0.85);
  border-radius: var(--radius-md);
  padding: 0.8rem;
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.meta dt {
  margin: 0 0 0.3rem;
  color: var(--color-text-soft);
  font-size: 0.82rem;
}

.meta dd {
  margin: 0;
  color: var(--color-text-strong);
  font-weight: 600;
}

.keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.keywords span {
  padding: 0.3rem 0.85rem;
  background: rgba(191, 219, 254, 0.7);
  color: #1d4ed8;
  border-radius: var(--radius-pill);
  font-size: 0.85rem;
  font-weight: 600;
}

.certificate {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  font-size: 0.95rem;
  color: var(--color-text-strong);
}

.certificate a {
  color: #0ea5e9;
  font-weight: 600;
}

.certificate.missing {
  color: #b91c1c;
}

.seller {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.85rem;
  padding: 1.05rem;
  background: rgba(240, 249, 255, 0.9);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(14, 165, 233, 0.28);
}

.seller .label {
  display: block;
  font-size: 0.78rem;
  color: #0369a1;
  margin-bottom: 0.3rem;
  font-weight: 600;
}

.seller strong {
  display: block;
  color: var(--color-text-strong);
}

.actions {
  display: flex;
  gap: 0.9rem;
  flex-wrap: wrap;
}

.actions button {
  flex: 1 1 150px;
  padding: 0.75rem 1.05rem;
  border: none;
  border-radius: var(--radius-pill);
  font-weight: 600;
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.actions .approve {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  box-shadow: 0 22px 40px rgba(34, 197, 94, 0.32);
}

.actions .reject {
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: #fff;
  box-shadow: 0 22px 40px rgba(249, 115, 22, 0.32);
}

.actions button:hover {
  transform: translateY(-2px);
}

@media (max-width: 640px) {
  .review-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1.1rem;
  }

  .refresh {
    width: 100%;
  }
}
</style>
