<template>
  <section class="house-reviews surface-card">
    <header class="reviews-header">
      <div>
        <h2>{{ t('reviews.title') }}</h2>
        <p>{{ t('reviews.subtitle') }}</p>
      </div>
    </header>

    <div class="reviews-layout">
      <form class="review-form" @submit.prevent="submitReview">
        <h3>{{ t('reviews.form.heading') }}</h3>
        <label class="field">
          <span>{{ t('reviews.form.houseLabel') }}</span>
          <select v-model="form.houseId">
            <option value="" disabled>{{ t('reviews.form.houseLabel') }}</option>
            <option
              v-for="house in availableHouses"
              :key="house.id"
              :value="String(house.id)"
            >
              {{ house.title }}
            </option>
          </select>
        </label>

        <div class="field rating-field">
          <span>{{ t('reviews.form.ratingLabel') }}</span>
          <div class="stars">
            <button
              v-for="score in 5"
              :key="`rating-${score}`"
              type="button"
              :class="['star', { active: form.rating >= score }]"
              :aria-pressed="form.rating >= score"
              @click="setRating(score)"
            >
              {{ form.rating >= score ? '⭐' : '' }}
            </button>
          </div>
          <small class="hint">{{ t('reviews.form.ratingHint') }}</small>
        </div>

        <label class="field">
          <span>{{ t('reviews.form.contentLabel') }}</span>
          <textarea
            v-model="form.content"
            rows="4"
            :placeholder="t('reviews.form.contentPlaceholder')"
            maxlength="500"
          ></textarea>
          <small class="counter">{{ t('reviews.form.charCount', { count: characterCount }) }}</small>
        </label>

        <p v-if="formError" class="form-error">{{ formError }}</p>
        <p v-else-if="!availableHouses.length" class="form-hint">{{ noHousesText }}</p>

        <button class="submit" type="submit" :disabled="!canSubmit">
          {{ canSubmit ? t('reviews.form.submit') : t('reviews.form.submitDisabled') }}
        </button>
      </form>

      <aside class="public-reviews">
        <h3>{{ t('reviews.list.publicTitle') }}</h3>
        <p v-if="publicReviews.length === 0" class="empty">{{ t('reviews.list.empty') }}</p>
        <ul v-else>
          <li v-for="review in publicReviews" :key="review.id" class="public-item">
            <header>
              <h4>{{ review.houseTitle || fallbackTitle }}</h4>
              <span class="rating">{{ formattedRating(review.rating) }}</span>
            </header>
            <p class="content">{{ review.content }}</p>
            <footer>
              <span>{{ t('reviews.list.ratingLabel', { rating: review.rating, author: review.authorDisplayName }) }}</span>
              <time>{{ formatDate(review.moderatedAt || review.createdAt) }}</time>
            </footer>
          </li>
        </ul>
      </aside>
    </div>

    <section v-if="myReviews.length" class="my-reviews">
      <h3>{{ t('reviews.list.myTitle') }}</h3>
      <ul>
        <li v-for="review in myReviews" :key="`mine-${review.id}`" class="my-item">
          <header>
            <div>
              <h4>{{ review.houseTitle || fallbackTitle }}</h4>
              <span class="status" :data-status="review.status">
                {{ statusLabel(review.status) }}
              </span>
            </div>
            <span class="rating">{{ formattedRating(review.rating) }}</span>
          </header>
          <p class="content">{{ review.content }}</p>
          <footer>
            <time>{{ t('reviews.list.timeLabel', { time: formatDate(review.createdAt) }) }}</time>
            <span v-if="review.moderationNote" class="note">
              {{ t('reviews.list.moderationNote', { note: review.moderationNote }) }}
            </span>
          </footer>
        </li>
      </ul>
    </section>
  </section>
</template>

<script setup>
import { computed, inject, reactive, ref, watch } from 'vue';

const props = defineProps({
  houses: { type: Array, default: () => [] },
  reviews: { type: Array, default: () => [] },
  currentUser: { type: Object, default: null },
  eligibleHouseIds: { type: Array, default: () => [] }
});

const emit = defineEmits(['submit']);

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

const form = reactive({
  houseId: '',
  rating: 5,
  content: ''
});

const formError = ref('');

const eligibleHouseIdSet = computed(() => new Set((props.eligibleHouseIds ?? []).map((id) => String(id))));

const availableHouses = computed(() =>
  (props.houses ?? []).filter((house) => {
    if (!house) {
      return false;
    }
    const id = String(house.id ?? '');
    return id && eligibleHouseIdSet.value.has(id);
  })
);

watch(
  availableHouses,
  (list) => {
    if (!list.some((item) => String(item.id) === String(form.houseId))) {
      form.houseId = list.length === 1 ? String(list[0].id ?? '') : '';
    }
  },
  { immediate: true }
);

const myReviews = computed(() => {
  const username = props.currentUser?.username;
  if (!username) {
    return [];
  }
  return (props.reviews ?? [])
    .filter((review) => review?.authorUsername === username)
    .sort((a, b) => new Date(b.createdAt ?? 0) - new Date(a.createdAt ?? 0));
});

