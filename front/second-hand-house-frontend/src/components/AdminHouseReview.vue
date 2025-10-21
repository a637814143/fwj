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
  gap: 1.5rem;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem 1.75rem;
  border-radius: 1.25rem;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.12), rgba(59, 130, 246, 0.12));
  border: 1px solid rgba(14, 165, 233, 0.25);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.25);
}

.review-header h2 {
  margin: 0;
  font-size: 1.6rem;
  color: #0f172a;
}

.review-header p {
  margin: 0.35rem 0 0;
  color: #0369a1;
}

.refresh {
  border: none;
  border-radius: 999px;
  padding: 0.55rem 1.4rem;
  background: linear-gradient(135deg, #0ea5e9, #2563eb);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 15px 28px rgba(14, 165, 233, 0.25);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.refresh:hover {
  transform: translateY(-1px);
  box-shadow: 0 20px 35px rgba(14, 165, 233, 0.32);
}

.state {
  padding: 2.5rem 1.5rem;
  border-radius: 1.25rem;
  text-align: center;
  font-size: 1rem;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(226, 232, 240, 0.9);
  color: #475569;
}

.state.loading {
  color: #0369a1;
  background: rgba(14, 165, 233, 0.08);
  border-color: rgba(14, 165, 233, 0.25);
}

.state.empty h3 {
  margin: 0 0 0.35rem;
  color: #0f172a;
}

.state.empty p {
  margin: 0;
  color: #475569;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.5rem;
}

.review-card {
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 1.5rem;
  overflow: hidden;
  box-shadow: 0 25px 40px rgba(15, 23, 42, 0.18);
  border: 1px solid rgba(226, 232, 240, 0.9);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.review-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 32px 60px rgba(15, 23, 42, 0.22);
}

.visual {
  position: relative;
  height: 180px;
  background: linear-gradient(135deg, #bae6fd, #93c5fd);
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
  color: #0f172a;
  font-weight: 600;
}

.badge {
  position: absolute;
  top: 1rem;
  left: 1rem;
  padding: 0.35rem 0.9rem;
  border-radius: 999px;
  background: rgba(234, 179, 8, 0.92);
  color: #78350f;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  box-shadow: 0 12px 18px rgba(250, 204, 21, 0.25);
}

.content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
}

.card-header h3 {
  margin: 0;
  font-size: 1.35rem;
  color: #0f172a;
}

.card-header .address {
  margin: 0.3rem 0 0;
  color: #475569;
}

.description {
  margin: 0;
  color: #334155;
  line-height: 1.55;
}

.meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.85rem;
  margin: 0;
}

.meta div {
  background: rgba(241, 245, 249, 0.85);
  border-radius: 0.85rem;
  padding: 0.75rem;
}

.meta dt {
  margin: 0 0 0.25rem;
  color: #64748b;
  font-size: 0.8rem;
}

.meta dd {
  margin: 0;
  color: #0f172a;
  font-weight: 600;
}

.keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.keywords span {
  padding: 0.25rem 0.75rem;
  background: rgba(191, 219, 254, 0.6);
  color: #1d4ed8;
  border-radius: 999px;
  font-size: 0.85rem;
}

.certificate {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
  color: #0f172a;
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
  gap: 0.75rem;
  padding: 1rem;
  background: rgba(240, 249, 255, 0.8);
  border-radius: 1rem;
  border: 1px solid rgba(14, 165, 233, 0.2);
}

.seller .label {
  display: block;
  font-size: 0.75rem;
  color: #0369a1;
  margin-bottom: 0.25rem;
}

.seller strong {
  display: block;
  color: #0f172a;
}

.actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.actions button {
  flex: 1 1 140px;
  padding: 0.65rem 1rem;
  border: none;
  border-radius: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.actions .approve {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  box-shadow: 0 18px 30px rgba(34, 197, 94, 0.3);
}

.actions .reject {
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: #fff;
  box-shadow: 0 18px 30px rgba(249, 115, 22, 0.3);
}

.actions button:hover {
  transform: translateY(-2px);
}

@media (max-width: 640px) {
  .review-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .refresh {
    width: 100%;
  }
}
</style>
