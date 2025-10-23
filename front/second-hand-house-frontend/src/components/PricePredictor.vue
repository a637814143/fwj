<template>
  <section class="price-predictor surface-card">
    <header class="predictor-header">
      <h3>{{ t('prediction.title') }}</h3>
      <p>{{ t('prediction.subtitle') }}</p>
    </header>

    <form class="predictor-form" @submit.prevent="submit">
      <div class="field-grid">
        <label>
          <span>{{ t('prediction.fields.area.label') }}</span>
          <input
            v-model.number="form.area"
            type="number"
            min="10"
            step="1"
            required
            :placeholder="t('prediction.fields.area.placeholder')"
            :disabled="loading"
          />
        </label>
        <label>
          <span>{{ t('prediction.fields.rooms.label') }}</span>
          <input
            v-model.number="form.averageRooms"
            type="number"
            min="0.5"
            step="0.1"
            required
            :placeholder="t('prediction.fields.rooms.placeholder')"
            :disabled="loading"
          />
        </label>
        <label>
          <span>{{ t('prediction.fields.age.label') }}</span>
          <input
            v-model.number="form.propertyAge"
            type="number"
            min="0"
            max="120"
            step="1"
            required
            :placeholder="t('prediction.fields.age.placeholder')"
            :disabled="loading"
          />
        </label>
        <label>
          <span>{{ t('prediction.fields.subway.label') }}</span>
          <input
            v-model.number="form.distanceToSubway"
            type="number"
            min="0"
            step="0.1"
            required
            :placeholder="t('prediction.fields.subway.placeholder')"
            :disabled="loading"
          />
        </label>
        <label>
          <span>{{ t('prediction.fields.crime.label') }}</span>
          <input
            v-model.number="form.crimeRate"
            type="number"
            min="0"
            step="0.1"
            required
            :placeholder="t('prediction.fields.crime.placeholder')"
            :disabled="loading"
          />
        </label>
        <label>
          <span>{{ t('prediction.fields.school.label') }}</span>
          <input
            v-model.number="form.schoolScore"
            type="number"
            min="0"
            max="100"
            step="1"
            required
            :placeholder="t('prediction.fields.school.placeholder')"
            :disabled="loading"
          />
        </label>
      </div>
      <div class="actions">
        <button type="submit" :disabled="loading">
          {{ loading ? t('prediction.actions.calculating') : t('prediction.actions.submit') }}
        </button>
        <button type="button" class="secondary" :disabled="loading" @click="reset">
          {{ t('prediction.actions.reset') }}
        </button>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
    </form>

    <transition name="fade">
      <div v-if="result" class="result">
        <div class="result-summary">
          <div class="main-value">
            <span class="label">{{ t('prediction.result.estimate') }}</span>
            <strong>￥{{ formatNumber(result.predictedPrice) }}<small>万</small></strong>
          </div>
          <ul class="range">
            <li>
              <span>{{ t('prediction.result.lower') }}</span>
              <strong>￥{{ formatNumber(result.lowerBound) }} 万</strong>
            </li>
            <li>
              <span>{{ t('prediction.result.upper') }}</span>
              <strong>￥{{ formatNumber(result.upperBound) }} 万</strong>
            </li>
            <li>
              <span>{{ t('prediction.result.confidence') }}</span>
              <strong>{{ (result.confidence * 100).toFixed(0) }}%</strong>
            </li>
          </ul>
        </div>
        <div class="contributions">
          <h4>{{ t('prediction.result.contributionsTitle') }}</h4>
          <ul>
            <li
              v-for="item in formattedContributions"
              :key="item.feature"
              :class="['contribution', item.sign]"
            >
              <span class="feature">{{ item.label }}</span>
              <span class="impact">{{ item.sign === 'positive' ? '+' : '−' }}￥{{ item.value }} 万</span>
            </li>
          </ul>
        </div>
      </div>
    </transition>
  </section>
</template>

<script setup>
import { computed, inject, reactive, ref, watch } from 'vue';
import axios from 'axios';

const props = defineProps({
  apiBaseUrl: {
    type: String,
    required: true
  }
});

const settings = inject('appSettings', { language: 'zh' });
const translate = inject('translate', (key) => key);
const t = (key, vars) => translate(key, vars);

const form = reactive({
  area: 110,
  averageRooms: 3.2,
  propertyAge: 10,
  distanceToSubway: 1.5,
  crimeRate: 3,
  schoolScore: 80
});

const loading = ref(false);
const error = ref('');
const result = ref(null);

const client = axios.create({
  baseURL: props.apiBaseUrl,
  headers: { 'Content-Type': 'application/json' }
});

watch(
  () => props.apiBaseUrl,
  (value) => {
    client.defaults.baseURL = value;
  }
);

const locale = computed(() => (settings?.language === 'en' ? 'en-US' : 'zh-CN'));

