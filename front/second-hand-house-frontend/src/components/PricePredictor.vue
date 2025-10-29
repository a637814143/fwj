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

        <div class="trend-section">
          <h4>{{ t('prediction.trend.title') }}</h4>
          <p class="trend-subtitle">{{ t('prediction.trend.subtitle') }}</p>
          <p v-if="trendLoading" class="trend-message">{{ t('prediction.trend.loading') }}</p>
          <p v-else-if="trendError" class="trend-message notice">{{ trendError }}</p>
          <div
            v-if="hasTrend"
            class="trend-panel"
            role="img"
            :aria-label="t('prediction.trend.ariaLabel')"
          >
            <svg viewBox="0 0 100 100" preserveAspectRatio="none">
              <defs>
                <linearGradient id="trendGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#d28a7c" stop-opacity="0.5" />
                  <stop offset="100%" stop-color="#d28a7c" stop-opacity="0" />
                </linearGradient>
              </defs>
              <path v-if="trendAreaPath" class="trend-area" :d="trendAreaPath" />
              <path v-if="trendPath" class="trend-line" :d="trendPath" />
              <g class="trend-dots">
                <circle
                  v-for="point in trendPoints"
                  :key="`${point.label}-${point.x}`"
                  :cx="point.x"
                  :cy="point.y"
                  r="1.6"
                />
              </g>
            </svg>
            <div class="trend-axis">
              <span>{{ trendAxisLabels.start }}</span>
              <span>{{ trendAxisLabels.mid }}</span>
              <span>{{ trendAxisLabels.end }}</span>
            </div>
          </div>
          <div v-if="hasTrend && trendSummary.start && trendSummary.end" class="trend-summary">
            <div>
              <span>{{ trendSummary.start.label }}</span>
              <strong>¥{{ formatNumber(trendSummary.start.value) }} ×10k</strong>
            </div>
            <div>
              <span>{{ trendSummary.end.label }}</span>
              <strong>¥{{ formatNumber(trendSummary.end.value) }} ×10k</strong>
            </div>
            <div :class="['change', trendSummary.change >= 0 ? 'up' : 'down']">
              <span>{{ t('prediction.trend.change') }}</span>
              <strong>
                {{ trendSummary.change >= 0 ? '+' : '' }}{{ trendSummary.change.toFixed(1) }}%
              </strong>
            </div>
          </div>
          <p v-if="trendSourceMessage" class="trend-source">{{ trendSourceMessage }}</p>
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
const trendSeries = ref([]);
const trendLoading = ref(false);
const trendError = ref('');
const trendSource = ref('none');

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

