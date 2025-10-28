<template>
  <section class="review-moderation surface-card">
    <header class="moderation-header">
      <div>
        <h2>{{ t('reviews.moderation.title') }}</h2>
        <p>{{ t('reviews.moderation.subtitle') }}</p>
      </div>
      <span class="badge">{{ reviews.length }}</span>
    </header>

    <p v-if="!reviews.length" class="empty">{{ t('reviews.moderation.empty') }}</p>

    <ul v-else class="moderation-list">
      <li v-for="review in orderedReviews" :key="review.id" class="moderation-item">
        <header>
          <div>
            <h3>{{ review.houseTitle || '-' }}</h3>
            <span class="meta">{{ t('reviews.moderation.authorLabel', { author: review.authorDisplayName }) }}</span>
          </div>
          <span class="rating">{{ formattedRating(review.rating) }}</span>
        </header>
        <p class="content">{{ review.content }}</p>
        <footer>
          <time>{{ t('reviews.moderation.createdLabel', { time: formatDate(review.createdAt) }) }}</time>
        </footer>
        <label class="field">
          <span class="field-label">{{ t('reviews.moderation.notePlaceholder') }}</span>
          <textarea v-model="remarks[review.id]" rows="2" maxlength="200"></textarea>
        </label>
        <div class="actions">
          <button type="button" class="approve" :disabled="moderating" @click="moderate(review.id, 'APPROVED')">
            {{ t('reviews.moderation.approve') }}
          </button>
          <button type="button" class="reject" :disabled="moderating" @click="moderate(review.id, 'REJECTED')">
            {{ t('reviews.moderation.reject') }}
          </button>
        </div>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { computed, inject, reactive, watch } from 'vue';

const props = defineProps({
  reviews: { type: Array, default: () => [] },
  moderating: { type: Boolean, default: false }
});

const emit = defineEmits(['moderate']);

const translate = inject('translate', (key, vars) => {
  if (typeof key !== 'string') {
    return '';
  }
  if (!vars) {
    return key;
  }
  return key;
});

const t = (key, vars) => translate(key, vars);

const remarks = reactive({});

const orderedReviews = computed(() =>
  (props.reviews ?? []).slice().sort((a, b) => new Date(a.createdAt ?? 0) - new Date(b.createdAt ?? 0))
);

watch(
  () => props.reviews,
  (list) => {
    const ids = new Set((list ?? []).map((item) => item.id));
    Object.keys(remarks).forEach((key) => {
      if (!ids.has(key)) {
        delete remarks[key];
      }
    });
  },
  { immediate: true }
);

const formattedRating = (rating) => t('reviews.moderation.ratingLabel', { rating });

const formatDate = (value) => {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  return date.toLocaleString();
};

const moderate = (reviewId, status) => {
  emit('moderate', {
    reviewId,
    status,
    note: remarks[reviewId] ?? ''
  });
  remarks[reviewId] = '';
};
</script>

<style scoped>
.review-moderation {
  display: flex;
  flex-direction: column;
  gap: 1.4rem;
  padding: 1.6rem;
}

.moderation-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.moderation-header h2 {
  margin: 0;
  font-size: 1.35rem;
  color: var(--color-text-strong);
}

.moderation-header p {
  margin: 0.3rem 0 0;
  color: var(--color-text-soft);
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 2.5rem;
  padding: 0.35rem 0.9rem;
  border-radius: var(--radius-pill);
  background: rgba(59, 130, 246, 0.15);
  color: #1d4ed8;
  font-weight: 600;
}

.empty {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.95rem;
}

.moderation-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.moderation-item {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.92);
  padding: 1.4rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  box-shadow: var(--shadow-sm);
}

.moderation-item header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.moderation-item h3 {
  margin: 0;
  font-size: 1.1rem;
  color: var(--color-text-strong);
}

.meta {
  display: block;
  margin-top: 0.25rem;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.rating {
  font-family: 'SF Mono', 'Roboto Mono', monospace;
  letter-spacing: 0.1em;
  color: #f97316;
  font-size: 0.95rem;
}

.content {
  margin: 0;
  color: var(--color-text-soft);
  line-height: 1.6;
}

footer {
  display: flex;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.field-label {
  font-size: 0.85rem;
  color: var(--color-text-soft);
}

textarea {
  width: 100%;
}

.actions {
  display: flex;
  gap: 0.8rem;
  justify-content: flex-end;
}

.approve,
.reject {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.45rem 1.3rem;
  font-weight: 600;
  color: var(--color-text-on-emphasis);
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base),
    opacity var(--transition-base);
}

.approve {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  box-shadow: 0 12px 24px rgba(34, 197, 94, 0.22);
}

.reject {
  background: linear-gradient(135deg, #f97316, #ea580c);
  box-shadow: 0 12px 24px rgba(234, 88, 12, 0.25);
}

.approve:disabled,
.reject:disabled {
  opacity: 0.65;
  box-shadow: none;
  cursor: not-allowed;
}

.approve:not(:disabled):hover,
.reject:not(:disabled):hover {
  transform: translateY(-2px);
}
</style>
