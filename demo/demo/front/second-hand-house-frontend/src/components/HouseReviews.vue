<template>
  <section class="reviews-panel">
    <div class="panel-header">
      <h2>买家评价</h2>
      <button v-if="house" class="btn refresh" type="button" @click="refreshReviews" :disabled="loading">
        {{ loading ? '刷新中...' : '刷新评价' }}
      </button>
    </div>

    <p v-if="!house" class="empty">请选择一套房源查看评价。</p>

    <div v-else class="content">
      <div class="house-meta">
        <div class="meta-text">
          <h3>{{ house.title }}</h3>
          <p>{{ house.address }}</p>
          <ul>
            <li><strong>价格：</strong>{{ formatNumber(house.price) }} 万元</li>
            <li><strong>面积：</strong>{{ formatNumber(house.area) }} ㎡</li>
            <li><strong>卖家：</strong>{{ house.sellerName }}（{{ house.sellerUsername || '未知账号' }}）</li>
            <li><strong>联系方式：</strong>{{ house.contactNumber }}</li>
          </ul>
          <p v-if="averageRating" class="average">综合评分：{{ averageRating.toFixed(1) }} / 5 （共 {{ reviews.length }} 条）</p>
        </div>
        <div v-if="Array.isArray(house.imageUrls) && house.imageUrls.length" class="meta-gallery">
          <img
            v-for="(image, index) in house.imageUrls"
            :key="index"
            :src="image"
            :alt="`${house.title} 图 ${index + 1}`"
            loading="lazy"
          />
        </div>
      </div>

      <form v-if="canReview" class="review-form" @submit.prevent="handleSubmit">
        <h3>发表我的评价</h3>
        <label>
          评分
          <select v-model.number="form.rating" required :disabled="submitting || loading">
            <option disabled value="">请选择评分</option>
            <option v-for="value in [5, 4, 3, 2, 1]" :key="value" :value="value">
              {{ value }} 分
            </option>
          </select>
        </label>
        <label>
          评价内容
          <textarea
            v-model.trim="form.comment"
            rows="3"
            placeholder="分享一下购买体验或看房感受"
            :disabled="submitting || loading"
          ></textarea>
        </label>
        <div class="form-actions">
          <button class="btn submit" type="submit" :disabled="submitting || !form.rating">
            {{ submitting ? '提交中...' : '提交评价' }}
          </button>
        </div>
      </form>

      <div class="review-list">
        <h3>评价列表</h3>
        <p v-if="loading" class="muted">评价加载中...</p>
        <p v-else-if="!reviews.length" class="muted">暂时还没有买家评价，期待你的第一条分享！</p>
        <article v-else v-for="review in reviews" :key="review.id" class="review-item">
          <header>
            <strong>{{ review.buyerDisplayName }}</strong>
            <span class="rating">评分：{{ review.rating }} / 5</span>
            <time>{{ formatDate(review.createdAt) }}</time>
          </header>
          <p class="comment">{{ review.comment || '买家未填写评价内容。' }}</p>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, watch } from 'vue';

const props = defineProps({
  house: {
    type: Object,
    default: null
  },
  reviews: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  submitting: {
    type: Boolean,
    default: false
  },
  currentUser: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['submit-review', 'refresh']);

const form = reactive({
  rating: '',
  comment: ''
});

const canReview = computed(() => props.currentUser?.role === 'BUYER');

const averageRating = computed(() => {
  if (!props.reviews.length) {
    return 0;
  }
  const total = props.reviews.reduce((sum, item) => sum + Number(item.rating || 0), 0);
  return total / props.reviews.length;
});

watch(
  () => props.house?.id,
  () => {
    form.rating = '';
    form.comment = '';
  }
);

watch(
  () => props.reviews.length,
  (newLength, oldLength) => {
    if (newLength > oldLength) {
      form.rating = '';
      form.comment = '';
    }
  }
);

const handleSubmit = () => {
  if (!props.house || !form.rating) {
    return;
  }
  emit('submit-review', {
    rating: Number(form.rating),
    comment: form.comment
  });
};

const refreshReviews = () => {
  if (!props.house) {
    return;
  }
  emit('refresh');
};

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
    return '';
  }
  return new Date(value).toLocaleString('zh-CN');
};
</script>

<style scoped>
.reviews-panel {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 1.5rem;
  background: #fff;
  border-radius: 1rem;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header h2 {
  margin: 0;
}

.btn.refresh {
  background: #0ea5e9;
  color: #fff;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 0.75rem;
  font-weight: 600;
}

.btn.refresh:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.empty {
  margin: 0;
  color: #64748b;
  text-align: center;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.house-meta {
  display: flex;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.meta-text {
  flex: 1 1 280px;
}

.meta-text ul {
  padding-left: 1.1rem;
  margin: 0.5rem 0;
  color: #475569;
}

.meta-text li {
  margin-bottom: 0.25rem;
}

.meta-text .average {
  margin-top: 0.5rem;
  color: #0ea5e9;
  font-weight: 600;
}

.meta-gallery {
  flex: 1 1 200px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 0.75rem;
}

.meta-gallery img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  border-radius: 0.75rem;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.review-form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1rem;
  border-radius: 0.85rem;
  background: #f8fafc;
}

.review-form h3 {
  margin: 0;
  color: #1e293b;
}

.review-form label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  color: #1f2937;
  font-weight: 600;
}

.review-form select,
.review-form textarea {
  padding: 0.65rem 0.75rem;
  border-radius: 0.65rem;
  border: 1px solid #cbd5f5;
  background: #fff;
}

.review-form textarea {
  resize: vertical;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

.btn.submit {
  padding: 0.55rem 1.2rem;
  border: none;
  border-radius: 0.75rem;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  font-weight: 600;
}

.btn.submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.review-list h3 {
  margin: 0;
}

.review-item {
  padding: 0.85rem 1rem;
  border-radius: 0.85rem;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.review-item header {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: baseline;
  margin-bottom: 0.35rem;
}

.review-item .rating {
  color: #f97316;
  font-weight: 600;
}

.review-item time {
  color: #94a3b8;
}

.review-item .comment {
  margin: 0;
  color: #334155;
}

.muted {
  color: #94a3b8;
}
</style>
