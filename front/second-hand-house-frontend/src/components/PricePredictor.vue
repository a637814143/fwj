<template>
  <section class="price-predictor surface-card">
    <header class="predictor-header">
      <div class="title-block">
        <h3>{{ t('prediction.title') }}</h3>
        <p>{{ t('prediction.subtitle') }}</p>
      </div>
      <div class="quota-pill" :data-state="requiresPaid ? 'paid' : 'free'">
        <span>{{ quotaLabel }}</span>
      </div>
    </header>

    <div class="quota-notice">
      <p>{{ quotaDescription }}</p>
      <p v-if="pointsMessage" class="points-message">{{ pointsMessage }}</p>
    </div>

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
        <button type="submit" :disabled="submitDisabled">
          {{ loading ? t('prediction.actions.calculating') : t('prediction.actions.submit') }}
        </button>
        <button type="button" class="secondary" :disabled="loading" @click="reset">
          {{ t('prediction.actions.reset') }}
        </button>
      </div>
      <p v-if="requiresPaid && !canAfford" class="error subtle">
        {{ t('prediction.errors.pointsInsufficient') }}
      </p>
      <p v-if="error" class="error primary">{{ error }}</p>
    </form>

    <transition name="fade">
      <div v-if="result" class="result">
        <div class="result-summary">
          <div class="main-value">
            <span class="label">{{ t('prediction.result.estimate') }}</span>
            <strong>¥{{ formatNumber(result.predictedPrice) }}<small>×10k</small></strong>
          </div>
          <ul class="range">
            <li>
              <span>{{ t('prediction.result.lower') }}</span>
              <strong>¥{{ formatNumber(result.lowerBound) }} ×10k</strong>
            </li>
            <li>
              <span>{{ t('prediction.result.upper') }}</span>
              <strong>¥{{ formatNumber(result.upperBound) }} ×10k</strong>
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
              <span class="impact">{{ item.sign === 'positive' ? '+' : '−' }}¥{{ item.value }} ×10k</span>
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
  },
  currentUser: {
    type: Object,
    default: null
  },
  wallet: {
    type: Object,
    default: null
  },
  consumePoints: {
    type: Function,
    default: null
  }
});

const settings = inject('appSettings', { language: 'zh' });
const translate = inject('translate', (key) => key);
const t = (key, vars) => translate(key, vars);

const FREE_LIMIT = 3;
const POINT_COST = 10;

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

const usage = reactive({ date: '', freeUses: 0, paidUses: 0 });

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

const usageStorageKey = computed(() =>
  props.currentUser?.username ? `shh-price-usage-${props.currentUser.username}` : null
);

const todayToken = () => new Date().toISOString().slice(0, 10);

const resetUsage = (date = todayToken()) => {
  usage.date = date;
  usage.freeUses = 0;
  usage.paidUses = 0;
};

const ensureUsageDate = () => {
  const token = todayToken();
  if (usage.date !== token) {
    resetUsage(token);
    persistUsage();
  }
};

const restoreUsage = () => {
  resetUsage();
  const key = usageStorageKey.value;
  if (!key || typeof window === 'undefined') {
    return;
  }
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) {
      return;
    }
    const parsed = JSON.parse(raw);
    if (parsed && typeof parsed === 'object') {
      usage.date = parsed.date || todayToken();
      usage.freeUses = Number(parsed.freeUses) || 0;
      usage.paidUses = Number(parsed.paidUses) || 0;
    }
  } catch (err) {
    console.warn('Failed to restore prediction usage', err);
    resetUsage();
  }
  ensureUsageDate();
};

const persistUsage = () => {
  const key = usageStorageKey.value;
  if (!key || typeof window === 'undefined') {
    return;
  }
  try {
    window.localStorage.setItem(
      key,
      JSON.stringify({ date: usage.date, freeUses: usage.freeUses, paidUses: usage.paidUses })
    );
  } catch (err) {
    console.warn('Failed to persist prediction usage', err);
  }
};

watch(
  usageStorageKey,
  () => {
    restoreUsage();
  },
  { immediate: true }
);

const registerUsage = (type) => {
  ensureUsageDate();
  if (type === 'paid') {
    usage.paidUses += 1;
  } else {
    usage.freeUses += 1;
  }
  persistUsage();
};

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

const remainingFreeUses = computed(() => {
  ensureUsageDate();
  return Math.max(0, FREE_LIMIT - usage.freeUses);
});

const requiresPaid = computed(() => remainingFreeUses.value === 0);
const pointsBalance = computed(() => Number(props.wallet?.points ?? 0));
const canAfford = computed(() => pointsBalance.value >= POINT_COST);

const quotaLabel = computed(() =>
  props.currentUser
    ? t('prediction.limits.label', { count: remainingFreeUses.value })
    : t('prediction.limits.loginLabel')
);

const quotaDescription = computed(() => {
  if (!props.currentUser) {
    return t('prediction.limits.loginHint');
  }
  if (remainingFreeUses.value > 0) {
    return t('prediction.limits.freeAvailable', { count: remainingFreeUses.value });
  }
  return t('prediction.limits.paidHint', { cost: POINT_COST });
});