const serverErrorMap = Object.freeze({
  '预测需要提供房屋面积': 'prediction.errors.areaRequired',
  '房屋面积必须大于 10 平方米': 'prediction.errors.areaMin',
  '请提供平均卧室数量': 'prediction.errors.roomsRequired',
  '平均卧室数量必须大于 0': 'prediction.errors.roomsMin',
  '请提供房龄信息': 'prediction.errors.ageRequired',
  '房龄不能为负数': 'prediction.errors.ageMin',
  '房龄不能超过 120 年': 'prediction.errors.ageMax',
  '请提供距离地铁的距离': 'prediction.errors.subwayRequired',
  '地铁距离不能为负数': 'prediction.errors.subwayMin',
  '请提供社区治安指数': 'prediction.errors.crimeRequired',
  '治安指数不能为负数': 'prediction.errors.crimeMin',
  '请提供学校评分': 'prediction.errors.schoolRequired',
  '学校评分不能为负数': 'prediction.errors.schoolMin',
  '学校评分不能超过 100 分': 'prediction.errors.schoolMax'
});

const containsCJK = (value) => /[\u3400-\u9FFF]/.test(value ?? '');

const translateServerError = (message) => {
  if (!message) {
    return t('prediction.errors.generic');
  }
  const trimmed = String(message).trim();
  const key = serverErrorMap[trimmed];
  if (key) {
    return t(key);
  }
  if (settings?.language === 'en' && containsCJK(trimmed)) {
    return t('prediction.errors.generic');
  }
  return trimmed;
};

const formattedContributions = computed(() => {
  if (!result.value?.contributions) {
    return [];
  }
  return result.value.contributions.map((item) => {
    const numeric = Number(item.impact);
    return {
      feature: item.featureKey,
      label: t(`prediction.features.${item.featureKey}`),
      value: formatNumber(Math.abs(numeric)),
      sign: numeric >= 0 ? 'positive' : 'negative'
    };
  });
});

const formatNumber = (value) => {
  if (value == null || value === '') {
    return '0.00';
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '0.00';
  }
  return num.toLocaleString(locale.value, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const submit = async () => {
  if (loading.value) {
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const payload = { ...form };
    const { data } = await client.post('/analytics/price-prediction', payload);
    result.value = data;
  } catch (err) {
    const detail = err.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      const raw = Array.isArray(firstError) ? firstError[0] : firstError;
      error.value = translateServerError(raw);
    } else {
      error.value = translateServerError(detail?.detail);
    }
  } finally {
    loading.value = false;
  }
};

const reset = () => {
  form.area = 110;
  form.averageRooms = 3.2;
  form.propertyAge = 10;
  form.distanceToSubway = 1.5;
  form.crimeRate = 3;
  form.schoolScore = 80;
  result.value = null;
  error.value = '';
};
</script>

<style scoped>
.price-predictor {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(148, 163, 184, 0.25);
}

.predictor-header h3 {
  margin: 0;
  font-size: 1.3rem;
}

.predictor-header p {
  margin: 0.4rem 0 0;
  color: var(--color-text-muted);
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.9rem;
}

.field-grid label {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  font-weight: 600;
  color: var(--color-text-strong);
}

.field-grid input {
  padding: 0.6rem 0.75rem;
  border-radius: var(--radius-md);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.85);
}

.actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.actions button {
  padding: 0.65rem 1.4rem;
  border: none;
  border-radius: var(--radius-pill);
  font-weight: 600;
  cursor: pointer;
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: 0 12px 22px rgba(37, 99, 235, 0.22);
}

.actions button.secondary {
  background: rgba(255, 255, 255, 0.8);
  color: var(--color-text-strong);
  border: 1px solid rgba(148, 163, 184, 0.35);
  box-shadow: none;
}

.actions button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.error {
  margin: 0;
  color: #dc2626;
  font-weight: 600;
}

.result {
  display: grid;
  gap: 1.2rem;
}

.result-summary {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.main-value {
  display: flex;
  align-items: flex-end;
  gap: 0.6rem;
}

.main-value strong {
  font-size: 2.1rem;
  display: flex;
  align-items: flex-end;
  gap: 0.2rem;
}

.main-value small {
  font-size: 0.9rem;
}

.range {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.75rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.range li {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  color: var(--color-text-muted);
}

.contributions h4 {
  margin: 0 0 0.6rem;
}

.contributions ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.55rem;
}

.contribution {
  display: flex;
  justify-content: space-between;
  padding: 0.65rem 0.85rem;
  border-radius: var(--radius-md);
  background: rgba(37, 99, 235, 0.08);
  font-weight: 600;
}

.contribution.negative {
  background: rgba(220, 38, 38, 0.08);
}

.contribution.positive .impact {
  color: #2563eb;
}

.contribution.negative .impact {
  color: #dc2626;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .main-value strong {
    font-size: 1.6rem;
  }
}
</style>