const publicReviews = computed(() =>
  (props.reviews ?? [])
    .filter((review) => review?.status === 'APPROVED')
    .sort((a, b) => new Date(b.moderatedAt ?? b.createdAt ?? 0) - new Date(a.moderatedAt ?? a.createdAt ?? 0))
);

const characterCount = computed(() => form.content.trim().length);

const canSubmit = computed(
  () => form.houseId && characterCount.value >= 8 && form.rating >= 1 && form.rating <= 5
);

const fallbackTitle = computed(() => t('reviews.form.houseLabel'));

const noHousesText = computed(() =>
  availableHouses.value.length ? '' : t('reviews.validation.purchaseRequired')
);

const formattedRating = (rating) => {
  const normalized = Math.max(0, Math.min(5, Math.round(Number(rating) || 0)));
  return '★'.repeat(normalized) + '☆'.repeat(5 - normalized);
};

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

const statusLabel = (status) => {
  switch (status) {
    case 'APPROVED':
      return t('reviews.list.approved');
    case 'REJECTED':
      return t('reviews.list.rejected');
    default:
      return t('reviews.list.pending');
  }
};

const setRating = (value) => {
  form.rating = value;
};

const submitReview = () => {
  formError.value = '';
  if (!form.houseId) {
    formError.value = t('reviews.validation.houseRequired');
    return;
  }
  if (!form.rating || form.rating < 1) {
    formError.value = t('reviews.validation.ratingRequired');
    return;
  }
  if (characterCount.value < 8) {
    formError.value = t('reviews.validation.contentLength');
    return;
  }
  emit('submit', {
    houseId: form.houseId,
    rating: form.rating,
    content: form.content.trim()
  });
  form.content = '';
};
</script>

<style scoped>
.house-reviews {
  display: flex;
  flex-direction: column;
  gap: 1.6rem;
  padding: 1.8rem;
}

.reviews-header h2 {
  margin: 0;
  font-size: 1.5rem;
  color: var(--color-text-strong);
}

.reviews-header p {
  margin: 0.35rem 0 0;
  color: var(--color-text-soft);
}

.reviews-layout {
  display: grid;
  gap: 1.4rem;
  grid-template-columns: minmax(320px, 1fr) minmax(320px, 1fr);
  align-items: start;
}

.review-form,
.public-reviews {
  background: rgba(248, 244, 239, 0.85);
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  padding: 1.5rem;
  box-shadow: 0 20px 45px rgba(140, 126, 112, 0.15);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.review-form h3,
.public-reviews h3 {
  margin: 0;
  font-size: 1.15rem;
  color: var(--color-text-strong);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

select,
textarea {
  width: 100%;
}

.rating-field {
  gap: 0.25rem;
}

.stars {
  display: inline-flex;
  gap: 0.4rem;
}

.star {
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 50%;
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  background: #ffffff;
  color: #d9c7b1;
  font-size: 1.25rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform var(--transition-base), background var(--transition-base),
    color var(--transition-base);
}

.star.active {
  background: #ffffff;
  color: #c48b5a;
  border-color: color-mix(in srgb, var(--color-accent) 55%, transparent);
  box-shadow: 0 10px 20px rgba(196, 139, 90, 0.18);
}

.star:hover {
  transform: translateY(-2px);
}

.hint,
.counter {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.form-error {
  margin: 0;
  color: #8f3b3b;
  background: rgba(235, 200, 194, 0.65);
  border-left: 4px solid rgba(198, 120, 110, 0.75);
  padding: 0.6rem 0.8rem;
  border-radius: var(--radius-md);
}

.form-hint {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.submit {
  align-self: flex-start;
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.55rem 1.8rem;
  font-weight: 600;
  background: var(--gradient-primary);
  color: var(--color-text-on-emphasis);
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-base), box-shadow var(--transition-base),
    opacity var(--transition-base);
}

.submit:disabled {
  opacity: 0.65;
  box-shadow: none;
  cursor: not-allowed;
}

.public-reviews ul,
.my-reviews ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.public-item,
.my-item {
  border-radius: var(--radius-md);
  border: 1px solid color-mix(in srgb, var(--color-border) 75%, transparent);
  background: rgba(255, 255, 255, 0.92);
  padding: 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.public-item header,
.my-item header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.public-item h4,
.my-item h4 {
  margin: 0;
  font-size: 1rem;
  color: var(--color-text-strong);
}

.rating {
  font-family: 'SF Mono', 'Roboto Mono', monospace;
  letter-spacing: 0.1em;
  color: #f97316;
}

.content {
  margin: 0;
  color: var(--color-text-soft);
  line-height: 1.6;
}

.public-item footer,
.my-item footer {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.note {
  color: #7c3aed;
}

.status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.2rem 0.75rem;
  border-radius: var(--radius-pill);
  font-size: 0.85rem;
  font-weight: 600;
  background: rgba(148, 163, 184, 0.2);
  color: var(--color-text-soft);
}

.status[data-status='APPROVED'] {
  background: rgba(34, 197, 94, 0.18);
  color: #166534;
}

.status[data-status='REJECTED'] {
  background: rgba(239, 68, 68, 0.18);
  color: #b91c1c;
}

.empty {
  margin: 0;
  color: var(--color-text-muted);
}

@media (max-width: 1024px) {
  .reviews-layout {
    grid-template-columns: 1fr;
  }
}
</style>