watch(
  result,
  (value) => {
    if (value) {
      loadTrend(value);
    } else {
      trendRequestToken += 1;
      trendSeries.value = [];
      trendError.value = '';
      trendSource.value = 'none';
      trendLoading.value = false;
    }
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

let trendRequestToken = 0;

const computeTimeValue = (label, fallback) => {
  if (typeof label === 'string' && label.trim()) {
    const normalized = label.trim().length === 7 ? `${label.trim()}-01` : label.trim();
    const timestamp = Date.parse(normalized);
    if (!Number.isNaN(timestamp)) {
      return timestamp;
    }
  }
  return fallback;
};

const normaliseTrendEntry = (entry, index) => {
  if (entry == null) {
    return null;
  }
  if (Array.isArray(entry)) {
    const label = String(entry[0] ?? '');
    const value = Number(entry[1]);
    if (Number.isFinite(value)) {
      return { label, value, timeValue: computeTimeValue(label, index) };
    }
    return null;
  }
  if (typeof entry === 'object') {
    const label = String(
      entry.label ?? entry.month ?? entry.date ?? entry.period ?? entry.time ?? ''
    );
    const value = Number(
      entry.value ?? entry.price ?? entry.amount ?? entry.average ?? entry.mean ?? entry.total
    );
    if (Number.isFinite(value)) {
      return { label, value, timeValue: computeTimeValue(label, index) };
    }
    if (Number.isFinite(Number(entry))) {
      const numeric = Number(entry);
      return { label, value: numeric, timeValue: computeTimeValue(label, index) };
    }
    return null;
  }
  if (typeof entry === 'number') {
    return { label: '', value: entry, timeValue: index };
  }
  return null;
};

const extractTrendSeries = (payload) => {
  if (!payload) {
    return [];
  }
  const candidates = [
    payload.trend?.monthly,
    payload.trend,
    payload.priceTrend,
    payload.historicalPrices,
    payload.history,
    payload.series,
    Array.isArray(payload) ? payload : null
  ];
  for (const candidate of candidates) {
    if (!candidate) {
      continue;
    }
    if (Array.isArray(candidate)) {
      const mapped = candidate
        .map((item, index) => normaliseTrendEntry(item, index))
        .filter(Boolean)
        .sort((a, b) => a.timeValue - b.timeValue)
        .map((item) => ({ label: item.label, value: item.value }));
      if (mapped.length >= 2) {
        return mapped;
      }
    } else if (typeof candidate === 'object') {
      const mapped = Object.entries(candidate)
        .map(([key, value], index) => normaliseTrendEntry({ label: key, value }, index))
        .filter(Boolean)
        .sort((a, b) => a.timeValue - b.timeValue)
        .map((item) => ({ label: item.label, value: item.value }));
      if (mapped.length >= 2) {
        return mapped;
      }
    }
  }
  return [];
};

const buildSyntheticTrend = (basePrice = 100) => {
  const now = new Date();
  const base = Number(basePrice);
  const safeBase = Number.isFinite(base) && base > 0 ? base : 100;
  const series = [];
  for (let i = 23; i >= 0; i -= 1) {
    const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
    const label = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    const seasonal = Math.sin((i / 6) * Math.PI) * 0.04;
    const drift = (12 - i) * 0.004;
    const value = Math.max(0.1, safeBase * (1 + drift + seasonal));
    series.push({ label, value: Number(value.toFixed(2)) });
  }
  return series;
};

const trendPoints = computed(() => {
  if (!Array.isArray(trendSeries.value) || trendSeries.value.length === 0) {
    return [];
  }
  const values = trendSeries.value.map((item) => Number(item.value)).filter(Number.isFinite);
  if (!values.length) {
    return [];
  }
  const max = Math.max(...values);
  const min = Math.min(...values);
  const range = max - min || 1;
  const count = trendSeries.value.length;
  return trendSeries.value.map((item, index) => ({
    label: item.label,
    value: Number(item.value),
    x: count === 1 ? 0 : (index / (count - 1)) * 100,
    y: 100 - ((Number(item.value) - min) / range) * 100
  }));
});

const trendPath = computed(() => {
  if (trendPoints.value.length < 2) {
    return '';
  }
  const segments = trendPoints.value.map((point) => `${point.x},${point.y}`);
  return `M ${segments.join(' L ')}`;
});

const trendAreaPath = computed(() => {
  if (trendPoints.value.length < 2) {
    return '';
  }
  const first = trendPoints.value[0];
  const last = trendPoints.value[trendPoints.value.length - 1];
  const segments = trendPoints.value.map((point) => `${point.x},${point.y}`);
  return `M ${first.x},100 L ${segments.join(' L ')} L ${last.x},100 Z`;
});

const trendAxisLabels = computed(() => {
  if (!trendSeries.value.length) {
    return { start: '', mid: '', end: '' };
  }
  const start = trendSeries.value[0]?.label ?? '';
  const end = trendSeries.value[trendSeries.value.length - 1]?.label ?? '';
  const mid = trendSeries.value[Math.floor(trendSeries.value.length / 2)]?.label ?? '';
  return { start, mid, end };
});

const trendSummary = computed(() => {
  if (trendSeries.value.length < 2) {
    return { start: null, end: null, change: 0 };
  }
  const start = trendSeries.value[0];
  const end = trendSeries.value[trendSeries.value.length - 1];
  const base = Number(start.value);
  const target = Number(end.value);
  const change = Number.isFinite(base) && base !== 0 ? ((target - base) / base) * 100 : 0;
  return { start, end, change };
});

const trendSourceMessage = computed(() => {
  if (!trendSeries.value.length) {
    return '';
  }
  switch (trendSource.value) {
    case 'payload':
      return t('prediction.trend.source.payload');
    case 'api':
      return t('prediction.trend.source.api');
    case 'synthetic':
      return t('prediction.trend.source.synthetic');
    default:
      return '';
  }
});

const hasTrend = computed(() => trendSeries.value.length >= 2);

const loadTrend = async (payload) => {
  const token = ++trendRequestToken;
  trendError.value = '';
  const initialSeries = extractTrendSeries(payload);
  if (initialSeries.length >= 2) {
    if (token === trendRequestToken) {
      trendSeries.value = initialSeries;
      trendSource.value = 'payload';
    }
    return;
  }
  trendLoading.value = true;
  try {
    const { data } = await client.get('/analytics/price-trend', {
      params: {
        area: form.area,
        rooms: form.averageRooms,
        age: form.propertyAge
      }
    });
    if (token !== trendRequestToken) {
      return;
    }
    const fromApi = extractTrendSeries(data);
    if (fromApi.length >= 2) {
      trendSeries.value = fromApi;
      trendSource.value = 'api';
      return;
    }
    trendSeries.value = buildSyntheticTrend(payload?.predictedPrice ?? form.area / 10);
    trendSource.value = 'synthetic';
    trendError.value = '';
  } catch (err) {
    if (token !== trendRequestToken) {
      return;
    }
    console.warn('Failed to load trend data', err);
    trendSeries.value = buildSyntheticTrend(payload?.predictedPrice ?? form.area / 10);
    trendSource.value = 'synthetic';
    trendError.value = t('prediction.trend.unavailable');
  } finally {
    if (token === trendRequestToken) {
      trendLoading.value = false;
    }
  }
};

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
  trendSeries.value = [];
  trendError.value = '';
  trendSource.value = 'none';
  trendLoading.value = false;
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

.trend-section {
  display: grid;
  gap: 0.75rem;
}

.trend-subtitle {
  margin: 0;
  color: var(--color-text-soft);
  font-size: 0.9rem;
}

.trend-message {
  margin: 0;
  color: var(--color-text-muted);
  font-weight: 600;
}

.trend-message.notice {
  color: #8c6a5d;
}

.trend-panel {
  position: relative;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-border) 75%, transparent);
  background: color-mix(in srgb, var(--color-surface) 78%, transparent);
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.trend-panel svg {
  width: 100%;
  height: 160px;
}

.trend-area {
  fill: url(#trendGradient);
}

.trend-line {
  fill: none;
  stroke: #d28a7c;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.trend-dots circle {
  fill: #d28a7c;
}

.trend-axis {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.trend-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  justify-content: space-between;
}

.trend-summary div {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.trend-summary strong {
  font-size: 1rem;
}

.trend-summary .change.up strong {
  color: #2f855a;
}

.trend-summary .change.down strong {
  color: #c05621;
}

.trend-source {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-muted);
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