const pointsMessage = computed(() => {
  if (!props.currentUser) {
    return '';
  }
  if (requiresPaid.value) {
    return canAfford.value
      ? t('prediction.limits.pointsReady', { balance: pointsBalance.value, cost: POINT_COST })
      : t('prediction.errors.pointsInsufficient');
  }
  if (pointsBalance.value > 0) {
    return t('prediction.limits.pointsBalance', { balance: pointsBalance.value });
  }
  return '';
});

const submitDisabled = computed(
  () =>
    loading.value ||
    !props.currentUser ||
    (requiresPaid.value && (!props.consumePoints || !canAfford.value))
);

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
  if (loading.value || submitDisabled.value) {
    return;
  }
  loading.value = true;
  error.value = '';
  result.value = null;

  try {
    const payload = { ...form };
    const { data } = await client.post('/analytics/price-prediction', payload);
    if (requiresPaid.value) {
      if (!props.consumePoints) {
        throw new Error(t('prediction.errors.pointsUnsupported'));
      }
      try {
        await props.consumePoints(POINT_COST);
      } catch (consumeError) {
        const message =
          consumeError instanceof Error
            ? consumeError.message
            : t('prediction.errors.consumeFailed');
        throw new Error(message);
      }
      registerUsage('paid');
    } else {
      registerUsage('free');
    }
    result.value = data;
  } catch (err) {
    if (err.response?.data?.detail) {
      error.value = translateServerError(err.response.data.detail);
    } else if (err instanceof Error) {
      error.value = err.message || t('prediction.errors.generic');
    } else {
      error.value = t('prediction.errors.generic');
    }
  } finally {
    loading.value = false;
  }
};

const reset = () => {
  Object.assign(form, {
    area: 110,
    averageRooms: 3.2,
    propertyAge: 10,
    distanceToSubway: 1.5,
    crimeRate: 3,
    schoolScore: 80
  });
  result.value = null;
  error.value = '';
};
</script>

<style scoped>
.price-predictor {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 1.8rem;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(244, 240, 236, 0.82));
}

.predictor-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.title-block h3 {
  margin: 0;
  font-size: 1.45rem;
  color: var(--color-text-strong);
}

.title-block p {
  margin: 0.35rem 0 0;
  color: var(--color-text-soft);
}

.quota-pill {
  border-radius: var(--radius-pill);
  padding: 0.45rem 1rem;
  background: rgba(206, 202, 195, 0.45);
  color: var(--color-text-strong);
  font-weight: 600;
  letter-spacing: 0.02em;
}

.quota-pill[data-state='paid'] {
  background: rgba(186, 160, 160, 0.58);
  color: #fff;
}

.quota-notice {
  display: grid;
  gap: 0.35rem;
  font-size: 0.95rem;
  color: var(--color-text-soft);
}

.points-message {
  margin: 0;
  color: var(--color-text-muted);
}

.predictor-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.field-grid label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-weight: 600;
  color: var(--color-text-strong);
}

.field-grid input {
  padding: 0.75rem 0.85rem;
  border-radius: var(--radius-md);
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  background: rgba(255, 255, 255, 0.85);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.actions {
  display: flex;
  gap: 0.85rem;
  flex-wrap: wrap;
}

.actions button {
  padding: 0.7rem 1.6rem;
  border: none;
  border-radius: var(--radius-pill);
  font-weight: 600;
  cursor: pointer;
  background: linear-gradient(135deg, #c9b5a6, #9fa5aa);
  color: #fff;
  box-shadow: 0 16px 32px rgba(158, 143, 132, 0.25);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.actions button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 20px 40px rgba(158, 143, 132, 0.28);
}

.actions button.secondary {
  background: rgba(255, 255, 255, 0.85);
  color: var(--color-text-strong);
  border: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
  box-shadow: none;
}

.actions button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  box-shadow: none;
}

.error {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
}

.error.primary {
  color: #b05d5d;
}

.error.subtle {
  color: #8c6a5d;
}

.result {
  display: grid;
  gap: 1.35rem;
  background: rgba(255, 255, 255, 0.85);
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  padding: 1.4rem;
}

.result-summary {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.main-value {
  display: flex;
  align-items: flex-end;
  gap: 0.6rem;
}

.main-value strong {
  font-size: 2.2rem;
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
  color: #7c6f66;
}

.main-value small {
  font-size: 0.9rem;
  color: var(--color-text-soft);
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
  gap: 0.25rem;
  color: var(--color-text-soft);
}

.contributions h4 {
  margin: 0 0 0.75rem;
  color: var(--color-text-strong);
}

.contributions ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.65rem;
}

.contribution {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0.9rem;
  border-radius: var(--radius-md);
  background: rgba(189, 186, 178, 0.25);
  color: var(--color-text-strong);
  font-weight: 600;
}

.contribution.positive {
  background: rgba(183, 196, 186, 0.35);
}

.contribution.negative {
  background: rgba(198, 170, 170, 0.32);
}

.contribution .impact {
  font-variant-numeric: tabular-nums;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 720px) {
  .predictor-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .quota-pill {
    align-self: flex-start;
  }
}
</style>
