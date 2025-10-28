<template>
  <section class="review-board">
    <header class="review-header">
      <div>
        <h2>Listing review dashboard</h2>
        <p><strong>{{ pendingCount }}</strong> listings require review before they can be shown to buyers.</p>
      </div>
      <button type="button" class="refresh" @click="$emit('refresh')">
        Refresh list
      </button>
    </header>

    <div v-if="loading" class="state loading">Loading listings awaiting review…</div>
    <div v-else-if="pendingCount === 0" class="state empty">
      <h3>No listings are waiting for review</h3>
      <p>Listings submitted by sellers will appear here for approval.</p>
    </div>
    <div v-else class="card-grid">
      <article v-for="house in houses" :key="house.id" class="review-card">
        <div class="visual" :class="{ placeholder: !firstImage(house) }">
          <img v-if="firstImage(house)" :src="firstImage(house)" :alt="house.title" />
          <span v-else>No cover image</span>
          <span class="badge">Pending review</span>
        </div>
        <div class="content">
          <header class="card-header">
            <h3>{{ house.title }}</h3>
            <p class="address">{{ house.address }}</p>
          </header>
          <p v-if="house.description" class="description">{{ house.description }}</p>

          <dl class="meta">
            <div>
              <dt>Price</dt>
              <dd>￥{{ formatCurrency(house.price) }}</dd>
            </div>
            <div>
              <dt>Down payment</dt>
              <dd>￥{{ formatCurrency(house.downPayment) }}</dd>
            </div>
            <div>
              <dt>Instalment plan</dt>
              <dd>
                ￥{{ formatCurrency(house.installmentMonthlyPayment) }} ×
                {{ house.installmentMonths || '—' }} months
              </dd>
            </div>
            <div>
              <dt>Floor area</dt>
              <dd>{{ formatNumber(house.area) }} ㎡</dd>
            </div>
            <div>
              <dt>Listed on</dt>
              <dd>{{ formatDate(house.listingDate) }}</dd>
            </div>
            <div>
              <dt>Floor</dt>
              <dd>{{ house.floor != null ? `Level ${house.floor}` : '—' }}</dd>
            </div>
          </dl>

          <div class="keywords" v-if="Array.isArray(house.keywords) && house.keywords.length">
            <span v-for="keyword in house.keywords" :key="`${house.id}-${keyword}`">#{{ keyword }}</span>
          </div>

          <div class="seller">
            <div>
              <span class="label">Seller account</span>
              <strong>{{ house.sellerUsername }}</strong>
            </div>
            <div>
              <span class="label">Contact name</span>
              <strong>{{ house.sellerName }}</strong>
            </div>
            <div>
              <span class="label">Contact phone</span>
              <strong>{{ house.contactNumber }}</strong>
            </div>
          </div>

          <footer class="actions">
            <button class="approve" type="button" @click="review(house, 'APPROVED')">Approve</button>
            <button class="reject" type="button" @click="review(house, 'REJECTED')">Reject</button>
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

const formatCurrency = (value) => {
  if (value == null || Number.isNaN(Number(value))) {
    return '0.00';
  }
  return Number(value).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const formatNumber = (value) => {
  if (value == null || Number.isNaN(Number(value))) {
    return '—';
  }
  return Number(value).toLocaleString('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });
};

const formatDate = (value) => {
  if (!value) {
    return '—';
  }
  return new Date(value).toLocaleDateString('en-US');
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
